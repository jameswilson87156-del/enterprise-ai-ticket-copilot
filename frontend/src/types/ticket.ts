export type TicketStatus =
  | 'PENDING_CLASSIFICATION'
  | 'PENDING_PROCESS'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'KNOWLEDGE_BASED'
  | 'AI_DRAFTED'
  | 'REVIEW_REQUIRED'
  | 'APPROVED'
  | 'REJECTED'
  | 'CHANGES_REQUESTED'

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

export interface KnowledgeDraft {
  articleNo: string
  title: string
  category: string
  status: 'DRAFT' | 'PUBLISHED'
  owner: string
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

export interface StructuredCitation {
  knowledgeArticleId: string
  supportedClaim: string
  reason: string
  evidenceExcerpt: string
}

export interface ValidatedCitationEvidence {
  resultCitationId: number | null
  retrievalHitId: number | null
  citationType: string
  knowledgeArticleId: string
  knowledgeTitle: string
  evidenceExcerpt: string
  supportedClaim: string
}

export interface StructuredOutputEvidence {
  answer: string
  citations: StructuredCitation[]
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | string
  modelHumanReviewRequired?: boolean | null
  finalHumanReviewRequired: boolean
  missingInformation: string[]
  abstained: boolean
  abstentionReasonCode: string | null
  outputValidationStatus: string
  citationValidationStatus: string
  validCitationCount: number
  rejectedCitationCount: number
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
  structuredOutput?: StructuredOutputEvidence | null
  abstained?: boolean
  abstentionReasonCode?: string | null
  riskLevel?: string | null
  modelHumanReviewRequired?: boolean
  finalHumanReviewRequired?: boolean
  citationValidationStatus?: string | null
  validatedCitations?: ValidatedCitationEvidence[]
  missingInformation?: string[]
  outputValidationStatus?: string | null
}

export interface TraceAiAnalysisEvidence {
  analysisId: number | null
  recordId: number | null
  providerName: string | null
  modelName: string | null
  fallbackUsed: boolean
  fallbackReason: string | null
  provider: string | null
  model: string | null
  fallbackStrategy: string | null
  latencyMs: number | null
  status: string | null
  createdAt: string | null
  errorMessage: string | null
  promptSummary: string | null
  responseSummary: string | null
  requestedProvider: string | null
  requestedProtocol: string | null
  actualProvider: string | null
  actualProtocol: string | null
  errorCategory: string | null
  structuredOutput: StructuredOutputEvidence | null
  validatedCitations: ValidatedCitationEvidence[]
}

export interface GenerationRecordEvidence {
  recordId: number | null
  businessType: string | null
  sourceType: string | null
  providerName: string | null
  modelName: string | null
  fallbackUsed: boolean
  fallbackReason: string | null
  provider: string | null
  model: string | null
  fallbackStrategy: string | null
  latencyMs: number | null
  status: string | null
  createdAt: string | null
  errorMessage: string | null
  promptSummary: string | null
  responseSummary: string | null
}

export interface TraceStep {
  stepName: string
  recordId: number | null
  sourceType: string | null
  status: string | null
  latencyMs: number | null
  createdAt: string | null
  summary: string | null
}

export interface StatusHistoryEvidence {
  historyId: number | null
  fromStatus: string | null
  toStatus: string | null
  actor: string | null
  note: string | null
  occurredAt: string | null
}

export interface RagReference {
  articleNo: string
  knowledgeTitle: string
  sourcePath: string | null
  matchedKeyword: string | null
  relevanceScore: number | null
  snippet: string | null
  usedInDraft: boolean
  linkedTicketId: string | null
  linkedRunId: string | null
}

export interface HumanReviewEvidence {
  reviewStatus: string | null
  reviewer: string | null
  decision: string | null
  comment: string | null
  reviewedAt: string | null
  nextAction: string | null
}

export interface CopilotRunEvidence {
  runId: string | null
  traceId: string | null
  requestedProvider: string | null
  requestedProtocol: string | null
  actualProvider: string | null
  actualProtocol: string | null
  runStatus: string | null
  fallbackUsed: boolean
  fallbackReasonCode: string | null
  errorCategory: string | null
  sanitizedErrorSummary: string | null
  startedAt: string | null
  completedAt: string | null
  totalLatencyMs: number | null
  retrievalHitCount: number | null
  outputProduced: boolean
  humanReviewRequired: boolean
  analysisId: number | null
  generationRecordId: number | null
  structuredResultId: number | null
  riskLevel: string | null
  abstained: boolean
  abstentionReasonCode: string | null
  citationValidationStatus: string | null
  outputValidationStatus: string | null
}

export interface ReviewRecordEvidence {
  reviewRecordId: number | null
  runId: string | null
  decision: string | null
  reviewer: string | null
  comment: string | null
  previousStatus: string | null
  newStatus: string | null
  createdAt: string | null
}

export interface TraceEvidence {
  ticketId: string
  runId: string | null
  traceId: string | null
  traceMode: string | null
  currentStep: string | null
  stepTimeline: TraceStep[]
  statusHistory: StatusHistoryEvidence[]
  totalLatency: number
  reviewRequired: boolean
  aiAnalysis: TraceAiAnalysisEvidence | null
  generationRecords: GenerationRecordEvidence[]
  ragReferences: RagReference[]
  humanReview: HumanReviewEvidence | null
  copilotRun: CopilotRunEvidence | null
  reviewRecords: ReviewRecordEvidence[]
  structuredOutput: StructuredOutputEvidence | null
  validatedCitations: ValidatedCitationEvidence[]
  evidenceSource: string | null
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

export interface UpdateTicketStatusRequest {
  status: TicketStatus
  actor?: string
  note?: string
  resolvedSummary?: string
}

export interface ReviewDecisionRequest {
  comment?: string
}

export interface WorkbenchMetrics {
  pendingTickets: number
  aiHitRate: number
  knowledgeCoverage: number
  todayKnowledgeDrafts: number
}

export interface AuthUser {
  username: string
  displayName: string
  role: string
}

export interface AuthResponse {
  token: string
  user: AuthUser
}

export interface ApiErrorResponse {
  code: number
  message: string
  path: string
  timestamp: string
}

export interface HealthResponse {
  status: string
  service: string
}
