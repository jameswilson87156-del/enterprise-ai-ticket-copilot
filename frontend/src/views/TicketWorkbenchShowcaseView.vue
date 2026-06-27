<script setup lang="ts">
import { computed, ref } from 'vue'

type ShowcaseFilter = 'ALL' | 'PENDING' | 'REVIEW' | 'KNOWLEDGE'
type ShowcasePriority = 'P1' | 'P2' | 'P3'
type ShowcaseTone = 'danger' | 'warning' | 'info' | 'success'

interface ShowcaseTimelineItem {
  time: string
  title: string
  note: string
}

interface ShowcaseReference {
  id: string
  title: string
  source: string
  score: number
}

interface ShowcaseSimilarTicket {
  id: string
  title: string
  status: string
  tone: ShowcaseTone
}

interface ShowcaseTicket {
  id: string
  title: string
  summary: string
  requester: string
  department: string
  assignee: string
  source: string
  environment: string
  status: string
  priority: ShowcasePriority
  priorityNote: string
  category: string
  updatedAt: string
  sla: string
  impact: string
  confidence: number
  tags: string[]
  searchText: string
  classificationReason: string
  replyDraft: string
  riskNotes: string[]
  timeline: ShowcaseTimelineItem[]
  references: ShowcaseReference[]
  similarTickets: ShowcaseSimilarTicket[]
}

const showcaseFilters: Array<{ id: ShowcaseFilter; label: string }> = [
  { id: 'ALL', label: '全部' },
  { id: 'PENDING', label: '待处理' },
  { id: 'REVIEW', label: '待审核' },
  { id: 'KNOWLEDGE', label: '已沉淀' }
]

