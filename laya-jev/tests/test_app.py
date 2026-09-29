import asyncio
import copy
import threading

import httpx
import pytest
from fastapi.testclient import TestClient

from app import InferenceWorker, InputBudgetError, Settings, create_app


def payload():
    return {
        "model": "jev-latest", "state": {"question": "讲个开心的故事", "memory": []},
        "questions": {
            "urgent": {"type": "noul", "instructions": ["Urgent?", {"hint": "now"}]},
            "emotion": {"type": "choice", "instructions": {"question": "Emotion?"},
                        "criteria": {"happy": {"meaning": "开心"}, "neutral": None}},
            "rating": {"type": "score", "instructions": "Rate it",
                       "criteria": ["low", {"meaning": "high"}]},
        },
    }


class Engine:
    def __init__(self):
        self.calls = []
        self.closed = False
        self.loaded = False

    def load(self):
        self.loaded = True

    def close(self):
        self.closed = True

    def predict(self, body):
        self.calls.append(copy.deepcopy(body))
        return {"model": "laya-multilingual", "answers": {
            "urgent": {"type": "noul", "noul": 0.2},
            "emotion": {"type": "choice", "choice": "happy", "confidence": 0.8,
                        "probabilities": {"happy": 0.9, "neutral": 0.1}},
            "rating": {"type": "score", "score": 0.7, "confidence": 0.5,
                       "probabilities": {"0": 0.3, "1": 0.7},
                       "legend": {"0": "low", "1": {"meaning": "high"}}},
        }, "usage": {"input_tokens": 42, "output_tokens": 0}}


@pytest.mark.parametrize("alias", ["jev-latest", "jev-preview", "jev-1.13.0",
                                    "multilingual", "convaiinnovations/laya-multilingual"])
def test_protocol_and_aliases(alias):
    engine = Engine()
    body = payload()
    body["model"] = alias
    with TestClient(create_app(engine, Settings())) as client:
        response = client.post("/v1/systemone", json=body,
                               headers={"Authorization": "Bearer existing-java-key"})
        assert response.status_code == 200
        result = response.json()
        assert set(result["answers"]) == set(body["questions"])
        assert result["usage"] == {"input_tokens": 42, "output_tokens": 0}
        assert engine.calls == [body]
        assert client.get("/health").json()["status"] == "ok"
    assert engine.closed


def test_auth():
    with TestClient(create_app(Engine(), Settings(api_key="secret"))) as client:
        assert client.post("/v1/systemone", json=payload()).status_code == 401
        assert client.post("/v1/systemone", json=payload(),
                           headers={"Authorization": "Bearer secret"}).status_code == 200


@pytest.mark.parametrize("mutate", [
    lambda b: b.update(model="unknown"),
    lambda b: b.update(state=None),
    lambda b: b.update(questions={}),
    lambda b: b["questions"]["emotion"].update(type="bad"),
    lambda b: b["questions"]["emotion"].update(criteria={}),
    lambda b: b["questions"]["emotion"].update(criteria={str(i): None for i in range(256)}),
    lambda b: b["questions"]["rating"].update(criteria=["one"]),
    lambda b: b["questions"]["rating"].update(criteria=["level"] * 11),
    lambda b: b["questions"]["urgent"].update(criteria={"yes": "yes"}),
    lambda b: b["questions"]["urgent"].update(instructions=""),
])
def test_invalid_requests(mutate):
    engine = Engine()
    body = payload()
    mutate(body)
    with TestClient(create_app(engine, Settings())) as client:
        assert client.post("/v1/systemone", json=body).status_code == 422
    assert not engine.calls


def test_json_and_size_limits():
    with TestClient(create_app(Engine(), Settings(max_body_bytes=5000))) as client:
        assert client.post("/v1/systemone", content="{").status_code == 422
        assert client.post("/v1/systemone", content='{"state":NaN}').status_code == 422
        assert client.post("/v1/systemone", content="x" * 5001).status_code == 413


@pytest.mark.parametrize("error,status", [(RuntimeError("private input"), 500),
                                          (InputBudgetError("too long"), 422),
                                          (MemoryError("private input"), 529)])
def test_inference_errors(error, status):
    class Broken(Engine):
        def predict(self, body):
            raise error
    with TestClient(create_app(Broken(), Settings())) as client:
        response = client.post("/v1/systemone", json=payload())
        assert response.status_code == status
        assert "private input" not in response.text
        assert client.get("/health").status_code == (503 if status == 529 else 200)


class Blocking(Engine):
    def __init__(self):
        super().__init__()
        self.started = threading.Event()
        self.release = threading.Event()

    def predict(self, body):
        self.started.set()
        assert self.release.wait(5)
        return super().predict(body)


