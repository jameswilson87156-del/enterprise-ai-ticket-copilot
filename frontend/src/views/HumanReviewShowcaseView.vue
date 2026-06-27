<script setup lang="ts">
interface NavItem {
  label: string
  active?: boolean
}

interface HeroStatus {
  title: string
  caption: string
  value: string
  note: string
  tone: 'green' | 'orange' | 'blue' | 'purple'
}

interface KpiItem {
  label: string
  value: string
  note: string
  tone: 'blue' | 'green' | 'orange' | 'red' | 'purple'
}

interface ReviewQueueItem {
  reviewId: string
  ticketId: string
  title: string
  status: string
  priority: 'P1' | 'P2' | 'P3'
  risk: string
  reviewer: string
  updatedAt: string
  active?: boolean
}

interface EvidenceItem {
  label: string
  value: string
  note: string
  tone: 'blue' | 'green' | 'orange' | 'purple'
  primary?: boolean
}

interface DecisionItem {
  action: string
  label: string
  note: string
  tone: 'approve' | 'changes' | 'reject'
}

interface TimelineItem {
  time: string
  title: string
  note: string
  tone: 'info' | 'success' | 'warning'
}

const navItems: NavItem[] = [
  { label: '仪表盘' },
  { label: '工单管理' },
  { label: '知识库' },
  { label: 'RAG 引用' },
  { label: 'Prompt 模板' },
  { label: 'Provider 健康' },
  { label: 'Trace 证据' },
  { label: 'Human Review', active: true },
  { label: '演示设置' }
]

const heroStatuses: HeroStatus[] = [
  { title: '人工门禁', caption: 'Review Gate', value: 'enabled', note: '发布前确认', tone: 'green' },
  { title: '自动动作', caption: 'Auto Action', value: 'disabled', note: '不自动关闭', tone: 'orange' },
  { title: 'Trace 证据', caption: 'Trace Evidence', value: 'attached', note: '已关联 Trace', tone: 'blue' },
  { title: '知识引用', caption: 'Knowledge Reference', value: 'required', note: '审核前查看', tone: 'purple' }
]

const kpis: KpiItem[] = [
  { label: '待审核', value: '42', note: '等待 reviewer', tone: 'orange' },
  { label: '已批准', value: '118', note: '人工确认通过', tone: 'green' },
  { label: '请求修改', value: '16', note: '补充证据中', tone: 'blue' },
  { label: '已拒绝', value: '9', note: '证据不足', tone: 'red' },
  { label: '平均审核耗时', value: '18m', note: 'demo 中位值', tone: 'purple' }
]

const reviewQueue: ReviewQueueItem[] = [
  {
    reviewId: 'REV-2026-042',
    ticketId: 'DEMO-0005',
    title: '报价接口返回 500',
    status: '等待审核',
    priority: 'P1',
    risk: '高风险',
    reviewer: '二线支持',
    updatedAt: '11:38',
    active: true
  },
  {
    reviewId: 'REV-2026-039',
    ticketId: 'DEMO-0006',
    title: '报表权限审批后仍无法访问',
    status: '请求修改',
    priority: 'P2',
    risk: '中风险',
    reviewer: '权限负责人',
    updatedAt: '11:12'
  },
  {
    reviewId: 'REV-2026-037',
    ticketId: 'DEMO-0004',
    title: '客户列表查询超时',
    status: '已批准',
    priority: 'P2',
    risk: '中风险',
    reviewer: '支持组长',
    updatedAt: '10:46'
  },
  {
    reviewId: 'REV-2026-031',
    ticketId: 'DEMO-0003',
    title: 'Redis 连接失败导致会话校验超时',
    status: '等待审核',
    priority: 'P1',
    risk: '高风险',
    reviewer: '平台支持',
    updatedAt: '10:30'
  }
]

const evidenceItems: EvidenceItem[] = [
  { label: 'RAG 知识引用', value: 'KB-API-500', note: '接口 500 分层排查手册', tone: 'purple', primary: true },
  { label: 'Trace 证据', value: 'TRACE-0005', note: 'Provider fallback / 389ms', tone: 'blue', primary: true },
  { label: 'Provider / fallback', value: 'local-rule fallback', note: 'OpenAI-compatible 未配置', tone: 'orange' },
  { label: 'Similar Tickets', value: '3 条', note: 'DEMO-0003 / 0004 / 0006', tone: 'green' }
]

