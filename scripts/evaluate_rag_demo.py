"""Evaluate demo keyword retrieval, citation gating, and human review metrics.

This script intentionally uses only Python standard library modules. It does
not call a real LLM provider, does not read API keys, and does not connect to
MySQL. The corpus mirrors the repository's demo knowledge articles so the
result is a reproducible portfolio metric snapshot, not a production RAG score.
"""

from __future__ import annotations

import argparse
import json
import math
import platform
import statistics
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CASES = REPO_ROOT / "data" / "eval" / "ticket_rag_eval_cases.jsonl"
DEFAULT_JSON = REPO_ROOT / "docs" / "metrics" / "rag_metrics_latest.json"
DEFAULT_MD = REPO_ROOT / "docs" / "metrics" / "rag_metrics_snapshot.md"

TOP_K_DEFAULT = 3
MIN_SCORE = 35

DEMO_BOUNDARY = (
    "Synthetic demo dataset + local keyword retrieval + citation gating. "
    "No embedding, Vector DB, reranker, real provider call, or production user data."
)

KNOWLEDGE_CORPUS: list[dict[str, Any]] = [
    {
        "id": "KB-ACCOUNT-001",
        "title": "账号登录失败与 MFA 重置处理流程",
        "category": "账号问题",
        "keywords": ["账号", "登录", "密码", "MFA", "SSO", "认证", "锁定"],
        "content": "核验员工身份后检查账号锁定状态、MFA 绑定和 SSO 审计日志。任何重置动作必须由人工确认。",
        "owner": "IT Identity",
    },
    {
        "id": "KB-IAM-002",
        "title": "权限申请与最小权限核验清单",
        "category": "权限问题",
        "keywords": ["权限", "授权", "角色", "RBAC", "审批", "访问", "共享盘", "报表"],
        "content": "核验申请人、审批记录、目标资源和最小权限角色。授权动作不得由规则引擎自动执行。",
        "owner": "IT Governance",
    },
    {
        "id": "KB-OPS-003",
        "title": "系统故障超时与 5xx 排查手册",
        "category": "系统故障",
        "keywords": ["502", "500", "timeout", "超时", "异常", "报错", "服务", "网关", "error"],
        "content": "比对发布窗口、告警、依赖服务延迟和错误率。生产变更、降级或回滚必须由值班人员确认。",
        "owner": "SRE Enablement",
    },
    {
        "id": "KB-DATA-004",
        "title": "数据同步异常与报表口径排查",
        "category": "数据问题",
        "keywords": ["数据", "同步", "报表", "不一致", "缺失", "导入", "导出", "指标"],
        "content": "确认数据源、同步批次、口径变更和抽样记录，必要时保留修复前后的审计材料。",
        "owner": "Data Platform",
    },
    {
        "id": "KB-PROCESS-005",
        "title": "内部流程咨询分派与知识沉淀规范",
        "category": "流程咨询",
        "keywords": ["流程", "如何", "怎么", "申请", "规范", "指引", "步骤", "咨询"],
        "content": "将流程类问题转交对应流程负责人，处理完成后补充可复用指引并进入知识审核。",
        "owner": "Service Desk",
    },
    {
        "id": "KB-JAVA-PORT",
        "title": "Java 服务端口占用排查手册",
        "category": "系统故障",
        "keywords": ["Java", "端口", "占用", "BindException", "Tomcat", "8080"],
        "content": "检查端口监听进程，确认是否为旧实例未退出。释放端口或调整 server.port 前必须由人工确认。",
        "owner": "SRE Enablement",
    },
    {
        "id": "KB-SPRING-BEAN",
        "title": "Spring Boot BeanCreationException 处理清单",
        "category": "系统故障",
        "keywords": ["Spring Boot", "BeanCreationException", "bean", "依赖注入", "启动失败"],
        "content": "定位异常 bean、配置项、构造器依赖和 profile。变更配置或回滚版本需要人工确认。",
        "owner": "Java Platform",
    },
    {
        "id": "KB-REDIS-CONN",
        "title": "Redis 连接失败与连接池排查",
        "category": "系统故障",
        "keywords": ["Redis", "连接失败", "timeout", "Lettuce", "Jedis", "连接池"],
        "content": "检查 Redis 地址、网络 ACL、密码、连接池耗尽和超时参数。生产参数调整必须人工确认。",
        "owner": "Infra Platform",
    },
    {
        "id": "KB-MYSQL-SLOW",
        "title": "MySQL 慢查询定位与索引评估流程",
        "category": "数据问题",
        "keywords": ["MySQL", "慢查询", "索引", "SQL", "执行计划", "锁等待"],
        "content": "收集慢查询 SQL、执行计划、索引命中和锁等待。上线索引前需要评估写入影响。",
        "owner": "DBA Team",
    },
    {
        "id": "KB-API-500",
        "title": "接口 500 错误分层排查手册",
        "category": "系统故障",
        "keywords": ["接口", "500", "error", "traceId", "异常", "网关"],
        "content": "使用 traceId 关联网关、应用日志和依赖调用。恢复动作和对外口径必须人工确认。",
        "owner": "Service Desk",
    },
    {
        "id": "KB-IAM-ROLE",
        "title": "权限配置问题与 RBAC 核验流程",
        "category": "权限问题",
        "keywords": ["权限", "RBAC", "角色", "403", "Forbidden", "访问控制", "审批"],
        "content": "核对审批、角色、资源策略和审计记录。授权动作不能自动执行。",
        "owner": "IT Governance",
    },
    {
        "id": "KB-DEPLOY-ENV",
        "title": "部署环境变量缺失排查清单",
        "category": "系统故障",
        "keywords": ["部署", "环境变量", "配置", "profile", "container", "Kubernetes"],
        "content": "比对环境变量、ConfigMap、Secret 和启动参数。修改部署配置必须人工确认。",
        "owner": "Release Engineering",
    },
    {
        "id": "KB-FAQ-KNOWLEDGE",
        "title": "重复咨询转知识库沉淀规范",
        "category": "流程咨询",
        "keywords": ["重复咨询", "知识库", "FAQ", "流程", "沉淀", "审核"],
        "content": "识别重复问题后生成知识草稿，由知识负责人确认后发布。",
        "owner": "Knowledge Ops",
    },
    {
        "id": "KB-DRAFT-DEMO-0008",
        "title": "采购系统审批流程 FAQ",
        "category": "流程咨询",
        "keywords": ["采购", "审批", "FAQ", "流程", "材料"],
        "content": "汇总采购审批材料、审批节点和常见退回原因，已由知识负责人人工确认。",
        "owner": "Knowledge Reviewer",
    },
]


