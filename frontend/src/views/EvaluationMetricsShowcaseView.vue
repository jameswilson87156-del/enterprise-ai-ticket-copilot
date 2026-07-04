<script setup lang="ts">
import {
  baselineRows,
  boundaryStatements,
  currentScopeItems,
  evaluationCases,
  evaluationMetricCards,
  evaluationSnapshot,
  failureAnalysis,
  nextStageItems,
  reproducibilityCommands,
  reproducibilityReports
} from '../data/evaluationMetrics'

const primaryMetricCards = evaluationMetricCards.slice(0, 8)
const rankMetricCards = evaluationMetricCards.slice(8)
</script>

<template>
  <section class="evaluation-showcase" data-screenshot="evaluation-metrics" aria-label="Evaluation Metrics Showcase">
    <header class="evaluation-showcase__hero">
      <div>
        <p class="evaluation-showcase__eyebrow">Evaluation / Metrics</p>
        <h1>Evaluation / Metrics</h1>
        <p>
          基于 {{ evaluationSnapshot.sampleCount }} 条自建企业工单 synthetic demo 评测集的本地评测结果。
          当前只覆盖 local keyword retrieval、citation gating、Human Review gate 与 local-rule fallback。
        </p>
        <div class="evaluation-showcase__hero-tags">
          <span>{{ evaluationSnapshot.datasetType }}</span>
          <span>Top-K: {{ evaluationSnapshot.topK }}</span>
          <span>{{ evaluationSnapshot.providerMode }}</span>
        </div>
      </div>

      <aside class="evaluation-showcase__dataset-card" aria-label="Dataset summary">
        <span>Dataset</span>
        <strong>{{ evaluationSnapshot.datasetPath }}</strong>
        <p>Generated at {{ evaluationSnapshot.generatedAt }}. Contains real user data: {{ evaluationSnapshot.containsRealUserData ? 'yes' : 'no' }}.</p>
      </aside>
    </header>

    <section class="evaluation-showcase__metrics" aria-label="Metrics overview">
      <article v-for="metric in primaryMetricCards" :key="metric.label" :data-tone="metric.tone">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <p>{{ metric.note }}</p>
        <small>{{ metric.source }}</small>
      </article>
    </section>

    <section class="evaluation-showcase__rank-row" aria-label="Optional rank metrics">
      <article v-for="metric in rankMetricCards" :key="metric.label" :data-tone="metric.tone">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.note }}</small>
      </article>
      <article>
        <span>Provider Fallback Rate</span>
        <strong>{{ evaluationSnapshot.providerFallbackRate }}</strong>
        <small>Expected in no-key local run; not a model quality metric.</small>
      </article>
      <article>
        <span>Knowledge Miss Fallback</span>
        <strong>{{ evaluationSnapshot.knowledgeMissFallbackRate }}</strong>
        <small>No expected source or no reliable retrieved source.</small>
      </article>
    </section>

    <section class="evaluation-showcase__grid evaluation-showcase__grid--scope" aria-label="Baseline and scope">
      <article class="evaluation-showcase__panel">
        <div class="evaluation-showcase__panel-heading">
          <div>
            <p class="evaluation-showcase__eyebrow">Baseline / Scope</p>
            <h2>当前版本 baseline</h2>
          </div>
          <span>{{ evaluationSnapshot.primaryBaseline }}</span>
        </div>
        <div class="evaluation-showcase__scope-list">
          <span v-for="item in currentScopeItems" :key="item">{{ item }}</span>
        </div>
        <div class="evaluation-showcase__baseline-table" role="table" aria-label="Baseline comparison">
          <div class="evaluation-showcase__baseline-head" role="row">
            <span>Baseline</span>
            <span>Top-K Hit</span>
            <span>Citation Coverage</span>
            <span>Citation Precision</span>
            <span>Failed</span>
            <span>Review</span>
          </div>
          <article v-for="row in baselineRows" :key="row.name" class="evaluation-showcase__baseline-row" role="row">
            <div>
              <strong>{{ row.name }}</strong>
              <small>{{ row.scope }}</small>
            </div>
            <span>{{ row.topKHitRate }}</span>
            <span>{{ row.citationCoverage }}</span>
            <span>{{ row.citationPrecision }}</span>
            <span>{{ row.failedCases }}</span>
            <span>{{ row.humanReviewRequired }}</span>
          </article>
        </div>
      </article>

      <article class="evaluation-showcase__panel">
        <div class="evaluation-showcase__panel-heading">
          <div>
            <p class="evaluation-showcase__eyebrow">Next-stage</p>
            <h2>下一阶段真实模型评测</h2>
          </div>
          <span>not claimed</span>
        </div>
        <div class="evaluation-showcase__next-list">
          <span v-for="item in nextStageItems" :key="item">{{ item }}</span>
        </div>
        <p class="evaluation-showcase__plain-note">
          当前版本不包含 BM25、embedding、Vector DB、Hybrid、Rerank 或真实 Provider 小规模实验。
          Answer Relevance、Faithfulness、Token Cost 等指标需要真实模型输入输出和人工标注后再写。
        </p>
      </article>
    </section>

    <section class="evaluation-showcase__grid evaluation-showcase__grid--cases" aria-label="Evaluation cases and failure analysis">
      <article class="evaluation-showcase__panel evaluation-showcase__panel--cases">
        <div class="evaluation-showcase__panel-heading">
          <div>
            <p class="evaluation-showcase__eyebrow">Evaluation Cases</p>
            <h2>样本运行结果</h2>
          </div>
          <span>{{ evaluationCases.length }} shown / {{ evaluationSnapshot.sampleCount }} total</span>
        </div>
        <div class="evaluation-showcase__case-table" role="table" aria-label="Evaluation case table">
          <div class="evaluation-showcase__case-head" role="row">
            <span>Case</span>
            <span>Category</span>
            <span>Expected Knowledge</span>
            <span>Hit / Miss</span>
            <span>Citation</span>
            <span>Review</span>
          </div>
          <article v-for="item in evaluationCases" :key="item.id" class="evaluation-showcase__case-row" :data-status="item.topKStatus" role="row">
            <div>
              <strong>{{ item.id }}</strong>
              <small>{{ item.priority }}</small>
            </div>
            <span>{{ item.category }}</span>
            <span>{{ item.expectedKnowledgeIds.length ? item.expectedKnowledgeIds.join(', ') : 'fallback expected' }}</span>
            <span>{{ item.topKStatus }}</span>
            <span>{{ item.citationStatus }}</span>
            <span>{{ item.reviewRequired ? 'required' : 'not required' }}</span>
            <small class="evaluation-showcase__case-reason">{{ item.failureReason }}</small>
          </article>
        </div>
      </article>

      <aside class="evaluation-showcase__panel evaluation-showcase__panel--failure" aria-label="Failure analysis">
        <div class="evaluation-showcase__panel-heading">
          <div>
            <p class="evaluation-showcase__eyebrow">Failure Analysis</p>
            <h2>失败类型分布</h2>
          </div>
          <span>failed {{ evaluationSnapshot.failedCaseCount }}</span>
        </div>
        <div class="evaluation-showcase__failure-list">
          <section v-for="item in failureAnalysis" :key="item.label" :data-tone="item.tone">
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
            <p>{{ item.note }}</p>
          </section>
        </div>
      </aside>
    </section>

    <section class="evaluation-showcase__grid evaluation-showcase__grid--bottom" aria-label="Reproducibility and boundary">
      <article class="evaluation-showcase__panel">
        <div class="evaluation-showcase__panel-heading">
          <div>
            <p class="evaluation-showcase__eyebrow">Reproducibility</p>
            <h2>本地复现命令与报告路径</h2>
          </div>
          <span>local only</span>
        </div>
        <div class="evaluation-showcase__commands">
          <code v-for="command in reproducibilityCommands" :key="command">{{ command }}</code>
        </div>
        <div class="evaluation-showcase__reports">
          <span v-for="report in reproducibilityReports" :key="report">{{ report }}</span>
        </div>
      </article>

      <article class="evaluation-showcase__panel evaluation-showcase__panel--boundary">
        <div class="evaluation-showcase__panel-heading">
          <div>
            <p class="evaluation-showcase__eyebrow">Boundary</p>
            <h2>不能夸大的能力边界</h2>
          </div>
          <span>portfolio-safe</span>
        </div>
        <div class="evaluation-showcase__boundary-list">
          <span v-for="item in boundaryStatements" :key="item">{{ item }}</span>
        </div>
        <p class="evaluation-showcase__plain-note">
          Demo metrics are generated from synthetic evaluation cases and local scripts.
          They are used for portfolio verification, not production claims.
        </p>
      </article>
    </section>
  </section>
