<script setup lang="ts">
import { computed, ref } from 'vue'
import { evaluationSnapshot } from '../data/evaluationMetrics'

type WorkbenchFilter = 'ALL' | 'REVIEW' | 'P1' | 'FALLBACK'
type WorkbenchPriority = 'P1' | 'P2' | 'P3'
type WorkbenchTone = 'blue' | 'cyan' | 'green' | 'amber' | 'red' | 'violet' | 'slate'
type DemoAction = 'idle' | 'draft' | 'citation' | 'trace' | 'review'

interface WorkbenchEvidence {
  knowledgeId: string
  sourceTitle: string
  sourcePath: string
  matchedKeywords: string[]
  keywordScore: number
  citationUsed: boolean
  confidence: string
}

interface WorkbenchTraceStep {
  name: string
  status: string
  latency: string
  path: string
  tone: WorkbenchTone
}

interface WorkbenchReview {
  status: string
  riskReason: string
  reviewerAction: string
  editedPreview: string
  requiredReason: string
  citationWarning: string
}

interface WorkbenchTicket {
  id: string
  title: string
  summary: string
  requester: string
  team: string
  system: string
  category: string
  priority: WorkbenchPriority
  urgency: string
  status: string
  sla: string
  providerPath: string
  citationState: string
  reviewState: string
  tags: string[]
  searchText: string
  context: string[]
  classification: string
  priorityReason: string
  analysisSummary: string
  likelyCause: string
  recommendedPlan: string
  riskNotes: string[]
  draftLabel: string
  draftReply: string
  retrieval: {
    query: string
    topK: string
    hitStatus: string
    latency: string
    method: string
  }
  evidences: WorkbenchEvidence[]
  trace: WorkbenchTraceStep[]
  review: WorkbenchReview
  evalCaseId: string
}

