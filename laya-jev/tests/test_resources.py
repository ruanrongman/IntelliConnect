import pytest

from app import Settings
from resources import GIB, Resources, allocate


@pytest.mark.parametrize("cores,logical,expected", [
    (1, 1, (1, 1, 1)), (2, 4, (1, 1, 1)), (4, 8, (1, 3, 3)),
    (8, 16, (3, 2, 6)), (16, 32, (4, 3, 12)), (32, 64, (4, 4, 24)),
])
def test_allocation_varies_with_detected_core_count(cores, logical, expected):
    assert allocate(Resources(cores, logical, logical, 32 * GIB)) == expected


def test_affinity_and_memory_limit_model_replicas():
    assert allocate(Resources(16, 32, 4, 32 * GIB)) == (1, 3, 3)
    assert allocate(Resources(8, 16, 16, 6 * GIB)) == (2, 3, 6)
    with pytest.raises(ValueError, match="RAM"):
        allocate(Resources(8, 16, 16, 3 * GIB))


@pytest.mark.parametrize("workers,threads,expected", [
    (2, None, (2, 3, 6)), (None, 2, (3, 2, 6)), (None, 4, (1, 4, 6)),
    (2, 2, (2, 2, 6)), (6, 1, (6, 1, 6)),
])
def test_manual_settings_override_auto_allocation(workers, threads, expected):
    assert allocate(Resources(8, 16, 16, 32 * GIB), workers, threads) == expected


@pytest.mark.parametrize("workers,threads", [(0, None), (None, 0), (4, 2), (7, 1)])
def test_invalid_or_oversubscribed_settings_fail_at_startup(workers, threads):
    with pytest.raises(ValueError):
        allocate(Resources(8, 16, 16, 32 * GIB), workers, threads)


def test_environment_settings_read_hardware_each_time(monkeypatch):
    monkeypatch.delenv("LAYA_WORKERS", raising=False)
    monkeypatch.delenv("LAYA_THREADS", raising=False)
    monkeypatch.setenv("LAYA_DEVICE", "cpu")
    monkeypatch.setattr(Resources, "detect", lambda: Resources(8, 16, 16, 32 * GIB))
    assert (Settings.from_env().workers, Settings.from_env().threads) == (3, 2)
    monkeypatch.setattr(Resources, "detect", lambda: Resources(16, 32, 32, 32 * GIB))
    assert (Settings.from_env().workers, Settings.from_env().threads) == (4, 3)
    monkeypatch.setenv("LAYA_WORKERS", "2")
    monkeypatch.setenv("LAYA_THREADS", "4")
    assert (Settings.from_env().workers, Settings.from_env().threads) == (2, 4)


@pytest.mark.parametrize("request_timeout,inference_timeout,expected", [
    ("8", None, 60), ("90", None, 90), ("8", "120", 120),
])
def test_execution_timeout_is_separate_from_request_wait(monkeypatch, request_timeout,
                                                       inference_timeout, expected):
    monkeypatch.setattr(Resources, "detect", lambda: Resources(8, 16, 16, 32 * GIB))
    monkeypatch.setenv("LAYA_REQUEST_TIMEOUT_SECONDS", request_timeout)
    if inference_timeout is None:
        monkeypatch.delenv("LAYA_INFERENCE_TIMEOUT_SECONDS", raising=False)
    else:
        monkeypatch.setenv("LAYA_INFERENCE_TIMEOUT_SECONDS", inference_timeout)
    assert Settings.from_env().inference_timeout == expected


@pytest.mark.parametrize("inference_timeout", ["0", "-1", "7", "nan", "inf"])
def test_invalid_execution_timeout_fails_at_startup(monkeypatch, inference_timeout):
    monkeypatch.setattr(Resources, "detect", lambda: Resources(8, 16, 16, 32 * GIB))
    monkeypatch.setenv("LAYA_REQUEST_TIMEOUT_SECONDS", "8")
    monkeypatch.setenv("LAYA_INFERENCE_TIMEOUT_SECONDS", inference_timeout)
    with pytest.raises(ValueError, match="LAYA_INFERENCE_TIMEOUT_SECONDS"):
        Settings.from_env()
