package com.enterpriseai.ticketcopilot.model;

public record SystemContext(
    String application,
    String environment,
    String region,
    String lastDeployment,
    String affectedUsers
) {
}
