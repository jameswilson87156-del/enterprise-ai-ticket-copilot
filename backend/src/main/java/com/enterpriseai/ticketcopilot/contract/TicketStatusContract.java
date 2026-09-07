package com.enterpriseai.ticketcopilot.contract;

import java.util.List;

/**
 * Stable status vocabulary shared by the legacy API and the next API version.
 *
 * <p>The values intentionally remain strings because they are persisted in the
 * existing MySQL schema and are exposed by the current REST DTOs. Keeping the
 * vocabulary in one immutable contract prevents package refactors from
 * silently changing the wire or database representation.</p>
 */
public final class TicketStatusContract {

    public static final String PENDING_CLASSIFICATION = "PENDING_CLASSIFICATION";
    public static final String PENDING_PROCESS = "PENDING_PROCESS";
    public static final String AI_DRAFTED = "AI_DRAFTED";
    public static final String REVIEW_REQUIRED = "REVIEW_REQUIRED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String APPROVED = "APPROVED";
    public static final String RESOLVED = "RESOLVED";
    public static final String REJECTED = "REJECTED";
    public static final String KNOWLEDGE_BASED = "KNOWLEDGE_BASED";

    private static final List<String> ALL_STATUSES = List.of(
        PENDING_CLASSIFICATION,
        PENDING_PROCESS,
        AI_DRAFTED,
        REVIEW_REQUIRED,
        IN_PROGRESS,
        APPROVED,
        RESOLVED,
        REJECTED,
        KNOWLEDGE_BASED
    );

    /**
     * Statuses accepted by the current generic manual status endpoint.
     * PENDING_CLASSIFICATION is an internal intake state and must only be
     * entered by ticket creation, not by an arbitrary client update.
     */
    private static final List<String> MANUAL_TRANSITION_TARGETS = List.of(
        PENDING_PROCESS,
        AI_DRAFTED,
        REVIEW_REQUIRED,
        IN_PROGRESS,
        APPROVED,
        RESOLVED,
        REJECTED,
        KNOWLEDGE_BASED
    );

    private TicketStatusContract() {
    }

    public static List<String> allStatuses() {
        return ALL_STATUSES;
    }

    public static List<String> manualTransitionTargets() {
        return MANUAL_TRANSITION_TARGETS;
    }

    public static boolean isKnown(String status) {
        return status != null && ALL_STATUSES.contains(status);
    }

    public static boolean isSupportedManualTarget(String status) {
        return status != null && MANUAL_TRANSITION_TARGETS.contains(status);
    }
}
