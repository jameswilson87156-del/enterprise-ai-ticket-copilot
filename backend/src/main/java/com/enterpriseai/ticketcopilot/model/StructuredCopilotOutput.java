package com.enterpriseai.ticketcopilot.model;

import java.util.List;

public record StructuredCopilotOutput(
    String answer,
    List<StructuredCitation> citations,
    RiskLevel riskLevel,
    boolean humanReviewRequired,
    List<String> missingInformation,
    boolean abstained,
    AbstentionReasonCode abstentionReasonCode
) {
}
