<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import {
  confirmKnowledgeDraft,
  createKnowledgeDraft,
  createTicket,
  fetchAiAnalysis,
  fetchMetrics,
  fetchTicket,
  fetchTickets,
  isDemoRuntime,
  updateTicketStatus
} from './api/tickets'
import AiRecommendationPanel from './components/AiRecommendationPanel.vue'
import MetricStrip from './components/MetricStrip.vue'
import SearchFilterBar from './components/SearchFilterBar.vue'
import StatusTimeline from './components/StatusTimeline.vue'
import TicketDetailPanel from './components/TicketDetailPanel.vue'
import TicketIntakePanel from './components/TicketIntakePanel.vue'
import TicketQueue from './components/TicketQueue.vue'
import type { AiAnalysis, CreateTicketRequest, TicketDetail, TicketStatus, TicketSummary, WorkbenchMetrics } from './types/ticket'

type TicketFilter = 'ALL' | 'PENDING' | 'REVIEW' | 'KNOWLEDGE'

const tickets = shallowRef<TicketSummary[]>([])
const selectedTicketId = shallowRef<string | null>(null)
const selectedTicket = shallowRef<TicketDetail | null>(null)
const selectedAnalysis = shallowRef<AiAnalysis | null>(null)
const metrics = shallowRef<WorkbenchMetrics | null>(null)
const loading = shallowRef(false)
const submitting = shallowRef(false)
const error = shallowRef('')
const feedback = shallowRef('')
const searchKeyword = ref('')
const activeFilter = ref<TicketFilter>('ALL')
const formResetVersion = ref(0)

const pendingConfirmationCount = computed(() => tickets.value.filter((ticket) => ['PENDING_PROCESS', 'IN_PROGRESS'].includes(ticket.status)).length)
const aiAnalyzedCount = computed(() => tickets.value.filter((ticket) => ticket.aiConfidence > 0).length)
const highRiskCount = computed(() => tickets.value.filter((ticket) => ticket.priority === 'P1').length)
const reviewQueueCount = computed(() => tickets.value.filter((ticket) => ['PENDING_PROCESS', 'IN_PROGRESS'].includes(ticket.status)).length)

const navItems = [
  '仪表盘',
  '工单处理',
  '知识库 / RAG',
  'Trace 追踪',
  'Human Review',
  'Provider',
  '设置'
]

const boundaryTags = ['No real LLM', 'No vector DB']

const metricItems = computed(() => [
  { label: '工单总览', value: String(tickets.value.length), delta: isDemoRuntime ? 'Portfolio Demo 数据' : 'MySQL 数据源', tone: 'steel' as const },
  { label: '待人工确认', value: String(reviewQueueCount.value), delta: '执行前复核', tone: 'warm' as const },
  { label: '知识关联率', value: `${metrics.value?.knowledgeCoverage ?? 0}%`, delta: '关键词命中 / 已沉淀', tone: 'knowledge' as const },
  { label: '规则已分析', value: String(aiAnalyzedCount.value), delta: `${metrics.value?.aiHitRate ?? 0}% 可解释命中`, tone: 'mint' as const },
  { label: '高风险提示', value: String(highRiskCount.value), delta: 'P1 需人工优先看', tone: 'danger' as const }
])

const filteredTickets = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return tickets.value.filter((ticket) => {
    const matchesKeyword =
      !keyword ||
      [ticket.id, ticket.title, ticket.requester, ticket.team, ticket.category, ticket.searchText ?? ''].some((value) =>
        value.toLowerCase().includes(keyword)
      )
    const matchesFilter =
      activeFilter.value === 'ALL' ||
      (activeFilter.value === 'PENDING' && ['PENDING_CLASSIFICATION', 'PENDING_PROCESS'].includes(ticket.status)) ||
      (activeFilter.value === 'REVIEW' && ['PENDING_PROCESS', 'IN_PROGRESS'].includes(ticket.status)) ||
      (activeFilter.value === 'KNOWLEDGE' && ticket.status === 'KNOWLEDGE_BASED')
    return matchesKeyword && matchesFilter
  })
})

