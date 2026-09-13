# Python AI Sidecar Local Load Test

- Generated: `2026-09-13T23:14:44+08:00`
- Requests: **1000**
- Concurrency: **20**
- Warmup: **20**
- Success: **1000**
- Errors: **0**
- Success rate: **100.0%**
- Throughput: **545.37 req/s**
- Latency p50 / p95 / p99 / max: **33.9436 / 51.3784 / 57.9002 / 65.458 ms**

## Correctness gate

Every successful request must return `KB-JAVA-PORT` as Top-1 evidence for the fixed Java port-conflict scenario. A 200 response with another Top-1 result is counted as an error.

## Boundary

Local Windows host, localhost HTTP, single Uvicorn process, synthetic keyword retrieval. This benchmark is reproducible portfolio evidence, not a production SLA or cloud capacity claim.