def test_simultaneous_request_is_rejected_and_health_stays_responsive():
    async def scenario():
        engine = Blocking()
        app = create_app(engine, Settings(timeout=3))
        async with app.router.lifespan_context(app):
            async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app),
                                         base_url="http://test") as client:
                first = asyncio.create_task(client.post("/v1/systemone", json=payload()))
                try:
                    assert await asyncio.to_thread(engine.started.wait, 2)
                    second = await asyncio.wait_for(client.post("/v1/systemone", json=payload()), 0.5)
                    assert second.status_code == 529
                    assert second.headers["retry-after"] == "3"
                    health = await asyncio.wait_for(client.get("/health"), 0.5)
                    assert health.status_code == 200 and health.json()["busy"]
                finally:
                    engine.release.set()
                    assert (await first).status_code == 200
                assert (await client.post("/v1/systemone", json=payload())).status_code == 200
    asyncio.run(scenario())


async def release_inferences(worker, *engines):
    pending = list(worker.pending)
    for engine in engines:
        engine.release.set()
    await asyncio.gather(*(asyncio.wrap_future(future) for future in pending),
                         return_exceptions=True)
    await asyncio.sleep(0)  # Allow the worker's completion callbacks to run.


def test_request_timeout_reserves_slot_then_recovers_without_restart():
    async def scenario():
        engine = Blocking()
        app = create_app(engine, Settings(timeout=0.05))
        async with app.router.lifespan_context(app):
            async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app),
                                         base_url="http://test") as client:
                try:
                    response = await client.post("/v1/systemone", json=payload())
                    assert response.status_code == 529
                    assert response.headers["retry-after"] == "3"
                    health = await client.get("/health")
                    assert health.status_code == 200
                    assert health.json()["active_requests"] == 1
                    assert not health.json()["restart_required"]
                    assert (await client.post("/v1/systemone", json=payload())).status_code == 529
                finally:
                    await release_inferences(app.state.worker, engine)
                health = await client.get("/health")
                assert health.status_code == 200
                assert health.json()["active_requests"] == 0
                assert (await client.post("/v1/systemone", json=payload())).status_code == 200
    asyncio.run(scenario())


def test_caller_cancellation_cannot_release_running_native_inference():
    async def scenario():
        engine = Blocking()
        worker = InferenceWorker(engine, timeout=3)
        task = asyncio.create_task(worker.run(payload()))
        try:
            assert await asyncio.to_thread(engine.started.wait, 2)
            task.cancel()
            with pytest.raises(asyncio.CancelledError):
                await task
            assert worker.busy
            with pytest.raises(Exception) as error:
                await worker.run(payload())
            assert error.value.status_code == 529
        finally:
            pending = list(worker.pending)
            engine.release.set()
            for future in pending:
                await asyncio.to_thread(future.result, 2)
            worker.close()
    asyncio.run(scenario())


def test_execution_watchdog_survives_caller_cancellation_and_recovers_on_completion():
    async def scenario():
        engine = Blocking()
        worker = InferenceWorker(engine, timeout=0.02, inference_timeout=0.05)
        task = asyncio.create_task(worker.run(payload()))
        try:
            assert await asyncio.to_thread(engine.started.wait, 2)
            task.cancel()
            with pytest.raises(asyncio.CancelledError):
                await task
            await asyncio.sleep(0.08)
            assert worker.busy and worker.unhealthy
        finally:
            await release_inferences(worker, engine)
            assert not worker.busy and not worker.unhealthy
            assert await worker.run(payload())
            worker.close()
    asyncio.run(scenario())


def test_two_inferences_really_overlap_and_only_excess_request_is_rejected():
    async def scenario():
        engines = [Blocking(), Blocking()]
        available = iter(engines)
        app = create_app(settings=Settings(workers=2, timeout=3),
                         engine_factory=lambda _: next(available))
        async with app.router.lifespan_context(app):
            async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app),
                                         base_url="http://test") as client:
                first_body, second_body = payload(), payload()
                second_body["state"] = {"question": "A completely different request"}
                tasks = [asyncio.create_task(client.post("/v1/systemone", json=body))
                         for body in (first_body, second_body)]
                try:
                    # Neither inference is released until BOTH have entered predict().
                    # A serial implementation cannot pass this check.
                    for engine in engines:
                        assert await asyncio.to_thread(engine.started.wait, 1)
                    health = await asyncio.wait_for(client.get("/health"), 0.5)
                    assert health.json()["active_requests"] == 2
                    assert health.json()["workers"] == 2
                    excess = await asyncio.wait_for(client.post("/v1/systemone", json=payload()), 0.5)
                    assert excess.status_code == 529
                    assert excess.headers["retry-after"] == "3"
                    engines[0].release.set()
                    assert (await tasks[0]).status_code == 200
                    # The freed slot can serve another request while the second is still running.
                    assert (await client.post("/v1/systemone", json=first_body)).status_code == 200
                    assert not tasks[1].done()
                finally:
                    for engine in engines:
                        engine.release.set()
                    responses = await asyncio.gather(*tasks)
                    assert all(response.status_code == 200 for response in responses)
                assert engines[0].calls == [first_body, first_body]
                assert engines[1].calls == [second_body]
        assert all(engine.closed for engine in engines)
    asyncio.run(scenario())