// Showcase demo data only: these synthetic enterprise ticket cases are used for
// portfolio frontend display. They are not real enterprise data, real users, or
// production traffic, and they do not imply a real online AI automation system.
const workbenchTickets: WorkbenchTicket[] = [
  {
    id: 'DEMO-API-005',
    title: '报价接口返回 500，销售无法生成报价单',
    summary: '报价中心在华东区域灰度后出现 500，销售运营无法生成企业客户报价单。',
    requester: '销售运营 / synthetic user',
    team: 'Sales Ops',
    system: 'quote-center',
    category: '系统故障',
    priority: 'P1',
    urgency: 'High Risk',
    status: '处理中',
    sla: '00:47:12',
    providerPath: 'local-rule fallback',
    citationState: 'Citation Ready',
    reviewState: 'Needs Review',
    tags: ['High Risk', 'Citation Ready', 'Needs Review', 'SLA Watch'],
    searchText: 'quote center api 500 traceId price policy sales ops citation review',
    context: [
      '影响销售运营与客户成功团队的报价单生成。',
      '日志包含 traceId=qc-20260704-500 与 PricePolicyService 空指针。',
      '最近变更为今天 09:40 quote-center 灰度发布。'
    ],
    classification: '系统故障 / API 5xx',
    priorityReason: 'P1：报价链路影响客户跟进，任何回滚或降级都必须人工确认。',
    analysisSummary: '本地规则命中 500、traceId、PricePolicyService 和报价中心关键词，判断为系统故障并触发人工复核。',
    likelyCause: '报价策略服务在灰度版本中出现空指针，或某类客户报价输入缺少策略映射。',
    recommendedPlan: '先用 traceId 串联网关、应用日志和最近发布差异；确认影响面后由值班人员决定修复、降级或回滚。',
    riskNotes: ['不得自动回滚生产灰度。', '不得批量重放报价请求。', '需要引用知识库并进入 Human Review。'],
    draftLabel: 'Requires human review',
    draftReply:
      '您好，报价接口异常已进入人工排查。当前初步判断与报价中心服务 500 有关，我们会先核对 traceId、灰度发布差异和依赖服务日志；处理动作确认后再同步修复进度与临时方案。',
    retrieval: {
      query: 'quote-center HTTP 500 PricePolicyService traceId 报价失败',
      topK: `Top-${evaluationSnapshot.topK}`,
      hitStatus: 'Top-K hit / multi-source citation',
      latency: evaluationSnapshot.avgRetrievalLatency,
      method: 'keyword retrieval'
    },
    evidences: [
      {
        knowledgeId: 'KB-API-500',
        sourceTitle: '接口 500 错误分层排查手册',
        sourcePath: 'support-knowledge/api/500-troubleshooting',
        matchedKeywords: ['500', 'traceId', '接口异常', '回滚确认'],
        keywordScore: 92,
        citationUsed: true,
        confidence: 'high'
      },
      {
        knowledgeId: 'KB-OPS-003',
        sourceTitle: '系统故障超时与 5xx 排查手册',
        sourcePath: 'ops/runbook/5xx-timeout',
        matchedKeywords: ['灰度发布', '依赖服务', '降级策略'],
        keywordScore: 84,
        citationUsed: true,
        confidence: 'medium'
      }
    ],
    trace: [
      { name: 'Ticket Input', status: 'received', latency: '0.001 ms', path: 'demo ticket payload', tone: 'blue' },
      { name: 'Query Rewrite', status: 'local normalize', latency: '0.004 ms', path: 'keyword terms', tone: 'cyan' },
      { name: 'Retrieval', status: 'Top-K hit', latency: evaluationSnapshot.avgRetrievalLatency, path: 'keyword retrieval', tone: 'green' },
      { name: 'Prompt Build', status: 'template draft', latency: '0.011 ms', path: 'local-rule template', tone: 'violet' },
      { name: 'Provider Call', status: 'skipped', latency: '0 ms', path: 'no API key / fallback', tone: 'amber' },
      { name: 'Citation Attach', status: '2 citations', latency: '0.006 ms', path: 'citation gating', tone: 'green' },
      { name: 'Human Review', status: 'required', latency: 'pending', path: 'review gate', tone: 'red' }
    ],
    review: {
      status: '待人工复核',
      riskReason: 'P1 业务影响 + 多来源 citation + 可能涉及降级或回滚。',
      reviewerAction: '先请求值班 SRE 补充 traceId 关联日志，再决定是否通过草稿。',
      editedPreview: '请保留失败报价单和 traceId。支持人员确认处理动作后，会同步修复进度。',
      requiredReason: 'high-risk-change + citation-required',
      citationWarning: '引用来源匹配预期知识源；仍需人工确认是否适用于当前灰度版本。'
    },
    evalCaseId: 'EVAL-010'
  },
  {
    id: 'DEMO-SSO-001',
    title: '员工 SSO 登录失败并触发 MFA 重试',
    summary: '多名员工反馈 SSO 登录失败，MFA 重试后仍无法进入员工门户。',
    requester: '员工服务 / synthetic user',
    team: 'Employee Portal',
    system: 'sso-gateway',
    category: '账号与认证',
    priority: 'P1',
    urgency: 'SLA Watch',
    status: '待处理',
    sla: '01:18:44',
    providerPath: 'local-rule fallback',
    citationState: 'Citation Ready',
    reviewState: 'Needs Review',
    tags: ['SLA Watch', 'Needs Review', 'Draft Ready'],
    searchText: 'SSO MFA login failure employee portal authentication retry',
    context: ['SSO 登录页返回 401 与 MFA challenge expired。', '影响多个内部部门的员工门户访问。', '需要确认身份源同步与 MFA 服务状态。'],
    classification: '账号与认证 / SSO',
    priorityReason: 'P1：登录入口受影响，需快速判断身份源、MFA 与网关状态。',
    analysisSummary: '本地规则命中 SSO、MFA、401、login failure，优先按账号认证链路处理。',
    likelyCause: 'MFA challenge 过期或身份源同步延迟导致会话校验失败。',
    recommendedPlan: '核对 SSO 网关错误率、MFA 服务状态与最近身份源同步批次；收敛影响范围后再发布用户通知。',
    riskNotes: ['不能自动重置 MFA。', '不能绕过身份验证流程。'],
    draftLabel: 'Requires human review',
    draftReply: '您好，SSO 登录失败已进入人工排查。我们会先核对 MFA 服务、身份源同步和网关错误率；确认处理动作后再同步恢复进度。',
    retrieval: {
      query: 'SSO MFA 401 challenge expired 登录失败',
      topK: `Top-${evaluationSnapshot.topK}`,
      hitStatus: 'Top-K hit',
      latency: '0.0443 ms',
      method: 'keyword retrieval'
    },
    evidences: [
      {
        knowledgeId: 'KB-SSO-001',
        sourceTitle: 'SSO 登录失败与 MFA 排查流程',
        sourcePath: 'iam/sso/mfa-login-failure',
        matchedKeywords: ['SSO', 'MFA', '401', 'challenge expired'],
        keywordScore: 90,
        citationUsed: true,
        confidence: 'high'
      }
    ],
    trace: [
      { name: 'Ticket Input', status: 'received', latency: '0.001 ms', path: 'demo ticket payload', tone: 'blue' },
      { name: 'Query Rewrite', status: 'local normalize', latency: '0.003 ms', path: 'sso / mfa terms', tone: 'cyan' },
      { name: 'Retrieval', status: 'Top-K hit', latency: '0.0443 ms', path: 'keyword retrieval', tone: 'green' },
      { name: 'Prompt Build', status: 'template draft', latency: '0.010 ms', path: 'local-rule template', tone: 'violet' },
      { name: 'Provider Call', status: 'skipped', latency: '0 ms', path: 'no API key / fallback', tone: 'amber' },
      { name: 'Citation Attach', status: '1 citation', latency: '0.005 ms', path: 'citation gating', tone: 'green' },
      { name: 'Human Review', status: 'required', latency: 'pending', path: 'review gate', tone: 'red' }
    ],
    review: {
      status: '待人工复核',
      riskReason: '登录入口影响面可能扩大，任何绕过认证或批量重置都需要审批。',
      reviewerAction: '请求 IAM 值班确认身份源同步窗口与 MFA 服务状态。',
      editedPreview: '我们已定位到 SSO / MFA 链路，会在人工确认后同步下一步处理。',
      requiredReason: 'identity-risk + review-required',
      citationWarning: '当前 citation 与 SSO/MFA 关键词匹配，需人工确认是否为身份源同步问题。'
    },
    evalCaseId: 'EVAL-016'
  },
  {
    id: 'DEMO-RBAC-002',
    title: '审批完成后仍无法访问 HR 报表',
    summary: 'HRBP 已完成审批，但进入人事报表时仍提示无权限。',
    requester: 'HRBP / synthetic user',
    team: 'HR Ops',
    system: 'hr-reporting',
    category: '权限问题',
    priority: 'P2',
    urgency: 'Needs Review',
    status: '待人工确认',
    sla: '03:42:19',
    providerPath: 'local-rule fallback',
    citationState: 'Citation Ready',
    reviewState: 'Needs Review',
    tags: ['Needs Review', 'Citation Ready', 'RBAC'],
    searchText: 'RBAC 403 permission approval HR report access',
    context: ['审批单显示 completed，但用户组未包含 hr-report-viewer。', '访问日志返回 403 Forbidden。', '需要核对审批流与权限同步记录。'],
    classification: '权限问题 / RBAC',
    priorityReason: 'P2：单系统权限阻塞，但涉及 HR 数据访问，必须人工复核。',
    analysisSummary: '本地规则命中 403、审批、RBAC、report-viewer，判断为权限同步或角色映射问题。',
    likelyCause: '审批完成事件尚未同步到 RBAC 角色，或报表路径绑定了错误角色。',
    recommendedPlan: '核验审批单、用户组同步日志和报表路径权限；确认后由权限支持人工补齐。',
    riskNotes: ['不得自动授权 HR 报表权限。', '需要保留审批与审计记录。'],
    draftLabel: 'Draft only',
    draftReply: '您好，当前更像是审批完成后的权限同步问题。请补充审批单号和报表路径，权限支持会人工核对 RBAC 同步记录后处理。',
    retrieval: {
      query: 'RBAC 403 审批完成 报表无权限',
      topK: `Top-${evaluationSnapshot.topK}`,
      hitStatus: 'Top-K hit',
      latency: '0.0418 ms',
      method: 'keyword retrieval'
    },
    evidences: [
      {
        knowledgeId: 'KB-IAM-ROLE',
        sourceTitle: '权限配置问题与 RBAC 核验流程',
        sourcePath: 'iam/rbac/role-sync-checklist',
        matchedKeywords: ['RBAC', '403', '审批', '角色同步'],
        keywordScore: 88,
        citationUsed: true,
        confidence: 'high'
      }
    ],
    trace: [
      { name: 'Ticket Input', status: 'received', latency: '0.001 ms', path: 'demo ticket payload', tone: 'blue' },
      { name: 'Query Rewrite', status: 'local normalize', latency: '0.003 ms', path: 'rbac / 403 terms', tone: 'cyan' },
      { name: 'Retrieval', status: 'Top-K hit', latency: '0.0418 ms', path: 'keyword retrieval', tone: 'green' },
      { name: 'Prompt Build', status: 'template draft', latency: '0.010 ms', path: 'local-rule template', tone: 'violet' },
      { name: 'Provider Call', status: 'skipped', latency: '0 ms', path: 'no API key / fallback', tone: 'amber' },
      { name: 'Citation Attach', status: '1 citation', latency: '0.004 ms', path: 'citation gating', tone: 'green' },
      { name: 'Human Review', status: 'required', latency: 'pending', path: 'review gate', tone: 'red' }
    ],
    review: {
      status: '待人工复核',
      riskReason: 'HR 报表涉及敏感数据，权限补齐必须有审批依据。',
      reviewerAction: '请求权限支持核对审批单号、同步批次和目标角色。',
      editedPreview: '请提供审批单号与报表路径，我们会人工核对权限同步记录。',
      requiredReason: 'sensitive-permission + audit-required',
      citationWarning: '引用来源为 RBAC 核验流程，不能替代审批。'
    },
    evalCaseId: 'EVAL-003'
  },
  {
    id: 'DEMO-SYNC-003',
    title: 'CRM 数据同步延迟导致指标看板口径不一致',
    summary: '客户成功团队发现今日续费看板与 CRM 明细相差约 6.25%。',
    requester: '客户成功 / synthetic user',
    team: 'Customer Success',
    system: 'crm-analytics',
    category: '数据问题',
    priority: 'P2',
    urgency: 'SLA Watch',
    status: '待处理',
    sla: '04:16:35',
    providerPath: 'local-rule fallback',
    citationState: 'Citation Ready',
    reviewState: 'Review Required',
    tags: ['SLA Watch', 'Citation Ready', 'Draft Ready'],
    searchText: 'CRM data sync delay dashboard metric mismatch renewal',
    context: ['续费看板更新时间落后 CRM 明细约 90 分钟。', '当前影响客户成功团队的早会指标复盘。', '需要核对同步任务、数据口径和失败重试。'],
    classification: '数据问题 / 同步延迟',
    priorityReason: 'P2：影响业务分析，但不直接改变客户数据。',
    analysisSummary: '本地规则命中数据同步、看板口径、延迟与 renewal dashboard 关键词。',
    likelyCause: '同步任务部分分片失败或指标表刷新延迟。',
    recommendedPlan: '检查 ETL 批次、失败重试和指标表刷新时间；先说明口径延迟，再由数据支持确认是否补跑。',
    riskNotes: ['不得自动补跑生产数据任务。', '指标修复需保留批次与审计记录。'],
    draftLabel: 'Draft only',
    draftReply: '您好，当前更像是数据同步延迟导致的看板口径差异。我们会先核对 ETL 批次、失败重试和指标刷新时间，再确认是否需要人工补跑。',
    retrieval: {
      query: 'CRM renewal dashboard 数据同步延迟 指标口径',
      topK: `Top-${evaluationSnapshot.topK}`,
      hitStatus: 'Top-K hit',
      latency: '0.0452 ms',
      method: 'keyword retrieval'
    },
    evidences: [
      {
        knowledgeId: 'KB-DATA-004',
        sourceTitle: '报表数据同步与口径核对流程',
        sourcePath: 'data/runbook/sync-metric-check',
        matchedKeywords: ['数据同步', '指标口径', '补跑', '批次'],
        keywordScore: 86,
        citationUsed: true,
        confidence: 'high'
      }
    ],
    trace: [
      { name: 'Ticket Input', status: 'received', latency: '0.001 ms', path: 'demo ticket payload', tone: 'blue' },
      { name: 'Query Rewrite', status: 'local normalize', latency: '0.004 ms', path: 'sync / metrics terms', tone: 'cyan' },
      { name: 'Retrieval', status: 'Top-K hit', latency: '0.0452 ms', path: 'keyword retrieval', tone: 'green' },
      { name: 'Prompt Build', status: 'template draft', latency: '0.011 ms', path: 'local-rule template', tone: 'violet' },
      { name: 'Provider Call', status: 'skipped', latency: '0 ms', path: 'no API key / fallback', tone: 'amber' },
      { name: 'Citation Attach', status: '1 citation', latency: '0.005 ms', path: 'citation gating', tone: 'green' },
      { name: 'Human Review', status: 'required', latency: 'pending', path: 'review gate', tone: 'amber' }
    ],
    review: {
      status: '待数据支持确认',
      riskReason: '补跑或改指标表会影响业务报表，需要人工确认。',
      reviewerAction: '请求数据支持提供 ETL 批次和补跑计划。',
      editedPreview: '我们会先确认同步批次，再同步是否需要人工补跑。',
      requiredReason: 'data-fix + audit-required',
      citationWarning: 'citation 已命中数据同步流程；是否补跑仍需人工判断。'
    },
    evalCaseId: 'EVAL-006'
  },
  {
    id: 'DEMO-FALLBACK-004',
    title: '会议室设备无法投屏，知识库暂无可靠来源',
    summary: '会议室投屏设备偶发断连，当前知识库没有匹配的标准处理流程。',
    requester: '行政支持 / synthetic user',
    team: 'Workplace Ops',
    system: 'meeting-room-device',
    category: '流程咨询',
    priority: 'P3',
    urgency: 'Fallback',
    status: '待补充',
    sla: '12:00:00',
    providerPath: 'local-rule fallback',
    citationState: 'Fallback',
    reviewState: 'Needs Review',
    tags: ['Fallback', 'Needs Review', 'Knowledge Missing'],
    searchText: 'meeting room cast device no knowledge fallback',
    context: ['投屏设备重启后短暂恢复，但 30 分钟后再次断连。', '知识库暂无会议室设备条目。', '应避免用无关网络策略知识强行引用。'],
    classification: '流程咨询 / 知识缺失',
    priorityReason: 'P3：不影响核心业务系统，但需要标记知识库缺口。',
    analysisSummary: '本地规则无法找到可靠知识来源，进入 fallback 和人工复核。',
    likelyCause: '设备固件、无线网络或会议室本地环境问题；当前知识库不足以支持明确结论。',
    recommendedPlan: '先请求补充设备型号、房间号和错误现象；不要附会无关知识条目。',
    riskNotes: ['缺少可靠 citation，不应生成确定性处理结论。', '适合沉淀新的设备排查知识条目。'],
    draftLabel: 'Requires human review',
    draftReply: '您好，当前信息不足以给出确定处理结论。请补充会议室、设备型号和断连截图；支持人员会人工判断是否需要现场排查或新增知识库条目。',
    retrieval: {
      query: '会议室 投屏 设备断连 无知识库来源',
      topK: `Top-${evaluationSnapshot.topK}`,
      hitStatus: 'fallback / expected miss',
      latency: '0.0366 ms',
      method: 'keyword retrieval'
    },
    evidences: [
      {
        knowledgeId: 'NO-RELIABLE-SOURCE',
        sourceTitle: '未找到可靠知识来源',
        sourcePath: 'fallback/no-citation-attached',
        matchedKeywords: ['会议室', '投屏', '设备断连'],
        keywordScore: 34,
        citationUsed: false,
        confidence: 'low'
      }
    ],
    trace: [
      { name: 'Ticket Input', status: 'received', latency: '0.001 ms', path: 'demo ticket payload', tone: 'blue' },
      { name: 'Query Rewrite', status: 'local normalize', latency: '0.003 ms', path: 'device terms', tone: 'cyan' },
      { name: 'Retrieval', status: 'expected miss', latency: '0.0366 ms', path: 'keyword retrieval', tone: 'amber' },
      { name: 'Prompt Build', status: 'fallback draft', latency: '0.009 ms', path: 'local-rule template', tone: 'violet' },
      { name: 'Provider Call', status: 'skipped', latency: '0 ms', path: 'no API key / fallback', tone: 'amber' },
      { name: 'Citation Attach', status: 'blocked', latency: '0.002 ms', path: 'citation gating', tone: 'red' },
      { name: 'Human Review', status: 'required', latency: 'pending', path: 'review gate', tone: 'red' }
    ],
    review: {
      status: '待补充信息',
      riskReason: '无可靠知识来源，不能把低相关结果当作 citation。',
      reviewerAction: '请求补充设备信息，并建议后续沉淀新知识。',
      editedPreview: '当前知识库缺少可靠来源，请补充设备型号和现场现象。',
      requiredReason: 'knowledge-missing + fallback',
      citationWarning: 'citation gating 阻止无关来源进入草稿。'
    },
    evalCaseId: 'EVAL-015'
  },
  {
    id: 'DEMO-SLA-006',
    title: 'VIP 客户 onboarding 审批卡在外部系统回调',
    summary: '客户成功反馈 VIP 客户 onboarding 超过 SLA，审批回调状态长时间未更新。',
    requester: '客户成功 / synthetic user',
    team: 'Customer Success',
    system: 'onboarding-flow',
    category: '流程与集成',
    priority: 'P1',
    urgency: 'SLA Watch',
    status: '待人工确认',
    sla: '00:29:58',
    providerPath: 'local-rule fallback',
    citationState: 'Citation Ready',
    reviewState: 'Needs Review',
    tags: ['SLA Watch', 'High Risk', 'Needs Review'],
    searchText: 'VIP onboarding approval callback SLA watch external integration',
    context: ['外部审批系统回调状态停留在 pending。', '客户成功团队需要明确是否升级人工处理。', '涉及客户承诺时效，需要保守措辞。'],
    classification: '流程与集成 / SLA 风险',
    priorityReason: 'P1：SLA 即将超时且涉及 VIP 客户承诺。',
    analysisSummary: '本地规则命中 onboarding、审批回调、SLA 和 VIP 客户关键词。',
    likelyCause: '外部审批回调失败、消息队列延迟或状态同步任务失败。',
    recommendedPlan: '核对回调日志、消息队列和状态同步任务；由人工确认是否升级处理。',
    riskNotes: ['不能自动绕过审批。', '客户承诺时效需要人工确认后再回复。'],
    draftLabel: 'Requires human review',
    draftReply: '您好，onboarding 审批状态已进入 SLA 风险关注。我们会先核对外部系统回调、消息队列和状态同步任务；若需要升级人工处理，会由负责人确认后推进。',
    retrieval: {
      query: 'onboarding 审批回调 pending SLA VIP',
      topK: `Top-${evaluationSnapshot.topK}`,
      hitStatus: 'Top-K hit',
      latency: '0.0472 ms',
      method: 'keyword retrieval'
    },
    evidences: [
      {
        knowledgeId: 'KB-SLA-ONBOARDING',
        sourceTitle: 'SLA 高风险流程升级与人工确认',
        sourcePath: 'support/sla/onboarding-escalation',
        matchedKeywords: ['SLA', '升级', '人工确认', 'onboarding'],
        keywordScore: 87,
        citationUsed: true,
        confidence: 'high'
      }
    ],
    trace: [
      { name: 'Ticket Input', status: 'received', latency: '0.001 ms', path: 'demo ticket payload', tone: 'blue' },
      { name: 'Query Rewrite', status: 'local normalize', latency: '0.004 ms', path: 'sla / callback terms', tone: 'cyan' },
      { name: 'Retrieval', status: 'Top-K hit', latency: '0.0472 ms', path: 'keyword retrieval', tone: 'green' },
      { name: 'Prompt Build', status: 'template draft', latency: '0.012 ms', path: 'local-rule template', tone: 'violet' },
      { name: 'Provider Call', status: 'skipped', latency: '0 ms', path: 'no API key / fallback', tone: 'amber' },
      { name: 'Citation Attach', status: '1 citation', latency: '0.006 ms', path: 'citation gating', tone: 'green' },
      { name: 'Human Review', status: 'required', latency: 'pending', path: 'review gate', tone: 'red' }
    ],
    review: {
      status: '待负责人确认',
      riskReason: 'SLA 即将超时，客户侧回复需要负责人确认。',
      reviewerAction: '请求客户成功负责人确认升级口径和预计恢复时间。',
      editedPreview: '我们已将该工单标记为 SLA 风险，会在人工确认后同步处理计划。',
      requiredReason: 'sla-risk + customer-impact',
      citationWarning: 'SLA 升级流程可引用，但客户承诺时间不能由 demo 自动生成。'
    },
    evalCaseId: 'EVAL-012'
  }
]

