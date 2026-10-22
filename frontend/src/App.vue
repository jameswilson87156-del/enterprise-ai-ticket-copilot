<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import {
  confirmKnowledgeDraft,
  createKnowledgeDraft,
  createTicket,
  fetchAiAnalysis,
  fetchMetrics,
  fetchTicket,
  fetchTickets,
  isDemoRuntime,
  runRetrievalEvaluation,
  updateTicketStatus
} from './api/tickets'
import AiRecommendationPanel from './components/AiRecommendationPanel.vue'
import MetricStrip from './components/MetricStrip.vue'
import SearchFilterBar from './components/SearchFilterBar.vue'
import StatusTimeline from './components/StatusTimeline.vue'
import TicketDetailPanel from './components/TicketDetailPanel.vue'
import TicketIntakePanel from './components/TicketIntakePanel.vue'
import TicketQueue from './components/TicketQueue.vue'
import type { AiAnalysis, CreateTicketRequest, RetrievalEvaluationCase, RetrievalEvaluationReport, TicketDetail, TicketStatus, TicketSummary, WorkbenchMetrics } from './types/ticket'

type TicketFilter = 'ALL' | 'PENDING' | 'REVIEW' | 'KNOWLEDGE'

const tickets = shallowRef<TicketSummary[]>([])
const selectedTicketId = shallowRef<string | null>(null)
const selectedTicket = shallowRef<TicketDetail | null>(null)
const selectedAnalysis = shallowRef<AiAnalysis | null>(null)
const metrics = shallowRef<WorkbenchMetrics | null>(null)
const loading = shallowRef(false)
const submitting = shallowRef(false)
const error = shallowRef('')
const searchKeyword = ref('')
const activeFilter = ref<TicketFilter>('ALL')
const activeView = ref<'tickets' | 'evaluation'>('tickets')
const evaluationReport = shallowRef<RetrievalEvaluationReport | null>(null)
const evaluationBusy = ref(false)
const evaluationError = ref('')

const pendingConfirmationCount = computed(() => tickets.value.filter((ticket) => ['PENDING_PROCESS', 'IN_PROGRESS'].includes(ticket.status)).length)
const knowledgeBasedCount = computed(() => tickets.value.filter((ticket) => ticket.status === 'KNOWLEDGE_BASED').length)
const aiAnalyzedCount = computed(() => tickets.value.filter((ticket) => ticket.aiConfidence > 0).length)

const metricItems = computed(() => [
  { label: '今日工单', value: String(tickets.value.length), delta: isDemoRuntime ? '本地演示数据' : 'MySQL 数据', tone: 'steel' as const },
  { label: '待处理', value: String(metrics.value?.pendingTickets ?? pendingConfirmationCount.value), delta: '人工队列', tone: 'warm' as const },
  { label: 'AI 已分析', value: String(aiAnalyzedCount.value), delta: `${metrics.value?.aiHitRate ?? 0}% 规则命中`, tone: 'mint' as const },
  { label: '待人工确认', value: String(pendingConfirmationCount.value), delta: '建议不自动执行', tone: 'danger' as const },
  { label: '已沉淀知识', value: String(knowledgeBasedCount.value), delta: `草稿 ${metrics.value?.todayKnowledgeDrafts ?? 0}`, tone: 'knowledge' as const }
])

const filteredTickets = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return tickets.value.filter((ticket) => {
    const matchesKeyword =
      !keyword ||
      [ticket.id, ticket.title, ticket.requester, ticket.team, ticket.category].some((value) => value.toLowerCase().includes(keyword))
    const matchesFilter =
      activeFilter.value === 'ALL' ||
      (activeFilter.value === 'PENDING' && ['PENDING_CLASSIFICATION', 'PENDING_PROCESS'].includes(ticket.status)) ||
      (activeFilter.value === 'REVIEW' && ['PENDING_PROCESS', 'IN_PROGRESS'].includes(ticket.status)) ||
      (activeFilter.value === 'KNOWLEDGE' && ticket.status === 'KNOWLEDGE_BASED')
    return matchesKeyword && matchesFilter
  })
})

