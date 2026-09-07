<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { isDemoRuntime } from '../api/tickets'
import { useTicketRealFlow } from '../composables/useTicketRealFlow'
import { boolLabel, displayDateTime, safeText, statusLabel } from '../utils/realFlowFormat'
import EmptyState from '../components/ui/EmptyState.vue'
import ErrorState from '../components/ui/ErrorState.vue'
import LoadingState from '../components/ui/LoadingState.vue'
import PageHeader from '../components/ui/PageHeader.vue'
import PanelHeader from '../components/ui/PanelHeader.vue'
import StatusBadge from '../components/ui/StatusBadge.vue'

const flow = useTicketRealFlow()

onMounted(() => {
  if (flow.backendState.value === 'UNKNOWN') {
    void flow.refresh()
  }
})

const trace = computed(() => flow.trace.value)
const run = computed(() => trace.value?.copilotRun ?? null)
const structured = computed(() => flow.currentStructuredOutput.value)
const steps = computed(() => trace.value?.stepTimeline ?? [])
const modeLabel = computed(() => {
  if (isDemoRuntime) {
    return trace.value?.evidenceSource ?? 'DEMO_LOCAL'
  }
  return trace.value?.evidenceSource === 'IMMUTABLE_RUN' ? 'IMMUTABLE_RUN' : trace.value?.traceMode ?? 'LEGACY_DERIVED'
})
const runStatusTone = computed(() => run.value?.runStatus === 'COMPLETED' ? 'success' : run.value ? 'warning' : 'neutral')
</script>