def normalize(value: str | None) -> str:
    return (value or "").lower()


def case_text(case: dict[str, Any]) -> str:
    context = case.get("ticket_context", {})
    parts = [
        case.get("user_question", ""),
        context.get("title", ""),
        context.get("description", ""),
        context.get("system_name", ""),
        context.get("error_log", ""),
        context.get("business_context", ""),
    ]
    return normalize(" ".join(str(part) for part in parts if part is not None))


def article_text(article: dict[str, Any]) -> str:
    return normalize(
        " ".join(
            [
                article["id"],
                article["title"],
                article["category"],
                " ".join(article["keywords"]),
                article["content"],
                article["owner"],
            ]
        )
    )


def article_score(article: dict[str, Any], case: dict[str, Any], mode: str) -> int:
    text = case_text(case)
    score = 0
    if mode == "keyword" and case.get("category") == article["category"]:
        score += 25
    for keyword in article["keywords"]:
        if normalize(keyword) in text:
            score += 18
    title = normalize(article["title"])
    if title and title[: min(len(title), 4)] in text:
        score += 10
    return min(96, score)


def retrieve(case: dict[str, Any], top_k: int, mode: str) -> tuple[list[dict[str, Any]], float]:
    started = time.perf_counter()
    scored = []
    for article in KNOWLEDGE_CORPUS:
        score = article_score(article, case, mode)
        if score >= MIN_SCORE:
            item = dict(article)
            item["score"] = score
            scored.append(item)
    scored.sort(key=lambda item: (-item["score"], item["id"]))
    elapsed_ms = (time.perf_counter() - started) * 1000
    return scored[:top_k], elapsed_ms


