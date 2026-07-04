<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, type Component } from 'vue'
import {
  boundaryStatements,
  evaluationSnapshot,
  providerStatusItems,
  recentTicketRuns
} from './data/evaluationMetrics'
import DashboardShowcaseView from './views/DashboardShowcaseView.vue'
import EvaluationMetricsShowcaseView from './views/EvaluationMetricsShowcaseView.vue'
import HumanReviewShowcaseView from './views/HumanReviewShowcaseView.vue'
import KnowledgeRagShowcaseView from './views/KnowledgeRagShowcaseView.vue'
import TicketWorkbenchShowcaseView from './views/TicketWorkbenchShowcaseView.vue'
import TraceShowcaseView from './views/TraceShowcaseView.vue'

type ShowcaseRoute =
  | 'dashboard'
  | 'ticket-detail'
  | 'knowledge-base'
  | 'retrieval-evidence'
  | 'trace-timeline'
  | 'human-review'
  | 'evaluation-metrics'

interface NavItem {
  label: string
  caption: string
  route: ShowcaseRoute
}

interface RouteMeta {
  eyebrow: string
  title: string
  description: string
  contextTitle: string
}

const routeAliases: Record<string, ShowcaseRoute> = {
  dashboard: 'dashboard',
  tickets: 'ticket-detail',
  'ticket-detail': 'ticket-detail',
  'ticket-workbench': 'ticket-detail',
  knowledge: 'knowledge-base',
  rag: 'knowledge-base',
  'knowledge-base': 'knowledge-base',
  evidence: 'retrieval-evidence',
  retrieval: 'retrieval-evidence',
  'retrieval-evidence': 'retrieval-evidence',
  trace: 'trace-timeline',
  'trace-evidence': 'trace-timeline',
  'trace-timeline': 'trace-timeline',
  review: 'human-review',
  'human-review': 'human-review',
  eval: 'evaluation-metrics',
  evaluation: 'evaluation-metrics',
  metrics: 'evaluation-metrics',
  'evaluation-metrics': 'evaluation-metrics'
}

const navItems: NavItem[] = [
  { label: 'Dashboard', caption: 'system cockpit', route: 'dashboard' },
  { label: 'Ticket Workbench', caption: 'queue + copilot', route: 'ticket-detail' },
  { label: 'Knowledge Base', caption: 'keyword sources', route: 'knowledge-base' },
  { label: 'Retrieval Evidence', caption: 'Top-K citations', route: 'retrieval-evidence' },
  { label: 'Trace Timeline', caption: 'run audit trail', route: 'trace-timeline' },
  { label: 'Human Review', caption: 'review gate', route: 'human-review' },
  { label: 'Evaluation / Metrics', caption: 'local eval loop', route: 'evaluation-metrics' }
]

const showcaseComponents: Record<ShowcaseRoute, Component> = {
  dashboard: DashboardShowcaseView,
  'ticket-detail': TicketWorkbenchShowcaseView,
  'knowledge-base': KnowledgeRagShowcaseView,
  'retrieval-evidence': TraceShowcaseView,
  'trace-timeline': TraceShowcaseView,
  'human-review': HumanReviewShowcaseView,
  'evaluation-metrics': EvaluationMetricsShowcaseView
}

const routeMeta: Record<ShowcaseRoute, RouteMeta> = {
  dashboard: {
    eyebrow: 'Showcase Demo / Operations Cockpit',
    title: 'Enterprise Ticket RAG Copilot',
    description: 'RAG Evidence, Trace, Human Review, and Evaluation Metrics for a synthetic enterprise ticket demo.',
    contextTitle: 'Dashboard context'
  },
  'ticket-detail': {
    eyebrow: 'Ticket Workbench',
    title: 'Ticket Workbench',
    description: 'Queue, ticket context, local-rule analysis, citation preview, and visible review actions.',
    contextTitle: 'Workbench context'
  },
  'knowledge-base': {
    eyebrow: 'Knowledge Operations',
    title: 'Knowledge Base',
    description: 'Keyword retrieval sources, chunk evidence, related tickets, and citation preview.',
    contextTitle: 'Knowledge context'
  },
  'retrieval-evidence': {
    eyebrow: 'Retrieval Evidence',
    title: 'Retrieval Evidence',
    description: 'Top-K keyword matches, citation decisions, fallback notes, and review cues.',
    contextTitle: 'Evidence context'
  },
  'trace-timeline': {
    eyebrow: 'Trace Timeline',
    title: 'Trace Timeline',
    description: 'Run steps, provider fallback, generation record, JSON evidence, and human gate.',
    contextTitle: 'Trace context'
  },
  'human-review': {
    eyebrow: 'Human Review',
    title: 'Human Review',
    description: 'Evidence-first review queue with approve, request changes, and reject decisions.',
    contextTitle: 'Review context'
  },
  'evaluation-metrics': {
    eyebrow: 'Evaluation / Metrics',
    title: 'Evaluation / Metrics',
    description: 'Local reproducible evaluation for synthetic demo cases, citation gating, and review gate.',
    contextTitle: 'Evaluation context'
  }
}

