"""Compare the current keyword baseline with a dependency-free Retrieval V2.

Retrieval V2 is an experiment for the fixed synthetic portfolio dataset. It uses
only the Python standard library: field-aware keyword evidence, corpus-derived
IDF weighting, category mismatch penalties, and a no-answer threshold. It does
not implement embeddings, a vector database, or production retrieval.
"""

from __future__ import annotations

import argparse
import json
import math
import re
import statistics
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from scripts.evaluate_rag_demo import (
    DEFAULT_CASES,
    KNOWLEDGE_CORPUS,
    REPO_ROOT,
    evaluate_case,
    load_cases,
    normalize,
    summarize_results,
)

DEFAULT_JSON = REPO_ROOT / "docs" / "metrics" / "retrieval_v2_comparison_latest.json"
DEFAULT_MD = REPO_ROOT / "docs" / "metrics" / "retrieval_v2_comparison_latest.md"
TOP_K_DEFAULT = 3
V2_MIN_SCORE = 18.0
RETRIEVAL_V2_BOUNDARY = (
    "Dependency-free field-aware IDF retrieval experiment on the synthetic demo corpus. "
    "No embedding model, Vector DB, dense retrieval, external reranker, real provider call, or production data."
)

GENERIC_TERMS = {
    "服务",
    "异常",
    "报错",
    "error",
    "流程",
    "访问",
    "配置",
    "审批",
    "登录",
    "数据",
    "启动失败",
}

ARTICLE_ANCHORS = {
    "KB-JAVA-PORT": ("java", "8080", "bindexception", "端口"),
    "KB-SPRING-BEAN": ("spring boot", "beancreationexception", "bean"),
    "KB-REDIS-CONN": ("redis", "lettuce", "jedis"),
    "KB-MYSQL-SLOW": ("mysql", "慢查询", "sql", "索引", "执行计划"),
    "KB-DEPLOY-ENV": ("环境变量", "configmap", "secret", "kubernetes", "部署"),
}

FIELD_WEIGHTS = {
    "user_question": 1.25,
    "title": 1.15,
    "description": 1.0,
    "error_log": 1.35,
    "system_name": 0.55,
    "business_context": 0.20,
}


def _case_fields(case: dict[str, Any]) -> dict[str, str]:
    context = case.get("ticket_context", {})
    return {
        "user_question": normalize(str(case.get("user_question", ""))),
        "title": normalize(str(context.get("title", ""))),
        "description": normalize(str(context.get("description", ""))),
        "error_log": normalize(str(context.get("error_log", ""))),
        "system_name": normalize(str(context.get("system_name", ""))),
        "business_context": normalize(str(context.get("business_context", ""))),
    }


def _document_frequency(keyword: str) -> int:
    needle = normalize(keyword)
    return sum(
        1
        for article in KNOWLEDGE_CORPUS
        if any(needle == normalize(candidate) for candidate in article["keywords"])
    )


def _idf(keyword: str) -> float:
    df = _document_frequency(keyword)
    corpus_size = len(KNOWLEDGE_CORPUS)
    return math.log(1 + (corpus_size - df + 0.5) / (df + 0.5))


def _specificity(keyword: str) -> float:
    normalized = normalize(keyword)
    if normalized in GENERIC_TERMS:
        return 0.35
    if re.search(r"[0-9]", normalized):
        return 1.35
    if re.search(r"[a-z]", normalized) and len(normalized) >= 4:
        return 1.25
    if len(normalized) >= 4:
        return 1.15
    return 1.0


def v2_article_score(article: dict[str, Any], case: dict[str, Any]) -> float:
    fields = _case_fields(case)
    score = 0.0
    matched_specific = 0

    for keyword in article["keywords"]:
        needle = normalize(keyword)
        best_field_score = 0.0
        for field_name, field_text in fields.items():
            if needle and needle in field_text:
                best_field_score = max(best_field_score, FIELD_WEIGHTS[field_name])
        if best_field_score:
            specificity = _specificity(keyword)
            score += 5.0 * best_field_score * specificity * _idf(keyword)
            if specificity >= 1.0:
                matched_specific += 1

    expected_category = case.get("category")
    if expected_category and expected_category == article["category"]:
        score += 6.0
    elif expected_category:
        score -= 15.0

    anchors = ARTICLE_ANCHORS.get(article["id"])
    if anchors:
        high_signal_text = " ".join(
            fields[name] for name in ("user_question", "title", "description", "error_log", "system_name")
        )
        if not any(normalize(anchor) in high_signal_text for anchor in anchors):
            score -= 14.0

    # Small coordination bonus for multiple concrete signals; this helps keep
    # Java/Redis/API-specific evidence above generic operational articles.
    if matched_specific >= 2:
        score += min(8.0, (matched_specific - 1) * 2.5)

    # P1 system failures often legitimately need the general operations guide as
    # a secondary source, but only when the ticket actually contains failure signals.
    if (
        article["id"] == "KB-OPS-003"
        and case.get("category") == "系统故障"
        and case.get("priority") == "P1"
        and any(signal in " ".join(fields.values()) for signal in ("500", "502", "timeout", "超时", "错误率", "回滚"))
    ):
        score += 6.0

    return round(score, 4)


