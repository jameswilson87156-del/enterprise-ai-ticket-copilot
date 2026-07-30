<script setup lang="ts">
import { computed, ref } from 'vue'
import RealRunEvidencePanel from '../components/RealRunEvidencePanel.vue'
import { evaluationSnapshot } from '../data/evaluationMetrics'

type TracePriority = 'P1' | 'P2' | 'P3'
type TraceTone = 'blue' | 'cyan' | 'green' | 'amber' | 'red' | 'violet' | 'slate'
type StepStatus = 'completed' | 'active' | 'blocked' | 'waiting' | 'skipped'

interface TraceEvidence {
  knowledgeId: string
  sourceTitle: string
  sourcePath: string
  matchedKeywords: string[]
  keywordScore: number
  citationUsed: boolean
  hitStatus: string
}

interface TraceStep {
  id: string
  name: string
  type: string
  status: StepStatus
  statusLabel: string
  latency: string
  inputSummary: string
  outputSummary: string
  hasEvidence: boolean
  fallbackTriggered: boolean
  reviewRequired: boolean
  tone: TraceTone
}

interface ReviewGate {
  status: string
  required: boolean
  riskReason: string
  reviewerAction: string
  editedPreview: string
  approveState: string
  requestChangesState: string
  rejectState: string
}

interface TraceRun {
  id: string
  traceId: string
  ticketId: string
  title: string
  category: string
  priority: TracePriority
  providerPath: string
  retrievalMode: string
  topK: string
  totalLatency: string
  reviewStatus: string
  boundary: string
  query: string
  rewrittenQuery: string
  fallbackReason: string
  fallbackStrategy: string
  citationPrecisionNote: string
  currentStepId: string
  steps: TraceStep[]
  evidences: TraceEvidence[]
  review: ReviewGate
}

