package com.enterpriseai.ticketcopilot.service;

public record AiProviderResult(
    String providerName,
    String modelName,
    boolean fallbackUsed,
    String fallbackReason,
    String actualProvider,
    String actualProtocol,
    String errorCategory,
    long latencyMs,
    String status,
    String errorMessage,
    String promptSummary,
    String responseSummary,
    String content,
    String sourceType
) {
}