const activeRoute = ref<ShowcaseRoute>(readHashRoute())
const activeComponent = computed(() => showcaseComponents[activeRoute.value])
const activeMeta = computed(() => routeMeta[activeRoute.value])
const activeNavLabel = computed(() => navItems.find((item) => item.route === activeRoute.value)?.label ?? 'Dashboard')
const showContextPanel = computed(() => activeRoute.value === 'dashboard' || activeRoute.value === 'evaluation-metrics')

function readHashRoute(): ShowcaseRoute {
  const hash = window.location.hash.replace(/^#\/?/, '').trim()
  return routeAliases[hash] ?? 'dashboard'
}

function navigateTo(route: ShowcaseRoute) {
  activeRoute.value = route
  window.history.pushState(null, '', `#${route}`)
}

function handleRouteChange() {
  activeRoute.value = readHashRoute()
}

onMounted(() => {
  window.addEventListener('hashchange', handleRouteChange)
  window.addEventListener('popstate', handleRouteChange)
})

onBeforeUnmount(() => {
  window.removeEventListener('hashchange', handleRouteChange)
  window.removeEventListener('popstate', handleRouteChange)
})
</script>

<template>
  <div class="portfolio-shell" :data-active-route="activeRoute">
    <aside class="portfolio-shell__sidebar" aria-label="Showcase navigation">
      <div class="portfolio-shell__brand" aria-label="Enterprise Ticket RAG Copilot">
        <span class="portfolio-shell__brand-mark" aria-hidden="true">ET</span>
        <div>
          <strong>Enterprise Ticket</strong>
          <span>RAG Copilot</span>
        </div>
      </div>

      <nav class="portfolio-shell__nav" aria-label="Primary showcase routes">
        <button
          v-for="item in navItems"
          :key="item.route"
          type="button"
          class="portfolio-shell__nav-item"
          :class="{ 'portfolio-shell__nav-item--active': activeRoute === item.route }"
          @click="navigateTo(item.route)"
        >
          <span class="portfolio-shell__nav-dot" aria-hidden="true"></span>
          <span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.caption }}</small>
          </span>
        </button>
      </nav>

      <section class="portfolio-shell__boundary" aria-label="Demo boundary">
        <span>Project Mode</span>
        <strong>Showcase Demo</strong>
        <p>synthetic dataset, local keyword retrieval, citation gating, local-rule fallback.</p>
      </section>
    </aside>

    <section class="portfolio-shell__workspace">
      <header class="portfolio-shell__topbar" aria-label="Runtime status">
        <div class="portfolio-shell__topbar-title">
          <span>{{ activeMeta.eyebrow }}</span>
          <strong>{{ activeNavLabel }}</strong>
        </div>
        <div class="portfolio-shell__status-strip">
          <span>Project Mode: Showcase Demo</span>
          <span>Provider: local-rule fallback</span>
          <span>Retrieval: keyword retrieval</span>
          <span>Eval Dataset: {{ evaluationSnapshot.sampleCount }} synthetic cases</span>
          <span>Demo Boundary</span>
        </div>
      </header>

      <main class="portfolio-shell__body" :class="{ 'portfolio-shell__body--with-context': showContextPanel }">
        <section class="portfolio-shell__content" aria-live="polite">
          <component :is="activeComponent" />
        </section>

        <aside v-if="showContextPanel" class="portfolio-shell__context" :aria-label="activeMeta.contextTitle">
          <section class="portfolio-shell__context-card portfolio-shell__context-card--strong">
            <span>Provider status</span>
            <strong>local-rule fallback active</strong>
            <p>OpenAI-compatible provider path is optional and not configured in this no-key local run.</p>
          </section>

          <section class="portfolio-shell__context-card">
            <span>Eval snapshot</span>
            <div class="portfolio-shell__mini-metrics">
              <b>{{ evaluationSnapshot.topKHitRate }}</b>
              <small>Top-K Hit Rate</small>
              <b>{{ evaluationSnapshot.citationPrecision }}</b>
              <small>Citation Precision</small>
              <b>{{ evaluationSnapshot.avgRetrievalLatency }}</b>
              <small>Avg Retrieval Latency</small>
            </div>
          </section>

          <section class="portfolio-shell__context-card">
            <span>Provider / retrieval scope</span>
            <div class="portfolio-shell__status-list">
              <article v-for="item in providerStatusItems" :key="item.label" :data-tone="item.tone">
                <strong>{{ item.value }}</strong>
                <span>{{ item.label }}</span>
                <small>{{ item.note }}</small>
              </article>
            </div>
          </section>

          <section class="portfolio-shell__context-card">
            <span>Recent trace / review</span>
            <ol class="portfolio-shell__trace-list">
              <li v-for="run in recentTicketRuns.slice(0, 3)" :key="run.caseId">
                <strong>{{ run.ticketId }}</strong>
                <span>{{ run.citationStatus }} / {{ run.reviewStatus }}</span>
                <small>{{ run.caseId }} · {{ run.providerPath }}</small>
              </li>
            </ol>
          </section>

          <section class="portfolio-shell__context-card portfolio-shell__context-card--boundary">
            <span>Boundary note</span>
            <p>Demo metrics are generated from synthetic evaluation cases and local scripts. They are used for portfolio verification, not production claims.</p>
            <div>
              <small v-for="item in boundaryStatements.slice(0, 4)" :key="item">{{ item }}</small>
            </div>
          </section>
        </aside>
      </main>
    </section>
  </div>