def test_cancelled_slot_stays_reserved_while_another_slot_completes():
    async def scenario():
        blocked, quick = Blocking(), Engine()
        worker = InferenceWorker([blocked, quick], timeout=0.03, inference_timeout=0.3)
        task = asyncio.create_task(worker.run(payload()))
        try:
            assert await asyncio.to_thread(blocked.started.wait, 1)
            task.cancel()
            with pytest.raises(asyncio.CancelledError):
                await task
            assert await worker.run(payload())
            assert worker.active_count == 1
            # A cancelled request may outlive its HTTP deadline without poisoning the pool.
            await asyncio.sleep(0.06)
            assert not worker.unhealthy and worker.busy
            assert await worker.run(payload())
            # Completing quick requests must not cancel the blocked request's execution watchdog.
            await asyncio.sleep(0.3)
            assert worker.unhealthy and worker.busy
            with pytest.raises(Exception) as error:
                await worker.run(payload())
            assert error.value.status_code == 529
        finally:
            await release_inferences(worker, blocked)
            worker.close()
    asyncio.run(scenario())


def test_request_timeout_does_not_disable_other_slots():
    async def scenario():
        blocked, quick = Blocking(), Engine()
        worker = InferenceWorker([blocked, quick], timeout=0.05)
        try:
            with pytest.raises(Exception) as error:
                await worker.run(payload())
            assert error.value.status_code == 529
            assert worker.active_count == 1
            assert not worker.unhealthy
            assert await worker.run(payload())
            assert len(quick.calls) == 1
            assert not blocked.calls
        finally:
            await release_inferences(worker, blocked)
            worker.close()
    asyncio.run(scenario())


def test_execution_deadline_requests_restart_only_while_inference_is_stuck():
    async def scenario():
        engines = [Blocking(), Blocking()]
        available = iter(engines)
        app = create_app(settings=Settings(workers=2, timeout=0.02, inference_timeout=0.1),
                         engine_factory=lambda _: next(available))
        async with app.router.lifespan_context(app):
            async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app),
                                         base_url="http://test") as client:
                try:
                    responses = await asyncio.gather(*(
                        client.post("/v1/systemone", json=payload()) for _ in engines))
                    assert all(response.status_code == 529 for response in responses)
                    await asyncio.sleep(0.15)
                    health = await client.get("/health")
                    assert health.status_code == 503
                    assert health.json()["restart_required"]
                    assert health.json()["stalled_requests"] == 2
                    assert health.json()["inference_timeout_seconds"] == 0.1
                    # Finishing one stalled inference must not hide the other one.
                    first = next(future for future, slot in app.state.worker.pending.items() if slot == 0)
                    engines[0].release.set()
                    await asyncio.wrap_future(first)
                    await asyncio.sleep(0)
                    assert (await client.get("/health")).status_code == 503
                    assert (await client.post("/v1/systemone", json=payload())).status_code == 529
                finally:
                    await release_inferences(app.state.worker, *engines)
                health = await client.get("/health")
                assert health.status_code == 200
                assert health.json()["active_requests"] == 0
                assert health.json()["stalled_requests"] == 0
                assert (await client.post("/v1/systemone", json=payload())).status_code == 200
    asyncio.run(scenario())


@pytest.mark.parametrize("cancel_caller", [False, True])
def test_memory_failure_after_http_timeout_or_cancellation_still_requires_restart(cancel_caller):
    class OutOfMemory(Blocking):
        def predict(self, body):
            super().predict(body)
            raise MemoryError("private input")

    async def scenario():
        engine = OutOfMemory()
        app = create_app(engine, Settings(timeout=0.05))
        async with app.router.lifespan_context(app):
            async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app),
                                         base_url="http://test") as client:
                task = asyncio.create_task(client.post("/v1/systemone", json=payload()))
                try:
                    assert await asyncio.to_thread(engine.started.wait, 1)
                    if cancel_caller:
                        task.cancel()
                        with pytest.raises(asyncio.CancelledError):
                            await task
                    else:
                        assert (await task).status_code == 529
                finally:
                    await release_inferences(app.state.worker, engine)
                health = await client.get("/health")
                assert health.status_code == 503
                assert health.json()["active_requests"] == 0
                assert health.json()["restart_required"]
                assert (await client.post("/v1/systemone", json=payload())).status_code == 529
    asyncio.run(scenario())


def test_partial_startup_failure_unloads_all_replicas():
    class Broken(Engine):
        def load(self):
            raise RuntimeError("load failed")
    engines = [Engine(), Broken()]
    available = iter(engines)
    with pytest.raises(RuntimeError, match="load failed"):
        with TestClient(create_app(settings=Settings(workers=2),
                                  engine_factory=lambda _: next(available))):
            pass
    assert all(engine.closed for engine in engines)
