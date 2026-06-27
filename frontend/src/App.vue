<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import DashboardShowcaseView from './views/DashboardShowcaseView.vue'
import HumanReviewShowcaseView from './views/HumanReviewShowcaseView.vue'
import KnowledgeRagShowcaseView from './views/KnowledgeRagShowcaseView.vue'
import TicketWorkbenchShowcaseView from './views/TicketWorkbenchShowcaseView.vue'
import TraceShowcaseView from './views/TraceShowcaseView.vue'

type ShowcaseRoute = 'dashboard' | 'ticket-detail' | 'knowledge-base' | 'trace-evidence' | 'human-review'

const routeAliases: Record<string, ShowcaseRoute> = {
  dashboard: 'dashboard',
  tickets: 'ticket-detail',
  'ticket-detail': 'ticket-detail',
  'ticket-workbench': 'ticket-detail',
  knowledge: 'knowledge-base',
  rag: 'knowledge-base',
  'knowledge-base': 'knowledge-base',
  trace: 'trace-evidence',
  'trace-evidence': 'trace-evidence',
  review: 'human-review',
  'human-review': 'human-review'
}

const navRouteLabels: Record<string, ShowcaseRoute> = {
  仪表盘: 'dashboard',
  工单管理: 'ticket-detail',
  知识库: 'knowledge-base',
  'RAG 引用': 'knowledge-base',
  'Trace 证据': 'trace-evidence',
  'Human Review': 'human-review'
}

const showcaseComponents = {
  dashboard: DashboardShowcaseView,
  'ticket-detail': TicketWorkbenchShowcaseView,
  'knowledge-base': KnowledgeRagShowcaseView,
  'trace-evidence': TraceShowcaseView,
  'human-review': HumanReviewShowcaseView
}

const activeRoute = ref<ShowcaseRoute>(readHashRoute())
const activeComponent = computed(() => showcaseComponents[activeRoute.value])

function readHashRoute(): ShowcaseRoute {
  const hash = window.location.hash.replace(/^#\/?/, '').trim()
  return routeAliases[hash] ?? 'dashboard'
}

function navigateTo(route: ShowcaseRoute) {
  if (activeRoute.value === route) {
    return
  }
  activeRoute.value = route
  window.history.pushState(null, '', `#${route}`)
}

function handleHashChange() {
  activeRoute.value = readHashRoute()
}

function handleShowcaseNavigation(event: MouseEvent) {
  const target = event.target
  if (!(target instanceof HTMLElement)) {
    return
  }
  const label = target.textContent?.trim()
  if (!label) {
    return
  }
  const route = navRouteLabels[label]
  if (route) {
    navigateTo(route)
  }
}

onMounted(() => {
  window.addEventListener('hashchange', handleHashChange)
  document.addEventListener('click', handleShowcaseNavigation)
})

onBeforeUnmount(() => {
  window.removeEventListener('hashchange', handleHashChange)
  document.removeEventListener('click', handleShowcaseNavigation)
})
</script>

<template>
  <component :is="activeComponent" />
</template>
