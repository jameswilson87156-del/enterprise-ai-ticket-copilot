<script setup lang="ts">
interface NavItem {
  label: string
  active?: boolean
}

interface KpiCard {
  label: string
  value: string
  note: string
  tone: 'blue' | 'green' | 'orange' | 'purple' | 'cyan'
}

interface KnowledgeDocument {
  id: string
  title: string
  category: string
  sourcePath: string
  updatedAt: string
  chunks: string
  owner: string
  status: string
  statusTone: 'ready' | 'review'
  keywords: string[]
  active?: boolean
}

interface ChunkEvidence {
  id: string
  docId: string
  score: string
  scoreWidth: string
  scoreTone: 'high' | 'medium'
  keywords: string[]
  excerpt: string
  scenario: string
  updatedAt: string
  position: string
  relatedTicket: string
}

interface RelatedTicket {
  id: string
  title: string
  status: string
  priority: string
}

interface PreviewItem {
  label: string
  value: string
  note: string
}

interface FlowStep {
  step: string
  title: string
  note: string
}

interface TimelineItem {
  time: string
  title: string
  detail: string
  tone: 'info' | 'success' | 'warning'
}

const navItems: NavItem[] = [
  { label: '仪表盘' },
  { label: '工单管理' },
  { label: '知识库', active: true },
  { label: 'RAG 引用' },
  { label: 'Prompt 模板' },
  { label: 'Provider 健康' },
  { label: 'Trace 证据' },
  { label: 'Human Review' },
  { label: '演示设置' }
]

const kpis: KpiCard[] = [
  { label: '知识文档', value: '238', note: '演示文档样本', tone: 'blue' },
  { label: 'Chunk 数量', value: '1,426', note: '本地切片视图', tone: 'purple' },
  { label: '关键词命中', value: '82%', note: '关键词匹配路径', tone: 'green' },
  { label: '关联工单', value: '64', note: '来源与引用关系', tone: 'cyan' },
  { label: '待审核草稿', value: '7', note: '发布前人工确认', tone: 'orange' }
]

const documents: KnowledgeDocument[] = [
  {
    id: 'KB-API-500',
    title: '接口 500 错误分层排查手册',
    category: '系统故障',
    sourcePath: 'knowledge_article/KB-API-500',
    updatedAt: '2026-06-20 14:32',
    chunks: '24',
    owner: '支持运营',
    status: '可引用',
    statusTone: 'ready',
    keywords: ['500', '报价接口', '网关日志'],
    active: true
  },
  {
    id: 'KB-OPS-003',
    title: '系统故障超时与 5xx 排查手册',
    category: '运维知识',
    sourcePath: 'knowledge_article/KB-OPS-003',
    updatedAt: '2026-06-19 09:18',
    chunks: '31',
    owner: 'SRE 支持',
    status: '可引用',
    statusTone: 'ready',
    keywords: ['超时', '5xx', '依赖服务']
  },
  {
    id: 'KB-ORDER-012',
    title: '订单服务降级处理指南',
    category: '订单域',
    sourcePath: 'knowledge_article/KB-ORDER-012',
    updatedAt: '2026-06-18 16:05',
    chunks: '18',
    owner: '订单支持',
    status: '需复核',
    statusTone: 'review',
    keywords: ['降级', '订单服务', '人工审批']
  },
  {
    id: 'KB-REDIS-CONN',
    title: 'Redis 连接池耗尽排查手册',
    category: '平台知识',
    sourcePath: 'knowledge_article/KB-REDIS-CONN',
    updatedAt: '2026-06-18 11:26',
    chunks: '27',
    owner: '平台支持',
    status: '可引用',
    statusTone: 'ready',
    keywords: ['连接池', '登录超时', 'Redis']
  }
]

const chunkEvidence: ChunkEvidence[] = [
  {
    id: 'chunk-api-500-03',
    docId: 'KB-API-500',
    score: '0.91',
    scoreWidth: '91%',
    scoreTone: 'high',
    keywords: ['报价接口', '500', '灰度发布'],
    excerpt: '当接口在灰度窗口后集中返回 500，应先核对发布差异、traceId、网关日志与依赖服务状态。',
    scenario: '报价中心提交企业客户报价失败',
    updatedAt: '06-20 14:32',
    position: '段落 2 / 句子 4',
    relatedTicket: 'DEMO-0005'
  },
  {
    id: 'chunk-ops-003-11',
    docId: 'KB-OPS-003',
    score: '0.84',
    scoreWidth: '84%',
    scoreTone: 'high',
    keywords: ['5xx', '依赖服务', '人工确认'],
    excerpt: '5xx 故障排查应记录调用链证据，模板草稿只能作为处理建议，状态变更需要支持人员确认。',
    scenario: '网关 5xx 与依赖服务异常联动',
    updatedAt: '06-19 09:18',
    position: '段落 5 / 句子 2',
    relatedTicket: 'DEMO-0005'
  },
  {
    id: 'chunk-order-012-07',
    docId: 'KB-ORDER-012',
    score: '0.76',
    scoreWidth: '76%',
    scoreTone: 'medium',
    keywords: ['降级', '回滚', '审批'],
    excerpt: '订单链路降级或回滚必须经过负责人复核，不应由系统根据草稿自动执行。',
    scenario: '订单服务风险处置建议',
    updatedAt: '06-18 16:05',
    position: '段落 3 / 句子 1',
    relatedTicket: 'DEMO-0012'
  }
]