const decisions: DecisionItem[] = [
  { action: 'Approve', label: '批准草稿', note: '允许人工确认后的状态更新。', tone: 'approve' },
  { action: 'Request Changes', label: '请求修改', note: '退回并要求补充证据或改写草稿。', tone: 'changes' },
  { action: 'Reject', label: '拒绝建议', note: '停止使用本次建议，不执行自动处理。', tone: 'reject' }
]

const reviewTimeline: TimelineItem[] = [
  { time: '11:18', title: 'AI draft generated', note: '模板化回复草稿已生成，等待人工确认。', tone: 'info' },
  { time: '11:24', title: 'RAG reference attached', note: '已关联 KB-API-500 与报价接口故障知识。', tone: 'success' },
  { time: '11:31', title: 'Trace evidence recorded', note: '生成记录、fallback 原因与状态历史已入审计视图。', tone: 'success' },
  { time: '11:38', title: 'Review requested', note: 'P1 工单进入 Human Review 队列。', tone: 'warning' }
]

const boundaryNotes = [
  '当前为 Human-in-the-loop demo，不自动回复客户。',
  'Approve / Request Changes / Reject 只展示人工门禁，不自动关闭工单。',
  '不声明生产级权限体系、无人值守 Agent 或工具运行时能力。',
  '草稿、知识引用和 Trace 证据都必须由 reviewer 人工确认。'
]
</script>

