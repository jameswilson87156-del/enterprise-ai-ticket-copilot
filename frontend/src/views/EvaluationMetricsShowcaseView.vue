<script setup lang="ts">
import {
  currentScopeItems,
  evaluationBaselineRows,
  evaluationMetricCards,
  evaluationSnapshot,
  nextStageItems
} from '../data/evaluationMetrics'

const primaryMetricCards = evaluationMetricCards.slice(0, 8)

const supplementalMetrics = [
  {
    label: 'MRR',
    value: evaluationSnapshot.mrr,
    note: '可选本地排序指标'
  },
  {
    label: 'NDCG@K',
    value: evaluationSnapshot.ndcgAtK,
    note: '可选二值相关性指标'
  },
  {
    label: '本地路径状态',
    value: '已启用',
    note: '无 API Key 环境预期路径'
  },
  {
    label: '知识缺失回退',
    value: evaluationSnapshot.knowledgeMissFallbackRate,
    note: '无期望来源或不可检索'
  }
]
</script>

<template>
  <section class="evaluation-showcase" data-screenshot="evaluation-metrics" aria-label="评测指标中心">
    <header class="evaluation-showcase__hero">
      <div class="evaluation-showcase__intro">
        <p class="evaluation-showcase__eyebrow">Evaluation / Metrics · 本地 RAG 评测与引用证据指标</p>
        <h1>评测指标中心</h1>
        <p class="evaluation-showcase__summary">
          基于自建工单评测集，评估检索命中、引用证据、失败样本与人工复核门禁。
        </p>
        <div class="evaluation-showcase__hero-tags" aria-label="当前评测范围">
          <span>synthetic demo dataset</span>
          <span>Top-K: {{ evaluationSnapshot.topK }}</span>
          <span>local keyword retrieval</span>
          <span>citation gating</span>
          <span>local-rule fallback</span>
        </div>
      </div>

      <aside class="evaluation-showcase__dataset-card" aria-label="评测数据集">
        <div class="evaluation-showcase__dataset-title">
          <span aria-hidden="true">▤</span>
          <strong>评测数据集</strong>
        </div>
        <code>{{ evaluationSnapshot.datasetPath }}</code>
        <dl>
          <div>
            <dt>生成时间</dt>
            <dd>2026-07-04 14:00</dd>
          </div>
          <div>
            <dt>包含真实用户数据</dt>
            <dd>{{ evaluationSnapshot.containsRealUserData ? '是' : '否' }}</dd>
          </div>
        </dl>
      </aside>
    </header>

    <section class="evaluation-showcase__metrics" aria-label="核心评测指标">
      <article v-for="metric in primaryMetricCards" :key="metric.label" :data-tone="metric.tone">
        <div class="evaluation-showcase__metric-label">
          <span class="evaluation-showcase__metric-dot" aria-hidden="true"></span>
          <span>{{ metric.label }}</span>
        </div>
        <strong>{{ metric.value }}</strong>
        <p>{{ metric.note }}</p>
        <a v-if="metric.label === '失败样本数'" href="#trace-timeline">查看失败案例 <span aria-hidden="true">→</span></a>
        <a v-else-if="metric.label === '需人工复核'" href="#human-review">查看 Review <span aria-hidden="true">→</span></a>
      </article>
    </section>

    <section class="evaluation-showcase__supplemental" aria-label="补充评测指标">
      <article v-for="metric in supplementalMetrics" :key="metric.label">
        <span>{{ metric.label }}</span>
        <strong :class="{ 'evaluation-showcase__status-value': metric.value === '已启用' }">{{ metric.value }}</strong>
        <small>{{ metric.note }}</small>
      </article>
    </section>

    <section class="evaluation-showcase__planning-grid" aria-label="Baseline 与下一阶段实验计划">
      <article class="evaluation-showcase__panel evaluation-showcase__panel--baseline">
        <div class="evaluation-showcase__panel-heading">
          <div>
            <p class="evaluation-showcase__eyebrow">Baseline / Scope</p>
            <h2>当前版本 baseline</h2>
          </div>
          <a href="#evaluation-metrics" title="本地演示入口">查看 baseline 详情 <span aria-hidden="true">→</span></a>
        </div>

        <div class="evaluation-showcase__scope-list" aria-label="Baseline 范围">
          <span v-for="item in currentScopeItems" :key="item">{{ item }}</span>
        </div>

        <div class="evaluation-showcase__baseline-table" role="table" aria-label="Baseline 对比">
          <div class="evaluation-showcase__baseline-head" role="row">
            <span role="columnheader">策略方案</span>
            <span role="columnheader">Top-K 命中率</span>
            <span role="columnheader">引用覆盖率</span>
            <span role="columnheader">引用准确率</span>
            <span role="columnheader">失败样本数</span>
          </div>
          <article v-for="row in evaluationBaselineRows" :key="row.name" class="evaluation-showcase__baseline-row" role="row">
            <div role="cell">
              <strong>{{ row.label }}</strong>
              <small>{{ row.name }}</small>
            </div>
            <span role="cell">{{ row.topKHitRate }}</span>
            <span role="cell">{{ row.citationCoverage }}</span>
            <span role="cell">{{ row.citationPrecision }}</span>
            <span role="cell">{{ row.failedCases }}</span>
          </article>
        </div>
      </article>

      <article class="evaluation-showcase__panel evaluation-showcase__panel--next">
        <div class="evaluation-showcase__panel-heading">
          <div>
            <p class="evaluation-showcase__eyebrow">Planned Experiments</p>
            <h2>下一阶段实验计划</h2>
          </div>
          <span class="evaluation-showcase__planned-badge">规划中</span>
        </div>
        <div class="evaluation-showcase__next-list">
          <span v-for="item in nextStageItems" :key="item">{{ item }}</span>
        </div>
        <p class="evaluation-showcase__plain-note">
          当前版本不包含 BM25、embedding、Vector DB、Hybrid、Rerank 或真实模型质量评测；相关指标需在真实模型接入后追加。
        </p>
      </article>
    </section>
  </section>
