<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { isDemoRuntime } from '../api/tickets'
import { useTicketRealFlow } from '../composables/useTicketRealFlow'
import {
  abstentionLabel,
  boolLabel,
  displayDateTime,
  priorityLabel,
  priorityTone,
  riskBadgeTone,
  safeText,
  statusLabel,
  statusTone
} from '../utils/realFlowFormat'
import type { CreateTicketRequest } from '../types/ticket'
import EmptyState from '../components/ui/EmptyState.vue'
import ErrorState from '../components/ui/ErrorState.vue'
import LoadingState from '../components/ui/LoadingState.vue'
import PageHeader from '../components/ui/PageHeader.vue'
import PanelHeader from '../components/ui/PanelHeader.vue'
import StatusBadge from '../components/ui/StatusBadge.vue'

const flow = useTicketRealFlow()
const createDialog = ref<HTMLDialogElement | null>(null)
const evidenceExpanded = ref(false)

const form = reactive<CreateTicketRequest>({
  title: '合成工单：订单查询出现缓存超时',
  description: '演示订单服务在读取缓存时超时，支持人员无法查看订单详情。此合成样例不含真实用户、账号或凭据。',
  systemName: 'synthetic-order-service',
  errorLog: '',
  urgency: 'P1',
  requester: '演示用户',
  requesterDepartment: '演示支持组'
})

const searchQuery = ref('')
const statusFilter = ref('ALL')
const priorityFilter = ref('ALL')
const formMessage = ref<string | null>(null)
const reviewComment = ref('')
const reviewMessage = ref<string | null>(null)

onMounted(() => {
  if (flow.backendState.value === 'UNKNOWN') {
    void flow.refresh()
  }
})

const selected = computed(() => flow.selectedTicket.value)
const analysis = computed(() => flow.analysis.value)
const trace = computed(() => flow.trace.value)
const run = computed(() => flow.currentRun.value)
const structured = computed(() => flow.currentStructuredOutput.value)
const citations = computed(() => flow.currentValidatedCitations.value)
const ragReferences = computed(() => flow.currentRagReferences.value)
const canRunCopilot = computed(() => Boolean(selected.value && !flow.runningTicketId.value && flow.backendState.value === 'CONNECTED'))
const isReviewable = computed(() => ['REVIEW_REQUIRED', 'AI_DRAFTED'].includes(selected.value?.status ?? ''))
const canReview = computed(() => Boolean(isReviewable.value && !flow.reviewSubmitting.value))
const createdTime = computed(() => selected.value?.timeline?.[0]?.time ?? '当前工单详情未暴露创建时间')
const validatedCitationCount = computed(() => structured.value?.validCitationCount ?? citations.value.length)
const displayedAnswer = computed(() => structured.value?.answer ?? analysis.value?.replySuggestion ?? 'Copilot 尚未返回建议。')
const riskLevel = computed(() => structured.value?.riskLevel ?? analysis.value?.riskLevel ?? (selected.value?.priority === 'P1' ? 'HIGH' : selected.value?.priority === 'P2' ? 'MEDIUM' : 'LOW'))
const formMessageIsError = computed(() => Boolean(formMessage.value && !formMessage.value.startsWith('工单已')))
const reviewMessageIsError = computed(() => Boolean(reviewMessage.value && !reviewMessage.value.startsWith('人工复核结果')))
const dataSourceLabel = computed(() => {
  if (flow.backendState.value === 'UNAVAILABLE') {
    return '后端不可用'
  }
  if (flow.backendState.value !== 'CONNECTED') {
    return '连接中'
  }
  return isDemoRuntime ? '本地演示' : '后端 API'
})
const filteredTickets = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return flow.tickets.value.filter((ticket) => {
    const searchable = `${ticket.id} ${ticket.title} ${ticket.requester} ${ticket.category} ${ticket.team} ${ticket.searchText ?? ''}`.toLowerCase()
    const matchesQuery = !query || searchable.includes(query)
    const matchesStatus = statusFilter.value === 'ALL' || ticket.status === statusFilter.value
    const matchesPriority = priorityFilter.value === 'ALL' || ticket.priority === priorityFilter.value
    return matchesQuery && matchesStatus && matchesPriority
  })
})
const reviewGateLabel = computed(() => {
  if (!selected.value) {
    return '选择工单'
  }
  if (isReviewable.value) {
    return '待人工决策'
  }
  return '未进入复核'
})

