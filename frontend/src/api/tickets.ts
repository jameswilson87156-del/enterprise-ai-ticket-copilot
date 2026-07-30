import type { AiAnalysis, CreateTicketRequest, KnowledgeDraft, TicketDetail, TicketStatus, TicketSummary, WorkbenchMetrics } from '../types/ticket'
import {
  demoConfirmKnowledgeDraft,
  demoCreateKnowledgeDraft,
  demoCreateTicket,
  demoFetchAiAnalysis,
  demoFetchMetrics,
  demoFetchTicket,
  demoFetchTickets,
  demoUpdateTicketStatus
} from '../data/demoTickets'

export const isDemoRuntime = import.meta.env?.MODE === 'demo' || import.meta.env?.VITE_DEMO_MODE === 'true'

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

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {})
    },
    ...options
  })
  if (!response.ok) {
    throw new Error(`Request failed with HTTP status ${response.status}.`)
  }
  return response.json() as Promise<T>
}

export function fetchTickets() {
  if (isDemoRuntime) {
    return demoFetchTickets()
  }
  return request<TicketSummary[]>('/api/tickets')
}

export function fetchTicket(id: string) {
  if (isDemoRuntime) {
    return demoFetchTicket(id)
  }
  return request<TicketDetail>(`/api/tickets/${encodeURIComponent(id)}`)
}

export function fetchAiAnalysis(id: string) {
  if (isDemoRuntime) {
    return demoFetchAiAnalysis(id)
  }
  return request<AiAnalysis>(`/api/tickets/${encodeURIComponent(id)}/ai-analysis`)
}

export function fetchMetrics() {
  if (isDemoRuntime) {
    return demoFetchMetrics()
  }
  return request<WorkbenchMetrics>('/api/tickets/metrics')
}

export async function fetchTraceEvidence(
  ticketId: string,
  options: { signal?: AbortSignal } = {}
): Promise<TraceEvidenceResponse | null> {
  let response: Response
  try {
    response = await fetch(`/api/tickets/${encodeURIComponent(ticketId)}/trace-evidence`, {
      method: 'GET',
      headers: {
        Accept: 'application/json'
      },
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
  return request<TicketDetail>('/api/tickets', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function updateTicketStatus(id: string, status: TicketStatus, note: string, resolvedSummary = '') {
  if (isDemoRuntime) {
    return demoUpdateTicketStatus(id, status, note, resolvedSummary)
  }
  return request<TicketDetail>(`/api/tickets/${encodeURIComponent(id)}/status`, {
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
  return request<KnowledgeDraft>(`/api/tickets/${encodeURIComponent(id)}/knowledge-draft`, {
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
  return request<KnowledgeDraft>(`/api/tickets/knowledge/${encodeURIComponent(articleNo)}/confirm`, {
    method: 'POST'
  })
}