</template>

<style scoped>
.portfolio-shell {
  --shell-canvas: #07101d;
  --shell-canvas-deep: #040912;
  --shell-sidebar: #08111f;
  --shell-panel: rgba(12, 23, 38, 0.88);
  --shell-panel-strong: rgba(16, 31, 51, 0.94);
  --shell-border: rgba(151, 180, 214, 0.16);
  --shell-border-strong: rgba(91, 141, 239, 0.26);
  --shell-text: #eef5ff;
  --shell-secondary: #c7d5ea;
  --shell-muted: #7e91ab;
  --shell-blue: #3d7cff;
  --shell-cyan: #21c7d9;
  --shell-green: #2bd88f;
  --shell-amber: #ffb45c;
  --shell-red: #ff5c7a;
  --shell-violet: #8b7cf6;
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  min-height: 100vh;
  overflow: hidden;
  color: var(--shell-text);
  background:
    linear-gradient(135deg, rgba(4, 9, 18, 0.98), rgba(7, 16, 29, 0.98)),
    repeating-linear-gradient(90deg, rgba(151, 180, 214, 0.035) 0 1px, transparent 1px 72px);
  font-family: Aptos, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  letter-spacing: 0;
}

.portfolio-shell *,
.portfolio-shell *::before,
.portfolio-shell *::after {
  box-sizing: border-box;
}

.portfolio-shell__sidebar {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 16px;
  min-height: 100vh;
  border-right: 1px solid var(--shell-border);
  padding: 16px 14px;
  background:
    linear-gradient(180deg, rgba(8, 17, 31, 0.98), rgba(4, 9, 18, 0.98)),
    var(--shell-sidebar);
  box-shadow: 16px 0 38px rgba(0, 0, 0, 0.2);
}

.portfolio-shell__brand {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 48px;
}

.portfolio-shell__brand-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 1px solid rgba(33, 199, 217, 0.34);
  border-radius: 8px;
  color: #dff8ff;
  background:
    linear-gradient(135deg, rgba(33, 199, 217, 0.24), rgba(61, 124, 255, 0.2)),
    rgba(4, 10, 22, 0.8);
  box-shadow: 0 14px 30px rgba(61, 124, 255, 0.18);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 13px;
  font-weight: 900;
}

.portfolio-shell__brand strong,
.portfolio-shell__brand span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.portfolio-shell__brand strong {
  color: var(--shell-text);
  font-size: 14px;
  line-height: 1.25;
}

.portfolio-shell__brand span {
  margin-top: 3px;
  color: var(--shell-muted);
  font-size: 12px;
  font-weight: 700;
}

.portfolio-shell__nav {
  display: grid;
  align-content: start;
  gap: 7px;
  min-height: 0;
  overflow: auto;
}

.portfolio-shell__nav-item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 48px;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 7px 9px;
  color: var(--shell-secondary);
  background: transparent;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.portfolio-shell__nav-item:hover,
