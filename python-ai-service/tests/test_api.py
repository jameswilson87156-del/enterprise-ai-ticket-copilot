from __future__ import annotations

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_reports_local_artifacts() -> None:
    response = client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["service"] == "ticket-copilot-python-ai-service"
    assert body["metricsAvailable"] is True
    assert body["datasetAvailable"] is True


def test_readiness_validates_reproducible_artifacts() -> None:
    response = client.get("/ready")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ready"
    assert body["datasetCaseCount"] == 16
    assert response.headers["x-request-id"]


def test_retrieval_preview_returns_expected_java_port_evidence() -> None:
    response = client.post(
        "/api/retrieval/preview",
        json={
            "question": "Java 服务启动失败，8080 端口被占用，BindException 怎么排查？",
            "category": "系统故障",
            "topK": 3,
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["mode"] == "keyword-retrieval-demo"
    assert body["hitCount"] >= 1
    assert body["hits"][0]["knowledgeId"] == "KB-JAVA-PORT"
    assert body["hits"][0]["score"] >= 35
    assert "No embedding" in body["boundary"]


def test_retrieval_v2_keeps_java_port_precise() -> None:
    response = client.post(
        "/api/retrieval/preview",
        json={
            "question": "Java 服务启动失败，8080 端口被占用，BindException 怎么排查？",
            "category": "系统故障",
            "topK": 3,
            "retrievalMode": "v2",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["mode"] == "retrieval-v2-demo"
    assert [hit["knowledgeId"] for hit in body["hits"]] == ["KB-JAVA-PORT"]
    assert "field-aware IDF" in body["boundary"]


def test_retrieval_v2_abstains_for_missing_device_knowledge() -> None:
    response = client.post(
        "/api/retrieval/preview",
        json={
            "question": "会议室投影仪遥控器失灵，应该联系谁处理？",
            "category": "流程咨询",
            "topK": 3,
            "retrievalMode": "v2",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["mode"] == "retrieval-v2-demo"
    assert body["hitCount"] == 0
    assert body["hits"] == []


def test_metrics_expose_request_counts_and_latency() -> None:
    metrics = client.get("/internal/metrics")
    assert metrics.status_code == 200
    body = metrics.json()
    assert body["totalRequests"] >= 1
    assert any(route["path"] == "/api/retrieval/preview" for route in body["routes"])

    prometheus = client.get("/metrics")
    assert prometheus.status_code == 200
    assert "ticket_ai_http_requests_total" in prometheus.text
    assert "ticket_ai_http_latency_milliseconds" in prometheus.text


def test_retrieval_preview_validates_top_k() -> None:
    response = client.post(
        "/api/retrieval/preview",
        json={"question": "Redis timeout", "topK": 10},
    )

    assert response.status_code == 422


def test_evaluation_summary_reads_reproducible_snapshot() -> None:
    response = client.get("/api/evaluation/summary")

    assert response.status_code == 200
    body = response.json()
    assert body["sampleCount"] == 16
    assert body["topK"] == 3
    assert body["metrics"]["topKHitRate"] == 100.0
    assert body["metrics"]["citationPrecision"] == 81.11
    assert body["metrics"]["failedCaseCount"] == 6