<template>
  <section class="page-stack" data-screenshot="trace-timeline" aria-label="运行记录 Trace Timeline">
    <PageHeader
      eyebrow="运行记录 / 证据回放"
      :title="isDemoRuntime ? 'Demo Trace 回放' : 'Trace 运行记录回放'"
      :description="isDemoRuntime ? '回放本地 Demo 派生的步骤、检索、结构化输出和复核关系；不代表后端持久化 IMMUTABLE_RUN。' : '回放后端持久化运行证据；查看时不会重新调用 Provider、重新检索或重写 Citation。'"
    >
      <template #meta><StatusBadge :label="modeLabel" :tone="isDemoRuntime ? 'violet' : 'info'" /></template>
      <template #actions><button type="button" class="button button--secondary" :disabled="flow.loadingTickets.value" @click="flow.refresh">{{ flow.loadingTickets.value ? '读取中…' : '刷新 Trace' }}</button><a class="button button--ghost" href="#retrieval-evidence">打开证据分层 →</a></template>
    </PageHeader>

    <ErrorState v-if="flow.traceError.value" title="运行证据未读取 · Trace Evidence" :message="flow.traceError.value" />
    <LoadingState v-else-if="flow.loadingDetail.value && !flow.selectedTicket.value" label="读取 Trace 运行证据" />

    <section class="trace-summary-grid" aria-label="运行摘要">
      <article><span>当前工单</span><strong>{{ flow.selectedTicket.value?.id ?? '未选择' }}</strong><small>{{ flow.selectedTicket.value?.title ?? '从工作台选择工单' }}</small></article>
      <article><span>运行状态</span><strong><StatusBadge :label="safeText(run?.runStatus, '未运行')" :tone="runStatusTone" :compact="true" /></strong><small>证据来源：{{ modeLabel }}</small></article>
      <article><span>总耗时</span><strong>{{ safeText(run?.totalLatencyMs ?? trace?.totalLatency, '—') }} ms</strong><small>包含已返回步骤</small></article>
      <article><span>Trace ID</span><strong class="mono">{{ safeText(trace?.traceId ?? run?.traceId) }}</strong><small>运行 ID（runId）={{ safeText(trace?.runId ?? run?.runId) }}</small></article>
      <article><span>复核门槛</span><strong>{{ boolLabel(trace?.reviewRequired) }}</strong><small>{{ safeText(trace?.currentStep) }}</small></article>
    </section>

    <section class="trace-layout-page">
      <article class="surface workbench-surface">
        <PanelHeader eyebrow="处理过程" title="运行步骤" :count="steps.length" description="显示业务步骤、状态和耗时；不展开原始请求与响应内容。" />
        <div v-if="steps.length" class="trace-step-list">
          <article v-for="(step, index) in steps" :key="`${step.stepName}-${index}-${step.createdAt}`" class="trace-step">
            <span class="trace-step__index">{{ String(index + 1).padStart(2, '0') }}</span>
            <div class="trace-step__body"><strong>{{ step.stepName }}</strong><span>{{ safeText(step.summary, '未返回步骤摘要') }}</span><small>{{ safeText(step.sourceType) }} · {{ safeText(step.status) }} · {{ displayDateTime(step.createdAt) }}</small></div>
            <span class="trace-step__latency">{{ safeText(step.latencyMs, '—') }} ms</span>
          </article>
        </div>
        <EmptyState v-else title="暂无运行步骤" :description="isDemoRuntime ? '运行本地 Copilot 后会生成派生步骤。' : '运行 Copilot 后从 Trace API 读取持久化步骤。'" />
      </article>

      <aside class="trace-side-stack">
        <section class="surface workbench-surface">
          <PanelHeader eyebrow="运行信息" title="运行元数据" />
          <div class="panel-body">
            <dl class="key-value-list">
              <div><dt>runId</dt><dd>{{ safeText(run?.runId) }}</dd></div>
              <div><dt>traceId</dt><dd>{{ safeText(run?.traceId) }}</dd></div>
              <div><dt>开始时间（startedAt）</dt><dd>{{ displayDateTime(run?.startedAt) }}</dd></div>
              <div><dt>完成时间（completedAt）</dt><dd>{{ displayDateTime(run?.completedAt) }}</dd></div>
              <div><dt>请求 Provider（requestedProvider）</dt><dd>{{ safeText(run?.requestedProvider) }}</dd></div>
              <div><dt>实际 Provider（actualProvider）</dt><dd>{{ safeText(run?.actualProvider) }}</dd></div>
              <div><dt>是否 fallback（fallbackUsed）</dt><dd>{{ boolLabel(run?.fallbackUsed) }}</dd></div>
              <div><dt>错误类别（errorCategory）</dt><dd>{{ safeText(run?.errorCategory, 'NONE') }}</dd></div>
            </dl>
          </div>
        </section>
        <section class="surface-subtle context-callout context-callout--warning">
          <strong>安全边界</strong>
          <p>{{ isDemoRuntime ? '本地 Trace 是派生回放，所有 Provider / model 字段是 fixture 事实。' : '原始 prompt、Provider response 和敏感错误不会由此页面展开。' }}</p>
          <a class="link-button" href="#human-review">查看复核门禁 →</a>
        </section>
      </aside>
    </section>

    <section class="trace-detail-grid">
      <article class="surface workbench-surface">
        <PanelHeader eyebrow="处理结果" title="结构化输出与检索引用" :count="trace?.ragReferences?.length ?? 0" />
        <div v-if="structured || trace?.ragReferences?.length" class="panel-body">
          <p v-if="structured" class="result-answer">{{ structured.answer }}</p>
          <div v-if="trace?.ragReferences?.length" class="card-list trace-reference-list">
            <article v-for="(hit, index) in trace.ragReferences.slice(0, 4)" :key="`${hit.articleNo}-${index}`" class="reference-card"><div class="reference-card__topline"><span>#{{ index + 1 }} · {{ hit.articleNo }}</span><span>{{ safeText(hit.relevanceScore) }}</span></div><strong>{{ hit.knowledgeTitle }}</strong><p>{{ safeText(hit.snippet, '无 excerpt snapshot') }}</p></article>
          </div>
        </div>
        <EmptyState v-else title="暂无结构化输出" description="运行尚未产生结构化结果或检索快照（retrieval snapshot）。" />
      </article>
      <article class="surface workbench-surface">
        <PanelHeader eyebrow="状态与复核" title="状态流转与复核记录" :count="(trace?.statusHistory?.length ?? 0) + (trace?.reviewRecords?.length ?? 0)" />
        <div class="panel-body">
          <div v-if="trace?.statusHistory?.length" class="history-list history-list--flush">
            <article v-for="item in trace.statusHistory" :key="item.historyId ?? `${item.toStatus}-${item.occurredAt}`" class="history-row"><time>{{ displayDateTime(item.occurredAt) }}</time><div><strong>{{ statusLabel(item.fromStatus) }} → {{ statusLabel(item.toStatus) }}</strong><span>{{ safeText(item.actor) }} · {{ safeText(item.note, '无备注') }}</span></div></article>
          </div>
          <div v-if="trace?.reviewRecords?.length" class="history-list history-list--compact">
            <article v-for="record in trace.reviewRecords" :key="record.reviewRecordId ?? `${record.runId}-${record.createdAt}`" class="history-row"><time>{{ displayDateTime(record.createdAt) }}</time><div><strong>{{ safeText(record.decision) }} · {{ safeText(record.reviewer) }}</strong><span>{{ safeText(record.comment, '无备注') }}</span></div></article>
          </div>
          <p v-if="!trace?.statusHistory?.length && !trace?.reviewRecords?.length" class="tiny-note">暂无状态或复核记录。</p>
        </div>
      </article>
    </section>
  </section>
</template>