// Showcase demo trace data only. These synthetic traces are for portfolio UI
// demonstration and are not production logs, real user traffic, real LLM calls,
// or proof of a complete online Agent / Trace runtime.
const traceRuns: TraceRun[] = [
  {
    id: 'RUN-DEMO-API-005',
    traceId: 'TRACE-DEMO-API-005',
    ticketId: 'DEMO-API-005',
    title: '报价接口返回 500，销售无法生成报价单',
    category: '系统故障 / API 5xx',
    priority: 'P1',
    providerPath: 'local-rule fallback',
    retrievalMode: 'keyword retrieval + citation gating',
    topK: `Top-${evaluationSnapshot.topK}`,
    totalLatency: '0.066 ms',
    reviewStatus: 'Human Review required',
    boundary: 'synthetic demo / local-rule fallback',
    query: 'quote-center HTTP 500 PricePolicyService traceId 报价失败',
    rewrittenQuery: '报价接口 500 traceId PricePolicyService 灰度发布',
    fallbackReason: 'API_KEY_MISSING',
    fallbackStrategy: 'skip provider call and use local-rule template draft',
    citationPrecisionNote: 'expected citations attached; still requires human review for rollback risk',
    currentStepId: 'provider-call',
    steps: [
      {
        id: 'ticket-input',
        name: 'Ticket Input',
        type: 'intake',
        status: 'completed',
        statusLabel: 'received',
        latency: '0.001 ms',
        inputSummary: 'P1 quote-center 500 with traceId and gray release context.',
        outputSummary: 'Ticket context normalized for local analysis.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: false,
        tone: 'blue'
      },
      {
        id: 'query-rewrite',
        name: 'Query Rewrite',
        type: 'local normalize',
        status: 'completed',
        statusLabel: 'normalized',
        latency: '0.004 ms',
        inputSummary: 'Title, logs, affected system, and business impact.',
        outputSummary: '500 / traceId / PricePolicyService / gray release terms.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: false,
        tone: 'cyan'
      },
      {
        id: 'retrieval',
        name: 'Retrieval',
        type: 'keyword retrieval',
        status: 'completed',
        statusLabel: 'Top-K hit',
        latency: evaluationSnapshot.avgRetrievalLatency,
        inputSummary: 'Category-aware local keyword scoring.',
        outputSummary: 'KB-API-500 and KB-OPS-003 ranked into Top-K.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: false,
        tone: 'green'
      },
      {
        id: 'citation-attach',
        name: 'Citation Attach',
        type: 'citation gating',
        status: 'completed',
        statusLabel: '2 citations',
        latency: '0.006 ms',
        inputSummary: 'Top-K sources and draft evidence requirements.',
        outputSummary: 'Two expected citations attached to the draft.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'violet'
      },
      {
        id: 'prompt-build',
        name: 'Prompt Build',
        type: 'rule template',
        status: 'completed',
        statusLabel: 'template built',
        latency: '0.011 ms',
        inputSummary: 'Ticket context, citation ids, risk notes.',
        outputSummary: 'Local template draft prepared; no remote prompt sent.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'blue'
      },
      {
        id: 'provider-call',
        name: 'Provider Call',
        type: 'provider fallback',
        status: 'active',
        statusLabel: 'skipped / local-rule fallback',
        latency: '0 ms',
        inputSummary: 'OpenAI-compatible path checked with no committed API key.',
        outputSummary: 'real provider not configured; no API key committed; local-rule fallback used.',
        hasEvidence: true,
        fallbackTriggered: true,
        reviewRequired: true,
        tone: 'amber'
      },
      {
        id: 'answer-draft',
        name: 'Answer Draft',
        type: 'template draft',
        status: 'completed',
        statusLabel: 'draft ready',
        latency: '0.010 ms',
        inputSummary: 'Troubleshooting plan and citation evidence.',
        outputSummary: 'Draft reply generated for support staff review.',
        hasEvidence: true,
        fallbackTriggered: true,
        reviewRequired: true,
        tone: 'green'
      },
      {
        id: 'human-review',
        name: 'Human Review',
        type: 'review gate',
        status: 'waiting',
        statusLabel: 'review required',
        latency: 'pending',
        inputSummary: 'P1 impact, rollback risk, and cited evidence.',
        outputSummary: 'Awaiting SRE/support decision before any status change.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'red'
      }
    ],
    evidences: [
      {
        knowledgeId: 'KB-API-500',
        sourceTitle: '接口 500 错误分层排查手册',
        sourcePath: 'support-knowledge/api/500-troubleshooting',
        matchedKeywords: ['500', 'traceId', '接口异常', '回滚确认'],
        keywordScore: 92,
        citationUsed: true,
        hitStatus: 'hit'
      },
      {
        knowledgeId: 'KB-OPS-003',
        sourceTitle: '系统故障超时与 5xx 排查手册',
        sourcePath: 'ops/runbook/5xx-timeout',
        matchedKeywords: ['灰度发布', '依赖服务', '降级策略'],
        keywordScore: 84,
        citationUsed: true,
        hitStatus: 'hit'
      }
    ],
    review: {
      status: '待人工复核',
      required: true,
      riskReason: 'P1 业务影响 + 可能涉及灰度回滚或降级，不能由系统自动执行。',
      reviewerAction: '请求值班 SRE 补充 traceId 关联日志后，再决定 approve / request changes / reject。',
      editedPreview: '请保留失败报价单和 traceId。支持人员确认处理动作后，会同步修复进度与临时方案。',
      approveState: '待人工确认',
      requestChangesState: '建议补充日志',
      rejectState: '风险过高时可拒绝'
    }
  },
  {
    id: 'RUN-DEMO-SSO-001',
    traceId: 'TRACE-DEMO-SSO-001',
    ticketId: 'DEMO-SSO-001',
    title: '员工 SSO 登录失败并触发 MFA 重试',
    category: '账号与认证 / SSO',
    priority: 'P1',
    providerPath: 'local-rule fallback',
    retrievalMode: 'keyword retrieval + citation gating',
    topK: `Top-${evaluationSnapshot.topK}`,
    totalLatency: '0.063 ms',
    reviewStatus: 'Human Review required',
    boundary: 'synthetic demo / no real user identity',
    query: 'SSO MFA 401 challenge expired 登录失败',
    rewrittenQuery: 'SSO login failure MFA retry 401 identity sync',
    fallbackReason: 'PROVIDER_DISABLED',
    fallbackStrategy: 'local identity-support template',
    citationPrecisionNote: 'citation precision warning: login keywords may add account-source ambiguity',
    currentStepId: 'citation-attach',
    steps: [
      {
        id: 'ticket-input',
        name: 'Ticket Input',
        type: 'intake',
        status: 'completed',
        statusLabel: 'received',
        latency: '0.001 ms',
        inputSummary: 'Multiple employees report SSO / MFA login failure.',
        outputSummary: 'Authentication ticket context captured.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: false,
        tone: 'blue'
      },
      {
        id: 'query-rewrite',
        name: 'Query Rewrite',
        type: 'local normalize',
        status: 'completed',
        statusLabel: 'normalized',
        latency: '0.003 ms',
        inputSummary: 'SSO, MFA, 401, challenge expired.',
        outputSummary: 'Identity and MFA terms extracted.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: false,
        tone: 'cyan'
      },
      {
        id: 'retrieval',
        name: 'Retrieval',
        type: 'keyword retrieval',
        status: 'completed',
        statusLabel: 'Top-K hit',
        latency: '0.0443 ms',
        inputSummary: 'Local keyword scoring for IAM knowledge.',
        outputSummary: 'SSO/MFA and account source references ranked.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'green'
      },
      {
        id: 'citation-attach',
        name: 'Citation Attach',
        type: 'citation gating',
        status: 'active',
        statusLabel: 'precision warning',
        latency: '0.005 ms',
        inputSummary: 'Expected SSO/MFA source plus ambiguous login keyword match.',
        outputSummary: 'Citation attached with precision warning.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'amber'
      },
      {
        id: 'prompt-build',
        name: 'Prompt Build',
        type: 'rule template',
        status: 'completed',
        statusLabel: 'template built',
        latency: '0.010 ms',
        inputSummary: 'Citation notes and identity risk controls.',
        outputSummary: 'Draft stays in review; no MFA reset suggested automatically.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'blue'
      },
      {
        id: 'provider-call',
        name: 'Provider Call',
        type: 'provider fallback',
        status: 'skipped',
        statusLabel: 'skipped / local-rule fallback',
        latency: '0 ms',
        inputSummary: 'OpenAI-compatible optional path not configured.',
        outputSummary: 'real provider not configured; no API key committed; local-rule fallback used.',
        hasEvidence: true,
        fallbackTriggered: true,
        reviewRequired: true,
        tone: 'amber'
      },
      {
        id: 'answer-draft',
        name: 'Answer Draft',
        type: 'template draft',
        status: 'completed',
        statusLabel: 'draft ready',
        latency: '0.009 ms',
        inputSummary: 'SSO/MFA evidence and user-facing status note.',
        outputSummary: 'Draft asks for IAM confirmation; no bypass action.',
        hasEvidence: true,
        fallbackTriggered: true,
        reviewRequired: true,
        tone: 'green'
      },
      {
        id: 'human-review',
        name: 'Human Review',
        type: 'review gate',
        status: 'waiting',
        statusLabel: 'review required',
        latency: 'pending',
        inputSummary: 'Identity risk and potentially broad employee impact.',
        outputSummary: 'IAM owner must confirm action before update.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'red'
      }
    ],
    evidences: [
      {
        knowledgeId: 'KB-SSO-001',
        sourceTitle: 'SSO 登录失败与 MFA 排查流程',
        sourcePath: 'iam/sso/mfa-login-failure',
        matchedKeywords: ['SSO', 'MFA', '401', 'challenge expired'],
        keywordScore: 90,
        citationUsed: true,
        hitStatus: 'hit'
      },
      {
        knowledgeId: 'KB-IAM-002',
        sourceTitle: '权限申请与最小权限审核流程',
        sourcePath: 'iam/access-review/minimum-privilege',
        matchedKeywords: ['login', 'identity', 'approval'],
        keywordScore: 63,
        citationUsed: false,
        hitStatus: 'warning'
      }
    ],
    review: {
      status: '待 IAM 值班确认',
      required: true,
      riskReason: '登录入口影响面可能扩大，不能绕过 MFA 或批量重置认证状态。',
      reviewerAction: '确认身份源同步窗口、MFA 服务状态和影响范围后再处理。',
      editedPreview: '我们已定位到 SSO / MFA 链路，会在人工确认后同步下一步处理。',
      approveState: '待 IAM 确认',
      requestChangesState: '补充身份源批次',
      rejectState: '禁止绕过认证'
    }
  },
  {
    id: 'RUN-DEMO-RBAC-002',
    traceId: 'TRACE-DEMO-RBAC-002',
    ticketId: 'DEMO-RBAC-002',
    title: '审批完成后仍无法访问 HR 报表',
    category: '权限问题 / RBAC',
    priority: 'P2',
    providerPath: 'local-rule fallback',
    retrievalMode: 'keyword retrieval + citation gating',
    topK: `Top-${evaluationSnapshot.topK}`,
    totalLatency: '0.059 ms',
    reviewStatus: 'Human Review required',
    boundary: 'synthetic demo / no real permission operation',
    query: 'RBAC 403 审批完成 报表无权限',
    rewrittenQuery: 'HR report access 403 approval completed role sync',
    fallbackReason: 'API_KEY_MISSING',
    fallbackStrategy: 'local RBAC review template',
    citationPrecisionNote: 'expected RBAC source cited; approval still required',
    currentStepId: 'human-review',
    steps: [
      {
        id: 'ticket-input',
        name: 'Ticket Input',
        type: 'intake',
        status: 'completed',
        statusLabel: 'received',
        latency: '0.001 ms',
        inputSummary: 'HRBP approved request but report access returns 403.',
        outputSummary: 'Permission-sensitive ticket marked for review.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'blue'
      },
      {
        id: 'query-rewrite',
        name: 'Query Rewrite',
        type: 'local normalize',
        status: 'completed',
        statusLabel: 'normalized',
        latency: '0.003 ms',
        inputSummary: '403, approval completed, HR report, role sync.',
        outputSummary: 'RBAC and approval keywords extracted.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'cyan'
      },
      {
        id: 'retrieval',
        name: 'Retrieval',
        type: 'keyword retrieval',
        status: 'completed',
        statusLabel: 'Top-K hit',
        latency: '0.0418 ms',
        inputSummary: 'Permission category and RBAC keywords.',
        outputSummary: 'KB-IAM-ROLE ranked as primary evidence.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'green'
      },
      {
        id: 'citation-attach',
        name: 'Citation Attach',
        type: 'citation gating',
        status: 'completed',
        statusLabel: '1 citation',
        latency: '0.004 ms',
        inputSummary: 'RBAC source and approval audit terms.',
        outputSummary: 'Citation attached; no direct grant action.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'violet'
      },
      {
        id: 'prompt-build',
        name: 'Prompt Build',
        type: 'rule template',
        status: 'completed',
        statusLabel: 'template built',
        latency: '0.010 ms',
        inputSummary: 'Approval record, role sync, audit boundary.',
        outputSummary: 'Draft asks for approval id and target role verification.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'blue'
      },
      {
        id: 'provider-call',
        name: 'Provider Call',
        type: 'provider fallback',
        status: 'skipped',
        statusLabel: 'skipped / local-rule fallback',
        latency: '0 ms',
        inputSummary: 'Provider disabled for local demo.',
        outputSummary: 'real provider not configured; no API key committed; local-rule fallback used.',
        hasEvidence: true,
        fallbackTriggered: true,
        reviewRequired: true,
        tone: 'amber'
      },
      {
        id: 'answer-draft',
        name: 'Answer Draft',
        type: 'template draft',
        status: 'completed',
        statusLabel: 'draft ready',
        latency: '0.009 ms',
        inputSummary: 'RBAC citation and risk policy.',
        outputSummary: 'Draft requests approval number and role sync check.',
        hasEvidence: true,
        fallbackTriggered: true,
        reviewRequired: true,
        tone: 'green'
      },
      {
        id: 'human-review',
        name: 'Human Review',
        type: 'review gate',
        status: 'active',
        statusLabel: 'review required',
        latency: 'pending',
        inputSummary: 'Sensitive HR report access and approval evidence.',
        outputSummary: 'No automatic authorization; reviewer must decide.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'red'
      }
    ],
    evidences: [
      {
        knowledgeId: 'KB-IAM-ROLE',
        sourceTitle: '权限配置问题与 RBAC 核验流程',
        sourcePath: 'iam/rbac/role-sync-checklist',
        matchedKeywords: ['RBAC', '403', '审批', '角色同步'],
        keywordScore: 88,
        citationUsed: true,
        hitStatus: 'hit'
      }
    ],
    review: {
      status: '待权限支持确认',
      required: true,
      riskReason: 'HR 报表涉及敏感数据，权限补齐必须保留审批依据和审计记录。',
      reviewerAction: '人工核对审批单、同步批次和目标角色后再处理。',
      editedPreview: '请提供审批单号与报表路径，我们会人工核对权限同步记录。',
      approveState: '待审批单核对',
      requestChangesState: '补充审批依据',
      rejectState: '缺少审批则拒绝'
    }
  },
  {
    id: 'RUN-DEMO-SYNC-003',
    traceId: 'TRACE-DEMO-SYNC-003',
    ticketId: 'DEMO-SYNC-003',
    title: 'CRM 数据同步延迟导致指标看板口径不一致',
    category: '数据问题 / 同步延迟',
    priority: 'P2',
    providerPath: 'local-rule fallback',
    retrievalMode: 'keyword retrieval + citation gating',
    topK: `Top-${evaluationSnapshot.topK}`,
    totalLatency: '0.065 ms',
    reviewStatus: 'Data support review',
    boundary: 'synthetic demo / no production data fix',
    query: 'CRM renewal dashboard 数据同步延迟 指标口径',
    rewrittenQuery: 'CRM data sync delay renewal dashboard metric mismatch',
    fallbackReason: 'PROVIDER_DISABLED',
    fallbackStrategy: 'local data quality template',
    citationPrecisionNote: 'expected data source cited; data repair remains manual',
    currentStepId: 'retrieval',
    steps: [
      {
        id: 'ticket-input',
        name: 'Ticket Input',
        type: 'intake',
        status: 'completed',
        statusLabel: 'received',
        latency: '0.001 ms',
        inputSummary: 'CRM dashboard differs from detail table by 6.25%.',
        outputSummary: 'Data quality context captured.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: false,
        tone: 'blue'
      },
      {
        id: 'query-rewrite',
        name: 'Query Rewrite',
        type: 'local normalize',
        status: 'completed',
        statusLabel: 'normalized',
        latency: '0.004 ms',
        inputSummary: 'Data sync, dashboard, renewal metric.',
        outputSummary: 'Sync delay and metric terms extracted.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: false,
        tone: 'cyan'
      },
      {
        id: 'retrieval',
        name: 'Retrieval',
        type: 'keyword retrieval',
        status: 'active',
        statusLabel: 'Top-K hit',
        latency: '0.0452 ms',
        inputSummary: 'Data issue category and sync keywords.',
        outputSummary: 'KB-DATA-004 selected as primary evidence.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'green'
      },
      {
        id: 'citation-attach',
        name: 'Citation Attach',
        type: 'citation gating',
        status: 'completed',
        statusLabel: '1 citation',
        latency: '0.005 ms',
        inputSummary: 'Top-K data runbook evidence.',
        outputSummary: 'Citation attached to draft response.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'violet'
      },
      {
        id: 'prompt-build',
        name: 'Prompt Build',
        type: 'rule template',
        status: 'completed',
        statusLabel: 'template built',
        latency: '0.011 ms',
        inputSummary: 'Sync delay, ETL batch, review boundary.',
        outputSummary: 'Draft recommends manual ETL batch check.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'blue'
      },
      {
        id: 'provider-call',
        name: 'Provider Call',
        type: 'provider fallback',
        status: 'skipped',
        statusLabel: 'skipped / local-rule fallback',
        latency: '0 ms',
        inputSummary: 'Provider path not configured.',
        outputSummary: 'real provider not configured; no API key committed; local-rule fallback used.',
        hasEvidence: true,
        fallbackTriggered: true,
        reviewRequired: true,
        tone: 'amber'
      },
      {
        id: 'answer-draft',
        name: 'Answer Draft',
        type: 'template draft',
        status: 'completed',
        statusLabel: 'draft ready',
        latency: '0.010 ms',
        inputSummary: 'Data citation and business impact.',
        outputSummary: 'Draft explains sync delay and asks data support to confirm.',
        hasEvidence: true,
        fallbackTriggered: true,
        reviewRequired: true,
        tone: 'green'
      },
      {
        id: 'human-review',
        name: 'Human Review',
        type: 'review gate',
        status: 'waiting',
        statusLabel: 'review required',
        latency: 'pending',
        inputSummary: 'Potential data replay or ETL repair.',
        outputSummary: 'Awaiting data support before any repair.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'amber'
      }
    ],
    evidences: [
      {
        knowledgeId: 'KB-DATA-004',
        sourceTitle: '报表数据同步与口径核对流程',
        sourcePath: 'data/runbook/sync-metric-check',
        matchedKeywords: ['数据同步', '指标口径', '补跑', '批次'],
        keywordScore: 86,
        citationUsed: true,
        hitStatus: 'hit'
      }
    ],
    review: {
      status: '待数据支持确认',
      required: true,
      riskReason: '补跑或改指标表会影响业务报表，需要人工确认。',
      reviewerAction: '请求数据支持提供 ETL 批次和补跑计划。',
      editedPreview: '我们会先确认同步批次，再同步是否需要人工补跑。',
      approveState: '待数据支持确认',
      requestChangesState: '补充 ETL 批次',
      rejectState: '缺证据不执行'
    }
  },
  {
    id: 'RUN-DEMO-FALLBACK-004',
    traceId: 'TRACE-DEMO-FALLBACK-004',
    ticketId: 'DEMO-FALLBACK-004',
    title: '会议室设备无法投屏，知识库暂无可靠来源',
    category: '流程咨询 / 知识缺失',
    priority: 'P3',
    providerPath: 'local-rule fallback',
    retrievalMode: 'keyword retrieval + expected miss',
    topK: `Top-${evaluationSnapshot.topK}`,
    totalLatency: '0.052 ms',
    reviewStatus: 'Needs Review',
    boundary: 'synthetic demo / knowledge-missing fallback',
    query: '会议室 投屏 设备断连 无知识库来源',
    rewrittenQuery: 'meeting room cast device disconnect no reliable knowledge',
    fallbackReason: 'NO_RELIABLE_SOURCE',
    fallbackStrategy: 'fallback draft asks for more context',
    citationPrecisionNote: 'hit / miss status: expected miss; citation blocked',
    currentStepId: 'citation-attach',
    steps: [
      {
        id: 'ticket-input',
        name: 'Ticket Input',
        type: 'intake',
        status: 'completed',
        statusLabel: 'received',
        latency: '0.001 ms',
        inputSummary: 'Meeting room casting device disconnects repeatedly.',
        outputSummary: 'Low-priority support context captured.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: false,
        tone: 'blue'
      },
      {
        id: 'query-rewrite',
        name: 'Query Rewrite',
        type: 'local normalize',
        status: 'completed',
        statusLabel: 'normalized',
        latency: '0.003 ms',
        inputSummary: '投屏、设备断连、会议室.',
        outputSummary: 'Device support keywords extracted.',
        hasEvidence: true,
        fallbackTriggered: false,
        reviewRequired: false,
        tone: 'cyan'
      },
      {
        id: 'retrieval',
        name: 'Retrieval',
        type: 'keyword retrieval',
        status: 'completed',
        statusLabel: 'expected miss',
        latency: '0.0366 ms',
        inputSummary: 'No reliable device runbook expected.',
        outputSummary: 'No reliable knowledge source should be cited.',
        hasEvidence: true,
        fallbackTriggered: true,
        reviewRequired: true,
        tone: 'amber'
      },
      {
        id: 'citation-attach',
        name: 'Citation Attach',
        type: 'citation gating',
        status: 'blocked',
        statusLabel: 'citation blocked',
        latency: '0.002 ms',
        inputSummary: 'Low keyword score and no reliable source.',
        outputSummary: 'Citation skipped; fallback reason recorded.',
        hasEvidence: true,
        fallbackTriggered: true,
        reviewRequired: true,
        tone: 'red'
      },
      {
        id: 'prompt-build',
        name: 'Prompt Build',
        type: 'fallback template',
        status: 'completed',
        statusLabel: 'fallback draft',
        latency: '0.009 ms',
        inputSummary: 'Knowledge missing flag and request-for-details template.',
        outputSummary: 'Draft asks for room, device model, and screenshot.',
        hasEvidence: true,
        fallbackTriggered: true,
        reviewRequired: true,
        tone: 'blue'
      },
      {
        id: 'provider-call',
        name: 'Provider Call',
        type: 'provider fallback',
        status: 'skipped',
        statusLabel: 'skipped / local-rule fallback',
        latency: '0 ms',
        inputSummary: 'No remote provider configured for demo.',
        outputSummary: 'real provider not configured; no API key committed; local-rule fallback used.',
        hasEvidence: true,
        fallbackTriggered: true,
        reviewRequired: true,
        tone: 'amber'
      },
      {
        id: 'answer-draft',
        name: 'Answer Draft',
        type: 'fallback draft',
        status: 'completed',
        statusLabel: 'needs context',
        latency: '0.008 ms',
        inputSummary: 'No citation available; ask for more information.',
        outputSummary: 'Draft avoids confident diagnosis.',
        hasEvidence: false,
        fallbackTriggered: true,
        reviewRequired: true,
        tone: 'amber'
      },
      {
        id: 'human-review',
        name: 'Human Review',
        type: 'review gate',
        status: 'waiting',
        statusLabel: 'review required',
        latency: 'pending',
        inputSummary: 'Missing reliable citation and device-specific context.',
        outputSummary: 'Reviewer can request changes or create a knowledge gap item.',
        hasEvidence: false,
        fallbackTriggered: false,
        reviewRequired: true,
        tone: 'red'
      }
    ],
    evidences: [
      {
        knowledgeId: 'NO-RELIABLE-SOURCE',
        sourceTitle: '未找到可靠知识来源',
        sourcePath: 'fallback/no-citation-attached',
        matchedKeywords: ['会议室', '投屏', '设备断连'],
        keywordScore: 34,
        citationUsed: false,
        hitStatus: 'miss'
      }
    ],
    review: {
      status: '待补充信息',
      required: true,
      riskReason: '知识库缺少可靠来源，不能给出确定性处理结论。',
      reviewerAction: '请求补充会议室、设备型号和断连截图，必要时沉淀新知识条目。',
      editedPreview: '当前信息不足以给出确定处理结论，请补充会议室、设备型号和断连截图。',
      approveState: '不建议直接通过',
      requestChangesState: '请求补充信息',
      rejectState: '拒绝无来源草稿'
    }
  }
]

