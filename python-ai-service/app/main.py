from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Literal

from fastapi import FastAPI, HTTPException, Response
from pydantic import BaseModel, Field

REPO_ROOT = Path(__file__).resolve().parents[2]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from scripts.evaluate_rag_demo import DEMO_BOUNDARY, retrieve
from scripts.evaluate_retrieval_v2 import RETRIEVAL_V2_BOUNDARY, retrieve_v2
from app.observability import request_metrics, request_observability_middleware

METRICS_PATH = REPO_ROOT / "docs" / "metrics" / "rag_metrics_latest.json"
DATASET_PATH = REPO_ROOT / "data" / "eval" / "ticket_rag_eval_cases.jsonl"

app = FastAPI(
    title="Enterprise Ticket Copilot Python AI Service",
    version="0.1.0",
    description=(
        "Portfolio-side Python service for bounded keyword retrieval and reproducible "
        "RAG evaluation evidence. It does not call a real LLM or Vector DB."
    ),
)

app.middleware("http")(request_observability_middleware)


class HealthResponse(BaseModel):
    status: Literal["ok"]
    service: str
    metrics_available: bool = Field(alias="metricsAvailable")
    dataset_available: bool = Field(alias="datasetAvailable")

    model_config = {"populate_by_name": True}


class ReadinessResponse(BaseModel):
    status: Literal["ready"]
    metrics_available: bool = Field(alias="metricsAvailable")
    dataset_available: bool = Field(alias="datasetAvailable")
    dataset_case_count: int = Field(alias="datasetCaseCount")

    model_config = {"populate_by_name": True}


class RetrievalPreviewRequest(BaseModel):
    question: str = Field(min_length=3, max_length=2000)
    category: str | None = Field(default=None, max_length=100)
    top_k: int = Field(default=3, alias="topK", ge=1, le=5)
    retrieval_mode: Literal["keyword", "v2"] = Field(default="keyword", alias="retrievalMode")

    model_config = {"populate_by_name": True}


class RetrievalHit(BaseModel):
    knowledge_id: str = Field(alias="knowledgeId")
    title: str
    category: str
    score: float
    owner: str
    excerpt: str

    model_config = {"populate_by_name": True}


class RetrievalPreviewResponse(BaseModel):
    mode: Literal["keyword-retrieval-demo", "retrieval-v2-demo"]
    top_k: int = Field(alias="topK")
    hit_count: int = Field(alias="hitCount")
    latency_ms: float = Field(alias="latencyMs")
    hits: list[RetrievalHit]
    boundary: str

    model_config = {"populate_by_name": True}


class MetricSummary(BaseModel):
    top_k_hit_rate: float = Field(alias="topKHitRate")
    context_recall_at_k: float = Field(alias="contextRecallAtK")
    citation_coverage: float = Field(alias="citationCoverage")
    citation_precision: float = Field(alias="citationPrecision")
    failed_case_count: int = Field(alias="failedCaseCount")
    human_review_required_count: int = Field(alias="humanReviewRequiredCount")

    model_config = {"populate_by_name": True}


class EvaluationSummaryResponse(BaseModel):
    project: str
    generated_at: str = Field(alias="generatedAt")
    sample_count: int = Field(alias="sampleCount")
    top_k: int = Field(alias="topK")
    metrics: MetricSummary
    boundary: str

    model_config = {"populate_by_name": True}


def _load_metrics() -> dict:
    if not METRICS_PATH.exists():
        raise HTTPException(status_code=503, detail="evaluation metrics snapshot is unavailable")
    try:
        return json.loads(METRICS_PATH.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as exc:
        raise HTTPException(status_code=503, detail="evaluation metrics snapshot is invalid") from exc


def _dataset_case_count() -> int:
    if not DATASET_PATH.exists():
        raise HTTPException(status_code=503, detail="evaluation dataset is unavailable")
    try:
        lines = [line for line in DATASET_PATH.read_text(encoding="utf-8-sig").splitlines() if line.strip()]
        for line in lines:
            json.loads(line)
        return len(lines)
    except (OSError, json.JSONDecodeError) as exc:
        raise HTTPException(status_code=503, detail="evaluation dataset is invalid") from exc


@app.get("/health", response_model=HealthResponse, response_model_by_alias=True)
def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        service="ticket-copilot-python-ai-service",
        metricsAvailable=METRICS_PATH.exists(),
        datasetAvailable=DATASET_PATH.exists(),
    )