def reciprocal_rank(retrieved_ids: list[str], expected_ids: list[str]) -> float:
    expected = set(expected_ids)
    for index, article_id in enumerate(retrieved_ids, start=1):
        if article_id in expected:
            return 1 / index
    return 0.0


def ndcg_at_k(retrieved_ids: list[str], expected_ids: list[str], top_k: int) -> float:
    expected = set(expected_ids)
    gains = [1 if article_id in expected else 0 for article_id in retrieved_ids[:top_k]]
    dcg = sum(gain / math.log2(index + 2) for index, gain in enumerate(gains))
    ideal_count = min(len(expected), top_k)
    if ideal_count == 0:
        return 0.0
    idcg = sum(1 / math.log2(index + 2) for index in range(ideal_count))
    return dcg / idcg if idcg else 0.0


def keyword_context_recall(retrieved: list[dict[str, Any]], expected_keywords: list[str]) -> float:
    if not expected_keywords:
        return 0.0
    context = " ".join(article_text(article) for article in retrieved)
    covered = sum(1 for keyword in expected_keywords if normalize(keyword) in context)
    return covered / len(expected_keywords)


def evaluate_case(
    case: dict[str, Any],
    *,
    top_k: int,
    retrieval_mode: str,
    citations_enabled: bool,
    human_gate_enabled: bool,
) -> dict[str, Any]:
    retrieved, latency_ms = retrieve(case, top_k, retrieval_mode)
    retrieved_ids = [article["id"] for article in retrieved]
    expected_ids = case.get("expected_knowledge_ids", [])
    expected_set = set(expected_ids)
    citation_ids = retrieved_ids if citations_enabled and retrieved_ids else []
    hit = bool(expected_set.intersection(retrieved_ids)) if expected_ids else None
    citation_expected = bool(expected_ids)
    citation_coverage = bool(citation_ids) if citation_expected else None
    citation_precision = (
        len(expected_set.intersection(citation_ids)) / len(citation_ids)
        if citation_ids and citation_expected
        else None
    )
    context_recall = keyword_context_recall(retrieved, case.get("expected_keywords", []))
    mrr = reciprocal_rank(retrieved_ids, expected_ids) if expected_ids else None
    ndcg = ndcg_at_k(retrieved_ids, expected_ids, top_k) if expected_ids else None

    failures = []
    if expected_ids and not hit:
        failures.append("missed_expected_knowledge")
    if expected_ids and citations_enabled and not citation_ids:
        failures.append("missing_citation")
    if expected_ids and citation_ids and citation_precision is not None and citation_precision < 1:
        failures.append("citation_contains_unexpected_source")
    if not expected_ids and retrieved_ids:
        failures.append("unexpected_retrieval_for_fallback_case")

    expected_gate = bool(case.get("baseline_expected", {}).get("human_review_required"))
    needs_review = False
    if human_gate_enabled and (expected_gate or failures or not citation_ids or not expected_ids):
        needs_review = True

    simulated_answer = {
        "mode": "local-rule simulated answer",
        "answer_points": case.get("expected_answer_points", [])[:3],
        "citation_ids": citation_ids,
        "human_review_required": needs_review,
    }

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
        "simulated_answer": simulated_answer,
    }


def mean(values: list[float]) -> float:
    return sum(values) / len(values) if values else 0.0


def pct(value: float) -> float:
    return round(value * 100, 2)


