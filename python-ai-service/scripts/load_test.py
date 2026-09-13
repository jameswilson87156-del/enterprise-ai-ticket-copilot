from __future__ import annotations

import argparse
import json
import math
import platform
import statistics
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_JSON = REPO_ROOT / "docs" / "performance" / "python_sidecar_load_latest.json"
DEFAULT_MD = REPO_ROOT / "docs" / "performance" / "python_sidecar_load_latest.md"
BOUNDARY = (
    "Local Windows host, localhost HTTP, single Uvicorn process, synthetic keyword retrieval. "
    "This benchmark is reproducible portfolio evidence, not a production SLA or cloud capacity claim."
)
PAYLOAD = json.dumps(
    {
        "question": "Java 服务启动失败，8080 端口被占用，BindException 怎么排查？",
        "category": "系统故障",
        "topK": 3,
    },
    ensure_ascii=False,
).encode("utf-8")


def percentile(values: list[float], value: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    index = max(0, min(len(ordered) - 1, math.ceil(value * len(ordered)) - 1))
    return ordered[index]


def request_once(url: str, timeout: float) -> dict[str, Any]:
    started = time.perf_counter()
    status = 0
    correct = False
    error: str | None = None
    try:
        request = urllib.request.Request(
            url,
            data=PAYLOAD,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=timeout) as response:
            status = response.status
            body = json.loads(response.read().decode("utf-8"))
            hits = body.get("hits") or []
            correct = bool(hits and hits[0].get("knowledgeId") == "KB-JAVA-PORT")
    except urllib.error.HTTPError as exc:
        status = exc.code
        error = f"HTTP {exc.code}"
    except Exception as exc:  # noqa: BLE001 - benchmark must record transport failures.
        error = f"{type(exc).__name__}: {exc}"
    duration_ms = (time.perf_counter() - started) * 1000
    return {"status": status, "correct": correct, "durationMs": duration_ms, "error": error}


def run_load(base_url: str, requests_count: int, concurrency: int, warmup: int, timeout: float) -> dict[str, Any]:
    endpoint = base_url.rstrip("/") + "/api/retrieval/preview"
    for _ in range(warmup):
        request_once(endpoint, timeout)

    started = time.perf_counter()
    results: list[dict[str, Any]] = []
    with ThreadPoolExecutor(max_workers=concurrency) as pool:
        futures = [pool.submit(request_once, endpoint, timeout) for _ in range(requests_count)]
        for future in as_completed(futures):
            results.append(future.result())
    elapsed = time.perf_counter() - started

    successes = [item for item in results if item["status"] == 200 and item["correct"]]
    errors = [item for item in results if item not in successes]
    latencies = [float(item["durationMs"]) for item in successes]
    error_samples = [item["error"] or f"status={item['status']} correct={item['correct']}" for item in errors[:10]]

    return {
        "generatedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
        "environment": {
            "python": platform.python_version(),
            "platform": platform.platform(),
            "baseUrl": base_url,
            "processModel": "single uvicorn process",
        },
        "scenario": {
            "endpoint": "/api/retrieval/preview",
            "requests": requests_count,
            "concurrency": concurrency,
            "warmupRequests": warmup,
            "timeoutSeconds": timeout,
            "expectedTop1": "KB-JAVA-PORT",
        },
        "result": {
            "successes": len(successes),
            "errors": len(errors),
            "successRate": round((len(successes) / requests_count) * 100, 4) if requests_count else 0.0,
            "durationSeconds": round(elapsed, 4),
            "throughputRps": round(len(successes) / elapsed, 2) if elapsed else 0.0,
            "latencyMs": {
                "min": round(min(latencies), 4) if latencies else 0.0,
                "avg": round(statistics.fmean(latencies), 4) if latencies else 0.0,
                "p50": round(percentile(latencies, 0.50), 4),
                "p95": round(percentile(latencies, 0.95), 4),
                "p99": round(percentile(latencies, 0.99), 4),
                "max": round(max(latencies), 4) if latencies else 0.0,
            },
            "errorSamples": error_samples,
        },
        "boundary": BOUNDARY,
    }


def render_markdown(report: dict[str, Any]) -> str:
    scenario = report["scenario"]
    result = report["result"]
    latency = result["latencyMs"]
    return f"""# Python AI Sidecar Local Load Test\n\n- Generated: `{report['generatedAt']}`\n- Requests: **{scenario['requests']}**\n- Concurrency: **{scenario['concurrency']}**\n- Warmup: **{scenario['warmupRequests']}**\n- Success: **{result['successes']}**\n- Errors: **{result['errors']}**\n- Success rate: **{result['successRate']}%**\n- Throughput: **{result['throughputRps']} req/s**\n- Latency p50 / p95 / p99 / max: **{latency['p50']} / {latency['p95']} / {latency['p99']} / {latency['max']} ms**\n\n## Correctness gate\n\nEvery successful request must return `KB-JAVA-PORT` as Top-1 evidence for the fixed Java port-conflict scenario. A 200 response with another Top-1 result is counted as an error.\n\n## Boundary\n\n{report['boundary']}\n"""


def main() -> int:
    parser = argparse.ArgumentParser(description="Concurrent local load test for the Python AI sidecar.")
    parser.add_argument("--base-url", default="http://127.0.0.1:18083")
    parser.add_argument("--requests", type=int, default=1000)
    parser.add_argument("--concurrency", type=int, default=20)
    parser.add_argument("--warmup", type=int, default=20)
    parser.add_argument("--timeout", type=float, default=5.0)
    parser.add_argument("--json", type=Path, default=DEFAULT_JSON)
    parser.add_argument("--markdown", type=Path, default=DEFAULT_MD)
    args = parser.parse_args()

    if args.requests < 1 or args.concurrency < 1 or args.warmup < 0:
        parser.error("requests/concurrency must be positive and warmup must be non-negative")

    report = run_load(args.base_url, args.requests, args.concurrency, args.warmup, args.timeout)
    args.json.parent.mkdir(parents=True, exist_ok=True)
    args.markdown.parent.mkdir(parents=True, exist_ok=True)
    args.json.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    args.markdown.write_text(render_markdown(report), encoding="utf-8")
    print(json.dumps(report["result"], ensure_ascii=False))
    return 0 if report["result"]["errors"] == 0 else 2


if __name__ == "__main__":
    raise SystemExit(main())
