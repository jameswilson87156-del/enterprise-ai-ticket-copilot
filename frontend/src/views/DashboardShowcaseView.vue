<script setup lang="ts">
import {
  baselineRows,
  boundaryStatements,
  dashboardMetrics,
  evaluationMetricCards,
  evaluationSnapshot,
  failureAnalysis,
  providerStatusItems,
  recentTicketRuns,
  topKnowledgeSources
} from '../data/evaluationMetrics'

interface WorkflowStep {
  id: string
  title: string
  note: string
  state: string
}

interface ReviewQueueItem {
  id: string
  title: string
  reason: string
  tone: 'amber' | 'red' | 'violet'
}

interface ActivityItem {
  time: string
  title: string
  detail: string
  tone: 'cyan' | 'green' | 'amber'
}

const snapshotMetrics = evaluationMetricCards.slice(1, 8)

const workflowSteps: WorkflowStep[] = [
  { id: '01', title: 'Ticket Input', note: '工单标题、日志、业务上下文进入本地 demo 流程。', state: 'demo input' },
  { id: '02', title: 'Keyword Retrieval', note: `Top-${evaluationSnapshot.topK} 本地关键词检索，不是向量检索。`, state: 'local' },
  { id: '03', title: 'Citation Gating', note: '模拟答案必须携带可解释 citation IDs。', state: 'evidence' },
  { id: '04', title: 'Human Review', note: '高风险、缺证据或引用异常样本进入人工复核。', state: 'review' }
]

const reviewQueue: ReviewQueueItem[] = [
  {
    id: 'EVAL-016',
    title: '登录页返回 500，容易误判为账号问题',
    reason: 'citation_contains_unexpected_source',
    tone: 'red'
  },
  {
    id: 'EVAL-015',
    title: '会议室设备问题当前知识库无可靠来源',
    reason: 'unexpected_retrieval_for_fallback_case',
    tone: 'amber'
  },
  {
    id: 'EVAL-011',
    title: '部署环境变量缺失，引用来源需要收敛',
    reason: 'citation_contains_unexpected_source',
    tone: 'violet'
  }
]

const activityItems: ActivityItem[] = [
  {
    time: '12:25',
    title: 'Local evaluation snapshot generated',
    detail: '16 synthetic demo cases, keyword retrieval + citation gating.',
    tone: 'green'
  },
  {
    time: '12:25',
    title: 'Provider fallback expected',
    detail: 'No real API key configured; provider fallback rate is a boundary signal.',
    tone: 'amber'
  },
  {
    time: '12:24',
    title: 'Human Review gate applied',
    detail: '15 cases require review due to risk, evidence, or policy boundaries.',
    tone: 'cyan'
  }
]
</script>