const relatedTickets: RelatedTicket[] = [
  { id: 'DEMO-0005', title: '报价接口返回 500，销售无法生成报价单', status: '处理中', priority: 'P1' },
  { id: 'DEMO-0003', title: 'Redis 连接失败导致会话校验超时', status: '处理中', priority: 'P1' },
  { id: 'DEMO-0007', title: 'VPN 策略变更后供应商无法访问测试环境', status: '待审核', priority: 'P2' }
]

const previewItems: PreviewItem[] = [
  { label: '引用条目', value: 'KB-API-500', note: '接口 500 错误分层排查手册' },
  { label: '引用原因', value: '关键词 + 分类匹配', note: '报价接口、500、灰度发布均命中' },
  { label: '关联工单', value: 'DEMO-0005', note: '报价接口返回 500，销售无法生成报价单' },
  { label: 'Human Review', value: '待支持人员确认', note: '不自动回复，不自动发布知识' }
]

const evidenceFlow: FlowStep[] = [
  { step: '01', title: '知识文档', note: '已发布知识条目' },
  { step: '02', title: 'Chunk', note: '命中关键片段' },
  { step: '03', title: '工单', note: '关联当前工单' },
  { step: '04', title: '回复草稿', note: '形成回复草稿' },
  { step: '05', title: 'Human Review', note: '人工确认后才允许更新' }
]

const boundaryItems = [
  '当前为 keyword retrieval / RAG reference view demo。',
  'Vector DB 未启用，不声明生产级召回。',
  '知识草稿发布、回复使用和状态变更都需要 Human Review。',
  '不自动回复、不自动处理、不自动发布知识条目。'
]

const timeline: TimelineItem[] = [
  { time: '11:46', title: '知识条目被引用', detail: 'KB-API-500 被 DEMO-0005 草稿引用。', tone: 'success' },
  { time: '11:42', title: 'Chunk 命中', detail: 'chunk-api-500-03 命中报价接口、500、灰度发布。', tone: 'info' },
  { time: '11:38', title: '回复草稿生成', detail: '本地模板草稿已生成，等待支持人员复核。', tone: 'warning' },
  { time: '11:35', title: 'Human Review 请求', detail: '高优先级工单进入人工确认队列。', tone: 'info' },
  { time: '11:20', title: '知识草稿待发布', detail: '新排查说明等待发布确认。', tone: 'warning' }
]
</script>

