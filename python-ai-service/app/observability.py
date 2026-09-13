from __future__ import annotations

import logging
import math
import threading
import time
import uuid
from collections import Counter, defaultdict, deque
from typing import Any

from fastapi import Request
from starlette.responses import Response

logger = logging.getLogger("ticket_copilot.python_ai_service")


class RequestMetrics:
    """Small in-process request metrics registry for the local portfolio sidecar.

    The registry intentionally avoids an external metrics dependency. It keeps a
    bounded latency window so the service can expose local p50/p95/p99 evidence
    without pretending to be a distributed production telemetry backend.
    """

    def __init__(self, latency_window: int = 2000) -> None:
        self._lock = threading.Lock()
        self._latency_window = latency_window
        self._in_flight = 0
        self._requests: Counter[tuple[str, str, int]] = Counter()
        self._latencies_ms: dict[tuple[str, str], deque[float]] = defaultdict(
            lambda: deque(maxlen=self._latency_window)
        )

    def request_started(self) -> None:
        with self._lock:
            self._in_flight += 1

    def request_finished(self, method: str, path: str, status: int, duration_ms: float) -> None:
        with self._lock:
            self._in_flight = max(0, self._in_flight - 1)
            if path == "/internal/metrics":
                return
            self._requests[(method, path, status)] += 1
            self._latencies_ms[(method, path)].append(duration_ms)

    @staticmethod
    def _percentile(values: list[float], percentile: float) -> float:
        if not values:
            return 0.0
        ordered = sorted(values)
        index = max(0, min(len(ordered) - 1, math.ceil(percentile * len(ordered)) - 1))
        return ordered[index]

    def snapshot(self) -> dict[str, Any]:
        with self._lock:
            requests = dict(self._requests)
            latencies = {key: list(values) for key, values in self._latencies_ms.items()}
            in_flight = self._in_flight

        total_requests = sum(requests.values())
        total_errors = sum(count for (_, _, status), count in requests.items() if status >= 500)
        routes: list[dict[str, Any]] = []
        route_keys = sorted({(method, path) for method, path, _ in requests} | set(latencies))
        for method, path in route_keys:
            route_latencies = latencies.get((method, path), [])
            route_requests = sum(
                count
                for (record_method, record_path, _), count in requests.items()
                if record_method == method and record_path == path
            )
            route_errors = sum(
                count
                for (record_method, record_path, status), count in requests.items()
                if record_method == method and record_path == path and status >= 500
            )
            routes.append(
                {
                    "method": method,
                    "path": path,
                    "requests": route_requests,
                    "errors": route_errors,
                    "latencyWindow": len(route_latencies),
                    "latencyMs": {
                        "p50": round(self._percentile(route_latencies, 0.50), 4),
                        "p95": round(self._percentile(route_latencies, 0.95), 4),
                        "p99": round(self._percentile(route_latencies, 0.99), 4),
                        "max": round(max(route_latencies), 4) if route_latencies else 0.0,
                    },
                }
            )

        return {
            "totalRequests": total_requests,
            "totalErrors": total_errors,
            "inFlight": in_flight,
            "latencyWindowSize": self._latency_window,
            "routes": routes,
        }

    def prometheus_text(self) -> str:
        snapshot = self.snapshot()
        lines = [
            "# HELP ticket_ai_http_requests_total HTTP requests observed by the local Python AI sidecar.",
            "# TYPE ticket_ai_http_requests_total counter",
        ]
        with self._lock:
            request_items = sorted(self._requests.items())
        for (method, path, status), count in request_items:
            lines.append(
                'ticket_ai_http_requests_total{method="%s",path="%s",status="%s"} %d'
                % (method, path, status, count)
            )
        lines.extend(
            [
                "# HELP ticket_ai_http_requests_in_flight Current in-flight requests in this process.",
                "# TYPE ticket_ai_http_requests_in_flight gauge",
                f"ticket_ai_http_requests_in_flight {snapshot['inFlight']}",
                "# HELP ticket_ai_http_latency_milliseconds Local bounded-window request latency quantiles.",
                "# TYPE ticket_ai_http_latency_milliseconds gauge",
            ]
        )
        for route in snapshot["routes"]:
            for quantile in ("p50", "p95", "p99", "max"):
                lines.append(
                    'ticket_ai_http_latency_milliseconds{method="%s",path="%s",quantile="%s"} %s'
                    % (route["method"], route["path"], quantile, route["latencyMs"][quantile])
                )
        return "\n".join(lines) + "\n"


request_metrics = RequestMetrics()


async def request_observability_middleware(request: Request, call_next) -> Response:
    request_id = uuid.uuid4().hex
    started = time.perf_counter()
    request_metrics.request_started()
    status = 500
    try:
        response = await call_next(request)
        status = response.status_code
        response.headers["X-Request-ID"] = request_id
        return response
    finally:
        duration_ms = (time.perf_counter() - started) * 1000
        request_metrics.request_finished(request.method, request.url.path, status, duration_ms)
        logger.info(
            "request_complete request_id=%s method=%s path=%s status=%s duration_ms=%.3f",
            request_id,
            request.method,
            request.url.path,
            status,
            duration_ms,
        )