const recentAiRecords = computed(() =>
  tickets.value.slice(0, 4).map((ticket) => ({
    id: ticket.id,
    title: ticket.title,
    category: ticket.category,
    confidence: ticket.aiConfidence,
    status: ticket.status
  }))
)

const knowledgeRows = computed(() => {
  const hits = selectedAnalysis.value?.knowledgeHits ?? []
  const draft = selectedTicket.value?.knowledgeDraft
  return [
    ...hits.map((hit) => ({
      id: hit.id,
      title: hit.title,
      meta: `${hit.owner} / 命中 ${hit.relevance}% / 校验 ${hit.lastVerifiedAt}`,
      state: '可参考'
    })),
    ...(draft
      ? [
          {
            id: draft.articleNo,
            title: draft.title,
            meta: `${draft.owner} / 来源 ${selectedTicket.value?.id ?? '-'} / ${draft.category}`,
            state: draft.status === 'PUBLISHED' ? '已发布' : '待审核'
          }
        ]
      : [])
  ]
})

const humanRecords = computed(() =>
  (selectedTicket.value?.timeline ?? [])
    .filter((event) => !['System', 'Rule Engine', 'Rule Template', '系统', '规则引擎', '规则模板'].includes(event.actor))
    .slice(-4)
    .reverse()
)

const runtimeText = computed(() => {
  if (error.value) {
    return error.value
  }
  return isDemoRuntime ? '本地 Demo 模式 / 演示数据' : 'MySQL 数据 / 人工确认闭环'
})

const runEvaluation = async () => {
  if (isDemoRuntime) {
    return
  }
  evaluationBusy.value = true
  evaluationError.value = ''
  try {
    evaluationReport.value = await runRetrievalEvaluation()
  } catch (err) {
    evaluationError.value = err instanceof Error ? err.message : '评测运行失败'
  } finally {
    evaluationBusy.value = false
  }
}

const formatPercent = (value: number) => `${(value * 100).toFixed(1)}%`

const evaluationOutcome = (testCase: RetrievalEvaluationCase) => {
  if (testCase.relevantArticleNos.length === 0) {
    return testCase.correctNoMatch ? '正确无召回' : `误召回 ${testCase.retrieved.length} 条`
  }
  if (testCase.firstRelevantRank === 0) {
    return '未召回相关知识'
  }
  return `首次相关第 ${testCase.firstRelevantRank} 位`
}

const evaluationQueryTypeText = (queryType: string) => {
  const labels: Record<string, string> = {
    direct: '直接问句',
    paraphrase: '改写问句',
    hard_negative: '易混淆正例',
    no_match: '无匹配负例'
  }
  return labels[queryType] ?? queryType
}

const evaluationFailureText = (testCase: RetrievalEvaluationCase) => {
  if (testCase.failureType === 'UNEXPECTED_RETRIEVAL') {
    return '无匹配问题发生误召回'
  }
  if (testCase.failureType === 'NO_RELEVANT_ARTICLE_RETRIEVED') {
    return '正向问题无相关知识命中'
  }
  if (testCase.failureType === 'PARTIAL_RELEVANT_RECALL') {
    return '多相关知识只召回部分'
  }
  if (testCase.failureType === 'RELEVANT_ARTICLE_RANKED_BELOW_FIRST') {
    return '相关知识未排首位'
  }
  return testCase.queryType === 'no_match' ? '未误召回' : '相关知识排首位'
}

const loadTickets = async () => {
  tickets.value = await fetchTickets()
  metrics.value = await fetchMetrics()
  if (!selectedTicketId.value && tickets.value.length) {
    await selectTicket(tickets.value[0].id)
  }
  if (!tickets.value.length) {
    selectedTicketId.value = null
    selectedTicket.value = null
    selectedAnalysis.value = null
  }
}

const selectTicket = async (id: string) => {
  selectedTicketId.value = id
  const [detail, analysis] = await Promise.all([fetchTicket(id), fetchAiAnalysis(id)])
  selectedTicket.value = detail
  selectedAnalysis.value = analysis
}