const filters: Array<{ id: WorkbenchFilter; label: string }> = [
  { id: 'ALL', label: '全部' },
  { id: 'REVIEW', label: '需复核' },
  { id: 'P1', label: 'P1 高风险' },
  { id: 'FALLBACK', label: 'Fallback' }
]

const selectedTicketId = ref('DEMO-API-005')
const searchKeyword = ref('')
const activeFilter = ref<WorkbenchFilter>('ALL')
const demoAction = ref<DemoAction>('idle')

const filteredTickets = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return workbenchTickets.filter((ticket) => {
    const matchesKeyword =
      !keyword ||
      [ticket.id, ticket.title, ticket.summary, ticket.system, ticket.category, ticket.searchText].some((value) => value.toLowerCase().includes(keyword))
    const matchesFilter =
      activeFilter.value === 'ALL' ||
      (activeFilter.value === 'REVIEW' && ticket.reviewState.toLowerCase().includes('review')) ||
      (activeFilter.value === 'P1' && ticket.priority === 'P1') ||
      (activeFilter.value === 'FALLBACK' && ticket.citationState === 'Fallback')
    return matchesKeyword && matchesFilter
  })
})

const selectedTicket = computed(() => workbenchTickets.find((ticket) => ticket.id === selectedTicketId.value) ?? workbenchTickets[0])