const showcaseTickets: ShowcaseTicket[] = [
  {
    id: 'DEMO-0005',
    title: '报价接口返回 500，销售无法生成报价单',
    summary: '销售在报价中心提交企业客户报价时接口返回 500，错误集中在华东区域，影响本日上午报价单生成。',
    requester: '林悦 / 销售运营',
    department: 'Sales Ops',
    assignee: '支持二组',
    source: 'quote-center',
    environment: 'internal',
    status: '处理中',
    priority: 'P1',
    priorityNote: '销售运营与客户成功团队受影响，需人工复核后推进。',
    category: '系统故障',
    updatedAt: '11:10',
    sla: '01:23:45',
    impact: '销售运营与客户成功团队',
    confidence: 92,
    tags: ['华东业务区', '今天 09:40 灰度发布', '系统故障 / P1'],
    searchText: 'quote-center pricing 500 sales ops 报价接口',
    classificationReason: '错误日志出现 500 与 PricePolicyService，且请求集中在报价中心灰度窗口后。',
    replyDraft:
      '您好，已收到报价接口异常反馈。当前判断与报价中心后端服务有关，支持人员会先确认灰度发布差异、关联 traceId 与依赖服务日志，再同步处理进度。',
    riskNotes: ['报价链路影响销售收入，降级或回滚必须人工确认。', '当前建议仅来自规则分类和知识引用，不代表真实 LLM 自动决策。'],
    timeline: [
      { time: '10:24', title: '待分类', note: '系统 / 工单进入规则分类队列。' },
      { time: '10:29', title: '待处理', note: '规则引擎 / 完成分类、知识命中和模板化建议草稿。' },
      { time: '10:44', title: '处理中', note: '支持人员 / 人工接手，关联 traceId 排查。' }
    ],
    references: [
      { id: 'KB-API-500', title: '接口 500 错误分层排查手册', source: '支持知识库 / 校验 2026-06-20', score: 88 },
      { id: 'KB-OPS-003', title: '系统故障超时与 5xx 排查手册', source: '运维知识库 / 校验 2026-06-19', score: 74 }
    ],
    similarTickets: [
      { id: 'DEMO-0003', title: 'Redis 连接失败导致会话校验超时', status: 'IN_PROGRESS', tone: 'success' },
      { id: 'DEMO-0002', title: 'Spring Boot 启动时报 BeanCreationException', status: 'PENDING_PROCESS', tone: 'info' },
      { id: 'DEMO-0001', title: 'Java 服务启动失败，提示 8080 端口已被占用', status: 'IN_PROGRESS', tone: 'success' }
    ]
  },
  {
    id: 'DEMO-0003',
    title: 'Redis 连接失败导致会话校验超时',
    summary: '员工门户间歇性登录失败，日志出现 KB-REDIS-CONN 相关连接池耗尽告警。',
    requester: '周宁 / 员工服务',
    department: 'Employee Portal',
    assignee: '平台支持',
    source: 'employee-portal',
    environment: 'prod-cn',
    status: '处理中',
    priority: 'P1',
    priorityNote: '员工登录链路受影响，需优先处理连接池与超时阈值。',
    category: '系统故障',
    updatedAt: '10:30',
    sla: '00:52:18',
    impact: '员工门户登录',
    confidence: 95,
    tags: ['KB-REDIS-CONN', 'employee-portal', '连接池'],
    searchText: 'KB-REDIS-CONN Redis 连接失败 session timeout employee portal',
    classificationReason: '命中 Redis、连接失败、会话超时和 KB-REDIS-CONN 关键词。',
    replyDraft: '您好，当前登录超时与 Redis 连接池状态相关。支持人员会先核对连接池水位、近期部署和慢请求日志。',
    riskNotes: ['登录链路存在放大风险，需确认是否触发限流或临时扩容。'],
    timeline: [
      { time: '10:12', title: '待分类', note: '系统 / 工单进入分类队列。' },
      { time: '10:18', title: '待处理', note: '规则引擎 / 命中 Redis 连接失败知识条目。' },
      { time: '10:30', title: '处理中', note: '平台支持 / 人工接手排查连接池。' }
    ],
    references: [
      { id: 'KB-REDIS-CONN', title: 'Redis 连接池耗尽排查手册', source: '平台知识库 / 校验 2026-06-18', score: 91 },
      { id: 'KB-LOGIN-012', title: '员工门户会话超时处理流程', source: '员工服务知识库', score: 78 }
    ],
    similarTickets: [
      { id: 'DEMO-0001', title: 'Java 服务启动失败，提示 8080 端口已被占用', status: 'RESOLVED', tone: 'success' },
      { id: 'DEMO-0006', title: 'HR 权限审批后仍无法访问报表', status: 'PENDING_PROCESS', tone: 'warning' }
    ]
  },
  {
    id: 'DEMO-0002',
    title: 'Spring Boot 启动时报 BeanCreationException',
    summary: '支付服务启动失败，BeanCreationException 指向数据源配置缺失。',
    requester: '李雷 / 支付研发',
    department: 'Payment',
    assignee: '后端支持',
    source: 'payment-service',
    environment: 'staging',
    status: '待处理',
    priority: 'P1',
    priorityNote: '影响测试环境发布验证，建议后端支持优先介入。',
    category: '系统故障',
    updatedAt: '10:00',
    sla: '02:18:30',
    impact: '支付服务验证',
    confidence: 89,
    tags: ['payment-service', 'BeanCreationException', 'datasource'],
    searchText: 'Spring Boot BeanCreationException datasource payment-service',
    classificationReason: '错误日志命中 BeanCreationException 与 datasource。',
    replyDraft: '您好，初步看是启动配置缺失。请补充 active profile、datasource 配置和启动日志前后 80 行。',
    riskNotes: ['测试环境阻塞发布验证，需确认是否同样影响生产配置模板。'],
    timeline: [
      { time: '09:52', title: '待分类', note: '系统 / 接收启动失败日志。' },
      { time: '10:00', title: '待处理', note: '规则引擎 / 命中启动失败模板。' }
    ],
    references: [{ id: 'KB-SPRING-010', title: 'Spring Boot 数据源配置缺失排查', source: '后端知识库', score: 82 }],
    similarTickets: [{ id: 'DEMO-0001', title: 'Java 服务启动失败，提示 8080 端口已被占用', status: 'RESOLVED', tone: 'success' }]
  },
  {
    id: 'DEMO-0004',
    title: 'CRM 客户列表查询超过 12 秒',
    summary: '客户成功团队反馈 CRM 客户列表查询慢，慢 SQL 集中在客户标签 join。',
    requester: '韩笙 / 客户成功',
    department: 'Customer Success',
    assignee: '数据支持',
    source: 'crm-analytics',
    environment: 'prod-cn',
    status: '待处理',
    priority: 'P2',
    priorityNote: '影响客服查询效率，未阻塞交易链路。',
    category: '数据问题',
    updatedAt: '09:50',
    sla: '04:42:11',
    impact: '客户成功团队',
    confidence: 84,
    tags: ['crm-analytics', 'slow-query', 'customer-tag'],
    searchText: 'CRM slow query customer list analytics',
    classificationReason: '命中慢查询、客户列表和数据分析关键词。',
    replyDraft: '您好，已记录 CRM 查询慢问题。建议先确认时间范围、筛选条件和慢 SQL 样本，再评估索引或缓存策略。',
    riskNotes: ['性能优化需避免直接改生产索引，建议先压测和回放。'],
    timeline: [
      { time: '09:38', title: '待分类', note: '系统 / 接收性能问题。' },
      { time: '09:50', title: '待处理', note: '规则引擎 / 命中慢查询处理模板。' }
    ],
    references: [{ id: 'KB-SQL-021', title: '慢 SQL 初步定位与回放流程', source: '数据知识库', score: 76 }],
    similarTickets: [{ id: 'DEMO-0008', title: '知识库条目已沉淀：慢 SQL 处理流程', status: 'KNOWLEDGE_BASED', tone: 'success' }]
  },
  {
    id: 'DEMO-0001',
    title: 'Java 服务启动失败，提示 8080 端口已被占用',
    summary: '订单服务本地启动失败，提示 8080 端口占用，需要判断是否为本机进程冲突。',
    requester: '李皓 / 订单研发',
    department: 'Order Service',
    assignee: '支持一组',
    source: 'order-service',
    environment: 'local-dev',
    status: '处理中',
    priority: 'P2',
    priorityNote: '不影响生产，但阻塞研发本地调试。',
    category: '系统故障',
    updatedAt: '09:10',
    sla: '06:18:10',
    impact: '研发本地启动',
    confidence: 86,
    tags: ['8080', 'order-service', 'local-dev'],
    searchText: 'Java 8080 port occupied order service',
    classificationReason: '错误日志命中端口占用和 Java 服务启动失败。',
    replyDraft: '您好，请先确认 8080 端口占用进程，再决定切换本地端口或停止冲突进程。',
    riskNotes: ['本地排查不应自动结束进程，需研发确认。'],
    timeline: [
      { time: '08:58', title: '待分类', note: '系统 / 接收启动失败问题。' },
      { time: '09:10', title: '处理中', note: '支持一组 / 人工确认处理。' }
    ],
    references: [{ id: 'KB-PORT-8080', title: '本地端口占用排查', source: '研发支持知识库', score: 80 }],
    similarTickets: [{ id: 'DEMO-0002', title: 'Spring Boot 启动时报 BeanCreationException', status: 'PENDING_PROCESS', tone: 'info' }]
  },
  {
    id: 'DEMO-0006',
    title: 'HR 权限审批后仍无法访问报表',
    summary: 'HR 已通过权限审批，但用户访问人事报表仍提示无权限。',
    requester: '钱璐 / HRBP',
    department: 'HR',
    assignee: '权限支持',
    source: 'hr-reporting',
    environment: 'prod-cn',
    status: '待处理',
    priority: 'P3',
    priorityNote: '单用户权限问题，需核对审批流与 RBAC 同步。',
    category: '权限问题',
    updatedAt: '08:55',
    sla: '08:34:00',
    impact: 'HR 报表访问',
    confidence: 83,
    tags: ['RBAC', 'HR report', 'permission'],
    searchText: 'HR permission RBAC report access',
    classificationReason: '命中权限、审批、RBAC 与报表访问关键词。',
    replyDraft: '您好，请提供审批单号和报表路径。支持人员会核对审批完成时间与权限同步记录。',
    riskNotes: ['权限变更必须人工复核，不做自动授权。'],
    timeline: [
      { time: '08:44', title: '待分类', note: '系统 / 接收权限问题。' },
      { time: '08:55', title: '待处理', note: '规则引擎 / 命中权限问题模板。' }
    ],
    references: [{ id: 'KB-RBAC-006', title: 'RBAC 审批后同步延迟排查', source: '权限知识库', score: 79 }],
    similarTickets: [{ id: 'DEMO-0003', title: 'Redis 连接失败导致会话校验超时', status: 'IN_PROGRESS', tone: 'success' }]
  },
  {
    id: 'DEMO-0007',
    title: 'VPN 策略变更后供应商无法访问测试环境',
    summary: '供应商反馈 VPN 可连接但测试环境接口不可达，疑似策略组变更后网段未同步。',
    requester: '赵敏 / 供应商管理',
    department: 'Vendor Ops',
    assignee: '网络支持',
    source: 'vpn-gateway',
    environment: 'test-net',
    status: '待审核',
    priority: 'P2',
    priorityNote: '外部供应商联调受阻，需要审核后推进策略核对。',
    category: '权限问题',
    updatedAt: '08:40',
    sla: '05:12:08',
    impact: '供应商联调',
    confidence: 81,
    tags: ['VPN', 'vendor', 'network-policy'],
    searchText: 'VPN vendor network policy test environment',
    classificationReason: '命中 VPN、供应商、测试环境和策略变更关键词。',
    replyDraft: '您好，建议先核对 VPN 策略组、目标网段和最近变更记录，再由网络支持人工确认是否放通。',
    riskNotes: ['网络放通属于高风险变更，必须人工审核。'],
    timeline: [
      { time: '08:20', title: '待分类', note: '系统 / 接收外部访问问题。' },
      { time: '08:33', title: '待审核', note: '规则引擎 / 标记需 Human Review。' }
    ],
    references: [{ id: 'KB-VPN-014', title: 'VPN 策略变更后访问不可达排查', source: '网络知识库', score: 77 }],
    similarTickets: [{ id: 'DEMO-0006', title: 'HR 权限审批后仍无法访问报表', status: 'PENDING_PROCESS', tone: 'warning' }]
  },
  {
    id: 'DEMO-0008',
    title: '知识库条目已沉淀：慢 SQL 处理流程',
    summary: '已从历史 CRM 慢查询工单沉淀为知识条目，用于后续相似问题引用。',
    requester: '系统 / 知识沉淀',
    department: 'Knowledge Ops',
    assignee: '知识负责人',
    source: 'knowledge-base',
    environment: 'docs',
    status: '已沉淀',
    priority: 'P3',
    priorityNote: '历史问题已进入知识库，可作为参考引用。',
    category: '知识沉淀',
    updatedAt: '昨天',
    sla: '-',
    impact: '后续慢查询处理',
    confidence: 90,
    tags: ['KB-SQL-021', 'knowledge-based', 'slow-query'],
    searchText: 'knowledge based slow sql KB-SQL-021 已沉淀',
    classificationReason: '该条目为已确认知识沉淀，不代表自动生成后直接发布。',
    replyDraft: '可引用该知识条目作为慢 SQL 排查流程参考，仍需结合当前工单上下文人工确认。',
    riskNotes: ['知识引用只辅助排查，不能替代当前生产变更审批。'],
    timeline: [
      { time: '昨天', title: '已解决', note: '支持人员 / 人工确认处理完成。' },
      { time: '昨天', title: '已沉淀', note: '知识负责人 / 人工确认发布知识条目。' }
    ],
    references: [{ id: 'KB-SQL-021', title: '慢 SQL 初步定位与回放流程', source: '数据知识库', score: 96 }],
    similarTickets: [{ id: 'DEMO-0004', title: 'CRM 客户列表查询超过 12 秒', status: 'PENDING_PROCESS', tone: 'info' }]
  }
]

