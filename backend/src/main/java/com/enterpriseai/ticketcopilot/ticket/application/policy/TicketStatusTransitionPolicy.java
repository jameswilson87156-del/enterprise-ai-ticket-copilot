package com.enterpriseai.ticketcopilot.ticket.application.policy;

import java.util.Set;

import com.enterpriseai.ticketcopilot.contract.TicketStatusContract;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Central state-transition policy for the ticket workflow.
 *
 * <p>The existing API exposes status values as strings, so this policy keeps
 * the string contract while making the operation, current state and runtime
 * evidence explicit. It intentionally contains only transitions supported by
 * the current source, architecture notes and Showcase tests. An unknown edge
 * is rejected until the product meaning is confirmed.</p>
 */
@Component
public final class TicketStatusTransitionPolicy {

    public enum Operation {
        INITIAL_CLASSIFICATION,
        RUN_COPILOT,
        MANUAL_STATUS_UPDATE,
        APPROVE_REVIEW,
        REQUEST_REVIEW_CHANGES,
        REJECT_REVIEW,
        CREATE_KNOWLEDGE_DRAFT,
        CONFIRM_KNOWLEDGE_DRAFT,
        PUBLISH_KNOWLEDGE_DRAFT
    }

    /**
     * Runtime context used for every state-changing workflow operation.
     * currentRunHasReviewDecision distinguishes a new Copilot run from a run
     * that already received a review action; reviewCompleted captures final
     * review decisions such as approve/reject.
     */
    public record TransitionContext(
        String currentStatus,
        String targetStatus,
        Operation operation,
        boolean hasCurrentCopilotRun,
        boolean reviewCompleted,
        boolean currentRunHasReviewDecision
    ) {
    }

    private static final Set<String> TERMINAL_STATUSES = Set.of(
        TicketStatusContract.APPROVED,
        TicketStatusContract.REJECTED,
        TicketStatusContract.KNOWLEDGE_BASED
    );

    public void assertAllowed(TransitionContext context) {
        if (context == null || context.operation() == null) {
            throw conflict(context, "operation context is required");
        }
        if (!TicketStatusContract.isKnown(context.currentStatus())) {
            throw conflict(context, "current status is unknown");
        }
        if (!TicketStatusContract.isKnown(context.targetStatus())) {
            throw conflict(context, "target status is unknown");
        }

        switch (context.operation()) {
            case INITIAL_CLASSIFICATION -> assertInitialClassification(context);
            case RUN_COPILOT -> assertCopilotRun(context);
            case MANUAL_STATUS_UPDATE -> assertManualStatusUpdate(context);
            case APPROVE_REVIEW, REQUEST_REVIEW_CHANGES, REJECT_REVIEW -> assertReviewDecision(context);
            case CREATE_KNOWLEDGE_DRAFT -> assertKnowledgeDraftCreation(context);
            case CONFIRM_KNOWLEDGE_DRAFT, PUBLISH_KNOWLEDGE_DRAFT -> assertKnowledgePublication(context);
        }
    }

    public boolean isTerminal(String status) {
        return TERMINAL_STATUSES.contains(status);
    }

    /**
     * A published draft confirmation has no state mutation. The existing API
     * already treats that request as idempotent, so the service may return the
     * persisted resource without appending history or changing the ticket.
     */
    public boolean isIdempotentKnowledgeConfirmation(String draftStatus) {
        return "PUBLISHED".equals(draftStatus);
    }

    private void assertInitialClassification(TransitionContext context) {
        if (context.currentStatus().equals(TicketStatusContract.PENDING_CLASSIFICATION)
            && context.targetStatus().equals(TicketStatusContract.PENDING_PROCESS)
            && !context.hasCurrentCopilotRun()
            && !context.reviewCompleted()
            && !context.currentRunHasReviewDecision()) {
            return;
        }
        reject(context, "only ticket intake may classify PENDING_CLASSIFICATION into PENDING_PROCESS");
    }

    private void assertCopilotRun(TransitionContext context) {
        if (isTerminal(context.currentStatus())) {
            reject(context, "terminal tickets cannot run Copilot");
        }
        if (context.reviewCompleted()) {
            reject(context, "the current Copilot Run has already completed review");
        }

        boolean firstRun = TicketStatusContract.PENDING_PROCESS.equals(context.currentStatus());
        boolean rerun = TicketStatusContract.AI_DRAFTED.equals(context.currentStatus())
            || TicketStatusContract.REVIEW_REQUIRED.equals(context.currentStatus());
        if (!firstRun && !rerun) {
            reject(context, "Copilot is only available from PENDING_PROCESS, AI_DRAFTED or REVIEW_REQUIRED");
        }
        if (rerun && !context.hasCurrentCopilotRun()) {
            reject(context, "a rerun requires a persisted current Copilot Run");
        }

        // A provider failure can create a run without changing the ticket
        // status. Same-state execution is therefore an explicit no-op edge.
        if (context.currentStatus().equals(context.targetStatus())) {
            return;
        }
        boolean validTarget = TicketStatusContract.AI_DRAFTED.equals(context.targetStatus())
            || TicketStatusContract.REVIEW_REQUIRED.equals(context.targetStatus());
        if (!validTarget) {
            reject(context, "Copilot may only produce AI_DRAFTED or REVIEW_REQUIRED");
        }
    }