const selectedRunId = ref(traceRuns[0].id)
const selectedStepId = ref(traceRuns[0].currentStepId)

const selectedRun = computed(() => traceRuns.find((run) => run.id === selectedRunId.value) ?? traceRuns[0])
const selectedStep = computed(() => selectedRun.value.steps.find((step) => step.id === selectedStepId.value) ?? selectedRun.value.steps[0])

const overviewItems = computed(() => [
  { label: 'Run ID', value: selectedRun.value.id },
  { label: 'Ticket ID', value: selectedRun.value.ticketId },
  { label: 'Category', value: selectedRun.value.category },
  { label: 'Priority', value: selectedRun.value.priority },
  { label: 'Provider Path', value: selectedRun.value.providerPath },
  { label: 'Retrieval Mode', value: selectedRun.value.retrievalMode },
  { label: 'Top-K', value: selectedRun.value.topK },
  { label: 'Total Latency', value: selectedRun.value.totalLatency },
  { label: 'Review Status', value: selectedRun.value.reviewStatus },
  { label: 'Boundary', value: selectedRun.value.boundary }
])

const selectedStepDebugJson = computed(() =>
  JSON.stringify(
    {
      runId: selectedRun.value.id,
      traceId: selectedRun.value.traceId,
      ticketId: selectedRun.value.ticketId,
      currentStep: selectedStep.value.name,
      stepType: selectedStep.value.type,
      providerPath: selectedRun.value.providerPath,
      retrievalMode: selectedRun.value.retrievalMode,
      topK: selectedRun.value.topK,
      latencyMs: selectedStep.value.latency,
      citationIds: selectedRun.value.evidences.filter((evidence) => evidence.citationUsed).map((evidence) => evidence.knowledgeId),
      reviewRequired: selectedStep.value.reviewRequired,
      fallbackReason: selectedRun.value.fallbackReason
    },
    null,
    2
  )
)