</template>

<style scoped>
.evaluation-showcase {
  --eval-panel: rgba(12, 23, 38, 0.9);
  --eval-border: rgba(151, 180, 214, 0.15);
  --eval-border-strong: rgba(91, 141, 239, 0.26);
  --eval-text: #eef5ff;
  --eval-secondary: #c7d5ea;
  --eval-muted: #7e91ab;
  --eval-blue: #3d7cff;
  --eval-cyan: #21c7d9;
  --eval-green: #2bd88f;
  --eval-amber: #ffb45c;
  --eval-red: #ff5c7a;
  --eval-violet: #8b7cf6;
  display: grid;
  gap: 10px;
  min-width: 0;
  color: var(--eval-text);
}

.evaluation-showcase h1,
.evaluation-showcase h2,
.evaluation-showcase p {
  margin: 0;
  letter-spacing: 0;
}

.evaluation-showcase__hero,
.evaluation-showcase__panel,
.evaluation-showcase__metrics article,
.evaluation-showcase__rank-row article {
  border: 1px solid var(--eval-border);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(16, 31, 51, 0.9), rgba(8, 17, 31, 0.94)),
    var(--eval-panel);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.2);
}

.evaluation-showcase__hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.42fr);
  gap: 14px;
  border-color: var(--eval-border-strong);
  padding: 17px;
  background:
    linear-gradient(135deg, rgba(16, 31, 51, 0.96), rgba(8, 17, 31, 0.9)),
    repeating-linear-gradient(90deg, rgba(151, 180, 214, 0.04) 0 1px, transparent 1px 48px);
}

