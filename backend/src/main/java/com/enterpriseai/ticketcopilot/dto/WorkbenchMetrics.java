package com.enterpriseai.ticketcopilot.dto;

public record WorkbenchMetrics(
    long pendingTickets,
    int aiHitRate,
    int knowledgeCoverage,
    long todayKnowledgeDrafts
) {
}