const runStats = [
  { label: 'synthetic traces', value: String(traceRuns.length), note: 'showcase demo data' },
  { label: 'provider fallback', value: '100%', note: 'no real API key used' },
  { label: 'review gates', value: '5 / 5', note: 'human review visible' }
]

function selectRun(run: TraceRun) {
  selectedRunId.value = run.id
  selectedStepId.value = run.currentStepId
}

function selectStep(step: TraceStep) {
  selectedStepId.value = step.id
}
</script>

<template>
  <section class="trace-timeline" data-screenshot="trace-timeline" aria-label="Trace Timeline">
    <RealRunEvidencePanel />
    <header class="trace-timeline__hero">
      <div class="trace-timeline__intro">
        <p class="trace-timeline__eyebrow">AI Copilot Run Trace · RAG Evidence Timeline</p>
        <h1>Trace Timeline</h1>
        <p class="trace-timeline__subtitle">
          AI 工单处理运行链路 · Retrieval · Citation · Provider Fallback · Human Review
        </p>
        <p class="trace-timeline__summary">
          基于 synthetic demo 工单，展示 local-rule fallback、keyword retrieval、citation gating 与人工复核门禁的运行链路。
        </p>
        <div class="trace-timeline__tags" aria-label="当前演示边界">
          <span>synthetic demo trace</span>
          <span>local-rule fallback</span>
          <span>keyword retrieval</span>
          <span>citation gating</span>
          <span>未使用真实 API Key</span>
        </div>
      </div>

      <aside class="trace-timeline__hero-card" aria-label="Run Summary">
        <div class="trace-timeline__hero-card-title">
          <span>Run Overview</span>
          <strong>{{ selectedRun.traceId }}</strong>
        </div>
        <div class="trace-timeline__stat-row">
          <section v-for="item in runStats" :key="item.label">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
            <small>{{ item.note }}</small>
          </section>
        </div>
      </aside>
    </header>

    <section class="trace-timeline__overview" aria-label="Run Overview">
      <div v-for="item in overviewItems" :key="item.label">
        <dt>{{ item.label }}</dt>
        <dd>{{ item.value }}</dd>
      </div>
    </section>

    <section class="trace-timeline__workspace" aria-label="Trace Timeline 工作区">
      <aside class="trace-timeline__panel trace-timeline__runs" aria-label="Run List">
        <div class="trace-timeline__panel-heading">
          <div>
            <p class="trace-timeline__eyebrow">Run List / Trace Summary</p>
            <h2>运行链路</h2>
          </div>
          <span>{{ traceRuns.length }} runs</span>
        </div>

        <div class="trace-timeline__run-list">
          <button
            v-for="run in traceRuns"
            :key="run.id"
            type="button"
            class="trace-timeline__run"
            :class="{ 'trace-timeline__run--active': run.id === selectedRun.id }"
            @click="selectRun(run)"
          >
            <span class="trace-timeline__run-id">{{ run.ticketId }}</span>
            <strong>{{ run.title }}</strong>
            <small>{{ run.category }} · {{ run.priority }}</small>
            <em>{{ run.reviewStatus }}</em>
          </button>
        </div>

        <section class="trace-timeline__boundary-card" aria-label="Demo Boundary">
          <strong>Boundary</strong>
          <p>
            当前页面使用 synthetic demo trace、local-rule fallback、keyword retrieval 与 citation gating，仅用于作品集演示，不代表真实生产链路、真实 LLM 调用或真实用户数据。
          </p>
        </section>
      </aside>

      <main class="trace-timeline__panel trace-timeline__steps" aria-label="Step Timeline">
        <div class="trace-timeline__panel-heading">
          <div>
            <p class="trace-timeline__eyebrow">Trace Timeline</p>
            <h2>Step Timeline</h2>
          </div>
          <span>{{ selectedRun.totalLatency }}</span>
        </div>

        <div class="trace-timeline__step-list">
          <button
            v-for="(step, index) in selectedRun.steps"
            :key="step.id"
            type="button"
            class="trace-timeline__step"
            :class="[
              `trace-timeline__step--${step.status}`,
              `trace-timeline__step--${step.tone}`,
              { 'trace-timeline__step--selected': step.id === selectedStep.id }
            ]"
            @click="selectStep(step)"
          >
            <span class="trace-timeline__step-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <div class="trace-timeline__step-main">
              <span class="trace-timeline__step-kicker">{{ step.type }}</span>
              <strong>{{ step.name }}</strong>
              <p>{{ step.inputSummary }}</p>
              <small>{{ step.outputSummary }}</small>
            </div>
            <aside class="trace-timeline__step-meta">
              <b>{{ step.statusLabel }}</b>
              <em>{{ step.latency }}</em>
              <span v-if="step.hasEvidence">evidence</span>
              <span v-if="step.fallbackTriggered">fallback</span>
              <span v-if="step.reviewRequired">review</span>
            </aside>
          </button>
        </div>
      </main>

      <aside class="trace-timeline__detail" aria-label="Step Detail / Evidence / Raw JSON">
        <section class="trace-timeline__panel trace-timeline__detail-card">
          <div class="trace-timeline__panel-heading">
            <div>
              <p class="trace-timeline__eyebrow">Step Detail</p>
              <h2>{{ selectedStep.name }}</h2>
            </div>
            <span :data-tone="selectedStep.tone">{{ selectedStep.statusLabel }}</span>
          </div>

          <dl class="trace-timeline__detail-grid">
            <div><dt>step type</dt><dd>{{ selectedStep.type }}</dd></div>
            <div><dt>latency</dt><dd>{{ selectedStep.latency }}</dd></div>
            <div><dt>has evidence</dt><dd>{{ selectedStep.hasEvidence ? 'yes' : 'no' }}</dd></div>
            <div><dt>review required</dt><dd>{{ selectedStep.reviewRequired ? 'yes' : 'no' }}</dd></div>
          </dl>

          <section class="trace-timeline__provider-note" aria-label="Provider 路径">
            <strong>Provider Call skipped / local-rule fallback</strong>
            <p>real provider not configured · no API key committed · local-rule fallback used</p>
            <small>Fallback reason: {{ selectedRun.fallbackReason }} · {{ selectedRun.fallbackStrategy }}</small>
          </section>

          <section class="trace-timeline__inline-review" aria-label="Human Review gate summary">
            <strong>Human Review gate</strong>
            <p>{{ selectedRun.review.status }} · {{ selectedRun.review.riskReason }}</p>
          </section>

          <section class="trace-timeline__debug-inline" aria-label="Raw JSON preview">
            <div>
              <strong>Raw JSON / Debug Detail</strong>
              <span>current step</span>
            </div>
            <pre><code>{{ selectedStepDebugJson }}</code></pre>
          </section>
        </section>

        <section class="trace-timeline__panel trace-timeline__evidence" aria-label="Retrieval / Citation Evidence">
          <div class="trace-timeline__panel-heading">
            <div>
              <p class="trace-timeline__eyebrow">检索证据 / 引用证据</p>
              <h2>Retrieval / Citation Evidence</h2>
            </div>
            <span>{{ selectedRun.topK }}</span>
          </div>

          <dl class="trace-timeline__query-box">
            <div><dt>Query</dt><dd>{{ selectedRun.query }}</dd></div>
            <div><dt>Rewritten</dt><dd>{{ selectedRun.rewrittenQuery }}</dd></div>
          </dl>

          <div class="trace-timeline__evidence-list">
            <article v-for="evidence in selectedRun.evidences" :key="evidence.knowledgeId" class="trace-timeline__evidence-item">
              <div>
                <strong>{{ evidence.knowledgeId }}</strong>
                <span>{{ evidence.hitStatus }}</span>
              </div>
              <p>{{ evidence.sourceTitle }}</p>
              <small>{{ evidence.sourcePath }}</small>
              <div class="trace-timeline__keywords">
                <em v-for="keyword in evidence.matchedKeywords" :key="keyword">{{ keyword }}</em>
              </div>
              <dl>
                <div><dt>keyword score</dt><dd>{{ evidence.keywordScore }}</dd></div>
                <div><dt>citation used</dt><dd>{{ evidence.citationUsed ? 'yes' : 'no' }}</dd></div>
              </dl>
            </article>
          </div>
          <p class="trace-timeline__soft-note">{{ selectedRun.citationPrecisionNote }}</p>
        </section>

        <section class="trace-timeline__panel trace-timeline__review" aria-label="Human Review Gate">
          <div class="trace-timeline__panel-heading">
            <div>
              <p class="trace-timeline__eyebrow">人工复核门禁</p>
              <h2>Human Review gate</h2>
            </div>
            <span>{{ selectedRun.review.status }}</span>
          </div>
          <dl class="trace-timeline__review-pairs">
            <div><dt>review required</dt><dd>{{ selectedRun.review.required ? 'yes' : 'no' }}</dd></div>
            <div><dt>risk reason</dt><dd>{{ selectedRun.review.riskReason }}</dd></div>
            <div><dt>review action</dt><dd>{{ selectedRun.review.reviewerAction }}</dd></div>
          </dl>
          <blockquote>{{ selectedRun.review.editedPreview }}</blockquote>
          <div class="trace-timeline__review-actions" aria-label="demo review action states">
            <button type="button">{{ selectedRun.review.approveState }}</button>
            <button type="button">{{ selectedRun.review.requestChangesState }}</button>
            <button type="button">{{ selectedRun.review.rejectState }}</button>
          </div>
          <small>以上是 demo action state，不会提交真实线上操作。</small>
        </section>

        <section class="trace-timeline__panel trace-timeline__json" aria-label="Raw JSON / Debug Detail">
          <div class="trace-timeline__panel-heading">
            <div>
              <p class="trace-timeline__eyebrow">Raw JSON</p>
              <h2>Debug Detail</h2>
            </div>
            <span>current step</span>
          </div>
          <pre><code>{{ selectedStepDebugJson }}</code></pre>
        </section>
      </aside>
    </section>
  </section>