const selectedTicketId = ref('DEMO-0005')
const searchKeyword = ref('')
const activeFilter = ref<ShowcaseFilter>('ALL')
const draftGenerated = ref(false)
const draftConfirmed = ref(false)

const filteredTickets = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return showcaseTickets.filter((ticket) => {
    const matchesKeyword =
      !keyword ||
      [ticket.id, ticket.title, ticket.summary, ticket.requester, ticket.category, ticket.source, ticket.searchText].some((value) =>
        value.toLowerCase().includes(keyword)
      )
    const matchesFilter =
      activeFilter.value === 'ALL' ||
      (activeFilter.value === 'PENDING' && ['待分类', '待处理', '处理中'].includes(ticket.status)) ||
      (activeFilter.value === 'REVIEW' && ['待审核', '处理中'].includes(ticket.status)) ||
      (activeFilter.value === 'KNOWLEDGE' && ticket.status === '已沉淀')
    return matchesKeyword && matchesFilter
  })
})

const selectedTicket = computed(() => showcaseTickets.find((ticket) => ticket.id === selectedTicketId.value) ?? showcaseTickets[0])

const queueCounts = computed(() => ({
  all: showcaseTickets.length,
  review: showcaseTickets.filter((ticket) => ['待审核', '处理中'].includes(ticket.status)).length,
  p1: showcaseTickets.filter((ticket) => ticket.priority === 'P1').length
}))

