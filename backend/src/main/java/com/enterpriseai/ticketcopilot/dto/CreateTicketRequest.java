package com.enterpriseai.ticketcopilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
    @NotBlank(message = "title is required")
    @Size(max = 180, message = "title must not exceed 180 characters")
    String title,
    @NotBlank(message = "description is required")
    String description,
    @Size(max = 96, message = "systemName must not exceed 96 characters")
    String systemName,
    @Size(max = 20000, message = "errorLog must not exceed 20000 characters")
    String errorLog,
    @Pattern(regexp = "P1|P2|P3", message = "urgency must be P1, P2 or P3")
    String urgency,
    @Size(max = 64, message = "requester must not exceed 64 characters")
    String requester,
    @Size(max = 96, message = "requesterDepartment must not exceed 96 characters")
    String requesterDepartment
) {
}
