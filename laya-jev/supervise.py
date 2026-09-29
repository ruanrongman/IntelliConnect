"""Restart the isolated API/model process after crashes or stalled inference."""
from __future__ import annotations

import json
import os
from pathlib import Path
import subprocess
import sys
import time
from urllib.error import HTTPError, URLError
from urllib.request import build_opener, ProxyHandler

ROOT = Path(__file__).resolve().parent
RUNTIME = ROOT / ".runtime"


def main():
    RUNTIME.mkdir(exist_ok=True)
    stop = RUNTIME / "stop"
    stop.unlink(missing_ok=True)
    (RUNTIME / "supervisor.pid").write_text(str(os.getpid()))
    host = os.getenv("LAYA_HOST", "127.0.0.1")
    host = "127.0.0.1" if host == "0.0.0.0" else host
    health = f"http://{host}:{int(os.getenv('LAYA_PORT', '8001'))}/health"
    startup_timeout = float(os.getenv("LAYA_STARTUP_TIMEOUT_SECONDS", "300"))
    # Local health checks must not use the machine's external HTTP proxy.
    opener = build_opener(ProxyHandler({}))
    creationflags = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
    with (RUNTIME / "service.log").open("a", encoding="utf-8") as output:
        while not stop.exists():
            process = subprocess.Popen([sys.executable, "-u", str(ROOT / "app.py")], cwd=ROOT,
                                       stdout=output, stderr=output, creationflags=creationflags)
            (RUNTIME / "server.pid").write_text(str(process.pid))
            started = time.monotonic()
            ready = False
            failures = 0
            try:
                while process.poll() is None and not stop.exists():
                    time.sleep(2)
                    try:
                        with opener.open(health, timeout=2) as response:
                            status = json.load(response)
                        if status.get("restart_required"):
                            break
                        ready, failures = True, 0
                    except HTTPError as exc:
                        if exc.code == 503:
                            break
                        failures += 1
                    except (URLError, TimeoutError, OSError, ValueError):
                        failures += 1
                    if ready and failures >= 5:
                        break
                    if not ready and time.monotonic() - started > startup_timeout:
                        break
            finally:
                if process.poll() is None:
                    if os.name == "nt":
                        # A Windows venv python.exe is a launcher with a real Python child.
                        # terminate() alone leaves that child holding the port and model memory.
                        subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"],
                                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                                       timeout=10, creationflags=creationflags, check=False)
                    else:
                        process.terminate()
                    try:
                        process.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        process.kill()
                        process.wait(timeout=5)
                (RUNTIME / "server.pid").unlink(missing_ok=True)
            if not stop.exists():
                print("Restarting Laya service after exit or failed health check", flush=True)
                time.sleep(3)
    (RUNTIME / "supervisor.pid").unlink(missing_ok=True)


if __name__ == "__main__":
    main()
