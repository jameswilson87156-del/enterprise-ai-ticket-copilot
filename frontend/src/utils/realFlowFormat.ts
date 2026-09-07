import type { TicketStatus } from '../types/ticket'

export const statusLabels: Record<TicketStatus, string> = {
  PENDING_CLASSIFICATION: '待分类',
  PENDING_PROCESS: '待处理',
  IN_PROGRESS: '处理中',
  RESOLVED: '已解决',
  KNOWLEDGE_BASED: '已沉淀',
  AI_DRAFTED: 'AI 已生成',
  REVIEW_REQUIRED: '待人工复核',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  CHANGES_REQUESTED: '要求修改'
}

export function statusLabel(status?: string | null) {
  if (!status) {
    return '未返回'
  }
  return statusLabels[status as TicketStatus] ?? status
}

export function statusTone(status?: string | null): 'success' | 'info' | 'warning' | 'danger' | 'neutral' | 'violet' {
  if (!status) {
    return 'neutral'
  }
  if (['RESOLVED', 'APPROVED', 'KNOWLEDGE_BASED'].includes(status)) {
    return 'success'
  }
  if (['REVIEW_REQUIRED', 'AI_DRAFTED', 'PENDING_PROCESS', 'PENDING_CLASSIFICATION', 'CHANGES_REQUESTED'].includes(status)) {
    return 'warning'
  }
  if (['REJECTED'].includes(status)) {
    return 'danger'
  }
  if (['IN_PROGRESS'].includes(status)) {
    return 'info'
  }
  return 'neutral'
}

export function priorityTone(priority?: string | null): 'danger' | 'warning' | 'info' | 'neutral' {
  if (priority === 'P1') {
    return 'danger'
  }
  if (priority === 'P2') {
    return 'warning'
  }
  if (priority === 'P3') {
    return 'info'
  }
  return 'neutral'
}

export function priorityLabel(priority?: string | null) {
  if (priority === 'P1') {
    return 'P1 · 紧急'
  }
  if (priority === 'P2') {
    return 'P2 · 高'
  }
  if (priority === 'P3') {
    return 'P3 · 中'
  }
  return safeText(priority)
}

export function boolLabel(value?: boolean | null) {
  if (value === true) {
    return 'YES'
  }
  if (value === false) {
    return 'NO'
  }
  return 'NOT_REPORTED'
}

export function safeText(value?: string | number | null, fallback = '未返回') {
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  return String(value)
}

export function displayDateTime(value?: string | null) {
  if (!value) {
    return '未返回'
  }
  return value.replace('T', ' ').slice(0, 19)
}

export function riskTone(value?: string | null) {
  if (value === 'HIGH') {
    return 'danger'
  }
  if (value === 'MEDIUM') {
    return 'warm'
  }
  if (value === 'LOW') {
    return 'mint'
  }
  return 'steel'
}

export function riskBadgeTone(value?: string | null): 'danger' | 'warning' | 'success' | 'neutral' {
  if (value === 'HIGH') {
    return 'danger'
  }
  if (value === 'MEDIUM') {
    return 'warning'
  }
  if (value === 'LOW') {
    return 'success'
  }
  return 'neutral'
}

export function abstentionLabel(code?: string | null) {
  switch (code) {
    case 'NO_RETRIEVAL_EVIDENCE':
      return '未检索到可支撑证据，系统按策略拒答。'
    case 'CITATION_VALIDATION_FAILED':
      return '引用校验未通过，系统保守拒答。'
    case 'PROVIDER_EMPTY_OUTPUT':
      return 'Provider 输出为空，系统未生成建议。'
    case null:
    case undefined:
    case '':
      return '未触发拒答。'
    default:
      return `拒答原因代码：${code}`
  }
}