<template>
  <section class="dashboard-phase" data-screenshot="dashboard" aria-label="Enterprise Ticket RAG Copilot dashboard">
    <header class="dashboard-phase__hero">
      <div class="dashboard-phase__hero-copy">
        <p class="dashboard-phase__eyebrow">Enterprise audit cockpit / Phase 1</p>
        <h1>Enterprise Ticket RAG Copilot</h1>
        <p>
          企业工单知识库智能助手 · RAG Evidence · Trace · Human Review · Evaluation Metrics。
          基于 synthetic demo dataset、local keyword retrieval、citation gating 与 local-rule fallback 的作品集演示。
        </p>
        <div class="dashboard-phase__hero-tags" aria-label="Dashboard boundary tags">
          <span>synthetic demo dataset</span>
          <span>keyword retrieval</span>
          <span>citation gating</span>
          <span>local-rule fallback</span>
        </div>
      </div>

      <aside class="dashboard-phase__hero-status" aria-label="Local evaluation headline">
        <section>
          <span>Eval Dataset</span>
          <strong>{{ evaluationSnapshot.sampleCount }} cases</strong>
          <small>synthetic enterprise ticket cases</small>
        </section>
        <section>
          <span>Top-K</span>
          <strong>{{ evaluationSnapshot.topK }}</strong>
          <small>{{ evaluationSnapshot.retrievalMethod }}</small>
        </section>
        <section>
          <span>Provider Path</span>
          <strong>local-rule fallback</strong>
          <small>real provider not configured / not claimed</small>
        </section>
      </aside>
    </header>

    <section class="dashboard-phase__kpis" aria-label="Dashboard KPIs">
      <article v-for="metric in dashboardMetrics" :key="metric.label" class="dashboard-phase__kpi" :data-tone="metric.tone">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <p>{{ metric.note }}</p>
        <small>{{ metric.source }}</small>
      </article>
    </section>

    <section class="dashboard-phase__primary-grid" aria-label="Dashboard primary work area">
      <article class="dashboard-phase__panel dashboard-phase__panel--runs" aria-label="Recent ticket runs">
        <div class="dashboard-phase__panel-heading">
          <div>
            <p class="dashboard-phase__eyebrow">Recent Ticket Runs</p>
            <h2>工单运行记录</h2>
          </div>
          <span>local eval output</span>
        </div>
        <div class="dashboard-phase__run-table" role="table" aria-label="Recent ticket runs table">
          <div class="dashboard-phase__run-head" role="row">
            <span>Ticket</span>
            <span>Category</span>
            <span>Retrieval</span>
            <span>Citation</span>
            <span>Review</span>
            <span>Provider</span>
          </div>
          <article v-for="run in recentTicketRuns" :key="run.caseId" class="dashboard-phase__run-row" role="row">
            <div>
              <strong>{{ run.ticketId }}</strong>
              <small>{{ run.caseId }} · {{ run.priority }} · {{ run.latency }}</small>
            </div>
            <span>{{ run.category }}</span>
            <span>{{ run.retrievalStatus }}</span>
            <span>{{ run.citationStatus }}</span>
            <span>{{ run.reviewStatus }}</span>
            <span>{{ run.providerPath }}</span>
          </article>
        </div>
      </article>

      <article class="dashboard-phase__panel dashboard-phase__panel--eval" aria-label="Evaluation snapshot">
        <div class="dashboard-phase__panel-heading">
          <div>
            <p class="dashboard-phase__eyebrow">Evaluation Snapshot</p>
            <h2>本地评测快照</h2>
          </div>
          <span>{{ evaluationSnapshot.primaryBaseline }}</span>
        </div>
        <div class="dashboard-phase__snapshot-grid">
          <section v-for="metric in snapshotMetrics" :key="metric.label" :data-tone="metric.tone">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
            <small>{{ metric.note }}</small>
          </section>
        </div>
        <div class="dashboard-phase__baseline-strip" aria-label="Baseline comparison summary">
          <article v-for="row in baselineRows.slice(0, 4)" :key="row.name">
            <span>{{ row.name }}</span>
            <strong>{{ row.citationPrecision }}</strong>
            <small>citation precision / failed {{ row.failedCases }}</small>
          </article>
        </div>
      </article>
    </section>

    <section class="dashboard-phase__secondary-grid" aria-label="Dashboard secondary context">
      <article class="dashboard-phase__panel" aria-label="Provider fallback status">
        <div class="dashboard-phase__panel-heading">
          <div>
            <p class="dashboard-phase__eyebrow">Provider / Fallback</p>
            <h2>Provider 状态</h2>
          </div>
        </div>
        <div class="dashboard-phase__provider-list">
          <section v-for="item in providerStatusItems" :key="item.label" :data-tone="item.tone">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
            <p>{{ item.note }}</p>
          </section>
        </div>
      </article>

      <article class="dashboard-phase__panel" aria-label="Top knowledge sources">
        <div class="dashboard-phase__panel-heading">
          <div>
            <p class="dashboard-phase__eyebrow">Top Knowledge Sources</p>
            <h2>知识来源</h2>
          </div>
        </div>
        <div class="dashboard-phase__source-list">
          <section v-for="source in topKnowledgeSources" :key="source.id">
            <div>
              <strong>{{ source.id }}</strong>
              <span>{{ source.name }}</span>
            </div>
            <small>{{ source.matchedTickets }}</small>
            <small>{{ source.citationUsage }}</small>
            <small>{{ source.keywordStatus }}</small>
            <em>{{ source.sourceType }}</em>
          </section>
        </div>
      </article>

      <article class="dashboard-phase__panel" aria-label="Human review queue">
        <div class="dashboard-phase__panel-heading">
          <div>
            <p class="dashboard-phase__eyebrow">Human Review Queue</p>
            <h2>复核压力</h2>
          </div>
          <span>{{ evaluationSnapshot.humanReviewRequiredCount }}</span>
        </div>
        <div class="dashboard-phase__review-list">
          <section v-for="item in reviewQueue" :key="item.id" :data-tone="item.tone">
            <strong>{{ item.id }}</strong>
            <span>{{ item.title }}</span>
            <small>{{ item.reason }}</small>
          </section>
        </div>
      </article>

      <article class="dashboard-phase__panel" aria-label="Latest trace and audit activity">
        <div class="dashboard-phase__panel-heading">
          <div>
            <p class="dashboard-phase__eyebrow">Trace / Audit Activity</p>
            <h2>最新审计轨迹</h2>
          </div>
        </div>
        <ol class="dashboard-phase__timeline">
          <li v-for="item in activityItems" :key="`${item.time}-${item.title}`" :data-tone="item.tone">
            <time>{{ item.time }}</time>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.detail }}</p>
            </div>
          </li>
        </ol>
      </article>
    </section>

    <section class="dashboard-phase__workflow" aria-label="RAG trace evaluation workflow">
      <article v-for="step in workflowSteps" :key="step.id">
        <span>{{ step.id }}</span>
        <strong>{{ step.title }}</strong>
        <p>{{ step.note }}</p>
        <small>{{ step.state }}</small>
      </article>
    </section>

    <aside class="dashboard-phase__boundary" aria-label="Portfolio metrics boundary">
      <strong>Demo metrics are generated from synthetic evaluation cases and local scripts. They are used for portfolio verification, not production claims.</strong>
      <div>
        <span v-for="item in boundaryStatements" :key="item">{{ item }}</span>
      </div>
    </aside>

    <section class="dashboard-phase__failure-bar" aria-label="Failure analysis summary">
      <article v-for="item in failureAnalysis" :key="item.label" :data-tone="item.tone">
        <span>{{ item.label }}</span>
        <strong>{{ item.count }}</strong>
        <small>{{ item.note }}</small>
      </article>
    </section>
  </section>
