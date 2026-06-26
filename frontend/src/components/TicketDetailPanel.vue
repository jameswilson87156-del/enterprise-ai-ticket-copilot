<script setup lang="ts">
import type { TicketDetail } from '../types/ticket'

defineProps<{
  ticket: TicketDetail | null
  loading: boolean
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
  <article class="detail-panel" data-screenshot="ticket-detail" :aria-busy="loading">
    <template v-if="ticket">
      <div class="detail-header">
        <div>
          <p class="eyebrow">当前工单</p>
          <h1>{{ ticket.title }}</h1>
          <div class="detail-subline">
            <span>{{ ticket.id }}</span>
            <span>Requester：{{ ticket.requester }}</span>
            <span>Assignee：支持队列</span>
            <span>Source：{{ ticket.systemContext.environment }}</span>
          </div>
        </div>
        <div class="detail-badges">
          <span class="priority-badge" :class="`priority-badge--${ticket.priority.toLowerCase()}`">{{ ticket.priority }}</span>
          <span class="status-pill status-pill--large">{{ statusLabel[ticket.status] }}</span>
        </div>
      </div>

      <section class="detail-section incident-brief">
        <div class="section-title">
          <span>01</span>
          <h2>问题描述</h2>
        </div>
        <p>{{ ticket.description }}</p>
        <div class="requester-line">
          <span>提交人：{{ ticket.requester }}</span>
          <span>部门：{{ ticket.department }}</span>
          <span>当前分类：{{ ticket.category }}</span>
          <span>标签：{{ ticket.category }} / {{ ticket.systemContext.application }} / {{ ticket.priority }}</span>
        </div>
      </section>

      <section class="context-grid">
        <div class="context-item">
          <span>应用</span>
          <strong>{{ ticket.systemContext.application }}</strong>
        </div>
        <div class="context-item">
          <span>环境</span>
          <strong>{{ ticket.systemContext.environment }}</strong>
        </div>
        <div class="context-item">
          <span>区域</span>
          <strong>{{ ticket.systemContext.region }}</strong>
        </div>
        <div class="context-item">
          <span>最近发布</span>
          <strong>{{ ticket.systemContext.lastDeployment }}</strong>
        </div>
        <div class="context-item context-item--wide">
          <span>影响范围</span>
          <strong>{{ ticket.systemContext.affectedUsers }}</strong>
        </div>
      </section>

      <section class="detail-section log-section">
        <div class="section-title">
          <span>02</span>
          <h2>原始日志</h2>
        </div>
        <pre><code v-for="line in ticket.errorLogs" :key="line">{{ line }}
</code></pre>
      </section>

      <section class="detail-section">
        <div class="section-title">
          <span>03</span>
          <h2>业务上下文</h2>
        </div>
        <ul class="signal-list">
          <li v-for="item in ticket.businessContext" :key="item">{{ item }}</li>
        </ul>
      </section>

      <section class="detail-section">
        <div class="section-title">
          <span>04</span>
          <h2>人工与系统处理记录</h2>
        </div>
        <div class="detail-timeline">
          <article v-for="event in ticket.timeline" :key="`${event.time}-${event.state}-${event.note}`">
            <time>{{ event.time }}</time>
            <div>
              <strong>{{ statusLabel[event.state] ?? event.state }}</strong>
              <span>{{ event.actor }} / {{ event.note }}</span>
            </div>
          </article>
        </div>
      </section>

      <section v-if="ticket.knowledgeDraft" class="knowledge-draft-box">
        <div class="section-title">
          <span>05</span>
          <h2>知识沉淀草稿</h2>
        </div>
        <span class="draft-confirmation">待人工确认</span>
        <strong>{{ ticket.knowledgeDraft.title }}</strong>
        <p>{{ ticket.knowledgeDraft.articleNo }} / {{ ticket.knowledgeDraft.status }} / {{ ticket.knowledgeDraft.owner }}</p>
      </section>
    </template>
    <div v-else class="empty-state empty-state--center">
      <strong>{{ loading ? '正在加载工单详情' : '等待工单选择' }}</strong>
      <span>{{ loading ? '正在同步详情与处理上下文。' : '提交或选择工单后，这里会展示数据库详情、日志和上下文。' }}</span>
    </div>
  </article>
</template>
