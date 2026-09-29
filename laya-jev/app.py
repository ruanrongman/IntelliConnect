"""Local multilingual Laya service compatible with IntelliConnect's JevClient."""
from __future__ import annotations

import asyncio
import hmac
import json
import logging
import math
import os
import re
from concurrent.futures import ThreadPoolExecutor
from contextlib import asynccontextmanager
from dataclasses import dataclass
from functools import cache
from pathlib import Path
from typing import Any

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

from resources import Resources, allocate

LOG = logging.getLogger("laya_jev")
ROOT = Path(__file__).resolve().parent
MODEL_ID = "convaiinnovations/laya-multilingual"


@dataclass(frozen=True)
class Settings:
    model_path: str = MODEL_ID
    device: str = "cpu"
    api_key: str = ""
    max_tokens: int = 8192
    max_questions: int = 32
    max_batch_tokens: int = 16384
    timeout: float = 8.0
    inference_timeout: float = 60.0
    busy_retry_seconds: int = 3
    threads: int = 4
    workers: int = 1
    physical_cores: int = 0
    logical_cpus: int = 0
    cpu_budget: int = 0
    max_body_bytes: int = 1_048_576

    @classmethod
    def from_env(cls):
        resources = Resources.detect()
        device = os.getenv("LAYA_DEVICE", "cpu")
        timeout = float(os.getenv("LAYA_REQUEST_TIMEOUT_SECONDS", "8"))
        workers, threads, budget = allocate(
            resources,
            workers=int(os.environ["LAYA_WORKERS"]) if "LAYA_WORKERS" in os.environ else None,
            threads=int(os.environ["LAYA_THREADS"]) if "LAYA_THREADS" in os.environ else None,
            device=device,
        )
        settings = cls(
            model_path=os.getenv("LAYA_MODEL_PATH", MODEL_ID),
            device=device,
            api_key=os.getenv("LAYA_API_KEY", ""),
            max_tokens=int(os.getenv("LAYA_MAX_TOKENS", "8192")),
            max_questions=int(os.getenv("LAYA_MAX_QUESTIONS", "32")),
            max_batch_tokens=int(os.getenv("LAYA_MAX_BATCH_TOKENS", "16384")),
            timeout=timeout,
            inference_timeout=float(os.getenv("LAYA_INFERENCE_TIMEOUT_SECONDS",
                                              str(max(60.0, timeout)))),
            busy_retry_seconds=int(os.getenv("LAYA_BUSY_RETRY_SECONDS", "3")),
            threads=threads, workers=workers, cpu_budget=budget,
            physical_cores=resources.physical_cores, logical_cpus=resources.logical_cpus,
        )
        if not 16 <= settings.max_tokens <= 8192:
            raise ValueError("LAYA_MAX_TOKENS must be between 16 and 8192")
        if (not math.isfinite(settings.timeout) or settings.timeout <= 0
                or min(settings.threads, settings.max_questions, settings.max_batch_tokens,
                       settings.busy_retry_seconds) < 1):
            raise ValueError("Thread, question, token and timeout limits must be positive")
        if (not math.isfinite(settings.inference_timeout)
                or settings.inference_timeout < settings.timeout):
            raise ValueError("LAYA_INFERENCE_TIMEOUT_SECONDS must be finite and at least "
                             "LAYA_REQUEST_TIMEOUT_SECONDS")
        return settings


def invalid(message: str):
    raise HTTPException(422, detail=message)


def structured(value: Any) -> bool:
    return isinstance(value, (dict, list)) or isinstance(value, str) and bool(value.strip())


