<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, type Component, watch } from 'vue'
import { isDemoRuntime } from './api/tickets'
import { useTicketRealFlow } from './composables/useTicketRealFlow'
import AppSidebar from './components/layout/AppSidebar.vue'
import AppTopbar from './components/layout/AppTopbar.vue'
import NavIcon from './components/layout/NavIcon.vue'
import DashboardShowcaseView from './views/DashboardShowcaseView.vue'
import EvaluationMetricsShowcaseView from './views/EvaluationMetricsShowcaseView.vue'
import HumanReviewShowcaseView from './views/HumanReviewShowcaseView.vue'
import KnowledgeRagShowcaseView from './views/KnowledgeRagShowcaseView.vue'
import TicketWorkbenchShowcaseView from './views/TicketWorkbenchShowcaseView.vue'
import TraceShowcaseView from './views/TraceShowcaseView.vue'
import TraceTimelineShowcaseView from './views/TraceTimelineShowcaseView.vue'

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
  icon: 'overview' | 'workbench' | 'knowledge' | 'evidence' | 'trace' | 'review' | 'evaluation'
}

interface RouteMeta {
  eyebrow: string
  title: string
  description: string
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
  'trace-evidence': 'retrieval-evidence',
  'trace-timeline': 'trace-timeline',
  review: 'human-review',
  'human-review': 'human-review',
  eval: 'evaluation-metrics',
  evaluation: 'evaluation-metrics',
  metrics: 'evaluation-metrics',
  'evaluation-metrics': 'evaluation-metrics'
}

const navItems: NavItem[] = [
  { label: '总览', caption: '支持运营', route: 'dashboard', icon: 'overview' },
  { label: '工单工作台', caption: '队列与处理', route: 'ticket-detail', icon: 'workbench' },
  { label: '知识库', caption: '关联内容', route: 'knowledge-base', icon: 'knowledge' },
  { label: '检索证据', caption: '引用与来源', route: 'retrieval-evidence', icon: 'evidence' },
  { label: '运行记录', caption: '处理过程', route: 'trace-timeline', icon: 'trace' },
  { label: '人工复核', caption: '待决策', route: 'human-review', icon: 'review' },
  { label: '评测指标', caption: '离线数据', route: 'evaluation-metrics', icon: 'evaluation' }
]

const showcaseComponents: Record<ShowcaseRoute, Component> = {
  dashboard: DashboardShowcaseView,
  'ticket-detail': TicketWorkbenchShowcaseView,
  'knowledge-base': KnowledgeRagShowcaseView,
  'retrieval-evidence': TraceShowcaseView,
  'trace-timeline': TraceTimelineShowcaseView,
  'human-review': HumanReviewShowcaseView,
  'evaluation-metrics': EvaluationMetricsShowcaseView
}

const routeMeta: Record<ShowcaseRoute, RouteMeta> = {
  dashboard: {
    eyebrow: '支持运营 / 总览',
    title: '总览',
    description: '从当前工单队列、AI 证据和复核门禁快速判断系统下一步。'
  },
  'ticket-detail': {
    eyebrow: '工单 / 工作台',
    title: '工单工作台',
    description: '队列、工单上下文、Copilot 建议、证据和审核动作保持在一个工作区。'
  },
  'knowledge-base': {
    eyebrow: '知识库 / 只读',
    title: '知识库',
    description: '查看当前工单关联的知识命中与检索快照；本页只读，不扩展知识库 CRUD。'
  },
  'retrieval-evidence': {
    eyebrow: '证据 / 引用',
    title: '检索证据',
    description: '把检索参考与已校验 Citation 分层展示，避免把命中误读为事实正确。'
  },
  'trace-timeline': {
    eyebrow: '运行记录 / 详情',
    title: '运行记录',
    description: '回放一次 Copilot 运行的步骤、状态、延迟、fallback 和人工复核证据。'
  },
  'human-review': {
    eyebrow: '人工复核 / 待决策',
    title: '人工复核',
    description: '在可审计的建议、风险与引用上下文中完成最终人工决策。'
  },
  'evaluation-metrics': {
    eyebrow: '评测 / 本地数据',
    title: '评测指标中心',
    description: '查看本地 RAG 评测 baseline、引用证据指标与下一阶段实验边界。'
  }
}

