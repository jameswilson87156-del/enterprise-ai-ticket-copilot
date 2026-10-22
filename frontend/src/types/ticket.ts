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

export interface RetrievalEvaluationArticle {
  articleNo: string
  title: string
  relevance: number
}

export interface RetrievalEvaluationCase {
  id: string
  query: string
  queryType: 'direct' | 'paraphrase' | 'hard_negative' | 'no_match' | string
  expectedArticleNo: string | null
  relevantArticleNos: string[]
  annotationRationale: string
  expectedRank: number
  firstRelevantRank: number
  hitAt1: boolean
  hitAt3: boolean
  correctNoMatch: boolean
  recallAt1: number
  recallAt3: number
  precisionAt3: number
  failureType: string
  relevantRanks: Array<{
    articleNo: string
    rank: number
  }>
  retrieved: RetrievalEvaluationArticle[]
}

export interface RetrievalEvaluationReport {
  generatedAt: string
  datasetVersion: string
  dataSource: string
  corpusVersion: string
  corpusArticleCount: number
  retrievalMode: string
  topK: number
  metrics: {
    total: number
    positiveQueryCount: number
    noMatchQueryCount: number
    hitAt1: number
    hitAt3: number
    hitRateAt1: number
    hitRateAt3: number
    recallAt1: number
    recallAt3: number
    precisionAt3: number
    meanReciprocalRank: number
    noMatchTrueNegativeCount: number
    noMatchFalsePositiveCount: number
    noMatchSpecificity: number
  }
  cases: RetrievalEvaluationCase[]
}