const selectTicket = (ticketId: string) => {
  selectedTicketId.value = ticketId
  draftGenerated.value = false
  draftConfirmed.value = false
}

const generateDraft = () => {
  draftGenerated.value = true
  draftConfirmed.value = false
}

const confirmDraft = () => {
  draftGenerated.value = true
  draftConfirmed.value = true
}
</script>

<template>
  <section class="showcase-ticket-workbench" data-screenshot="ticket-detail" aria-label="Ticket Workbench Showcase">
    <header class="showcase-workbench-header">
      <div>
        <p class="showcase-eyebrow">Ticket Workbench / Local showcase</p>
        <h1>工单处理工作台</h1>
      </div>
      <div class="showcase-header-metrics" aria-label="本地演示队列摘要">
        <span><strong>{{ queueCounts.all }}</strong> demo tickets</span>
        <span><strong>{{ queueCounts.review }}</strong> human review</span>
        <span><strong>{{ queueCounts.p1 }}</strong> P1 focus</span>
      </div>
    </header>

    <section class="showcase-workspace-grid" aria-label="左队列、中详情、右 Copilot 三栏工作台">
      <aside class="showcase-panel showcase-queue-panel" aria-label="工单队列">
        <div class="showcase-panel-heading">
          <div>
            <p class="showcase-eyebrow">Queue</p>
            <h2>待处理队列</h2>
          </div>
          <span class="showcase-count-badge">{{ filteredTickets.length }}</span>
        </div>

        <label class="showcase-search" for="showcase-ticket-search">
          <span>搜索</span>
          <input
            id="showcase-ticket-search"
            v-model="searchKeyword"
            type="search"
            aria-label="搜索"
            placeholder="搜索工单、问题描述、知识库..."
          />
        </label>

        <div class="showcase-filter-group" aria-label="工单筛选">
          <button
            v-for="filter in showcaseFilters"
            :key="filter.id"
            type="button"
            class="showcase-filter-button"
            :class="{ 'showcase-filter-button--active': activeFilter === filter.id }"
            @click="activeFilter = filter.id"
          >
            {{ filter.label }}
          </button>
        </div>

        <div class="showcase-ticket-list" aria-live="polite">
          <button
            v-for="ticket in filteredTickets"
            :key="ticket.id"
            type="button"
            class="showcase-ticket-card"
            :class="{ 'showcase-ticket-card--active': ticket.id === selectedTicket.id }"
            @click="selectTicket(ticket.id)"
          >
            <span class="showcase-ticket-priority" :data-priority="ticket.priority">{{ ticket.priority }}</span>
            <span class="showcase-ticket-time">{{ ticket.updatedAt }}</span>
            <strong>{{ ticket.id }}</strong>
            <b>{{ ticket.title }}</b>
            <small>{{ ticket.requester }}</small>
            <span class="showcase-ticket-tags">
              <em v-for="tag in ticket.tags.slice(0, 2)" :key="tag">{{ tag }}</em>
            </span>
          </button>
          <p v-if="!filteredTickets.length" class="showcase-empty-state">暂无匹配工单</p>
        </div>
      </aside>

      <article class="showcase-panel showcase-detail-panel" aria-label="当前工单详情">
        <div class="showcase-detail-header">
          <div>
            <p class="showcase-eyebrow">Ticket Detail</p>
            <h1>{{ selectedTicket.title }}</h1>
            <p>{{ selectedTicket.summary }}</p>
          </div>
          <div class="showcase-status-stack">
            <span class="showcase-priority-pill" :data-priority="selectedTicket.priority">{{ selectedTicket.priority }}</span>
            <span class="showcase-state-pill">{{ selectedTicket.status }}</span>
          </div>
        </div>

        <dl class="showcase-metadata-grid">
          <div>
            <dt>Ticket ID</dt>
            <dd>{{ selectedTicket.id }}</dd>
          </div>
          <div>
            <dt>Requester</dt>
            <dd>{{ selectedTicket.requester }}</dd>
          </div>
          <div>
            <dt>Assignee</dt>
            <dd>{{ selectedTicket.assignee }}</dd>
          </div>
          <div>
            <dt>Source</dt>
            <dd>{{ selectedTicket.source }}</dd>
          </div>
          <div>
            <dt>Category</dt>
            <dd>{{ selectedTicket.category }}</dd>
          </div>
          <div>
            <dt>Environment</dt>
            <dd>{{ selectedTicket.environment }}</dd>
          </div>
          <div>
            <dt>SLA</dt>
            <dd>{{ selectedTicket.sla }}</dd>
          </div>
          <div>
            <dt>Impact</dt>
            <dd>{{ selectedTicket.impact }}</dd>
          </div>
        </dl>

        <section class="showcase-detail-section">
          <div class="showcase-section-heading">
            <span>01</span>
            <h2>工单描述</h2>
          </div>
          <p>{{ selectedTicket.summary }}</p>
          <div class="showcase-tag-row">
            <span v-for="tag in selectedTicket.tags" :key="tag">{{ tag }}</span>
          </div>
        </section>

        <section class="showcase-detail-section showcase-timeline-section">
          <div class="showcase-section-heading">
            <span>02</span>
            <h2>状态时间线</h2>
          </div>
          <ol class="showcase-timeline">
            <li v-for="item in selectedTicket.timeline" :key="`${selectedTicket.id}-${item.time}-${item.title}`">
              <time>{{ item.time }}</time>
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.note }}</p>
              </div>
            </li>
          </ol>
        </section>

        <div class="showcase-action-bar" aria-label="人工处理动作">
          <button type="button" class="showcase-action-button showcase-action-button--muted">开始处理</button>
          <button type="button" class="showcase-action-button">请求修改</button>
          <button type="button" class="showcase-action-button showcase-action-button--primary">人工确认</button>
        </div>
      </article>

      <aside class="showcase-panel showcase-copilot-panel" data-screenshot="ai-analysis" aria-label="AI Copilot 辅助分析面板">
        <div class="showcase-panel-heading showcase-panel-heading--compact">
          <div>
            <p class="showcase-eyebrow">AI Copilot</p>
            <h2>辅助分析面板</h2>
          </div>
          <span class="showcase-connected-pill">已接入 local-rule fallback</span>
        </div>

        <div class="showcase-signal-grid">
          <section>
            <span>规则分类建议</span>
            <strong>{{ selectedTicket.category }}</strong>
            <p>local-rule fallback / 置信度 {{ selectedTicket.confidence }}%</p>
          </section>
          <section>
            <span>优先级判断</span>
            <strong>{{ selectedTicket.priority }}</strong>
            <p>{{ selectedTicket.priorityNote }}</p>
          </section>
          <section>
            <span>预计影响</span>
            <strong>{{ selectedTicket.impact }}</strong>
            <p>来自工单上下文，非自动决策。</p>
          </section>
        </div>

        <div class="showcase-copilot-pair">
          <section class="showcase-copilot-block">
            <div class="showcase-block-title">
              <h3>相似工单 Top 3</h3>
              <span>{{ selectedTicket.similarTickets.length }}</span>
            </div>
            <div class="showcase-similar-list">
              <article v-for="item in selectedTicket.similarTickets" :key="`${selectedTicket.id}-${item.id}`">
                <div>
                  <strong>{{ item.id }}</strong>
                  <p>{{ item.title }}</p>
                </div>
                <span :data-tone="item.tone">{{ item.status }}</span>
              </article>
            </div>
          </section>

          <section class="showcase-copilot-block">
            <div class="showcase-block-title">
              <h3>RAG 知识引用</h3>
              <span>{{ selectedTicket.references.length }}</span>
            </div>
            <div class="showcase-reference-list">
              <article v-for="reference in selectedTicket.references" :key="`${selectedTicket.id}-${reference.id}`">
                <div>
                  <strong>{{ reference.id }}</strong>
                  <p>{{ reference.title }}</p>
                  <small>{{ reference.source }}</small>
                </div>
                <b>{{ reference.score }}%</b>
              </article>
            </div>
          </section>
        </div>

        <section class="showcase-copilot-block showcase-draft-block">
          <div class="showcase-block-title">
            <h3>模板化排查草稿</h3>
            <span>draft</span>
          </div>
          <p>{{ selectedTicket.replyDraft }}</p>
          <ol>
            <li>使用 traceId 关联网关、应用日志和依赖调用。</li>
            <li>确认输入来源和最近发布差异。</li>
          </ol>
        </section>

        <section class="showcase-risk-block" aria-label="风险提示">
          <h3>风险提示</h3>
          <p v-for="risk in selectedTicket.riskNotes" :key="risk">{{ risk }}</p>
        </section>

        <section class="showcase-review-block" aria-label="Human Review">
          <div class="showcase-block-title">
            <h3>Human Review</h3>
            <span>visible</span>
          </div>
          <div class="showcase-review-actions">
            <button type="button" class="showcase-review-button showcase-review-button--approve">Approve</button>
            <button type="button" class="showcase-review-button showcase-review-button--changes">Request Changes</button>
            <button type="button" class="showcase-review-button showcase-review-button--reject">Reject</button>
          </div>
          <div class="showcase-knowledge-actions">
            <button type="button" class="showcase-link-button" @click="generateDraft">生成知识草稿</button>
            <button type="button" class="showcase-link-button" @click="confirmDraft">人工确认入库</button>
          </div>
          <p v-if="draftGenerated && !draftConfirmed" class="showcase-feedback">知识草稿已生成，发布前仍需人工审核。</p>
          <p v-if="draftConfirmed" class="showcase-feedback">知识草稿已由人工确认并完成沉淀。</p>
        </section>
      </aside>
    </section>
  </section>
