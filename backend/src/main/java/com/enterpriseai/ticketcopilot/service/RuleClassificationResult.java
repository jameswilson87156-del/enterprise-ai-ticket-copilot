package com.enterpriseai.ticketcopilot.service;

public record RuleClassificationResult(
    String category,
    int confidence
) {
}
