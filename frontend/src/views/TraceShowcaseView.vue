<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { isDemoRuntime } from '../api/tickets'
import { useTicketRealFlow } from '../composables/useTicketRealFlow'
import { boolLabel, safeText } from '../utils/realFlowFormat'
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
const analysis = computed(() => flow.analysis.value)
const retrievalRows = computed(() => trace.value?.ragReferences ?? [])
const validatedCitations = computed(() => flow.currentValidatedCitations.value)
const currentTicketLabel = computed(() => flow.selectedTicket.value?.id ?? '未选择工单')
const outputStatus = computed(() => trace.value?.copilotRun?.outputValidationStatus ?? trace.value?.structuredOutput?.outputValidationStatus ?? analysis.value?.outputValidationStatus ?? '未返回')
const citationStatus = computed(() => trace.value?.copilotRun?.citationValidationStatus ?? trace.value?.structuredOutput?.citationValidationStatus ?? analysis.value?.citationValidationStatus ?? '未返回')
</script>

<template>
  <section class="page-stack" data-screenshot="trace-timeline" aria-label="检索证据 Retrieval Evidence">
    <PageHeader
      eyebrow="证据 / 两层查看"
      title="检索证据分层"
      :description="isDemoRuntime ? '用本地 Demo 派生快照对照检索参考（Retrieval Reference）和已校验引用（Validated Citation）；它们不代表真实 Provider 结果。' : '检索参考（Retrieval Reference）与已校验模型引用（Validated Model Citation）均来自后端 Trace Evidence；前端不新增或补造 Citation。'"
    >
      <template #meta><StatusBadge :label="trace?.evidenceSource ?? (isDemoRuntime ? 'DEMO_LOCAL' : '等待运行')" :tone="trace ? 'success' : 'neutral'" /></template>
      <template #actions><a class="button button--secondary" href="#trace-timeline">打开 Trace →</a><a class="button button--ghost" href="#knowledge-base">查看知识命中</a></template>
    </PageHeader>

    <ErrorState v-if="flow.traceError.value" title="运行证据未读取 · Trace Evidence" :message="flow.traceError.value" />
    <LoadingState v-else-if="flow.loadingDetail.value && !flow.selectedTicket.value" label="读取检索证据" />

    <section class="data-strip" aria-label="证据上下文">
      <span class="data-chip data-chip--cyan">工单 · {{ currentTicketLabel }}</span>
      <span class="data-chip">检索来源 · {{ retrievalRows.length }} 条</span>
      <span class="data-chip data-chip--green">已校验引用 · {{ validatedCitations.length }} 条</span>
      <span class="data-chip data-chip--violet">运行记录 · {{ safeText(trace?.traceId ?? trace?.runId) }}</span>
    </section>

    <section class="validation-strip" aria-label="校验状态摘要">
      <article><span>输出校验</span><strong>{{ outputStatus }}</strong></article>
      <article><span>引用校验</span><strong>{{ citationStatus }}</strong></article>
      <article><span>有效引用数</span><strong>{{ trace?.structuredOutput?.validCitationCount ?? validatedCitations.length }}</strong></article>
      <article><span>未通过引用数</span><strong>{{ trace?.structuredOutput?.rejectedCitationCount ?? 0 }}</strong></article>
    </section>

    <section class="evidence-layout">
      <article class="surface workbench-surface">
        <PanelHeader eyebrow="检索来源" title="检索引用" :count="retrievalRows.length" description="检索阶段保存的证据快照" />
        <div v-if="retrievalRows.length" class="panel-body card-list">
          <article v-for="(hit, index) in retrievalRows" :key="`${hit.articleNo}-${index}`" class="reference-card">
            <div class="reference-card__topline"><span>第 {{ index + 1 }} 条 · {{ hit.articleNo }}</span><span>相关度 {{ safeText(hit.relevanceScore) }}</span></div>
            <strong>{{ hit.knowledgeTitle }}</strong>
            <p class="reference-card__meta">匹配词：{{ safeText(hit.matchedKeyword) }} · 已用于回复：{{ boolLabel(hit.usedInDraft) }}</p>
            <p>{{ safeText(hit.snippet, '无 excerpt snapshot') }}</p>
          </article>
        </div>
        <EmptyState v-else title="暂无检索参考" :description="isDemoRuntime ? '运行本地 Copilot 后生成派生检索快照。' : '运行后从 /trace-evidence 读取；不会用静态 Mock 补齐。'" />
      </article>

      <article class="surface workbench-surface">
        <PanelHeader eyebrow="已校验引用" title="回复中的引用" :count="validatedCitations.length" description="结构化输出通过引用关系校验后的证据" />
        <div v-if="validatedCitations.length" class="panel-body card-list">
          <article v-for="citation in validatedCitations" :key="`${citation.resultCitationId}-${citation.knowledgeArticleId}`" class="reference-card reference-card--active">
            <div class="reference-card__topline"><span>{{ citation.knowledgeArticleId }}</span><span>{{ citation.citationType }}</span></div>
            <strong>{{ citation.knowledgeTitle }}</strong>
            <p class="reference-card__meta">支持的说明：{{ citation.supportedClaim }}</p>
            <p>{{ citation.evidenceExcerpt }}</p>
          </article>
        </div>
        <EmptyState v-else title="暂无 Validated Citation" description="如果后端返回 NOT_APPLICABLE、INVALID 或尚未运行，这里保持为空并显示校验状态。" />
      </article>
    </section>

    <section class="trace-detail-grid">
      <article class="surface workbench-surface">
        <PanelHeader eyebrow="分析结果" title="知识命中" :count="analysis?.knowledgeHits?.length ?? 0" />
        <div v-if="analysis?.knowledgeHits?.length" class="panel-body card-list">
          <article v-for="hit in analysis.knowledgeHits" :key="hit.id" class="knowledge-hit">
            <div class="knowledge-hit__topline"><span>{{ hit.id }}</span><span>{{ hit.relevance }}%</span></div>
            <strong>{{ hit.title }}</strong>
            <p class="knowledge-hit__meta">负责人={{ hit.owner }} · 最后核验={{ hit.lastVerifiedAt }}</p>
          </article>
        </div>
        <EmptyState v-else title="暂无知识命中" description="只展示 analysis API 返回的 knowledge hits。" />
      </article>
      <aside class="knowledge-side-stack">
        <section class="surface-subtle context-callout context-callout--warning">
          <strong>解读边界</strong>
          <p>引用归属校验（Citation membership validation）只确认引用 ID 属于本次运行允许的检索证据，不代表已完成逐句事实蕴含验证。</p>
        </section>
        <section class="surface-subtle context-callout">
          <strong>下一步</strong>
          <p>如需查看步骤、fallback、状态历史和 review records，请进入同一工单的 Trace Timeline。</p>
          <a class="link-button" href="#trace-timeline">查看 Trace Timeline →</a>
        </section>
      </aside>
    </section>
  </section>
</template>