</template>

<style scoped>
.trace-timeline {
  --trace-panel: rgba(12, 23, 38, 0.9);
  --trace-panel-strong: rgba(16, 31, 51, 0.95);
  --trace-border: rgba(151, 180, 214, 0.16);
  --trace-border-strong: rgba(91, 141, 239, 0.28);
  --trace-text: #eef5ff;
  --trace-secondary: #c7d5ea;
  --trace-muted: #7e91ab;
  --trace-blue: #3d7cff;
  --trace-cyan: #21c7d9;
  --trace-green: #2bd88f;
  --trace-amber: #ffb45c;
  --trace-red: #ff5c7a;
  --trace-violet: #8b7cf6;
  display: grid;
  gap: 10px;
  min-width: 0;
  color: var(--trace-text);
}

.trace-timeline *,
.trace-timeline *::before,
.trace-timeline *::after {
  box-sizing: border-box;
}

.trace-timeline h1,
.trace-timeline h2,
.trace-timeline p,
.trace-timeline dl,
.trace-timeline dd,
.trace-timeline blockquote {
  margin: 0;
}

.trace-timeline button {
  font: inherit;
}

.trace-timeline__hero,
.trace-timeline__overview,
.trace-timeline__panel {
  border: 1px solid var(--trace-border);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(16, 31, 51, 0.9), rgba(8, 17, 31, 0.95)),
    var(--trace-panel);
  box-shadow: 0 16px 38px rgba(0, 0, 0, 0.18);
}