</template>

<style scoped>
.dashboard-phase {
  --dash-panel: rgba(12, 23, 38, 0.9);
  --dash-panel-strong: rgba(16, 31, 51, 0.94);
  --dash-border: rgba(151, 180, 214, 0.15);
  --dash-border-strong: rgba(91, 141, 239, 0.26);
  --dash-text: #eef5ff;
  --dash-secondary: #c7d5ea;
  --dash-muted: #7e91ab;
  --dash-blue: #3d7cff;
  --dash-cyan: #21c7d9;
  --dash-green: #2bd88f;
  --dash-amber: #ffb45c;
  --dash-red: #ff5c7a;
  --dash-violet: #8b7cf6;
  display: grid;
  gap: 10px;
  min-width: 0;
  color: var(--dash-text);
}

.dashboard-phase h1,
.dashboard-phase h2,
.dashboard-phase p {
  margin: 0;
  letter-spacing: 0;
}

.dashboard-phase__hero,
.dashboard-phase__panel,
.dashboard-phase__workflow,
.dashboard-phase__boundary,
.dashboard-phase__failure-bar {
  border: 1px solid var(--dash-border);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(16, 31, 51, 0.9), rgba(8, 17, 31, 0.94)),
    var(--dash-panel);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.2);
}

.dashboard-phase__hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(330px, 0.48fr);
  gap: 14px;
  min-height: 178px;
  border-color: var(--dash-border-strong);
  padding: 17px;
  background:
    linear-gradient(135deg, rgba(16, 31, 51, 0.96), rgba(8, 17, 31, 0.9)),
    repeating-linear-gradient(90deg, rgba(151, 180, 214, 0.04) 0 1px, transparent 1px 48px);
}

.dashboard-phase__eyebrow {
  margin: 0 0 6px;
  color: #9fc4ff;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
}

.dashboard-phase__hero h1 {
  color: var(--dash-text);
  font-size: 34px;
  line-height: 1.05;
}

.dashboard-phase__hero-copy > p:not(.dashboard-phase__eyebrow) {
  max-width: 760px;
  margin-top: 9px;
  color: var(--dash-secondary);
  font-size: 14px;
  line-height: 1.55;
}