function scrollToSection(id: string) {
  const target = document.getElementById(id)
  if (!target) {
    return
  }
  target.scrollIntoView({
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
    block: 'start'
  })
}

function validateForm() {
  if (!form.title.trim() || !form.description.trim() || !form.systemName.trim()) {
    return '请填写标题、描述和系统名称。'
  }
  if (!form.requester?.trim() || !form.requesterDepartment?.trim()) {
    return '请填写请求人和所属部门。'
  }
  return null
}

async function submitCreate() {
  formMessage.value = validateForm()
  if (formMessage.value) {
    return
  }
  try {
    await flow.createSyntheticTicket({ ...form, errorLog: form.errorLog ?? '' })
    formMessage.value = isDemoRuntime
      ? '工单已写入本地 Demo 内存，并已重新读取本地列表。'
      : '工单已通过后端 API 创建，并已从 API 重新读取。'
  } catch {
    formMessage.value = flow.lastError.value
  }
}

async function runCopilot() {
  try {
    await flow.runSelectedCopilot()
  } catch {
    // flow.lastError contains the sanitized error for the user-facing state.
  }
}

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
    await flow.submitReview(decision, comment || '审核通过。')
    reviewMessage.value = isDemoRuntime
      ? '人工复核结果已写入本地 Demo，并重新读取 Trace。'
      : '人工复核结果已由后端确认并重新读取 Trace。'
  } catch {
    reviewMessage.value = flow.lastError.value
  }
}
</script>