const queueSummary = computed(() => ({
  total: workbenchTickets.length,
  review: workbenchTickets.filter((ticket) => ticket.reviewState.toLowerCase().includes('review')).length,
  citationReady: workbenchTickets.filter((ticket) => ticket.citationState === 'Citation Ready').length,
  fallback: workbenchTickets.filter((ticket) => ticket.citationState === 'Fallback').length
}))

const selectedCitationCount = computed(() => selectedTicket.value.evidences.filter((evidence) => evidence.citationUsed).length)

const actionFeedback = computed(() => {
  const labels: Record<DemoAction, string> = {
    idle: 'Demo actions are local UI states only; no ticket is sent, closed, or modified in a real system.',
    draft: '已重新生成草稿预览：仍为 Draft only，需要 Human Review 后才能采纳。',
    citation: '已附加 citation 预览：仅展示 citation gating 结果，不写入真实知识库。',
    trace: '已打开 Trace 入口提示：当前 traceId / runId 是 showcase 标识，不是完整分布式 tracing runtime。',
    review: '已发送复核预览：本地 demo 状态，不会通知真实 reviewer。'
  }
  return labels[demoAction.value]
})

function selectTicket(ticketId: string) {
  selectedTicketId.value = ticketId
  demoAction.value = 'idle'
}

function setDemoAction(action: DemoAction) {
  demoAction.value = action
}
</script>