@app.get("/ready", response_model=ReadinessResponse, response_model_by_alias=True)
def ready() -> ReadinessResponse:
    _load_metrics()
    case_count = _dataset_case_count()
    return ReadinessResponse(
        status="ready",
        metricsAvailable=True,
        datasetAvailable=True,
        datasetCaseCount=case_count,
    )


@app.get("/internal/metrics")
def internal_metrics() -> dict:
    return request_metrics.snapshot()


@app.get("/metrics", response_class=Response)
def prometheus_metrics() -> Response:
    return Response(
        content=request_metrics.prometheus_text(),
        media_type="text/plain; version=0.0.4; charset=utf-8",
    )


@app.post(
    "/api/retrieval/preview",
    response_model=RetrievalPreviewResponse,
    response_model_by_alias=True,
)
def retrieval_preview(payload: RetrievalPreviewRequest) -> RetrievalPreviewResponse:
    case = {
        "user_question": payload.question,
        "category": payload.category,
        "ticket_context": {},
    }
    if payload.retrieval_mode == "v2":
        hits, latency_ms = retrieve_v2(case, payload.top_k)
        mode = "retrieval-v2-demo"
        boundary = RETRIEVAL_V2_BOUNDARY
    else:
        hits, latency_ms = retrieve(case, payload.top_k, "keyword")
        mode = "keyword-retrieval-demo"
        boundary = DEMO_BOUNDARY
    return RetrievalPreviewResponse(
        mode=mode,
        topK=payload.top_k,
        hitCount=len(hits),
        latencyMs=round(latency_ms, 4),
        hits=[
            RetrievalHit(
                knowledgeId=hit["id"],
                title=hit["title"],
                category=hit["category"],
                score=hit["score"],
                owner=hit["owner"],
                excerpt=hit["content"],
            )
            for hit in hits
        ],
        boundary=boundary,
    )


@app.get(
    "/api/evaluation/summary",
    response_model=EvaluationSummaryResponse,
    response_model_by_alias=True,
)
def evaluation_summary() -> EvaluationSummaryResponse:
    snapshot = _load_metrics()
    primary = snapshot.get("primary_metrics") or {}
    dataset = snapshot.get("dataset") or {}
    retrieval = snapshot.get("retrieval") or {}

    required = (
        "top_k_hit_rate",
        "context_recall_at_k",
        "citation_coverage",
        "citation_precision",
        "failed_case_count",
        "human_review_required_count",
    )
    if any(key not in primary for key in required):
        raise HTTPException(status_code=503, detail="evaluation metrics snapshot is incomplete")

    return EvaluationSummaryResponse(
        project=str(snapshot.get("project", "Enterprise Ticket RAG Copilot")),
        generatedAt=str(snapshot.get("generated_at", "unknown")),
        sampleCount=int(dataset.get("sample_count", 0)),
        topK=int(retrieval.get("top_k", 0)),
        metrics=MetricSummary(
            topKHitRate=float(primary["top_k_hit_rate"]),
            contextRecallAtK=float(primary["context_recall_at_k"]),
            citationCoverage=float(primary["citation_coverage"]),
            citationPrecision=float(primary["citation_precision"]),
            failedCaseCount=int(primary["failed_case_count"]),
            humanReviewRequiredCount=int(primary["human_review_required_count"]),
        ),
        boundary=DEMO_BOUNDARY,
    )
