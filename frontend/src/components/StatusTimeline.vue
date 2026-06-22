<script setup lang="ts">
import type { TimelineEvent } from '../types/ticket'

defineProps<{
  events: TimelineEvent[]
}>()

const statusLabel: Record<string, string> = {
  PENDING_CLASSIFICATION: '待分类',
  PENDING_PROCESS: '待人工确认',
  IN_PROGRESS: '处理中',
  RESOLVED: '已解决',
  KNOWLEDGE_BASED: '已沉淀'
}
</script>

<template>
  <section class="timeline-panel">
    <div class="timeline-heading">
      <div>
        <p class="eyebrow">状态历史</p>
        <h2>状态流转</h2>
      </div>
      <span>规则草稿不会直接改变工单状态</span>
    </div>
    <div v-if="events.length" class="timeline-track">
      <article v-for="event in events" :key="`${event.time}-${event.state}`" class="timeline-event">
        <time>{{ event.time }}</time>
        <strong>{{ statusLabel[event.state] ?? event.state }}</strong>
        <span>{{ event.actor }}</span>
        <p>{{ event.note }}</p>
      </article>
    </div>
    <div v-else class="empty-state">
      <strong>暂无流转记录</strong>
      <span>工单提交后会写入 ticket_status_history。</span>
    </div>
  </section>
</template>