<template>
  <div class="knowledge-showcase" data-screenshot="knowledge-base">
    <aside class="knowledge-showcase-sidebar" aria-label="工作区导航">
      <div class="knowledge-showcase-brand" aria-label="企业工单 RAG Copilot">
        <svg class="knowledge-showcase-brand-mark" viewBox="0 0 44 44" role="img" aria-label="企业 SaaS 品牌图标">
          <defs>
            <linearGradient id="knowledge-showcase-logo-fill" x1="8" y1="7" x2="36" y2="37" gradientUnits="userSpaceOnUse">
              <stop offset="0" stop-color="#21c7d9" />
              <stop offset="0.5" stop-color="#3d7cff" />
              <stop offset="1" stop-color="#8b7cf6" />
            </linearGradient>
            <linearGradient id="knowledge-showcase-logo-line" x1="13" y1="10" x2="31" y2="34" gradientUnits="userSpaceOnUse">
              <stop offset="0" stop-color="#eef5ff" />
              <stop offset="1" stop-color="#a8c9ff" />
            </linearGradient>
            <filter id="knowledge-showcase-logo-glow" x="-35%" y="-35%" width="170%" height="170%">
              <feGaussianBlur stdDeviation="2.3" result="blur" />
              <feColorMatrix in="blur" type="matrix" values="0 0 0 0 0.18 0 0 0 0 0.42 0 0 0 0 1 0 0 0 0.55 0" result="glow" />
              <feMerge>
                <feMergeNode in="glow" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
          </defs>
          <path
            d="M22 4.8 37 13.4v17.2L22 39.2 7 30.6V13.4L22 4.8Z"
            fill="rgba(8, 17, 31, 0.95)"
            stroke="url(#knowledge-showcase-logo-fill)"
            stroke-width="1.9"
            filter="url(#knowledge-showcase-logo-glow)"
          />
          <path d="M22 11.1 31.4 16.5v10.9L22 32.9l-9.4-5.5V16.5L22 11.1Z" fill="url(#knowledge-showcase-logo-fill)" opacity="0.24" />
          <path d="M22 11.1v10.8m0 0 9.4-5.4M22 21.9l-9.4-5.4m0 0v10.9l9.4 5.5 9.4-5.5V16.5" fill="none" stroke="url(#knowledge-showcase-logo-line)" stroke-width="1.45" stroke-linecap="round" stroke-linejoin="round" />
          <path d="M16.8 24.7 22 27.7l5.2-3" fill="none" stroke="#21c7d9" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
          <circle cx="22" cy="11.1" r="2.25" fill="#21c7d9" />
          <circle cx="12.6" cy="16.5" r="2.1" fill="#9fc4ff" />
          <circle cx="31.4" cy="16.5" r="2.1" fill="#8b7cf6" />
          <circle cx="22" cy="32.9" r="2.25" fill="#eef5ff" />
        </svg>
        <div>
          <strong>企业工单</strong>
          <span>RAG Copilot</span>
        </div>
      </div>

      <nav class="knowledge-showcase-nav" aria-label="主导航">
        <button
          v-for="item in navItems"
          :key="item.label"
          class="knowledge-showcase-nav-item"
          :class="{ 'knowledge-showcase-nav-item--active': item.active }"
          type="button"
        >
          <span aria-hidden="true"></span>
          {{ item.label }}
        </button>
      </nav>

      <section class="knowledge-showcase-boundary" aria-label="能力边界">
        <span>作品集演示</span>
        <strong>关键词检索视图</strong>
        <p>演示数据、无 Vector DB，知识发布需要人工确认。</p>
      </section>
    </aside>

    <section class="knowledge-showcase-main">
      <header class="knowledge-showcase-topbar">
        <label class="knowledge-showcase-search">
          <span>搜索</span>
          <input type="search" placeholder="搜索知识文档、Chunk、关键词、关联工单..." aria-label="搜索知识库演示工作区" readonly />
        </label>
        <div class="knowledge-showcase-topbar-actions" aria-label="运行状态">
          <span class="knowledge-showcase-chip knowledge-showcase-chip--green">演示环境</span>
          <span class="knowledge-showcase-chip knowledge-showcase-chip--blue">Provider: local-rule fallback</span>
          <span class="knowledge-showcase-chip knowledge-showcase-chip--muted">管理员演示</span>
        </div>
      </header>

      <main class="knowledge-showcase-content">
        <section class="knowledge-showcase-hero" aria-label="知识库概览">
          <div class="knowledge-showcase-hero-copy">
            <p class="knowledge-showcase-eyebrow">知识证据与引用审计控制台</p>
            <h1>知识库 / RAG 引用中心</h1>
            <p>统一展示知识文档、Chunk 命中、RAG 引用预览、关联工单与 Human Review 审核链路。</p>
            <div class="knowledge-showcase-hero-tags" aria-label="能力边界标签">
              <span>local-rule fallback</span>
              <span>keyword retrieval</span>
              <span>RAG reference view</span>
              <span>Human Review required</span>
            </div>
          </div>
          <div class="knowledge-showcase-status-grid" aria-label="知识能力状态">
            <section>
              <strong>关键词检索</strong>
              <span>Keyword retrieval</span>
              <em>启用</em>
              <small>本地关键词匹配</small>
            </section>
            <section>
              <strong>Vector DB</strong>
              <span>能力边界</span>
              <em>未启用</em>
              <small>当前不声明向量检索能力</small>
            </section>
            <section>
              <strong>RAG 引用视图</strong>
              <span>RAG Reference View</span>
              <em>demo evidence</em>
              <small>仅展示证据视图</small>
            </section>
            <section>
              <strong>人工审核</strong>
              <span>Human Review</span>
              <em>required</em>
              <small>发布前人工确认</small>
            </section>
          </div>
        </section>

        <section class="knowledge-showcase-kpi-grid" aria-label="知识库指标">
          <article v-for="kpi in kpis" :key="kpi.label" class="knowledge-showcase-kpi" :data-tone="kpi.tone">
            <span>{{ kpi.label }}</span>
            <strong>{{ kpi.value }}</strong>
            <p>{{ kpi.note }}</p>
          </article>
        </section>

        <section class="knowledge-showcase-workspace" aria-label="知识库与 RAG 引用工作区">
          <article class="knowledge-showcase-panel knowledge-showcase-doc-panel" aria-label="知识文档列表">
            <div class="knowledge-showcase-panel-header">
              <div>
                <p class="knowledge-showcase-eyebrow">知识源</p>
                <h2>知识文档</h2>
              </div>
              <span>证据文档</span>
            </div>
            <div class="knowledge-showcase-doc-list">
              <section
                v-for="document in documents"
                :key="document.id"
                class="knowledge-showcase-doc"
                :class="{ 'knowledge-showcase-doc--active': document.active }"
              >
                <div class="knowledge-showcase-doc-top">
                  <strong>{{ document.id }}</strong>
                  <em :data-status="document.statusTone">{{ document.status }}</em>
                </div>
                <h3>{{ document.title }}</h3>
                <p>{{ document.sourcePath }}</p>
                <div class="knowledge-showcase-doc-meta">
                  <span>{{ document.category }}</span>
                  <span>{{ document.chunks }} Chunks</span>
                  <span>{{ document.updatedAt }}</span>
                </div>
                <div class="knowledge-showcase-keywords">
                  <b v-for="keyword in document.keywords" :key="`${document.id}-${keyword}`">{{ keyword }}</b>
                </div>
              </section>
            </div>
          </article>

          <article class="knowledge-showcase-panel knowledge-showcase-chunk-panel" aria-label="Chunk 与关键词命中">
            <div class="knowledge-showcase-panel-header">
              <div>
                <p class="knowledge-showcase-eyebrow">证据命中</p>
                <h2>Chunk 命中证据</h2>
              </div>
              <span>demo score</span>
            </div>
            <div class="knowledge-showcase-chunk-list">
              <section v-for="chunk in chunkEvidence" :key="chunk.id" class="knowledge-showcase-chunk" :data-tone="chunk.scoreTone">
                <header>
                  <div>
                    <strong>{{ chunk.id }}</strong>
                    <span>{{ chunk.docId }} / {{ chunk.position }}</span>
                  </div>
                  <div class="knowledge-showcase-score">
                    <em>{{ chunk.score }}</em>
                    <i><span :style="{ width: chunk.scoreWidth }"></span></i>
                  </div>
                </header>
                <blockquote>{{ chunk.excerpt }}</blockquote>
                <div class="knowledge-showcase-chunk-meta">
                  <span>场景：{{ chunk.scenario }}</span>
                  <span>关联工单：{{ chunk.relatedTicket }}</span>
                  <span>更新：{{ chunk.updatedAt }}</span>
                </div>
                <div class="knowledge-showcase-keywords">
                  <b v-for="keyword in chunk.keywords" :key="`${chunk.id}-${keyword}`">{{ keyword }}</b>
                </div>
              </section>
            </div>
          </article>

          <aside class="knowledge-showcase-panel knowledge-showcase-preview-panel" aria-label="RAG 引用预览">
            <div class="knowledge-showcase-panel-header">
              <div>
                <p class="knowledge-showcase-eyebrow">当前工单证据</p>
                <h2>RAG 引用预览</h2>
              </div>
              <span>Human Review</span>
            </div>
            <div class="knowledge-showcase-decision-path" aria-label="引用决策路径">
              <span>RAG 引用</span>
              <span>工单证据</span>
              <span>回复草稿</span>
              <span>Human Review</span>
            </div>
            <div class="knowledge-showcase-preview-list">
              <section v-for="item in previewItems" :key="item.label" class="knowledge-showcase-preview-item">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <p>{{ item.note }}</p>
              </section>
            </div>

            <p class="knowledge-showcase-preview-note">仅作证据预览，不代表自动回复、自动处理或自动发布知识条目。</p>

            <section class="knowledge-showcase-related" aria-label="关联工单">
              <div class="knowledge-showcase-block-heading">
                <h3>关联工单</h3>
                <span>{{ relatedTickets.length }}</span>
              </div>
              <article v-for="ticket in relatedTickets.slice(0, 1)" :key="ticket.id" class="knowledge-showcase-ticket">
                <strong>{{ ticket.id }}</strong>
                <p>{{ ticket.title }}</p>
                <div>
                  <span>{{ ticket.status }}</span>
                  <em>{{ ticket.priority }}</em>
                </div>
              </article>
            </section>
          </aside>
        </section>

        <section class="knowledge-showcase-bottom-grid" aria-label="证据流、边界和活动">
          <article class="knowledge-showcase-panel knowledge-showcase-flow-panel" aria-label="知识到工单证据流">
            <div class="knowledge-showcase-panel-header">
              <div>
                <p class="knowledge-showcase-eyebrow">证据链路</p>
                <h2>知识到工单证据链</h2>
              </div>
            </div>
            <div class="knowledge-showcase-flow">
              <section v-for="step in evidenceFlow" :key="step.step" class="knowledge-showcase-flow-step">
                <span>{{ step.step }}</span>
                <strong>{{ step.title }}</strong>
                <p>{{ step.note }}</p>
              </section>
            </div>
          </article>

          <article class="knowledge-showcase-panel knowledge-showcase-boundary-panel" aria-label="能力边界">
            <div class="knowledge-showcase-panel-header">
              <div>
                <p class="knowledge-showcase-eyebrow">能力边界</p>
                <h2>能力边界</h2>
              </div>
            </div>
            <ul class="knowledge-showcase-boundary-list">
              <li v-for="item in boundaryItems" :key="item">{{ item }}</li>
            </ul>
          </article>

          <article class="knowledge-showcase-panel knowledge-showcase-timeline-panel" aria-label="最近活动">
            <div class="knowledge-showcase-panel-header">
              <div>
                <p class="knowledge-showcase-eyebrow">审计日志</p>
                <h2>最近活动</h2>
              </div>
            </div>
            <ol class="knowledge-showcase-timeline">
              <li v-for="item in timeline" :key="`${item.time}-${item.title}`" :data-tone="item.tone">
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
.knowledge-showcase {
  --knowledge-showcase-canvas: #07101d;
  --knowledge-showcase-deep: #040912;
  --knowledge-showcase-sidebar: #08111f;
  --knowledge-showcase-panel: #0c1726;
  --knowledge-showcase-panel-strong: #101f33;
  --knowledge-showcase-border: rgba(151, 180, 214, 0.16);
  --knowledge-showcase-border-strong: rgba(91, 141, 239, 0.24);
  --knowledge-showcase-text: #eef5ff;
  --knowledge-showcase-secondary: #c7d5ea;
  --knowledge-showcase-muted: #7e91ab;
  --knowledge-showcase-blue: #3d7cff;
  --knowledge-showcase-cyan: #21c7d9;
  --knowledge-showcase-green: #2bd88f;
  --knowledge-showcase-orange: #ffb45c;
  --knowledge-showcase-red: #ff5c7a;
  --knowledge-showcase-purple: #8b7cf6;
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr);
  min-height: 100vh;
  background:
    radial-gradient(circle at 73% 0, rgba(61, 124, 255, 0.18), transparent 34%),
    radial-gradient(circle at 32% 22%, rgba(139, 124, 246, 0.12), transparent 24%),
    var(--knowledge-showcase-canvas);
  color: var(--knowledge-showcase-text);
  font-family: Aptos, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.knowledge-showcase *,