.portfolio-shell__nav-item--active {
  border-color: rgba(61, 124, 255, 0.38);
  color: var(--shell-text);
  background: linear-gradient(135deg, rgba(61, 124, 255, 0.22), rgba(33, 199, 217, 0.08));
}

.portfolio-shell__nav-item--active {
  box-shadow: inset 3px 0 0 rgba(33, 199, 217, 0.88), 0 12px 28px rgba(61, 124, 255, 0.12);
}

.portfolio-shell__nav-dot {
  width: 8px;
  height: 8px;
  border-radius: 3px;
  background: rgba(126, 145, 171, 0.55);
}

.portfolio-shell__nav-item--active .portfolio-shell__nav-dot {
  background: var(--shell-cyan);
  box-shadow: 0 0 0 4px rgba(33, 199, 217, 0.14);
}

.portfolio-shell__nav-item strong,
.portfolio-shell__nav-item small {
  display: block;
}

.portfolio-shell__nav-item strong {
  font-size: 13px;
  line-height: 1.2;
}

.portfolio-shell__nav-item small {
  margin-top: 3px;
  color: var(--shell-muted);
  font-size: 11px;
  line-height: 1.2;
}

.portfolio-shell__boundary {
  display: grid;
  gap: 6px;
  border: 1px solid rgba(255, 180, 92, 0.18);
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 180, 92, 0.06);
}

.portfolio-shell__boundary span,
.portfolio-shell__context-card > span,
.portfolio-shell__topbar-title span {
  color: #9fc4ff;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
}

.portfolio-shell__boundary strong {
  color: var(--shell-text);
  font-size: 13px;
}

.portfolio-shell__boundary p {
  margin: 0;
  color: var(--shell-muted);
  font-size: 12px;
  line-height: 1.45;
}

.portfolio-shell__workspace {
  min-width: 0;
  min-height: 100vh;
}

.portfolio-shell__topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: minmax(190px, 0.28fr) minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  min-height: 62px;
  border-bottom: 1px solid var(--shell-border);
  padding: 9px 16px;
  background: rgba(4, 9, 18, 0.9);
  backdrop-filter: blur(18px);
}