</template>

<style scoped>
.evaluation-showcase {
  --eval-panel: rgba(11, 24, 39, 0.88);
  --eval-panel-strong: rgba(14, 29, 47, 0.94);
  --eval-border: rgba(145, 174, 207, 0.17);
  --eval-text: #edf4ff;
  --eval-secondary: #bdcbe0;
  --eval-muted: #788ca6;
  --eval-blue: #4b8cff;
  --eval-cyan: #32c8d6;
  --eval-green: #36d48d;
  --eval-amber: #f4ae42;
  --eval-red: #ff635f;
  --eval-violet: #a27af4;
  display: grid;
  gap: 9px;
  min-width: 0;
  color: var(--eval-text);
}

.evaluation-showcase *,
.evaluation-showcase *::before,
.evaluation-showcase *::after {
  box-sizing: border-box;
}

.evaluation-showcase h1,
.evaluation-showcase h2,
.evaluation-showcase p,
.evaluation-showcase dl,
.evaluation-showcase dd {
  margin: 0;
}

.evaluation-showcase__hero,
.evaluation-showcase__panel,
.evaluation-showcase__metrics article,
.evaluation-showcase__supplemental {
  border: 1px solid var(--eval-border);
  border-radius: 9px;
  background:
    linear-gradient(180deg, rgba(16, 32, 51, 0.9), rgba(7, 17, 30, 0.94)),
    var(--eval-panel);
  box-shadow: 0 16px 38px rgba(0, 0, 0, 0.16);
}

.evaluation-showcase__hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.43fr);
  gap: 18px;
  align-items: stretch;
  padding: 15px 16px;
}

.evaluation-showcase__intro {
  align-self: center;
}

.evaluation-showcase__eyebrow {
  color: #a8c8f7;
  font-size: 11px;
  font-weight: 800;
  line-height: 1.35;
}

.evaluation-showcase__hero h1 {
  margin-top: 7px;
  font-size: clamp(27px, 2.2vw, 36px);
  line-height: 1.08;
  letter-spacing: -0.02em;
}