def validate(body: Any, settings: Settings) -> dict:
    if not isinstance(body, dict) or not structured(body.get("state")):
        invalid("state must be a string, object or array")
    model = body.get("model")
    if not isinstance(model, str) or not (
        re.fullmatch(r"jev-[A-Za-z0-9._-]+", model)
        or model in {"multilingual", "laya-multilingual", MODEL_ID}
    ):
        invalid("Unsupported model; use jev-latest or multilingual")
    questions = body.get("questions")
    if not isinstance(questions, dict) or not 1 <= len(questions) <= settings.max_questions:
        invalid(f"questions must contain 1 to {settings.max_questions} entries")
    for qid, question in questions.items():
        if not qid.strip() or not isinstance(question, dict):
            invalid("Each question needs a nonblank id and an object definition")
        if not structured(question.get("instructions")):
            invalid("instructions must be a string, object or array")
        kind, criteria = question.get("type"), question.get("criteria")
        if kind == "noul":
            if criteria is not None and (
                not isinstance(criteria, dict) or not set(criteria) <= {"true", "false"}
                or not all(structured(value) for value in criteria.values())
            ):
                invalid("noul criteria must describe true and/or false")
        elif kind == "choice":
            if (not isinstance(criteria, dict) or not 1 <= len(criteria) <= 255
                    or not all(value is None or structured(value) for value in criteria.values())):
                invalid("choice requires 1 to 255 options with structured or null descriptions")
        elif kind == "score":
            if (not isinstance(criteria, list) or not 2 <= len(criteria) <= 10
                    or not all(structured(value) for value in criteria)):
                invalid("score requires 2 to 10 structured levels")
        else:
            invalid("Question type must be noul, choice or score")
    return body


class InputBudgetError(ValueError):
    pass


@cache
def configure_torch(threads: int):
    # Set these before importing torch; all model replicas share the same CPU thread budget.
    os.environ["OMP_NUM_THREADS"] = str(threads)
    os.environ["MKL_NUM_THREADS"] = str(threads)
    import torch
    torch.set_num_threads(threads)
    torch.set_num_interop_threads(1)


class LayaEngine:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.agent = None

    def load(self):
        os.environ.setdefault("HF_HOME", str(ROOT / ".runtime" / "huggingface"))
        os.environ.setdefault("TOKENIZERS_PARALLELISM", "false")
        os.environ.setdefault("USE_TF", "0")
        configure_torch(self.settings.threads)
        import laya

        # Direct SDK loading avoids Router(preload=True) loading three separate models.
        self.agent = laya.load(self.settings.model_path, device=self.settings.device)
        supported = getattr(self.agent.model.encoder.config, "max_position_embeddings", 8192)
        if self.settings.max_tokens > supported:
            raise ValueError("LAYA_MAX_TOKENS exceeds checkpoint position limit")
        # Disallow a silent CPU fallback if CUDA was explicitly requested.
        if self.settings.device.startswith("cuda") and self.agent.device.type != "cuda":
            raise RuntimeError("CUDA requested but the model could not load on CUDA")
        LOG.info("Loaded multilingual Laya on %s", self.agent.device)

    def prepare(self, state: Any, questions: dict) -> int:
        """Budget exactly as laya 0.3.7 does; never truncate instructions or state."""
        from laya.common import render_options, serialize_state

        tok = self.agent.tok
        def token_count(text):
            return len(tok(text.replace(tok.mask_token, " "), add_special_tokens=False)["input_ids"])

        state_tokens = token_count(serialize_state(state))
        largest_head = 0
        lengths = []
        for question in questions.values():
            internal = self.agent._to_internal(question)
            instruction_tokens = token_count(f"{internal['t']} question: {internal['ins']}")
            # Upstream caps each rendered criterion at 48 tokens (plus its marker).
            option_tokens = sum(1 + min(48, token_count(" " + option))
                                for option in render_options(internal))
            # Keep at least 16 tokens in upstream's instruction allocation to avoid shrinking options.
            largest_head = max(largest_head, max(16, instruction_tokens) + option_tokens)
            length = instruction_tokens + option_tokens + state_tokens + 4
            if length > self.settings.max_tokens:
                raise InputBudgetError("Input exceeds LAYA_MAX_TOKENS; shorten state or instructions")
            lengths.append(length)
        # The batch is padded to the longest sequence, so account for padded allocation.
        if max(lengths) * len(lengths) > self.settings.max_batch_tokens:
            raise InputBudgetError("Batch exceeds LAYA_MAX_BATCH_TOKENS; split the questions")
        return largest_head

    def predict(self, body: dict) -> dict:
        head_budget = self.prepare(body["state"], body["questions"])
        # Each concurrent slot owns its own agent, tokenizer, weights and mutable configuration.
        self.agent.cfg["max_len"] = self.settings.max_tokens
        self.agent.cfg["head_max_len"] = head_budget
        result = self.agent.predict(body["state"], body["questions"])
        result["model"] = "laya-multilingual"
        return result

    def close(self):
        if self.agent is not None:
            self.agent.__exit__(None, None, None)