.trace-timeline__hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(330px, 0.42fr);
  gap: 16px;
  align-items: stretch;
  min-height: 136px;
  padding: 16px;
  border-color: rgba(91, 141, 239, 0.25);
  background:
    radial-gradient(circle at 88% 0, rgba(33, 199, 217, 0.12), transparent 34%),
    linear-gradient(135deg, rgba(16, 31, 51, 0.96), rgba(8, 17, 31, 0.92));
}

.trace-timeline__intro {
  align-self: center;
  min-width: 0;
}

.trace-timeline__eyebrow {
  color: #9fc4ff;
  font-size: 11px;
  font-weight: 900;
  line-height: 1.35;
}

.trace-timeline__hero h1 {
  margin-top: 6px;
  font-size: 34px;
  line-height: 1.08;
}

.trace-timeline__subtitle {
  margin-top: 7px !important;
  color: var(--trace-secondary);
  font-size: 14px;
  line-height: 1.45;
  font-weight: 800;
}

.trace-timeline__summary {
  margin-top: 7px !important;
  max-width: 860px;
  color: var(--trace-muted);
  font-size: 12px;
  line-height: 1.5;
}

.trace-timeline__tags,
.trace-timeline__keywords,
.trace-timeline__review-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.trace-timeline__tags {
  margin-top: 11px;
}

.trace-timeline__tags span,
.trace-timeline__step-meta span,
.trace-timeline__keywords em {
  display: inline-flex;
  align-items: center;
  min-height: 23px;
  border: 1px solid rgba(33, 199, 217, 0.18);
  border-radius: 6px;
  padding: 3px 7px;
  color: #9fe8f1;
  background: rgba(33, 199, 217, 0.055);
  font-size: 10.5px;
  font-style: normal;
  font-weight: 760;
}

.trace-timeline__tags span:nth-child(2),
.trace-timeline__tags span:nth-child(5) {
  border-color: rgba(255, 180, 92, 0.2);
  color: #ffd6a8;
  background: rgba(255, 180, 92, 0.06);
}

.trace-timeline__hero-card {
  display: grid;
  align-content: center;
  gap: 12px;
  min-width: 0;
  border: 1px solid rgba(151, 180, 214, 0.14);
  border-radius: 8px;
  padding: 13px;
  background: rgba(4, 9, 18, 0.34);
}

.trace-timeline__hero-card-title {
  display: grid;
  gap: 4px;
}

.trace-timeline__hero-card-title span,
.trace-timeline__panel-heading > span,
.trace-timeline__detail-card > .trace-timeline__panel-heading > span {
  color: var(--trace-muted);
  font-size: 10.5px;
  font-weight: 850;
}

.trace-timeline__hero-card-title strong {
  overflow-wrap: anywhere;
  color: var(--trace-text);
  font: 800 12px/1.35 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.trace-timeline__stat-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 7px;
}

.trace-timeline__stat-row section {
  display: grid;
  gap: 4px;
  border: 1px solid rgba(151, 180, 214, 0.1);
  border-left: 3px solid var(--trace-cyan);
  border-radius: 7px;
  padding: 8px;
  background: rgba(4, 9, 18, 0.38);
}

