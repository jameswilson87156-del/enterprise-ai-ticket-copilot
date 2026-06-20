package com.enterpriseai.ticketcopilot.model;

public record TimelineEvent(
    String time,
    String state,
    String actor,
    String note
) {
}