.dashboard-phase__hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 12px;
}

.dashboard-phase__hero-tags span,
.dashboard-phase__panel-heading > span,
.dashboard-phase__boundary span {
  display: inline-flex;
  align-items: center;
  min-height: 25px;
  border: 1px solid rgba(33, 199, 217, 0.16);
  border-radius: 7px;
  padding: 4px 8px;
  color: #9fe8f1;
  background: rgba(33, 199, 217, 0.06);
  font-size: 11px;
  font-weight: 850;
}

.dashboard-phase__hero-status {
  display: grid;
  gap: 8px;
}

.dashboard-phase__hero-status section {
  display: grid;
  gap: 4px;
  border: 1px solid rgba(151, 180, 214, 0.12);
  border-left: 3px solid var(--dash-cyan);
  border-radius: 8px;
  padding: 9px 10px;
  background: rgba(4, 9, 18, 0.42);
}

.dashboard-phase__hero-status span,
.dashboard-phase__hero-status small {
  color: var(--dash-muted);
  font-size: 11px;
  line-height: 1.35;
}

.dashboard-phase__hero-status strong {
  color: var(--dash-text);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 14px;
}

.dashboard-phase__kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.dashboard-phase__kpi {
  display: grid;
  gap: 5px;
  min-height: 88px;
  border: 1px solid var(--dash-border);
  border-top: 3px solid var(--dash-blue);
  border-radius: 8px;
  padding: 10px 11px;
  background: rgba(12, 23, 38, 0.82);
}

.dashboard-phase__kpi[data-tone='green'],
.dashboard-phase__provider-list section[data-tone='green'],
.dashboard-phase__failure-bar article[data-tone='green'] {
  border-top-color: var(--dash-green);
}

.dashboard-phase__kpi[data-tone='cyan'] {
  border-top-color: var(--dash-cyan);
}

.dashboard-phase__kpi[data-tone='amber'] {
  border-top-color: var(--dash-amber);
}

.dashboard-phase__kpi[data-tone='red'] {
  border-top-color: var(--dash-red);
}

.dashboard-phase__kpi[data-tone='violet'] {
  border-top-color: var(--dash-violet);
}

.dashboard-phase__kpi[data-tone='slate'] {
  border-top-color: #5b6f8d;
}

.dashboard-phase__kpi span,
.dashboard-phase__kpi p,
.dashboard-phase__kpi small {
  color: var(--dash-muted);
  font-size: 11px;
  line-height: 1.3;
}

.dashboard-phase__kpi span {
  font-weight: 900;
}

.dashboard-phase__kpi strong {
  color: var(--dash-text);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 24px;
  line-height: 1;
}

.dashboard-phase__primary-grid,
.dashboard-phase__secondary-grid {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.dashboard-phase__primary-grid {
  grid-template-columns: minmax(0, 1.04fr) minmax(330px, 0.96fr);
}

.dashboard-phase__secondary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.dashboard-phase__panel {
  min-width: 0;
  overflow: hidden;
}

.dashboard-phase__panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-height: 52px;
  border-bottom: 1px solid var(--dash-border);
  padding: 10px 12px;
}

.dashboard-phase__panel-heading h2 {
  color: var(--dash-text);
  font-size: 16px;
  line-height: 1.2;
}

.dashboard-phase__run-table {
  display: grid;
  gap: 5px;
  padding: 9px 10px 11px;
}

.dashboard-phase__run-head,
.dashboard-phase__run-row {
  display: grid;
  grid-template-columns: minmax(120px, 1.1fr) 0.76fr 0.82fr 1fr 0.82fr 1fr;
  gap: 7px;
  align-items: center;
}

.dashboard-phase__run-head {
  color: var(--dash-muted);
  font-size: 10.5px;
  font-weight: 900;
}

.dashboard-phase__run-row {
  min-height: 48px;
  border: 1px solid rgba(151, 180, 214, 0.09);
  border-radius: 8px;
  padding: 7px;
  background: rgba(4, 9, 18, 0.32);
}

