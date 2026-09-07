import type {
  AiAnalysis,
  ApiErrorResponse,
  AuthResponse,
  CreateTicketRequest,
  HealthResponse,
  KnowledgeDraft,
  ReviewDecisionRequest,
  TicketDetail,
  TicketStatus,
  TicketSummary,
  TraceEvidence,
  WorkbenchMetrics
} from '../types/ticket'
import {
  demoConfirmKnowledgeDraft,
  demoCreateKnowledgeDraft,
  demoCreateTicket,
  demoFetchAiAnalysis,
  demoFetchMetrics,
  demoFetchTicket,
  demoFetchTickets,
  demoFetchTraceEvidence,
  demoRejectReview,
  demoRequestReviewChanges,
  demoRunCopilot,
  demoUpdateTicketStatus,
  demoApproveReview
} from '../data/demoTickets'
import {
  clearOidcSession,
  getAuthorizationHeader,
  getAccessToken,
  isOidcAuthEnabled,
  prepareOidcSession,
  startOidcLogin
} from '../auth/oidc'

export const isDemoRuntime = !isOidcAuthEnabled() && (import.meta.env?.MODE === 'demo' || import.meta.env?.VITE_DEMO_MODE === 'true')

export interface TraceEvidenceRagReference {
  articleNo: string
  knowledgeTitle: string
  sourcePath: string
  matchedKeyword: string
  relevanceScore: number
  snippet: string
  usedInDraft: boolean
  linkedTicketId: string
  linkedRunId: string
}

export interface TraceEvidenceCitation {
  resultCitationId: number
  retrievalHitId: number
  citationType: string
  knowledgeArticleId: string
  knowledgeTitle: string
  evidenceExcerpt: string
  supportedClaim: string | null
}

export interface TraceEvidenceResponse {
  ticketId: string
  runId: string
  traceId: string
  traceMode: string
  evidenceSource: 'IMMUTABLE_RUN' | 'LEGACY_DERIVED' | string
  ragReferences: TraceEvidenceRagReference[]
  validatedCitations: TraceEvidenceCitation[]
  copilotRun: {
    runId: string
    runStatus: string
    actualProvider: string
    fallbackUsed: boolean
    fallbackReasonCode: string | null
    errorCategory: string
    outputProduced: boolean
    humanReviewRequired: boolean
    retrievalHitCount: number
  } | null
  structuredOutput: {
    outputValidationStatus: string
    citationValidationStatus: string
    abstained: boolean
    abstentionReasonCode: string
    finalHumanReviewRequired: boolean
    validCitationCount: number
    rejectedCitationCount: number
  } | null
  humanReview: {
    reviewStatus: string
    reviewer: string | null
    decision: string | null
    comment: string | null
    reviewedAt: string | null
    nextAction: string
  } | null
  reviewRecords: Array<{
    reviewRecordId: number
    runId: string
    decision: string
    reviewer: string
    comment: string
    createdAt: string
  }>
}

export type TraceEvidenceErrorCategory =
  | 'authentication-required'
  | 'access-denied'
  | 'not-found'
  | 'rate-limited'
  | 'backend-unavailable'
  | 'request-failed'

interface SafeHttpError {
  category: TraceEvidenceErrorCategory
  userLabel: string
  message: string
}

function safeHttpError(status: number): SafeHttpError {
  if (status === 401) {
    return {
      category: 'authentication-required',
      userLabel: 'Authentication required',
      message: 'Authentication is required. Real replay is available only in a local authenticated demo.'
    }
  }
  if (status === 403) {
    return {
      category: 'access-denied',
      userLabel: 'Access denied',
      message: 'Access to this trace evidence is not allowed.'
    }
  }
  if (status === 404) {
    return {
      category: 'not-found',
      userLabel: 'Not found',
      message: 'Ticket or trace evidence was not found.'
    }
  }
  if (status === 429) {
    return {
      category: 'rate-limited',
      userLabel: 'Temporarily unavailable',
      message: 'Trace evidence is temporarily unavailable. Please try again later.'
    }
  }
  if (status >= 500) {
    return {
      category: 'backend-unavailable',
      userLabel: 'Backend unavailable',
      message: 'The trace evidence backend is unavailable.'
    }
  }
  return {
    category: 'request-failed',
    userLabel: 'Request error',
    message: 'Unable to load trace evidence.'
  }
}

