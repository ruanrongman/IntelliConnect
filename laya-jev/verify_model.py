"""Verify the large artifacts from a fixed public checkpoint revision."""
import hashlib
import json
from pathlib import Path

REVISION = "052592a15d198d9ad47da779604259b10b47b7aa"
HASHES = {
    "model.safetensors": "9d628fd971b700382ac6f65920a86f149777b2e748e0c955fb3b19695aa8f204",
    "tokenizer/tokenizer.json": "609d8f4c067cd3950f88594c5a802616cea245823836ef5848ee4fc40aab5b6f",
}


def verify(directory: Path):
    for name, expected in HASHES.items():
        with (directory / name).open("rb") as file:
            actual = hashlib.file_digest(file, "sha256").hexdigest()
        if actual != expected:
            raise RuntimeError(f"SHA-256 mismatch: {name}")
    for name in ("rl_agent_config.json", "encoder/config.json", "tokenizer/tokenizer_config.json"):
        json.loads((directory / name).read_text(encoding="utf-8"))
    (directory / "verified.json").write_text(json.dumps({"revision": REVISION, "sha256": HASHES},
                                                       indent=2), encoding="utf-8")
    print("Multilingual checkpoint hashes verified")


if __name__ == "__main__":
    verify(Path(__file__).resolve().parent / ".runtime" / "models" / "multilingual")