<template>
  <section class="ticket-workbench" data-screenshot="ticket-detail" aria-label="Ticket Workbench">
    <header class="ticket-workbench__hero" data-screenshot="ticket-workbench">
      <div class="ticket-workbench__hero-copy">
        <p class="ticket-workbench__eyebrow">Ticket Workbench / 工单工作台</p>
        <h1>Ticket Workbench</h1>
        <p>企业工单 AI 处理工作台 · RAG Evidence · Trace · Human Review。</p>
        <small>基于 synthetic demo 工单，展示 local-rule fallback、keyword retrieval、citation gating 与人工复核门禁的处理链路。</small>
      </div>
      <div class="ticket-workbench__hero-stats" aria-label="Workbench demo status">
        <section>
          <strong>{{ queueSummary.total }}</strong>
          <span>demo tickets</span>
        </section>
        <section>
          <strong>{{ queueSummary.review }}</strong>
          <span>needs review</span>
        </section>
        <section>
          <strong>{{ queueSummary.citationReady }}</strong>
          <span>citation ready</span>
        </section>
        <section>
          <strong>{{ queueSummary.fallback }}</strong>
          <span>fallback</span>
        </section>
      </div>
    </header>

    <section class="ticket-workbench__grid" aria-label="Ticket Queue, Ticket Detail, Evidence Trace and Human Review">
      <aside class="ticket-workbench__panel ticket-workbench__queue" aria-label="工单队列">
        <div class="ticket-workbench__panel-heading">
          <div>
            <p class="ticket-workbench__eyebrow">Ticket Queue</p>
            <h2>工单队列</h2>
          </div>
          <span>{{ filteredTickets.length }}</span>
        </div>

        <label class="ticket-workbench__search" for="ticket-workbench-search">
          <span>搜索</span>
          <input id="ticket-workbench-search" v-model="searchKeyword" type="search" placeholder="搜索 ID、系统、category..." />
        </label>

        <div class="ticket-workbench__filters" aria-label="工单筛选">
          <button
            v-for="filter in filters"
            :key="filter.id"
            type="button"
            :class="{ 'ticket-workbench__filter--active': activeFilter === filter.id }"
            @click="activeFilter = filter.id"
          >
            {{ filter.label }}
          </button>
        </div>

        <div class="ticket-workbench__ticket-list" aria-live="polite">
          <button
            v-for="ticket in filteredTickets"
            :key="ticket.id"
            type="button"
            class="ticket-workbench__ticket-card"
            :class="{ 'ticket-workbench__ticket-card--active': ticket.id === selectedTicket.id }"
            @click="selectTicket(ticket.id)"
          >
            <span class="ticket-workbench__ticket-id">{{ ticket.id }}</span>
            <span class="ticket-workbench__priority" :data-priority="ticket.priority">{{ ticket.priority }}</span>
            <strong>{{ ticket.title }}</strong>
            <small>{{ ticket.summary }}</small>
            <dl>
              <div><dt>category</dt><dd>{{ ticket.category }}</dd></div>
              <div><dt>SLA</dt><dd>{{ ticket.sla }}</dd></div>
              <div><dt>status</dt><dd>{{ ticket.status }}</dd></div>
              <div><dt>provider</dt><dd>{{ ticket.providerPath }}</dd></div>
            </dl>
            <div class="ticket-workbench__chips">
              <span v-for="tag in ticket.tags.slice(0, 3)" :key="tag">{{ tag }}</span>
            </div>
          </button>
          <p v-if="!filteredTickets.length" class="ticket-workbench__empty">暂无匹配工单</p>
        </div>
      </aside>

      <main class="ticket-workbench__panel ticket-workbench__detail" aria-label="当前工单与 AI Draft">
        <article class="ticket-workbench__current">
          <div>
            <p class="ticket-workbench__eyebrow">当前工单</p>
            <h2>{{ selectedTicket.title }}</h2>
            <p>{{ selectedTicket.summary }}</p>
          </div>
          <div class="ticket-workbench__status-stack">
            <span class="ticket-workbench__priority" :data-priority="selectedTicket.priority">{{ selectedTicket.priority }}</span>
            <span>{{ selectedTicket.urgency }}</span>
            <span>{{ selectedTicket.status }}</span>
          </div>
        </article>

        <dl class="ticket-workbench__meta" aria-label="当前工单元数据">
          <div><dt>Requester</dt><dd>{{ selectedTicket.requester }}</dd></div>
          <div><dt>System</dt><dd>{{ selectedTicket.system }}</dd></div>
          <div><dt>Category</dt><dd>{{ selectedTicket.category }}</dd></div>
          <div><dt>SLA / Risk</dt><dd>{{ selectedTicket.sla }} · {{ selectedTicket.urgency }}</dd></div>
          <div><dt>Citation</dt><dd>{{ selectedTicket.citationState }}</dd></div>
          <div><dt>Review</dt><dd>{{ selectedTicket.reviewState }}</dd></div>
        </dl>

        <section class="ticket-workbench__context-card">
          <div class="ticket-workbench__section-title">
            <span>01</span>
            <h3>Ticket Context</h3>
          </div>
          <ul>
            <li v-for="item in selectedTicket.context" :key="item">{{ item }}</li>
          </ul>
        </section>

        <section class="ticket-workbench__analysis-grid" aria-label="AI 分析结果">
          <article>
            <div class="ticket-workbench__section-title">
              <span>02</span>
              <h3>AI 分析结果</h3>
            </div>
            <dl class="ticket-workbench__analysis-pairs">
              <div><dt>分类与优先级</dt><dd>{{ selectedTicket.classification }} · {{ selectedTicket.priorityReason }}</dd></div>
              <div><dt>问题摘要</dt><dd>{{ selectedTicket.analysisSummary }}</dd></div>
              <div><dt>可能原因</dt><dd>{{ selectedTicket.likelyCause }}</dd></div>
              <div><dt>处理建议</dt><dd>{{ selectedTicket.recommendedPlan }}</dd></div>
            </dl>
          </article>

          <article class="ticket-workbench__draft">
            <div class="ticket-workbench__section-title">
              <span>03</span>
              <h3>AI 回复草稿</h3>
              <em>{{ selectedTicket.draftLabel }}</em>
            </div>
            <p>{{ selectedTicket.draftReply }}</p>
            <div class="ticket-workbench__action-bar" aria-label="Demo actions">
              <button type="button" @click="setDemoAction('draft')">重新生成草稿</button>
              <button type="button" @click="setDemoAction('citation')">Attach Citation</button>
              <button type="button" @click="setDemoAction('trace')">查看 Trace</button>
              <button type="button" @click="setDemoAction('review')">发送复核</button>
            </div>
            <p class="ticket-workbench__feedback">{{ actionFeedback }}</p>
          </article>
        </section>

        <section class="ticket-workbench__risk" aria-label="风险提示">
          <div>
            <h3>风险提示</h3>
            <p>Showcase demo only. Drafts require Human Review before action.</p>
          </div>
          <ul>
            <li v-for="risk in selectedTicket.riskNotes" :key="risk">{{ risk }}</li>
          </ul>
        </section>
      </main>

      <aside class="ticket-workbench__panel ticket-workbench__evidence" aria-label="引用证据、运行链路与人工复核">
        <section class="ticket-workbench__rail-card ticket-workbench__rail-card--citation">
          <div class="ticket-workbench__panel-heading ticket-workbench__panel-heading--compact">
            <div>
              <p class="ticket-workbench__eyebrow">Citation Evidence</p>
              <h2>引用证据</h2>
            </div>
            <span>{{ selectedCitationCount }}/{{ selectedTicket.evidences.length }}</span>
          </div>
          <article v-for="evidence in selectedTicket.evidences" :key="`${selectedTicket.id}-${evidence.knowledgeId}`" class="ticket-workbench__evidence-item">
            <div>
              <strong>{{ evidence.knowledgeId }}</strong>
              <p>{{ evidence.sourceTitle }}</p>
              <small>{{ evidence.sourcePath }}</small>
            </div>
            <b :data-used="evidence.citationUsed">{{ evidence.citationUsed ? 'used' : 'blocked' }}</b>
            <div class="ticket-workbench__keyword-row">
              <span v-for="keyword in evidence.matchedKeywords" :key="keyword">{{ keyword }}</span>
            </div>
            <dl>
              <div><dt>keyword score</dt><dd>{{ evidence.keywordScore }}%</dd></div>
              <div><dt>confidence</dt><dd>{{ evidence.confidence }}</dd></div>
            </dl>
          </article>
        </section>

        <section class="ticket-workbench__rail-card ticket-workbench__rail-card--review">
          <div class="ticket-workbench__section-title">
            <span>H</span>
            <h3>人工复核</h3>
          </div>
          <dl class="ticket-workbench__summary-list">
            <div><dt>复核状态</dt><dd>{{ selectedTicket.review.status }}</dd></div>
            <div><dt>risk reason</dt><dd>{{ selectedTicket.review.riskReason }}</dd></div>
            <div><dt>reviewer action</dt><dd>{{ selectedTicket.review.reviewerAction }}</dd></div>
          </dl>
          <blockquote>{{ selectedTicket.review.editedPreview }}</blockquote>
          <div class="ticket-workbench__review-actions">
            <button type="button">通过</button>
            <button type="button">请求修改</button>
            <button type="button">驳回</button>
          </div>
        </section>

        <section class="ticket-workbench__rail-card ticket-workbench__rail-card--trace">
          <div class="ticket-workbench__section-title">
            <span>T</span>
            <h3>运行链路</h3>
          </div>
          <ol class="ticket-workbench__trace">
            <li v-for="step in selectedTicket.trace" :key="`${selectedTicket.id}-${step.name}`" :data-tone="step.tone">
              <div>
                <strong>{{ step.name }}</strong>
                <small>{{ step.path }}</small>
              </div>
              <span>{{ step.status }}</span>
              <em>{{ step.latency }}</em>
            </li>
          </ol>
        </section>

        <section class="ticket-workbench__rail-card">
          <div class="ticket-workbench__section-title">
            <span>R</span>
            <h3>检索摘要</h3>
          </div>
          <dl class="ticket-workbench__summary-list ticket-workbench__summary-list--compact">
            <div><dt>query</dt><dd>{{ selectedTicket.retrieval.query }}</dd></div>
            <div><dt>top-k</dt><dd>{{ selectedTicket.retrieval.topK }}</dd></div>
            <div><dt>hit status</dt><dd>{{ selectedTicket.retrieval.hitStatus }}</dd></div>
            <div><dt>latency</dt><dd>{{ selectedTicket.retrieval.latency }}</dd></div>
            <div><dt>method</dt><dd>{{ selectedTicket.retrieval.method }}</dd></div>
          </dl>
        </section>

        <section class="ticket-workbench__rail-card ticket-workbench__rail-card--eval">
          <div class="ticket-workbench__section-title">
            <span>E</span>
            <h3>Evaluation Link</h3>
          </div>
          <dl class="ticket-workbench__summary-list">
            <div><dt>eval case</dt><dd>{{ selectedTicket.evalCaseId }}</dd></div>
            <div><dt>citation warning</dt><dd>{{ selectedTicket.review.citationWarning }}</dd></div>
            <div><dt>review required</dt><dd>{{ selectedTicket.review.requiredReason }}</dd></div>
          </dl>
        </section>
      </aside>
    </section>
  </section>