.knowledge-showcase *::before,
.knowledge-showcase *::after {
  box-sizing: border-box;
}

.knowledge-showcase h1,
.knowledge-showcase h2,
.knowledge-showcase h3,
.knowledge-showcase p {
  letter-spacing: 0;
}

.knowledge-showcase-sidebar {
  position: sticky;
  top: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 18px;
  height: 100vh;
  padding: 14px 12px;
  border-right: 1px solid var(--knowledge-showcase-border);
  background:
    linear-gradient(180deg, rgba(8, 17, 31, 0.98), rgba(4, 9, 18, 0.98)),
    var(--knowledge-showcase-sidebar);
}

.knowledge-showcase-brand {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 11px;
  align-items: center;
  min-height: 50px;
}

.knowledge-showcase-brand-mark {
  width: 42px;
  height: 42px;
}

.knowledge-showcase-brand strong,
.knowledge-showcase-brand span {
  display: block;
}

.knowledge-showcase-brand strong {
  color: var(--knowledge-showcase-text);
  font-size: 15px;
  font-weight: 900;
  line-height: 1.2;
}

.knowledge-showcase-brand span {
  margin-top: 3px;
  color: #d6e5ff;
  font-size: 12px;
  font-weight: 800;
}

.knowledge-showcase-nav {
  display: grid;
  align-content: start;
  gap: 7px;
  min-height: 0;
  overflow: auto;
}