<template>
  <div class="human-review-showcase" data-screenshot="human-review">
    <aside class="human-review-showcase-sidebar" aria-label="Human Review navigation">
      <div class="human-review-showcase-brand">
        <svg viewBox="0 0 48 48" aria-hidden="true" class="human-review-showcase-logo">
          <defs>
            <linearGradient id="review-logo-gradient" x1="8" x2="40" y1="8" y2="40">
              <stop offset="0" stop-color="#22d3ee" />
              <stop offset="0.55" stop-color="#3d7cff" />
              <stop offset="1" stop-color="#8b7cf6" />
            </linearGradient>
          </defs>
          <path d="M24 4 41 14v20L24 44 7 34V14L24 4Z" fill="rgba(61,124,255,.14)" stroke="url(#review-logo-gradient)" stroke-width="2" />
          <path d="M15 18 24 13l9 5v11l-9 5-9-5V18Z" fill="none" stroke="url(#review-logo-gradient)" stroke-width="2" />
          <circle cx="24" cy="13" r="2.5" fill="#22d3ee" />
          <circle cx="15" cy="29" r="2.5" fill="#3d7cff" />
          <circle cx="33" cy="29" r="2.5" fill="#8b7cf6" />
          <path d="M24 16v10l-8 4m8-4 8 4" fill="none" stroke="#bfe8ff" stroke-width="1.5" stroke-linecap="round" />
        </svg>
        <div>
          <strong>企业工单</strong>
          <span>RAG Copilot</span>
        </div>
      </div>

      <nav class="human-review-showcase-nav" aria-label="Showcase navigation">
        <span v-for="item in navItems" :key="item.label" :class="{ 'human-review-showcase-nav-item--active': item.active }" class="human-review-showcase-nav-item">
          {{ item.label }}
        </span>
      </nav>

      <section class="human-review-showcase-sidebar-card" aria-label="Capability boundary">
        <strong>人工审核边界</strong>
        <p>演示数据、local-rule fallback，所有决策都需要 reviewer 人工确认。</p>
      </section>
    </aside>

    <main class="human-review-showcase-main">
      <header class="human-review-showcase-topbar">
        <label class="human-review-showcase-search">
          <span>⌕</span>
          <input aria-label="搜索审核单" placeholder="搜索审核单、工单、Trace、知识引用、Reviewer..." />
        </label>
        <div class="human-review-showcase-topbar-status">
          <span>演示环境</span>
          <strong>Provider: local-rule fallback</strong>
          <em>Reviewer demo</em>
        </div>
      </header>

      <section class="human-review-showcase-hero" aria-label="Human Review hero">
        <div class="human-review-showcase-hero-copy">
          <p class="human-review-showcase-eyebrow">Human Review Center</p>
          <h1>Human Review 审核中心</h1>
          <h2>人工审核 / 决策控制台</h2>
          <p>统一展示待审核工单、AI 草稿、RAG 引用、Trace 证据、风险提示与人工决策记录。</p>
          <div class="human-review-showcase-hero-tags" aria-label="Review boundaries">
            <span>Human-in-the-loop demo</span>
            <span>不自动关闭工单</span>
            <span>证据优先</span>
          </div>
        </div>
        <div class="human-review-showcase-status-grid">
          <section v-for="status in heroStatuses" :key="status.title" :data-tone="status.tone" class="human-review-showcase-status-card">
            <span>{{ status.caption }}</span>
            <strong>{{ status.title }}</strong>
            <b>{{ status.value }}</b>
            <small>{{ status.note }}</small>
          </section>
        </div>
      </section>

      <section class="human-review-showcase-kpis" aria-label="Human Review KPIs">
        <article v-for="kpi in kpis" :key="kpi.label" :data-tone="kpi.tone" class="human-review-showcase-kpi">
          <span>{{ kpi.label }}</span>
          <strong>{{ kpi.value }}</strong>
          <small>{{ kpi.note }}</small>
        </article>
      </section>

      <section class="human-review-showcase-workspace" aria-label="Human Review workspace">
        <aside class="human-review-showcase-panel human-review-showcase-queue" aria-label="审核队列">
          <div class="human-review-showcase-panel-heading">
            <div>
              <p class="human-review-showcase-eyebrow">Review Queue</p>
              <h3>审核队列</h3>
            </div>
            <span>42</span>
          </div>
          <div class="human-review-showcase-queue-list">
            <article v-for="item in reviewQueue" :key="item.reviewId" :class="{ 'human-review-showcase-queue-item--active': item.active }" class="human-review-showcase-queue-item">
              <div class="human-review-showcase-queue-top">
                <span>{{ item.reviewId }}</span>
                <b :data-priority="item.priority">{{ item.priority }}</b>
              </div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.ticketId }} / {{ item.status }}</p>
              <div class="human-review-showcase-queue-meta">
                <span>{{ item.risk }}</span>
                <span>{{ item.reviewer }}</span>
                <span>{{ item.updatedAt }}</span>
              </div>
            </article>
          </div>
        </aside>

        <section class="human-review-showcase-panel human-review-showcase-detail" aria-label="审核详情">
          <div class="human-review-showcase-panel-heading">
            <div>
              <p class="human-review-showcase-eyebrow">Review Detail</p>
              <h3>审核详情</h3>
            </div>
            <span>DEMO-0005</span>
          </div>

          <div class="human-review-showcase-ticket-summary">
            <div>
              <span>工单标题</span>
              <strong>报价接口返回 500，销售无法生成报价单</strong>
              <p>影响销售运营与客户成功团队，建议人工确认后再推进状态变更。</p>
            </div>
            <div class="human-review-showcase-ticket-badges">
              <span>P1 紧急</span>
              <span>等待审核</span>
              <span>高风险</span>
            </div>
          </div>

          <div class="human-review-showcase-detail-grid">
            <section>
              <span>AI 分类建议</span>
              <strong>系统故障</strong>
              <small>500、traceId、网关日志。</small>
            </section>
            <section>
              <span>优先级判断</span>
              <strong>P1 人工确认</strong>
              <small>收入链路，不允许自动处理。</small>
            </section>
            <section>
              <span>影响范围</span>
              <strong>销售运营 / 客成</strong>
              <small>华东报价中心部分失败。</small>
            </section>
            <section>
              <span>风险原因</span>
              <strong>变更需审批</strong>
              <small>需核对 Trace 与知识引用。</small>
            </section>
          </div>

          <section class="human-review-showcase-draft" aria-label="AI 回复草稿">
            <span>AI 回复草稿</span>
            <p>您好，已收到报价接口异常反馈。当前判断与报价中心后端服务有关，支持人员会先确认灰度发布差异、关联 traceId 与依赖服务日志，再同步处理进度。</p>
          </section>

          <section class="human-review-showcase-comment" aria-label="Reviewer comment">
            <span>审核备注 / Reviewer comment</span>
            <p>请补充网关错误率截图和最近一次发布窗口，确认是否需要请求变更审批。</p>
          </section>
        </section>

        <aside class="human-review-showcase-panel human-review-showcase-evidence" aria-label="证据面板">
          <div class="human-review-showcase-panel-heading">
            <div>
              <p class="human-review-showcase-eyebrow">Evidence Panel</p>
              <h3>证据面板</h3>
            </div>
            <span>attached</span>
          </div>

          <div class="human-review-showcase-evidence-list">
            <section
              v-for="item in evidenceItems"
              :key="item.label"
              :data-tone="item.tone"
              :class="{ 'human-review-showcase-evidence-item--primary': item.primary }"
              class="human-review-showcase-evidence-item"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <p>{{ item.note }}</p>
            </section>
          </div>

          <section class="human-review-showcase-boundary-note">
            <strong>边界说明</strong>
            <p>证据仅用于人工审核，不代表模型调用、工具运行时或无人值守 Agent 能力已落地。</p>
          </section>
        </aside>
      </section>

      <section class="human-review-showcase-decision-panel" aria-label="人工决策区">
        <div class="human-review-showcase-decision-copy">
          <p class="human-review-showcase-eyebrow">Decision Panel</p>
          <h3>人工决策区</h3>
          <span>按钮仅展示 Human Review demo，不会自动回复、关闭或分派工单。</span>
        </div>
        <div class="human-review-showcase-decision-actions">
          <button v-for="decision in decisions" :key="decision.action" :class="`human-review-showcase-decision--${decision.tone}`" type="button">
            <strong>{{ decision.action }}</strong>
            <span>{{ decision.label }}</span>
            <small>{{ decision.note }}</small>
          </button>
        </div>
      </section>

      <section class="human-review-showcase-bottom-grid" aria-label="Review timeline and boundary">
        <article class="human-review-showcase-panel human-review-showcase-timeline">
          <div class="human-review-showcase-panel-heading">
            <div>
              <p class="human-review-showcase-eyebrow">Review Timeline</p>
              <h3>审核记录</h3>
            </div>
          </div>
          <div class="human-review-showcase-timeline-list">
            <section v-for="event in reviewTimeline" :key="`${event.time}-${event.title}`" :data-tone="event.tone" class="human-review-showcase-timeline-item">
              <time>{{ event.time }}</time>
              <div>
                <strong>{{ event.title }}</strong>
                <p>{{ event.note }}</p>
              </div>
            </section>
          </div>
        </article>

        <article class="human-review-showcase-panel human-review-showcase-risk">
          <div class="human-review-showcase-panel-heading">
            <div>
              <p class="human-review-showcase-eyebrow">Risk / Boundary Panel</p>
              <h3>能力边界</h3>
            </div>
          </div>
          <ul>
            <li v-for="note in boundaryNotes" :key="note">{{ note }}</li>
          </ul>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.human-review-showcase {
  --review-canvas: #06101d;
  --review-canvas-deep: #030814;
  --review-sidebar: #071120;
  --review-panel: rgba(12, 23, 38, 0.92);
  --review-panel-strong: rgba(15, 31, 52, 0.94);
  --review-border: rgba(151, 180, 214, 0.16);
  --review-border-strong: rgba(91, 141, 239, 0.24);
  --review-text: #eef5ff;
  --review-soft: #bfd0e7;
  --review-muted: #7f92ad;
  --review-blue: #3d7cff;
  --review-cyan: #22d3ee;
  --review-green: #2bd88f;
  --review-orange: #ffb45c;
  --review-red: #ff5c7a;
  --review-purple: #8b7cf6;
  display: grid;
  grid-template-columns: 224px minmax(0, 1fr);
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
  color: var(--review-text);
  background:
    radial-gradient(circle at 24% 0%, rgba(61, 124, 255, 0.18), transparent 32%),
    radial-gradient(circle at 92% 12%, rgba(34, 211, 238, 0.12), transparent 28%),
    linear-gradient(135deg, #040912 0%, var(--review-canvas) 46%, #071427 100%);
  font-family: Aptos, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  letter-spacing: 0;
}

.human-review-showcase * {
  box-sizing: border-box;
}

.human-review-showcase-sidebar {
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 20px;
  min-height: 100vh;
  border-right: 1px solid rgba(151, 180, 214, 0.12);
  padding: 18px 14px;
  background: linear-gradient(180deg, rgba(8, 17, 31, 0.98), rgba(4, 9, 18, 0.98));
}

.human-review-showcase-brand {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.human-review-showcase-logo {
  width: 44px;
  height: 44px;
  filter: drop-shadow(0 0 14px rgba(61, 124, 255, 0.28));
}

.human-review-showcase-brand strong,
.human-review-showcase-brand span {
  display: block;
}

.human-review-showcase-brand strong {
  font-size: 15px;
  line-height: 1.25;
}

.human-review-showcase-brand span {
  margin-top: 3px;
  color: var(--review-soft);
  font-size: 12px;
  font-weight: 700;
}

.human-review-showcase-nav {
  display: grid;
  align-content: start;
  gap: 6px;
  padding-top: 8px;
}

.human-review-showcase-nav-item {
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 9px 10px;
  color: #a7bad3;
  font-size: 13px;
}

.human-review-showcase-nav-item--active {
  border-color: rgba(61, 124, 255, 0.42);
  color: #ecf6ff;
  background: linear-gradient(135deg, rgba(61, 124, 255, 0.25), rgba(34, 211, 238, 0.1));
  box-shadow: inset 3px 0 0 rgba(34, 211, 238, 0.78);
}

.human-review-showcase-sidebar-card {
  border: 1px solid rgba(61, 124, 255, 0.18);
  border-radius: 8px;
  padding: 13px;
  background: rgba(11, 25, 42, 0.88);
}

.human-review-showcase-sidebar-card strong {
  display: block;
  font-size: 13px;
}

.human-review-showcase-sidebar-card p {
  margin: 7px 0 0;
  color: var(--review-muted);
  font-size: 12px;
  line-height: 1.55;
}

.human-review-showcase-main {
  display: grid;
  grid-template-rows: auto auto auto minmax(0, auto) auto minmax(0, auto);
  gap: 7px;
  min-width: 0;
  height: 100vh;
  overflow: hidden;
  padding: 12px 18px 12px;
}

.human-review-showcase-topbar {
  display: grid;
  grid-template-columns: minmax(360px, 0.65fr) auto;
  gap: 16px;
  align-items: center;
}

.human-review-showcase-search {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 9px;
  align-items: center;
  min-height: 36px;
  border: 1px solid rgba(151, 180, 214, 0.14);
  border-radius: 8px;
  padding: 0 13px;
  background: rgba(3, 8, 18, 0.8);
}

.human-review-showcase-search span {
  color: var(--review-cyan);
  font-size: 14px;
}

.human-review-showcase-search input {
  min-width: 0;
  border: 0;
  outline: 0;
  color: var(--review-text);
  background: transparent;
  font-size: 13px;
}

.human-review-showcase-search input::placeholder {
  color: #7f93ad;
}

.human-review-showcase-topbar-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.human-review-showcase-topbar-status span,
.human-review-showcase-topbar-status strong,
.human-review-showcase-topbar-status em {
  border: 1px solid rgba(151, 180, 214, 0.16);
  border-radius: 8px;
  padding: 7px 10px;
  background: rgba(12, 23, 38, 0.78);
  color: var(--review-soft);
  font-size: 12px;
  font-style: normal;
  line-height: 1;
}

.human-review-showcase-topbar-status span {
  border-color: rgba(43, 216, 143, 0.28);
  color: #b8ffe0;
  background: rgba(43, 216, 143, 0.11);
}

.human-review-showcase-topbar-status strong {
  border-color: rgba(61, 124, 255, 0.3);
  color: #cfe0ff;
}

.human-review-showcase-topbar-status em {
  color: #8fa3bf;
}

.human-review-showcase-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(370px, 0.72fr);
  gap: 10px;
  min-height: 96px;
  border: 1px solid var(--review-border-strong);
  border-radius: 8px;
  padding: 9px 11px;
  background:
    linear-gradient(135deg, rgba(15, 31, 52, 0.94), rgba(6, 16, 29, 0.88)),
    radial-gradient(circle at 82% 20%, rgba(139, 124, 246, 0.16), transparent 42%);
  box-shadow: 0 15px 34px rgba(0, 0, 0, 0.24);
}

.human-review-showcase-hero-copy {
  min-width: 0;
}

.human-review-showcase-eyebrow {
  margin: 0 0 3px;
  color: #8fa3bf;
  font-size: 10.5px;
  font-weight: 800;
  letter-spacing: 0;
}

.human-review-showcase-hero h1 {
  margin: 0;
  color: var(--review-text);
  font-size: 24px;
  line-height: 1.05;
  letter-spacing: 0;
}

.human-review-showcase-hero h2 {
  margin: 4px 0 0;
  color: #dfeaff;
  font-size: 13.5px;
  letter-spacing: 0;
}

.human-review-showcase-hero p {
  max-width: 780px;
  margin: 5px 0 0;
  color: var(--review-soft);
  font-size: 13px;
  line-height: 1.42;
}

.human-review-showcase-hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.human-review-showcase-hero-tags span {
  border: 1px solid rgba(151, 180, 214, 0.18);
  border-radius: 7px;
  padding: 5px 7px;
  color: #c8d8ee;
  background: rgba(5, 13, 25, 0.68);
  font-size: 12px;
  font-weight: 750;
}

.human-review-showcase-status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.human-review-showcase-status-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 2px 8px;
  min-width: 0;
  border: 1px solid rgba(151, 180, 214, 0.14);
  border-radius: 8px;
  padding: 6px 7px;
  background: rgba(4, 11, 22, 0.62);
}