def summarize_results(case_results: list[dict[str, Any]], sample_count: int, top_k: int) -> dict[str, Any]:
    retrieval_cases = [result for result in case_results if result["expected_knowledge_ids"]]
    citation_cases = retrieval_cases
    top_k_hits = [1 if result["top_k_hit"] else 0 for result in retrieval_cases]
    citation_coverages = [1 if result["citation_coverage"] else 0 for result in citation_cases]
    citation_precisions = [
        result["citation_precision"]
        for result in citation_cases
        if result["citation_precision"] is not None
    ]
    context_recalls = [result["context_recall_at_k"] for result in case_results]
    mrr_values = [result["mrr"] for result in retrieval_cases if result["mrr"] is not None]
    ndcg_values = [result["ndcg_at_k"] for result in retrieval_cases if result["ndcg_at_k"] is not None]
    latencies = [result["retrieval_latency_ms"] for result in case_results]
    failed_cases = [result for result in case_results if result["failure_reasons"]]
    human_review_cases = [result for result in case_results if result["human_review_required"]]
    knowledge_fallback_cases = [result for result in case_results if result["knowledge_fallback_used"]]
    provider_fallback_cases = [result for result in case_results if result["provider_fallback_used"]]

    return {
        "sample_count": sample_count,
        "retrieval_case_count": len(retrieval_cases),
        "top_k": top_k,
        "top_k_hit_rate": pct(mean(top_k_hits)),
        "context_recall_at_k": pct(mean(context_recalls)),
        "citation_coverage": pct(mean(citation_coverages)),
        "citation_precision": pct(mean(citation_precisions)),
        "mrr": round(mean(mrr_values), 4),
        "ndcg_at_k": round(mean(ndcg_values), 4),
        "retrieval_latency_ms": {
            "avg": round(mean(latencies), 4),
            "median": round(statistics.median(latencies), 4) if latencies else 0.0,
            "max": round(max(latencies), 4) if latencies else 0.0,
        },
        "fallback_rate": pct(len(set(result["case_id"] for result in knowledge_fallback_cases + provider_fallback_cases)) / sample_count),
        "knowledge_miss_fallback_rate": pct(len(knowledge_fallback_cases) / sample_count),
        "provider_fallback_rate": pct(len(provider_fallback_cases) / sample_count),
        "failed_case_count": len(failed_cases),
        "human_review_required_count": len(human_review_cases),
        "failed_cases": [
            {
                "case_id": result["case_id"],
                "expected_knowledge_ids": result["expected_knowledge_ids"],
                "retrieved_ids": result["retrieved_ids"],
                "failure_reasons": result["failure_reasons"],
                "failure_reason_if_miss": result["failure_reason_if_miss"],
            }
            for result in failed_cases
        ],
        "human_review_cases": [result["case_id"] for result in human_review_cases],
    }


def load_cases(path: Path) -> list[dict[str, Any]]:
    cases = []
    with path.open("r", encoding="utf-8-sig") as handle:
        for line_no, line in enumerate(handle, start=1):
            if not line.strip():
                continue
            try:
                cases.append(json.loads(line))
            except json.JSONDecodeError as exc:
                raise SystemExit(f"Invalid JSONL at {path}:{line_no}: {exc}") from exc
    if not cases:
        raise SystemExit(f"No cases found in {path}")
    return cases


def evaluate_baseline(cases: list[dict[str, Any]], top_k: int, config: dict[str, Any]) -> dict[str, Any]:
    case_results = [
        evaluate_case(
            case,
            top_k=top_k,
            retrieval_mode=config["retrieval_mode"],
            citations_enabled=config["citations_enabled"],
            human_gate_enabled=config["human_gate_enabled"],
        )
        for case in cases
    ]
    return {
        "description": config["description"],
        "retrieval_mode": config["retrieval_mode"],
        "citations_enabled": config["citations_enabled"],
        "human_gate_enabled": config["human_gate_enabled"],
        "metrics": summarize_results(case_results, len(cases), top_k),
        "case_results": case_results,
    }