</template>

<style scoped>
.showcase-ticket-workbench {
  --showcase-canvas: #07101d;
  --showcase-canvas-deep: #040912;
  --showcase-panel: #0c1726;
  --showcase-panel-strong: #101f33;
  --showcase-border: rgba(151, 180, 214, 0.16);
  --showcase-border-strong: rgba(91, 141, 239, 0.28);
  --showcase-text: #eef5ff;
  --showcase-secondary: #c7d5ea;
  --showcase-muted: #7e91ab;
  --showcase-blue: #3d7cff;
  --showcase-cyan: #21c7d9;
  --showcase-green: #2bd88f;
  --showcase-orange: #ffb45c;
  --showcase-red: #ff5c7a;
  --showcase-purple: #8b7cf6;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  min-height: calc(100vh - 104px);
  color: var(--showcase-text);
}

.showcase-workbench-header,
.showcase-panel {
  border: 1px solid var(--showcase-border);
  background:
    linear-gradient(180deg, rgba(16, 31, 51, 0.92), rgba(8, 17, 31, 0.96)),
    var(--showcase-panel);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.26);
}

.showcase-workbench-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 56px;
  padding: 10px 14px;
  border-radius: 8px;
}

.showcase-workbench-header h1,
.showcase-panel-heading h2,
.showcase-detail-header h1,
.showcase-section-heading h2,
.showcase-block-title h3 {
  margin: 0;
  letter-spacing: 0;
}