.knowledge-showcase-nav-item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 38px;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 0 10px;
  background: transparent;
  color: var(--knowledge-showcase-secondary);
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  text-align: left;
}

.knowledge-showcase-nav-item span {
  width: 8px;
  height: 8px;
  border-radius: 3px;
  background: rgba(126, 145, 171, 0.55);
}

.knowledge-showcase-nav-item--active {
  border-color: rgba(61, 124, 255, 0.48);
  background: linear-gradient(180deg, rgba(61, 124, 255, 0.3), rgba(33, 199, 217, 0.09));
  color: var(--knowledge-showcase-text);
  box-shadow: 0 12px 28px rgba(61, 124, 255, 0.16);
}

.knowledge-showcase-nav-item--active span {
  background: var(--knowledge-showcase-blue);
  box-shadow: 0 0 0 4px rgba(61, 124, 255, 0.16);
}

.knowledge-showcase-boundary {
  display: grid;
  gap: 7px;
  padding: 13px;
  border: 1px solid rgba(151, 180, 214, 0.13);
  border-radius: 8px;
  background: rgba(4, 10, 22, 0.66);
}

.knowledge-showcase-boundary span,
.knowledge-showcase-eyebrow {
  color: #9fc4ff;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
}

.knowledge-showcase-boundary strong {
  font-size: 13px;
}

.knowledge-showcase-boundary p {
  margin: 0;
  color: var(--knowledge-showcase-muted);
  font-size: 12px;
  line-height: 1.45;
}

.knowledge-showcase-main {
  min-width: 0;
}

.knowledge-showcase-topbar {
  position: sticky;
  top: 0;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(280px, 560px) minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  min-height: 56px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--knowledge-showcase-border);
  background: rgba(5, 11, 22, 0.9);
  backdrop-filter: blur(18px);
}

.knowledge-showcase-search {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 40px;
  border: 1px solid var(--knowledge-showcase-border);
  border-radius: 8px;
  padding: 0 12px;
  background: rgba(10, 21, 38, 0.72);
}

.knowledge-showcase-search span {
  color: var(--knowledge-showcase-muted);
  font-size: 12px;
  font-weight: 900;
}

.knowledge-showcase-search input {
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--knowledge-showcase-secondary);
}

.knowledge-showcase-search input::placeholder {
  color: rgba(199, 213, 234, 0.74);
}

.knowledge-showcase-topbar-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
}

.knowledge-showcase-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  border: 1px solid var(--knowledge-showcase-border);
  border-radius: 8px;
  padding: 0 10px;
  background: rgba(10, 21, 38, 0.74);
  color: var(--knowledge-showcase-secondary);
  font-size: 12px;
  font-weight: 900;
}

.knowledge-showcase-chip--green {
  border-color: rgba(43, 216, 143, 0.26);
  color: var(--knowledge-showcase-green);
}

.knowledge-showcase-chip--blue {
  border-color: rgba(61, 124, 255, 0.28);
  color: #9fc4ff;
}

.knowledge-showcase-chip--muted {
  border-color: rgba(151, 180, 214, 0.1);
  background: rgba(10, 21, 38, 0.42);
  color: rgba(199, 213, 234, 0.7);
}

.knowledge-showcase-content {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 7px 12px 12px;
}

.knowledge-showcase-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(480px, 0.92fr);
  gap: 9px;
  min-height: 98px;
  border: 1px solid rgba(91, 141, 239, 0.25);
  border-radius: 8px;
  padding: 9px 14px;
  overflow: hidden;
  background:
    radial-gradient(circle at 84% 34%, rgba(139, 124, 246, 0.26), transparent 28%),
    linear-gradient(135deg, rgba(16, 31, 51, 0.94), rgba(8, 17, 31, 0.92));
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.31);
}

.knowledge-showcase-eyebrow {
  margin: 0 0 5px;
}

.knowledge-showcase-hero h1 {
  margin: 0;
  color: var(--knowledge-showcase-text);
  font-size: 30px;
  line-height: 1.05;
}

