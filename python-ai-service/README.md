# Python AI Service

这是 Enterprise Ticket RAG Copilot 的可选 Python AI 应用 sidecar，用于展示真实可运行的 Python 3.12 + FastAPI + Pydantic 工程能力。

它复用仓库现有 `scripts/evaluate_rag_demo.py` 的 keyword retrieval baseline 和 `docs/metrics/rag_metrics_latest.json` 的可复现评测快照，不改变 Java/Spring Boot 主业务链路。

## 能力

- `GET /health`：进程级健康检查。
- `GET /ready`：校验评测快照与 16 条 synthetic dataset 是否可读。
- `POST /api/retrieval/preview`：输入工单问题、可选分类和 Top-K，返回 keyword retrieval 证据。
- `GET /api/evaluation/summary`：返回当前 synthetic demo 评测摘要。
- `GET /internal/metrics`：查看进程内请求计数、错误数、in-flight 和 bounded-window p50/p95/p99/max 延迟。
- `GET /metrics`：暴露 Prometheus text format 的本地指标出口，不依赖额外 prometheus client 包。
- 每个 HTTP 响应返回 `X-Request-ID`，服务日志记录 method/path/status/duration。
- Pydantic 请求/响应契约和 FastAPI 自动 OpenAPI 文档。
- pytest + FastAPI TestClient 覆盖健康/就绪、检索、参数校验、评测摘要和 metrics。

## 边界

- 不调用真实 LLM Provider。
- 不读取 API Key。
- 不连接 MySQL。
- 不使用 embedding、Vector DB、Hybrid Retrieval 或 Rerank。
- 评测指标只代表 synthetic demo dataset + local keyword retrieval baseline。

## 本地运行

```powershell
cd python-ai-service
py -3 -m uvicorn app.main:app --reload --port 18083
```

打开 `http://127.0.0.1:18083/docs` 查看 FastAPI OpenAPI 页面。

## 测试

```powershell
cd python-ai-service
py -3 -m pytest -q
```

## 本地并发压测

```powershell
cd python-ai-service
py -3 -B scripts/load_test.py --base-url http://127.0.0.1:18083 --requests 1000 --concurrency 20 --warmup 20
```

2026-09-13 本机实测：1000 请求、并发 20、1000 成功 / 0 错误，约 `545.37 req/s`；端到端 HTTP 延迟 p50 `33.94 ms`、p95 `51.38 ms`、p99 `57.90 ms`、max `65.46 ms`。每个成功请求还必须保证固定 Java 端口冲突样例的 Top-1 为 `KB-JAVA-PORT`，否则即使 HTTP 200 也计为压测错误。

证据：`docs/performance/python_sidecar_load_latest.json` 与 `docs/performance/python_sidecar_load_latest.md`。

以上结果仅代表本地 Windows、localhost、单 Uvicorn 进程和 synthetic keyword retrieval，不是生产 SLA、云容量或真实模型推理性能。