</template>

<style scoped>
.ticket-workbench {
  --workbench-panel: rgba(12, 23, 38, 0.9);
  --workbench-panel-strong: rgba(16, 31, 51, 0.94);
  --workbench-border: rgba(145, 174, 207, 0.16);
  --workbench-text: #eef5ff;
  --workbench-secondary: #c7d5ea;
  --workbench-muted: #7e91ab;
  --workbench-blue: #3d7cff;
  --workbench-cyan: #21c7d9;
  --workbench-green: #2bd88f;
  --workbench-amber: #ffb45c;
  --workbench-red: #ff5c7a;
  --workbench-violet: #8b7cf6;
  display: grid;
  gap: 10px;
  min-width: 0;
  color: var(--workbench-text);
}

.ticket-workbench *,
.ticket-workbench *::before,
.ticket-workbench *::after {
  box-sizing: border-box;
}

.ticket-workbench h1,
.ticket-workbench h2,
.ticket-workbench h3,
.ticket-workbench p,
.ticket-workbench dl,
.ticket-workbench dd,
.ticket-workbench blockquote {
  margin: 0;
}

.ticket-workbench__hero,
.ticket-workbench__panel,
.ticket-workbench__context-card,
.ticket-workbench__analysis-grid article,
.ticket-workbench__risk,
.ticket-workbench__rail-card {
  border: 1px solid var(--workbench-border);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(16, 31, 51, 0.9), rgba(8, 17, 31, 0.95)),
    var(--workbench-panel);
  box-shadow: 0 16px 38px rgba(0, 0, 0, 0.18);
}

.ticket-workbench__hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  min-height: 118px;
  padding: 15px 16px;
  border-color: rgba(91, 141, 239, 0.25);
  background:
    radial-gradient(circle at 84% 0, rgba(33, 199, 217, 0.12), transparent 34%),
    linear-gradient(135deg, rgba(16, 31, 51, 0.96), rgba(8, 17, 31, 0.92));
}

.ticket-workbench__eyebrow {
  color: #9fc4ff;
  font-size: 11px;
  font-weight: 900;
  line-height: 1.35;
}

.ticket-workbench__hero h1 {
  margin-top: 6px;
  font-size: clamp(28px, 2.1vw, 36px);
  line-height: 1.06;
  letter-spacing: -0.02em;
}

.ticket-workbench__hero p {
  margin-top: 8px;
  color: var(--workbench-secondary);
  font-size: 14px;
  line-height: 1.45;
}

.ticket-workbench__hero small {
  display: block;
  margin-top: 8px;
  color: var(--workbench-muted);
  font-size: 11px;
  line-height: 1.45;
}

.ticket-workbench__hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(78px, 1fr));
  gap: 8px;
}

.ticket-workbench__hero-stats section {
  display: grid;
  gap: 5px;
  min-height: 72px;
  border: 1px solid rgba(151, 180, 214, 0.12);
  border-left: 3px solid var(--workbench-cyan);
  border-radius: 8px;
  padding: 10px;
  background: rgba(4, 9, 18, 0.38);
}

.ticket-workbench__hero-stats strong {
  color: var(--workbench-text);
  font: 700 22px/1 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.ticket-workbench__hero-stats span {
  color: var(--workbench-muted);
  font-size: 10.5px;
  line-height: 1.25;
}

.ticket-workbench__grid {
  display: grid;
  grid-template-columns: minmax(255px, 0.88fr) minmax(420px, 1.28fr) minmax(292px, 0.96fr);
  gap: 10px;
  min-width: 0;
  align-items: start;
}

.ticket-workbench__panel {
  min-width: 0;
  overflow: hidden;
}

.ticket-workbench__queue,
.ticket-workbench__evidence {
  display: grid;
  align-content: start;
  gap: 0;
}

.ticket-workbench__detail {
  display: grid;
  gap: 10px;
  background:
    radial-gradient(circle at 100% 0, rgba(61, 124, 255, 0.12), transparent 32%),
    linear-gradient(180deg, rgba(12, 23, 38, 0.94), rgba(6, 13, 24, 0.98));
}

.ticket-workbench__panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--workbench-border);
  padding: 12px;
}

.ticket-workbench__panel-heading--compact {
  padding-bottom: 10px;
}

.ticket-workbench__panel-heading h2 {
  margin-top: 2px;
  font-size: 16px;
  line-height: 1.2;
}

.ticket-workbench__panel-heading > span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 31px;
  min-height: 27px;
  border: 1px solid rgba(33, 199, 217, 0.26);
  border-radius: 7px;
  color: #9fe8f1;
  background: rgba(33, 199, 217, 0.08);
  font: 800 12px/1 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.ticket-workbench__search {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  margin: 10px;
  border: 1px solid rgba(151, 180, 214, 0.13);
  border-radius: 8px;
  padding: 8px 9px;
  background: rgba(4, 10, 22, 0.42);
}

.ticket-workbench__search span {
  color: #9fc4ff;
  font-size: 11px;
  font-weight: 900;
}

.ticket-workbench__search input {
  min-width: 0;
  border: 0;
  outline: 0;
  color: var(--workbench-text);
  background: transparent;
  font: inherit;
  font-size: 12px;
}

.ticket-workbench__search:focus-within {
  border-color: rgba(61, 124, 255, 0.48);
  box-shadow: 0 0 0 3px rgba(61, 124, 255, 0.12);
}

.ticket-workbench__filters {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  margin: 0 10px 10px;
}

.ticket-workbench button {
  font: inherit;
}

