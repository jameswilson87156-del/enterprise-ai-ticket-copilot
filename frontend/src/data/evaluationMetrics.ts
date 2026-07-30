// Synced from docs/metrics/rag_metrics_latest.json, docs/metrics/rag_metrics_snapshot.md,
// and data/eval/ticket_rag_eval_cases.jsonl.
// Source scope: synthetic demo dataset + local keyword retrieval + citation gating.
// These constants are only for portfolio frontend display and do not represent
// production metrics, real vector RAG quality, real provider accuracy, or real user traffic.

export type MetricTone = 'blue' | 'cyan' | 'green' | 'amber' | 'red' | 'violet' | 'slate'
export type CaseStatus = 'hit' | 'miss' | 'review'

export interface MetricCardData {
  label: string
  value: string
  note: string
  tone: MetricTone
  source: string
}

export interface BaselineRow {
  label: string
  name: string
  scope: string
  topKHitRate: string
  citationCoverage: string
  citationPrecision: string
  failedCases: string
  humanReviewRequired: string
}

export interface EvaluationCaseRow {
  id: string
  category: string
  priority: 'P1' | 'P2' | 'P3'
  expectedKnowledgeIds: string[]
  topKStatus: CaseStatus
  citationStatus: string
  failureReason: string
  reviewRequired: boolean
}

export interface TicketRunRow {
  ticketId: string
  caseId: string
  category: string
  priority: 'P1' | 'P2' | 'P3'
  retrievalStatus: string
  citationStatus: string
  reviewStatus: string
  latency: string
  providerPath: string
}

export interface KnowledgeSourceRow {
  id: string
  name: string
  matchedTickets: string
  citationUsage: string
  keywordStatus: string
  sourceType: string
}

export interface ProviderStatusItem {
  label: string
  value: string
  note: string
  tone: MetricTone
}

export interface FailureAnalysisItem {
  label: string
  count: string
  note: string
  tone: MetricTone
}

export const evaluationSnapshot = {
  generatedAt: '2026-07-04T14:00:05+08:00',
  datasetPath: 'data/eval/ticket_rag_eval_cases.jsonl',
  datasetType: 'synthetic enterprise ticket demo dataset',
  containsRealUserData: false,
  retrievalMethod: 'category-aware keyword retrieval matching the demo KnowledgeMatchingService scoring shape',
  providerMode: 'local-rule fallback; no real API key used',
  topK: 3,
  sampleCount: 16,
  retrievalCaseCount: 15,
  topKHitRate: '100.00%',
  contextRecallAtK: '90.00%',
  citationCoverage: '100.00%',
  citationPrecision: '81.11%',
  mrr: '1.0000',
  ndcgAtK: '0.9742',
  avgRetrievalLatency: '0.0428 ms',
  medianRetrievalLatency: '0.0418 ms',
  maxRetrievalLatency: '0.0636 ms',
  providerFallbackRate: '100.00%',
  knowledgeMissFallbackRate: '6.25%',
  failedCaseCount: 6,
  humanReviewRequiredCount: 15,
  primaryBaseline: 'with_human_review_gate'
} as const

export const dashboardMetrics: MetricCardData[] = [
  {
    label: 'Total Tickets',
    value: '8',
    note: 'frontend demoTickets.ts seed cases',
    tone: 'blue',
    source: '本地 demo 工单'
  },
  {
    label: 'AI Drafts',
    value: '8',
    note: '模板化草稿 / demo assisted only',
    tone: 'violet',
    source: '本地 demo 工单'
  },
  {
    label: 'Knowledge Hits',
    value: '15 / 16',
    note: 'retrieval cases with expected knowledge IDs',
    tone: 'green',
    source: 'rag_metrics_latest.json'
  },
  {
    label: 'Human Reviews',
    value: '15',
    note: 'high-risk or low-evidence cases gated',
    tone: 'amber',
    source: 'rag_metrics_latest.json'
  },
  {
    label: 'Provider Status',
    value: 'fallback',
    note: 'no real API key configured',
    tone: 'red',
    source: 'rag_metrics_latest.json'
  },
  {
    label: 'Avg Local Retrieval',
    value: evaluationSnapshot.avgRetrievalLatency,
    note: 'local in-memory keyword scoring',
    tone: 'cyan',
    source: 'rag_metrics_latest.json'
  },
  {
    label: 'Eval Samples',
    value: String(evaluationSnapshot.sampleCount),
    note: 'synthetic enterprise ticket cases',
    tone: 'slate',
    source: 'ticket_rag_eval_cases.jsonl'
  },
  {
    label: 'Failed Cases',
    value: String(evaluationSnapshot.failedCaseCount),
    note: 'citation / fallback quality failures',
    tone: 'red',
    source: 'rag_metrics_latest.json'
  }
]