const recentAiRecords = computed(() =>
  tickets.value.slice(0, 4).map((ticket) => ({
    id: ticket.id,
    title: ticket.title,
    category: ticket.category,
    confidence: ticket.aiConfidence,
    status: ticket.status
  }))
)

const knowledgeRows = computed(() => {
  const hits = selectedAnalysis.value?.knowledgeHits ?? []
  const draft = selectedTicket.value?.knowledgeDraft
  return [
    ...hits.map((hit) => ({
      id: hit.id,
      title: hit.title,
      meta: `${hit.owner} / 命中 ${hit.relevance}% / 校验 ${hit.lastVerifiedAt}`,
      state: '可参考'
    })),
    ...(draft
      ? [
          {
            id: draft.articleNo,
            title: draft.title,
            meta: `${draft.owner} / 来源 ${selectedTicket.value?.id ?? '-'} / ${draft.category}`,
            state: draft.status === 'PUBLISHED' ? '已发布' : '待审核'
          }
        ]
      : [])
  ]
})

const humanRecords = computed(() =>
  (selectedTicket.value?.timeline ?? [])
    .filter((event) => !['System', 'Rule Engine', 'Rule Template', '系统', '规则引擎', '规则模板'].includes(event.actor))
    .slice(-4)
    .reverse()
)

const selectedTraceSummary = computed(() => {
  const events = selectedTicket.value?.timeline ?? []
  return {
    steps: events.length,
    latest: events.at(-1)?.note ?? '等待工单选择后展示状态历史。',
    source: selectedTicket.value ? 'ticket_status_history / generation_record' : '未选择工单'
  }
})

const runtimeText = computed(() => {
  if (error.value) {
    return '操作失败 / 请检查运行环境'
  }
  if (loading.value && !tickets.value.length) {
    return '正在加载工单数据'
  }
  return isDemoRuntime ? '本地 Demo / 演示数据' : 'MySQL 数据 / 人工确认闭环'
})

const loadTickets = async () => {
  tickets.value = await fetchTickets()
  metrics.value = await fetchMetrics()
  if (!selectedTicketId.value && tickets.value.length) {
    await selectTicket(tickets.value[0].id)
  }
  if (!tickets.value.length) {
    selectedTicketId.value = null
    selectedTicket.value = null
    selectedAnalysis.value = null
  }
}

const selectTicket = async (id: string) => {
  selectedTicketId.value = id
  selectedTicket.value = null
  selectedAnalysis.value = null
  const [detail, analysis] = await Promise.all([fetchTicket(id), fetchAiAnalysis(id)])
  selectedTicket.value = detail
  selectedAnalysis.value = analysis
}

const refreshSelected = async () => {
  if (selectedTicketId.value) {
    await selectTicket(selectedTicketId.value)
  }
  await loadTickets()
}

const run = async (task: () => Promise<void>) => {
  loading.value = true
  error.value = ''
  try {
    await task()
    return true
  } catch (err) {
    error.value = err instanceof Error ? err.message : '操作失败'
    return false
  } finally {
    loading.value = false
  }
}

const submitTicket = async (payload: CreateTicketRequest) => {
  submitting.value = true
  feedback.value = ''
  try {
    const succeeded = await run(async () => {
      const detail = await createTicket(payload)
      await loadTickets()
      await selectTicket(detail.id)
    })
    if (succeeded) {
      formResetVersion.value += 1
      feedback.value = isDemoRuntime ? '演示工单已创建，数据仅保存在当前浏览器内存中。' : '工单已创建并进入人工处理队列。'
    }
  } finally {
    submitting.value = false
  }
}

const changeStatus = async (status: TicketStatus) => {
  if (!selectedTicketId.value) {
    return
  }
  const note = status === 'RESOLVED' ? '人工确认处理完成，等待知识沉淀。' : '人工确认接手处理。'
  feedback.value = ''
  const succeeded = await run(async () => {
    await updateTicketStatus(selectedTicketId.value as string, status, note, status === 'RESOLVED' ? '已由支持人员确认解决。' : '')
    await refreshSelected()
  })
  if (succeeded) {
    feedback.value = status === 'RESOLVED' ? '已人工确认解决，可继续生成知识草稿。' : '支持人员已确认接手处理。'
  }
}