.evaluation-showcase__summary {
  margin-top: 9px !important;
  color: var(--eval-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.evaluation-showcase__hero-tags,
.evaluation-showcase__scope-list,
.evaluation-showcase__next-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.evaluation-showcase__hero-tags {
  margin-top: 12px;
}

.evaluation-showcase__hero-tags span,
.evaluation-showcase__scope-list span,
.evaluation-showcase__next-list span,
.evaluation-showcase__planned-badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  border: 1px solid rgba(75, 140, 255, 0.18);
  border-radius: 6px;
  padding: 3px 8px;
  color: #9fc4ff;
  background: rgba(75, 140, 255, 0.055);
  font-size: 10.5px;
  font-weight: 750;
}

.evaluation-showcase__hero-tags span:nth-child(3),
.evaluation-showcase__hero-tags span:nth-child(4),
.evaluation-showcase__scope-list span:not(:last-child) {
  border-color: rgba(54, 212, 141, 0.16);
  color: #7addad;
  background: rgba(54, 212, 141, 0.045);
}

.evaluation-showcase__dataset-card {
  display: grid;
  align-content: center;
  gap: 10px;
  border: 1px solid rgba(145, 174, 207, 0.16);
  border-radius: 8px;
  padding: 13px 14px;
  background: rgba(5, 13, 24, 0.46);
}

.evaluation-showcase__dataset-title {
  display: flex;
  gap: 8px;
  align-items: center;
  color: var(--eval-secondary);
  font-size: 13px;
}

.evaluation-showcase__dataset-title > span {
  color: var(--eval-cyan);
  font-size: 17px;
}

.evaluation-showcase__dataset-card code {
  overflow-wrap: anywhere;
  color: #dbe9fb;
  font: 11px/1.45 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.evaluation-showcase__dataset-card dl {
  display: grid;
  gap: 7px;
}

.evaluation-showcase__dataset-card dl div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--eval-muted);
  font-size: 10.5px;
}

.evaluation-showcase__dataset-card dd {
  color: var(--eval-secondary);
}

.evaluation-showcase__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.evaluation-showcase__metrics article {
  position: relative;
  display: grid;
  align-content: start;
  gap: 7px;
  min-height: 128px;
  padding: 12px 13px;
  overflow: hidden;
}

.evaluation-showcase__metrics article::after {
  position: absolute;
  inset: 0 auto auto 0;
  width: 2px;
  height: 100%;
  background: var(--metric-color, var(--eval-blue));
  content: '';
  opacity: 0.82;
}

.evaluation-showcase__metrics article[data-tone='green'] { --metric-color: var(--eval-green); }
.evaluation-showcase__metrics article[data-tone='cyan'] { --metric-color: var(--eval-cyan); }
.evaluation-showcase__metrics article[data-tone='amber'] { --metric-color: var(--eval-amber); }
.evaluation-showcase__metrics article[data-tone='red'] { --metric-color: var(--eval-red); }
.evaluation-showcase__metrics article[data-tone='violet'] { --metric-color: var(--eval-violet); }

.evaluation-showcase__metric-label {
  display: flex;
  gap: 7px;
  align-items: center;
  color: var(--eval-secondary);
  font-size: 11.5px;
  font-weight: 760;
}

.evaluation-showcase__metric-dot {
  width: 7px;
  height: 7px;
  border: 1px solid var(--metric-color, var(--eval-blue));
  border-radius: 50%;
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--metric-color, var(--eval-blue)) 12%, transparent);
}

.evaluation-showcase__metrics strong {
  color: var(--eval-text);
  font: 500 clamp(24px, 2vw, 33px)/1 "Cascadia Code", SFMono-Regular, Consolas, monospace;
  letter-spacing: -0.035em;
}

.evaluation-showcase__metrics p {
  color: var(--eval-muted);
  font-size: 10.5px;
  line-height: 1.4;
}

.evaluation-showcase a {
  width: fit-content;
  color: #69a8ff;
  font-size: 10.5px;
  font-weight: 750;
  text-decoration: none;
}

.evaluation-showcase a:hover,
.evaluation-showcase a:focus-visible {
  color: #a9d1ff;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.evaluation-showcase__supplemental {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 9px 12px;
  box-shadow: none;
}

.evaluation-showcase__supplemental article {
  display: grid;
  gap: 4px;
  min-width: 0;
  border-right: 1px solid rgba(145, 174, 207, 0.12);
  padding: 2px 14px;
}