.showcase-workbench-header h1 {
  font-size: 24px;
  line-height: 1.12;
}

.showcase-eyebrow {
  margin: 0 0 4px;
  color: #8db9ff;
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
}

.showcase-header-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.showcase-header-metrics span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid rgba(91, 141, 239, 0.18);
  border-radius: 6px;
  background: rgba(4, 10, 22, 0.42);
  color: var(--showcase-secondary);
  font-size: 12px;
}

.showcase-header-metrics strong {
  color: var(--showcase-cyan);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
}

.showcase-workspace-grid {
  display: grid;
  grid-template-columns: minmax(290px, 330px) minmax(0, 1fr) minmax(340px, 390px);
  gap: 12px;
  min-height: 0;
}

.showcase-panel {
  min-width: 0;
  min-height: 0;
  border-radius: 8px;
  overflow: hidden;
}

.showcase-queue-panel,
.showcase-detail-panel,
.showcase-copilot-panel {
  display: flex;
  flex-direction: column;
}

.showcase-panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border-bottom: 1px solid var(--showcase-border);
}

.showcase-panel-heading h2 {
  font-size: 18px;
}

.showcase-panel-heading--compact {
  padding-bottom: 12px;
}

.showcase-count-badge,
.showcase-connected-pill,
.showcase-state-pill,
.showcase-priority-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  font-weight: 800;
}

.showcase-count-badge {
  min-width: 34px;
  height: 28px;
  color: var(--showcase-text);
  background: rgba(61, 124, 255, 0.2);
  border: 1px solid rgba(61, 124, 255, 0.42);
}

.showcase-connected-pill {
  max-width: 160px;
  min-height: 26px;
  padding: 0 9px;
  color: var(--showcase-green);
  background: rgba(43, 216, 143, 0.1);
  border: 1px solid rgba(43, 216, 143, 0.26);
  text-align: center;
}

.showcase-search {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  margin: 12px;
  padding: 9px 10px;
  border: 1px solid var(--showcase-border);
  border-radius: 8px;
  background: rgba(4, 10, 22, 0.42);
}

.showcase-search span {
  color: #9fc4ff;
  font-size: 12px;
  font-weight: 800;
}

.showcase-search input {
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--showcase-text);
  font: inherit;
}

.showcase-search input::placeholder {
  color: rgba(199, 213, 234, 0.54);
}

.showcase-search:focus-within {
  border-color: rgba(61, 124, 255, 0.5);
  box-shadow: 0 0 0 3px rgba(61, 124, 255, 0.12);
}

.showcase-filter-group {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  margin: 0 12px 12px;
}

.showcase-filter-button,
.showcase-action-button,
.showcase-review-button,
.showcase-link-button {
  border: 1px solid var(--showcase-border);
  border-radius: 7px;
  background: rgba(10, 21, 38, 0.84);
  color: var(--showcase-secondary);
  font: inherit;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    color 160ms ease,
    transform 160ms ease;
}

.showcase-filter-button {
  min-height: 34px;
}

.showcase-filter-button:hover,
.showcase-action-button:hover,
.showcase-review-button:hover,
.showcase-link-button:hover {
  transform: translateY(-1px);
  border-color: rgba(61, 124, 255, 0.46);
  color: var(--showcase-text);
}

.showcase-filter-button--active {
  color: var(--showcase-text);
  background: linear-gradient(180deg, rgba(61, 124, 255, 0.34), rgba(61, 124, 255, 0.15));
  border-color: rgba(61, 124, 255, 0.62);
}

.showcase-ticket-list {
  display: grid;
  gap: 8px;
  min-height: 0;
  padding: 0 12px 14px;
  overflow: auto;
}

.showcase-ticket-card {
  position: relative;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 5px 8px;
  min-height: 108px;
  padding: 12px 10px 11px 14px;
  border: 1px solid rgba(151, 180, 214, 0.13);
  border-left: 4px solid var(--showcase-orange);
  border-radius: 8px;
  background: rgba(8, 18, 33, 0.76);
  color: var(--showcase-text);
  text-align: left;
}

.showcase-ticket-card:hover {
  border-color: rgba(61, 124, 255, 0.38);
}

.showcase-ticket-card--active {
  border-color: rgba(61, 124, 255, 0.68);
  border-left-color: var(--showcase-red);
  background: linear-gradient(145deg, rgba(61, 124, 255, 0.2), rgba(139, 124, 246, 0.12));
  box-shadow: 0 0 0 1px rgba(61, 124, 255, 0.24), 0 0 24px rgba(61, 124, 255, 0.2);
}

