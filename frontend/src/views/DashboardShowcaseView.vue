<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { isDemoRuntime } from '../api/tickets'
import { useTicketRealFlow } from '../composables/useTicketRealFlow'
import { statusLabel, statusTone } from '../utils/realFlowFormat'
import EmptyState from '../components/ui/EmptyState.vue'
import ErrorState from '../components/ui/ErrorState.vue'
import LoadingState from '../components/ui/LoadingState.vue'
import MetricCard from '../components/ui/MetricCard.vue'
import PageHeader from '../components/ui/PageHeader.vue'
import PanelHeader from '../components/ui/PanelHeader.vue'
import StatusBadge from '../components/ui/StatusBadge.vue'

const flow = useTicketRealFlow()

onMounted(() => {
  if (flow.backendState.value === 'UNKNOWN') {
    void flow.refresh()
  }
})

const statusCounts = computed(() => {
  return flow.tickets.value.reduce<Record<string, number>>((acc, ticket) => {
    acc[ticket.status] = (acc[ticket.status] ?? 0) + 1
    return acc
  }, {})
})

const recentTickets = computed(() => flow.tickets.value.slice(0, 5))
const activityItems = computed(() => {
  const timeline = flow.selectedTicket.value?.timeline ?? []
  return timeline.slice(-4).reverse()
})
const connectionLabel = computed(() => {
  if (isDemoRuntime) {
    return '本地演示'
  }
  if (flow.backendState.value === 'CONNECTED') {
    return '后端 API'
  }
  if (flow.backendState.value === 'UNAVAILABLE') {
    return '后端不可用'
  }
  return '连接中'
})
const evidenceLabel = computed(() => {
  if (!flow.selectedTicket.value) {
    return '等待工单'
  }
  if (!flow.trace.value) {
    return '等待 Trace'
  }
  return `${flow.currentRagReferences.value.length} 条引用`
})
const metricCards = computed(() => [
  {
    label: '当前工单',
    value: flow.tickets.value.length,
    note: isDemoRuntime ? '浏览器内存中的合成列表' : '来自 /api/tickets',
    tone: 'blue' as const,
    link: '#ticket-detail'
  },
  {
    label: '待人工复核',
    value: flow.pendingReviewCount.value,
    note: '待复核状态：REVIEW_REQUIRED / AI_DRAFTED',
    tone: 'amber' as const,
    link: '#human-review'
  },
  {
    label: '待处理工单',
    value: flow.metrics.value?.pendingTickets ?? '—',
    note: isDemoRuntime ? '本地内存计算' : '来自 /api/tickets/metrics',
    tone: 'violet' as const,
    link: '#evaluation-metrics'
  },
  {
    label: 'AI 命中率',
    value: flow.metrics.value?.aiHitRate != null ? `${flow.metrics.value.aiHitRate}%` : '—',
    note: isDemoRuntime ? '本地 fixture 置信度覆盖' : '后端最小指标，非生产基准',
    tone: 'cyan' as const,
    link: '#evaluation-metrics'
  }
])
</script>

