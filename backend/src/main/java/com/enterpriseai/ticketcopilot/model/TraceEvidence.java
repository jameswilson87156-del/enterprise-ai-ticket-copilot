package com.enterpriseai.ticketcopilot.model;

import java.time.LocalDateTime;
import java.util.List;

public record TraceEvidence(
    String ticketId,
    String runId,
    String traceId,
    String traceMode,
    String currentStep,
    List<TraceStep> stepTimeline,
    List<StatusHistoryEvidence> statusHistory,
    long totalLatency,
    boolean reviewRequired,
    AiAnalysisEvidence aiAnalysis,
    List<GenerationRecordEvidence> generationRecords,
    List<RagReference> ragReferences,
    HumanReviewEvidence humanReview
) {

    public record AiAnalysisEvidence(
        Long analysisId,
        Long recordId,
        String providerName,
        String modelName,
        boolean fallbackUsed,
        String fallbackReason,
        String provider,
        String model,
        String fallbackStrategy,
        Long latencyMs,
        String status,
        LocalDateTime createdAt,
        String errorMessage,
        String promptSummary,
        String responseSummary
    ) {
    }

    public record GenerationRecordEvidence(
        Long recordId,
        String businessType,
        String sourceType,
        String providerName,
        String modelName,
        boolean fallbackUsed,
        String fallbackReason,
        String provider,
        String model,
        String fallbackStrategy,
        Long latencyMs,
        String status,
        LocalDateTime createdAt,
        String errorMessage,
        String promptSummary,
        String responseSummary
    ) {
    }

    public record TraceStep(
        String stepName,
        Long recordId,
        String sourceType,
        String status,
        Long latencyMs,
        LocalDateTime createdAt,
        String summary
    ) {
    }

    public record StatusHistoryEvidence(
        Long historyId,
        String fromStatus,
        String toStatus,
        String actor,
        String note,
        LocalDateTime occurredAt
    ) {
    }

    public record RagReference(
        String articleNo,
        String knowledgeTitle,
        String sourcePath,
        String matchedKeyword,
        Integer relevanceScore,
        String snippet,
        boolean usedInDraft,
        String linkedTicketId,
        String linkedRunId
    ) {
    }

    public record HumanReviewEvidence(
        String reviewStatus,
        String reviewer,
        String decision,
        String comment,
        LocalDateTime reviewedAt,
        String nextAction
    ) {
    }
}