.human-review-showcase-status-card span,
.human-review-showcase-status-card small {
  display: block;
  color: var(--review-muted);
  font-size: 11px;
}

.human-review-showcase-status-card strong,
.human-review-showcase-status-card b {
  display: block;
  overflow-wrap: anywhere;
}

.human-review-showcase-status-card strong {
  margin-top: 1px;
  font-size: 12.5px;
  line-height: 1.2;
}

.human-review-showcase-status-card b {
  margin-top: 1px;
  color: var(--review-text);
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 11px;
  text-align: right;
}

.human-review-showcase-status-card small {
  grid-column: 1 / -1;
}

.human-review-showcase-status-card[data-tone='green'] {
  border-color: rgba(43, 216, 143, 0.24);
}

.human-review-showcase-status-card[data-tone='orange'] {
  border-color: rgba(255, 180, 92, 0.24);
}

.human-review-showcase-status-card[data-tone='blue'] {
  border-color: rgba(61, 124, 255, 0.24);
}

.human-review-showcase-status-card[data-tone='purple'] {
  border-color: rgba(139, 124, 246, 0.24);
}

.human-review-showcase-kpis {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.human-review-showcase-kpi {
  position: relative;
  min-height: 60px;
  overflow: hidden;
  border: 1px solid var(--review-border);
  border-radius: 8px;
  padding: 8px 10px;
  background: rgba(12, 23, 38, 0.83);
}

.human-review-showcase-kpi::before {
  position: absolute;
  inset: 0 0 auto;
  height: 2px;
  content: "";
  background: var(--review-blue);
}

.human-review-showcase-kpi[data-tone='green']::before {
  background: var(--review-green);
}

.human-review-showcase-kpi[data-tone='orange']::before {
  background: var(--review-orange);
}

.human-review-showcase-kpi[data-tone='red']::before {
  background: var(--review-red);
}

.human-review-showcase-kpi[data-tone='purple']::before {
  background: var(--review-purple);
}

.human-review-showcase-kpi span,
.human-review-showcase-kpi small {
  display: block;
  color: var(--review-muted);
  font-size: 11px;
}

.human-review-showcase-kpi strong {
  display: block;
  margin-top: 5px;
  color: var(--review-text);
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 22px;
  line-height: 1;
}

.human-review-showcase-kpi small {
  margin-top: 4px;
}

.human-review-showcase-workspace {
  display: grid;
  grid-template-columns: minmax(268px, 0.7fr) minmax(430px, 1.08fr) minmax(300px, 0.84fr);
  gap: 9px;
  min-height: 0;
}

.human-review-showcase-panel {
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(151, 180, 214, 0.15);
  border-radius: 8px;
  background: var(--review-panel);
}

.human-review-showcase-panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid rgba(151, 180, 214, 0.12);
  padding: 8px 11px;
}