def retrieve_v2(case: dict[str, Any], top_k: int) -> tuple[list[dict[str, Any]], float]:
    started = time.perf_counter()
    scored: list[dict[str, Any]] = []
    for article in KNOWLEDGE_CORPUS:
        score = v2_article_score(article, case)
        if score >= V2_MIN_SCORE:
            item = dict(article)
            item["score"] = score
            scored.append(item)
    scored.sort(key=lambda item: (-item["score"], item["id"]))
    elapsed_ms = (time.perf_counter() - started) * 1000
    return scored[:top_k], elapsed_ms


def evaluate_case_v2(case: dict[str, Any], top_k: int) -> dict[str, Any]:
    # Reuse the baseline evaluator's metric semantics, replacing only retrieval.
    retrieved, latency_ms = retrieve_v2(case, top_k)
    retrieved_ids = [article["id"] for article in retrieved]
    expected_ids = case.get("expected_knowledge_ids", [])
    expected_set = set(expected_ids)
    citation_ids = retrieved_ids if retrieved_ids else []
    hit = bool(expected_set.intersection(retrieved_ids)) if expected_ids else None
    citation_expected = bool(expected_ids)
    citation_coverage = bool(citation_ids) if citation_expected else None
    citation_precision = (
        len(expected_set.intersection(citation_ids)) / len(citation_ids)
        if citation_ids and citation_expected
        else None
    )

    expected_keywords = case.get("expected_keywords", [])
    context = " ".join(
        normalize(" ".join([article["id"], article["title"], article["category"], " ".join(article["keywords"]), article["content"], article["owner"]]))
        for article in retrieved
    )
    context_recall = (
        sum(1 for keyword in expected_keywords if normalize(keyword) in context) / len(expected_keywords)
        if expected_keywords
        else 0.0
    )

    mrr = None
    ndcg = None
    if expected_ids:
        mrr = 0.0
        for index, article_id in enumerate(retrieved_ids, start=1):
            if article_id in expected_set:
                mrr = 1 / index
                break
        gains = [1 if article_id in expected_set else 0 for article_id in retrieved_ids[:top_k]]
        dcg = sum(gain / math.log2(index + 2) for index, gain in enumerate(gains))
        ideal_count = min(len(expected_set), top_k)
        idcg = sum(1 / math.log2(index + 2) for index in range(ideal_count))
        ndcg = dcg / idcg if idcg else 0.0

    failures: list[str] = []
    if expected_ids and not hit:
        failures.append("missed_expected_knowledge")
    if expected_ids and not citation_ids:
        failures.append("missing_citation")
    if expected_ids and citation_ids and citation_precision is not None and citation_precision < 1:
        failures.append("citation_contains_unexpected_source")
    if not expected_ids and retrieved_ids:
        failures.append("unexpected_retrieval_for_fallback_case")

    expected_gate = bool(case.get("baseline_expected", {}).get("human_review_required"))
    needs_review = bool(expected_gate or failures or not citation_ids or not expected_ids)

    return {
        "case_id": case["id"],
        "category": case["category"],
        "priority": case["priority"],
        "expected_knowledge_ids": expected_ids,
        "retrieved_ids": retrieved_ids,
        "retrieved_scores": {article["id"]: article["score"] for article in retrieved},
        "top_k_hit": hit,
        "context_recall_at_k": round(context_recall, 4),
        "citation_coverage": citation_coverage,
        "citation_precision": None if citation_precision is None else round(citation_precision, 4),
        "mrr": None if mrr is None else round(mrr, 4),
        "ndcg_at_k": None if ndcg is None else round(ndcg, 4),
        "retrieval_latency_ms": round(latency_ms, 4),
        "knowledge_fallback_used": not retrieved_ids or not expected_ids,
        "provider_fallback_used": True,
        "human_review_required": needs_review,
        "failure_reasons": failures,
        "failure_reason_if_miss": case.get("failure_reason_if_miss", ""),
        "simulated_answer": {
            "mode": "local-rule simulated answer",
            "answer_points": case.get("expected_answer_points", [])[:3],
            "citation_ids": citation_ids,
            "human_review_required": needs_review,
        },
    }


