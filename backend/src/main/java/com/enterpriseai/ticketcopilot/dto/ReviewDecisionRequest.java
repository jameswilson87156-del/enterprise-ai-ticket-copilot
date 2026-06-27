package com.enterpriseai.ticketcopilot.dto;

import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(
    @Size(max = 1000, message = "comment must not exceed 1000 characters")
    String comment
) {
}