.trace-timeline__stat-row strong {
  color: var(--trace-text);
  font: 800 18px/1 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.trace-timeline__stat-row span,
.trace-timeline__stat-row small {
  color: var(--trace-muted);
  font-size: 10px;
  line-height: 1.25;
}

.trace-timeline__overview {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  overflow: hidden;
}

.trace-timeline__overview div {
  min-width: 0;
  border-right: 1px solid rgba(151, 180, 214, 0.09);
  border-bottom: 1px solid rgba(151, 180, 214, 0.08);
  padding: 9px 11px;
}

.trace-timeline__overview div:nth-child(5n) {
  border-right: 0;
}

.trace-timeline__overview dt,
.trace-timeline__detail-grid dt,
.trace-timeline__query-box dt,
.trace-timeline__evidence-item dt,
.trace-timeline__review-pairs dt {
  color: var(--trace-muted);
  font-size: 10px;
  font-weight: 850;
  line-height: 1.3;
}

.trace-timeline__overview dd,
.trace-timeline__detail-grid dd,
.trace-timeline__query-box dd,
.trace-timeline__evidence-item dd,
.trace-timeline__review-pairs dd {
  margin-top: 3px;
  overflow-wrap: anywhere;
  color: var(--trace-secondary);
  font-size: 11px;
  line-height: 1.35;
}

.trace-timeline__overview dd,
.trace-timeline__detail-grid dd,
.trace-timeline__evidence-item dd {
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.trace-timeline__workspace {
  display: grid;
  grid-template-columns: minmax(246px, 0.78fr) minmax(430px, 1.24fr) minmax(330px, 0.98fr);
  gap: 10px;
  align-items: start;
  min-width: 0;
}

.trace-timeline__panel {
  min-width: 0;
  overflow: hidden;
}

.trace-timeline__panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid var(--trace-border);
  padding: 11px 12px;
}

.trace-timeline__panel-heading h2 {
  margin-top: 2px;
  font-size: 15px;
  line-height: 1.2;
}

.trace-timeline__runs,
.trace-timeline__steps,
.trace-timeline__detail {
  display: grid;
  align-content: start;
  gap: 10px;
}

.trace-timeline__run-list {
  display: grid;
  gap: 8px;
  padding: 10px;
}

.trace-timeline__run {
  display: grid;
  gap: 5px;
  min-width: 0;
  border: 1px solid rgba(151, 180, 214, 0.12);
  border-left: 3px solid var(--trace-blue);
  border-radius: 8px;
  padding: 10px;
  color: var(--trace-text);
  background: rgba(7, 16, 29, 0.72);
  text-align: left;
  cursor: pointer;
}

.trace-timeline__run:hover,
.trace-timeline__run:focus-visible,
.trace-timeline__run--active {
  border-color: rgba(61, 124, 255, 0.5);
  background: linear-gradient(145deg, rgba(61, 124, 255, 0.18), rgba(139, 124, 246, 0.08));
  outline: 0;
}

.trace-timeline__run--active {
  border-left-color: var(--trace-cyan);
  box-shadow: inset 3px 0 0 rgba(33, 199, 217, 0.72);
}

.trace-timeline__run-id {
  color: #9fc4ff;
  font: 900 11px/1.2 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.trace-timeline__run strong {
  color: var(--trace-text);
  font-size: 12px;
  line-height: 1.35;
}

.trace-timeline__run small,
.trace-timeline__run em {
  color: var(--trace-muted);
  font-size: 10.5px;
  line-height: 1.3;
  font-style: normal;
}

.trace-timeline__run em {
  width: fit-content;
  border: 1px solid rgba(255, 180, 92, 0.18);
  border-radius: 6px;
  padding: 3px 6px;
  color: #ffd6a8;
  background: rgba(255, 180, 92, 0.06);
  font-weight: 800;
}

.trace-timeline__boundary-card {
  display: grid;
  gap: 6px;
  margin: 0 10px 10px;
  border: 1px solid rgba(255, 180, 92, 0.18);
  border-radius: 8px;
  padding: 10px;
  background: rgba(255, 180, 92, 0.055);
}

.trace-timeline__boundary-card strong {
  color: #ffd6a8;
  font-size: 12px;
}

.trace-timeline__boundary-card p,
.trace-timeline__soft-note,
.trace-timeline__review small {
  color: var(--trace-muted);
  font-size: 11px;
  line-height: 1.45;
}

.trace-timeline__step-list {
  position: relative;
  display: grid;
  gap: 7px;
  padding: 10px;
}

.trace-timeline__step-list::before {
  position: absolute;
  top: 31px;
  bottom: 31px;
  left: 24px;
  width: 1px;
  background: linear-gradient(180deg, rgba(61, 124, 255, 0.45), rgba(43, 216, 143, 0.42), rgba(255, 180, 92, 0.4), rgba(255, 92, 122, 0.36));
  content: "";
}

.trace-timeline__step {
  position: relative;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) minmax(116px, 0.38fr);
  gap: 9px;
  align-items: start;
  min-width: 0;
  border: 1px solid rgba(151, 180, 214, 0.12);
  border-left: 3px solid var(--step-color, var(--trace-blue));
  border-radius: 8px;
  padding: 9px;
  color: var(--trace-text);
  background: rgba(4, 9, 18, 0.38);
  text-align: left;
  cursor: pointer;
}

.trace-timeline__step:hover,
.trace-timeline__step:focus-visible,
.trace-timeline__step--selected {
  border-color: rgba(61, 124, 255, 0.48);
  outline: 0;
}

.trace-timeline__step--selected {
  background: linear-gradient(135deg, rgba(61, 124, 255, 0.18), rgba(33, 199, 217, 0.08));
  box-shadow: 0 0 0 1px rgba(61, 124, 255, 0.14), 0 14px 28px rgba(61, 124, 255, 0.12);
}

.trace-timeline__step--cyan { --step-color: var(--trace-cyan); }
.trace-timeline__step--green { --step-color: var(--trace-green); }
.trace-timeline__step--amber { --step-color: var(--trace-amber); }
.trace-timeline__step--red { --step-color: var(--trace-red); }
.trace-timeline__step--violet { --step-color: var(--trace-violet); }
.trace-timeline__step--slate { --step-color: #5b6f8d; }

.trace-timeline__step-index {
  position: relative;
  z-index: 1;
  display: inline-grid;
  width: 25px;
  height: 25px;
  place-items: center;
  border-radius: 50%;
  color: #06101b;
  background: var(--step-color, var(--trace-blue));
  font: 900 10px/1 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.trace-timeline__step-main {
  min-width: 0;
}

.trace-timeline__step-kicker {
  display: block;
  color: var(--trace-muted);
  font-size: 10px;
  font-weight: 850;
  line-height: 1.25;
}

.trace-timeline__step-main strong {
  display: block;
  margin-top: 3px;
  color: var(--trace-text);
  font-size: 13px;
  line-height: 1.25;
}

.trace-timeline__step-main p {
  margin-top: 4px;
  color: var(--trace-secondary);
  font-size: 11px;
  line-height: 1.4;
}

.trace-timeline__step-main small {
  display: block;
  margin-top: 4px;
  color: var(--trace-muted);
  font-size: 10.5px;
  line-height: 1.35;
}

.trace-timeline__step-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 5px;
  min-width: 0;
}

.trace-timeline__step-meta b,
.trace-timeline__step-meta em {
  display: block;
  width: 100%;
  text-align: right;
}

.trace-timeline__step-meta b {
  color: #dfeaff;
  font-size: 10.5px;
  line-height: 1.25;
}

.trace-timeline__step-meta em {
  color: var(--trace-muted);
  font: 800 10px/1.25 "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-style: normal;
}

.trace-timeline__step-meta span {
  min-height: 20px;
  padding: 2px 5px;
  font-size: 9.5px;
}

.trace-timeline__detail {
  grid-template-columns: 1fr;
}

.trace-timeline__detail-card > .trace-timeline__panel-heading > span,
.trace-timeline__evidence > .trace-timeline__panel-heading > span,
.trace-timeline__review > .trace-timeline__panel-heading > span,
.trace-timeline__json > .trace-timeline__panel-heading > span {
  border: 1px solid rgba(151, 180, 214, 0.13);
  border-radius: 6px;
  padding: 4px 7px;
  background: rgba(4, 9, 18, 0.36);
}

.trace-timeline__detail-card > .trace-timeline__panel-heading > span[data-tone='green'] {
  color: #aaf4d1;
  border-color: rgba(43, 216, 143, 0.2);
}

.trace-timeline__detail-card > .trace-timeline__panel-heading > span[data-tone='amber'],
.trace-timeline__review > .trace-timeline__panel-heading > span {
  color: #ffd6a8;
  border-color: rgba(255, 180, 92, 0.2);
}

.trace-timeline__detail-card > .trace-timeline__panel-heading > span[data-tone='red'] {
  color: #ffc0ca;
  border-color: rgba(255, 92, 122, 0.2);
}

.trace-timeline__detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
  padding: 10px;
}

.trace-timeline__detail-grid div,
.trace-timeline__query-box div,
.trace-timeline__review-pairs div {
  min-width: 0;
  border: 1px solid rgba(151, 180, 214, 0.1);
  border-radius: 7px;
  padding: 8px;
  background: rgba(4, 9, 18, 0.34);
}

.trace-timeline__provider-note {
  display: grid;
  gap: 6px;
  margin: 0 10px 10px;
  border: 1px solid rgba(255, 180, 92, 0.2);
  border-radius: 8px;
  padding: 10px;
  background: linear-gradient(135deg, rgba(255, 180, 92, 0.08), rgba(4, 9, 18, 0.34));
}

.trace-timeline__provider-note strong {
  color: #ffd6a8;
  font-size: 12px;
}

.trace-timeline__provider-note p,
.trace-timeline__provider-note small {
  color: var(--trace-muted);
  font-size: 11px;
  line-height: 1.4;
}

.trace-timeline__inline-review,
.trace-timeline__debug-inline {
  display: grid;
  gap: 6px;
  margin: 0 10px 10px;
  border: 1px solid rgba(151, 180, 214, 0.11);
  border-radius: 8px;
  padding: 9px;
  background: rgba(4, 9, 18, 0.34);
}

.trace-timeline__inline-review {
  border-color: rgba(255, 180, 92, 0.16);
  background: rgba(255, 180, 92, 0.045);
}

.trace-timeline__inline-review strong,
.trace-timeline__debug-inline strong {
  color: var(--trace-text);
  font-size: 11.5px;
}

.trace-timeline__inline-review p {
  color: var(--trace-muted);
  font-size: 10.5px;
  line-height: 1.4;
}

.trace-timeline__debug-inline div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.trace-timeline__debug-inline span {
  color: var(--trace-muted);
  font-size: 10px;
  font-weight: 850;
}

.trace-timeline__debug-inline pre {
  max-height: 88px;
  margin: 0;
  overflow: hidden;
  border: 1px solid rgba(139, 124, 246, 0.15);
  border-radius: 7px;
  padding: 8px;
  color: #d7e7ff;
  background: rgba(3, 7, 14, 0.75);
  font: 10px/1.35 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.trace-timeline__query-box,
.trace-timeline__review-pairs {
  display: grid;
  gap: 7px;
  padding: 10px 10px 0;
}

.trace-timeline__evidence-list {
  display: grid;
  gap: 8px;
  padding: 10px;
}

.trace-timeline__evidence-item {
  display: grid;
  gap: 5px;
  border: 1px solid rgba(151, 180, 214, 0.11);
  border-left: 3px solid var(--trace-violet);
  border-radius: 8px;
  padding: 9px;
  background: rgba(4, 9, 18, 0.34);
}

.trace-timeline__evidence-item > div:first-child {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.trace-timeline__evidence-item strong {
  color: #c9c3ff;
  font: 900 11px/1.2 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.trace-timeline__evidence-item > div:first-child span {
  border: 1px solid rgba(43, 216, 143, 0.2);
  border-radius: 6px;
  padding: 3px 6px;
  color: #aaf4d1;
  background: rgba(43, 216, 143, 0.07);
  font-size: 9.5px;
  font-weight: 850;
}

.trace-timeline__evidence-item p {
  color: var(--trace-text);
  font-size: 11px;
  font-weight: 800;
  line-height: 1.3;
}

.trace-timeline__evidence-item small {
  color: var(--trace-muted);
  font-size: 10px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.trace-timeline__keywords {
  margin-top: 2px;
}

.trace-timeline__evidence-item dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.trace-timeline__soft-note {
  margin: 0 10px 10px !important;
  border: 1px solid rgba(255, 180, 92, 0.13);
  border-radius: 7px;
  padding: 8px;
  background: rgba(255, 180, 92, 0.045);
}

.trace-timeline__review-pairs div:nth-child(2),
.trace-timeline__review-pairs div:nth-child(3) {
  border-color: rgba(255, 180, 92, 0.14);
}

.trace-timeline__review blockquote {
  margin: 10px 10px 0;
  border-left: 3px solid rgba(33, 199, 217, 0.55);
  border-radius: 0 7px 7px 0;
  padding: 8px 9px;
  color: var(--trace-secondary);
  background: rgba(33, 199, 217, 0.055);
  font-size: 11px;
  line-height: 1.45;
}

.trace-timeline__review-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  padding: 10px 10px 0;
}

.trace-timeline__review-actions button {
  min-height: 31px;
  border: 1px solid rgba(151, 180, 214, 0.14);
  border-radius: 7px;
  color: var(--trace-secondary);
  background: rgba(10, 21, 38, 0.78);
  font-size: 10.5px;
  font-weight: 850;
  cursor: default;
}

.trace-timeline__review-actions button:first-child {
  border-color: rgba(43, 216, 143, 0.28);
  color: #aaf4d1;
  background: rgba(43, 216, 143, 0.075);
}

.trace-timeline__review-actions button:nth-child(2) {
  border-color: rgba(255, 180, 92, 0.28);
  color: #ffd6a8;
  background: rgba(255, 180, 92, 0.07);
}

.trace-timeline__review-actions button:last-child {
  border-color: rgba(255, 92, 122, 0.28);
  color: #ffc0ca;
  background: rgba(255, 92, 122, 0.07);
}

.trace-timeline__review small {
  display: block;
  padding: 8px 10px 10px;
}

.trace-timeline__json pre {
  max-height: 300px;
  margin: 0;
  overflow: auto;
  border-top: 1px solid rgba(139, 124, 246, 0.16);
  padding: 10px;
  color: #d7e7ff;
  background:
    linear-gradient(180deg, rgba(3, 7, 14, 0.82), rgba(3, 7, 14, 0.94)),
    #03070e;
  font: 11px/1.45 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

@media (max-width: 1380px) {
  .trace-timeline__workspace {
    grid-template-columns: minmax(240px, 0.72fr) minmax(400px, 1.16fr) minmax(310px, 0.92fr);
  }

  .trace-timeline__overview {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .trace-timeline__overview div:nth-child(5n) {
    border-right: 1px solid rgba(151, 180, 214, 0.09);
  }

  .trace-timeline__overview div:nth-child(4n) {
    border-right: 0;
  }
}

@media (max-width: 1180px) {
  .trace-timeline__hero,
  .trace-timeline__workspace {
    grid-template-columns: 1fr;
  }

  .trace-timeline__overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .trace-timeline__overview div:nth-child(4n),
  .trace-timeline__overview div:nth-child(5n) {
    border-right: 1px solid rgba(151, 180, 214, 0.09);
  }

  .trace-timeline__overview div:nth-child(2n) {
    border-right: 0;
  }

  .trace-timeline__run-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .trace-timeline__hero {
    min-height: 0;
    padding: 13px;
  }

  .trace-timeline__hero h1 {
    font-size: 29px;
  }

  .trace-timeline__stat-row,
  .trace-timeline__overview,
  .trace-timeline__run-list,
  .trace-timeline__detail-grid,
  .trace-timeline__evidence-item dl,
  .trace-timeline__review-actions {
    grid-template-columns: 1fr;
  }

  .trace-timeline__overview div,
  .trace-timeline__overview div:nth-child(2n),
  .trace-timeline__overview div:nth-child(4n),
  .trace-timeline__overview div:nth-child(5n) {
    border-right: 0;
  }

  .trace-timeline__step {
    grid-template-columns: 28px minmax(0, 1fr);
  }

  .trace-timeline__step-meta {
    grid-column: 2;
    justify-content: flex-start;
  }

  .trace-timeline__step-meta b,
  .trace-timeline__step-meta em {
    text-align: left;
  }
}
</style>
