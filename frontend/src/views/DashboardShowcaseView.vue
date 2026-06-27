<script setup lang="ts">
interface KpiCard {
  label: string
  value: string
  note: string
  trend: string
  tone: 'blue' | 'green' | 'orange' | 'red' | 'purple' | 'cyan'
}

interface AgentRun {
  ticketId: string
  title: string
  category: string
  provider: string
  fallback: string
  review: string
  latency: string
  trace: string
}

interface ProviderStatus {
  name: string
  state: string
  latency: string
  note: string
  tone: 'online' | 'fallback' | 'idle'
}

interface PromptTemplate {
  name: string
  version: string
  owner: string
  boundary: string
}

interface WorkflowStep {
  label: string
  title: string
  detail: string
  meta: string
}

interface KnowledgeReference {
  title: string
  source: string
  score: string
  note: string
}

interface HumanReview {
  action: string
  ticketId: string
  reviewer: string
  state: string
  tone: 'approve' | 'changes' | 'reject' | 'required'
}

interface ActivityItem {
  time: string
  title: string
  detail: string
  tone: 'info' | 'success' | 'warning' | 'danger'
}

const navItems = [
  '仪表盘',
  '工单管理',
  '知识库',
  'RAG 引用',
  'Prompt 模板',
  'Provider 健康',
  'Trace 证据',
  'Human Review',
  '演示设置'
]

const kpis: KpiCard[] = [
  { label: '今日运行', value: '128', note: '演示运行统计', trend: '09:00 后新增 14 次', tone: 'blue' },
  { label: '成功率', value: '96.4%', note: '规则路径完成率', trend: '不代表生产指标', tone: 'green' },
  { label: '平均延迟', value: '412 ms', note: '本地演示中位耗时', trend: '模板与检索汇总', tone: 'cyan' },
  { label: '人工审核', value: '23', note: '等待支持人员处理', trend: '7 条待确认', tone: 'orange' },
  { label: '知识命中', value: '82%', note: '关键词引用命中', trend: '关键词检索口径', tone: 'purple' },
  { label: 'Fallback 率', value: '100%', note: '未配置调用密钥', trend: 'local-rule fallback', tone: 'red' }
]

const agentRuns: AgentRun[] = [
  {
    ticketId: 'DEMO-0005',
    title: '企业报价流程中报价接口返回 500',
    category: '系统故障',
    provider: '本地规则',
    fallback: '密钥缺失',
    review: '待审核',
    latency: '389 ms',
    trace: 'trace-demo-0005'
  },
  {
    ticketId: 'DEMO-0003',
    title: '缓存连接池耗尽导致登录超时',
    category: '系统故障',
    provider: '本地规则',
    fallback: 'Provider 已禁用',
    review: '审核中',
    latency: '427 ms',
    trace: 'trace-demo-0003'
  },
  {
    ticketId: 'DEMO-0007',
    title: '供应商接入策略阻断测试环境访问',
    category: '权限问题',
    provider: '本地规则',
    fallback: '基础地址缺失',
    review: '待审核',
    latency: '352 ms',
    trace: 'trace-demo-0007'
  },
  {
    ticketId: 'DEMO-0004',
    title: '客户列表查询耗时超过 12 秒',
    category: '数据问题',
    provider: '本地规则',
    fallback: '无',
    review: '已批准',
    latency: '318 ms',
    trace: 'trace-demo-0004'
  },
  {
    ticketId: 'DEMO-0006',
    title: '报表权限审批后仍无法访问',
    category: '权限问题',
    provider: '本地规则',
    fallback: '解析异常示例',
    review: '请求修改',
    latency: '466 ms',
    trace: 'trace-demo-0006'
  }
]

const providers: ProviderStatus[] = [
  { name: 'local-rule fallback', state: '运行中', latency: '38 ms', note: '默认演示路径', tone: 'fallback' },
  { name: 'OpenAI-compatible', state: '环境变量控制', latency: '未调用', note: '需临时环境变量', tone: 'idle' },
  { name: 'Human Review', state: '已启用', latency: '人工处理', note: '不会自动关闭', tone: 'online' },
  { name: 'Trace 证据', state: '已启用', latency: '只读聚合', note: '汇总审计记录', tone: 'online' }
]

const prompts: PromptTemplate[] = [
  { name: '工单分类 Prompt', version: 'v1.8', owner: '支持运营', boundary: '仅解释规则判断' },
  { name: '回复草稿 Prompt', version: 'v2.3', owner: '服务台', boundary: '生成草稿，不自动发送' },
  { name: '风险复核 Prompt', version: 'v1.1', owner: '变更审核', boundary: '人工决策前置' },
  { name: '知识引用 Prompt', version: 'v1.4', owner: '知识运营', boundary: '关键词检索来源' }
]

const workflow: WorkflowStep[] = [
  { label: '01', title: 'Prompt', detail: '标题、日志与上下文进入模板。', meta: '结构化演示输入' },
  { label: '02', title: 'Provider', detail: '默认走 local-rule fallback，扩展由环境变量控制。', meta: '可选扩展' },
  { label: '03', title: 'Tool Call', detail: '只展示检索与工单上下文证据。', meta: '非完整运行时' },
  { label: '04', title: 'Trace', detail: '记录生成、状态与引用证据。', meta: '只读聚合' },
  { label: '05', title: 'Human Review', detail: '人工确认后才允许变更。', meta: '人工门禁' }
]

