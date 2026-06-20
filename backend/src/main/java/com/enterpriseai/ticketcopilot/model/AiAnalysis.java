package com.enterpriseai.ticketcopilot.model;

import java.util.List;

public record AiAnalysis(
    String ticketId,
    String classification,
    String classificationReason,
    int confidence,
    String confirmationState,
    List<KnowledgeHit> knowledgeHits,
    List<String> troubleshootingSteps,
    String replySuggestion,
    List<String> riskNotes
) {
}
