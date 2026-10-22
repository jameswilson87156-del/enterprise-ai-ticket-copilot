# Ticket Copilot Retrieval V2 H2 Evidence

- Dataset: `v2-full-corpus-qrels`; 14 fixed demo article IDs from `schema.sql` + `demo-data.sql`.
- Queries: 18 positive cases (including multi-relevant qrels) and 4 no-match controls.
- Runtime path: real Spring context, MyBatis-Plus mapper, H2 MySQL-mode schema, and `HybridKnowledgeScorer` keyword baseline.
- Command: `mvn -o "-Dtest=RetrievalEvaluationServiceTest,RetrievalEvaluationH2IntegrationTest" test` from `backend/`.
- Result: 5 tests passed, 0 failed, 0 errors, 0 skipped.
- Retrieval output: [knowledge-retrieval-v2-h2.json](knowledge-retrieval-v2-h2.json).
- SHA-256: `362B88424A81C97884B95AFBB7F24772A21E705BD47173696BA8EA653FDED2EB`.
- Results: Hit Rate@1/3 14/18 (77.8%); Macro Recall@1 59.3%; Macro Recall@3 72.2%; Precision@3 37.0%; MRR 0.778; no-match specificity 3/4 (75.0%).
- Failures: positive misses R04/R06/R08/R14; partial multi-qrels R10/R12; false positive N01 (`KB-PROCESS-005`).
- Boundaries: synthetic cases and test-seeded article metadata only; no customer data, live MySQL, DeepSeek, Embedding API, or browser request was used.
