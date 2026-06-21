<script setup lang="ts">
import { computed } from 'vue'
import type { AiAnalysis, TicketDetail, TicketStatus } from '../types/ticket'

const props = defineProps<{
  analysis: AiAnalysis | null
  ticket: TicketDetail | null
  busy: boolean
}>()

const confirmationLabel = computed(() => {
  switch (props.ticket?.status) {
    case 'IN_PROGRESS':
      return '已人工接手'
    case 'RESOLVED':
      return '已人工确认'
    case 'KNOWLEDGE_BASED':
      return '已确认并沉淀'
    default:
      return props.analysis?.confirmationState ?? '待人工确认'
  }
})

const emit = defineEmits<{
  status: [status: TicketStatus]
  draft: []
  confirmDraft: []
}>()
</script>

<template>
  <aside class="ai-panel" data-screenshot="ai-analysis" :aria-busy="busy">
    <div class="ai-panel__top">
      <div>
        <p class="eyebrow">AI 建议</p>
        <h2>AI 辅助分析</h2>
      </div>
      <span class="human-lock" role="status" aria-live="polite">{{ confirmationLabel }}</span>
    </div>

    <template v-if="analysis && ticket">
      <section class="ai-classification">
        <span>问题分类</span>
        <strong>{{ analysis.classification }}</strong>
        <p class="reason-line">{{ analysis.classificationReason }}</p>
        <div class="confidence-bar" aria-label="规则置信度">
          <span :style="{ width: `${analysis.confidence}%` }"></span>
        </div>
        <small>置信度 {{ analysis.confidence }}%，本阶段不调用真实 LLM，仅供人工确认前参考</small>
      </section>

      <section class="ai-block">
        <div class="mini-title">知识库建议</div>
        <article v-for="hit in analysis.knowledgeHits" :key="hit.id" class="knowledge-hit">
          <div>
            <span>{{ hit.id }}</span>
            <strong>{{ hit.title }}</strong>
          </div>
          <b>{{ hit.relevance }}%</b>
          <small>{{ hit.owner }} / 已校验 {{ hit.lastVerifiedAt }}</small>
        </article>
        <p v-if="!analysis.knowledgeHits.length" class="muted-line">暂无命中，建议处理完成后沉淀知识。</p>
      </section>

      <section class="ai-block">
        <div class="mini-title">下一步处理建议</div>
        <ol class="step-list">
          <li v-for="step in analysis.troubleshootingSteps" :key="step">{{ step }}</li>
        </ol>
      </section>

      <section class="ai-block">
        <div class="mini-title">回复建议</div>
        <p class="reply-draft">{{ analysis.replySuggestion }}</p>
      </section>

      <section class="risk-box">
        <div class="mini-title">风险提示与人工边界</div>
        <ul>
          <li v-for="note in analysis.riskNotes" :key="note">{{ note }}</li>
        </ul>
      </section>

      <div class="workflow-actions">
        <button
          type="button"
          class="secondary-action"
          :disabled="busy || ['IN_PROGRESS', 'RESOLVED', 'KNOWLEDGE_BASED'].includes(ticket.status)"
          @click="emit('status', 'IN_PROGRESS')"
        >
          确认接手处理
        </button>
        <button
          type="button"
          class="primary-action"
          :disabled="busy || ['RESOLVED', 'KNOWLEDGE_BASED'].includes(ticket.status)"
          @click="emit('status', 'RESOLVED')"
        >
          人工确认已解决
        </button>
        <button type="button" class="secondary-action" :disabled="busy || ticket.status !== 'RESOLVED'" @click="emit('draft')">生成知识草稿</button>
        <button
          type="button"
          class="primary-action"
          :disabled="busy || ticket.knowledgeDraft?.status !== 'DRAFT'"
          @click="emit('confirmDraft')"
        >
          人工确认入库
        </button>
      </div>
    </template>

    <div v-else class="empty-state empty-state--center">
      <strong>{{ busy ? '正在加载规则分析' : '等待规则分析' }}</strong>
      <span>{{ busy ? '正在同步分类、知识命中与处理建议。' : '提交或选择工单后会展示分类、知识命中、排查步骤和回复建议。' }}</span>
    </div>
  </aside>
</template>
