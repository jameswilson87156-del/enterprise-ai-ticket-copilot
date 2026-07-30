package com.enterpriseai.ticketcopilot.model;

public record StructuredCitation(
    String knowledgeArticleId,
    String supportedClaim,
    String reason,
    String evidenceExcerpt
) {
}
