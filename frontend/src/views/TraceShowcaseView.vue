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
  tone: 'blue' | 'green' | 'orange' | 'purple'
}

interface KpiCard {
  label: string
  value: string
  note: string
  tone: 'blue' | 'green' | 'orange' | 'purple' | 'cyan'
}

interface WorkflowStep {
  id: string
  title: string
  status: string
  latency: string
  description: string
  tone: 'done' | 'active' | 'waiting'
  active?: boolean
}

interface DetailField {
  label: string
  value: string
  size?: 'wide'
}

interface EvidenceItem {
  label: string
  value: string
  note: string
  tone: 'blue' | 'green' | 'orange' | 'purple'
}

interface MetadataItem {
  label: string
  value: string
}

interface LogItem {
  time: string
  level: string
  text: string
  tone: 'info' | 'success' | 'warning'
}

interface StatusStep {
  label: string
  status: string
  time: string
  active?: boolean
}

interface ReviewItem {
  action: string
  state: string
  note: string
  tone: 'approve' | 'change' | 'reject' | 'required'
}

const navItems: NavItem[] = [
  { label: '仪表盘' },
  { label: '工单管理' },
  { label: '知识库' },
  { label: 'RAG 引用' },
  { label: 'Prompt 模板' },
  { label: 'Provider 健康' },
  { label: 'Trace 证据', active: true },
  { label: 'Human Review' },
  { label: '演示设置' }
]

const heroStatuses: HeroStatus[] = [
  { title: 'Trace 证据', caption: 'Trace Evidence', value: 'enabled', note: '证据视图已启用', tone: 'green' },
  { title: 'Provider 记录', caption: 'Provider Record', value: 'local-rule fallback', note: '本地规则路径', tone: 'blue' },
  { title: 'Tool Call 证据', caption: 'Tool Call', value: 'evidence view only', note: '仅展示证据', tone: 'purple' },
  { title: '人工审核', caption: 'Human Review', value: 'required', note: '状态变更前确认', tone: 'orange' }
]

const kpis: KpiCard[] = [
  { label: 'Trace 记录', value: '389', note: '审计视图', tone: 'blue' },
  { label: 'Provider 记录', value: '128', note: '调用摘要', tone: 'cyan' },
  { label: 'Fallback 次数', value: '82%', note: '演示路径', tone: 'orange' },
  { label: 'Tool Call 证据', value: '46', note: '证据片段', tone: 'purple' },
  { label: 'Human Review 节点', value: '23', note: '人工门禁', tone: 'green' }
]

const workflowSteps: WorkflowStep[] = [
  { id: '01', title: 'Prompt 接收', status: '已记录', latency: '42 ms', description: '标题、日志与上下文入队。', tone: 'done' },
  { id: '02', title: 'Provider 选择', status: '已完成', latency: '68 ms', description: '检查 OpenAI-compatible 配置。', tone: 'done' },
  { id: '03', title: 'local-rule fallback', status: '当前步骤', latency: '389 ms', description: '记录本地规则路径与原因。', tone: 'active', active: true },
  { id: '04', title: 'Knowledge Reference', status: '已附加', latency: '164 ms', description: '关键词命中并附加引用。', tone: 'done' },
  { id: '05', title: 'Draft Reply', status: '已生成', latency: '211 ms', description: '模板草稿进入待审。', tone: 'done' },
  { id: '06', title: 'Human Review', status: '待确认', latency: '人工处理', description: '人工确认后推进状态。', tone: 'waiting' },
  { id: '07', title: 'Status Update', status: '等待中', latency: '-', description: '等待审核决策。', tone: 'waiting' }
]

const detailFields: DetailField[] = [
  { label: '步骤 ID', value: 'step_03_fallback', size: 'wide' },
  { label: '类型', value: 'provider_fallback' },
  { label: '状态', value: 'recorded' },
  { label: 'Provider', value: 'local-rule fallback', size: 'wide' },
  { label: 'Fallback 原因', value: 'API_KEY_MISSING', size: 'wide' },
  { label: '延迟', value: '389 ms' },
  { label: '关联工单', value: 'DEMO-0005' },
  { label: '审核状态', value: 'Human Review required', size: 'wide' }
]

const providerEvidence: EvidenceItem[] = [
  { label: 'Provider 路径', value: 'local-rule fallback', note: '默认演示路径', tone: 'blue' },
  { label: 'Model', value: 'local-rule-demo', note: '本地规则示例', tone: 'purple' },
  { label: 'Fallback', value: 'API_KEY_MISSING', note: '已写入证据摘要', tone: 'orange' }
]

const toolEvidence: EvidenceItem[] = [
  { label: '关键词证据', value: '报价接口 / 500 / traceId', note: 'keyword retrieval', tone: 'blue' },
  { label: '知识引用', value: 'KB-API-500', note: 'RAG reference view', tone: 'purple' },
  { label: '边界说明', value: 'evidence view only', note: 'Tool Call evidence view', tone: 'orange' }
]

const traceSummary: MetadataItem[] = [
  { label: 'Trace ID', value: 'TRACE-0005' },
  { label: '工单 ID', value: 'DEMO-0005' },
  { label: '状态', value: 'REVIEW_REQ' },
  { label: '延迟', value: '389 ms' },
]

