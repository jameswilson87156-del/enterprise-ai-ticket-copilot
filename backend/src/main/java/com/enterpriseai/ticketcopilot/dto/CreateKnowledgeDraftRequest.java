package com.enterpriseai.ticketcopilot.dto;

public record CreateKnowledgeDraftRequest(
    String title,
    String content,
    String owner,
    boolean confirm
) {
}