const refreshSelected = async () => {
  if (selectedTicketId.value) {
    await selectTicket(selectedTicketId.value)
  }
  await loadTickets()
}

const run = async (task: () => Promise<void>) => {
  loading.value = true
  error.value = ''
  try {
    await task()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '操作失败'
  } finally {
    loading.value = false
  }
}

const submitTicket = async (payload: CreateTicketRequest) => {
  submitting.value = true
  try {
    await run(async () => {
      const detail = await createTicket(payload)
      await loadTickets()
      await selectTicket(detail.id)
    })
  } finally {
    submitting.value = false
  }
}

const changeStatus = async (status: TicketStatus) => {
  if (!selectedTicketId.value) {
    return
  }
  const note = status === 'RESOLVED' ? '人工确认处理完成，等待知识沉淀。' : '人工确认接手处理。'
  await run(async () => {
    await updateTicketStatus(selectedTicketId.value as string, status, note, status === 'RESOLVED' ? '已由支持人员确认解决。' : '')
    await refreshSelected()
  })
}

const generateDraft = async () => {
  if (!selectedTicketId.value) {
    return
  }
  await run(async () => {
    await createKnowledgeDraft(selectedTicketId.value as string)
    await refreshSelected()
  })
}

const confirmDraft = async () => {
  const articleNo = selectedTicket.value?.knowledgeDraft?.articleNo
  if (!articleNo) {
    return
  }
  await run(async () => {
    await confirmKnowledgeDraft(articleNo)
    await refreshSelected()
  })
}

onMounted(() => {
  void run(loadTickets)
})
</script>