export const evaluationMetricCards: MetricCardData[] = [
  {
    label: '评测样本',
    value: String(evaluationSnapshot.sampleCount),
    note: 'synthetic demo cases',
    tone: 'blue',
    source: 'ticket_rag_eval_cases.jsonl'
  },
  {
    label: 'Top-K 命中率',
    value: evaluationSnapshot.topKHitRate,
    note: `Top-${evaluationSnapshot.topK} 结果命中预期知识源`,
    tone: 'green',
    source: 'rag_metrics_latest.json'
  },
  {
    label: 'Context Recall@K',
    value: evaluationSnapshot.contextRecallAtK,
    note: '期望关键词被 Top-K 覆盖',
    tone: 'cyan',
    source: 'rag_metrics_latest.json'
  },
  {
    label: '引用覆盖率',
    value: evaluationSnapshot.citationCoverage,
    note: 'AI 草稿已附带引用来源',
    tone: 'violet',
    source: 'rag_metrics_latest.json'
  },
  {
    label: '引用准确率',
    value: evaluationSnapshot.citationPrecision,
    note: '引用来源匹配预期知识源',
    tone: 'amber',
    source: 'rag_metrics_latest.json'
  },
  {
    label: '平均检索耗时',
    value: evaluationSnapshot.avgRetrievalLatency,
    note: '本地内存检索耗时',
    tone: 'cyan',
    source: 'rag_metrics_latest.json'
  },
  {
    label: '失败样本数',
    value: String(evaluationSnapshot.failedCaseCount),
    note: '未命中、引用偏差或 fallback 样本',
    tone: 'red',
    source: 'rag_metrics_latest.json'
  },
  {
    label: '需人工复核',
    value: String(evaluationSnapshot.humanReviewRequiredCount),
    note: '触发人工复核门禁',
    tone: 'amber',
    source: 'rag_metrics_latest.json'
  },
  {
    label: 'MRR',
    value: evaluationSnapshot.mrr,
    note: '可选本地排序指标',
    tone: 'slate',
    source: 'rag_metrics_latest.json'
  },
  {
    label: 'NDCG@K',
    value: evaluationSnapshot.ndcgAtK,
    note: '可选二值相关性指标',
    tone: 'slate',
    source: 'rag_metrics_latest.json'
  }
]

export const baselineRows: BaselineRow[] = [
  {
    label: '关键词检索',
    name: 'keyword_only',
    scope: 'category-aware keyword retrieval; no citation enforcement',
    topKHitRate: '100.00%',
    citationCoverage: '0.00%',
    citationPrecision: '0.00%',
    failedCases: '1',
    humanReviewRequired: '0'
  },
  {
    label: '简单关键词评分',
    name: 'naive_keyword_score',
    scope: 'keyword scoring without category weighting',
    topKHitRate: '100.00%',
    citationCoverage: '0.00%',
    citationPrecision: '0.00%',
    failedCases: '0',
    humanReviewRequired: '0'
  },
  {
    label: '引用门禁',
    name: 'with_citation_required',
    scope: 'keyword retrieval plus simulated citation IDs',
    topKHitRate: '100.00%',
    citationCoverage: '100.00%',
    citationPrecision: '81.11%',
    failedCases: '6',
    humanReviewRequired: '0'
  },
  {
    label: '人工复核门禁',
    name: 'with_human_review_gate',
    scope: 'citation required plus high-risk review gate',
    topKHitRate: '100.00%',
    citationCoverage: '100.00%',
    citationPrecision: '81.11%',
    failedCases: '6',
    humanReviewRequired: '15'
  }
]