const flow = useTicketRealFlow()
const activeRoute = ref<ShowcaseRoute>(readHashRoute())
const commandOpen = ref(false)
const commandQuery = ref('')
const commandActiveIndex = ref(0)
const commandDialogRef = ref<HTMLElement | null>(null)
const commandInputRef = ref<HTMLInputElement | null>(null)
const commandTriggerRef = ref<HTMLElement | null>(null)
const activeComponent = computed(() => showcaseComponents[activeRoute.value])
const activeMeta = computed(() => routeMeta[activeRoute.value])
const activeNavLabel = computed(() => navItems.find((item) => item.route === activeRoute.value)?.label ?? '总览')
const pendingReviewCount = computed(() => flow.pendingReviewCount.value)
const runtimeLabel = computed(() => {
  if (isDemoRuntime) {
    return 'Demo · 本地演示'
  }
  if (flow.backendState.value === 'CONNECTED') {
    return 'Real · 后端 API'
  }
  if (flow.backendState.value === 'UNAVAILABLE') {
    return '后端不可用'
  }
  return '正在连接后端'
})
const runtimeTone = computed<'demo' | 'real' | 'error' | 'checking'>(() => {
  if (isDemoRuntime) {
    return 'demo'
  }
  if (flow.backendState.value === 'CONNECTED') {
    return 'real'
  }
  if (flow.backendState.value === 'UNAVAILABLE') {
    return 'error'
  }
  return 'checking'
})
const providerLabel = computed(() => {
  if (isDemoRuntime) {
    return '本地规则路径'
  }
  return flow.backendState.value === 'CONNECTED' ? '后端处理' : '尚未确认'
})
const providerTone = computed<'connected' | 'fallback' | 'unknown'>(() => {
  if (isDemoRuntime) {
    return 'fallback'
  }
  return flow.backendState.value === 'CONNECTED' ? 'connected' : 'unknown'
})
const commandItems = computed(() => {
  const query = commandQuery.value.trim().toLowerCase()
  if (!query) {
    return navItems
  }
  return navItems.filter((item) => `${item.label} ${item.caption}`.toLowerCase().includes(query))
})
const activeCommandOptionId = computed(() => {
  const item = commandItems.value[commandActiveIndex.value]
  return item ? commandOptionId(item.route) : undefined
})

watch(commandQuery, () => {
  commandActiveIndex.value = 0
})

watch(commandItems, (items) => {
  if (commandActiveIndex.value >= items.length) {
    commandActiveIndex.value = Math.max(items.length - 1, 0)
  }
})

function readHashRoute(): ShowcaseRoute {
  const hash = window.location.hash.replace(/^#\/?/, '').trim()
  return routeAliases[hash] ?? 'dashboard'
}

function navigateTo(route: string) {
  const nextRoute = routeAliases[route] ?? 'dashboard'
  activeRoute.value = nextRoute
  closeCommandPalette(false)
  window.history.pushState(null, '', `#${nextRoute}`)
}

function handleRouteChange() {
  activeRoute.value = readHashRoute()
}

function commandOptionId(route: ShowcaseRoute) {
  return `command-option-${route}`
}

function openCommandPalette(trigger?: HTMLElement | null) {
  commandTriggerRef.value = trigger ?? (document.activeElement instanceof HTMLElement ? document.activeElement : null)
  commandOpen.value = true
  commandQuery.value = ''
  commandActiveIndex.value = 0
  void nextTick(() => commandInputRef.value?.focus())
}

function closeCommandPalette(restoreFocus = true) {
  const trigger = commandTriggerRef.value
  commandOpen.value = false
  commandQuery.value = ''
  commandActiveIndex.value = 0
  commandTriggerRef.value = null
  if (restoreFocus) {
    void nextTick(() => trigger?.focus())
  }
}

function commandFocusableElements() {
  const dialog = commandDialogRef.value
  if (!dialog) {
    return []
  }
  return Array.from(dialog.querySelectorAll<HTMLElement>('input:not([disabled]), button:not([disabled])'))
    .filter((element) => element.offsetParent !== null)
}

function handleCommandKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    closeCommandPalette()
    return
  }

  if (event.key === 'Tab') {
    const focusable = commandFocusableElements()
    if (!focusable.length) {
      return
    }
    const first = focusable[0]
    const last = focusable[focusable.length - 1]
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first.focus()
    }
    return
  }

  if (!commandItems.value.length) {
    return
  }

  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault()
    const direction = event.key === 'ArrowDown' ? 1 : -1
    commandActiveIndex.value = (commandActiveIndex.value + direction + commandItems.value.length) % commandItems.value.length
    return
  }

  if (event.key === 'Enter' && event.target === commandInputRef.value) {
    event.preventDefault()
    const item = commandItems.value[commandActiveIndex.value]
    if (item) {
      navigateTo(item.route)
    }
  }
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    if (commandOpen.value) {
      closeCommandPalette()
    } else {
      openCommandPalette()
    }
    return
  }
  if (event.key === 'Escape' && commandOpen.value) {
    event.preventDefault()
    closeCommandPalette()
  }
}

