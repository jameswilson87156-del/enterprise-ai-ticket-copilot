import { computed, readonly, ref, shallowRef } from 'vue'
import {
  approveReview,
  createTicket,
  fetchAiAnalysis,
  fetchHealth,
  fetchMetrics,
  fetchTicket,
  fetchTickets,
  fetchTraceEvidence,
  isDemoRuntime,
  rejectReview,
  requestReviewChanges,
  runCopilot,
  TicketApiError
} from '../api/tickets'
import type {
  AiAnalysis,
  CreateTicketRequest,
  ReviewDecisionRequest,
  TicketDetail,
  TicketSummary,
  TraceEvidence,
  WorkbenchMetrics
} from '../types/ticket'

type BackendState = 'UNKNOWN' | 'CONNECTED' | 'UNAVAILABLE'
type ReviewDecision = 'approve' | 'request-changes' | 'reject'

const tickets = shallowRef<TicketSummary[]>([])
const selectedTicket = shallowRef<TicketDetail | null>(null)
const selectedId = ref<string | null>(null)
const analysis = shallowRef<AiAnalysis | null>(null)
const trace = shallowRef<TraceEvidence | null>(null)
const metrics = shallowRef<WorkbenchMetrics | null>(null)
const backendState = ref<BackendState>('UNKNOWN')
const loadingTickets = ref(false)
const loadingDetail = ref(false)
const creatingTicket = ref(false)
const runningTicketId = ref<string | null>(null)
const reviewSubmitting = ref(false)
const lastError = ref<string | null>(null)
const analysisError = ref<string | null>(null)
const traceError = ref<string | null>(null)

const selectedSummary = computed(() => tickets.value.find((item) => item.id === selectedId.value) ?? null)
const isBackendConnected = computed(() => backendState.value === 'CONNECTED')
const reviewHistory = computed(() => trace.value?.reviewRecords ?? [])
const currentRun = computed(() => trace.value?.copilotRun ?? null)
const currentStructuredOutput = computed(() => analysis.value?.structuredOutput ?? trace.value?.structuredOutput ?? null)
const currentValidatedCitations = computed(() => analysis.value?.validatedCitations ?? trace.value?.validatedCitations ?? [])
const currentRagReferences = computed(() => trace.value?.ragReferences ?? [])
const pendingReviewCount = computed(() => tickets.value.filter((item) => item.status === 'REVIEW_REQUIRED' || item.status === 'AI_DRAFTED').length)

function safeMessage(error: unknown) {
  if (error instanceof TicketApiError) {
    return error.safeMessage
  }
  if (error instanceof Error && error.message) {
    return error.message.slice(0, 240)
  }
  return '后端请求失败。'
}

async function probeBackend() {
  if (isDemoRuntime) {
    backendState.value = 'CONNECTED'
    return
  }
  try {
    await fetchHealth()
    backendState.value = 'CONNECTED'
  } catch (error) {
    backendState.value = 'UNAVAILABLE'
    throw error
  }
}

async function refresh() {
  loadingTickets.value = true
  lastError.value = null
  try {
    await probeBackend()
    const [ticketRows, metricRows] = await Promise.all([fetchTickets(), fetchMetrics().catch(() => null)])
    tickets.value = ticketRows
    metrics.value = metricRows
    const nextId = selectedId.value && ticketRows.some((ticket) => ticket.id === selectedId.value)
      ? selectedId.value
      : ticketRows[0]?.id ?? null
    if (nextId) {
      await selectTicket(nextId)
    } else {
      selectedId.value = null
      selectedTicket.value = null
      analysis.value = null
      trace.value = null
    }
  } catch (error) {
    backendState.value = 'UNAVAILABLE'
    lastError.value = safeMessage(error)
    tickets.value = []
    metrics.value = null
    selectedId.value = null
    selectedTicket.value = null
    analysis.value = null
    trace.value = null
  } finally {
    loadingTickets.value = false
  }
}

async function selectTicket(id: string) {
  selectedId.value = id
  loadingDetail.value = true
  analysisError.value = null
  traceError.value = null
  try {
    selectedTicket.value = await fetchTicket(id)
    try {
      analysis.value = await fetchAiAnalysis(id)
    } catch (error) {
      analysis.value = null
      analysisError.value = safeMessage(error)
    }
    try {
      trace.value = await fetchTraceEvidence(id)
    } catch (error) {
      trace.value = null
      traceError.value = safeMessage(error)
    }
  } catch (error) {
    lastError.value = safeMessage(error)
    selectedTicket.value = null
    analysis.value = null
    trace.value = null
  } finally {
    loadingDetail.value = false
  }
}

async function createSyntheticTicket(payload: CreateTicketRequest) {
  creatingTicket.value = true
  lastError.value = null
  try {
    const created = await createTicket(payload)
    selectedTicket.value = created
    selectedId.value = created.id
    const latestTickets = await fetchTickets()
    tickets.value = latestTickets
    metrics.value = await fetchMetrics().catch(() => metrics.value)
    await selectTicket(created.id)
    return created
  } catch (error) {
    lastError.value = safeMessage(error)
    throw error
  } finally {
    creatingTicket.value = false
  }
}

async function runSelectedCopilot() {
  if (!selectedId.value || runningTicketId.value) {
    return null
  }
  const id = selectedId.value
  runningTicketId.value = id
  lastError.value = null
  try {
    selectedTicket.value = await runCopilot(id)
    const latestTickets = await fetchTickets()
    tickets.value = latestTickets
    metrics.value = await fetchMetrics().catch(() => metrics.value)
    await selectTicket(id)
    return selectedTicket.value
  } catch (error) {
    lastError.value = safeMessage(error)
    throw error
  } finally {
    runningTicketId.value = null
  }
}

async function submitReview(decision: ReviewDecision, comment: string) {
  if (!selectedId.value || reviewSubmitting.value) {
    return null
  }
  const payload: ReviewDecisionRequest = { comment }
  reviewSubmitting.value = true
  lastError.value = null
  try {
    if (decision === 'approve') {
      selectedTicket.value = await approveReview(selectedId.value, payload)
    } else if (decision === 'request-changes') {
      selectedTicket.value = await requestReviewChanges(selectedId.value, payload)
    } else {
      selectedTicket.value = await rejectReview(selectedId.value, payload)
    }
    const latestTickets = await fetchTickets()
    tickets.value = latestTickets
    metrics.value = await fetchMetrics().catch(() => metrics.value)
    await selectTicket(selectedId.value)
    return selectedTicket.value
  } catch (error) {
    lastError.value = safeMessage(error)
    throw error
  } finally {
    reviewSubmitting.value = false
  }
}

export function useTicketRealFlow() {
  return {
    tickets: readonly(tickets),
    selectedTicket: readonly(selectedTicket),
    selectedSummary,
    selectedId: readonly(selectedId),
    analysis: readonly(analysis),
    trace: readonly(trace),
    metrics: readonly(metrics),
    backendState: readonly(backendState),
    isBackendConnected,
    loadingTickets: readonly(loadingTickets),
    loadingDetail: readonly(loadingDetail),
    creatingTicket: readonly(creatingTicket),
    runningTicketId: readonly(runningTicketId),
    reviewSubmitting: readonly(reviewSubmitting),
    lastError: readonly(lastError),
    analysisError: readonly(analysisError),
    traceError: readonly(traceError),
    reviewHistory,
    currentRun,
    currentStructuredOutput,
    currentValidatedCitations,
    currentRagReferences,
    pendingReviewCount,
    refresh,
    selectTicket,
    createSyntheticTicket,
    runSelectedCopilot,
    submitReview
  }
}
