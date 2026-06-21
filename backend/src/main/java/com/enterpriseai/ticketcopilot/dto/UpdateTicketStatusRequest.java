package com.enterpriseai.ticketcopilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTicketStatusRequest(
    @NotBlank(message = "status is required")
    String status,
    @Size(max = 64, message = "actor must not exceed 64 characters")
    String actor,
    @Size(max = 1000, message = "note must not exceed 1000 characters")
    String note,
    @Size(max = 10000, message = "resolvedSummary must not exceed 10000 characters")
    String resolvedSummary
) {
}