<template>
  <section class="page-stack dashboard-layout" data-screenshot="dashboard" aria-label="支持运营总览 Dashboard">
    <PageHeader
      eyebrow="支持运营 / 总览"
      title="支持运营总览"
      :description="isDemoRuntime ? '用合成工单观察队列、证据和复核门禁；这些数据只存在于浏览器内存。' : '只展示当前后端 API 返回的工单、指标和证据状态，不补造 Provider 或生产统计。'"
    >
      <template #meta>
        <StatusBadge :label="isDemoRuntime ? 'Demo · 本地演示' : connectionLabel" :tone="isDemoRuntime ? 'violet' : flow.backendState.value === 'CONNECTED' ? 'success' : 'danger'" />
      </template>
      <template #actions>
        <button type="button" class="button button--secondary" :disabled="flow.loadingTickets.value" @click="flow.refresh">
          {{ flow.loadingTickets.value ? '刷新中…' : '刷新数据' }}
        </button>
        <a class="button button--primary" href="#ticket-detail">打开工作台 <span aria-hidden="true">→</span></a>
      </template>
    </PageHeader>

    <ErrorState v-if="flow.lastError.value" :message="flow.lastError.value" />

    <section class="metric-grid" aria-label="当前运营指标">
      <MetricCard v-for="card in metricCards" :key="card.label" v-bind="card" />
    </section>

    <section class="dashboard-overview">
      <article class="surface dashboard-panel">
        <PanelHeader eyebrow="队列 / 优先处理" title="优先处理队列" :count="flow.tickets.value.length">
          <a class="link-button" href="#ticket-detail">进入工作台 →</a>
        </PanelHeader>
        <LoadingState v-if="flow.loadingTickets.value" label="读取当前工单队列" />
        <div v-else-if="recentTickets.length" class="dashboard-list">
          <button v-for="ticket in recentTickets" :key="ticket.id" type="button" class="dashboard-ticket" @click="flow.selectTicket(ticket.id)">
            <span class="priority-rail" :class="`priority-rail--${ticket.priority.toLowerCase()}`" aria-hidden="true"></span>
            <span class="dashboard-ticket__body">
              <span class="dashboard-ticket__topline">
                <span class="mono">{{ ticket.id }}</span>
                <StatusBadge :label="statusLabel(ticket.status)" :tone="statusTone(ticket.status)" :compact="true" />
              </span>
              <strong>{{ ticket.title }}</strong>
              <span class="dashboard-ticket__meta"><span>{{ ticket.requester }}</span><span class="dot-separator" aria-hidden="true"></span><span>{{ ticket.category }}</span></span>
            </span>
            <span class="dashboard-ticket__time">{{ ticket.updatedAt }}</span>
          </button>
        </div>
        <EmptyState v-else title="当前没有工单" :description="isDemoRuntime ? 'Demo fixture 当前为空，可以从 Workbench 创建一条合成工单。' : '后端列表为空；页面不会用静态 Mock 替代 API 返回。'" />
      </article>

      <article class="surface dashboard-panel">
        <PanelHeader eyebrow="状态检查" title="处理状态" />
        <div class="health-grid">
          <div class="health-row">
            <div><strong>数据来源</strong><small>{{ isDemoRuntime ? '浏览器内存合成数据' : '后端 API 当前列表' }}</small></div>
            <span :class="['health-value', isDemoRuntime ? 'health-value--violet' : '']">{{ connectionLabel }}</span>
          </div>
          <div class="health-row">
            <div><strong>处理证据</strong><small>当前选择：{{ flow.selectedTicket.value?.id ?? '未选择' }}</small></div>
            <span class="health-value">{{ evidenceLabel }}</span>
          </div>
          <div class="health-row">
            <div><strong>复核门槛</strong><small>需要人工确认的建议不会自动执行</small></div>
            <span class="health-value health-value--amber">{{ flow.pendingReviewCount.value }} 待处理</span>
          </div>
          <div class="health-row">
            <div><strong>评测范围</strong><small>合成样本 · 关键词检索</small></div>
            <span class="health-value health-value--violet">本地</span>
          </div>
        </div>
      </article>
    </section>

    <section class="dashboard-lower">
      <article class="surface dashboard-panel">
        <PanelHeader eyebrow="队列分布" title="状态分布" />
        <div v-if="Object.keys(statusCounts).length" class="status-list">
          <div v-for="(count, status) in statusCounts" :key="status" class="status-row">
            <span class="status-row__label"><StatusBadge :label="statusLabel(status)" :tone="statusTone(status)" :compact="true" /></span>
            <strong class="status-row__value">{{ count }}</strong>
          </div>
        </div>
        <EmptyState v-else title="暂无状态分布" description="等待工单列表返回后，这里会按后端状态分组。" />
      </article>

      <article class="surface dashboard-panel">
        <PanelHeader eyebrow="当前工单" title="最近活动" :description="flow.selectedTicket.value ? `${flow.selectedTicket.value.id} · ${flow.selectedTicket.value.title}` : '选择一条工单查看其状态时间线'" />
        <div v-if="activityItems.length" class="activity-list">
          <div v-for="item in activityItems" :key="`${item.time}-${item.state}`" class="activity-row">
            <span class="activity-row__signal" aria-hidden="true"></span>
            <div class="activity-row__copy">
              <strong>{{ statusLabel(item.state) }}</strong>
              <span class="activity-row__meta">{{ item.actor }} · {{ item.note }}</span>
            </div>
            <time class="activity-row__time">{{ item.time }}</time>
          </div>
        </div>
        <EmptyState v-else title="还没有活动" description="从优先处理队列选择工单，读取真实或 Demo 的状态时间线。" />
      </article>
    </section>
  </section>
</template>