.ticket-workbench__filters button,
.ticket-workbench__action-bar button,
.ticket-workbench__review-actions button {
  min-height: 32px;
  border: 1px solid rgba(151, 180, 214, 0.14);
  border-radius: 7px;
  color: var(--workbench-secondary);
  background: rgba(10, 21, 38, 0.78);
  font-size: 11px;
  font-weight: 850;
  cursor: pointer;
}

.ticket-workbench__filters button:hover,
.ticket-workbench__action-bar button:hover,
.ticket-workbench__review-actions button:hover {
  border-color: rgba(61, 124, 255, 0.48);
  color: var(--workbench-text);
}

.ticket-workbench__filter--active {
  border-color: rgba(33, 199, 217, 0.42) !important;
  color: #dffbff !important;
  background: linear-gradient(135deg, rgba(61, 124, 255, 0.22), rgba(33, 199, 217, 0.08)) !important;
}

.ticket-workbench__ticket-list {
  display: grid;
  gap: 8px;
  max-height: 705px;
  padding: 0 10px 12px;
  overflow: auto;
}

.ticket-workbench__ticket-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 5px 8px;
  border: 1px solid rgba(151, 180, 214, 0.12);
  border-left: 3px solid var(--workbench-blue);
  border-radius: 8px;
  padding: 10px;
  color: var(--workbench-text);
  background: rgba(7, 16, 29, 0.72);
  text-align: left;
  cursor: pointer;
}

.ticket-workbench__ticket-card:hover,
.ticket-workbench__ticket-card--active {
  border-color: rgba(61, 124, 255, 0.5);
  background: linear-gradient(145deg, rgba(61, 124, 255, 0.18), rgba(139, 124, 246, 0.08));
}

.ticket-workbench__ticket-card--active {
  border-left-color: var(--workbench-cyan);
  box-shadow: inset 3px 0 0 rgba(33, 199, 217, 0.72);
}

.ticket-workbench__ticket-id,
.ticket-workbench__ticket-card small,
.ticket-workbench__ticket-card dt,
.ticket-workbench__ticket-card dd {
  color: var(--workbench-muted);
  font-size: 10.5px;
  line-height: 1.3;
}

