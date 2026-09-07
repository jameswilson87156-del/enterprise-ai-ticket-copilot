<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
const selectedReference = ref<string | null>(null)

onMounted(() => {
  if (flow.backendState.value === 'UNKNOWN') {
    void flow.refresh()
  }
})

const analysis = computed(() => flow.analysis.value)
const trace = computed(() => flow.trace.value)
const knowledgeHits = computed(() => analysis.value?.knowledgeHits ?? [])
const references = computed(() => trace.value?.ragReferences ?? [])
const activeReference = computed(() => references.value.find((item) => item.articleNo === selectedReference.value) ?? references.value[0] ?? null)
const queryContext = computed(() => flow.selectedTicket.value?.title ?? '等待选择工单')
const evidenceState = computed(() => {
  if (!flow.selectedTicket.value) {
    return '等待工单'
  }
  if (!trace.value) {
    return '等待 Trace'
  }
  return trace.value.evidenceSource ?? '已读取'
})
</script>

<template>
  <section class="page-stack" data-screenshot="knowledge-base" aria-label="知识库 Knowledge Base">
    <PageHeader
      eyebrow="知识库 / 只读"
      title="知识库与 RAG 引用"
      :description="isDemoRuntime ? '查看本地 Demo 的知识命中和派生检索快照（retrieval snapshot）；不会暗示真实向量库或持久化。' : '查看当前工单由后端返回的知识命中和检索快照；本页不扩展知识管理接口。'"
    >
      <template #meta><StatusBadge :label="isDemoRuntime ? 'Demo · 只读' : 'API · 只读'" :tone="isDemoRuntime ? 'violet' : 'info'" /></template>
      <template #actions><a class="button button--secondary" href="#ticket-detail">返回工单工作台</a><a class="button button--ghost" href="#retrieval-evidence">查看证据分层 →</a></template>
    </PageHeader>

    <ErrorState v-if="flow.lastError.value" :message="flow.lastError.value" />

    <section class="data-strip" aria-label="当前检索上下文">
      <span class="data-chip data-chip--cyan">当前问题 · {{ queryContext }}</span>
      <span class="data-chip">知识命中 · {{ knowledgeHits.length }}</span>
      <span class="data-chip">检索来源 · {{ references.length }}</span>
      <span class="data-chip data-chip--violet">证据状态 · {{ evidenceState }}</span>
    </section>

    <LoadingState v-if="flow.loadingDetail.value && !flow.selectedTicket.value" label="读取工单分析与知识证据" />

    <section v-else class="knowledge-layout">
      <article class="surface workbench-surface">
        <PanelHeader eyebrow="分析结果" title="知识命中" :count="knowledgeHits.length" />
        <div v-if="knowledgeHits.length" class="panel-body card-list">
          <article v-for="hit in knowledgeHits" :key="hit.id" class="knowledge-hit" :class="{ 'knowledge-hit--active': activeReference?.articleNo === hit.id }">
            <div class="knowledge-hit__topline"><span>{{ hit.id }}</span><strong class="knowledge-hit__score">{{ hit.relevance }}%</strong></div>
            <strong>{{ hit.title }}</strong>
            <p class="knowledge-hit__meta">负责人={{ hit.owner }} · 最后核验={{ hit.lastVerifiedAt }}</p>
            <div class="score-bar" aria-label="相关度"><span :style="{ width: `${Math.min(hit.relevance, 100)}%` }"></span></div>
          </article>
        </div>
        <EmptyState v-else title="暂无知识命中" :description="flow.selectedTicket.value ? '当前 AI analysis 没有返回知识条目；页面不会补造文档。' : '先在 Workbench 选择工单并运行 Copilot。'" />
      </article>

      <article class="surface workbench-surface">
        <PanelHeader eyebrow="检索快照" title="检索引用" :count="references.length" />
        <div v-if="references.length" class="panel-body card-list">
          <button v-for="(hit, index) in references" :key="`${hit.articleNo}-${index}`" type="button" class="reference-card" :class="{ 'reference-card--active': activeReference?.articleNo === hit.articleNo }" @click="selectedReference = hit.articleNo">
            <span class="reference-card__topline"><span>#{{ index + 1 }} · {{ hit.articleNo }}</span><span>相关度={{ safeText(hit.relevanceScore) }}</span></span>
            <strong>{{ hit.knowledgeTitle }}</strong>
            <span class="reference-card__meta">keyword={{ safeText(hit.matchedKeyword) }} · usedInDraft={{ boolLabel(hit.usedInDraft) }}</span>
            <p>{{ safeText(hit.snippet, '无 excerpt snapshot') }}</p>
          </button>
        </div>
        <EmptyState v-else title="暂无检索快照" :description="isDemoRuntime ? '运行本地 Copilot 后会生成派生引用。' : '运行后由 /trace-evidence 返回；不会用静态 Mock 补齐。'" />
      </article>

      <aside class="knowledge-side-stack">
        <section class="surface-subtle context-callout">
          <strong>当前引用焦点</strong>
          <span class="context-callout__value">{{ activeReference ? safeText(activeReference.relevanceScore) : '—' }}</span>
          <p>{{ activeReference ? `${activeReference.articleNo} · ${activeReference.knowledgeTitle}` : '选择一个 retrieval reference 查看详情。' }}</p>
          <a class="link-button" href="#retrieval-evidence">打开证据分层 →</a>
        </section>
        <section class="surface-subtle context-callout context-callout--warning">
          <strong>边界说明</strong>
          <p>当前后端没有独立知识库列表/上传接口。本页只读展示分析命中（knowledge hits）和 Trace 检索引用（retrieval references）；引用归属校验（membership validation）不等于事实蕴含验证。</p>
        </section>
        <section class="surface-subtle context-callout">
          <strong>当前工单</strong>
          <p class="mono">{{ flow.selectedTicket.value?.id ?? '未选择' }}</p>
          <p>{{ flow.selectedTicket.value?.title ?? '从 Workbench 选择工单后，这里会保持同一共享选择。' }}</p>
          <a class="link-button" href="#ticket-detail">回到 Workbench →</a>
        </section>
      </aside>
    </section>
  </section>
</template>