const knowledgeReferences: KnowledgeReference[] = [
  { title: '网关超时处理指南', source: '知识库 / 网关超时', score: '88%', note: '关键词检索 / RAG 视图' },
  { title: '订单服务故障处置手册', source: '运维手册 / 订单服务', score: '81%', note: '关联相似工单' },
  { title: '客户回复模板', source: '支持模板 / 客户通知', score: '76%', note: '草稿来源，需人工确认' }
]

const reviews: HumanReview[] = [
  { action: '批准', ticketId: 'DEMO-0004', reviewer: '支持组长', state: '知识草稿已确认', tone: 'approve' },
  { action: '请求修改', ticketId: 'DEMO-0006', reviewer: '权限负责人', state: '需补充审批证据', tone: 'changes' },
  { action: '拒绝', ticketId: 'DEMO-0011', reviewer: '事件经理', state: '回滚建议风险过高', tone: 'reject' },
  { action: '待审核', ticketId: 'DEMO-0005', reviewer: '二线支持', state: '一级报价流程影响', tone: 'required' }
]

const activity: ActivityItem[] = [
  { time: '11:42', title: 'Provider fallback 已触发', detail: 'OpenAI-compatible 未配置，已使用 local-rule fallback。', tone: 'warning' },
  { time: '11:38', title: '已请求 Human Review', detail: 'DEMO-0005 的回复草稿使用前需要人工批准。', tone: 'info' },
  { time: '11:31', title: 'Trace 证据已记录', detail: '生成记录与状态历史已关联到审核轨迹。', tone: 'success' },
  { time: '11:18', title: '已引用知识条目', detail: '网关超时处理指南通过关键词检索命中。', tone: 'success' },
  { time: '11:05', title: '工单状态已更新', detail: 'DEMO-0003 已由待处理转为人工审核中。', tone: 'info' }
]
</script>