def build_report(cases: list[dict[str, Any]], top_k: int) -> dict[str, Any]:
    baselines = {
        "keyword_only": {
            "description": "Current category-aware keyword retrieval without answer citation enforcement.",
            "retrieval_mode": "keyword",
            "citations_enabled": False,
            "human_gate_enabled": False,
        },
        "naive_keyword_score": {
            "description": "Keyword scoring without category bonus; used as a lightweight baseline.",
            "retrieval_mode": "naive",
            "citations_enabled": False,
            "human_gate_enabled": False,
        },
        "with_citation_required": {
            "description": "Category-aware keyword retrieval with citation IDs attached to the simulated answer.",
            "retrieval_mode": "keyword",
            "citations_enabled": True,
            "human_gate_enabled": False,
        },
        "with_human_review_gate": {
            "description": "Citation-required baseline plus a human review gate for high-risk, low-evidence, or fallback cases.",
            "retrieval_mode": "keyword",
            "citations_enabled": True,
            "human_gate_enabled": True,
        },
    }
    evaluated = {name: evaluate_baseline(cases, top_k, config) for name, config in baselines.items()}
    primary = evaluated["with_human_review_gate"]["metrics"]
    return {
        "generated_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        "project": "Enterprise Ticket RAG Copilot",
        "dataset": {
            "path": str(DEFAULT_CASES.relative_to(REPO_ROOT)),
            "type": "synthetic enterprise ticket demo dataset",
            "sample_count": len(cases),
            "contains_real_user_data": False,
        },
        "environment": {
            "python": sys.version.split()[0],
            "platform": platform.platform(),
            "provider_mode": "local-rule fallback; no real API key used",
        },
        "retrieval": {
            "method": "category-aware keyword retrieval matching the demo KnowledgeMatchingService scoring shape",
            "top_k": top_k,
            "min_score": MIN_SCORE,
            "knowledge_corpus_size": len(KNOWLEDGE_CORPUS),
        },
        "primary_baseline": "with_human_review_gate",
        "primary_metrics": primary,
        "baselines": evaluated,
        "boundaries": [
            DEMO_BOUNDARY,
            "Fallback rate includes provider fallback because this run intentionally does not configure a real provider key.",
            "Answer Relevance, Faithfulness, Hallucination Case Count, Token Cost, and Prompt v1/v2 uplift are not measured in this local run.",
        ],
        "resume_safe_expression": (
            f"基于 {len(cases)} 条自建企业工单 demo 评测集，完成 keyword retrieval + citation gating 的本地评测，"
            f"统计 Top-{top_k} Hit Rate、Context Recall@{top_k}、Citation Coverage、Citation Precision、Retrieval Latency 与 Human Review gate。"
        ),
        "do_not_claim": [
            "不要写真实向量检索或 Vector DB 已上线。",
            "不要写真实模型准确率、真实 Prompt 提升或生产可用。",
            "不要把 100% provider fallback 写成模型质量指标；它只说明本轮未接真实 API Key。",
            "不要声称服务真实企业用户或真实客户数据。",
        ],
    }


def format_pct(value: float) -> str:
    return f"{value:.2f}%"