const providerRecord: MetadataItem[] = [
  { label: 'providerName', value: 'local-rule fallback' },
  { label: 'modelName', value: 'rule-demo' },
  { label: 'fallbackUsed', value: 'true' },
  { label: 'fallbackReason', value: 'API_KEY_MISSING' }
]

const inputJson = `{
  "ticketId": "DEMO-0005",
  "prompt": "报价接口 500",
  "provider": "OpenAI-compatible",
  "context": ["traceId", "网关日志"]
}`

const outputJson = `{
  "fallbackReason": "API_KEY_MISSING",
  "nextStep": "Human Review",
  "status": "REVIEW_REQUIRED"
}`

const logs: LogItem[] = [
  { time: '14:32:21', level: 'INFO', text: 'Prompt received', tone: 'info' },
  { time: '14:32:24', level: 'WARN', text: 'Provider fallback selected', tone: 'warning' },
  { time: '14:32:27', level: 'INFO', text: 'Knowledge reference attached', tone: 'success' },
  { time: '14:32:31', level: 'INFO', text: 'Human Review requested', tone: 'info' }
]

const statusHistory: StatusStep[] = [
  { label: 'NEW', status: '工单创建', time: '14:31' },
  { label: 'CLASSIFIED', status: '规则分类', time: '14:32' },
  { label: 'REVIEW_REQ', status: '等待审核', time: '14:32', active: true },
  { label: 'APPROVED', status: '人工批准', time: '待处理' },
  { label: 'UPDATED', status: '状态更新', time: '待处理' }
]

const reviewTrail: ReviewItem[] = [
  { action: 'Approve', state: '允许推进', note: '支持人员确认后才可更新状态。', tone: 'approve' },
  { action: 'Request Changes', state: '请求修改', note: '补充证据或调整回复草稿。', tone: 'change' },
  { action: 'Reject', state: '拒绝草稿', note: '风险过高时停止使用建议。', tone: 'reject' },
  { action: 'Reviewer note', state: '不由系统关闭', note: '最终处理仍由人工完成。', tone: 'required' }
]

const boundaryItems = [
  '当前为 trace evidence / local-rule fallback demo。',
  '不声明真实 Multi-Agent Runtime。',
  '不声明完整 Tool Runtime。',
  '不声明真实 LLM provider 生产接入。',
  'Human Review 是门禁，不是自动处理。'
]
</script>