class InferenceWorker:
    def __init__(self, engines, timeout: float, busy_retry_seconds: int = 3,
                 *, inference_timeout: float = 60.0):
        self.engines = engines if isinstance(engines, list) else [engines]
        if not self.engines or len({id(engine) for engine in self.engines}) != len(self.engines):
            raise ValueError("Inference slots must have distinct engine instances")
        if (not math.isfinite(timeout) or timeout <= 0
                or not math.isfinite(inference_timeout) or inference_timeout < timeout):
            raise ValueError("Inference timeout must be finite and at least the positive request timeout")
        self.timeout = timeout
        self.inference_timeout = inference_timeout
        self.busy_retry_seconds = busy_retry_seconds
        self.executor = ThreadPoolExecutor(max_workers=len(self.engines), thread_name_prefix="laya")
        self.pending = {}
        self.stalled = set()
        self.memory_exhausted = False
        self.closed = False

    @property
    def unhealthy(self):
        # Slow requests recover when they finish; memory exhaustion still requires a restart.
        return self.memory_exhausted or bool(self.stalled)

    @property
    def active_count(self):
        return sum(not future.done() for future in self.pending)

    @property
    def busy(self):
        return self.active_count > 0

    async def run(self, body):
        # No await between availability check and submission: atomic on the single event loop.
        # Keep a slot reserved until its completion callback has also checked for fatal errors.
        occupied = set(self.pending.values())
        if len(occupied) == len(self.engines) or self.unhealthy or self.closed:
            LOG.warning("Inference unavailable: active=%d workers=%d unhealthy=%s",
                        len(occupied), len(self.engines), self.unhealthy)
            reason = "Inference unavailable; service restart required" if self.unhealthy \
                else "Inference capacity is busy; retry later"
            raise HTTPException(529, reason,
                                headers={"Retry-After": str(self.busy_retry_seconds)})
        slot = next(index for index in range(len(self.engines)) if index not in occupied)
        concurrent = self.executor.submit(self.engines[slot].predict, body)
        self.pending[concurrent] = slot
        future = asyncio.wrap_future(concurrent)
        # Only the longer execution deadline indicates a stuck native operation.
        # This watchdog survives both an HTTP timeout and caller cancellation.
        def expire():
            if not concurrent.done():
                self.stalled.add(concurrent)
                LOG.error("Inference still running after %.1f seconds in slot %d; restart required",
                          self.inference_timeout, slot)

        watchdog = asyncio.get_running_loop().call_later(self.inference_timeout, expire)
        def finished(completed):
            watchdog.cancel()
            self.pending.pop(concurrent, None)
            was_stalled = concurrent in self.stalled
            self.stalled.discard(concurrent)
            # Retrieve failures even when the HTTP caller disconnects or times out.
            if not completed.cancelled():
                error = completed.exception()
                if isinstance(error, MemoryError) or type(error).__name__ == "OutOfMemoryError":
                    self.memory_exhausted = True
            if was_stalled and not self.unhealthy:
                LOG.warning("Stalled inference completed in slot %d; service recovered", slot)

        future.add_done_callback(finished)
        try:
            # shield prevents caller cancellation from releasing a still-running native operation.
            return await asyncio.wait_for(asyncio.shield(future), timeout=self.timeout)
        except asyncio.TimeoutError:
            LOG.warning("Inference request timed out after %.1f seconds; slot %d remains reserved "
                        "until inference completes", self.timeout, slot)
            raise HTTPException(529, "Inference request timed out; retry later",
                                headers={"Retry-After": str(self.busy_retry_seconds)}) from None

    def close(self):
        self.closed = True
        # Never unload model parameters while an uncancellable native operation is still using them.
        if not self.busy:
            for engine in self.engines:
                engine.close()
        self.executor.shutdown(wait=False, cancel_futures=True)