.knowledge-showcase-hero-copy > p:not(.knowledge-showcase-eyebrow) {
  max-width: 720px;
  margin: 6px 0 0;
  color: var(--knowledge-showcase-secondary);
  font-size: 13.5px;
  line-height: 1.34;
}

.knowledge-showcase-hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 7px;
}

.knowledge-showcase-hero-tags span {
  min-height: 21px;
  border: 1px solid rgba(33, 199, 217, 0.15);
  border-radius: 6px;
  padding: 3px 7px;
  background: rgba(4, 10, 22, 0.42);
  color: rgba(199, 213, 234, 0.82);
  font-size: 10.5px;
  font-weight: 800;
}

.knowledge-showcase-status-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 7px;
  align-content: stretch;
}

.knowledge-showcase-status-grid section {
  display: grid;
  gap: 3px;
  min-width: 0;
  border: 1px solid rgba(151, 180, 214, 0.11);
  border-radius: 8px;
  padding: 7px 8px;
  background: linear-gradient(180deg, rgba(14, 27, 47, 0.8), rgba(4, 10, 22, 0.62));
}

.knowledge-showcase-status-grid span,
.knowledge-showcase-status-grid small {
  color: var(--knowledge-showcase-muted);
  font-size: 10px;
  line-height: 1.28;
}

.knowledge-showcase-status-grid small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-showcase-status-grid strong {
  color: var(--knowledge-showcase-text);
  font-size: 12.5px;
  line-height: 1.18;
}

.knowledge-showcase-status-grid em {
  color: rgba(159, 196, 255, 0.95);
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
  line-height: 1.2;
}

.knowledge-showcase-kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 7px;
}

.knowledge-showcase-kpi {
  position: relative;
  display: grid;
  gap: 3px;
  min-height: 56px;
  border: 1px solid rgba(151, 180, 214, 0.14);
  border-radius: 8px;
  padding: 8px 10px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(16, 31, 51, 0.9), rgba(9, 18, 32, 0.96));
}

.knowledge-showcase-kpi::before {
  position: absolute;
  inset: 0 0 auto;
  height: 2px;
  content: '';
  background: rgba(61, 124, 255, 0.86);
}

.knowledge-showcase-kpi[data-tone='green']::before {
  background: rgba(43, 216, 143, 0.82);
}

.knowledge-showcase-kpi[data-tone='orange']::before {
  background: rgba(255, 180, 92, 0.82);
}

.knowledge-showcase-kpi[data-tone='purple']::before {
  background: rgba(139, 124, 246, 0.82);
}

.knowledge-showcase-kpi[data-tone='cyan']::before {
  background: rgba(33, 199, 217, 0.82);
}

.knowledge-showcase-kpi span,
.knowledge-showcase-kpi p {
  margin: 0;
  color: var(--knowledge-showcase-muted);
  font-size: 12px;
}

.knowledge-showcase-kpi span {
  font-weight: 900;
}

.knowledge-showcase-kpi strong {
  color: var(--knowledge-showcase-text);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 21px;
  line-height: 1;
}

.knowledge-showcase-workspace {
  display: grid;
  grid-template-columns: minmax(280px, 0.88fr) minmax(0, 1.18fr) minmax(310px, 0.94fr);
  gap: 8px;
  height: 340px;
  min-width: 0;
}

.knowledge-showcase-panel {
  min-width: 0;
  border: 1px solid rgba(151, 180, 214, 0.135);
  border-radius: 8px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(16, 31, 51, 0.9), rgba(8, 17, 31, 0.98)),
    var(--knowledge-showcase-panel);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.18);
}

.knowledge-showcase-doc-panel,
.knowledge-showcase-chunk-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.knowledge-showcase-preview-panel {
  display: flex;
  flex-direction: column;
}

.knowledge-showcase-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-height: 42px;
  padding: 8px 11px;
  border-bottom: 1px solid var(--knowledge-showcase-border);
}

.knowledge-showcase-panel-header h2 {
  margin: 0;
  color: var(--knowledge-showcase-text);
  font-size: 15px;
  line-height: 1.12;
}

.knowledge-showcase-panel-header > span {
  border: 1px solid rgba(61, 124, 255, 0.18);
  border-radius: 6px;
  padding: 4px 7px;
  color: rgba(159, 196, 255, 0.78);
  background: rgba(61, 124, 255, 0.055);
  font-size: 10.5px;
  font-weight: 900;
  white-space: nowrap;
}

.knowledge-showcase-doc-list,
.knowledge-showcase-chunk-list,
.knowledge-showcase-preview-list {
  display: grid;
  gap: 6px;
  padding: 8px;
}

.knowledge-showcase-doc-list,
.knowledge-showcase-chunk-list {
  min-height: 0;
  overflow: auto;
}

.knowledge-showcase-preview-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  flex: 0 0 auto;
}

.knowledge-showcase-doc,
.knowledge-showcase-chunk,
.knowledge-showcase-preview-item,
.knowledge-showcase-ticket,
.knowledge-showcase-flow-step {
  min-width: 0;
  border: 1px solid rgba(151, 180, 214, 0.09);
  border-radius: 8px;
  padding: 7px;
  background: rgba(4, 10, 22, 0.34);
}

.knowledge-showcase-doc--active {
  border-color: rgba(61, 124, 255, 0.58);
  background: linear-gradient(135deg, rgba(61, 124, 255, 0.17), rgba(33, 199, 217, 0.08));
  box-shadow: 0 0 0 1px rgba(61, 124, 255, 0.2), 0 0 22px rgba(61, 124, 255, 0.16);
}

