<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { isDemoRuntime } from '../api/tickets'
import { useTicketRealFlow } from '../composables/useTicketRealFlow'
import { boolLabel, priorityLabel, priorityTone, riskBadgeTone, safeText, statusLabel, statusTone } from '../utils/realFlowFormat'
import EmptyState from '../components/ui/EmptyState.vue'
import ErrorState from '../components/ui/ErrorState.vue'
import LoadingState from '../components/ui/LoadingState.vue'
import PageHeader from '../components/ui/PageHeader.vue'
import PanelHeader from '../components/ui/PanelHeader.vue'
import StatusBadge from '../components/ui/StatusBadge.vue'

const flow = useTicketRealFlow()
const reviewComment = ref('Synthetic review note: evidence and citation layer inspected in local-rule E2E.')
const reviewMessage = ref<string | null>(null)

async function hydrateReviewQueue() {
  if (flow.backendState.value === 'UNKNOWN') {
    await flow.refresh()
  }
  const reviewable = flow.tickets.value.find((ticket) => ['REVIEW_REQUIRED', 'AI_DRAFTED'].includes(ticket.status))
  if (reviewable && !['REVIEW_REQUIRED', 'AI_DRAFTED'].includes(flow.selectedTicket.value?.status ?? '')) {
    await flow.selectTicket(reviewable.id)
  }
}

onMounted(() => {
  void hydrateReviewQueue()
})

const reviewableTickets = computed(() => flow.tickets.value.filter((ticket) => ['REVIEW_REQUIRED', 'AI_DRAFTED'].includes(ticket.status)))
const selected = computed(() => flow.selectedTicket.value)
const analysis = computed(() => flow.analysis.value)
const structured = computed(() => flow.currentStructuredOutput.value)
const citations = computed(() => flow.currentValidatedCitations.value)
const ragReferences = computed(() => flow.currentRagReferences.value)
const isReviewable = computed(() => ['REVIEW_REQUIRED', 'AI_DRAFTED'].includes(selected.value?.status ?? ''))
const canReview = computed(() => Boolean(isReviewable.value && !flow.reviewSubmitting.value))
const riskLevel = computed(() => structured.value?.riskLevel ?? analysis.value?.riskLevel ?? (selected.value?.priority === 'P1' ? 'HIGH' : selected.value?.priority === 'P2' ? 'MEDIUM' : 'LOW'))
const answer = computed(() => structured.value?.answer ?? analysis.value?.replySuggestion ?? '当前没有可展示的建议回复。')
const reviewMessageIsError = computed(() => Boolean(reviewMessage.value && !reviewMessage.value.startsWith('人工复核结果')))

async function submitReview(decision: 'approve' | 'request-changes' | 'reject') {
  reviewMessage.value = null
  const comment = reviewComment.value.trim()
  if (!comment && decision !== 'approve') {
    reviewMessage.value = '要求修改 / 驳回必须填写简短原因。'
    return
  }
  const label = decision === 'approve' ? '批准（Approve）' : decision === 'reject' ? '驳回（Reject）' : '要求修改（Request changes）'
  const persistenceNote = isDemoRuntime ? '写入本地 Demo 复核历史（review history）' : '通过后端 API 写入追加式复核历史（append-only review history）'
  if (!window.confirm(`确认提交 ${label} 决策？该操作将${persistenceNote}。`)) {
    return
  }
  try {
    await flow.submitReview(decision, comment || 'Synthetic approval note: local evidence reviewed.')
    reviewMessage.value = isDemoRuntime ? '人工复核结果已写入本地 Demo，并重新读取 Trace。' : '人工复核结果已由后端确认并重新读取 Trace。'
  } catch {
    reviewMessage.value = flow.lastError.value
  }
}
</script>