def create_app(engine=None, settings: Settings | None = None, *, engine_factory=None) -> FastAPI:
    settings = settings or Settings.from_env()
    if engine is not None and settings.workers != 1:
        raise ValueError("Use engine_factory to create a distinct engine for each worker")
    factory = engine_factory or LayaEngine
    engines = [engine] if engine is not None else [factory(settings) for _ in range(settings.workers)]

    def load_engines():
        try:
            # Sequential loading avoids upstream tokenizer config file edits racing at startup.
            for item in engines:
                item.load()
        except BaseException:
            for item in engines:
                item.close()
            raise

    @asynccontextmanager
    async def lifespan(app):
        await asyncio.to_thread(load_engines)
        worker = InferenceWorker(engines, settings.timeout, settings.busy_retry_seconds,
                                 inference_timeout=settings.inference_timeout)
        app.state.worker = worker
        LOG.info("CPU physical=%d logical=%d budget=%d; inference slots=%d threads/slot=%d",
                 settings.physical_cores, settings.logical_cpus, settings.cpu_budget,
                 len(engines), settings.threads)
        try:
            yield
        finally:
            worker.close()

    app = FastAPI(title="Laya Jev Adapter", lifespan=lifespan)

    @app.get("/health")
    async def health():
        worker = app.state.worker
        return JSONResponse(status_code=503 if worker.unhealthy else 200, content={
            "status": "unhealthy" if worker.unhealthy else "ok",
            "model": "laya-multilingual", "device": settings.device,
            "busy": worker.busy, "restart_required": worker.unhealthy,
            "stalled_requests": len(worker.stalled),
            "active_requests": worker.active_count, "workers": len(engines),
            "request_timeout_seconds": settings.timeout,
            "inference_timeout_seconds": settings.inference_timeout,
            "threads_per_worker": settings.threads, "physical_cores": settings.physical_cores,
            "logical_cpus": settings.logical_cpus, "cpu_budget": settings.cpu_budget,
        })

    @app.post("/v1/systemone")
    async def systemone(request: Request):
        if settings.api_key and not hmac.compare_digest(
            request.headers.get("authorization", "").encode(),
            ("Bearer " + settings.api_key).encode(),
        ):
            raise HTTPException(401, "Invalid or missing Bearer token")
        payload = bytearray()
        async for chunk in request.stream():
            payload.extend(chunk)
            if len(payload) > settings.max_body_bytes:
                raise HTTPException(413, "Request body too large")
        try:
            body = json.loads(payload, parse_constant=lambda _: invalid("Non-finite JSON value"))
        except (ValueError, UnicodeError):
            invalid("Request must contain valid JSON")
        body = validate(body, settings)
        try:
            return await app.state.worker.run(body)
        except InputBudgetError as exc:
            raise HTTPException(422, str(exc)) from None
        except HTTPException:
            raise
        except Exception as exc:
            # Messages from libraries may include input text; only log the exception type.
            LOG.error("Laya inference failed (%s)", type(exc).__name__)
            if isinstance(exc, MemoryError) or type(exc).__name__ == "OutOfMemoryError":
                app.state.worker.memory_exhausted = True
                raise HTTPException(529, "Model memory exhausted; restart required",
                                    headers={"Retry-After": "1"}) from None
            raise HTTPException(500, "Laya inference failed") from None

    return app


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(create_app(), host=os.getenv("LAYA_HOST", "127.0.0.1"),
                port=int(os.getenv("LAYA_PORT", "8001")), access_log=False)
