package com.enterpriseai.ticketcopilot.model;

public record TicketSummary(
    String id,
    String title,
    String requester,
    String team,
    String status,
    String priority,
    String category,
    int aiConfidence,
    String updatedAt
) {
}