def build_comparison(cases: list[dict[str, Any]], top_k: int) -> dict[str, Any]:
    baseline_results = [
        evaluate_case(
            case,
            top_k=top_k,
            retrieval_mode="keyword",
            citations_enabled=True,
            human_gate_enabled=True,
        )
        for case in cases
    ]
    v2_results = [evaluate_case_v2(case, top_k) for case in cases]
    baseline_metrics = summarize_results(baseline_results, len(cases), top_k)
    v2_metrics = summarize_results(v2_results, len(cases), top_k)

    changed_cases = []
    for baseline, v2 in zip(baseline_results, v2_results, strict=True):
        if baseline["retrieved_ids"] != v2["retrieved_ids"] or baseline["failure_reasons"] != v2["failure_reasons"]:
            changed_cases.append(
                {
                    "case_id": baseline["case_id"],
                    "expected_knowledge_ids": baseline["expected_knowledge_ids"],
                    "baseline_retrieved_ids": baseline["retrieved_ids"],
                    "v2_retrieved_ids": v2["retrieved_ids"],
                    "baseline_failures": baseline["failure_reasons"],
                    "v2_failures": v2["failure_reasons"],
                }
            )

    return {
        "generated_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        "dataset": {
            "path": str(DEFAULT_CASES.relative_to(REPO_ROOT)),
            "sample_count": len(cases),
            "type": "synthetic enterprise ticket demo dataset",
        },
        "top_k": top_k,
        "baseline": {
            "name": "category-aware keyword retrieval",
            "metrics": baseline_metrics,
        },
        "retrieval_v2": {
            "name": "field-aware IDF keyword retrieval + rerank + no-answer threshold",
            "min_score": V2_MIN_SCORE,
            "metrics": v2_metrics,
        },
        "changed_cases": changed_cases,
        "boundaries": [
            "Dependency-free local experiment on 16 synthetic demo cases.",
            "No embedding model, vector database, dense retrieval, external reranker, or real provider call.",
            "The same fixed dataset is used for design and evaluation, so these numbers are portfolio experiment evidence, not an unbiased production benchmark.",
        ],
    }


def _pct(value: float) -> str:
    return f"{value:.2f}%"


def render_markdown(report: dict[str, Any]) -> str:
    baseline = report["baseline"]["metrics"]
    v2 = report["retrieval_v2"]["metrics"]
    rows = [
        ("Top-K Hit Rate", baseline["top_k_hit_rate"], v2["top_k_hit_rate"], True),
        ("Context Recall@K", baseline["context_recall_at_k"], v2["context_recall_at_k"], True),
        ("Citation Coverage", baseline["citation_coverage"], v2["citation_coverage"], True),
        ("Citation Precision", baseline["citation_precision"], v2["citation_precision"], True),
        ("MRR", baseline["mrr"], v2["mrr"], False),
        ("NDCG@K", baseline["ndcg_at_k"], v2["ndcg_at_k"], False),
        ("Failed Case Count", baseline["failed_case_count"], v2["failed_case_count"], False),
    ]
    lines = [
        "# Retrieval V2 Comparison",
        "",
        f"- Generated at: `{report['generated_at']}`",
        f"- Dataset: `{report['dataset']['path']}`",
        f"- Samples: `{report['dataset']['sample_count']}` synthetic cases",
        f"- Top-K: `{report['top_k']}`",
        "- V2: field-aware IDF keyword retrieval + category rerank + no-answer threshold",
        "",
        "## Metrics",
        "",
        "| Metric | Keyword Baseline | Retrieval V2 |",
        "| --- | ---: | ---: |",
    ]
    for label, before, after, percent in rows:
        if percent:
            lines.append(f"| {label} | {_pct(before)} | {_pct(after)} |")
        else:
            lines.append(f"| {label} | {before} | {after} |")

    lines.extend(["", "## Changed Cases", ""])
    for item in report["changed_cases"]:
        lines.append(
            f"- `{item['case_id']}` expected {item['expected_knowledge_ids']}: "
            f"baseline {item['baseline_retrieved_ids']} -> V2 {item['v2_retrieved_ids']}; "
            f"failures {item['baseline_failures']} -> {item['v2_failures']}"
        )
    if not report["changed_cases"]:
        lines.append("- None.")

    lines.extend(["", "## Boundaries", ""])
    lines.extend(f"- {boundary}" for boundary in report["boundaries"])
    lines.append("")
    return "\n".join(lines)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Compare keyword baseline with dependency-free Retrieval V2.")
    parser.add_argument("--cases", type=Path, default=DEFAULT_CASES)
    parser.add_argument("--output-json", type=Path, default=DEFAULT_JSON)
    parser.add_argument("--output-md", type=Path, default=DEFAULT_MD)
    parser.add_argument("--top-k", type=int, default=TOP_K_DEFAULT)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    cases = load_cases(args.cases)
    report = build_comparison(cases, args.top_k)
    args.output_json.parent.mkdir(parents=True, exist_ok=True)
    args.output_md.parent.mkdir(parents=True, exist_ok=True)
    args.output_json.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8-sig")
    args.output_md.write_text(render_markdown(report), encoding="utf-8-sig")

    baseline = report["baseline"]["metrics"]
    v2 = report["retrieval_v2"]["metrics"]
    print("Retrieval V2 comparison completed")
    print(f"baseline citation_precision={baseline['citation_precision']:.2f}% failed={baseline['failed_case_count']}")
    print(f"v2 citation_precision={v2['citation_precision']:.2f}% failed={v2['failed_case_count']}")
    print(f"v2 hit_rate={v2['top_k_hit_rate']:.2f}% recall={v2['context_recall_at_k']:.2f}%")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