.evaluation-showcase__eyebrow {
  margin: 0 0 6px;
  color: #9fc4ff;
  font-size: 11px;
  font-weight: 900;
}

.evaluation-showcase__hero h1 {
  color: var(--eval-text);
  font-size: 34px;
  line-height: 1.05;
}

.evaluation-showcase__hero p {
  max-width: 780px;
  margin-top: 9px;
  color: var(--eval-secondary);
  font-size: 14px;
  line-height: 1.55;
}

.evaluation-showcase__hero-tags,
.evaluation-showcase__scope-list,
.evaluation-showcase__next-list,
.evaluation-showcase__reports,
.evaluation-showcase__boundary-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.evaluation-showcase__hero-tags {
  margin-top: 12px;
}

.evaluation-showcase__hero-tags span,
.evaluation-showcase__panel-heading > span,
.evaluation-showcase__scope-list span,
.evaluation-showcase__next-list span,
.evaluation-showcase__reports span,
.evaluation-showcase__boundary-list span {
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

.evaluation-showcase__dataset-card {
  display: grid;
  gap: 7px;
  border: 1px solid rgba(33, 199, 217, 0.18);
  border-left: 3px solid var(--eval-cyan);
  border-radius: 8px;
  padding: 11px;
  background: rgba(4, 9, 18, 0.38);
}

.evaluation-showcase__dataset-card span,
.evaluation-showcase__dataset-card p {
  color: var(--eval-muted);
  font-size: 11px;
  line-height: 1.4;
}

.evaluation-showcase__dataset-card strong {
  color: var(--eval-text);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.evaluation-showcase__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.evaluation-showcase__metrics article,
.evaluation-showcase__rank-row article {
  display: grid;
  gap: 5px;
  min-height: 88px;
  border-top: 3px solid var(--eval-blue);
  padding: 10px 11px;
}

.evaluation-showcase__metrics article[data-tone='green'] {
  border-top-color: var(--eval-green);
}

.evaluation-showcase__metrics article[data-tone='cyan'],
.evaluation-showcase__rank-row article[data-tone='cyan'] {
  border-top-color: var(--eval-cyan);
}

.evaluation-showcase__metrics article[data-tone='amber'] {
  border-top-color: var(--eval-amber);
}

.evaluation-showcase__metrics article[data-tone='red'] {
  border-top-color: var(--eval-red);
}

.evaluation-showcase__metrics article[data-tone='violet'] {
  border-top-color: var(--eval-violet);
}

.evaluation-showcase__metrics span,
.evaluation-showcase__metrics p,
.evaluation-showcase__metrics small,
.evaluation-showcase__rank-row span,
.evaluation-showcase__rank-row small {
  color: var(--eval-muted);
  font-size: 11px;
  line-height: 1.3;
}

.evaluation-showcase__metrics span,
.evaluation-showcase__rank-row span {
  font-weight: 900;
}

.evaluation-showcase__metrics strong,
.evaluation-showcase__rank-row strong {
  color: var(--eval-text);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 23px;
  line-height: 1;
}

.evaluation-showcase__rank-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.evaluation-showcase__rank-row article {
  min-height: 72px;
  box-shadow: none;
}

.evaluation-showcase__grid {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.evaluation-showcase__grid--scope {
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, 0.85fr);
}

.evaluation-showcase__grid--cases {
  grid-template-columns: minmax(0, 1.2fr) minmax(300px, 0.8fr);
}

.evaluation-showcase__grid--bottom {
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.95fr);
}

.evaluation-showcase__panel {
  min-width: 0;
  overflow: hidden;
}

.evaluation-showcase__panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-height: 52px;
  border-bottom: 1px solid var(--eval-border);
  padding: 10px 12px;
}