<template>
  <div class="dashboard-showcase" data-screenshot="dashboard">
    <aside class="dashboard-showcase-sidebar" aria-label="工作区导航">
      <div class="dashboard-showcase-brand" aria-label="企业工单 RAG Copilot">
        <svg class="dashboard-showcase-brand-mark" viewBox="0 0 44 44" role="img" aria-label="企业 AI SaaS 品牌图标">
          <defs>
            <linearGradient id="dashboard-showcase-logo-fill" x1="9" y1="7" x2="35" y2="38" gradientUnits="userSpaceOnUse">
              <stop offset="0" stop-color="#21c7d9" />
              <stop offset="0.48" stop-color="#3d7cff" />
              <stop offset="1" stop-color="#8b7cf6" />
            </linearGradient>
            <linearGradient id="dashboard-showcase-logo-line" x1="12" y1="10" x2="32" y2="34" gradientUnits="userSpaceOnUse">
              <stop offset="0" stop-color="#eef5ff" />
              <stop offset="1" stop-color="#9fc4ff" />
            </linearGradient>
            <filter id="dashboard-showcase-logo-glow" x="-35%" y="-35%" width="170%" height="170%">
              <feGaussianBlur stdDeviation="2.4" result="blur" />
              <feColorMatrix in="blur" type="matrix" values="0 0 0 0 0.24 0 0 0 0 0.48 0 0 0 0 1 0 0 0 0.58 0" result="glow" />
              <feMerge>
                <feMergeNode in="glow" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
          </defs>
          <path
            d="M22 4.5 37.2 13.3v17.4L22 39.5 6.8 30.7V13.3L22 4.5Z"
            fill="rgba(8, 17, 31, 0.92)"
            stroke="url(#dashboard-showcase-logo-fill)"
            stroke-width="1.9"
            filter="url(#dashboard-showcase-logo-glow)"
          />
          <path d="M22 10.8 31.6 16.3v11.1L22 33.2l-9.6-5.8V16.3L22 10.8Z" fill="url(#dashboard-showcase-logo-fill)" opacity="0.26" />
          <path d="M22 10.8v11.1m0 0 9.6-5.6M22 21.9l-9.6-5.6m0 0v11.1L22 33.2l9.6-5.8V16.3" fill="none" stroke="url(#dashboard-showcase-logo-line)" stroke-width="1.55" stroke-linecap="round" stroke-linejoin="round" />
          <path d="M16.7 24.8 22 27.9l5.3-3.1" fill="none" stroke="#21c7d9" stroke-width="1.25" stroke-linecap="round" stroke-linejoin="round" opacity="0.9" />
          <circle cx="22" cy="10.8" r="2.35" fill="#21c7d9" />
          <circle cx="12.4" cy="16.3" r="2.2" fill="#9fc4ff" />
          <circle cx="31.6" cy="16.3" r="2.2" fill="#8b7cf6" />
          <circle cx="22" cy="33.2" r="2.35" fill="#eef5ff" />
        </svg>
        <div>
          <strong>企业工单</strong>
          <span>RAG Copilot</span>
        </div>
      </div>

      <nav class="dashboard-showcase-nav" aria-label="主导航">
        <button
          v-for="item in navItems"
          :key="item"
          class="dashboard-showcase-nav-item"
          :class="{ 'dashboard-showcase-nav-item--active': item === '仪表盘' }"
          type="button"
        >
          <span aria-hidden="true"></span>
          {{ item }}
        </button>
      </nav>

      <section class="dashboard-showcase-boundary" aria-label="能力边界">
        <span>作品集演示</span>
        <strong>无生产自动化</strong>
        <p>演示数据、local-rule fallback，人工审核必需。</p>
      </section>
    </aside>

    <section class="dashboard-showcase-main">
      <header class="dashboard-showcase-topbar">
        <label class="dashboard-showcase-search">
          <span>搜索</span>
          <input type="search" placeholder="搜索工单、Trace、Prompt、知识库..." aria-label="搜索演示工作区" readonly />
        </label>
        <div class="dashboard-showcase-topbar-actions" aria-label="运行状态">
          <span class="dashboard-showcase-chip dashboard-showcase-chip--green">演示环境</span>
          <span class="dashboard-showcase-chip dashboard-showcase-chip--blue">Provider: local-rule fallback</span>
          <span class="dashboard-showcase-user">管理员演示</span>
        </div>
      </header>

      <main class="dashboard-showcase-content">
        <section class="dashboard-showcase-hero" aria-label="仪表盘概览">
          <div class="dashboard-showcase-hero-copy">
            <p class="dashboard-showcase-eyebrow">智能工单工作流控制台</p>
            <h1>企业工单 RAG Copilot</h1>
            <p>统一展示 Prompt、Provider、Trace、知识引用与 Human Review 的演示工作流。</p>
            <div class="dashboard-showcase-hero-tags" aria-label="能力标签">
              <span>local-rule fallback</span>
              <span>OpenAI-compatible 可选</span>
              <span>Trace 证据</span>
              <span>Human Review 门禁</span>
            </div>
          </div>

          <div class="dashboard-showcase-system-card" aria-label="系统状态">
            <div>
              <header>
                <span>Provider</span>
                <em>运行中</em>
              </header>
              <strong>local-rule fallback</strong>
              <small>默认路径。此截图不包含真实模型调用。</small>
            </div>
            <div>
              <header>
                <span>扩展能力</span>
                <em>可选扩展</em>
              </header>
              <strong>OpenAI-compatible</strong>
              <small>仅通过临时环境变量配置。</small>
            </div>
            <div>
              <header>
                <span>审核门禁</span>
                <em>人工门禁</em>
              </header>
              <strong>人工确认</strong>
              <small>不会自动回复、分派、回滚或关闭工单。</small>
            </div>
          </div>
        </section>

        <section class="dashboard-showcase-kpi-grid" aria-label="仪表盘 KPI">
          <article v-for="kpi in kpis" :key="kpi.label" class="dashboard-showcase-kpi" :data-tone="kpi.tone">
            <span>{{ kpi.label }}</span>
            <strong>{{ kpi.value }}</strong>
            <p>{{ kpi.note }}</p>
            <small>{{ kpi.trend }}</small>
          </article>
        </section>

        <section class="dashboard-showcase-grid dashboard-showcase-grid--primary">
          <article class="dashboard-showcase-panel dashboard-showcase-panel--runs" aria-label="最近智能体运行">
            <div class="dashboard-showcase-panel-header">
              <div>
                <p class="dashboard-showcase-eyebrow">最近智能体运行</p>
                <h2>工单工作流记录</h2>
              </div>
              <span>演示证据</span>
            </div>
            <div class="dashboard-showcase-runs-table" role="table" aria-label="最近智能体运行">
              <div class="dashboard-showcase-runs-head" role="row">
                <span>工单</span>
                <span>分类</span>
                <span>Provider</span>
                <span>Fallback</span>
                <span>审核</span>
                <span>Trace</span>
              </div>
              <div v-for="run in agentRuns" :key="run.ticketId" class="dashboard-showcase-run-row" role="row">
                <div>
                  <strong class="dashboard-showcase-ticket-link">{{ run.ticketId }}</strong>
                  <small>{{ run.title }}</small>
                </div>
                <span class="dashboard-showcase-run-badge dashboard-showcase-run-badge--category">{{ run.category }}</span>
                <span class="dashboard-showcase-run-badge dashboard-showcase-run-badge--provider">{{ run.provider }}</span>
                <span class="dashboard-showcase-run-badge dashboard-showcase-run-badge--fallback">{{ run.fallback }}</span>
                <span class="dashboard-showcase-run-badge dashboard-showcase-run-badge--review">{{ run.review }}</span>
                <span class="dashboard-showcase-trace-link">
                  <b>查看 Trace</b>
                  <small>{{ run.trace }} · {{ run.latency }}</small>
                </span>
              </div>
            </div>
          </article>

          <article class="dashboard-showcase-panel dashboard-showcase-panel--workflow" aria-label="工作流概览">
            <div class="dashboard-showcase-panel-header">
              <div>
                <p class="dashboard-showcase-eyebrow">工作流概览</p>
                <h2>Prompt 到审核路径</h2>
              </div>
              <span>只读演示</span>
            </div>
            <div class="dashboard-showcase-workflow">
              <section v-for="step in workflow" :key="step.label" class="dashboard-showcase-workflow-step">
                <span>{{ step.label }}</span>
                <strong>{{ step.title }}</strong>
                <p>{{ step.detail }}</p>
                <small>{{ step.meta }}</small>
              </section>
            </div>
            <p class="dashboard-showcase-workflow-note">仅展示审核路径与证据视图，不代表自动执行。</p>
          </article>
        </section>

        <section class="dashboard-showcase-grid dashboard-showcase-grid--secondary">
          <article class="dashboard-showcase-panel" aria-label="Provider 健康">
            <div class="dashboard-showcase-panel-header">
              <div>
                <p class="dashboard-showcase-eyebrow">Provider 健康</p>
                <h2>Fallback 与扩展边界</h2>
              </div>
            </div>
            <div class="dashboard-showcase-provider-list">
              <section v-for="provider in providers" :key="provider.name" class="dashboard-showcase-provider" :data-tone="provider.tone">
                <div>
                  <strong>{{ provider.name }}</strong>
                  <span>{{ provider.note }}</span>
                </div>
                <em>{{ provider.state }}</em>
                <small>{{ provider.latency }}</small>
              </section>
            </div>
          </article>

          <article class="dashboard-showcase-panel" aria-label="活跃 Prompt 模板">
            <div class="dashboard-showcase-panel-header">
              <div>
                <p class="dashboard-showcase-eyebrow">活跃 Prompt 模板</p>
                <h2>草稿来源</h2>
              </div>
            </div>
            <div class="dashboard-showcase-template-list">
              <section v-for="template in prompts" :key="template.name" class="dashboard-showcase-template">
                <strong>{{ template.name }}</strong>
                <span>{{ template.version }} / {{ template.owner }}</span>
                <small>{{ template.boundary }}</small>
              </section>
            </div>
          </article>

          <article class="dashboard-showcase-panel" aria-label="知识引用">
            <div class="dashboard-showcase-panel-header">
              <div>
                <p class="dashboard-showcase-eyebrow">知识引用</p>
                <h2>关键词检索 / RAG 视图</h2>
              </div>
            </div>
            <div class="dashboard-showcase-knowledge-list">
              <section v-for="reference in knowledgeReferences" :key="reference.title" class="dashboard-showcase-knowledge">
                <div>
                  <strong>{{ reference.title }}</strong>
                  <span>{{ reference.source }}</span>
                  <small>{{ reference.note }}</small>
                </div>
                <b>{{ reference.score }}</b>
              </section>
            </div>
          </article>

          <article class="dashboard-showcase-panel" aria-label="最近人工审核">
            <div class="dashboard-showcase-panel-header">
              <div>
                <p class="dashboard-showcase-eyebrow">最近人工审核</p>
                <h2>人工决策队列</h2>
              </div>
            </div>
            <div class="dashboard-showcase-review-list">
              <section v-for="review in reviews" :key="`${review.action}-${review.ticketId}`" class="dashboard-showcase-review" :data-tone="review.tone">
                <span>{{ review.action }}</span>
                <div>
                  <strong>{{ review.ticketId }}</strong>
                  <small>{{ review.reviewer }} / {{ review.state }}</small>
                </div>
              </section>
            </div>
          </article>

          <article class="dashboard-showcase-panel dashboard-showcase-panel--timeline" aria-label="最近活动">
            <div class="dashboard-showcase-panel-header">
              <div>
                <p class="dashboard-showcase-eyebrow">最近活动</p>
                <h2>审计友好的操作轨迹</h2>
              </div>
            </div>
            <ol class="dashboard-showcase-timeline">
              <li v-for="item in activity" :key="`${item.time}-${item.title}`" :data-tone="item.tone">
                <time>{{ item.time }}</time>
                <div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.detail }}</p>
                </div>
              </li>
            </ol>
          </article>
        </section>
      </main>
    </section>
  </div>
