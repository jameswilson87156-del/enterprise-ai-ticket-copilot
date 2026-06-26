export type TicketStatus = 'PENDING_CLASSIFICATION' | 'PENDING_PROCESS' | 'IN_PROGRESS' | 'RESOLVED' | 'KNOWLEDGE_BASED'
export type TicketPriority = 'P1' | 'P2' | 'P3'

export interface TicketSummary {
  id: string
  title: string
  requester: string
  team: string
  status: TicketStatus
  priority: TicketPriority
  category: string
  aiConfidence: number
  updatedAt: string
  searchText?: string
}

export interface SystemContext {
  application: string
  environment: string
  region: string
  lastDeployment: string
  affectedUsers: string
}

export interface TimelineEvent {
  time: string
  state: string
  actor: string
  note: string
}

export interface TicketDetail {
  id: string
  title: string
  requester: string
  department: string
  status: TicketStatus
  priority: TicketPriority
  category: string
  description: string
  systemContext: SystemContext
  errorLogs: string[]
  businessContext: string[]
  timeline: TimelineEvent[]
  knowledgeDraft?: KnowledgeDraft | null
}

export interface KnowledgeHit {
  id: string
  title: string
  relevance: number
  owner: string
  lastVerifiedAt: string
}

export interface AiAnalysis {
  ticketId: string
  classification: string
  classificationReason: string
  confidence: number
  confirmationState: string
  knowledgeHits: KnowledgeHit[]
  troubleshootingSteps: string[]
  replySuggestion: string
  riskNotes: string[]
}

export interface AiAnalysisEvidence {
  analysisId: string | number | null
  recordId: string | number | null
  provider: string
  model: string
  fallbackStrategy: string
  latencyMs: number | null
  status: string
  createdAt: string | null
  errorMessage: string | null
  promptSummary: string
  responseSummary: string
}

export interface GenerationRecordEvidence {
  recordId: string | number
  businessType: string
  sourceType: string
  provider: string
  model: string
  fallbackStrategy: string
  latencyMs: number
  status: string
  createdAt: string
  errorMessage: string | null
  promptSummary: string
  responseSummary: string
}

export interface TraceStepEvidence {
  stepName: string
  recordId: string | number | null
  sourceType: string
  status: string
  latencyMs: number | null
  createdAt: string | null
  summary: string
}

export interface StatusHistoryEvidence {
  historyId: string | number | null
  fromStatus: string | null
  toStatus: string
  actor: string
  note: string
  occurredAt: string | null
}

export interface RagReferenceEvidence {
  articleNo: string
  knowledgeTitle: string
  sourcePath: string
  matchedKeyword: string
  relevanceScore: number | null
  snippet: string
  usedInDraft: boolean
  linkedTicketId: string
  linkedRunId: string
}

export interface HumanReviewEvidence {
  reviewStatus: string
  reviewer: string
  decision: string
  comment: string
  reviewedAt: string | null
  nextAction: string
}

export interface TraceEvidence {
  ticketId: string
  runId: string
  traceId: string
  traceMode: string
  currentStep: string
  stepTimeline: TraceStepEvidence[]
  statusHistory: StatusHistoryEvidence[]
  totalLatency: number
  reviewRequired: boolean
  aiAnalysis: AiAnalysisEvidence
  generationRecords: GenerationRecordEvidence[]
  ragReferences: RagReferenceEvidence[]
  humanReview: HumanReviewEvidence
}

export interface KnowledgeDraft {
  articleNo: string
  title: string
  category: string
  status: 'DRAFT' | 'PUBLISHED'
  owner: string
}

export interface CreateTicketRequest {
  title: string
  description: string
  systemName: string
  errorLog: string
  urgency: TicketPriority
  requester?: string
  requesterDepartment?: string
}

export interface WorkbenchMetrics {
  pendingTickets: number
  aiHitRate: number
  knowledgeCoverage: number
  todayKnowledgeDrafts: number
}