.human-review-showcase-panel-heading h3 {
  margin: 0;
  color: var(--review-text);
  font-size: 15px;
  line-height: 1.15;
  letter-spacing: 0;
}

.human-review-showcase-panel-heading > span {
  border: 1px solid rgba(61, 124, 255, 0.24);
  border-radius: 7px;
  padding: 4px 7px;
  color: #bcd4ff;
  background: rgba(61, 124, 255, 0.1);
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 11px;
}

.human-review-showcase-queue-list,
.human-review-showcase-evidence-list {
  display: grid;
  gap: 6px;
  padding: 7px;
}

.human-review-showcase-evidence-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.human-review-showcase-queue-item {
  border: 1px solid rgba(151, 180, 214, 0.11);
  border-radius: 8px;
  padding: 8px;
  background: rgba(6, 14, 26, 0.64);
}

.human-review-showcase-queue-item--active {
  border-color: rgba(61, 124, 255, 0.5);
  background: linear-gradient(135deg, rgba(61, 124, 255, 0.18), rgba(34, 211, 238, 0.06));
  box-shadow: 0 8px 22px rgba(61, 124, 255, 0.1);
}

.human-review-showcase-queue-top,
.human-review-showcase-queue-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.human-review-showcase-queue-top span,
.human-review-showcase-queue-meta span {
  color: var(--review-muted);
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 11px;
}