.evaluation-showcase__supplemental article:first-child {
  padding-left: 2px;
}

.evaluation-showcase__supplemental article:last-child {
  border-right: 0;
}

.evaluation-showcase__supplemental span,
.evaluation-showcase__supplemental small {
  color: var(--eval-muted);
  font-size: 10.5px;
  line-height: 1.35;
}

.evaluation-showcase__supplemental strong {
  color: #c5d3e6;
  font: 500 17px/1.2 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.evaluation-showcase__supplemental .evaluation-showcase__status-value {
  color: var(--eval-green);
  font-family: inherit;
  font-weight: 750;
}

.evaluation-showcase__planning-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.07fr) minmax(330px, 0.93fr);
  gap: 9px;
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
  border-bottom: 1px solid var(--eval-border);
  padding: 10px 12px;
}

.evaluation-showcase__panel-heading h2 {
  margin-top: 2px;
  font-size: 15px;
  line-height: 1.2;
}

.evaluation-showcase__scope-list,
.evaluation-showcase__next-list {
  padding: 9px 11px 0;
}

.evaluation-showcase__next-list span,
.evaluation-showcase__planned-badge {
  border-color: rgba(162, 122, 244, 0.16);
  color: #c7b2f3;
  background: rgba(162, 122, 244, 0.045);
}

.evaluation-showcase__baseline-table {
  display: grid;
  gap: 4px;
  padding: 9px 10px 10px;
}

.evaluation-showcase__baseline-head,
.evaluation-showcase__baseline-row {
  display: grid;
  grid-template-columns: minmax(170px, 1.35fr) repeat(4, minmax(72px, 0.7fr));
  gap: 7px;
  align-items: center;
}

.evaluation-showcase__baseline-head {
  padding: 0 7px 3px;
  color: var(--eval-muted);
  font-size: 10px;
  font-weight: 750;
}

.evaluation-showcase__baseline-row {
  min-height: 46px;
  border: 1px solid rgba(145, 174, 207, 0.08);
  border-radius: 7px;
  padding: 7px;
  background: rgba(4, 11, 21, 0.32);
}

.evaluation-showcase__baseline-row strong,
.evaluation-showcase__baseline-row small {
  display: block;
}

.evaluation-showcase__baseline-row strong {
  color: #dbe7f7;
  font-size: 11px;
}

.evaluation-showcase__baseline-row small {
  margin-top: 3px;
  color: #63758d;
  font: 9px/1.25 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.evaluation-showcase__baseline-row > span {
  color: var(--eval-secondary);
  font: 10.5px/1.3 "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.evaluation-showcase__plain-note {
  margin: 10px 11px 11px !important;
  border: 1px solid rgba(145, 174, 207, 0.12);
  border-radius: 7px;
  padding: 10px;
  color: var(--eval-muted);
  background: rgba(4, 11, 21, 0.32);
  font-size: 11px;
  line-height: 1.55;
}

@media (max-width: 1180px) {
  .evaluation-showcase__hero,
  .evaluation-showcase__planning-grid {
    grid-template-columns: 1fr;
  }

  .evaluation-showcase__metrics,
  .evaluation-showcase__supplemental {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .evaluation-showcase__supplemental article:nth-child(2) {
    border-right: 0;
  }

  .evaluation-showcase__supplemental article:nth-child(n + 3) {
    border-top: 1px solid rgba(145, 174, 207, 0.12);
    padding-top: 8px;
  }
}

@media (max-width: 720px) {
  .evaluation-showcase__hero,
  .evaluation-showcase__metrics,
  .evaluation-showcase__supplemental {
    grid-template-columns: 1fr;
  }

  .evaluation-showcase__metrics article {
    min-height: 116px;
  }

  .evaluation-showcase__supplemental article {
    border-right: 0;
    border-bottom: 1px solid rgba(145, 174, 207, 0.12);
    padding: 8px 2px;
  }

  .evaluation-showcase__supplemental article:last-child {
    border-bottom: 0;
  }

  .evaluation-showcase__baseline-head {
    display: none;
  }

  .evaluation-showcase__baseline-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .evaluation-showcase__baseline-row > div {
    grid-column: 1 / -1;
  }
}
</style>