.dashboard-phase__run-row strong,
.dashboard-phase__run-row span {
  display: block;
  min-width: 0;
  overflow: hidden;
  color: var(--dash-secondary);
  font-size: 11px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dashboard-phase__run-row strong {
  color: #9fc4ff;
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 12px;
}

.dashboard-phase__run-row small {
  display: block;
  margin-top: 3px;
  color: var(--dash-muted);
  font-size: 10px;
  line-height: 1.25;
}

.dashboard-phase__snapshot-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
  padding: 10px;
}

.dashboard-phase__snapshot-grid section {
  display: grid;
  gap: 4px;
  border: 1px solid rgba(151, 180, 214, 0.1);
  border-left: 3px solid var(--dash-blue);
  border-radius: 8px;
  padding: 8px;
  background: rgba(4, 9, 18, 0.34);
}

.dashboard-phase__snapshot-grid section[data-tone='green'] {
  border-left-color: var(--dash-green);
}

.dashboard-phase__snapshot-grid section[data-tone='cyan'] {
  border-left-color: var(--dash-cyan);
}

.dashboard-phase__snapshot-grid section[data-tone='amber'] {
  border-left-color: var(--dash-amber);
}

.dashboard-phase__snapshot-grid section[data-tone='red'] {
  border-left-color: var(--dash-red);
}

.dashboard-phase__snapshot-grid section[data-tone='violet'] {
  border-left-color: var(--dash-violet);
}

.dashboard-phase__snapshot-grid span,
.dashboard-phase__snapshot-grid small {
  color: var(--dash-muted);
  font-size: 10.5px;
  line-height: 1.3;
}

.dashboard-phase__snapshot-grid strong {
  color: var(--dash-text);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 17px;
}

.dashboard-phase__baseline-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  padding: 0 10px 10px;
}

.dashboard-phase__baseline-strip article {
  border: 1px solid rgba(139, 124, 246, 0.13);
  border-radius: 8px;
  padding: 7px;
  background: rgba(139, 124, 246, 0.05);
}

.dashboard-phase__baseline-strip span,
.dashboard-phase__baseline-strip small {
  display: block;
  color: var(--dash-muted);
  font-size: 10px;
  line-height: 1.35;
}

.dashboard-phase__baseline-strip strong {
  display: block;
  margin: 3px 0;
  color: #d8d2ff;
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 13px;
}

.dashboard-phase__provider-list,
.dashboard-phase__source-list,
.dashboard-phase__review-list {
  display: grid;
  gap: 7px;
  padding: 10px;
}

.dashboard-phase__provider-list section,
.dashboard-phase__review-list section,
.dashboard-phase__source-list section {
  min-width: 0;
  border: 1px solid rgba(151, 180, 214, 0.1);
  border-left: 3px solid var(--dash-blue);
  border-radius: 8px;
  padding: 8px;
  background: rgba(4, 9, 18, 0.32);
}

.dashboard-phase__provider-list section[data-tone='amber'],
.dashboard-phase__review-list section[data-tone='amber'],
.dashboard-phase__failure-bar article[data-tone='amber'] {
  border-left-color: var(--dash-amber);
}

.dashboard-phase__provider-list section[data-tone='cyan'] {
  border-left-color: var(--dash-cyan);
}

.dashboard-phase__provider-list section[data-tone='green'] {
  border-left-color: var(--dash-green);
}

.dashboard-phase__review-list section[data-tone='red'],
.dashboard-phase__failure-bar article[data-tone='red'] {
  border-left-color: var(--dash-red);
}

.dashboard-phase__review-list section[data-tone='violet'],
.dashboard-phase__failure-bar article[data-tone='violet'] {
  border-left-color: var(--dash-violet);
}

.dashboard-phase__provider-list strong,
.dashboard-phase__review-list strong,
.dashboard-phase__source-list strong {
  display: block;
  color: var(--dash-text);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 12px;
}

.dashboard-phase__provider-list span,
.dashboard-phase__provider-list p,
.dashboard-phase__review-list span,
.dashboard-phase__review-list small,
.dashboard-phase__source-list span,
.dashboard-phase__source-list small,
.dashboard-phase__source-list em {
  display: block;
  margin-top: 4px;
  color: var(--dash-muted);
  font-size: 11px;
  line-height: 1.35;
  font-style: normal;
}

.dashboard-phase__source-list section {
  display: grid;
  grid-template-columns: minmax(0, 1fr) repeat(4, auto);
  gap: 7px;
  align-items: center;
}