.portfolio-shell__topbar-title {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.portfolio-shell__topbar-title strong {
  overflow: hidden;
  color: var(--shell-text);
  font-size: 15px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.portfolio-shell__status-strip {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 7px;
  min-width: 0;
}

.portfolio-shell__status-strip span {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  border: 1px solid var(--shell-border);
  border-radius: 8px;
  padding: 0 9px;
  color: var(--shell-secondary);
  background: rgba(12, 23, 38, 0.76);
  font-size: 11.5px;
  font-weight: 800;
  white-space: nowrap;
}

.portfolio-shell__status-strip span:nth-child(2),
.portfolio-shell__status-strip span:nth-child(3) {
  border-color: rgba(33, 199, 217, 0.22);
  color: #9fe8f1;
}

.portfolio-shell__status-strip span:nth-child(4) {
  border-color: rgba(139, 124, 246, 0.24);
  color: #d8d2ff;
}

.portfolio-shell__status-strip span:nth-child(5) {
  border-color: rgba(255, 180, 92, 0.24);
  color: #ffd6a8;
}

.portfolio-shell__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
  width: min(100%, 1680px);
  min-width: 0;
  margin: 0 auto;
  padding: 12px 14px 18px;
}

.portfolio-shell__body--with-context {
  grid-template-columns: minmax(0, 1fr) 338px;
}

.portfolio-shell__content {
  min-width: 0;
}

.portfolio-shell__context {
  display: grid;
  align-content: start;
  gap: 10px;
  min-width: 0;
}

.portfolio-shell__context-card {
  display: grid;
  gap: 8px;
  border: 1px solid var(--shell-border);
  border-radius: 8px;
  padding: 11px;
  background:
    linear-gradient(180deg, rgba(16, 31, 51, 0.82), rgba(8, 17, 31, 0.92)),
    var(--shell-panel);
}

.portfolio-shell__context-card--strong {
  border-color: rgba(33, 199, 217, 0.24);
  background: linear-gradient(135deg, rgba(33, 199, 217, 0.12), rgba(12, 23, 38, 0.9));
}

.portfolio-shell__context-card--boundary {
  border-color: rgba(255, 180, 92, 0.18);
  background: linear-gradient(135deg, rgba(255, 180, 92, 0.09), rgba(12, 23, 38, 0.88));
}

.portfolio-shell__context-card strong {
  color: var(--shell-text);
  font-size: 13px;
  line-height: 1.35;
}

.portfolio-shell__context-card p {
  margin: 0;
  color: var(--shell-muted);
  font-size: 12px;
  line-height: 1.5;
}

.portfolio-shell__mini-metrics {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 6px 9px;
  align-items: center;
}

.portfolio-shell__mini-metrics b {
  color: var(--shell-cyan);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 13px;
}

.portfolio-shell__mini-metrics small {
  color: var(--shell-muted);
  font-size: 11px;
}

.portfolio-shell__status-list {
  display: grid;
  gap: 6px;
}

.portfolio-shell__status-list article {
  display: grid;
  gap: 3px;
  border: 1px solid rgba(151, 180, 214, 0.09);
  border-left: 3px solid var(--shell-blue);
  border-radius: 8px;
  padding: 7px 8px;
  background: rgba(4, 9, 18, 0.38);
}

.portfolio-shell__status-list article[data-tone='green'] {
  border-left-color: var(--shell-green);
}

.portfolio-shell__status-list article[data-tone='amber'] {
  border-left-color: var(--shell-amber);
}

.portfolio-shell__status-list article[data-tone='cyan'] {
  border-left-color: var(--shell-cyan);
}

.portfolio-shell__status-list article[data-tone='slate'] {
  border-left-color: #5b6f8d;
}

.portfolio-shell__status-list strong {
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 12px;
}

.portfolio-shell__status-list span,
.portfolio-shell__status-list small {
  color: var(--shell-muted);
  font-size: 11px;
  line-height: 1.3;
}

.portfolio-shell__trace-list {
  display: grid;
  gap: 7px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.portfolio-shell__trace-list li {
  display: grid;
  gap: 3px;
  border-left: 2px solid rgba(33, 199, 217, 0.42);
  padding-left: 9px;
}

.portfolio-shell__trace-list strong {
  color: #dfeaff;
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 12px;
}

.portfolio-shell__trace-list span,
.portfolio-shell__trace-list small {
  color: var(--shell-muted);
  font-size: 11px;
  line-height: 1.35;
}

.portfolio-shell__context-card--boundary div {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.portfolio-shell__context-card--boundary small {
  border: 1px solid rgba(255, 180, 92, 0.16);
  border-radius: 6px;
  padding: 4px 6px;
  color: #ffd6a8;
  background: rgba(255, 180, 92, 0.06);
  font-size: 10.5px;
}

.portfolio-shell__content :deep(.knowledge-showcase),
.portfolio-shell__content :deep(.trace-showcase),
.portfolio-shell__content :deep(.human-review-showcase) {
  grid-template-columns: 1fr !important;
  min-height: auto !important;
  background: transparent !important;
  overflow: visible !important;
}

.portfolio-shell__content :deep(.knowledge-showcase-sidebar),
.portfolio-shell__content :deep(.trace-showcase-sidebar),
.portfolio-shell__content :deep(.human-review-showcase-sidebar),
.portfolio-shell__content :deep(.knowledge-showcase-topbar),
.portfolio-shell__content :deep(.trace-showcase-topbar),
.portfolio-shell__content :deep(.human-review-showcase-topbar) {
  display: none !important;
}

.portfolio-shell__content :deep(.knowledge-showcase-main),
.portfolio-shell__content :deep(.trace-showcase-main),
.portfolio-shell__content :deep(.human-review-showcase-main) {
  min-height: auto !important;
  height: auto !important;
  padding: 0 !important;
  overflow: visible !important;
}

.portfolio-shell__content :deep(.human-review-showcase-workspace),
.portfolio-shell__content :deep(.trace-showcase-workspace),
.portfolio-shell__content :deep(.knowledge-showcase-workspace) {
  min-height: auto !important;
}

@media (max-width: 1320px) {
  .portfolio-shell__body,
  .portfolio-shell__body--with-context {
    grid-template-columns: minmax(0, 1fr);
  }

  .portfolio-shell__context {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .portfolio-shell {
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .portfolio-shell__sidebar {
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid var(--shell-border);
  }

  .portfolio-shell__nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .portfolio-shell__topbar,
  .portfolio-shell__body,
  .portfolio-shell__context {
    grid-template-columns: 1fr;
  }

  .portfolio-shell__status-strip {
    justify-content: flex-start;
  }
}

@media (max-width: 620px) {
  .portfolio-shell__sidebar {
    padding: 12px;
  }

  .portfolio-shell__nav,
  .portfolio-shell__context {
    grid-template-columns: 1fr;
  }

  .portfolio-shell__body {
    padding: 10px;
  }

  .portfolio-shell__status-strip span {
    white-space: normal;
  }
}
</style>