.ticket-workbench__ticket-id,
.ticket-workbench__ticket-card dd {
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.ticket-workbench__ticket-card strong {
  grid-column: 1 / -1;
  color: var(--workbench-text);
  font-size: 12.5px;
  line-height: 1.32;
}

.ticket-workbench__ticket-card small {
  grid-column: 1 / -1;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.ticket-workbench__ticket-card dl {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 8px;
  border-top: 1px solid rgba(151, 180, 214, 0.08);
  padding-top: 7px;
}

.ticket-workbench__ticket-card div,
.ticket-workbench__meta div,
.ticket-workbench__analysis-pairs div,
.ticket-workbench__summary-list div {
  min-width: 0;
}

.ticket-workbench__ticket-card dt,
.ticket-workbench__meta dt,
.ticket-workbench__analysis-pairs dt,
.ticket-workbench__summary-list dt {
  color: var(--workbench-muted);
  font-size: 10px;
  font-weight: 850;
  line-height: 1.3;
}

.ticket-workbench__ticket-card dd {
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-workbench__priority {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  min-height: 24px;
  border-radius: 6px;
  font: 900 11px/1 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.ticket-workbench__priority[data-priority='P1'] {
  border: 1px solid rgba(255, 92, 122, 0.34);
  color: var(--workbench-red);
  background: rgba(255, 92, 122, 0.1);
}

.ticket-workbench__priority[data-priority='P2'] {
  border: 1px solid rgba(255, 180, 92, 0.32);
  color: var(--workbench-amber);
  background: rgba(255, 180, 92, 0.09);
}

.ticket-workbench__priority[data-priority='P3'] {
  border: 1px solid rgba(33, 199, 217, 0.32);
  color: var(--workbench-cyan);
  background: rgba(33, 199, 217, 0.08);
}

.ticket-workbench__chips,
.ticket-workbench__keyword-row {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.ticket-workbench__chips {
  grid-column: 1 / -1;
}

.ticket-workbench__chips span,
.ticket-workbench__keyword-row span {
  border: 1px solid rgba(33, 199, 217, 0.18);
  border-radius: 6px;
  padding: 3px 6px;
  color: #9fe8f1;
  background: rgba(33, 199, 217, 0.055);
  font-size: 10px;
  font-weight: 760;
}

.ticket-workbench__empty {
  display: grid;
  place-items: center;
  min-height: 96px;
  border: 1px dashed rgba(151, 180, 214, 0.2);
  border-radius: 8px;
  color: var(--workbench-muted);
  font-size: 12px;
}

.ticket-workbench__current {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  border-bottom: 1px solid var(--workbench-border);
  padding: 14px 15px;
}

.ticket-workbench__current h2 {
  margin-top: 4px;
  font-size: clamp(20px, 1.6vw, 27px);
  line-height: 1.15;
}

.ticket-workbench__current p {
  margin-top: 7px;
  color: var(--workbench-secondary);
  font-size: 12.5px;
  line-height: 1.5;
}

.ticket-workbench__status-stack {
  display: grid;
  gap: 7px;
  align-content: start;
}

.ticket-workbench__status-stack > span:not(.ticket-workbench__priority) {
  display: inline-flex;
  align-items: center;
  min-height: 25px;
  border: 1px solid rgba(151, 180, 214, 0.13);
  border-radius: 6px;
  padding: 0 8px;
  color: var(--workbench-secondary);
  background: rgba(4, 9, 18, 0.35);
  font-size: 10.5px;
  font-weight: 850;
}

.ticket-workbench__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-bottom: 1px solid var(--workbench-border);
}

.ticket-workbench__meta div {
  border-right: 1px solid rgba(151, 180, 214, 0.1);
  border-bottom: 1px solid rgba(151, 180, 214, 0.08);
  padding: 9px 12px;
}

.ticket-workbench__meta div:nth-child(3n) {
  border-right: 0;
}

.ticket-workbench__meta dd {
  margin-top: 4px;
  overflow-wrap: anywhere;
  color: var(--workbench-text);
  font-size: 12px;
  font-weight: 800;
}

.ticket-workbench__context-card,
.ticket-workbench__analysis-grid,
.ticket-workbench__risk {
  margin: 0 10px;
}

.ticket-workbench__context-card {
  padding: 11px 12px;
}

.ticket-workbench__section-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ticket-workbench__section-title span {
  display: inline-grid;
  min-width: 24px;
  min-height: 24px;
  place-items: center;
  border: 1px solid rgba(33, 199, 217, 0.22);
  border-radius: 6px;
  color: var(--workbench-cyan);
  background: rgba(33, 199, 217, 0.07);
  font: 900 11px/1 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.ticket-workbench__section-title h3 {
  font-size: 14px;
  line-height: 1.2;
}

.ticket-workbench__section-title em {
  margin-left: auto;
  border: 1px solid rgba(255, 180, 92, 0.22);
  border-radius: 6px;
  padding: 4px 7px;
  color: #ffd5a3;
  background: rgba(255, 180, 92, 0.06);
  font-size: 10px;
  font-style: normal;
  font-weight: 850;
}

.ticket-workbench__context-card ul,
.ticket-workbench__risk ul {
  display: grid;
  gap: 6px;
  margin: 9px 0 0;
  padding-left: 18px;
  color: var(--workbench-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.ticket-workbench__analysis-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(0, 0.92fr);
  gap: 10px;
}

.ticket-workbench__analysis-grid article {
  padding: 11px 12px;
  box-shadow: none;
}

.ticket-workbench__analysis-pairs,
.ticket-workbench__summary-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.ticket-workbench__analysis-pairs dd,
.ticket-workbench__summary-list dd {
  margin-top: 3px;
  color: var(--workbench-secondary);
  font-size: 11.5px;
  line-height: 1.45;
}

.ticket-workbench__draft p {
  margin-top: 10px;
  color: var(--workbench-secondary);
  font-size: 12px;
  line-height: 1.55;
}

.ticket-workbench__action-bar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
  margin-top: 11px;
}

.ticket-workbench__action-bar button:first-child {
  border-color: rgba(61, 124, 255, 0.48);
  color: #e8f0ff;
  background: linear-gradient(135deg, rgba(61, 124, 255, 0.55), rgba(33, 199, 217, 0.28));
}

.ticket-workbench__feedback {
  border: 1px solid rgba(255, 180, 92, 0.16);
  border-radius: 7px;
  padding: 8px;
  color: #ffd5a3 !important;
  background: rgba(255, 180, 92, 0.055);
  font-size: 10.5px !important;
}

.ticket-workbench__risk {
  display: grid;
  grid-template-columns: minmax(190px, 0.7fr) minmax(0, 1fr);
  gap: 12px;
  margin-bottom: 10px;
  padding: 11px 12px;
  border-color: rgba(255, 180, 92, 0.2);
  background: linear-gradient(135deg, rgba(255, 180, 92, 0.075), rgba(12, 23, 38, 0.9));
}

.ticket-workbench__risk h3 {
  font-size: 14px;
  color: #ffd5a3;
}

.ticket-workbench__risk p {
  margin-top: 5px;
  color: var(--workbench-muted);
  font-size: 11px;
  line-height: 1.45;
}

.ticket-workbench__risk ul {
  margin: 0;
}

.ticket-workbench__evidence {
  gap: 10px;
  padding: 0;
  background: transparent;
  border: 0;
  box-shadow: none;
}

.ticket-workbench__rail-card {
  overflow: hidden;
  box-shadow: none;
}

.ticket-workbench__rail-card > .ticket-workbench__section-title {
  padding: 11px 12px 0;
}

.ticket-workbench__evidence-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 5px 8px;
  border-top: 1px solid rgba(151, 180, 214, 0.09);
  padding: 7px 10px;
}

.ticket-workbench__evidence-item strong {
  color: #9fc4ff;
  font: 900 11px/1.2 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.ticket-workbench__evidence-item p {
  margin-top: 3px;
  color: var(--workbench-text);
  font-size: 10.8px;
  font-weight: 800;
  line-height: 1.22;
}

.ticket-workbench__evidence-item small {
  display: block;
  margin-top: 3px;
  color: var(--workbench-muted);
  font-size: 9px;
  line-height: 1.2;
}

.ticket-workbench__evidence-item b {
  align-self: start;
  border: 1px solid rgba(43, 216, 143, 0.26);
  border-radius: 6px;
  padding: 3px 6px;
  color: var(--workbench-green);
  background: rgba(43, 216, 143, 0.08);
  font-size: 9.5px;
}

.ticket-workbench__evidence-item b[data-used='false'] {
  border-color: rgba(255, 92, 122, 0.28);
  color: var(--workbench-red);
  background: rgba(255, 92, 122, 0.08);
}

.ticket-workbench__keyword-row,
.ticket-workbench__evidence-item dl {
  grid-column: 1 / -1;
}

.ticket-workbench__evidence-item dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.ticket-workbench__trace {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 5px;
  margin: 8px 0 0;
  padding: 0 10px 10px;
  list-style: none;
}

.ticket-workbench__trace li {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 4px;
  border: 1px solid rgba(151, 180, 214, 0.1);
  border-left: 3px solid var(--trace-color, var(--workbench-blue));
  border-radius: 7px;
  padding: 6px;
  background: rgba(4, 9, 18, 0.34);
}

.ticket-workbench__trace li[data-tone='cyan'] { --trace-color: var(--workbench-cyan); }
.ticket-workbench__trace li[data-tone='green'] { --trace-color: var(--workbench-green); }
.ticket-workbench__trace li[data-tone='amber'] { --trace-color: var(--workbench-amber); }
.ticket-workbench__trace li[data-tone='red'] { --trace-color: var(--workbench-red); }
.ticket-workbench__trace li[data-tone='violet'] { --trace-color: var(--workbench-violet); }

.ticket-workbench__trace strong {
  color: var(--workbench-text);
  font-size: 10.6px;
  line-height: 1.2;
}

.ticket-workbench__trace small {
  display: block;
  margin-top: 2px;
  color: var(--workbench-muted);
  font-size: 9px;
  line-height: 1.25;
}

.ticket-workbench__trace span,
.ticket-workbench__trace em {
  color: var(--workbench-secondary);
  font: 800 9px/1.2 "Cascadia Code", SFMono-Regular, Consolas, monospace;
  text-align: left;
}

.ticket-workbench__trace em {
  color: var(--workbench-muted);
  font-style: normal;
}

.ticket-workbench__summary-list {
  padding: 0 10px 10px;
}

.ticket-workbench__summary-list div {
  border-bottom: 1px solid rgba(151, 180, 214, 0.08);
  padding-bottom: 6px;
}

.ticket-workbench__summary-list--compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px 8px;
}

.ticket-workbench__summary-list--compact div:first-child {
  grid-column: 1 / -1;
}

.ticket-workbench__summary-list div:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.ticket-workbench__summary-list dd {
  overflow-wrap: anywhere;
}

.ticket-workbench__rail-card blockquote {
  margin: 0 10px 8px;
  border-left: 3px solid rgba(33, 199, 217, 0.55);
  border-radius: 0 7px 7px 0;
  padding: 7px 8px;
  color: var(--workbench-secondary);
  background: rgba(33, 199, 217, 0.055);
  font-size: 10.5px;
  line-height: 1.45;
}

.ticket-workbench__review-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 5px;
  padding: 0 10px 10px;
}

.ticket-workbench__review-actions button:first-child {
  color: var(--workbench-green);
  border-color: rgba(43, 216, 143, 0.3);
  background: rgba(43, 216, 143, 0.08);
}

.ticket-workbench__review-actions button:nth-child(2) {
  color: var(--workbench-amber);
  border-color: rgba(255, 180, 92, 0.3);
  background: rgba(255, 180, 92, 0.075);
}

.ticket-workbench__review-actions button:last-child {
  color: var(--workbench-red);
  border-color: rgba(255, 92, 122, 0.3);
  background: rgba(255, 92, 122, 0.075);
}

.ticket-workbench__rail-card--eval {
  border-color: rgba(139, 124, 246, 0.18);
}

@media (max-width: 1420px) {
  .ticket-workbench__hero {
    grid-template-columns: 1fr;
  }

  .ticket-workbench__hero-stats {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .ticket-workbench__grid {
    grid-template-columns: minmax(245px, 0.82fr) minmax(390px, 1.2fr) minmax(280px, 0.9fr);
  }

  .ticket-workbench__analysis-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1180px) {
  .ticket-workbench__grid,
  .ticket-workbench__risk {
    grid-template-columns: 1fr;
  }

  .ticket-workbench__ticket-list {
    max-height: none;
  }

  .ticket-workbench__evidence {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .ticket-workbench__hero-stats,
  .ticket-workbench__meta,
  .ticket-workbench__evidence,
  .ticket-workbench__action-bar,
  .ticket-workbench__review-actions {
    grid-template-columns: 1fr;
  }

  .ticket-workbench__current {
    grid-template-columns: 1fr;
  }

  .ticket-workbench__meta div {
    border-right: 0;
  }
}
</style>