<template>
  <div class="trace-showcase" data-screenshot="trace-evidence">
    <aside class="trace-showcase-sidebar" aria-label="工作区导航">
      <div class="trace-showcase-brand" aria-label="企业工单 RAG Copilot">
        <svg class="trace-showcase-brand-mark" viewBox="0 0 44 44" role="img" aria-label="企业 SaaS 品牌图标">
          <defs>
            <linearGradient id="trace-showcase-logo-fill" x1="8" y1="7" x2="36" y2="37" gradientUnits="userSpaceOnUse">
              <stop offset="0" stop-color="#21c7d9" />
              <stop offset="0.5" stop-color="#3d7cff" />
              <stop offset="1" stop-color="#8b7cf6" />
            </linearGradient>
            <linearGradient id="trace-showcase-logo-line" x1="13" y1="10" x2="31" y2="34" gradientUnits="userSpaceOnUse">
              <stop offset="0" stop-color="#eef5ff" />
              <stop offset="1" stop-color="#a8c9ff" />
            </linearGradient>
            <filter id="trace-showcase-logo-glow" x="-35%" y="-35%" width="170%" height="170%">
              <feGaussianBlur stdDeviation="2.3" result="blur" />
              <feColorMatrix in="blur" type="matrix" values="0 0 0 0 0.18 0 0 0 0 0.42 0 0 0 0 1 0 0 0 0.55 0" result="glow" />
              <feMerge>
                <feMergeNode in="glow" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
          </defs>
          <path
            d="M22 4.5 37.2 13.3v17.4L22 39.5 6.8 30.7V13.3L22 4.5Z"
            fill="rgba(8, 17, 31, 0.94)"
            stroke="url(#trace-showcase-logo-fill)"
            stroke-width="1.9"
            filter="url(#trace-showcase-logo-glow)"
          />
          <path d="M22 10.8 31.6 16.3v11.1L22 33.2l-9.6-5.8V16.3L22 10.8Z" fill="url(#trace-showcase-logo-fill)" opacity="0.24" />
          <path d="M22 10.8v11.1m0 0 9.6-5.6M22 21.9l-9.6-5.6m0 0v11.1L22 33.2l9.6-5.8V16.3" fill="none" stroke="url(#trace-showcase-logo-line)" stroke-width="1.55" stroke-linecap="round" stroke-linejoin="round" />
          <path d="M16.7 24.8 22 27.9l5.3-3.1" fill="none" stroke="#21c7d9" stroke-width="1.25" stroke-linecap="round" stroke-linejoin="round" opacity="0.9" />
          <circle cx="22" cy="10.8" r="2.25" fill="#21c7d9" />
          <circle cx="12.4" cy="16.3" r="2.1" fill="#9fc4ff" />
          <circle cx="31.6" cy="16.3" r="2.1" fill="#8b7cf6" />
          <circle cx="22" cy="33.2" r="2.25" fill="#eef5ff" />
        </svg>
        <div>
          <strong>企业工单</strong>
          <span>RAG Copilot</span>
        </div>
      </div>

      <nav class="trace-showcase-nav" aria-label="主导航">
        <span
          v-for="item in navItems"
          :key="item.label"
          class="trace-showcase-nav-item"
          :class="{ 'trace-showcase-nav-item--active': item.active }"
        >
          {{ item.label }}
        </span>
      </nav>

      <section class="trace-showcase-sidebar-card" aria-label="演示边界">
        <p>Trace 演示</p>
        <strong>只读证据视图</strong>
        <span>local-rule fallback、demo JSON、Human Review required。</span>
      </section>
    </aside>

    <main class="trace-showcase-main">
      <header class="trace-showcase-topbar">
        <label class="trace-showcase-search">
          <span>⌕</span>
          <input type="search" placeholder="搜索 traceId、工单、Provider、Tool Call、JSON..." aria-label="搜索 Trace 证据" readonly />
        </label>
        <div class="trace-showcase-topbar-status" aria-label="演示状态">
          <span class="trace-showcase-chip trace-showcase-chip--green">演示环境</span>
          <span class="trace-showcase-chip trace-showcase-chip--blue">Provider: local-rule fallback</span>
          <span class="trace-showcase-chip trace-showcase-chip--muted">管理员演示</span>
        </div>
      </header>

      <section class="trace-showcase-hero" aria-label="Trace 证据控制台">
        <div class="trace-showcase-hero-copy">
          <span class="trace-showcase-eyebrow">Trace 证据审计控制台</span>
          <h1>Trace 证据控制台</h1>
          <h2>Agent Run / Provider / Tool Call Evidence Console</h2>
          <p>统一展示工单处理链路中的 Provider 调用、Tool Call 证据、JSON 输入输出、状态历史与 Human Review 审核轨迹。</p>
          <div class="trace-showcase-flow-strip" aria-label="Trace 主链路">
            <span>Prompt 输入</span>
            <span>Provider 选择</span>
            <span>local-rule fallback</span>
            <span>Tool Call 证据</span>
            <span>Trace 记录</span>
            <span>Human Review</span>
          </div>
        </div>
        <div class="trace-showcase-hero-status" aria-label="Trace 状态卡">
          <article v-for="item in heroStatuses" :key="item.title" class="trace-showcase-status-card" :class="`trace-showcase-status-card--${item.tone}`">
            <span>{{ item.caption }}</span>
            <strong>{{ item.title }}</strong>
            <small><b>{{ item.value }}</b> · {{ item.note }}</small>
          </article>
        </div>
      </section>

      <section class="trace-showcase-kpis" aria-label="Trace KPI">
        <article v-for="kpi in kpis" :key="kpi.label" class="trace-showcase-kpi" :class="`trace-showcase-kpi--${kpi.tone}`">
          <span>{{ kpi.label }}</span>
          <strong>{{ kpi.value }}</strong>
          <small>{{ kpi.note }}</small>
        </article>
      </section>

      <section class="trace-showcase-workspace" aria-label="Trace 证据工作区">
        <article class="trace-showcase-panel trace-showcase-panel--steps">
          <div class="trace-showcase-panel-heading">
            <div>
              <p class="trace-showcase-eyebrow">Trace 审计流程</p>
              <h3>Trace 工作流步骤</h3>
            </div>
            <span class="trace-showcase-count">7 个步骤</span>
          </div>

          <div class="trace-showcase-step-list">
            <section
              v-for="step in workflowSteps"
              :key="step.id"
              class="trace-showcase-step"
              :class="[
                `trace-showcase-step--${step.tone}`,
                { 'trace-showcase-step--active': step.active }
              ]"
            >
              <span class="trace-showcase-step-id">{{ step.id }}</span>
              <div>
                <strong>{{ step.title }}</strong>
                <p>{{ step.description }}</p>
              </div>
              <aside>
                <b>{{ step.status }}</b>
                <small>{{ step.latency }}</small>
              </aside>
            </section>
          </div>
        </article>

        <article class="trace-showcase-panel trace-showcase-panel--detail">
          <div class="trace-showcase-panel-heading">
            <div>
              <p class="trace-showcase-eyebrow">步骤证据详情</p>
              <h3>步骤详情 / 证据</h3>
            </div>
            <span class="trace-showcase-chip trace-showcase-chip--orange">fallback recorded</span>
          </div>

          <div class="trace-showcase-detail-grid">
            <span
              v-for="field in detailFields"
              :key="field.label"
              class="trace-showcase-detail-field"
              :class="{ 'trace-showcase-detail-field--wide': field.size === 'wide' }"
            >
              <small>{{ field.label }}</small>
              <strong>{{ field.value }}</strong>
            </span>
          </div>

          <section class="trace-showcase-evidence-summary" aria-label="证据摘要">
            <span>证据摘要</span>
            <p>未提供临时 API Key，本次记录 `API_KEY_MISSING`，回退本地规则与关键词引用；草稿进入 Human Review。</p>
          </section>

          <div class="trace-showcase-evidence-columns">
            <section class="trace-showcase-evidence-box" aria-label="Provider Evidence">
              <div class="trace-showcase-mini-heading">
                <h4>Provider 证据</h4>
                <span>Provider Record</span>
              </div>
              <div class="trace-showcase-evidence-list">
                <span v-for="item in providerEvidence" :key="item.label" :class="`trace-showcase-evidence-row--${item.tone}`">
                  <small>{{ item.label }}</small>
                  <strong>{{ item.value }}</strong>
                  <em>{{ item.note }}</em>
                </span>
              </div>
            </section>

            <section class="trace-showcase-evidence-box" aria-label="Tool Call Evidence">
              <div class="trace-showcase-mini-heading">
                <h4>Tool Call 证据</h4>
                <span>Evidence View</span>
              </div>
              <div class="trace-showcase-evidence-list">
                <span v-for="item in toolEvidence" :key="item.label" :class="`trace-showcase-evidence-row--${item.tone}`">
                  <small>{{ item.label }}</small>
                  <strong>{{ item.value }}</strong>
                  <em>{{ item.note }}</em>
                </span>
              </div>
            </section>
          </div>
        </article>

        <article class="trace-showcase-panel trace-showcase-panel--metadata">
          <div class="trace-showcase-panel-heading">
            <div>
              <p class="trace-showcase-eyebrow">证据元数据</p>
              <h3>Trace 元数据 / JSON / 日志</h3>
            </div>
            <span class="trace-showcase-chip trace-showcase-chip--purple">demo JSON</span>
          </div>

          <div class="trace-showcase-meta-groups" aria-label="Trace evidence summary">
            <section class="trace-showcase-meta-group">
              <div class="trace-showcase-mini-heading">
                <h4>Trace 摘要</h4>
                <span>Trace Summary</span>
              </div>
              <dl>
                <template v-for="item in traceSummary" :key="item.label">
                  <dt>{{ item.label }}</dt>
                  <dd>{{ item.value }}</dd>
                </template>
              </dl>
            </section>

            <section class="trace-showcase-meta-group trace-showcase-meta-group--provider">
              <div class="trace-showcase-mini-heading">
                <h4>Provider 记录</h4>
                <span>Provider Record</span>
              </div>
              <dl>
                <template v-for="item in providerRecord" :key="item.label">
                  <dt>{{ item.label }}</dt>
                  <dd>{{ item.value }}</dd>
                </template>
              </dl>
            </section>
          </div>

          <section class="trace-showcase-json-preview" aria-label="JSON Preview">
            <div class="trace-showcase-mini-heading">
              <h4>JSON 证据预览</h4>
              <span>input / output</span>
            </div>
            <div class="trace-showcase-code-grid">
              <pre><code>{{ inputJson }}</code></pre>
              <pre><code>{{ outputJson }}</code></pre>
            </div>
          </section>

          <section class="trace-showcase-log-list" aria-label="Event Logs">
            <div class="trace-showcase-mini-heading">
              <h4>事件日志</h4>
              <span>4 条</span>
            </div>
            <span v-for="log in logs" :key="`${log.time}-${log.text}`" class="trace-showcase-log" :class="`trace-showcase-log--${log.tone}`">
              <b>{{ log.time }}</b>
              <em>{{ log.level }}</em>
              <small>{{ log.text }}</small>
            </span>
          </section>
        </article>
      </section>

      <section class="trace-showcase-bottom-grid" aria-label="状态历史与能力边界">
        <article class="trace-showcase-panel trace-showcase-panel--soft">
          <div class="trace-showcase-panel-heading">
            <div>
              <p class="trace-showcase-eyebrow">状态流转</p>
              <h3>状态历史</h3>
            </div>
          </div>
          <div class="trace-showcase-status-flow">
            <span v-for="item in statusHistory" :key="item.label" :class="{ 'trace-showcase-status-flow--active': item.active }">
              <strong>{{ item.label }}</strong>
              <small>{{ item.status }}</small>
              <em>{{ item.time }}</em>
            </span>
          </div>
        </article>

        <article class="trace-showcase-panel trace-showcase-panel--soft">
          <div class="trace-showcase-panel-heading">
            <div>
              <p class="trace-showcase-eyebrow">人工审核轨迹</p>
              <h3>Human Review 审核轨迹</h3>
            </div>
          </div>
          <div class="trace-showcase-review-list">
            <span v-for="item in reviewTrail" :key="item.action" :class="`trace-showcase-review--${item.tone}`">
              <strong>{{ item.action }}</strong>
              <small>{{ item.state }}</small>
              <em>{{ item.note }}</em>
            </span>
          </div>
        </article>

        <article class="trace-showcase-panel trace-showcase-panel--soft">
          <div class="trace-showcase-panel-heading">
            <div>
              <p class="trace-showcase-eyebrow">边界说明</p>
              <h3>能力边界</h3>
            </div>
          </div>
          <ul class="trace-showcase-boundary-list">
            <li v-for="item in boundaryItems" :key="item">{{ item }}</li>
          </ul>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