</template>

<style scoped>
.dashboard-showcase {
  --dashboard-showcase-canvas: #07101d;
  --dashboard-showcase-canvas-deep: #040912;
  --dashboard-showcase-sidebar: #08111f;
  --dashboard-showcase-panel: #0c1726;
  --dashboard-showcase-panel-strong: #101f33;
  --dashboard-showcase-border: rgba(151, 180, 214, 0.16);
  --dashboard-showcase-border-strong: rgba(91, 141, 239, 0.26);
  --dashboard-showcase-text: #eef5ff;
  --dashboard-showcase-secondary: #c7d5ea;
  --dashboard-showcase-muted: #7e91ab;
  --dashboard-showcase-blue: #3d7cff;
  --dashboard-showcase-cyan: #21c7d9;
  --dashboard-showcase-green: #2bd88f;
  --dashboard-showcase-orange: #ffb45c;
  --dashboard-showcase-red: #ff5c7a;
  --dashboard-showcase-purple: #8b7cf6;
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  min-height: 100vh;
  background:
    radial-gradient(circle at 55% -10%, rgba(61, 124, 255, 0.24), transparent 30%),
    linear-gradient(180deg, var(--dashboard-showcase-canvas), var(--dashboard-showcase-canvas-deep));
  color: var(--dashboard-showcase-text);
  font-family: Aptos, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.dashboard-showcase *,
.dashboard-showcase *::before,
.dashboard-showcase *::after {
  box-sizing: border-box;
}

.dashboard-showcase-sidebar {
  position: sticky;
  top: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 16px;
  height: 100vh;
  padding: 16px 12px;
  border-right: 1px solid var(--dashboard-showcase-border);
  background:
    linear-gradient(180deg, rgba(8, 17, 31, 0.96), rgba(4, 9, 18, 0.98)),
    var(--dashboard-showcase-sidebar);
}

.dashboard-showcase-brand {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 11px;
  align-items: center;
  min-height: 46px;
}

.dashboard-showcase-brand-mark {
  display: block;
  width: 42px;
  height: 42px;
  overflow: visible;
  filter: drop-shadow(0 0 18px rgba(61, 124, 255, 0.3));
}

.dashboard-showcase-brand strong,
.dashboard-showcase-brand span {
  display: block;
}

.dashboard-showcase-brand strong {
  color: var(--dashboard-showcase-text);
  font-size: 16px;
  font-weight: 950;
  line-height: 1.14;
}

.dashboard-showcase-brand span {
  margin-top: 2px;
  color: #d5e4fb;
  font-size: 13px;
  font-weight: 750;
}

.dashboard-showcase-nav {
  display: grid;
  align-content: start;
  gap: 7px;
  min-height: 0;
  overflow: auto;
}

.dashboard-showcase-nav-item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 38px;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 0 10px;
  background: transparent;
  color: var(--dashboard-showcase-secondary);
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  text-align: left;
  cursor: pointer;
}

