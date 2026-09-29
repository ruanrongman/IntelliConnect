"""Download only the multilingual checkpoint files used by the adapter."""
import os
from pathlib import Path
from verify_model import verify, REVISION

ROOT = Path(__file__).resolve().parent
os.environ.setdefault("HF_HOME", str(ROOT / ".runtime" / "huggingface"))

if __name__ == "__main__":
    from huggingface_hub import snapshot_download
    model_dir = ROOT / ".runtime" / "models" / "multilingual"
    snapshot_download(
        "convaiinnovations/laya-multilingual",
        revision=REVISION,
        local_dir=str(model_dir),
        token=False,
        allow_patterns=["rl_agent_config.json", "model.safetensors", "tokenizer/*", "encoder/*"],
        max_workers=4,
    )
    verify(model_dir)
    print(model_dir)