    private void assertManualStatusUpdate(TransitionContext context) {
        if (isTerminal(context.currentStatus())) {
            reject(context, "terminal tickets cannot be manually moved");
        }
        if (context.hasCurrentCopilotRun() && !context.reviewCompleted()) {
            reject(context, "a current Copilot Run must be reviewed before a manual status update");
        }
        boolean allowed = (TicketStatusContract.PENDING_PROCESS.equals(context.currentStatus())
                && (TicketStatusContract.IN_PROGRESS.equals(context.targetStatus())
                    || TicketStatusContract.RESOLVED.equals(context.targetStatus())))
            || (TicketStatusContract.IN_PROGRESS.equals(context.currentStatus())
                && TicketStatusContract.RESOLVED.equals(context.targetStatus()))
            || (TicketStatusContract.RESOLVED.equals(context.currentStatus())
                && TicketStatusContract.IN_PROGRESS.equals(context.targetStatus()));
        if (!allowed) {
            reject(context, "manual status update is not an approved current-to-target transition");
        }
    }

    private void assertReviewDecision(TransitionContext context) {
        if (isTerminal(context.currentStatus())) {
            reject(context, "terminal tickets cannot receive another review decision");
        }
        if (!context.hasCurrentCopilotRun()) {
            reject(context, "a current Copilot Run is required before review");
        }
        if (context.reviewCompleted() || context.currentRunHasReviewDecision()) {
            reject(context, "the current Copilot Run already has a review decision");
        }

        boolean reviewableSource = TicketStatusContract.AI_DRAFTED.equals(context.currentStatus())
            || TicketStatusContract.REVIEW_REQUIRED.equals(context.currentStatus());
        if (!reviewableSource) {
            reject(context, "only AI_DRAFTED or REVIEW_REQUIRED tickets can receive review decisions");
        }

        String expectedTarget = switch (context.operation()) {
            case APPROVE_REVIEW -> TicketStatusContract.RESOLVED;
            case REQUEST_REVIEW_CHANGES -> TicketStatusContract.REVIEW_REQUIRED;
            case REJECT_REVIEW -> TicketStatusContract.REJECTED;
            default -> null;
        };
        if (expectedTarget == null || !expectedTarget.equals(context.targetStatus())) {
            reject(context, "review operation has an invalid target status");
        }
    }

    private void assertKnowledgeDraftCreation(TransitionContext context) {
        if (!context.currentStatus().equals(context.targetStatus())) {
            reject(context, "draft creation must not change the ticket status");
        }
        boolean sourceAllowed = TicketStatusContract.RESOLVED.equals(context.currentStatus())
            || TicketStatusContract.KNOWLEDGE_BASED.equals(context.currentStatus());
        if (!sourceAllowed) {
            reject(context, "knowledge drafts may only be created from RESOLVED or KNOWLEDGE_BASED tickets");
        }
        if (TicketStatusContract.RESOLVED.equals(context.currentStatus())
            && context.hasCurrentCopilotRun()
            && !context.reviewCompleted()) {
            reject(context, "a Copilot-resolved ticket requires a completed review before knowledge drafting");
        }
    }

    private void assertKnowledgePublication(TransitionContext context) {
        if (!TicketStatusContract.RESOLVED.equals(context.currentStatus())
            || !TicketStatusContract.KNOWLEDGE_BASED.equals(context.targetStatus())) {
            reject(context, "knowledge publication requires RESOLVED to KNOWLEDGE_BASED");
        }
        if (context.hasCurrentCopilotRun() && !context.reviewCompleted()) {
            reject(context, "a Copilot-resolved ticket requires a completed review before knowledge publication");
        }
    }

    private void reject(TransitionContext context, String reason) {
        throw conflict(context, reason);
    }

    private ResponseStatusException conflict(TransitionContext context, String reason) {
        String operation = context == null || context.operation() == null ? "UNKNOWN" : context.operation().name();
        String current = context == null ? "null" : context.currentStatus();
        String target = context == null ? "null" : context.targetStatus();
        return new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Invalid ticket workflow transition: operation=" + operation
                + ", from=" + current
                + ", to=" + target
                + "; " + reason
        );
    }
}
