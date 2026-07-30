package com.enterpriseai.ticketcopilot.model;

import java.util.List;

public record StructuredOutputEvidence(
    String answer,
    List<StructuredCitation> citations,
    String riskLevel,
    Boolean modelHumanReviewRequired,
    boolean finalHumanReviewRequired,
    List<String> missingInformation,
    boolean abstained,
    String abstentionReasonCode,
    String outputValidationStatus,
    String citationValidationStatus,
    int validCitationCount,
    int rejectedCitationCount
) {
}