.knowledge-showcase-doc-top,
.knowledge-showcase-chunk header,
.knowledge-showcase-ticket div,
.knowledge-showcase-block-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.knowledge-showcase-doc-top strong,
.knowledge-showcase-chunk header strong,
.knowledge-showcase-ticket strong {
  color: #9fc4ff;
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 10.5px;
}

.knowledge-showcase-doc-top em,
.knowledge-showcase-chunk header em,
.knowledge-showcase-ticket em,
.knowledge-showcase-block-heading span {
  border-radius: 6px;
  padding: 3px 6px;
  color: var(--knowledge-showcase-green);
  background: rgba(43, 216, 143, 0.1);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 10px;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.knowledge-showcase-doc-top em[data-status='review'] {
  color: var(--knowledge-showcase-orange);
  background: rgba(255, 180, 92, 0.1);
}

.knowledge-showcase-doc h3 {
  margin: 5px 0 0;
  color: var(--knowledge-showcase-text);
  font-size: 12.5px;
  line-height: 1.22;
}

.knowledge-showcase-doc p {
  margin: 3px 0 0;
  overflow: hidden;
  color: var(--knowledge-showcase-muted);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-showcase-doc-meta,
.knowledge-showcase-chunk-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 5px;
}

.knowledge-showcase-doc-meta span,
.knowledge-showcase-chunk-meta span {
  color: rgba(199, 213, 234, 0.62);
  font-size: 10px;
  line-height: 1.3;
}

.knowledge-showcase-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 5px;
}

.knowledge-showcase-keywords b {
  border: 1px solid rgba(33, 199, 217, 0.13);
  border-radius: 6px;
  padding: 3px 5px;
  background: rgba(33, 199, 217, 0.055);
  color: rgba(159, 232, 241, 0.88);
  font-size: 10px;
  font-weight: 850;
}

.knowledge-showcase-chunk {
  display: grid;
  gap: 6px;
  padding: 8px;
  border-left: 2px solid rgba(33, 199, 217, 0.28);
}

.knowledge-showcase-chunk[data-tone='medium'] header em {
  color: var(--knowledge-showcase-orange);
  background: rgba(255, 180, 92, 0.1);
}

.knowledge-showcase-chunk header span {
  display: block;
  margin-top: 2px;
  color: var(--knowledge-showcase-muted);
  font-size: 10px;
}

.knowledge-showcase-chunk blockquote {
  margin: 0;
  border-left: 2px solid rgba(33, 199, 217, 0.46);
  border-radius: 6px;
  padding: 6px 8px;
  background: rgba(33, 199, 217, 0.048);
  color: var(--knowledge-showcase-secondary);
  font-size: 11.5px;
  line-height: 1.42;
}

.knowledge-showcase-score {
  display: grid;
  gap: 4px;
  justify-items: end;
  min-width: 70px;
}

.knowledge-showcase-score em {
  border-color: rgba(139, 124, 246, 0.16);
  background: rgba(139, 124, 246, 0.075);
  color: rgba(202, 196, 255, 0.82);
}

.knowledge-showcase-score i {
  display: block;
  width: 62px;
  height: 4px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(151, 180, 214, 0.12);
}

.knowledge-showcase-score i span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(139, 124, 246, 0.62), rgba(33, 199, 217, 0.66));
}

.knowledge-showcase-preview-item {
  display: grid;
  gap: 3px;
  padding: 7px;
  border-color: rgba(151, 180, 214, 0.08);
  background: rgba(5, 12, 24, 0.5);
}

.knowledge-showcase-preview-item:nth-child(2),
.knowledge-showcase-preview-item:nth-child(3),
.knowledge-showcase-preview-item:nth-child(4) {
  background: rgba(7, 15, 29, 0.52);
}

.knowledge-showcase-preview-item span {
  color: var(--knowledge-showcase-muted);
  font-size: 10.5px;
  font-weight: 900;
}

.knowledge-showcase-preview-item strong {
  color: var(--knowledge-showcase-text);
  font-size: 12.5px;
}

