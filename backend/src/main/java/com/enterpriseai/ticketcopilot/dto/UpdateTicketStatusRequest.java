package com.enterpriseai.ticketcopilot.dto;

public record UpdateTicketStatusRequest(
    String status,
    String actor,
    String note,
    String resolvedSummary
) {
}
