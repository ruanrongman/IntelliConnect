"""Bound model concurrency by usable CPU cores and available host memory."""
from dataclasses import dataclass
import math
import os

import psutil

GIB = 1024 ** 3


@dataclass(frozen=True)
class Resources:
    physical_cores: int
    logical_cpus: int
    available_cpus: int
    available_memory: int

    @classmethod
    def detect(cls):
        logical = psutil.cpu_count() or os.cpu_count() or 1
        physical = psutil.cpu_count(logical=False) or logical
        try:
            available = len(psutil.Process().cpu_affinity())
        except (AttributeError, NotImplementedError, psutil.Error):
            available = logical
        return cls(physical, logical, max(1, available), psutil.virtual_memory().available)


def allocate(resources: Resources, workers=None, threads=None, device="cpu"):
    cores = min(resources.physical_cores, resources.available_cpus)
    # Leave 25% of usable physical cores and 2 GiB RAM for Java/the desktop.
    budget = max(1, cores - math.ceil(cores / 4))
    memory_slots = max(0, (resources.available_memory - 2 * GIB) // (2 * GIB))
    if memory_slots < 1:
        raise ValueError("Laya needs at least 4 GiB available RAM for automatic model allocation")
    if workers is not None and workers < 1 or threads is not None and threads < 1:
        raise ValueError("LAYA_WORKERS and LAYA_THREADS must be positive")
    # Host core count cannot determine VRAM capacity; GPU replicas require an explicit override.
    default_workers = min(4, max(1, budget // (threads or 2)), memory_slots)
    workers = workers if workers is not None else (default_workers if device == "cpu" else 1)
    threads = threads if threads is not None else min(4, max(1, budget // workers))
    if workers * threads > budget:
        raise ValueError(f"LAYA_WORKERS * LAYA_THREADS exceeds the CPU budget ({budget})")
    if workers > memory_slots:
        raise ValueError("LAYA_WORKERS exceeds available RAM budget (2 GiB per model + 2 GiB reserve)")
    return workers, threads, budget