onMounted(() => {
  window.addEventListener('hashchange', handleRouteChange)
  window.addEventListener('popstate', handleRouteChange)
  window.addEventListener('keydown', handleGlobalKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('hashchange', handleRouteChange)
  window.removeEventListener('popstate', handleRouteChange)
  window.removeEventListener('keydown', handleGlobalKeydown)
})
</script>

<template>
  <div class="app-shell" :data-active-route="activeRoute">
    <a class="skip-link" href="#main-content">跳到主要内容</a>

    <AppSidebar
      :active-route="activeRoute"
      :items="navItems"
      :pending-review-count="pendingReviewCount"
      :runtime-label="runtimeLabel"
      @navigate="navigateTo"
    />

    <div class="app-shell__workspace">
      <AppTopbar
        :eyebrow="activeMeta.eyebrow"
        :title="activeNavLabel"
        :runtime-label="runtimeLabel"
        :runtime-tone="runtimeTone"
        :provider-label="providerLabel"
        :provider-tone="providerTone"
        @open-search="openCommandPalette()"
      />

      <main id="main-content" class="app-main">
        <div class="app-main__content">
          <component :is="activeComponent" />
        </div>
      </main>
    </div>

    <div v-if="commandOpen" class="command-layer" @click.self="closeCommandPalette()">
      <section
        ref="commandDialogRef"
        class="command-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="command-dialog-title"
        @keydown="handleCommandKeydown"
      >
        <h2 id="command-dialog-title" class="visually-hidden">页面搜索</h2>
        <div class="command-dialog__header">
          <span class="command-dialog__icon" aria-hidden="true">⌕</span>
          <input
            ref="commandInputRef"
            v-model="commandQuery"
            autofocus
            role="combobox"
            aria-label="搜索页面"
            aria-autocomplete="list"
            aria-controls="command-dialog-list"
            aria-expanded="true"
            :aria-activedescendant="activeCommandOptionId"
            placeholder="跳转到页面…"
          />
          <kbd>Esc</kbd>
        </div>
        <div id="command-dialog-list" class="command-dialog__list" role="listbox" aria-label="页面结果">
          <button
            v-for="(item, index) in commandItems"
            :id="commandOptionId(item.route)"
            :key="item.route"
            type="button"
            role="option"
            :aria-selected="index === commandActiveIndex"
            :class="{ 'command-dialog__item--active': index === commandActiveIndex }"
            @mouseenter="commandActiveIndex = index"
            @focus="commandActiveIndex = index"
            @click="navigateTo(item.route)"
          >
            <span class="app-sidebar__nav-icon" aria-hidden="true"><NavIcon :name="item.icon" /></span>
            <span><strong>{{ item.label }}</strong><small>{{ item.caption }}</small></span>
            <span v-if="item.route === 'human-review' && pendingReviewCount" class="app-sidebar__count">{{ pendingReviewCount }}</span>
          </button>
          <p v-if="!commandItems.length" class="command-dialog__empty">没有匹配的页面</p>
        </div>
        <p class="command-dialog__hint">Ctrl K 打开 · ↑↓ 选择 · Enter 进入 · Esc 关闭</p>
      </section>
    </div>
  </div>
</template>
