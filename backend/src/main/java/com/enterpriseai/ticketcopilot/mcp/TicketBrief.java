package com.enterpriseai.ticketcopilot.mcp;

/** MCP searchTickets 返回的工单摘要 */
public record TicketBrief(
    String ticketNo,
    String title,
    String status,
    String urgency,
    String category,
    String systemName,
    String requester
) {
}
