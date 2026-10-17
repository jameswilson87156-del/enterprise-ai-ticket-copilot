package com.enterpriseai.ticketcopilot.model;

public record KnowledgeDraft(
    String articleNo,
    String title,
    String category,
    String status,
    String owner
) {
}