.knowledge-showcase-preview-item p {
  margin: 0;
  display: -webkit-box;
  overflow: hidden;
  color: rgba(199, 213, 234, 0.64);
  font-size: 10.5px;
  line-height: 1.28;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.knowledge-showcase-related {
  display: grid;
  gap: 5px;
  flex: 1 1 auto;
  min-height: 0;
  margin: 0 8px 8px;
  overflow: hidden;
}

.knowledge-showcase-decision-path {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 4px;
  padding: 7px 8px 0;
}

.knowledge-showcase-decision-path span {
  position: relative;
  min-width: 0;
  border: 1px solid rgba(33, 199, 217, 0.12);
  border-radius: 7px;
  padding: 5px 4px;
  background: rgba(33, 199, 217, 0.045);
  color: rgba(199, 213, 234, 0.72);
  font-size: 9.5px;
  font-weight: 900;
  text-align: center;
}

.knowledge-showcase-decision-path span:not(:last-child)::after {
  position: absolute;
  top: 50%;
  right: -7px;
  width: 7px;
  height: 1px;
  content: '';
  background: rgba(33, 199, 217, 0.34);
}

.knowledge-showcase-block-heading h3 {
  margin: 0;
  color: var(--knowledge-showcase-text);
  font-size: 13px;
}

.knowledge-showcase-ticket {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 3px;
  padding: 6px 8px;
}

.knowledge-showcase-ticket p {
  margin: 0;
  overflow: hidden;
  color: var(--knowledge-showcase-secondary);
  font-size: 11px;
  line-height: 1.28;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-showcase-ticket div {
  display: inline-flex;
  gap: 5px;
}

.knowledge-showcase-ticket span {
  color: rgba(199, 213, 234, 0.64);
  font-size: 11px;
}

.knowledge-showcase-preview-note {
  margin: 0 8px 7px;
  border: 1px solid rgba(139, 124, 246, 0.15);
  border-radius: 8px;
  padding: 7px 8px;
  background: rgba(139, 124, 246, 0.055);
  color: rgba(199, 213, 234, 0.74);
  font-size: 10.5px;
  line-height: 1.3;
}

.knowledge-showcase-bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(270px, 0.75fr) minmax(320px, 0.9fr);
  gap: 8px;
  align-items: start;
}

.knowledge-showcase-bottom-grid .knowledge-showcase-panel {
  border-color: rgba(151, 180, 214, 0.06);
  background:
    linear-gradient(180deg, rgba(14, 27, 46, 0.72), rgba(8, 17, 31, 0.84)),
    var(--knowledge-showcase-panel);
  box-shadow: none;
}

.knowledge-showcase-flow {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 7px;
  padding: 8px;
  align-items: stretch;
}

.knowledge-showcase-flow-step {
  position: relative;
  display: grid;
  align-content: center;
  min-height: 70px;
  padding: 8px 7px 7px;
}

.knowledge-showcase-flow-step:not(:last-child)::after {
  position: absolute;
  top: 50%;
  right: -8px;
  width: 9px;
  height: 2px;
  content: '';
  background: rgba(33, 199, 217, 0.42);
  transform: translateY(-50%);
}

.knowledge-showcase-flow-step span {
  display: inline-grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border: 1px solid rgba(33, 199, 217, 0.28);
  border-radius: 999px;
  color: var(--knowledge-showcase-cyan);
  background: rgba(33, 199, 217, 0.08);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 10px;
  font-weight: 900;
}

.knowledge-showcase-flow-step strong {
  display: block;
  margin-top: 5px;
  color: var(--knowledge-showcase-text);
  font-size: 11.5px;
}

.knowledge-showcase-flow-step p {
  margin: 4px 0 0;
  color: var(--knowledge-showcase-muted);
  font-size: 10.5px;
  line-height: 1.28;
}

.knowledge-showcase-boundary-list {
  display: grid;
  gap: 5px;
  margin: 0;
  padding: 8px 12px 10px 25px;
  color: rgba(199, 213, 234, 0.78);
  font-size: 11px;
  line-height: 1.35;
}

.knowledge-showcase-boundary-list li::marker {
  color: var(--knowledge-showcase-orange);
}

.knowledge-showcase-timeline {
  display: grid;
  gap: 5px;
  margin: 0;
  padding: 8px 10px 10px;
  list-style: none;
}

.knowledge-showcase-timeline li {
  position: relative;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 8px;
  padding-left: 10px;
  border-left: 2px solid rgba(151, 180, 214, 0.12);
}

.knowledge-showcase-timeline li::before {
  position: absolute;
  top: 3px;
  left: -6px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  content: '';
  background: var(--knowledge-showcase-cyan);
  box-shadow: 0 0 0 4px rgba(33, 199, 217, 0.09);
}

.knowledge-showcase-timeline li[data-tone='success']::before {
  background: var(--knowledge-showcase-cyan);
}

.knowledge-showcase-timeline li[data-tone='warning']::before {
  background: var(--knowledge-showcase-orange);
}

.knowledge-showcase-timeline time {
  color: var(--knowledge-showcase-cyan);
  font-family: 'Cascadia Code', SFMono-Regular, Consolas, monospace;
  font-size: 10.5px;
  font-weight: 900;
}

.knowledge-showcase-timeline strong {
  color: var(--knowledge-showcase-text);
  font-size: 11.5px;
}

.knowledge-showcase-timeline p {
  margin: 2px 0 0;
  color: var(--knowledge-showcase-muted);
  font-size: 10.5px;
  line-height: 1.3;
}

@media (max-width: 1280px) {
  .knowledge-showcase {
    grid-template-columns: 208px minmax(0, 1fr);
  }

  .knowledge-showcase-hero,
  .knowledge-showcase-workspace,
  .knowledge-showcase-bottom-grid {
    grid-template-columns: 1fr;
  }

  .knowledge-showcase-status-grid,
  .knowledge-showcase-kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .knowledge-showcase-flow {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .knowledge-showcase-flow-step:not(:last-child)::after {
    display: none;
  }
}

@media (max-width: 880px) {
  .knowledge-showcase {
    grid-template-columns: 1fr;
  }

  .knowledge-showcase-sidebar {
    position: relative;
    height: auto;
  }

  .knowledge-showcase-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .knowledge-showcase-topbar,
  .knowledge-showcase-status-grid,
  .knowledge-showcase-kpi-grid,
  .knowledge-showcase-flow {
    grid-template-columns: 1fr;
  }

  .knowledge-showcase-topbar-actions {
    justify-content: flex-start;
  }

  .knowledge-showcase-hero h1 {
    font-size: 30px;
  }
}
</style>
