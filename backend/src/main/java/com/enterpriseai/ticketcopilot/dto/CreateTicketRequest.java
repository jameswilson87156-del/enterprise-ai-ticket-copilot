package com.enterpriseai.ticketcopilot.dto;

public record CreateTicketRequest(
    String title,
    String description,
    String systemName,
    String errorLog,
    String urgency,
    String requester,
    String requesterDepartment
) {
}
