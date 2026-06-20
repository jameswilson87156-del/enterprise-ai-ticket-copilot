package com.enterpriseai.ticketcopilot.model;

public record KnowledgeHit(
    String id,
    String title,
    int relevance,
    String owner,
    String lastVerifiedAt
) {
}