.dashboard-showcase-nav-item span {
  width: 8px;
  height: 8px;
  border-radius: 3px;
  background: rgba(126, 145, 171, 0.55);
}

.dashboard-showcase-nav-item--active {
  border-color: rgba(61, 124, 255, 0.42);
  background: linear-gradient(180deg, rgba(61, 124, 255, 0.28), rgba(61, 124, 255, 0.11));
  color: var(--dashboard-showcase-text);
  box-shadow: 0 12px 28px rgba(61, 124, 255, 0.15);
}

.dashboard-showcase-nav-item--active span {
  background: var(--dashboard-showcase-blue);
  box-shadow: 0 0 0 4px rgba(61, 124, 255, 0.16);
}

.dashboard-showcase-boundary {
  display: grid;
  gap: 7px;
  padding: 13px;
  border: 1px solid rgba(151, 180, 214, 0.14);
  border-radius: 8px;
  background: rgba(4, 10, 22, 0.64);
}

.dashboard-showcase-boundary span,
.dashboard-showcase-eyebrow {
  color: #8db9ff;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
}

.dashboard-showcase-boundary strong {
  font-size: 13px;
}

.dashboard-showcase-boundary p {
  margin: 0;
  color: var(--dashboard-showcase-muted);
  font-size: 12px;
  line-height: 1.45;
}

.dashboard-showcase-main {
  min-width: 0;
}

.dashboard-showcase-topbar {
  position: sticky;
  top: 0;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(260px, 520px) minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  min-height: 56px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--dashboard-showcase-border);
  background: rgba(5, 11, 22, 0.88);
  backdrop-filter: blur(18px);
}

.dashboard-showcase-search {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 40px;
  border: 1px solid var(--dashboard-showcase-border);
  border-radius: 8px;
  padding: 0 12px;
  background: rgba(10, 21, 38, 0.72);
}

.dashboard-showcase-search span {
  color: var(--dashboard-showcase-muted);
  font-size: 12px;
  font-weight: 900;
}

.dashboard-showcase-search input {
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--dashboard-showcase-secondary);
}

.dashboard-showcase-search input::placeholder {
  color: var(--dashboard-showcase-secondary);
  opacity: 0.86;
}

.dashboard-showcase-topbar-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
}

.dashboard-showcase-chip,
.dashboard-showcase-user {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  border: 1px solid var(--dashboard-showcase-border);
  border-radius: 8px;
  padding: 0 10px;
  background: rgba(10, 21, 38, 0.74);
  color: var(--dashboard-showcase-secondary);
  font-size: 12px;
  font-weight: 900;
}

.dashboard-showcase-user {
  border-color: rgba(151, 180, 214, 0.1);
  background: rgba(10, 21, 38, 0.42);
  color: rgba(199, 213, 234, 0.72);
  font-weight: 800;
}

.dashboard-showcase-chip--green {
  border-color: rgba(43, 216, 143, 0.26);
  color: var(--dashboard-showcase-green);
}

.dashboard-showcase-chip--blue {
  border-color: rgba(61, 124, 255, 0.28);
  color: #9fc4ff;
}

.dashboard-showcase-content {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 9px 12px;
}

.dashboard-showcase-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(420px, 0.9fr);
  gap: 11px;
  min-height: 128px;
  border: 1px solid rgba(91, 141, 239, 0.26);
  border-radius: 8px;
  padding: 12px 16px;
  overflow: hidden;
  background:
    radial-gradient(circle at 78% 32%, rgba(139, 124, 246, 0.28), transparent 27%),
    linear-gradient(135deg, rgba(16, 31, 51, 0.94), rgba(8, 17, 31, 0.92));
  box-shadow: 0 22px 54px rgba(0, 0, 0, 0.34);
}

.dashboard-showcase-hero-copy {
  min-width: 0;
}