.human-review-showcase-queue-top b {
  border-radius: 6px;
  padding: 3px 6px;
  color: #fff5e8;
  background: rgba(255, 180, 92, 0.18);
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 11px;
}

.human-review-showcase-queue-top b[data-priority='P1'] {
  color: #ffd3dc;
  background: rgba(255, 92, 122, 0.18);
}

.human-review-showcase-queue-item strong {
  display: block;
  margin-top: 6px;
  color: var(--review-text);
  font-size: 12.5px;
  line-height: 1.35;
}

.human-review-showcase-queue-item p {
  margin: 4px 0 6px;
  color: var(--review-soft);
  font-size: 12px;
}

.human-review-showcase-ticket-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  border-bottom: 1px solid rgba(151, 180, 214, 0.12);
  padding: 9px 11px;
}

.human-review-showcase-ticket-summary span,
.human-review-showcase-detail-grid span,
.human-review-showcase-draft span,
.human-review-showcase-comment span {
  display: block;
  color: var(--review-muted);
  font-size: 11px;
  font-weight: 800;
}

.human-review-showcase-ticket-summary strong {
  display: block;
  margin-top: 4px;
  color: var(--review-text);
  font-size: 16.5px;
  line-height: 1.2;
}

.human-review-showcase-ticket-summary p {
  margin: 5px 0 0;
  color: var(--review-soft);
  font-size: 12px;
  line-height: 1.35;
}