export const evaluationBaselineRows: BaselineRow[] = [
  {
    ...baselineRows[0],
    label: '关键词检索',
    scope: '分类感知关键词检索'
  },
  {
    ...baselineRows[1],
    label: '简单关键词评分',
    scope: '不含分类权重的关键词评分'
  },
  {
    ...baselineRows[3],
    label: '引用门禁本地版',
    name: 'citation_gated_local',
    scope: '关键词检索 + citation gating + review gate'
  }
]

export const evaluationCases: EvaluationCaseRow[] = [
  {
    id: 'EVAL-003',
    category: '权限问题',
    priority: 'P3',
    expectedKnowledgeIds: ['KB-IAM-ROLE', 'KB-IAM-002'],
    topKStatus: 'hit',
    citationStatus: 'expected sources cited',
    failureReason: '无',
    reviewRequired: true
  },
  {
    id: 'EVAL-006',
    category: '数据问题',
    priority: 'P2',
    expectedKnowledgeIds: ['KB-MYSQL-SLOW', 'KB-DATA-004'],
    topKStatus: 'hit',
    citationStatus: 'expected source cited',
    failureReason: '无',
    reviewRequired: true
  },
  {
    id: 'EVAL-007',
    category: '系统故障',
    priority: 'P2',
    expectedKnowledgeIds: ['KB-JAVA-PORT'],
    topKStatus: 'review',
    citationStatus: 'unexpected source included',
    failureReason: 'citation_contains_unexpected_source',
    reviewRequired: true
  },
  {
    id: 'EVAL-010',
    category: '系统故障',
    priority: 'P1',
    expectedKnowledgeIds: ['KB-API-500', 'KB-OPS-003'],
    topKStatus: 'hit',
    citationStatus: 'expected sources cited',
    failureReason: '无',
    reviewRequired: true
  },
  {
    id: 'EVAL-015',
    category: '流程咨询',
    priority: 'P3',
    expectedKnowledgeIds: [],
    topKStatus: 'miss',
    citationStatus: 'fallback expected but unrelated source retrieved',
    failureReason: 'unexpected_retrieval_for_fallback_case',
    reviewRequired: true
  },
  {
    id: 'EVAL-016',
    category: '系统故障',
    priority: 'P1',
    expectedKnowledgeIds: ['KB-API-500', 'KB-OPS-003'],
    topKStatus: 'review',
    citationStatus: 'ambiguous login keyword added account source',
    failureReason: 'citation_contains_unexpected_source',
    reviewRequired: true
  }
]

export const recentTicketRuns: TicketRunRow[] = [
  {
    ticketId: 'DEMO-0005',
    caseId: 'EVAL-010',
    category: '系统故障',
    priority: 'P1',
    retrievalStatus: 'Top-K hit',
    citationStatus: '2 expected citations',
    reviewStatus: 'Human Review',
    latency: '0.0438 ms',
    providerPath: 'local-rule fallback'
  },
  {
    ticketId: 'DEMO-0003',
    caseId: 'EVAL-009',
    category: '系统故障',
    priority: 'P1',
    retrievalStatus: 'Top-K hit',
    citationStatus: '2 expected citations',
    reviewStatus: 'Human Review',
    latency: '0.0443 ms',
    providerPath: 'local-rule fallback'
  },
  {
    ticketId: 'DEMO-0004',
    caseId: 'EVAL-006',
    category: '数据问题',
    priority: 'P2',
    retrievalStatus: 'Top-K hit',
    citationStatus: '1 expected citation',
    reviewStatus: 'Human Review',
    latency: '0.0452 ms',
    providerPath: 'local-rule fallback'
  },
  {
    ticketId: 'DEMO-0007',
    caseId: 'EVAL-011',
    category: '系统故障',
    priority: 'P2',
    retrievalStatus: 'Top-K hit',
    citationStatus: 'unexpected sources',
    reviewStatus: 'Needs review',
    latency: '0.0485 ms',
    providerPath: 'local-rule fallback'
  },
  {
    ticketId: 'DEMO-FALLBACK',
    caseId: 'EVAL-015',
    category: '流程咨询',
    priority: 'P3',
    retrievalStatus: 'fallback case',
    citationStatus: 'unexpected source',
    reviewStatus: 'Needs review',
    latency: '0.0366 ms',
    providerPath: 'local-rule fallback'
  }
]

