<script setup lang="ts">
import type { TicketSummary } from '../types/ticket'

defineProps<{
  tickets: TicketSummary[]
  selectedId: string | null
  loading: boolean
}>()

const emit = defineEmits<{
  select: [id: string]
}>()

const statusLabel: Record<string, string> = {
  PENDING_CLASSIFICATION: '待分类',
  PENDING_PROCESS: '待处理',
  IN_PROGRESS: '处理中',
  RESOLVED: '已解决',
  KNOWLEDGE_BASED: '已沉淀'
}
</script>

<template>
  <aside id="ticket-queue" class="queue-panel" aria-label="工单队列" :aria-busy="loading">
    <div class="panel-heading">
      <div>
        <p class="eyebrow">工单队列</p>
        <h2>工单队列</h2>
      </div>
      <span class="queue-count">{{ tickets.length }}</span>
    </div>

    <div v-if="tickets.length" class="queue-list">
      <button
        v-for="ticket in tickets"
        :key="ticket.id"
        class="ticket-row"
        :class="{ 'ticket-row--active': ticket.id === selectedId }"
        type="button"
        :aria-current="ticket.id === selectedId ? 'true' : undefined"
        :aria-label="`${ticket.id}，${ticket.title}，${statusLabel[ticket.status]}，规则置信度 ${ticket.aiConfidence}%`"
        @click="emit('select', ticket.id)"
      >
        <span class="ticket-row__rail" :class="`priority-${ticket.priority.toLowerCase()}`"></span>
        <span class="ticket-row__main">
          <span class="ticket-row__top">
            <span class="ticket-id">{{ ticket.id }}</span>
            <span class="ticket-time">{{ ticket.updatedAt }}</span>
          </span>
          <strong>{{ ticket.title }}</strong>
          <span class="ticket-meta">
            <span>{{ ticket.requester }} / {{ ticket.team }}</span>
            <span class="dot"></span>
            <span>{{ ticket.category }}</span>
          </span>
          <span class="ticket-row__bottom">
            <span class="status-pill">{{ statusLabel[ticket.status] }}</span>
            <span class="confidence-mini">
              <span :style="{ width: `${ticket.aiConfidence}%` }"></span>
            </span>
            <span class="confidence-number">{{ ticket.aiConfidence }}%</span>
          </span>
        </span>
      </button>
    </div>
    <div v-else class="empty-state" role="status" aria-live="polite">
      <strong>{{ loading ? '正在加载工单' : '暂无匹配工单' }}</strong>
      <span>{{ loading ? '正在读取工单队列，请稍候。' : '调整搜索词或筛选条件后继续查看。' }}</span>
    </div>
  </aside>
</template>