.dashboard-showcase-eyebrow {
  margin: 0 0 6px;
}

.dashboard-showcase h1,
.dashboard-showcase h2,
.dashboard-showcase p {
  letter-spacing: 0;
}

.dashboard-showcase h1 {
  margin: 0;
  color: var(--dashboard-showcase-text);
  font-size: 35px;
  line-height: 1.05;
}

.dashboard-showcase-hero-copy > p:not(.dashboard-showcase-eyebrow) {
  max-width: 680px;
  margin: 7px 0 0;
  color: var(--dashboard-showcase-secondary);
  font-size: 14px;
  line-height: 1.42;
}

.dashboard-showcase-hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 9px;
}

.dashboard-showcase-hero-tags span {
  min-height: 26px;
  border: 1px solid rgba(91, 141, 239, 0.22);
  border-radius: 7px;
  padding: 5px 9px;
  background: rgba(4, 10, 22, 0.48);
  color: var(--dashboard-showcase-secondary);
  font-size: 12px;
  font-weight: 800;
}

.dashboard-showcase-system-card {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  align-content: stretch;
}

.dashboard-showcase-system-card div {
  display: grid;
  gap: 5px;
  min-width: 0;
  border: 1px solid rgba(151, 180, 214, 0.12);
  border-radius: 8px;
  padding: 9px 10px;
  background:
    linear-gradient(180deg, rgba(14, 27, 47, 0.8), rgba(4, 10, 22, 0.62)),
    rgba(4, 10, 22, 0.56);
}

.dashboard-showcase-system-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.dashboard-showcase-system-card header span,
.dashboard-showcase-system-card small {
  color: var(--dashboard-showcase-muted);
  font-size: 11px;
  line-height: 1.35;
}

.dashboard-showcase-system-card em {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border-radius: 999px;
  padding: 3px 7px;
  background: rgba(33, 199, 217, 0.09);
  color: #9fe8f1;
  font-size: 10px;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.dashboard-showcase-system-card em::before {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  content: '';
  background: currentColor;
  box-shadow: 0 0 10px currentColor;
}

.dashboard-showcase-system-card strong {
  color: var(--dashboard-showcase-text);
  font-size: 13px;
  line-height: 1.3;
}

.dashboard-showcase-kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
}

.dashboard-showcase-kpi {
  position: relative;
  display: grid;
  gap: 5px;
  min-height: 84px;
  border: 1px solid var(--dashboard-showcase-border);
  border-radius: 8px;
  padding: 10px 12px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(16, 31, 51, 0.9), rgba(9, 18, 32, 0.96));
  box-shadow: inset 0 1px 0 rgba(238, 245, 255, 0.04);
}

.dashboard-showcase-kpi::before {
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  content: '';
  background: var(--dashboard-showcase-blue);
}

.dashboard-showcase-kpi[data-tone='green']::before {
  background: var(--dashboard-showcase-green);
}

.dashboard-showcase-kpi[data-tone='orange']::before {
  background: var(--dashboard-showcase-orange);
}

.dashboard-showcase-kpi[data-tone='red']::before {
  background: var(--dashboard-showcase-red);
}

.dashboard-showcase-kpi[data-tone='purple']::before {
  background: var(--dashboard-showcase-purple);
}

.dashboard-showcase-kpi[data-tone='cyan']::before {
  background: var(--dashboard-showcase-cyan);
}

.dashboard-showcase-kpi span,
.dashboard-showcase-kpi p,
.dashboard-showcase-kpi small {
  color: var(--dashboard-showcase-muted);
  font-size: 12px;
}

.dashboard-showcase-kpi span {
  font-weight: 900;
}

.dashboard-showcase-kpi strong {
  color: var(--dashboard-showcase-text);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 24px;
  line-height: 1;
}

.dashboard-showcase-kpi p,
.dashboard-showcase-kpi small {
  margin: 0;
  line-height: 1.25;
}

.dashboard-showcase-grid {
  display: grid;
  gap: 9px;
  min-width: 0;
}

.dashboard-showcase-grid--primary {
  grid-template-columns: minmax(0, 1.05fr) minmax(410px, 0.95fr);
}

.dashboard-showcase-grid--secondary {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.dashboard-showcase-panel {
  min-width: 0;
  border: 1px solid var(--dashboard-showcase-border);
  border-radius: 8px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(16, 31, 51, 0.9), rgba(8, 17, 31, 0.98)),
    var(--dashboard-showcase-panel);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.22);
}

.dashboard-showcase-grid--secondary .dashboard-showcase-panel {
  border-color: rgba(151, 180, 214, 0.085);
  background:
    linear-gradient(180deg, rgba(14, 27, 46, 0.82), rgba(8, 17, 31, 0.9)),
    var(--dashboard-showcase-panel);
  box-shadow: none;
}

.dashboard-showcase-panel--timeline {
  grid-column: span 2;
}

.dashboard-showcase-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-height: 50px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--dashboard-showcase-border);
}

.dashboard-showcase-grid--secondary .dashboard-showcase-panel-header {
  min-height: 48px;
  border-bottom-color: rgba(151, 180, 214, 0.1);
}

.dashboard-showcase-panel-header h2 {
  margin: 0;
  color: var(--dashboard-showcase-text);
  font-size: 15px;
  line-height: 1.2;
}

