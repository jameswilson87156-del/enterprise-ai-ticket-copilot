package com.enterpriseai.ticketcopilot.model;

public record ValidatedCitationEvidence(
    Long resultCitationId,
    Long retrievalHitId,
    String citationType,
    String knowledgeArticleId,
    String knowledgeTitle,
    String evidenceExcerpt,
    String supportedClaim
) {
}