<template>
  <section class="page-stack workbench-page" data-screenshot="ticket-workbench" aria-label="工单工作台 Ticket Workbench">
    <PageHeader
      eyebrow="工单 / 工作台"
      title="工单工作台"
      :description="isDemoRuntime ? '本地演示工作区 · 选择工单，核对证据，提交复核。' : '选择工单，核对证据，提交复核。所有变更由后端确认。'"
    >
      <template #meta>
        <StatusBadge :label="dataSourceLabel" :tone="flow.backendState.value === 'UNAVAILABLE' ? 'danger' : isDemoRuntime ? 'violet' : 'success'" />
      </template>
      <template #actions>
        <button type="button" class="button button--primary" @click="createDialog?.showModal()">＋ 创建合成工单</button>
        <a class="button button--ghost" href="#trace-timeline">查看 Trace →</a>
      </template>
    </PageHeader>

    <ErrorState v-if="flow.lastError.value" :message="flow.lastError.value" />

    <nav class="workbench-mobile-flow" aria-label="移动端工单处理流程">
      <a href="#ticket-queue" @click.prevent="scrollToSection('ticket-queue')"><span>01</span> 队列</a>
      <a href="#ticket-context" @click.prevent="scrollToSection('ticket-context')"><span>02</span> 工单</a>
      <a href="#copilot-decision-center" @click.prevent="scrollToSection('copilot-decision-center')"><span>03</span> 建议与证据</a>
      <a href="#copilot-review" @click.prevent="scrollToSection('copilot-review')"><span>04</span> 复核</a>
    </nav>

    <div class="workbench-layout">
      <aside id="ticket-queue" class="surface workbench-surface" aria-label="工单队列">
        <PanelHeader eyebrow="队列 / 优先级" title="工单队列" :count="filteredTickets.length">
          <span class="panel-count">{{ flow.pendingReviewCount.value }} 待复核</span>
        </PanelHeader>
        <div class="queue-toolbar">
          <input v-model="searchQuery" class="search-input" type="search" aria-label="搜索工单" placeholder="搜索 ID、标题或请求人…" />
          <div class="queue-filters">
            <select v-model="statusFilter" class="filter-select" aria-label="按状态过滤">
              <option value="ALL">所有状态</option>
              <option value="PENDING_PROCESS">待处理</option>
              <option value="IN_PROGRESS">处理中</option>
              <option value="REVIEW_REQUIRED">待人工复核</option>
              <option value="RESOLVED">已解决</option>
              <option value="KNOWLEDGE_BASED">已沉淀</option>
            </select>
            <select v-model="priorityFilter" class="filter-select" aria-label="按优先级过滤">
              <option value="ALL">所有优先级</option>
              <option value="P1">P1 紧急</option>
              <option value="P2">P2 高</option>
              <option value="P3">P3 中</option>
            </select>
          </div>
          <div class="queue-filter-note"><span>已载入 {{ flow.tickets.value.length }} 条</span><strong>{{ filteredTickets.length }} 条匹配</strong></div>
        </div>
        <LoadingState v-if="flow.loadingTickets.value" label="读取工单队列" />
        <div v-else-if="filteredTickets.length" class="queue-list-modern">
          <button v-for="ticket in filteredTickets" :key="ticket.id" type="button" class="queue-item" :class="{ 'queue-item--active': flow.selectedId.value === ticket.id }" @click="flow.selectTicket(ticket.id)">
            <span class="queue-item__rail" :class="`queue-item__rail--${ticket.priority.toLowerCase()}`" aria-hidden="true"></span>
            <span class="queue-item__body">
              <span class="queue-item__topline"><span class="queue-item__id">{{ ticket.id }}</span><StatusBadge :label="statusLabel(ticket.status)" :tone="statusTone(ticket.status)" :compact="true" /></span>
              <strong>{{ ticket.title }}</strong>
              <span class="queue-item__meta"><span>{{ ticket.requester }}</span><span class="dot-separator" aria-hidden="true"></span><span>{{ ticket.category }}</span></span>
              <span class="queue-item__bottom"><span>{{ ticket.updatedAt }}</span><span class="queue-item__confidence">{{ ticket.aiConfidence }}%</span></span>
            </span>
          </button>
        </div>
        <EmptyState v-else title="没有匹配工单" :description="flow.tickets.value.length ? '调整搜索或过滤条件；筛选只作用于当前已载入列表。' : isDemoRuntime ? 'Demo fixture 当前为空，可以从下方创建一条合成工单。' : '后端列表为空，页面不会用静态 Mock 补齐。'" />
      </aside>

      <article id="ticket-context" class="surface workbench-surface ticket-context" data-screenshot="ticket-detail" :aria-busy="flow.loadingDetail.value">
        <LoadingState v-if="flow.loadingDetail.value && !selected" label="读取工单详情与 AI 分析" />
        <template v-else-if="selected">
          <header class="ticket-context__header">
            <div class="ticket-context__topline">
              <span class="ticket-context__id">{{ selected.id }}</span>
              <div class="ticket-context__badges">
                <StatusBadge :label="priorityLabel(selected.priority)" :tone="priorityTone(selected.priority)" :compact="true" />
                <StatusBadge :label="statusLabel(selected.status)" :tone="statusTone(selected.status)" />
              </div>
            </div>
            <h2>{{ selected.title }}</h2>
          </header>
          <div class="ticket-context__meta-grid">
            <div class="metadata-cell"><span>请求人</span><strong>{{ selected.requester }}</strong></div>
            <div class="metadata-cell"><span>所属部门</span><strong>{{ selected.department }}</strong></div>
            <div class="metadata-cell"><span>系统</span><strong>{{ selected.systemContext.application }}</strong></div>
            <div class="metadata-cell"><span>分类</span><strong>{{ selected.category }}</strong></div>
            <div class="metadata-cell"><span>环境</span><strong>{{ selected.systemContext.environment }}</strong></div>
            <div class="metadata-cell"><span>创建时间</span><strong>{{ createdTime }}</strong></div>
          </div>
          <div class="ticket-story">
            <section class="story-block">
              <div class="story-block__heading"><h3>工单描述</h3><span>请求上下文</span></div>
              <p>{{ selected.description }}</p>
            </section>
            <section v-if="selected.errorLogs.length" class="story-block">
              <div class="story-block__heading"><h3>错误日志</h3><span>脱敏快照</span></div>
              <pre>{{ selected.errorLogs.join('\n') }}</pre>
            </section>
            <section class="story-block">
              <div class="story-block__heading"><h3>状态时间线</h3><span>{{ selected.timeline.length }} 条变更</span></div>
              <div class="timeline-list">
                <article v-for="item in selected.timeline" :key="`${item.time}-${item.state}`" class="timeline-item">
                  <time>{{ item.time }}</time>
                  <strong>{{ statusLabel(item.state) }}</strong>
                  <span>{{ item.actor }} · {{ item.note }}</span>
                </article>
              </div>
            </section>
          </div>
        </template>
        <EmptyState v-else title="选择一条工单" description="从左侧队列选择工单后，读取 TicketDetail、AI 分析和状态上下文。" />
      </article>

      <aside class="copilot-rail" aria-label="处理建议">
        <div class="copilot-rail__header">
          <div><h2>处理建议</h2><p>{{ isDemoRuntime ? '本地演示数据' : '由后端返回' }} · 需要人工确认</p></div>
          <StatusBadge :label="reviewGateLabel" :tone="isReviewable ? 'warning' : 'neutral'" :compact="true" />
        </div>

        <section id="copilot-decision-center" class="copilot-card copilot-card--decision" aria-labelledby="copilot-decision-title">
          <div class="copilot-card__label"><span>Copilot 决策建议</span><StatusBadge :label="structured ? '已生成' : analysis ? '分析预览' : '待运行'" :tone="structured ? 'success' : analysis ? 'warning' : 'neutral'" :compact="true" /></div>
          <div class="copilot-decision__headline">
            <div>
              <p class="copilot-decision__kicker">推荐处理</p>
              <h3 id="copilot-decision-title">{{ safeText(analysis?.classification, '等待 Copilot 运行') }}</h3>
            </div>
            <span class="copilot-decision__confidence">{{ analysis?.confidence ?? '—' }}<small>% 置信度</small></span>
          </div>
          <p class="copilot-decision__note">{{ analysis ? (structured?.abstained || analysis.abstained ? abstentionLabel(structured?.abstentionReasonCode ?? analysis.abstentionReasonCode) : '先核对右侧证据，再决定是否采纳建议回复。') : '选择工单并运行 Copilot 后，这里会显示可复核的处理结论。' }}</p>
          <button type="button" class="button button--primary copilot-decision__run" data-e2e="run-copilot" :disabled="!canRunCopilot" @click="runCopilot">
            {{ flow.runningTicketId.value ? 'Copilot 运行中…' : isDemoRuntime ? '运行本地 Copilot' : '运行后端 Copilot' }}
          </button>
          <p class="tiny-note copilot-decision__run-note">{{ selected ? '运行会重新读取工单详情、分析、Trace 和指标。' : '先选择一条工单。' }}</p>
        </section>

        <section class="copilot-card copilot-card--reply" aria-labelledby="copilot-reply-title">
          <div class="copilot-card__label"><span id="copilot-reply-title">建议回复</span><span>{{ structured ? '结构化结果' : '分析预览' }}</span></div>
          <p class="copilot-card__copy">{{ displayedAnswer }}</p>
        </section>

        <section class="copilot-card">
          <div class="copilot-card__label"><span>关联证据</span><button type="button" class="link-button" @click="evidenceExpanded = true; scrollToSection('workbench-evidence')">查看全部 →</button></div>
          <div v-if="ragReferences.length" class="evidence-preview">
            <div v-for="(hit, index) in ragReferences.slice(0, 3)" :key="`${hit.articleNo}-${index}`" class="evidence-preview__row">
              <span class="evidence-preview__rank">#{{ index + 1 }}</span>
              <strong>{{ hit.knowledgeTitle }}</strong>
              <small>{{ safeText(hit.relevanceScore) }}</small>
            </div>
          </div>
          <p v-else class="tiny-note flow-note--top">暂无检索快照（retrieval snapshot）；运行 Copilot 后以 API 返回为准。</p>
        </section>

        <section class="copilot-card copilot-card--risk-summary" aria-labelledby="copilot-risk-title">
          <div class="copilot-card__label"><span id="copilot-risk-title">风险与复核门禁</span><StatusBadge :label="reviewGateLabel" :tone="isReviewable ? 'warning' : 'neutral'" :compact="true" /></div>
          <div class="signal-grid">
            <article class="signal-card"><span>风险等级</span><strong>{{ safeText(riskLevel) }}</strong><small>{{ structured?.abstained || analysis?.abstained ? '安全拒答（abstained）' : '复核门禁（review gate）' }}</small></article>
            <article class="signal-card"><span>分类置信度</span><strong>{{ analysis?.confidence ?? '—' }}%</strong><small>当前分析</small></article>
            <article class="signal-card"><span>已校验引用</span><strong>{{ validatedCitationCount }}</strong><small>引用集合（citation set）</small></article>
          </div>
          <p class="copilot-risk-note">{{ structured?.abstained || analysis?.abstained ? abstentionLabel(structured?.abstentionReasonCode ?? analysis?.abstentionReasonCode) : isReviewable ? '建议已进入人工复核，动作提交前仍需确认备注。' : '当前工单尚未进入复核门禁，执行动作保持 disabled。' }}</p>
        </section>

        <section id="copilot-review" class="copilot-card" :class="{ 'copilot-card--risk': isReviewable }">
          <div class="copilot-card__label"><span>人工复核</span><strong>{{ reviewGateLabel }}</strong></div>
          <textarea v-model="reviewComment" class="form-control review-comment-input" rows="3" data-e2e="review-comment" aria-label="人工复核备注" placeholder="补充审核备注…"></textarea>
          <div class="decision-actions">
            <button type="button" class="button button--primary" data-e2e="approve-review" :disabled="!canReview" @click="submitReview('approve')">批准 · Approve</button>
            <div class="decision-actions__row">
              <button type="button" class="button button--warning" :disabled="!canReview" @click="submitReview('request-changes')">要求修改</button>
              <button type="button" class="button button--danger" data-e2e="reject-review" :disabled="!canReview" @click="submitReview('reject')">驳回 · Reject</button>
            </div>
          </div>
          <p v-if="reviewMessage" class="state-message" :class="{ 'state-message--error': reviewMessageIsError }" aria-live="polite">{{ reviewMessage }}</p>
          <p v-else class="tiny-note flow-note--top">非待复核状态按钮保持 disabled；复核成功后会重新读取复核历史（review history）。</p>
        </section>

        <details class="copilot-card metadata-details">
          <summary>运行信息 / 审计字段</summary>
          <dl class="key-value-list">
            <div><dt>runId</dt><dd>{{ safeText(run?.runId) }}</dd></div>
            <div><dt>traceId</dt><dd>{{ safeText(run?.traceId) }}</dd></div>
            <div><dt>runStatus</dt><dd>{{ safeText(run?.runStatus) }}</dd></div>
            <div><dt>evidenceSource</dt><dd>{{ safeText(trace?.evidenceSource) }}</dd></div>
            <div><dt>actualProvider</dt><dd>{{ safeText(run?.actualProvider) }}</dd></div>
            <div><dt>fallbackUsed</dt><dd>{{ boolLabel(run?.fallbackUsed) }}</dd></div>
            <div><dt>abstention</dt><dd>{{ abstentionLabel(run?.abstentionReasonCode ?? structured?.abstentionReasonCode) }}</dd></div>
            <div><dt>validation</dt><dd>{{ safeText(run?.citationValidationStatus ?? structured?.citationValidationStatus) }}</dd></div>
          </dl>
        </details>
      </aside>
    </div>

    <dialog ref="createDialog" class="ticket-create-dialog" aria-labelledby="create-dialog-title">
      <header class="ticket-create-dialog__heading"><div><small>新建 / 合成数据</small><h2 id="create-dialog-title">创建工单</h2></div><button type="button" class="button button--ghost" aria-label="关闭创建工单" @click="createDialog?.close()">关闭 ×</button></header>
      <article id="create-ticket" class="surface form-panel">
        <form class="form-layout" @submit.prevent="submitCreate">
          <label class="form-field" for="showcase-ticket-title"><span>标题</span><input id="showcase-ticket-title" v-model="form.title" class="form-control" data-e2e="ticket-title" /></label>
          <label class="form-field" for="showcase-ticket-description"><span>描述</span><textarea id="showcase-ticket-description" v-model="form.description" class="form-control" rows="4" data-e2e="ticket-description"></textarea></label>
          <div class="form-grid">
            <label class="form-field" for="showcase-ticket-system"><span>系统名称</span><input id="showcase-ticket-system" v-model="form.systemName" class="form-control" data-e2e="ticket-system" /></label>
            <label class="form-field" for="showcase-ticket-urgency"><span>紧急程度</span><select id="showcase-ticket-urgency" v-model="form.urgency" class="filter-select"><option>P1</option><option>P2</option><option>P3</option></select></label>
          </div>
          <div class="form-grid">
            <label class="form-field" for="showcase-ticket-requester"><span>请求人</span><input id="showcase-ticket-requester" v-model="form.requester" class="form-control" /></label>
            <label class="form-field" for="showcase-ticket-department"><span>所属部门</span><input id="showcase-ticket-department" v-model="form.requesterDepartment" class="form-control" /></label>
          </div>
          <p v-if="formMessage" class="state-message" :class="{ 'state-message--error': Boolean(formMessageIsError) }" aria-live="polite">{{ formMessage }}</p>
          <p v-else class="tiny-note">errorLog 默认留空；仅使用合成数据，不输入真实用户、账号、IP、凭据或秘密。</p>
          <button type="submit" class="button button--primary" data-e2e="create-ticket" :disabled="flow.creatingTicket.value">{{ flow.creatingTicket.value ? '提交中…' : isDemoRuntime ? '写入本地 Demo' : '通过后端 API 创建' }}</button>
        </form>
      </article>
    </dialog>
    <details id="workbench-evidence" class="workbench-evidence" :open="evidenceExpanded" @toggle="evidenceExpanded = ($event.target as HTMLDetailsElement).open">
      <summary><span>完整证据与审核记录</span><small>{{ validatedCitationCount }} 条引用 · {{ flow.reviewHistory.value.length }} 条复核记录</small></summary>
      <article class="surface result-panel">
        <PanelHeader eyebrow="处理结果" title="结构化输出与引用证据" :count="validatedCitationCount" />
        <div v-if="structured || analysis" class="result-body">
          <p class="result-answer">{{ displayedAnswer }}</p>
          <div class="result-grid">
            <article class="result-card"><span>风险等级</span><strong><StatusBadge :label="safeText(riskLevel)" :tone="riskBadgeTone(riskLevel)" :compact="true" /></strong></article>
            <article class="result-card"><span>拒答</span><strong>{{ boolLabel(structured?.abstained ?? analysis?.abstained) }}</strong></article>
            <article class="result-card"><span>最终复核</span><strong>{{ boolLabel(structured?.finalHumanReviewRequired ?? analysis?.finalHumanReviewRequired) }}</strong></article>
          </div>
          <p class="tiny-note">{{ structured?.abstained || analysis?.abstained ? abstentionLabel(structured?.abstentionReasonCode ?? analysis?.abstentionReasonCode) : '未触发拒答；仍需按复核门禁确认可执行动作。' }}</p>
          <div class="evidence-columns">
            <section class="evidence-card"><h3>检索来源</h3><div v-if="ragReferences.length" class="card-list"><div v-for="(hit, index) in ragReferences" :key="`${hit.articleNo}-${index}`" class="evidence-row"><div class="evidence-row__meta"><span>#{{ index + 1 }}</span><span>{{ hit.articleNo }}</span><span>相关度 {{ safeText(hit.relevanceScore) }}</span></div><strong>{{ hit.knowledgeTitle }}</strong><p>{{ safeText(hit.snippet, '无摘录快照') }}</p></div></div><p v-else class="tiny-note flow-note--top">暂无检索快照。</p></section>
            <section class="evidence-card"><h3>已校验引用</h3><div v-if="citations.length" class="card-list"><div v-for="citation in citations" :key="`${citation.resultCitationId}-${citation.knowledgeArticleId}`" class="evidence-row"><div class="evidence-row__meta"><span>{{ citation.knowledgeArticleId }}</span><span>{{ citation.citationType }}</span></div><strong>{{ citation.knowledgeTitle }}</strong><p>{{ citation.evidenceExcerpt }}</p></div></div><p v-else class="tiny-note flow-note--top">暂无已校验引用。</p></section>
          </div>
        </div>
        <EmptyState v-else title="等待 Copilot 输出" :description="isDemoRuntime ? '选择工单并运行本地 Copilot，随后展示 Demo 证据。' : '选择工单并运行 Copilot，随后只展示后端安全结果。'" />
      </article>

    <article class="surface history-panel">
      <PanelHeader eyebrow="复核记录" title="人工复核历史" :count="flow.reviewHistory.value.length" />
      <div v-if="flow.reviewHistory.value.length" class="history-list">
        <article v-for="record in flow.reviewHistory.value" :key="record.reviewRecordId ?? `${record.runId}-${record.createdAt}`" class="history-row">
          <time>{{ displayDateTime(record.createdAt) }}</time>
          <div><strong>{{ safeText(record.decision) }} · {{ safeText(record.reviewer) }}</strong><span>{{ safeText(record.comment, '无备注') }}</span><small>runId={{ safeText(record.runId) }} · {{ safeText(record.previousStatus) }} → {{ safeText(record.newStatus) }}</small></div>
        </article>
      </div>
      <EmptyState v-else title="暂无审核历史" :description="isDemoRuntime ? '运行并提交本地 Demo 复核后，这里会出现 append-only history。' : '历史只来自 Trace API 返回的 reviewRecords。'" />
    </article>
    </details>
  </section>
</template>