.evaluation-showcase__panel-heading h2 {
  color: var(--eval-text);
  font-size: 16px;
  line-height: 1.2;
}

.evaluation-showcase__scope-list,
.evaluation-showcase__next-list,
.evaluation-showcase__commands,
.evaluation-showcase__reports,
.evaluation-showcase__boundary-list {
  padding: 10px 12px 0;
}

.evaluation-showcase__next-list span,
.evaluation-showcase__boundary-list span {
  border-color: rgba(255, 180, 92, 0.17);
  color: #ffd6a8;
  background: rgba(255, 180, 92, 0.06);
}

.evaluation-showcase__baseline-table,
.evaluation-showcase__case-table {
  display: grid;
  gap: 5px;
  padding: 10px;
}

.evaluation-showcase__baseline-head,
.evaluation-showcase__baseline-row {
  display: grid;
  grid-template-columns: minmax(160px, 1.4fr) repeat(5, minmax(76px, 0.62fr));
  gap: 7px;
  align-items: center;
}

.evaluation-showcase__baseline-head,
.evaluation-showcase__case-head {
  color: var(--eval-muted);
  font-size: 10.5px;
  font-weight: 900;
}

.evaluation-showcase__baseline-row,
.evaluation-showcase__case-row {
  min-height: 46px;
  border: 1px solid rgba(151, 180, 214, 0.09);
  border-radius: 8px;
  padding: 7px;
  background: rgba(4, 9, 18, 0.32);
}

.evaluation-showcase__baseline-row strong,
.evaluation-showcase__case-row strong {
  display: block;
  color: #9fc4ff;
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 12px;
}

