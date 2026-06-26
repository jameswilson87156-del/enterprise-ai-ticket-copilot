import type { AiAnalysis, CreateTicketRequest, KnowledgeDraft, TicketDetail, TicketStatus, TicketSummary, TraceEvidence, WorkbenchMetrics } from '../types/ticket'
import {
  demoConfirmKnowledgeDraft,
  demoCreateKnowledgeDraft,
  demoCreateTicket,
  demoFetchAiAnalysis,
  demoFetchMetrics,
  demoFetchTicket,
  demoFetchTickets,
  demoFetchTraceEvidence,
  demoUpdateTicketStatus
} from '../data/demoTickets'

export const isDemoRuntime = import.meta.env.MODE === 'demo' || import.meta.env.VITE_DEMO_MODE === 'true'

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {})
    },
    ...options
  })
  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `Request failed: ${response.status}`)
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

export function fetchTraceEvidence(id: string) {
  if (isDemoRuntime) {
    return demoFetchTraceEvidence(id)
  }
  return request<TraceEvidence>(`/api/tickets/${encodeURIComponent(id)}/trace-evidence`)
}

export function fetchMetrics() {
  if (isDemoRuntime) {
    return demoFetchMetrics()
  }
  return request<WorkbenchMetrics>('/api/tickets/metrics')
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
