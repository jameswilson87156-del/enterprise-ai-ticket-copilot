# Retrieval V2 Comparison

- Generated at: `2026-09-14T01:20:04+08:00`
- Dataset: `data\eval\ticket_rag_eval_cases.jsonl`
- Samples: `16` synthetic cases
- Top-K: `3`
- V2: field-aware IDF keyword retrieval + category rerank + no-answer threshold

## Metrics

| Metric | Keyword Baseline | Retrieval V2 |
| --- | ---: | ---: |
| Top-K Hit Rate | 100.00% | 100.00% |
| Context Recall@K | 90.00% | 90.00% |
| Citation Coverage | 100.00% | 100.00% |
| Citation Precision | 81.11% | 97.78% |
| MRR | 1.0 | 1.0 |
| NDCG@K | 0.9742 | 0.9742 |
| Failed Case Count | 6 | 1 |

## Changed Cases

- `EVAL-003` expected ['KB-IAM-ROLE', 'KB-IAM-002']: baseline ['KB-IAM-002', 'KB-IAM-ROLE'] -> V2 ['KB-IAM-ROLE', 'KB-IAM-002']; failures [] -> []
- `EVAL-007` expected ['KB-JAVA-PORT']: baseline ['KB-JAVA-PORT', 'KB-OPS-003', 'KB-SPRING-BEAN'] -> V2 ['KB-JAVA-PORT']; failures ['citation_contains_unexpected_source'] -> []
- `EVAL-008` expected ['KB-SPRING-BEAN']: baseline ['KB-SPRING-BEAN', 'KB-API-500', 'KB-DEPLOY-ENV'] -> V2 ['KB-SPRING-BEAN']; failures ['citation_contains_unexpected_source'] -> []
- `EVAL-011` expected ['KB-DEPLOY-ENV']: baseline ['KB-DEPLOY-ENV', 'KB-API-500', 'KB-OPS-003'] -> V2 ['KB-DEPLOY-ENV']; failures ['citation_contains_unexpected_source'] -> []
- `EVAL-012` expected ['KB-FAQ-KNOWLEDGE', 'KB-PROCESS-005', 'KB-DRAFT-DEMO-0008']: baseline ['KB-DRAFT-DEMO-0008', 'KB-FAQ-KNOWLEDGE', 'KB-PROCESS-005'] -> V2 ['KB-FAQ-KNOWLEDGE', 'KB-DRAFT-DEMO-0008', 'KB-PROCESS-005']; failures [] -> []
- `EVAL-013` expected ['KB-OPS-003']: baseline ['KB-OPS-003', 'KB-REDIS-CONN'] -> V2 ['KB-OPS-003']; failures ['citation_contains_unexpected_source'] -> []
- `EVAL-015` expected []: baseline ['KB-FAQ-KNOWLEDGE'] -> V2 []; failures ['unexpected_retrieval_for_fallback_case'] -> []

## Boundaries

- Dependency-free local experiment on 16 synthetic demo cases.
- No embedding model, vector database, dense retrieval, external reranker, or real provider call.
- The same fixed dataset is used for design and evaluation, so these numbers are portfolio experiment evidence, not an unbiased production benchmark.
