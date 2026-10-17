package com.enterpriseai.ticketcopilot.model;

import java.util.List;

public record TicketDetail(
    String id,
    String title,
    String requester,
    String department,
    String status,
    String priority,
    String category,
    String description,
    SystemContext systemContext,
    List<String> errorLogs,
    List<String> businessContext,
    List<TimelineEvent> timeline,
    KnowledgeDraft knowledgeDraft
) {
}