.evaluation-showcase__baseline-row small,
.evaluation-showcase__case-row small {
  display: block;
  margin-top: 3px;
  color: var(--eval-muted);
  font-size: 10px;
  line-height: 1.25;
}

.evaluation-showcase__baseline-row span,
.evaluation-showcase__case-row span {
  min-width: 0;
  overflow: hidden;
  color: var(--eval-secondary);
  font-size: 11px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.evaluation-showcase__plain-note {
  margin: 10px 12px 12px;
  border: 1px solid rgba(255, 180, 92, 0.16);
  border-radius: 8px;
  padding: 10px;
  color: var(--eval-secondary);
  background: rgba(255, 180, 92, 0.055);
  font-size: 12px;
  line-height: 1.55;
}

.evaluation-showcase__case-head,
.evaluation-showcase__case-row {
  display: grid;
  grid-template-columns: 78px 74px minmax(150px, 1.1fr) 68px minmax(160px, 1fr) 82px;
  gap: 7px;
  align-items: center;
}

.evaluation-showcase__case-row {
  border-left: 3px solid var(--eval-green);
}

.evaluation-showcase__case-row[data-status='review'] {
  border-left-color: var(--eval-amber);
}

.evaluation-showcase__case-row[data-status='miss'] {
  border-left-color: var(--eval-red);
}

.evaluation-showcase__case-reason {
  grid-column: 1 / -1;
}

.evaluation-showcase__failure-list {
  display: grid;
  gap: 7px;
  padding: 10px;
}

.evaluation-showcase__failure-list section {
  border: 1px solid rgba(151, 180, 214, 0.1);
  border-left: 3px solid var(--eval-blue);
  border-radius: 8px;
  padding: 9px;
  background: rgba(4, 9, 18, 0.34);
}

.evaluation-showcase__failure-list section[data-tone='amber'] {
  border-left-color: var(--eval-amber);
}

.evaluation-showcase__failure-list section[data-tone='red'] {
  border-left-color: var(--eval-red);
}

.evaluation-showcase__failure-list section[data-tone='violet'] {
  border-left-color: var(--eval-violet);
}

.evaluation-showcase__failure-list section[data-tone='cyan'] {
  border-left-color: var(--eval-cyan);
}

.evaluation-showcase__failure-list span,
.evaluation-showcase__failure-list p {
  color: var(--eval-muted);
  font-size: 11px;
  line-height: 1.35;
}

.evaluation-showcase__failure-list strong {
  display: block;
  margin: 4px 0;
  color: var(--eval-text);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 20px;
}

.evaluation-showcase__commands {
  display: grid;
  gap: 8px;
}

.evaluation-showcase__commands code {
  display: block;
  border: 1px solid rgba(33, 199, 217, 0.18);
  border-radius: 8px;
  padding: 10px;
  overflow-wrap: anywhere;
  color: #dff8ff;
  background: rgba(3, 7, 14, 0.66);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 12px;
}

.evaluation-showcase__reports {
  padding-bottom: 12px;
}

.evaluation-showcase__panel--boundary {
  border-color: rgba(255, 180, 92, 0.18);
}

@media (max-width: 1180px) {
  .evaluation-showcase__hero,
  .evaluation-showcase__grid--scope,
  .evaluation-showcase__grid--cases,
  .evaluation-showcase__grid--bottom {
    grid-template-columns: 1fr;
  }

  .evaluation-showcase__metrics,
  .evaluation-showcase__rank-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .evaluation-showcase__hero h1 {
    font-size: 28px;
  }

  .evaluation-showcase__metrics,
  .evaluation-showcase__rank-row,
  .evaluation-showcase__baseline-head,
  .evaluation-showcase__baseline-row,
  .evaluation-showcase__case-head,
  .evaluation-showcase__case-row {
    grid-template-columns: 1fr;
  }

  .evaluation-showcase__baseline-head,
  .evaluation-showcase__case-head {
    display: none;
  }

  .evaluation-showcase__baseline-row span,
  .evaluation-showcase__case-row span {
    white-space: normal;
  }
}
</style>