def render_markdown(report: dict[str, Any]) -> str:
    metrics = report["primary_metrics"]
    lines = [
        "# RAG Metrics Snapshot",
        "",
        f"- Generated at: `{report['generated_at']}`",
        f"- Dataset: `{report['dataset']['path']}`",
        f"- Samples: `{report['dataset']['sample_count']}` synthetic demo cases",
        f"- Retrieval method: {report['retrieval']['method']}",
        f"- Top-K: `{report['retrieval']['top_k']}`",
        f"- Provider mode: {report['environment']['provider_mode']}",
        "",
        "## Primary Metrics",
        "",
        "| Metric | Value | Notes |",
        "| --- | ---: | --- |",
        f"| Top-K Hit Rate | {format_pct(metrics['top_k_hit_rate'])} | Only cases with expected knowledge IDs are counted. |",
        f"| Context Recall@K | {format_pct(metrics['context_recall_at_k'])} | Expected keywords covered by retrieved demo knowledge context. |",
        f"| Citation Coverage | {format_pct(metrics['citation_coverage'])} | Simulated answer carries citation IDs for expected-source cases. |",
        f"| Citation Precision | {format_pct(metrics['citation_precision'])} | Citation IDs that belong to expected knowledge IDs. |",
        f"| MRR | {metrics['mrr']:.4f} | Optional local retrieval rank metric. |",
        f"| NDCG@K | {metrics['ndcg_at_k']:.4f} | Optional binary relevance rank metric. |",
        f"| Avg Retrieval Latency | {metrics['retrieval_latency_ms']['avg']:.4f} ms | Local in-memory demo scoring only. |",
        f"| Knowledge Miss Fallback Rate | {format_pct(metrics['knowledge_miss_fallback_rate'])} | No expected source or no retrieved source. |",
        f"| Provider Fallback Rate | {format_pct(metrics['provider_fallback_rate'])} | Expected 100% in this no-key local run. |",
        f"| Failed Case Count | {metrics['failed_case_count']} | Retrieval/citation quality failures, not production incidents. |",
        f"| Human Review Required Count | {metrics['human_review_required_count']} | High-risk or low-evidence cases gated to review. |",
        "",
        "## Baseline Comparison",
        "",
        "| Baseline | Top-K Hit | Citation Coverage | Citation Precision | Failed Cases | Human Review Required |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    for name, baseline in report["baselines"].items():
        baseline_metrics = baseline["metrics"]
        lines.append(
            f"| `{name}` | {format_pct(baseline_metrics['top_k_hit_rate'])} | "
            f"{format_pct(baseline_metrics['citation_coverage'])} | "
            f"{format_pct(baseline_metrics['citation_precision'])} | "
            f"{baseline_metrics['failed_case_count']} | "
            f"{baseline_metrics['human_review_required_count']} |"
        )
    lines.extend(
        [
            "",
            "## Failed Cases",
            "",
        ]
    )
    if metrics["failed_cases"]:
        for item in metrics["failed_cases"]:
            lines.append(
                f"- `{item['case_id']}` expected {item['expected_knowledge_ids']} but retrieved {item['retrieved_ids']}; "
                f"reasons: {', '.join(item['failure_reasons'])}; note: {item['failure_reason_if_miss']}"
            )
    else:
        lines.append("- None for the primary baseline.")
    lines.extend(
        [
            "",
            "## Boundary Notes",
            "",
        ]
    )
    for note in report["boundaries"]:
        lines.append(f"- {note}")
    lines.extend(
        [
            "",
            "## Resume-Safe Expression",
            "",
            report["resume_safe_expression"],
            "",
            "## Do Not Claim",
            "",
        ]
    )
    for claim in report["do_not_claim"]:
        lines.append(f"- {claim}")
    lines.append("")
    return "\n".join(lines)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Evaluate local demo RAG retrieval and citation metrics.")
    parser.add_argument("--cases", type=Path, default=DEFAULT_CASES)
    parser.add_argument("--output-json", type=Path, default=DEFAULT_JSON)
    parser.add_argument("--output-md", type=Path, default=DEFAULT_MD)
    parser.add_argument("--top-k", type=int, default=TOP_K_DEFAULT)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    cases = load_cases(args.cases)
    report = build_report(cases, args.top_k)

    args.output_json.parent.mkdir(parents=True, exist_ok=True)
    args.output_md.parent.mkdir(parents=True, exist_ok=True)
    args.output_json.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8-sig")
    args.output_md.write_text(render_markdown(report), encoding="utf-8-sig")

    metrics = report["primary_metrics"]
    print("RAG demo evaluation completed")
    print(f"cases={metrics['sample_count']} top_k={metrics['top_k']}")
    print(f"top_k_hit_rate={metrics['top_k_hit_rate']:.2f}%")
    print(f"context_recall_at_k={metrics['context_recall_at_k']:.2f}%")
    print(f"citation_coverage={metrics['citation_coverage']:.2f}%")
    print(f"citation_precision={metrics['citation_precision']:.2f}%")
    print(f"failed_case_count={metrics['failed_case_count']}")
    print(f"human_review_required_count={metrics['human_review_required_count']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