<template>
  <div class="workbench-shell" data-screenshot="dashboard">
    <header class="page-hero">
      <div class="hero-copy">
        <span class="mode-chip" :class="{ 'mode-chip--error': error }">
          <span class="pulse-dot"></span>
          {{ runtimeText }}
        </span>
        <p class="eyebrow">企业 AI 工单 Copilot</p>
        <h1>企业 AI 工单作战台</h1>
        <p class="hero-lede">围绕工单分类、知识库匹配、处理建议和人工确认，构建可信的企业支持闭环。</p>
        <p class="hero-note">AI 辅助分析，人工最终确认；{{ isDemoRuntime ? '当前使用本地演示数据。' : '工单数据来自后端服务。' }}</p>
      </div>
    </header>

    <nav class="workbench-nav" aria-label="工作台功能">
      <button class="workbench-nav__tab" :class="{ 'workbench-nav__tab--active': activeView === 'tickets' }" @click="activeView = 'tickets'">工单工作台</button>
      <button class="workbench-nav__tab" :class="{ 'workbench-nav__tab--active': activeView === 'evaluation' }" @click="activeView = 'evaluation'">RAG 检索评测</button>
    </nav>

    <template v-if="activeView === 'tickets'">
    <SearchFilterBar v-model:keyword="searchKeyword" v-model:filter="activeFilter" />

    <MetricStrip :metrics="metricItems" />

    <section class="workspace-grid">
      <div class="ticket-column">
        <TicketQueue :tickets="filteredTickets" :selected-id="selectedTicketId" @select="(id) => run(() => selectTicket(id))" />
        <TicketIntakePanel :submitting="submitting" @submit="submitTicket" />
      </div>
      <div class="resolution-column">
        <TicketDetailPanel :ticket="selectedTicket" />
        <AiRecommendationPanel
          :analysis="selectedAnalysis"
          :ticket="selectedTicket"
          :busy="loading"
          @status="changeStatus"
          @draft="generateDraft"
          @confirm-draft="confirmDraft"
        />
      </div>
    </section>

    <section class="operations-grid" data-screenshot="knowledge-base" aria-label="知识库与人工确认记录">
      <article class="ops-panel">
        <div class="ops-panel__heading">
          <p class="eyebrow">AI 建议记录</p>
          <h2>最近 AI 建议记录</h2>
        </div>
        <div class="ops-list">
          <div v-for="record in recentAiRecords" :key="record.id" class="ops-row">
            <div>
              <span>{{ record.id }} / {{ record.category }}</span>
              <strong>{{ record.title }}</strong>
            </div>
            <b>{{ record.confidence }}%</b>
          </div>
        </div>
      </article>

      <article class="ops-panel ops-panel--wide">
        <div class="ops-panel__heading">
          <p class="eyebrow">知识库命中</p>
          <h2>知识库命中与沉淀</h2>
        </div>
        <div v-if="knowledgeRows.length" class="knowledge-table">
          <div v-for="row in knowledgeRows" :key="row.id" class="knowledge-table__row">
            <span>{{ row.id }}</span>
            <strong>{{ row.title }}</strong>
            <small>{{ row.meta }}</small>
            <em>{{ row.state }}</em>
          </div>
        </div>
        <div v-else class="empty-state">
          <strong>暂无知识命中</strong>
          <span>处理完成后可以生成知识草稿，人工确认后再发布。</span>
        </div>
      </article>

      <article class="ops-panel">
        <div class="ops-panel__heading">
          <p class="eyebrow">人工确认</p>
          <h2>人工确认记录</h2>
        </div>
        <div v-if="humanRecords.length" class="approval-list">
          <div v-for="record in humanRecords" :key="`${record.time}-${record.actor}-${record.note}`" class="approval-row">
            <time>{{ record.time }}</time>
            <div>
              <strong>{{ record.actor }}</strong>
              <span>{{ record.note }}</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <strong>等待人工动作</strong>
          <span>AI 建议不会自动改变工单状态。</span>
        </div>
      </article>
    </section>

    <StatusTimeline :events="selectedTicket?.timeline ?? []" />
    </template>

    <section v-else class="evaluation-workspace" aria-labelledby="evaluation-title">
      <header class="evaluation-heading">
        <div>
          <p class="eyebrow">检索质量 / 离线评测</p>
          <h2 id="evaluation-title">知识检索评测</h2>
          <p>在固定的演示知识全语料上，用多相关 qrels 和无匹配问题检查召回、排序与误召回。</p>
        </div>
        <button class="primary-action" :disabled="isDemoRuntime || evaluationBusy" @click="runEvaluation">
          {{ evaluationBusy ? '正在运行…' : evaluationReport ? '重新运行评测' : '运行检索评测' }}
        </button>
      </header>

      <div class="evaluation-notice" :class="{ 'evaluation-notice--warning': isDemoRuntime }">
        <strong>{{ isDemoRuntime ? '当前为前端 Demo 模式' : '关键词基线 / 合成样例 / 不调用大模型' }}</strong>
        <span>{{ isDemoRuntime ? '切换到后端 API 模式并连接项目数据库后，可运行真实评测接口。' : 'V2 固定候选为仓库种子中的 14 篇已发布知识，包含 18 条正向问句、4 条无匹配负例；只评估关键词基线，不代表生产数据或大模型效果。' }}</span>
      </div>

      <p v-if="evaluationError" class="evaluation-error" role="alert">运行失败：{{ evaluationError }}</p>

      <template v-if="evaluationReport">
        <div class="evaluation-metrics" aria-label="检索评测指标">
          <article class="evaluation-metric">
            <span>Hit Rate@1</span>
            <strong>{{ formatPercent(evaluationReport.metrics.hitRateAt1) }}</strong>
            <small>{{ evaluationReport.metrics.hitAt1 }} / {{ evaluationReport.metrics.positiveQueryCount }} 条正向问句首位命中</small>
          </article>
          <article class="evaluation-metric">
            <span>Hit Rate@3</span>
            <strong>{{ formatPercent(evaluationReport.metrics.hitRateAt3) }}</strong>
            <small>{{ evaluationReport.metrics.hitAt3 }} / {{ evaluationReport.metrics.positiveQueryCount }} 条正向问句前三命中</small>
          </article>
          <article class="evaluation-metric">
            <span>Macro Recall@3</span>
            <strong>{{ formatPercent(evaluationReport.metrics.recallAt3) }}</strong>
            <small>按每条正向问句的全部相关知识计算后取平均</small>
          </article>
          <article class="evaluation-metric">
            <span>Precision@3</span>
            <strong>{{ formatPercent(evaluationReport.metrics.precisionAt3) }}</strong>
            <small>正向问句前三候选中相关知识所占比例</small>
          </article>
          <article class="evaluation-metric">
            <span>MRR</span>
            <strong>{{ evaluationReport.metrics.meanReciprocalRank.toFixed(3) }}</strong>
            <small>按首次相关知识排名倒数取平均</small>
          </article>
          <article class="evaluation-metric">
            <span>无匹配特异度</span>
            <strong>{{ formatPercent(evaluationReport.metrics.noMatchSpecificity) }}</strong>
            <small>{{ evaluationReport.metrics.noMatchTrueNegativeCount }} / {{ evaluationReport.metrics.noMatchQueryCount }} 条负例正确保持无召回</small>
          </article>
          <article class="evaluation-metric evaluation-metric--meta">
            <span>评测设置</span>
            <strong>Top {{ evaluationReport.topK }}</strong>
            <small>{{ evaluationReport.corpusArticleCount }} 篇候选 · {{ evaluationReport.corpusVersion }} · {{ new Date(evaluationReport.generatedAt).toLocaleString('zh-CN') }}</small>
          </article>
        </div>

        <div class="evaluation-results">
          <div class="evaluation-results__heading">
            <div>
              <h3>逐条检索结果</h3>
              <p>同时展示多相关标注排名、部分召回、未召回和无匹配问题误召回。</p>
            </div>
            <span>{{ evaluationReport.datasetVersion }} · {{ evaluationReport.retrievalMode }}</span>
          </div>
          <div class="evaluation-table-wrap">
            <table class="evaluation-table">
              <thead>
                <tr>
                  <th>用例 / 查询</th>
                  <th>标注目标</th>
                  <th>目标排名</th>
                  <th>检索候选</th>
                  <th>诊断</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="testCase in evaluationReport.cases" :key="testCase.id">
                  <td>
                    <span class="evaluation-case-id">{{ testCase.id }} · {{ evaluationQueryTypeText(testCase.queryType) }}</span>
                    <strong>{{ testCase.query }}</strong>
                  </td>
                  <td>
                    <div v-if="testCase.relevantArticleNos.length" class="evaluation-qrels">
                      <code v-for="articleNo in testCase.relevantArticleNos" :key="articleNo">{{ articleNo }}</code>
                    </div>
                    <span v-else class="evaluation-empty">无匹配知识</span>
                    <small v-if="testCase.annotationRationale" class="evaluation-rationale">{{ testCase.annotationRationale }}</small>
                  </td>
                  <td><span class="evaluation-rank" :class="{ 'evaluation-rank--miss': testCase.relevantArticleNos.length === 0 ? !testCase.correctNoMatch : testCase.firstRelevantRank === 0 }">{{ evaluationOutcome(testCase) }}</span></td>
                  <td>
                    <ol v-if="testCase.retrieved.length" class="evaluation-candidates">
                      <li v-for="article in testCase.retrieved" :key="article.articleNo">
                        <span>{{ article.articleNo }} · {{ article.title }}</span>
                        <b>{{ article.relevance }}</b>
                      </li>
                    </ol>
                    <span v-else class="evaluation-empty">无候选结果</span>
                  </td>
                  <td><span class="evaluation-diagnosis" :class="{ 'evaluation-diagnosis--miss': testCase.failureType !== 'NONE' }">{{ evaluationFailureText(testCase) }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>
      <div v-else class="evaluation-empty-state">
        <strong>运行评测后查看真实结果</strong>
        <span>第一次打开不会预填指标。点击上方按钮后，页面会展示后端计算出的指标和每条查询的命中排名。</span>
      </div>
    </section>
  </div>
</template>
