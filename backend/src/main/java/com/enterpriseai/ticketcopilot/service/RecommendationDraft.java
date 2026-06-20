package com.enterpriseai.ticketcopilot.service;

import java.util.List;

public record RecommendationDraft(
    List<String> troubleshootingSteps,
    String replySuggestion,
    List<String> riskNotes
) {
}