.showcase-ticket-card strong,
.showcase-ticket-card small {
  color: var(--showcase-muted);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 11px;
}

.showcase-ticket-card b {
  grid-column: 1 / -1;
  font-size: 13px;
  line-height: 1.35;
}

.showcase-ticket-time {
  color: #9fc4ff;
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 11px;
}

.showcase-ticket-priority {
  justify-self: end;
  padding: 3px 6px;
  border-radius: 6px;
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  font-weight: 900;
}

.showcase-ticket-priority[data-priority='P1'],
.showcase-priority-pill[data-priority='P1'] {
  color: var(--showcase-red);
  background: rgba(255, 92, 122, 0.12);
  border: 1px solid rgba(255, 92, 122, 0.34);
}

.showcase-ticket-priority[data-priority='P2'],
.showcase-priority-pill[data-priority='P2'] {
  color: var(--showcase-orange);
  background: rgba(255, 180, 92, 0.1);
  border: 1px solid rgba(255, 180, 92, 0.32);
}

.showcase-ticket-priority[data-priority='P3'],
.showcase-priority-pill[data-priority='P3'] {
  color: var(--showcase-cyan);
  background: rgba(33, 199, 217, 0.1);
  border: 1px solid rgba(33, 199, 217, 0.32);
}

.showcase-ticket-tags {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.showcase-ticket-tags em,
.showcase-tag-row span {
  border: 1px solid rgba(33, 199, 217, 0.24);
  border-radius: 6px;
  background: rgba(33, 199, 217, 0.08);
  color: #9feaff;
  font-size: 11px;
  font-style: normal;
}

.showcase-ticket-tags em {
  padding: 3px 7px;
}

.showcase-empty-state {
  display: grid;
  place-items: center;
  min-height: 112px;
  margin: 0;
  border: 1px dashed rgba(151, 180, 214, 0.22);
  border-radius: 8px;
  color: var(--showcase-muted);
}

.showcase-detail-panel {
  background:
    radial-gradient(circle at 82% 0, rgba(61, 124, 255, 0.14), transparent 34%),
    linear-gradient(180deg, rgba(12, 23, 38, 0.94), rgba(6, 13, 24, 0.98));
}

.showcase-detail-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  padding: 16px;
  border-bottom: 1px solid var(--showcase-border);
}

.showcase-detail-header h1 {
  max-width: 760px;
  font-size: 24px;
  line-height: 1.18;
}

.showcase-detail-header p,
.showcase-detail-section p,
.showcase-draft-block p,
.showcase-risk-block p {
  margin: 8px 0 0;
  color: var(--showcase-secondary);
  font-size: 13px;
  line-height: 1.72;
}

.showcase-status-stack {
  display: grid;
  gap: 8px;
  align-content: start;
}

.showcase-priority-pill,
.showcase-state-pill {
  min-width: 66px;
  min-height: 30px;
  padding: 0 10px;
}

.showcase-state-pill {
  color: #cfe6ff;
  background: rgba(61, 124, 255, 0.16);
  border: 1px solid rgba(61, 124, 255, 0.32);
}

.showcase-metadata-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 0;
  border-bottom: 1px solid var(--showcase-border);
}

.showcase-metadata-grid div {
  min-width: 0;
  padding: 12px 14px;
  border-right: 1px solid rgba(151, 180, 214, 0.12);
  border-bottom: 1px solid rgba(151, 180, 214, 0.1);
}

.showcase-metadata-grid div:nth-child(4n) {
  border-right: 0;
}

.showcase-metadata-grid dt {
  color: #8fb0d5;
  font-size: 11px;
  font-weight: 800;
}

.showcase-metadata-grid dd {
  margin: 6px 0 0;
  color: var(--showcase-text);
  font-size: 13px;
  font-weight: 800;
  overflow-wrap: anywhere;
}

.showcase-detail-section {
  padding: 14px 16px;
  border-bottom: 1px solid var(--showcase-border);
}

.showcase-section-heading {
  display: flex;
  align-items: center;
  gap: 9px;
}

.showcase-section-heading span {
  color: var(--showcase-cyan);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  font-weight: 900;
}

.showcase-section-heading h2 {
  font-size: 16px;
}

.showcase-tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 12px;
}

.showcase-tag-row span {
  padding: 5px 9px;
}

.showcase-timeline {
  display: grid;
  gap: 10px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.showcase-timeline li {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 12px;
  position: relative;
}

.showcase-timeline li::before {
  content: '';
  position: absolute;
  left: 63px;
  top: 8px;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--showcase-cyan);
  box-shadow: 0 0 0 4px rgba(33, 199, 217, 0.12);
}

.showcase-timeline time {
  color: var(--showcase-cyan);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  font-weight: 900;
}

.showcase-timeline strong {
  color: var(--showcase-text);
  font-size: 13px;
}

.showcase-timeline p {
  margin: 4px 0 0;
  color: var(--showcase-secondary);
  font-size: 12px;
}

.showcase-action-bar {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: auto;
  padding: 14px 16px 16px;
}

.showcase-action-button {
  min-height: 38px;
}

.showcase-action-button--primary {
  color: #ffffff;
  background: linear-gradient(135deg, #2f6bff, #22a8ec);
  border-color: rgba(84, 166, 255, 0.6);
}

.showcase-action-button--muted {
  opacity: 0.58;
}

.showcase-copilot-panel {
  overflow: auto;
}

.showcase-signal-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
}

