# RAG Metrics Snapshot

- Generated at: `2026-07-04T14:00:05+08:00`
- Dataset: `data\eval\ticket_rag_eval_cases.jsonl`
- Samples: `16` synthetic demo cases
- Retrieval method: category-aware keyword retrieval matching the demo KnowledgeMatchingService scoring shape
- Top-K: `3`
- Provider mode: local-rule fallback; no real API key used

## Primary Metrics

| Metric | Value | Notes |
| --- | ---: | --- |
| Top-K Hit Rate | 100.00% | Only cases with expected knowledge IDs are counted. |
| Context Recall@K | 90.00% | Expected keywords covered by retrieved demo knowledge context. |
| Citation Coverage | 100.00% | Simulated answer carries citation IDs for expected-source cases. |
| Citation Precision | 81.11% | Citation IDs that belong to expected knowledge IDs. |
| MRR | 1.0000 | Optional local retrieval rank metric. |
| NDCG@K | 0.9742 | Optional binary relevance rank metric. |
| Avg Retrieval Latency | 0.0437 ms | Local in-memory demo scoring only. |
| Knowledge Miss Fallback Rate | 6.25% | No expected source or no retrieved source. |
| Provider Fallback Rate | 100.00% | Expected 100% in this no-key local run. |
| Failed Case Count | 6 | Retrieval/citation quality failures, not production incidents. |
| Human Review Required Count | 15 | High-risk or low-evidence cases gated to review. |

## Baseline Comparison

| Baseline | Top-K Hit | Citation Coverage | Citation Precision | Failed Cases | Human Review Required |
| --- | ---: | ---: | ---: | ---: | ---: |
| `keyword_only` | 100.00% | 0.00% | 0.00% | 1 | 0 |
| `naive_keyword_score` | 100.00% | 0.00% | 0.00% | 0 | 0 |
| `with_citation_required` | 100.00% | 100.00% | 81.11% | 6 | 0 |
| `with_human_review_gate` | 100.00% | 100.00% | 81.11% | 6 | 15 |

## Failed Cases

- `EVAL-007` expected ['KB-JAVA-PORT'] but retrieved ['KB-JAVA-PORT', 'KB-OPS-003', 'KB-SPRING-BEAN']; reasons: citation_contains_unexpected_source; note: 端口占用知识未命中会导致启动失败排查缺少明确方向。
- `EVAL-008` expected ['KB-SPRING-BEAN'] but retrieved ['KB-SPRING-BEAN', 'KB-API-500', 'KB-DEPLOY-ENV']; reasons: citation_contains_unexpected_source; note: Spring 启动失败未命中特定知识会弱化排查价值。
- `EVAL-011` expected ['KB-DEPLOY-ENV'] but retrieved ['KB-DEPLOY-ENV', 'KB-API-500', 'KB-OPS-003']; reasons: citation_contains_unexpected_source; note: 部署配置知识未命中会造成不安全的环境变量建议。
- `EVAL-013` expected ['KB-OPS-003'] but retrieved ['KB-OPS-003', 'KB-REDIS-CONN']; reasons: citation_contains_unexpected_source; note: SLA/P1 故障未命中系统故障知识会导致优先级和人工门禁不足。
- `EVAL-015` expected [] but retrieved ['KB-FAQ-KNOWLEDGE']; reasons: unexpected_retrieval_for_fallback_case; note: 缺知识场景如果仍引用无关知识，说明 citation gating 不足。
- `EVAL-016` expected ['KB-API-500', 'KB-OPS-003'] but retrieved ['KB-API-500', 'KB-OPS-003', 'KB-ACCOUNT-001']; reasons: citation_contains_unexpected_source; note: 相似登录词导致误命中账号知识，说明需要更好的分类或 rerank。

## Boundary Notes

- Synthetic demo dataset + local keyword retrieval + citation gating. No embedding, Vector DB, reranker, real provider call, or production user data.
- Fallback rate includes provider fallback because this run intentionally does not configure a real provider key.
- Answer Relevance, Faithfulness, Hallucination Case Count, Token Cost, and Prompt v1/v2 uplift are not measured in this local run.

## Resume-Safe Expression

基于 16 条自建企业工单 demo 评测集，完成 keyword retrieval + citation gating 的本地评测，统计 Top-3 Hit Rate、Context Recall@3、Citation Coverage、Citation Precision、Retrieval Latency 与 Human Review gate。

## Do Not Claim

- 不要写真实向量检索或 Vector DB 已上线。
- 不要写真实模型准确率、真实 Prompt 提升或生产可用。
- 不要把 100% provider fallback 写成模型质量指标；它只说明本轮未接真实 API Key。
- 不要声称服务真实企业用户或真实客户数据。