.dashboard-showcase-panel-header > span {
  border: 1px solid rgba(33, 199, 217, 0.22);
  border-radius: 6px;
  padding: 5px 8px;
  color: var(--dashboard-showcase-cyan);
  background: rgba(33, 199, 217, 0.08);
  font-size: 11px;
  font-weight: 900;
  white-space: nowrap;
}

.dashboard-showcase-runs-table {
  display: grid;
  gap: 4px;
  padding: 0 13px 8px;
}

.dashboard-showcase-runs-head,
.dashboard-showcase-run-row {
  display: grid;
  grid-template-columns: minmax(200px, 1.5fr) 0.82fr 0.82fr 1.02fr 0.82fr 1.05fr;
  gap: 9px;
  align-items: center;
}

.dashboard-showcase-runs-head {
  min-height: 28px;
  color: var(--dashboard-showcase-muted);
  font-size: 11px;
  font-weight: 900;
}

.dashboard-showcase-run-row {
  min-height: 50px;
  border: 1px solid rgba(151, 180, 214, 0.08);
  border-radius: 8px;
  padding: 6px 7px;
  background: rgba(4, 10, 22, 0.26);
  color: var(--dashboard-showcase-secondary);
  font-size: 12px;
}

.dashboard-showcase-run-row div {
  min-width: 0;
}

.dashboard-showcase-ticket-link {
  display: inline-flex;
  width: fit-content;
  border: 1px solid rgba(61, 124, 255, 0.22);
  border-radius: 6px;
  padding: 3px 6px;
  background: rgba(61, 124, 255, 0.1);
  color: #9fc4ff;
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  line-height: 1;
}

.dashboard-showcase-run-row small {
  display: block;
  margin-top: 4px;
  overflow: hidden;
  color: rgba(199, 213, 234, 0.44);
  font-family: Aptos, 'Segoe UI', sans-serif;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dashboard-showcase-run-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  max-width: 100%;
  min-height: 23px;
  border-radius: 6px;
  padding: 4px 7px;
  color: var(--dashboard-showcase-secondary);
  font-size: 11px;
  font-weight: 850;
  line-height: 1.1;
  white-space: nowrap;
}

.dashboard-showcase-run-badge--category {
  color: #cfe1ff;
  background: rgba(61, 124, 255, 0.1);
}

.dashboard-showcase-run-badge--provider {
  color: #9fe8f1;
  background: rgba(33, 199, 217, 0.09);
}

.dashboard-showcase-run-badge--fallback {
  color: #ffd09a;
  background: rgba(255, 180, 92, 0.09);
}

.dashboard-showcase-run-badge--review {
  color: #d8e5ff;
  background: rgba(139, 124, 246, 0.12);
}

.dashboard-showcase-trace-link {
  display: grid;
  gap: 3px;
  justify-items: start;
  color: #8fdfff;
  font-size: 11px;
  font-weight: 900;
  line-height: 1.15;
}

.dashboard-showcase-trace-link b {
  display: inline-flex;
  align-items: center;
  min-height: 23px;
  border: 1px solid rgba(33, 199, 217, 0.24);
  border-radius: 6px;
  padding: 4px 8px;
  background: rgba(33, 199, 217, 0.08);
  color: #9fe8f1;
  font-size: 11px;
  line-height: 1;
  white-space: nowrap;
}

.dashboard-showcase-trace-link small {
  margin: 0;
  color: rgba(199, 213, 234, 0.4);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 9.5px;
  line-height: 1.25;
  white-space: normal;
}

.dashboard-showcase-panel--workflow {
  align-self: start;
}

.dashboard-showcase-workflow {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 7px;
  padding: 9px 12px 7px;
}

.dashboard-showcase-workflow-step {
  position: relative;
  display: grid;
  gap: 5px;
  min-height: 118px;
  border: 1px solid rgba(91, 141, 239, 0.18);
  border-radius: 8px;
  padding: 9px;
  background: rgba(4, 10, 22, 0.52);
}

.dashboard-showcase-workflow-step:not(:last-child)::after {
  position: absolute;
  top: 30px;
  right: -9px;
  width: 10px;
  height: 2px;
  content: '';
  background: var(--dashboard-showcase-blue);
}

.dashboard-showcase-workflow-step span,
.dashboard-showcase-workflow-step small {
  color: var(--dashboard-showcase-muted);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  font-weight: 900;
}

.dashboard-showcase-workflow-step strong {
  color: var(--dashboard-showcase-text);
  font-size: 14px;
}

.dashboard-showcase-workflow-step p {
  margin: 0;
  color: var(--dashboard-showcase-secondary);
  font-size: 12px;
  line-height: 1.35;
}

.dashboard-showcase-workflow-note {
  margin: 0 12px 9px;
  border: 1px solid rgba(151, 180, 214, 0.08);
  border-radius: 8px;
  padding: 7px 10px;
  background: rgba(4, 10, 22, 0.24);
  color: rgba(199, 213, 234, 0.6);
  font-size: 11px;
  line-height: 1.35;
}

.dashboard-showcase-provider-list,
.dashboard-showcase-template-list,
.dashboard-showcase-knowledge-list,
.dashboard-showcase-review-list {
  display: grid;
  gap: 6px;
  padding: 9px 12px 11px;
}