.human-review-showcase-ticket-badges {
  display: grid;
  align-content: start;
  gap: 5px;
}

.human-review-showcase-ticket-badges span {
  border: 1px solid rgba(255, 180, 92, 0.22);
  border-radius: 7px;
  padding: 5px 7px;
  color: #ffe0b7;
  background: rgba(255, 180, 92, 0.1);
  text-align: center;
  white-space: nowrap;
}

.human-review-showcase-detail-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  padding: 8px 10px;
}

.human-review-showcase-detail-grid section {
  border: 1px solid rgba(151, 180, 214, 0.1);
  border-radius: 8px;
  padding: 7px;
  background: rgba(4, 12, 23, 0.52);
}

.human-review-showcase-detail-grid strong {
  display: block;
  margin-top: 4px;
  color: var(--review-text);
  font-size: 12px;
  line-height: 1.2;
}

.human-review-showcase-detail-grid small {
  display: block;
  margin-top: 4px;
  color: var(--review-muted);
  font-size: 10.5px;
  line-height: 1.3;
}

.human-review-showcase-draft,
.human-review-showcase-comment {
  margin: 0 10px 7px;
  border: 1px solid rgba(151, 180, 214, 0.12);
  border-radius: 8px;
  padding: 7px 8px;
  background: rgba(6, 14, 26, 0.58);
}

.human-review-showcase-draft p,
.human-review-showcase-comment p {
  margin: 5px 0 0;
  color: var(--review-soft);
  font-size: 11.5px;
  line-height: 1.38;
}

.human-review-showcase-comment {
  border-color: rgba(34, 211, 238, 0.18);
}

.human-review-showcase-evidence-item {
  border: 1px solid rgba(151, 180, 214, 0.11);
  border-left: 2px solid var(--review-blue);
  border-radius: 8px;
  padding: 7px 8px;
  background: rgba(4, 12, 23, 0.58);
}

.human-review-showcase-evidence-item--primary {
  border-color: rgba(91, 141, 239, 0.2);
  background: linear-gradient(135deg, rgba(61, 124, 255, 0.12), rgba(4, 12, 23, 0.62));
}

.human-review-showcase-evidence-item[data-tone='green'] {
  border-left-color: var(--review-green);
}

.human-review-showcase-evidence-item[data-tone='orange'] {
  border-left-color: var(--review-orange);
}

.human-review-showcase-evidence-item[data-tone='purple'] {
  border-left-color: var(--review-purple);
}

.human-review-showcase-evidence-item span {
  color: var(--review-muted);
  font-size: 11px;
  font-weight: 800;
}