const generateDraft = async () => {
  if (!selectedTicketId.value) {
    return
  }
  feedback.value = ''
  const succeeded = await run(async () => {
    await createKnowledgeDraft(selectedTicketId.value as string)
    await refreshSelected()
  })
  if (succeeded) {
    feedback.value = '知识草稿已生成，发布前仍需人工审核。'
  }
}

const confirmDraft = async () => {
  const articleNo = selectedTicket.value?.knowledgeDraft?.articleNo
  if (!articleNo) {
    return
  }
  feedback.value = ''
  const succeeded = await run(async () => {
    await confirmKnowledgeDraft(articleNo)
    await refreshSelected()
  })
  if (succeeded) {
    feedback.value = '知识草稿已由人工确认并完成沉淀。'
  }
}

onMounted(() => {
  void run(loadTickets)
})
</script>

<template>
  <div class="enterprise-shell" data-screenshot="dashboard" :aria-busy="loading">
    <a class="skip-link" href="#main">跳到主要内容</a>

    <aside class="app-sidebar" aria-label="主导航">
      <div class="product-mark" aria-label="Enterprise Ticket RAG Copilot">
        <span class="product-mark__glyph">RAG</span>
        <div>
          <strong>Enterprise Ticket</strong>
          <span>RAG Copilot</span>
        </div>
      </div>

      <nav class="side-nav" aria-label="工作台导航">
        <button
          v-for="item in navItems"
          :key="item"
          type="button"
          class="side-nav__item"
          :class="{ 'side-nav__item--active': item === '工单处理' }"
          :aria-current="item === '工单处理' ? 'page' : undefined"
        >
          <span class="side-nav__dot" aria-hidden="true"></span>
          {{ item }}
        </button>
      </nav>

      <div class="sidebar-status">
        <span class="pulse-dot"></span>
        <div>
          <strong>{{ isDemoRuntime ? 'Demo mode' : 'API mode' }}</strong>
          <small>Provider：local-rule fallback</small>
        </div>
      </div>
    </aside>

    <div class="app-main">
      <header class="topbar">
        <SearchFilterBar v-model:keyword="searchKeyword" v-model:filter="activeFilter" />

        <div class="topbar-status-group" aria-label="运行边界">
          <span class="topbar-pill">环境：Portfolio Demo</span>
          <span class="topbar-pill">Provider：local-rule fallback</span>
          <span class="topbar-pill">角色：Demo Reviewer</span>
          <span v-for="tag in boundaryTags" :key="tag" class="topbar-pill topbar-pill--guard">{{ tag }}</span>
        </div>
      </header>

      <main id="main" class="workbench-content">
        <section class="command-band" aria-label="工作台概览">
          <div>
            <p class="eyebrow">Ticket Operations / Human-in-the-loop</p>
            <h1>工单处理工作台</h1>
            <p>
              基于本地规则、关键词知识匹配和模板化建议草稿组织处理上下文；所有状态变更、知识发布和生产动作仍需人工确认。
            </p>
          </div>

          <div class="command-band__status">
            <span class="mode-chip" :class="{ 'mode-chip--error': error }" role="status" aria-live="polite">
              <span class="pulse-dot"></span>
              {{ runtimeText }}
            </span>
            <span class="mode-chip mode-chip--muted">Trace 追踪入口：状态历史 + 生成记录</span>
          </div>

          <MetricStrip :metrics="metricItems" />
        </section>

        <div v-if="error || feedback" class="operation-feedback" aria-live="polite" aria-atomic="true">
          <p v-if="error" class="operation-feedback__message operation-feedback__message--error" role="alert" aria-live="assertive">
            {{ error }}
          </p>
          <p v-else class="operation-feedback__message" role="status" aria-live="polite">{{ feedback }}</p>
        </div>

        <section class="workspace-grid" aria-label="工单处理三栏工作台">
          <div class="ticket-column">
            <TicketQueue
              :tickets="filteredTickets"
              :selected-id="selectedTicketId"
              :loading="loading && !tickets.length"
              @select="(id) => run(() => selectTicket(id))"
            />
          </div>
          <div class="detail-column">
            <TicketDetailPanel :ticket="selectedTicket" :loading="loading" />
          </div>
          <div class="assist-column">
            <AiRecommendationPanel
              :analysis="selectedAnalysis"
              :ticket="selectedTicket"
              :busy="loading"
              @status="changeStatus"
              @draft="generateDraft"
              @confirm-draft="confirmDraft"
            />
          </div>
        </section>

        <section class="secondary-grid">
          <div class="intake-column">
            <TicketIntakePanel :submitting="submitting" :reset-version="formResetVersion" @submit="submitTicket" />
          </div>
          <div class="operations-column">
            <section class="operations-grid" data-screenshot="knowledge-base" aria-label="知识库与人工确认记录">
              <article class="ops-panel">
                <div class="ops-panel__heading">
                  <p class="eyebrow">Draft Reply</p>
                  <h2>最近模板化建议草稿</h2>
                </div>
                <div class="ops-list">
                  <div v-for="record in recentAiRecords" :key="record.id" class="ops-row">
                    <div>
                      <span>{{ record.id }} / {{ record.category }}</span>
                      <strong>{{ record.title }}</strong>
                    </div>
                    <b>{{ record.confidence }}%</b>
                  </div>
                </div>
              </article>

              <article class="ops-panel ops-panel--wide">
                <div class="ops-panel__heading">
                  <p class="eyebrow">Knowledge Base / RAG 引用视图</p>
                  <h2>关键词知识匹配与沉淀</h2>
                </div>
                <p class="ops-panel__note">当前基于关键词匹配与知识引用，向量检索为后续扩展。</p>
                <div v-if="knowledgeRows.length" class="knowledge-table">
                  <div v-for="row in knowledgeRows" :key="row.id" class="knowledge-table__row">
                    <span>{{ row.id }}</span>
                    <strong>{{ row.title }}</strong>
                    <small>{{ row.meta }}</small>
                    <em>{{ row.state }}</em>
                  </div>
                </div>
                <div v-else class="empty-state">
                  <strong>暂无知识命中</strong>
                  <span>处理完成后可以生成知识草稿，人工确认后再发布。</span>
                </div>
              </article>

              <article class="ops-panel">
                <div class="ops-panel__heading">
                  <p class="eyebrow">Trace 追踪入口</p>
                  <h2>状态历史与生成记录</h2>
                </div>
                <div class="trace-card">
                  <span>{{ selectedTraceSummary.source }}</span>
                  <strong>{{ selectedTraceSummary.steps }} steps</strong>
                  <p>{{ selectedTraceSummary.latest }}</p>
                  <small>后续可扩展 Workflow Step Trace，不代表当前已有完整 Trace / Span 数据模型。</small>
                </div>
              </article>

              <article class="ops-panel">
                <div class="ops-panel__heading">
                  <p class="eyebrow">Human Review</p>
                  <h2>人工确认记录</h2>
                </div>
                <div v-if="humanRecords.length" class="approval-list">
                  <div v-for="record in humanRecords" :key="`${record.time}-${record.actor}-${record.note}`" class="approval-row">
                    <time>{{ record.time }}</time>
                    <div>
                      <strong>{{ record.actor }}</strong>
                      <span>{{ record.note }}</span>
                    </div>
                  </div>
                </div>
                <div v-else class="empty-state">
                  <strong>等待人工动作</strong>
                  <span>建议草稿不会自动改变工单状态。</span>
                </div>
              </article>
            </section>
          </div>
        </section>

        <StatusTimeline :events="selectedTicket?.timeline ?? []" />
      </main>
    </div>
  </div>
</template>