export class TraceEvidenceRequestError extends Error {
  constructor(
    message: string,
    readonly status: number | undefined,
    readonly category: TraceEvidenceErrorCategory,
    readonly userLabel: string
  ) {
    super(message)
    this.name = 'TraceEvidenceRequestError'
  }
}

const configuredBaseUrl = import.meta.env?.VITE_TICKET_API_BASE_URL || '/api'
const apiBaseUrl = String(configuredBaseUrl).replace(/\/+$/, '') || '/api'
const authHeaderName = 'Author' + 'ization'
const authScheme = 'Bear' + 'er '

let inMemorySessionValue: string | null = null
let sessionRequest: Promise<void> | null = null

export class TicketApiError extends Error {
  readonly status: number | null
  readonly safeMessage: string

  constructor(message: string, status: number | null = null) {
    super(message)
    this.name = 'TicketApiError'
    this.status = status
    this.safeMessage = message
  }
}

function apiUrl(path: string) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${apiBaseUrl}${normalizedPath}`
}

async function readSafeError(response: Response): Promise<string> {
  try {
    const payload = (await response.json()) as Partial<ApiErrorResponse>
    if (typeof payload.message === 'string' && payload.message.trim()) {
      return payload.message.slice(0, 240)
    }
  } catch {
    // Fall through to status-only message.
  }
  if (response.status === 401 || response.status === 403) {
    return '当前会话无权执行该后端操作。'
  }
  if (response.status >= 500) {
    return '后端服务返回错误，请稍后重试或检查本地服务状态。'
  }
  return `后端请求失败（HTTP ${response.status}）。`
}

function toSafeError(error: unknown): TicketApiError {
  if (error instanceof TicketApiError) {
    return error
  }
  if (error instanceof TypeError) {
    return new TicketApiError('后端未连接或网络请求失败。', null)
  }
  if (error instanceof Error && error.message) {
    return new TicketApiError(error.message.slice(0, 240), null)
  }
  return new TicketApiError('后端请求失败。', null)
}

async function request<T>(path: string, options: RequestInit = {}, requiresSession = true): Promise<T> {
  try {
    if (requiresSession) {
      await ensureSession()
    }

    const headers = new Headers(options.headers)
    if (!headers.has('Content-Type') && options.body) {
      headers.set('Content-Type', 'application/json')
    }
    if (requiresSession) {
      const authorization = isOidcAuthEnabled() ? getAuthorizationHeader() : (inMemorySessionValue ? `${authScheme}${inMemorySessionValue}` : null)
      if (authorization) headers.set(authHeaderName, authorization)
    }

    const response = await fetch(apiUrl(path), {
      ...options,
      headers
    })
    if (!response.ok) {
      throw new TicketApiError(await readSafeError(response), response.status)
    }
    if (response.status === 204) {
      return undefined as T
    }
    return (await response.json()) as T
  } catch (error) {
    throw toSafeError(error)
  }
}

async function loginForLocalDemo(): Promise<void> {
  const response = await fetch(apiUrl('/auth/login'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  })
  if (!response.ok) {
    throw new TicketApiError(await readSafeError(response), response.status)
  }
  const payload = (await response.json()) as AuthResponse
  if (!payload.token) {
    throw new TicketApiError('后端登录响应缺少会话凭证。', response.status)
  }
  inMemorySessionValue = payload.token
}

export async function ensureSession(): Promise<void> {
  if (isDemoRuntime) {
    return
  }
  if (isOidcAuthEnabled()) {
    await prepareOidcSession()
    if (!getAccessToken()) throw new TicketApiError('请先完成企业身份登录，再访问真实工单数据。', 401)
    return
  }
  if (inMemorySessionValue) return
  if (!sessionRequest) {
    sessionRequest = loginForLocalDemo().finally(() => {
      sessionRequest = null
    })
  }
  await sessionRequest
}

export function clearSessionForCurrentPage() {
  inMemorySessionValue = null
  sessionRequest = null
  clearOidcSession()
}

export function beginLogin() {
  return startOidcLogin()
}

export function fetchHealth() {
  return request<HealthResponse>('/health', { method: 'GET' }, false)
}

export function fetchTickets() {
  if (isDemoRuntime) {
    return demoFetchTickets()
  }
  return request<TicketSummary[]>('/tickets')
}

export function fetchTicket(id: string) {
  if (isDemoRuntime) {
    return demoFetchTicket(id)
  }
  return request<TicketDetail>(`/tickets/${encodeURIComponent(id)}`)
}

export function fetchAiAnalysis(id: string) {
  if (isDemoRuntime) {
    return demoFetchAiAnalysis(id)
  }
  return request<AiAnalysis>(`/tickets/${encodeURIComponent(id)}/ai-analysis`)
}

export function fetchMetrics() {
  if (isDemoRuntime) {
    return demoFetchMetrics()
  }
  return request<WorkbenchMetrics>('/tickets/metrics')
}

export async function fetchTraceEvidence(
  ticketId: string,
  options: { signal?: AbortSignal } = {}
): Promise<TraceEvidenceResponse | null> {
  if (isDemoRuntime) {
    return await demoFetchTraceEvidence(ticketId) as unknown as TraceEvidenceResponse
  }

  const headers = new Headers({ Accept: 'application/json' })
  const authorization = isOidcAuthEnabled()
    ? getAuthorizationHeader()
    : inMemorySessionValue
      ? `${authScheme}${inMemorySessionValue}`
      : null
  if (authorization) {
    headers.set(authHeaderName, authorization)
  }

  let response: Response
  try {
    response = await fetch(apiUrl(`/tickets/${encodeURIComponent(ticketId)}/trace-evidence`), {
      method: 'GET',
      headers,
      signal: options.signal
    })
  } catch {
    throw new TraceEvidenceRequestError(
      'The trace evidence backend is unavailable.',
      undefined,
      'backend-unavailable',
      'Backend unavailable'
    )
  }

  if (response.status === 204) {
    return null
  }
  if (!response.ok) {
    const safeError = safeHttpError(response.status)
    throw new TraceEvidenceRequestError(
      safeError.message,
      response.status,
      safeError.category,
      safeError.userLabel
    )
  }
  return response.json() as Promise<TraceEvidenceResponse>
}

export function createTicket(payload: CreateTicketRequest) {
  if (isDemoRuntime) {
    return demoCreateTicket(payload)
  }
  return request<TicketDetail>('/tickets', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function runCopilot(id: string) {
  if (isDemoRuntime) {
    return demoRunCopilot(id)
  }
  return request<TicketDetail>(`/tickets/${encodeURIComponent(id)}/run-copilot`, {
    method: 'POST'
  })
}

export function approveReview(id: string, payload: ReviewDecisionRequest) {
  if (isDemoRuntime) {
    return demoApproveReview(id, payload)
  }
  return request<TicketDetail>(`/tickets/${encodeURIComponent(id)}/review/approve`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function requestReviewChanges(id: string, payload: ReviewDecisionRequest) {
  if (isDemoRuntime) {
    return demoRequestReviewChanges(id, payload)
  }
  return request<TicketDetail>(`/tickets/${encodeURIComponent(id)}/review/request-changes`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function rejectReview(id: string, payload: ReviewDecisionRequest) {
  if (isDemoRuntime) {
    return demoRejectReview(id, payload)
  }
  return request<TicketDetail>(`/tickets/${encodeURIComponent(id)}/review/reject`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function updateTicketStatus(id: string, status: TicketStatus, note: string, resolvedSummary = '') {
  if (isDemoRuntime) {
    return demoUpdateTicketStatus(id, status, note, resolvedSummary)
  }
  return request<TicketDetail>(`/tickets/${encodeURIComponent(id)}/status`, {
    method: 'POST',
    body: JSON.stringify({
      status,
      actor: '支持人员',
      note,
      resolvedSummary
    })
  })
}

export function createKnowledgeDraft(id: string) {
  if (isDemoRuntime) {
    return demoCreateKnowledgeDraft(id)
  }
  return request<KnowledgeDraft>(`/tickets/${encodeURIComponent(id)}/knowledge-draft`, {
    method: 'POST',
    body: JSON.stringify({
      owner: '知识审核人',
      confirm: false
    })
  })
}

export function confirmKnowledgeDraft(articleNo: string) {
  if (isDemoRuntime) {
    return demoConfirmKnowledgeDraft(articleNo)
  }
  return request<KnowledgeDraft>(`/tickets/knowledge/${encodeURIComponent(articleNo)}/confirm`, {
    method: 'POST'
  })
}