.dashboard-showcase-provider,
.dashboard-showcase-template,
.dashboard-showcase-knowledge,
.dashboard-showcase-review {
  min-width: 0;
  border: 1px solid rgba(151, 180, 214, 0.085);
  border-radius: 8px;
  padding: 8px 9px;
  background: rgba(4, 10, 22, 0.28);
}

.dashboard-showcase-provider,
.dashboard-showcase-knowledge,
.dashboard-showcase-review {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.dashboard-showcase-provider small {
  grid-column: 2;
  color: var(--dashboard-showcase-muted);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 11px;
}

.dashboard-showcase-provider strong,
.dashboard-showcase-template strong,
.dashboard-showcase-knowledge strong,
.dashboard-showcase-review strong {
  display: block;
  color: var(--dashboard-showcase-text);
  font-size: 13px;
  line-height: 1.3;
}

.dashboard-showcase-provider span,
.dashboard-showcase-template span,
.dashboard-showcase-template small,
.dashboard-showcase-knowledge span,
.dashboard-showcase-knowledge small,
.dashboard-showcase-review small {
  display: block;
  margin-top: 4px;
  color: var(--dashboard-showcase-muted);
  font-size: 11px;
  line-height: 1.35;
}

.dashboard-showcase-provider em,
.dashboard-showcase-knowledge b,
.dashboard-showcase-review > span {
  border-radius: 6px;
  padding: 5px 7px;
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.dashboard-showcase-provider[data-tone='online'] em,
.dashboard-showcase-review[data-tone='approve'] > span {
  color: var(--dashboard-showcase-green);
  background: rgba(43, 216, 143, 0.1);
}

.dashboard-showcase-provider[data-tone='fallback'] em,
.dashboard-showcase-review[data-tone='changes'] > span,
.dashboard-showcase-review[data-tone='required'] > span {
  color: var(--dashboard-showcase-orange);
  background: rgba(255, 180, 92, 0.1);
}

.dashboard-showcase-provider[data-tone='idle'] em {
  color: var(--dashboard-showcase-cyan);
  background: rgba(33, 199, 217, 0.1);
}

.dashboard-showcase-review[data-tone='reject'] > span {
  color: var(--dashboard-showcase-red);
  background: rgba(255, 92, 122, 0.1);
}

.dashboard-showcase-template {
  display: grid;
  gap: 2px;
}

.dashboard-showcase-knowledge b {
  color: var(--dashboard-showcase-purple);
  background: rgba(139, 124, 246, 0.12);
}

.dashboard-showcase-timeline {
  display: grid;
  gap: 7px;
  margin: 0;
  padding: 10px 12px 12px;
  list-style: none;
}

.dashboard-showcase-timeline li {
  position: relative;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 10px;
  padding-left: 11px;
  border-left: 2px solid rgba(61, 124, 255, 0.28);
}

.dashboard-showcase-timeline li::before {
  position: absolute;
  top: 4px;
  left: -6px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  content: '';
  background: var(--dashboard-showcase-blue);
  box-shadow: 0 0 0 4px rgba(61, 124, 255, 0.14);
}

.dashboard-showcase-timeline li[data-tone='success']::before {
  background: var(--dashboard-showcase-green);
}

.dashboard-showcase-timeline li[data-tone='warning']::before {
  background: var(--dashboard-showcase-orange);
}

.dashboard-showcase-timeline li[data-tone='danger']::before {
  background: var(--dashboard-showcase-red);
}

.dashboard-showcase-timeline time {
  color: var(--dashboard-showcase-cyan);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  font-weight: 900;
}

.dashboard-showcase-timeline strong {
  color: var(--dashboard-showcase-text);
  font-size: 12px;
}

.dashboard-showcase-timeline p {
  margin: 3px 0 0;
  color: var(--dashboard-showcase-muted);
  font-size: 11px;
  line-height: 1.45;
}

@media (max-width: 1280px) {
  .dashboard-showcase {
    grid-template-columns: 208px minmax(0, 1fr);
  }

  .dashboard-showcase-kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .dashboard-showcase-grid--primary,
  .dashboard-showcase-grid--secondary,
  .dashboard-showcase-hero {
    grid-template-columns: 1fr;
  }

  .dashboard-showcase-panel--timeline {
    grid-column: auto;
  }
}

@media (max-width: 880px) {
  .dashboard-showcase {
    grid-template-columns: 1fr;
  }

  .dashboard-showcase-sidebar {
    position: relative;
    height: auto;
  }

  .dashboard-showcase-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-showcase-topbar,
  .dashboard-showcase-system-card,
  .dashboard-showcase-kpi-grid,
  .dashboard-showcase-runs-head,
  .dashboard-showcase-run-row,
  .dashboard-showcase-workflow,
  .dashboard-showcase-provider,
  .dashboard-showcase-knowledge,
  .dashboard-showcase-review {
    grid-template-columns: 1fr;
  }

  .dashboard-showcase-topbar-actions {
    justify-content: flex-start;
  }

  .dashboard-showcase-content {
    padding: 10px;
  }

  .dashboard-showcase h1 {
    font-size: 31px;
  }

  .dashboard-showcase-workflow-step:not(:last-child)::after {
    display: none;
  }
}
</style>