.human-review-showcase-evidence-item strong {
  display: block;
  margin-top: 3px;
  color: var(--review-text);
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 11px;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.human-review-showcase-evidence-item p,
.human-review-showcase-boundary-note p {
  margin: 3px 0 0;
  color: var(--review-muted);
  font-size: 10.5px;
  line-height: 1.34;
}

.human-review-showcase-boundary-note {
  margin: 0 7px 7px;
  border: 1px solid rgba(255, 180, 92, 0.16);
  border-radius: 8px;
  padding: 7px 8px;
  background: rgba(255, 180, 92, 0.06);
}

.human-review-showcase-boundary-note strong {
  color: #ffd9aa;
  font-size: 12px;
}

.human-review-showcase-decision-panel {
  display: grid;
  grid-template-columns: minmax(230px, 0.42fr) minmax(0, 1fr);
  gap: 9px;
  align-items: stretch;
  border: 1px solid rgba(151, 180, 214, 0.15);
  border-radius: 8px;
  padding: 9px;
  background: rgba(12, 23, 38, 0.83);
}

.human-review-showcase-decision-copy h3 {
  margin: 0;
  font-size: 16px;
}

.human-review-showcase-decision-copy span {
  display: block;
  margin-top: 5px;
  color: var(--review-muted);
  font-size: 12px;
  line-height: 1.5;
}

.human-review-showcase-decision-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.human-review-showcase-decision-actions button {
  border: 1px solid rgba(151, 180, 214, 0.14);
  border-radius: 8px;
  padding: 9px;
  color: var(--review-text);
  background: rgba(5, 13, 25, 0.62);
  cursor: default;
  text-align: left;
}

.human-review-showcase-decision-actions strong,
.human-review-showcase-decision-actions span,
.human-review-showcase-decision-actions small {
  display: block;
}

.human-review-showcase-decision-actions strong {
  font-size: 13px;
}

.human-review-showcase-decision-actions span {
  margin-top: 4px;
  font-size: 12px;
  font-weight: 800;
}

.human-review-showcase-decision-actions small {
  margin-top: 4px;
  color: var(--review-muted);
  font-size: 11px;
  line-height: 1.45;
}

.human-review-showcase-decision--approve {
  border-color: rgba(43, 216, 143, 0.28) !important;
  background: linear-gradient(135deg, rgba(43, 216, 143, 0.16), rgba(5, 13, 25, 0.62)) !important;
  box-shadow: inset 3px 0 0 rgba(43, 216, 143, 0.68);
}

.human-review-showcase-decision--changes {
  border-color: rgba(255, 180, 92, 0.3) !important;
  background: linear-gradient(135deg, rgba(255, 180, 92, 0.16), rgba(5, 13, 25, 0.62)) !important;
}

.human-review-showcase-decision--reject {
  border-color: rgba(255, 92, 122, 0.3) !important;
  background: linear-gradient(135deg, rgba(255, 92, 122, 0.14), rgba(5, 13, 25, 0.62)) !important;
}

.human-review-showcase-bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.7fr);
  gap: 9px;
  min-height: 0;
}

.human-review-showcase-bottom-grid .human-review-showcase-panel {
  border-color: rgba(151, 180, 214, 0.1);
  background: rgba(10, 20, 34, 0.78);
}

.human-review-showcase-timeline-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  padding: 0 9px 9px;
}

.human-review-showcase-timeline-item {
  position: relative;
  min-height: 86px;
  border: 1px solid rgba(151, 180, 214, 0.1);
  border-radius: 8px;
  padding: 9px;
  background: rgba(4, 12, 23, 0.5);
}

.human-review-showcase-timeline-item::before {
  position: absolute;
  top: 14px;
  right: 12px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  content: "";
  background: var(--review-blue);
}

.human-review-showcase-timeline-item[data-tone='success']::before {
  background: var(--review-green);
}

.human-review-showcase-timeline-item[data-tone='warning']::before {
  background: var(--review-orange);
}

.human-review-showcase-timeline-item time {
  color: var(--review-cyan);
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 11px;
}

.human-review-showcase-timeline-item strong {
  display: block;
  margin-top: 7px;
  color: var(--review-text);
  font-size: 12px;
}

.human-review-showcase-timeline-item p {
  margin: 4px 0 0;
  color: var(--review-muted);
  font-size: 11px;
  line-height: 1.45;
}

.human-review-showcase-risk {
  border-color: rgba(151, 180, 214, 0.12);
}

.human-review-showcase-risk ul {
  display: grid;
  gap: 5px;
  margin: 0;
  padding: 0 12px 11px;
  list-style: none;
}

.human-review-showcase-risk li {
  position: relative;
  padding-left: 14px;
  color: var(--review-soft);
  font-size: 11.5px;
  line-height: 1.36;
}

.human-review-showcase-risk li::before {
  position: absolute;
  top: 8px;
  left: 0;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  content: "";
  background: var(--review-cyan);
}

@media (max-width: 1180px) {
  .human-review-showcase {
    grid-template-columns: 1fr;
    overflow: auto;
  }

  .human-review-showcase-sidebar {
    display: none;
  }

  .human-review-showcase-main {
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .human-review-showcase-hero,
  .human-review-showcase-workspace,
  .human-review-showcase-decision-panel,
  .human-review-showcase-bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .human-review-showcase-main {
    padding: 12px;
  }

  .human-review-showcase-topbar,
  .human-review-showcase-kpis,
  .human-review-showcase-status-grid,
  .human-review-showcase-detail-grid,
  .human-review-showcase-decision-actions,
  .human-review-showcase-timeline-list {
    grid-template-columns: 1fr;
  }

  .human-review-showcase-evidence-list {
    grid-template-columns: 1fr;
  }

  .human-review-showcase-ticket-summary {
    grid-template-columns: 1fr;
  }
}
</style>
