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
const searchKeyword = ref('')
const activeFilter = ref<TicketFilter>('ALL')

const pendingConfirmationCount = computed(() => tickets.value.filter((ticket) => ['PENDING_PROCESS', 'IN_PROGRESS'].includes(ticket.status)).length)
const knowledgeBasedCount = computed(() => tickets.value.filter((ticket) => ticket.status === 'KNOWLEDGE_BASED').length)
const aiAnalyzedCount = computed(() => tickets.value.filter((ticket) => ticket.aiConfidence > 0).length)

const metricItems = computed(() => [
  { label: '今日工单', value: String(tickets.value.length), delta: isDemoRuntime ? '本地演示数据' : 'MySQL 数据', tone: 'steel' as const },
  { label: '待处理', value: String(metrics.value?.pendingTickets ?? pendingConfirmationCount.value), delta: '人工队列', tone: 'warm' as const },
  { label: 'AI 已分析', value: String(aiAnalyzedCount.value), delta: `${metrics.value?.aiHitRate ?? 0}% 规则命中`, tone: 'mint' as const },
  { label: '待人工确认', value: String(pendingConfirmationCount.value), delta: '建议不自动执行', tone: 'danger' as const },
  { label: '已沉淀知识', value: String(knowledgeBasedCount.value), delta: `草稿 ${metrics.value?.todayKnowledgeDrafts ?? 0}`, tone: 'knowledge' as const }
])

const filteredTickets = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return tickets.value.filter((ticket) => {
    const matchesKeyword =
      !keyword ||
      [ticket.id, ticket.title, ticket.requester, ticket.team, ticket.category].some((value) => value.toLowerCase().includes(keyword))
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

const runtimeText = computed(() => {
  if (error.value) {
    return error.value
  }
  return isDemoRuntime ? '本地 Demo 模式 / 演示数据' : 'MySQL 数据 / 人工确认闭环'
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
  } catch (err) {
    error.value = err instanceof Error ? err.message : '操作失败'
  } finally {
    loading.value = false
  }
}

const submitTicket = async (payload: CreateTicketRequest) => {
  submitting.value = true
  try {
    await run(async () => {
      const detail = await createTicket(payload)
      await loadTickets()
      await selectTicket(detail.id)
    })
  } finally {
    submitting.value = false
  }
}

const changeStatus = async (status: TicketStatus) => {
  if (!selectedTicketId.value) {
    return
  }
  const note = status === 'RESOLVED' ? '人工确认处理完成，等待知识沉淀。' : '人工确认接手处理。'
  await run(async () => {
    await updateTicketStatus(selectedTicketId.value as string, status, note, status === 'RESOLVED' ? '已由支持人员确认解决。' : '')
    await refreshSelected()
  })
}

const generateDraft = async () => {
  if (!selectedTicketId.value) {
    return
  }
  await run(async () => {
    await createKnowledgeDraft(selectedTicketId.value as string)
    await refreshSelected()
  })
}

const confirmDraft = async () => {
  const articleNo = selectedTicket.value?.knowledgeDraft?.articleNo
  if (!articleNo) {
    return
  }
  await run(async () => {
    await confirmKnowledgeDraft(articleNo)
    await refreshSelected()
  })
}

onMounted(() => {
  void run(loadTickets)
})
</script>

<template>
  <div class="workbench-shell" data-screenshot="dashboard">
    <header class="page-hero">
      <div class="hero-copy">
        <span class="mode-chip" :class="{ 'mode-chip--error': error }">
          <span class="pulse-dot"></span>
          {{ runtimeText }}
        </span>
        <p class="eyebrow">企业 AI 工单 Copilot</p>
        <h1>企业 AI 工单作战台</h1>
        <p class="hero-lede">围绕工单分类、知识库匹配、处理建议和人工确认，构建可信的企业支持闭环。</p>
        <p class="hero-note">AI 辅助分析，人工最终确认；当前页面使用本地演示数据进行预览。</p>
      </div>
    </header>

    <SearchFilterBar v-model:keyword="searchKeyword" v-model:filter="activeFilter" />

    <MetricStrip :metrics="metricItems" />

    <section class="workspace-grid">
      <div class="ticket-column">
        <TicketQueue :tickets="filteredTickets" :selected-id="selectedTicketId" @select="(id) => run(() => selectTicket(id))" />
        <TicketIntakePanel :submitting="submitting" @submit="submitTicket" />
      </div>
      <div class="resolution-column">
        <TicketDetailPanel :ticket="selectedTicket" />
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

    <section class="operations-grid" data-screenshot="knowledge-base" aria-label="知识库与人工确认记录">
      <article class="ops-panel">
        <div class="ops-panel__heading">
          <p class="eyebrow">AI 建议记录</p>
          <h2>最近 AI 建议记录</h2>
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
          <p class="eyebrow">知识库命中</p>
          <h2>知识库命中与沉淀</h2>
        </div>
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
          <p class="eyebrow">人工确认</p>
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
          <span>AI 建议不会自动改变工单状态。</span>
        </div>
      </article>
    </section>

    <StatusTimeline :events="selectedTicket?.timeline ?? []" />
  </div>
</template>