export const topKnowledgeSources: KnowledgeSourceRow[] = [
  {
    id: 'KB-OPS-003',
    name: '系统故障超时与 5xx 排查手册',
    matchedTickets: '5 expected cases',
    citationUsage: 'P1/P2 review-heavy',
    keywordStatus: 'keyword chunks available',
    sourceType: 'Ops knowledge'
  },
  {
    id: 'KB-API-500',
    name: '接口 500 错误分层排查手册',
    matchedTickets: '3 expected cases',
    citationUsage: 'multi-source citation',
    keywordStatus: 'traceId / 500 terms',
    sourceType: 'Support knowledge'
  },
  {
    id: 'KB-IAM-002',
    name: '权限申请与最小权限审核流程',
    matchedTickets: '2 expected cases',
    citationUsage: 'RBAC review gate',
    keywordStatus: '403 / role / approval',
    sourceType: 'IAM governance'
  },
  {
    id: 'KB-DATA-004',
    name: '报表数据同步与口径核对流程',
    matchedTickets: '2 expected cases',
    citationUsage: 'data quality cases',
    keywordStatus: 'sync / dashboard / metric',
    sourceType: 'Data knowledge'
  }
]

export const providerStatusItems: ProviderStatusItem[] = [
  {
    label: 'local-rule fallback',
    value: 'active',
    note: 'default no-key local path; no real provider claim',
    tone: 'amber'
  },
  {
    label: 'OpenAI-compatible',
    value: 'optional',
    note: 'environment-variable path exists, not configured in this run',
    tone: 'slate'
  },
  {
    label: 'keyword retrieval',
    value: `Top-${evaluationSnapshot.topK}`,
    note: 'category-aware local scoring, not vector retrieval',
    tone: 'cyan'
  },
  {
    label: 'API key committed',
    value: 'none',
    note: 'no real key is stored or displayed',
    tone: 'green'
  }
]

export const failureAnalysis: FailureAnalysisItem[] = [
  {
    label: 'citation mismatch',
    count: '5',
    note: 'expected source hit, but extra unrelated citation was attached',
    tone: 'amber'
  },
  {
    label: 'fallback mismatch',
    count: '1',
    note: 'no expected source, but keyword retrieval still returned a source',
    tone: 'red'
  },
  {
    label: 'review gate',
    count: '15',
    note: 'high-risk, low-evidence, or policy-required cases routed to review',
    tone: 'violet'
  },
  {
    label: 'provider fallback',
    count: '16 / 16',
    note: 'expected in no-key local run; not a model quality metric',
    tone: 'cyan'
  }
]

export const currentScopeItems = ['keyword retrieval', 'citation gating', 'human review gate', 'local-rule fallback']

export const nextStageItems = [
  'BM25',
  'Vector DB',
  'Hybrid retrieval',
  'Rerank',
  'real provider evaluation',
  'Answer Relevance',
  'Faithfulness',
  'Token Cost'
]

export const reproducibilityCommands = ['py .\\scripts\\evaluate_rag_demo.py', 'cd backend && mvn test']

export const reproducibilityReports = [
  'data/eval/ticket_rag_eval_cases.jsonl',
  'docs/metrics/rag_metrics_latest.json',
  'docs/metrics/rag_metrics_snapshot.md',
  'docs/evaluation/RAG_EVALUATION_PLAN.md'
]

export const boundaryStatements = [
  'synthetic demo dataset',
  'local keyword retrieval',
  'citation gating',
  'local-rule fallback',
  'not real vector RAG',
  'not production data',
  'not real user traffic',
  'no API key committed'
]
