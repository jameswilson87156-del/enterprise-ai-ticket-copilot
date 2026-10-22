package com.enterpriseai.ticketcopilot.service;

public record RuleClassificationResult(
    String category,
    int confidence,
    String reason,
    String source
) {

    public static final String SOURCE_RULE = "RULE";
    public static final String SOURCE_LLM = "LLM";
    public static final String SOURCE_RULE_FALLBACK = "RULE_FALLBACK";

    public RuleClassificationResult(String category, int confidence) {
        this(category, confidence, "", SOURCE_RULE);
    }
}