<template>
  <section class="page-stack" data-screenshot="human-review" aria-label="人工复核 Human Review">
    <PageHeader
      eyebrow="人工复核 / 待决策"
      title="人工复核中心"
      :description="isDemoRuntime ? '对本地合成建议进行可审计复核；决策只写入浏览器内存中的 Demo history。' : '对后端返回的 AI 建议执行复核；状态和 append-only history 由后端确认。'"
    >
      <template #meta><StatusBadge :label="`${reviewableTickets.length} 条待复核`" tone="warning" /></template>
      <template #actions><button type="button" class="button button--secondary" :disabled="flow.loadingTickets.value" @click="hydrateReviewQueue">{{ flow.loadingTickets.value ? '读取中…' : '刷新复核队列' }}</button><a class="button button--ghost" href="#ticket-detail">打开 Workbench →</a></template>
    </PageHeader>

    <ErrorState v-if="flow.lastError.value" :message="flow.lastError.value" />
    <section class="context-callout context-callout--warning" aria-label="人工复核提示"><strong>AI 不会自动关闭工单</strong><p>只有审核人确认建议、风险和证据后，批准（Approve）才会提交状态变更；要求修改（Request changes）/ 驳回（Reject）必须填写原因。</p></section>

    <section class="review-layout">
      <aside class="surface workbench-surface" aria-label="待复核队列">
        <PanelHeader eyebrow="队列 / 待决策" title="待复核队列" :count="reviewableTickets.length" />
        <LoadingState v-if="flow.loadingTickets.value" label="读取待复核队列" />
        <div v-else-if="reviewableTickets.length" class="review-queue-list">
          <button v-for="ticket in reviewableTickets" :key="ticket.id" type="button" class="review-queue-item" :class="{ 'review-queue-item--active': selected?.id === ticket.id }" @click="flow.selectTicket(ticket.id)">
            <span class="review-queue-item__topline"><span class="mono">{{ ticket.id }}</span><StatusBadge :label="priorityLabel(ticket.priority)" :tone="priorityTone(ticket.priority)" :compact="true" /></span>
            <strong>{{ ticket.title }}</strong>
            <span class="review-queue-item__bottom"><span>{{ ticket.requester }} · {{ ticket.updatedAt }}</span><span class="review-queue-item__risk">{{ ticket.aiConfidence }}%</span></span>
          </button>
        </div>
        <EmptyState v-else title="暂无待复核工单" :description="isDemoRuntime ? 'Demo fixture 没有待复核项目；可从 Workbench 运行一条本地 Copilot。' : '后端当前没有返回 REVIEW_REQUIRED / AI_DRAFTED。'" />
      </aside>

      <main class="review-main">
        <template v-if="selected">
          <article class="review-card">
            <header class="review-card__heading">
              <div><p class="page-eyebrow">{{ selected.id }} · {{ selected.category }}</p><h2>{{ selected.title }}</h2><p>{{ selected.requester }} · {{ selected.department }} · {{ selected.systemContext.application }}</p></div>
              <div class="data-strip"><StatusBadge :label="priorityLabel(selected.priority)" :tone="priorityTone(selected.priority)" :compact="true" /><StatusBadge :label="statusLabel(selected.status)" :tone="statusTone(selected.status)" :compact="true" /></div>
            </header>
            <p class="review-answer">{{ answer }}</p>
            <div class="review-signal-grid">
              <article class="review-signal"><span>分析置信度</span><strong>{{ analysis?.confidence ?? '—' }}%</strong></article>
              <article class="review-signal"><span>风险等级</span><strong><StatusBadge :label="safeText(riskLevel)" :tone="riskBadgeTone(riskLevel)" :compact="true" /></strong></article>
              <article class="review-signal"><span>需要人工复核</span><strong>{{ boolLabel(structured?.finalHumanReviewRequired ?? analysis?.finalHumanReviewRequired ?? true) }}</strong></article>
            </div>
          </article>

          <article class="review-card">
            <header class="review-card__heading"><div><p class="page-eyebrow">依据 / 引用</p><h3>审核依据</h3></div><a class="link-button" href="#retrieval-evidence">查看完整证据 →</a></header>
            <div v-if="ragReferences.length || citations.length" class="review-reference-list">
              <div v-for="(hit, index) in ragReferences.slice(0, 3)" :key="`${hit.articleNo}-${index}`" class="review-reference-row"><span class="review-reference-row__index">#{{ index + 1 }}</span><div><strong>{{ hit.articleNo }} · {{ hit.knowledgeTitle }}</strong><small>相关度 {{ safeText(hit.relevanceScore) }} · 已用于回复：{{ boolLabel(hit.usedInDraft) }} · {{ safeText(hit.snippet) }}</small></div></div>
              <div v-for="citation in citations.slice(0, 3)" :key="`${citation.resultCitationId}-${citation.knowledgeArticleId}`" class="review-reference-row"><span class="review-reference-row__index">✓</span><div><strong>{{ citation.knowledgeArticleId }} · 已校验引用</strong><small>{{ citation.evidenceExcerpt }}</small></div></div>
            </div>
            <EmptyState v-else title="暂无引用依据" description="当前分析没有返回检索或 Citation 证据；审核人应谨慎处理。" />
          </article>

          <article v-if="analysis?.riskNotes?.length" class="review-card review-card--risk">
            <header class="review-card__heading"><div><p class="page-eyebrow">注意事项</p><h3>风险提示</h3></div></header>
            <ul class="review-risk-list"><li v-for="note in analysis.riskNotes" :key="note">{{ note }}</li></ul>
          </article>
        </template>
        <EmptyState v-else title="选择待复核工单" description="从左侧队列选择一个 REVIEW_REQUIRED / AI_DRAFTED 工单，查看建议与证据。" />
      </main>

      <aside class="review-side-stack">
        <section class="surface review-card">
          <header class="review-card__heading"><div><p class="page-eyebrow">审核动作</p><h3>提交复核决策</h3></div><StatusBadge :label="isReviewable ? '可操作' : '暂不可操作'" :tone="isReviewable ? 'warning' : 'neutral'" :compact="true" /></header>
          <label class="form-field review-comment-field" for="human-review-comment"><span>复核备注</span><textarea id="human-review-comment" v-model="reviewComment" class="form-control" rows="6" data-e2e="review-comment" placeholder="说明审核结论、修改要求或驳回原因…"></textarea></label>
          <div class="review-actions">
            <button type="button" class="button button--primary" data-e2e="approve-review" :disabled="!canReview" @click="submitReview('approve')">批准 · Approve</button>
            <button type="button" class="button button--warning" :disabled="!canReview" @click="submitReview('request-changes')">要求修改 · Request changes</button>
            <button type="button" class="button button--danger" data-e2e="reject-review" :disabled="!canReview" @click="submitReview('reject')">驳回 · Reject</button>
          </div>
          <p v-if="reviewMessage" class="state-message" :class="{ 'state-message--error': reviewMessageIsError }" aria-live="polite">{{ reviewMessage }}</p>
          <p v-else class="tiny-note review-action-note">{{ isReviewable ? '审核提交后会重新读取当前工单、Trace 和复核历史。' : '只有待复核状态才会启用决策按钮。' }}</p>
        </section>

        <section class="surface-subtle context-callout">
          <strong>审核边界</strong>
          <p>{{ isDemoRuntime ? '这是本地合成数据，复核历史（review history）不会写入后端数据库。' : '这是后端返回的安全摘要，不展开 Provider 原始响应或敏感错误。' }}</p>
          <a class="link-button" href="#trace-timeline">查看运行 Trace →</a>
        </section>
      </aside>
    </section>

    <article class="surface history-panel">
      <PanelHeader eyebrow="复核记录" title="当前记录" :count="flow.reviewHistory.value.length" />
      <div v-if="flow.reviewHistory.value.length" class="history-list">
        <article v-for="record in flow.reviewHistory.value" :key="record.reviewRecordId ?? `${record.runId}-${record.createdAt}`" class="history-row"><time>{{ record.createdAt ?? '未返回' }}</time><div><strong>{{ safeText(record.decision) }} · {{ safeText(record.reviewer) }}</strong><span>{{ safeText(record.comment, '无备注') }}</span></div></article>
      </div>
      <EmptyState v-else title="暂无复核记录" description="提交一次 Demo 或真实后端复核后，记录会从 Trace Evidence 重新读取。" />
    </article>
  </section>
</template>
