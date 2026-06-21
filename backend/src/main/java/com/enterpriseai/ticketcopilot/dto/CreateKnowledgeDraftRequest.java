package com.enterpriseai.ticketcopilot.dto;

import jakarta.validation.constraints.Size;

public record CreateKnowledgeDraftRequest(
    @Size(max = 180, message = "title must not exceed 180 characters")
    String title,
    @Size(max = 20000, message = "content must not exceed 20000 characters")
    String content,
    @Size(max = 96, message = "owner must not exceed 96 characters")
    String owner,
    boolean confirm
) {
}