.showcase-signal-grid section {
  min-width: 0;
  padding: 11px 10px;
  border: 1px solid rgba(91, 141, 239, 0.18);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(22, 33, 65, 0.72), rgba(11, 21, 38, 0.9));
}

.showcase-signal-grid span,
.showcase-block-title span {
  color: #8fb0d5;
  font-size: 11px;
  font-weight: 900;
}

.showcase-signal-grid strong {
  display: block;
  margin-top: 7px;
  color: var(--showcase-text);
  font-size: 13px;
}

.showcase-signal-grid p {
  margin: 7px 0 0;
  color: var(--showcase-secondary);
  font-size: 11px;
  line-height: 1.5;
}

.showcase-copilot-block,
.showcase-risk-block,
.showcase-review-block {
  margin: 0 10px 10px;
  padding: 10px;
  border: 1px solid rgba(151, 180, 214, 0.14);
  border-radius: 8px;
  background: rgba(7, 16, 29, 0.64);
}

.showcase-copilot-pair {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
  margin: 0 10px 10px;
}

.showcase-copilot-pair .showcase-copilot-block {
  margin: 0;
}

.showcase-block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 7px;
}

.showcase-block-title h3,
.showcase-risk-block h3 {
  color: var(--showcase-text);
  font-size: 14px;
}

.showcase-similar-list,
.showcase-reference-list {
  display: grid;
  gap: 7px;
}

.showcase-similar-list article,
.showcase-reference-list article {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.showcase-similar-list strong,
.showcase-reference-list strong {
  color: #9fc4ff;
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 11px;
}

.showcase-similar-list p,
.showcase-reference-list p {
  margin: 3px 0 0;
  color: var(--showcase-text);
  font-size: 11px;
  font-weight: 800;
  line-height: 1.25;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.showcase-similar-list span,
.showcase-reference-list b {
  padding: 4px 7px;
  border-radius: 6px;
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  font-weight: 900;
}

.showcase-similar-list span[data-tone='success'] {
  color: var(--showcase-green);
  background: rgba(43, 216, 143, 0.11);
}

.showcase-similar-list span[data-tone='warning'] {
  color: var(--showcase-orange);
  background: rgba(255, 180, 92, 0.11);
}

.showcase-similar-list span[data-tone='info'] {
  color: var(--showcase-cyan);
  background: rgba(33, 199, 217, 0.1);
}

.showcase-reference-list small {
  color: var(--showcase-muted);
  font-size: 10px;
  line-height: 1.25;
}

.showcase-reference-list b {
  color: var(--showcase-green);
}

.showcase-draft-block ol {
  display: grid;
  gap: 4px;
  margin: 8px 0 0;
  padding-left: 18px;
  color: var(--showcase-secondary);
  font-size: 11px;
}

.showcase-draft-block p {
  font-size: 12px;
  line-height: 1.55;
}

.showcase-risk-block {
  border-color: rgba(255, 92, 122, 0.24);
  background: linear-gradient(180deg, rgba(255, 92, 122, 0.09), rgba(7, 16, 29, 0.7));
}

.showcase-risk-block h3 {
  margin: 0;
  color: #ff9bad;
}

.showcase-risk-block p {
  margin-top: 6px;
  font-size: 11px;
  line-height: 1.45;
}

.showcase-review-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.showcase-review-button {
  min-height: 34px;
}

.showcase-review-button--approve {
  color: var(--showcase-green);
  border-color: rgba(43, 216, 143, 0.34);
  background: rgba(43, 216, 143, 0.08);
}

.showcase-review-button--changes {
  color: var(--showcase-orange);
  border-color: rgba(255, 180, 92, 0.36);
  background: rgba(255, 180, 92, 0.08);
}

.showcase-review-button--reject {
  color: var(--showcase-red);
  border-color: rgba(255, 92, 122, 0.34);
  background: rgba(255, 92, 122, 0.08);
}

.showcase-knowledge-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 7px;
}

.showcase-link-button {
  min-height: 32px;
  color: #9fc4ff;
}

.showcase-feedback {
  margin: 9px 0 0;
  color: var(--showcase-green);
  font-size: 12px;
  font-weight: 800;
}

@media (max-width: 1380px) and (min-width: 1261px) {
  .showcase-workspace-grid {
    grid-template-columns: minmax(280px, 315px) minmax(0, 1fr) minmax(330px, 372px);
    gap: 10px;
  }

  .showcase-signal-grid {
    grid-template-columns: 1fr;
  }

  .showcase-detail-header h1 {
    font-size: 22px;
  }
}

@media (max-width: 1260px) {
  .showcase-ticket-workbench {
    min-height: 0;
  }

  .showcase-workspace-grid {
    grid-template-columns: 1fr;
  }

  .showcase-queue-panel,
  .showcase-detail-panel,
  .showcase-copilot-panel {
    min-height: auto;
  }
}

@media (max-width: 760px) {
  .showcase-workbench-header,
  .showcase-detail-header {
    grid-template-columns: 1fr;
  }

  .showcase-workbench-header {
    align-items: flex-start;
  }

  .showcase-header-metrics,
  .showcase-action-bar,
  .showcase-review-actions,
  .showcase-knowledge-actions,
  .showcase-metadata-grid,
  .showcase-signal-grid {
    grid-template-columns: 1fr;
  }

  .showcase-metadata-grid div {
    border-right: 0;
  }

  .showcase-filter-group {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