:global(body) {
  margin: 0;
  background: #040912;
}

.trace-showcase {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 224px minmax(0, 1fr);
  color: #eef5ff;
  background:
    radial-gradient(circle at 78% 8%, rgba(33, 199, 217, 0.13), transparent 30%),
    radial-gradient(circle at 42% 2%, rgba(61, 124, 255, 0.13), transparent 24%),
    linear-gradient(135deg, #050b15 0%, #07101d 46%, #08121f 100%);
  font-family: Aptos, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  letter-spacing: 0;
}

.trace-showcase-sidebar {
  min-height: 100vh;
  padding: 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background:
    linear-gradient(180deg, rgba(8, 17, 31, 0.98), rgba(5, 11, 21, 0.98)),
    radial-gradient(circle at 20% 0%, rgba(61, 124, 255, 0.18), transparent 28%);
  border-right: 1px solid rgba(151, 180, 214, 0.14);
  box-shadow: 16px 0 38px rgba(0, 0, 0, 0.22);
}

.trace-showcase-brand {
  min-height: 54px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.trace-showcase-brand-mark {
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
}

.trace-showcase-brand strong,
.trace-showcase-brand span {
  display: block;
}

.trace-showcase-brand strong {
  font-size: 15px;
  line-height: 1.25;
  font-weight: 800;
}

.trace-showcase-brand span {
  margin-top: 2px;
  color: #9fb3ce;
  font-size: 12px;
  font-weight: 700;
}

.trace-showcase-nav {
  display: grid;
  gap: 6px;
}

.trace-showcase-nav-item {
  min-height: 34px;
  padding: 8px 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  color: #98abc6;
  font-size: 13px;
  line-height: 1;
}

.trace-showcase-nav-item--active {
  color: #eef5ff;
  background: linear-gradient(135deg, rgba(61, 124, 255, 0.24), rgba(33, 199, 217, 0.12));
  border-color: rgba(61, 124, 255, 0.34);
  box-shadow: 0 12px 30px rgba(61, 124, 255, 0.16);
}

.trace-showcase-sidebar-card {
  margin-top: auto;
  padding: 14px;
  border-radius: 10px;
  background: rgba(12, 23, 38, 0.84);
  border: 1px solid rgba(151, 180, 214, 0.14);
}

.trace-showcase-sidebar-card p,
.trace-showcase-sidebar-card strong,
.trace-showcase-sidebar-card span {
  display: block;
  margin: 0;
}

.trace-showcase-sidebar-card p {
  color: #21c7d9;
  font-size: 11px;
  font-weight: 800;
}

.trace-showcase-sidebar-card strong {
  margin-top: 5px;
  font-size: 14px;
}

.trace-showcase-sidebar-card span {
  margin-top: 8px;
  color: #8fa2bd;
  font-size: 12px;
  line-height: 1.45;
}

.trace-showcase-main {
  min-width: 0;
  padding: 12px 18px 18px;
}

.trace-showcase-topbar {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.trace-showcase-search {
  width: min(560px, 48vw);
  height: 36px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 9px;
  border-radius: 9px;
  background: rgba(4, 9, 18, 0.72);
  border: 1px solid rgba(151, 180, 214, 0.14);
  color: #7e91ab;
}

.trace-showcase-search input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  color: #c7d5ea;
  background: transparent;
  font: inherit;
  font-size: 13px;
}

.trace-showcase-search input::placeholder {
  color: #6f829d;
}

.trace-showcase-topbar-status,
.trace-showcase-flow-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.trace-showcase-chip {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 5px 9px;
  border-radius: 7px;
  border: 1px solid rgba(151, 180, 214, 0.15);
  color: #c7d5ea;
  background: rgba(12, 23, 38, 0.76);
  font-size: 12px;
  font-weight: 700;
}

.trace-showcase-chip--green {
  color: #aaf4d1;
  border-color: rgba(43, 216, 143, 0.22);
  background: rgba(43, 216, 143, 0.1);
}

.trace-showcase-chip--blue {
  color: #b8d0ff;
  border-color: rgba(61, 124, 255, 0.24);
  background: rgba(61, 124, 255, 0.1);
}

.trace-showcase-chip--orange {
  color: #ffd6a8;
  border-color: rgba(255, 180, 92, 0.26);
  background: rgba(255, 180, 92, 0.1);
}

.trace-showcase-chip--purple {
  color: #d8d2ff;
  border-color: rgba(139, 124, 246, 0.24);
  background: rgba(139, 124, 246, 0.1);
}

.trace-showcase-chip--muted {
  color: #8fa2bd;
  background: rgba(12, 23, 38, 0.46);
}

.trace-showcase-hero {
  min-height: 100px;
  padding: 9px 12px;
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(420px, 0.92fr);
  gap: 12px;
  border-radius: 12px;
  background:
    linear-gradient(135deg, rgba(16, 31, 51, 0.92), rgba(8, 17, 31, 0.9)),
    radial-gradient(circle at 10% 20%, rgba(61, 124, 255, 0.2), transparent 30%);
  border: 1px solid rgba(91, 141, 239, 0.24);
  box-shadow: 0 22px 54px rgba(0, 0, 0, 0.28);
}

.trace-showcase-eyebrow {
  margin: 0;
  color: #21c7d9;
  font-size: 10.5px;
  font-weight: 900;
}

.trace-showcase-hero h1,
.trace-showcase-hero h2,
.trace-showcase-hero p {
  margin: 0;
}

.trace-showcase-hero h1 {
  margin-top: 3px;
  font-size: 23px;
  line-height: 1.08;
}

.trace-showcase-hero h2 {
  margin-top: 3px;
  color: #c7d5ea;
  font-size: 14px;
  font-weight: 800;
}

.trace-showcase-hero p {
  max-width: 820px;
  margin-top: 4px;
  color: #9fb3ce;
  font-size: 12.5px;
  line-height: 1.45;
}

.trace-showcase-flow-strip {
  margin-top: 6px;
}

.trace-showcase-flow-strip span {
  position: relative;
  padding: 4px 7px;
  border-radius: 7px;
  color: #cfe2ff;
  background: rgba(4, 9, 18, 0.46);
  border: 1px solid rgba(151, 180, 214, 0.13);
  font-size: 12px;
  font-weight: 800;
}

.trace-showcase-hero-status {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.trace-showcase-status-card {
  min-height: 42px;
  padding: 6px 9px;
  display: grid;
  align-content: center;
  gap: 4px;
  border-radius: 10px;
  background: rgba(4, 9, 18, 0.5);
  border: 1px solid rgba(151, 180, 214, 0.14);
}

.trace-showcase-status-card span,
.trace-showcase-status-card small {
  color: #8fa2bd;
  font-size: 10.5px;
}

.trace-showcase-status-card strong {
  color: #eef5ff;
  font-size: 13px;
}

.trace-showcase-status-card small b {
  color: #cfe2ff;
  font-weight: 800;
}

.trace-showcase-status-card--green {
  border-color: rgba(43, 216, 143, 0.24);
}

.trace-showcase-status-card--blue {
  border-color: rgba(61, 124, 255, 0.26);
}

.trace-showcase-status-card--orange {
  border-color: rgba(255, 180, 92, 0.27);
}

.trace-showcase-status-card--purple {
  border-color: rgba(139, 124, 246, 0.25);
}

.trace-showcase-kpis {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.trace-showcase-kpi {
  min-height: 54px;
  padding: 7px 11px;
  display: grid;
  gap: 4px;
  border-radius: 10px;
  background: linear-gradient(180deg, rgba(16, 31, 51, 0.82), rgba(12, 23, 38, 0.72));
  border: 1px solid rgba(151, 180, 214, 0.15);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.18);
}

.trace-showcase-kpi::before {
  content: "";
  display: block;
  width: 34px;
  height: 2px;
  border-radius: 999px;
  background: #3d7cff;
}

.trace-showcase-kpi span,
.trace-showcase-kpi small {
  color: #8fa2bd;
  font-size: 11.5px;
}

.trace-showcase-kpi strong {
  color: #eef5ff;
  font-size: 21px;
  line-height: 1;
}

.trace-showcase-kpi--green::before {
  background: #2bd88f;
}

.trace-showcase-kpi--orange::before {
  background: #ffb45c;
}

.trace-showcase-kpi--purple::before {
  background: #8b7cf6;
}

.trace-showcase-kpi--cyan::before {
  background: #21c7d9;
}

.trace-showcase-workspace {
  margin-top: 9px;
  display: grid;
  grid-template-columns: minmax(280px, 0.86fr) minmax(420px, 1.34fr) minmax(320px, 0.95fr);
  gap: 12px;
  align-items: stretch;
}

.trace-showcase-panel {
  min-width: 0;
  border-radius: 11px;
  background: linear-gradient(180deg, rgba(12, 23, 38, 0.9), rgba(7, 16, 29, 0.9));
  border: 1px solid rgba(151, 180, 214, 0.16);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.22);
}

.trace-showcase-panel--steps,
.trace-showcase-panel--detail,
.trace-showcase-panel--metadata {
  min-height: 300px;
  padding: 8px;
}

.trace-showcase-panel-heading,
.trace-showcase-mini-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.trace-showcase-panel-heading h3,
.trace-showcase-mini-heading h4 {
  margin: 2px 0 0;
  font-size: 14px;
  line-height: 1.2;
}

.trace-showcase-mini-heading h4 {
  font-size: 12.5px;
}

.trace-showcase-mini-heading span,
.trace-showcase-count {
  color: #7e91ab;
  font-size: 11px;
  font-weight: 800;
}

.trace-showcase-step-list {
  position: relative;
  margin-top: 8px;
  display: grid;
  gap: 4px;
}

.trace-showcase-step-list::before {
  content: "";
  position: absolute;
  left: 16px;
  top: 17px;
  bottom: 17px;
  width: 1px;
  background: linear-gradient(180deg, rgba(61, 124, 255, 0.5), rgba(43, 216, 143, 0.42), rgba(255, 180, 92, 0.38));
}

.trace-showcase-step {
  position: relative;
  min-height: 33px;
  padding: 5px 6px 5px 34px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 62px;
  gap: 7px;
  border-radius: 9px;
  background: rgba(4, 9, 18, 0.44);
  border: 1px solid rgba(151, 180, 214, 0.12);
}

.trace-showcase-step--active {
  border-color: rgba(61, 124, 255, 0.5);
  box-shadow: 0 0 0 1px rgba(61, 124, 255, 0.16), 0 15px 32px rgba(61, 124, 255, 0.14);
  background: linear-gradient(135deg, rgba(61, 124, 255, 0.18), rgba(33, 199, 217, 0.08));
}

.trace-showcase-step-id {
  position: absolute;
  left: 7px;
  top: 7px;
  z-index: 1;
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  color: #eef5ff;
  background: #3d7cff;
  font-size: 9px;
  font-weight: 900;
}

.trace-showcase-step--done .trace-showcase-step-id {
  background: #2bd88f;
}

.trace-showcase-step--waiting .trace-showcase-step-id {
  background: #ffb45c;
}

.trace-showcase-step strong,
.trace-showcase-step p,
.trace-showcase-step b,
.trace-showcase-step small {
  margin: 0;
}

.trace-showcase-step strong {
  font-size: 11px;
}

.trace-showcase-step p {
  display: -webkit-box;
  margin-top: 2px;
  overflow: hidden;
  color: #8fa2bd;
  font-size: 10px;
  line-height: 1.2;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.trace-showcase-step aside {
  display: grid;
  justify-items: end;
  gap: 2px;
}

.trace-showcase-step b {
  color: #cfe2ff;
  font-size: 10px;
}

.trace-showcase-step small {
  color: #7e91ab;
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 9px;
}

.trace-showcase-detail-grid,
.trace-showcase-meta-grid {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.trace-showcase-meta-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.trace-showcase-meta-groups {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
}

.trace-showcase-meta-group {
  min-width: 0;
  padding: 7px 8px;
  border-radius: 9px;
  background: rgba(4, 9, 18, 0.38);
  border: 1px solid rgba(151, 180, 214, 0.1);
}

.trace-showcase-meta-group--provider {
  border-color: rgba(61, 124, 255, 0.16);
  background: linear-gradient(135deg, rgba(61, 124, 255, 0.08), rgba(4, 9, 18, 0.38));
}

.trace-showcase-meta-group dl {
  margin: 6px 0 0;
  display: grid;
  grid-template-columns: 68px minmax(0, 1fr);
  gap: 4px 7px;
}

.trace-showcase-meta-group dt,
.trace-showcase-meta-group dd {
  margin: 0;
  min-width: 0;
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 9.8px;
  line-height: 1.2;
}

.trace-showcase-meta-group dt {
  color: #7e91ab;
}

.trace-showcase-meta-group dd {
  color: #dceaff;
  overflow-wrap: anywhere;
}

.trace-showcase-detail-grid span,
.trace-showcase-meta-grid span {
  min-width: 0;
  padding: 6px 7px;
  border-radius: 8px;
  background: rgba(4, 9, 18, 0.42);
  border: 1px solid rgba(151, 180, 214, 0.11);
}

.trace-showcase-detail-field--wide {
  grid-column: span 2;
}

.trace-showcase-detail-grid small,
.trace-showcase-meta-grid small {
  display: block;
  color: #7e91ab;
  font-size: 10px;
}

.trace-showcase-detail-grid strong,
.trace-showcase-meta-grid strong {
  display: block;
  margin-top: 3px;
  overflow: hidden;
  color: #dceaff;
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trace-showcase-detail-grid strong {
  overflow: visible;
  font-size: 10.5px;
  text-overflow: clip;
  white-space: normal;
  word-break: break-word;
}

.trace-showcase-evidence-summary {
  margin-top: 6px;
  padding: 6px 8px;
  border-radius: 9px;
  background: linear-gradient(135deg, rgba(61, 124, 255, 0.12), rgba(33, 199, 217, 0.06));
  border: 1px solid rgba(61, 124, 255, 0.2);
}

.trace-showcase-evidence-summary span {
  color: #21c7d9;
  font-size: 11px;
  font-weight: 900;
}

.trace-showcase-evidence-summary p {
  margin: 3px 0 0;
  color: #b6c6dd;
  font-size: 11px;
  line-height: 1.3;
}

.trace-showcase-evidence-columns {
  margin-top: 6px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.trace-showcase-evidence-box {
  min-width: 0;
  padding: 6px;
  border-radius: 10px;
  background: rgba(4, 9, 18, 0.36);
  border: 1px solid rgba(151, 180, 214, 0.12);
}

.trace-showcase-evidence-list {
  margin-top: 5px;
  display: grid;
  gap: 5px;
}

.trace-showcase-evidence-list span {
  min-width: 0;
  padding: 4px 6px;
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr);
  gap: 1px 6px;
  border-radius: 8px;
  background: rgba(12, 23, 38, 0.72);
  border-left: 3px solid #3d7cff;
}

.trace-showcase-evidence-list small {
  color: #7e91ab;
  font-size: 9.5px;
}

.trace-showcase-evidence-list strong {
  overflow: visible;
  color: #eef5ff;
  font-size: 10.8px;
  text-overflow: clip;
  white-space: normal;
}

.trace-showcase-evidence-list em {
  grid-column: 2;
  color: #8fa2bd;
  font-size: 9.2px;
  font-style: normal;
}

.trace-showcase-evidence-row--green {
  border-left-color: #2bd88f !important;
}

.trace-showcase-evidence-row--orange {
  border-left-color: #ffb45c !important;
}

.trace-showcase-evidence-row--purple {
  border-left-color: #8b7cf6 !important;
}

.trace-showcase-json-preview {
  margin-top: 7px;
}

.trace-showcase-code-grid {
  margin-top: 6px;
  display: grid;
  gap: 6px;
}

.trace-showcase-code-grid pre {
  max-height: 74px;
  margin: 0;
  padding: 7px 9px;
  overflow: hidden;
  border-radius: 9px;
  color: #cfe2ff;
  background: rgba(3, 7, 14, 0.72);
  border: 1px solid rgba(139, 124, 246, 0.17);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
  font-size: 9.5px;
  line-height: 1.3;
}

.trace-showcase-log-list {
  margin-top: 7px;
  display: grid;
  gap: 4px;
}

.trace-showcase-log {
  padding: 4px 7px;
  display: grid;
  grid-template-columns: 58px 44px minmax(0, 1fr);
  gap: 7px;
  align-items: center;
  border-radius: 8px;
  background: rgba(4, 9, 18, 0.38);
  border: 1px solid rgba(151, 180, 214, 0.1);
  font-family: "Cascadia Code", SFMono-Regular, Consolas, monospace;
}

.trace-showcase-log b,
.trace-showcase-log em,
.trace-showcase-log small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trace-showcase-log b {
  color: #c7d5ea;
  font-size: 10px;
}

.trace-showcase-log em {
  color: #21c7d9;
  font-size: 10px;
  font-style: normal;
}

.trace-showcase-log small {
  color: #9fb3ce;
  font-size: 10px;
}

.trace-showcase-log--warning em {
  color: #ffb45c;
}

.trace-showcase-log--success em {
  color: #2bd88f;
}

.trace-showcase-bottom-grid {
  margin-top: 9px;
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr;
  gap: 12px;
}

.trace-showcase-panel--soft {
  min-height: 124px;
  padding: 9px;
  border-color: rgba(151, 180, 214, 0.1);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.14);
}

.trace-showcase-status-flow {
  position: relative;
  margin-top: 13px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.trace-showcase-status-flow::before {
  content: "";
  position: absolute;
  left: 9%;
  right: 9%;
  top: 13px;
  height: 1px;
  background: rgba(151, 180, 214, 0.28);
}

.trace-showcase-status-flow span {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  gap: 3px;
  text-align: center;
}

.trace-showcase-status-flow strong::before {
  content: "";
  width: 11px;
  height: 11px;
  margin: 0 auto 6px;
  display: block;
  border-radius: 999px;
  background: #3d7cff;
  box-shadow: 0 0 0 4px rgba(61, 124, 255, 0.12);
}

.trace-showcase-status-flow--active strong::before {
  background: #ffb45c;
  box-shadow: 0 0 0 5px rgba(255, 180, 92, 0.12);
}

.trace-showcase-status-flow strong {
  color: #dceaff;
  max-width: 76px;
  font-size: 10px;
  line-height: 1.12;
  overflow-wrap: anywhere;
}

.trace-showcase-status-flow small,
.trace-showcase-status-flow em {
  color: #7e91ab;
  font-size: 10px;
  font-style: normal;
}

.trace-showcase-review-list {
  margin-top: 7px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.trace-showcase-review-list span {
  padding: 6px 7px;
  display: grid;
  gap: 3px;
  border-radius: 8px;
  background: rgba(4, 9, 18, 0.38);
  border: 1px solid rgba(151, 180, 214, 0.1);
}

.trace-showcase-review-list strong {
  color: #eef5ff;
  font-size: 11.5px;
}

.trace-showcase-review-list small {
  color: #c7d5ea;
  font-size: 10.5px;
}

.trace-showcase-review-list em {
  color: #7e91ab;
  font-size: 9.5px;
  line-height: 1.25;
  font-style: normal;
}

.trace-showcase-review--approve {
  border-color: rgba(43, 216, 143, 0.18) !important;
}

.trace-showcase-review--change,
.trace-showcase-review--required {
  border-color: rgba(255, 180, 92, 0.2) !important;
}

.trace-showcase-review--reject {
  border-color: rgba(255, 92, 122, 0.18) !important;
}

.trace-showcase-boundary-list {
  margin: 7px 0 0;
  padding: 0;
  display: grid;
  gap: 5px;
  list-style: none;
}

.trace-showcase-boundary-list li {
  position: relative;
  padding-left: 15px;
  color: #9fb3ce;
  font-size: 11.5px;
  line-height: 1.28;
}

.trace-showcase-boundary-list li::before {
  content: "";
  position: absolute;
  left: 0;
  top: 6px;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #21c7d9;
}

@media (max-width: 1180px) {
  .trace-showcase {
    grid-template-columns: 1fr;
  }

  .trace-showcase-sidebar {
    min-height: auto;
  }

  .trace-showcase-nav {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .trace-showcase-hero,
  .trace-showcase-workspace,
  .trace-showcase-bottom-grid {
    grid-template-columns: 1fr;
  }

  .trace-showcase-hero-status,
  .trace-showcase-kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .trace-showcase-main {
    padding: 12px;
  }

  .trace-showcase-topbar {
    height: auto;
    align-items: stretch;
    flex-direction: column;
  }

  .trace-showcase-search {
    width: 100%;
  }

  .trace-showcase-kpis,
  .trace-showcase-hero-status,
  .trace-showcase-detail-grid,
  .trace-showcase-meta-grid,
  .trace-showcase-evidence-columns,
  .trace-showcase-review-list,
  .trace-showcase-status-flow {
    grid-template-columns: 1fr;
  }

  .trace-showcase-status-flow::before {
    display: none;
  }
}
</style>