.dashboard-phase__source-list small,
.dashboard-phase__source-list em {
  margin: 0;
  white-space: nowrap;
}

.dashboard-phase__source-list em {
  color: #d8d2ff;
}

.dashboard-phase__timeline {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 10px 11px 12px;
  list-style: none;
}

.dashboard-phase__timeline li {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 9px;
  border-left: 2px solid var(--dash-cyan);
  padding-left: 10px;
}

.dashboard-phase__timeline li[data-tone='green'] {
  border-left-color: var(--dash-green);
}

.dashboard-phase__timeline li[data-tone='amber'] {
  border-left-color: var(--dash-amber);
}

.dashboard-phase__timeline time {
  color: var(--dash-cyan);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  font-weight: 900;
}

.dashboard-phase__timeline strong {
  color: var(--dash-text);
  font-size: 12px;
}

.dashboard-phase__timeline p {
  margin-top: 3px;
  color: var(--dash-muted);
  font-size: 11px;
  line-height: 1.4;
}

.dashboard-phase__workflow {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
}

.dashboard-phase__workflow article {
  position: relative;
  display: grid;
  gap: 5px;
  min-height: 95px;
  border: 1px solid rgba(33, 199, 217, 0.14);
  border-radius: 8px;
  padding: 9px;
  background: rgba(4, 9, 18, 0.34);
}

.dashboard-phase__workflow article:not(:last-child)::after {
  position: absolute;
  top: 25px;
  right: -10px;
  width: 10px;
  height: 2px;
  content: "";
  background: rgba(33, 199, 217, 0.5);
}

.dashboard-phase__workflow span,
.dashboard-phase__workflow small {
  color: var(--dash-muted);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  font-weight: 900;
}

.dashboard-phase__workflow strong {
  color: var(--dash-text);
  font-size: 13px;
}

.dashboard-phase__workflow p {
  color: var(--dash-secondary);
  font-size: 11px;
  line-height: 1.4;
}

.dashboard-phase__boundary {
  display: grid;
  gap: 8px;
  border-color: rgba(255, 180, 92, 0.22);
  padding: 11px 12px;
  background: linear-gradient(135deg, rgba(255, 180, 92, 0.1), rgba(12, 23, 38, 0.9));
}

.dashboard-phase__boundary strong {
  color: #ffe0b7;
  font-size: 13px;
  line-height: 1.45;
}

.dashboard-phase__boundary div {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.dashboard-phase__failure-bar {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
  box-shadow: none;
}

.dashboard-phase__failure-bar article {
  display: grid;
  gap: 4px;
  border: 1px solid rgba(151, 180, 214, 0.1);
  border-left: 3px solid var(--dash-blue);
  border-radius: 8px;
  padding: 8px;
  background: rgba(4, 9, 18, 0.34);
}

.dashboard-phase__failure-bar article[data-tone='cyan'] {
  border-left-color: var(--dash-cyan);
}

.dashboard-phase__failure-bar span,
.dashboard-phase__failure-bar small {
  color: var(--dash-muted);
  font-size: 11px;
  line-height: 1.3;
}

.dashboard-phase__failure-bar strong {
  color: var(--dash-text);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 18px;
}

@media (max-width: 1180px) {
  .dashboard-phase__hero,
  .dashboard-phase__primary-grid,
  .dashboard-phase__secondary-grid,
  .dashboard-phase__workflow,
  .dashboard-phase__failure-bar {
    grid-template-columns: 1fr;
  }

  .dashboard-phase__kpis,
  .dashboard-phase__snapshot-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-phase__workflow article:not(:last-child)::after {
    display: none;
  }
}

@media (max-width: 700px) {
  .dashboard-phase__hero h1 {
    font-size: 28px;
  }

  .dashboard-phase__kpis,
  .dashboard-phase__snapshot-grid,
  .dashboard-phase__baseline-strip,
  .dashboard-phase__run-head,
  .dashboard-phase__run-row,
  .dashboard-phase__source-list section {
    grid-template-columns: 1fr;
  }

  .dashboard-phase__run-head {
    display: none;
  }

  .dashboard-phase__run-row span {
    white-space: normal;
  }
}
</style>
