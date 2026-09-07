package com.enterpriseai.ticketcopilot.ticket.application.policy;

import java.util.List;
import java.util.stream.Stream;

import com.enterpriseai.ticketcopilot.contract.TicketStatusContract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketStatusTransitionPolicyTest {

    private final TicketStatusTransitionPolicy policy = new TicketStatusTransitionPolicy();

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("legalManualTransitions")
    void allowsOnlyDocumentedManualTransitions(String from, String to) {
        policy.assertAllowed(context(
            from,
            to,
            TicketStatusTransitionPolicy.Operation.MANUAL_STATUS_UPDATE,
            false,
            false,
            false
        ));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("illegalManualTransitions")
    void rejectsEveryOtherKnownManualTransition(String from, String to) {
        assertThatThrownBy(() -> policy.assertAllowed(context(
            from,
            to,
            TicketStatusTransitionPolicy.Operation.MANUAL_STATUS_UPDATE,
            false,
            false,
            false
        ))).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT)
        );
    }

    @ParameterizedTest(name = "review {0} from {1}")
    @MethodSource("legalReviewTransitions")
    void allowsReviewOnlyForACurrentUnreviewedCopilotRun(String operationName, String from, String to) {
        policy.assertAllowed(context(
            from,
            to,
            TicketStatusTransitionPolicy.Operation.valueOf(operationName),
            true,
            false,
            false
        ));
    }

    @Test
    void rejectsReviewWithoutRunOrAfterAReviewDecision() {
        assertConflict(context(
            TicketStatusContract.PENDING_PROCESS,
            TicketStatusContract.RESOLVED,
            TicketStatusTransitionPolicy.Operation.APPROVE_REVIEW,
            false,
            false,
            false
        ));
        assertConflict(context(
            TicketStatusContract.REVIEW_REQUIRED,
            TicketStatusContract.RESOLVED,
            TicketStatusTransitionPolicy.Operation.APPROVE_REVIEW,
            true,
            true,
            true
        ));
        assertConflict(context(
            TicketStatusContract.REVIEW_REQUIRED,
            TicketStatusContract.REVIEW_REQUIRED,
            TicketStatusTransitionPolicy.Operation.REQUEST_REVIEW_CHANGES,
            true,
            false,
            true
        ));
        assertConflict(context(
            TicketStatusContract.REJECTED,
            TicketStatusContract.RESOLVED,
            TicketStatusTransitionPolicy.Operation.APPROVE_REVIEW,
            true,
            false,
            false
        ));
    }

    @Test
    void allowsInitialAndRerunCopilotEdgesButNotTerminalOrOrphanedReruns() {
        policy.assertAllowed(context(
            TicketStatusContract.PENDING_PROCESS,
            TicketStatusContract.REVIEW_REQUIRED,
            TicketStatusTransitionPolicy.Operation.RUN_COPILOT,
            true,
            false,
            false
        ));
        policy.assertAllowed(context(
            TicketStatusContract.REVIEW_REQUIRED,
            TicketStatusContract.REVIEW_REQUIRED,
            TicketStatusTransitionPolicy.Operation.RUN_COPILOT,
            true,
            false,
            true
        ));
        assertConflict(context(
            TicketStatusContract.REVIEW_REQUIRED,
            TicketStatusContract.REVIEW_REQUIRED,
            TicketStatusTransitionPolicy.Operation.RUN_COPILOT,
            false,
            false,
            false
        ));
        assertConflict(context(
            TicketStatusContract.KNOWLEDGE_BASED,
            TicketStatusContract.REVIEW_REQUIRED,
            TicketStatusTransitionPolicy.Operation.RUN_COPILOT,
            true,
            false,
            false
        ));
    }

    @Test
    void keepsKnowledgeDraftAndPublicationEdgesNarrow() {
        policy.assertAllowed(context(
            TicketStatusContract.RESOLVED,
            TicketStatusContract.RESOLVED,
            TicketStatusTransitionPolicy.Operation.CREATE_KNOWLEDGE_DRAFT,
            false,
            false,
            false
        ));
        policy.assertAllowed(context(
            TicketStatusContract.KNOWLEDGE_BASED,
            TicketStatusContract.KNOWLEDGE_BASED,
            TicketStatusTransitionPolicy.Operation.CREATE_KNOWLEDGE_DRAFT,
            false,
            false,
            false
        ));
        policy.assertAllowed(context(
            TicketStatusContract.RESOLVED,
            TicketStatusContract.KNOWLEDGE_BASED,
            TicketStatusTransitionPolicy.Operation.CONFIRM_KNOWLEDGE_DRAFT,
            false,
            false,
            false
        ));
        assertConflict(context(
            TicketStatusContract.IN_PROGRESS,
            TicketStatusContract.KNOWLEDGE_BASED,
            TicketStatusTransitionPolicy.Operation.PUBLISH_KNOWLEDGE_DRAFT,
            false,
            false,
            false
        ));
        assertThat(policy.isIdempotentKnowledgeConfirmation("PUBLISHED")).isTrue();
        assertThat(policy.isIdempotentKnowledgeConfirmation("DRAFT")).isFalse();
    }

    private void assertConflict(TicketStatusTransitionPolicy.TransitionContext context) {
        assertThatThrownBy(() -> policy.assertAllowed(context))
            .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT)
            );
    }

    private TicketStatusTransitionPolicy.TransitionContext context(
        String from,
        String to,
        TicketStatusTransitionPolicy.Operation operation,
        boolean hasRun,
        boolean reviewCompleted,
        boolean reviewDecisionRecorded
    ) {
        return new TicketStatusTransitionPolicy.TransitionContext(
            from,
            to,
            operation,
            hasRun,
            reviewCompleted,
            reviewDecisionRecorded
        );
    }

    private static Stream<Arguments> legalManualTransitions() {
        return Stream.of(
            Arguments.of(TicketStatusContract.PENDING_PROCESS, TicketStatusContract.IN_PROGRESS),
            Arguments.of(TicketStatusContract.PENDING_PROCESS, TicketStatusContract.RESOLVED),
            Arguments.of(TicketStatusContract.IN_PROGRESS, TicketStatusContract.RESOLVED),
            Arguments.of(TicketStatusContract.RESOLVED, TicketStatusContract.IN_PROGRESS)
        );
    }

    private static Stream<Arguments> illegalManualTransitions() {
        List<String> statuses = TicketStatusContract.allStatuses();
        return statuses.stream()
            .flatMap(from -> statuses.stream()
                .filter(to -> !isLegalManual(from, to))
                .map(to -> Arguments.of(from, to)));
    }

    private static boolean isLegalManual(String from, String to) {
        return (TicketStatusContract.PENDING_PROCESS.equals(from)
                && (TicketStatusContract.IN_PROGRESS.equals(to) || TicketStatusContract.RESOLVED.equals(to)))
            || (TicketStatusContract.IN_PROGRESS.equals(from) && TicketStatusContract.RESOLVED.equals(to))
            || (TicketStatusContract.RESOLVED.equals(from) && TicketStatusContract.IN_PROGRESS.equals(to));
    }

    private static Stream<Arguments> legalReviewTransitions() {
        return Stream.of(
            Arguments.of("APPROVE_REVIEW", TicketStatusContract.AI_DRAFTED, TicketStatusContract.RESOLVED),
            Arguments.of("APPROVE_REVIEW", TicketStatusContract.REVIEW_REQUIRED, TicketStatusContract.RESOLVED),
            Arguments.of("REQUEST_REVIEW_CHANGES", TicketStatusContract.AI_DRAFTED, TicketStatusContract.REVIEW_REQUIRED),
            Arguments.of("REQUEST_REVIEW_CHANGES", TicketStatusContract.REVIEW_REQUIRED, TicketStatusContract.REVIEW_REQUIRED),
            Arguments.of("REJECT_REVIEW", TicketStatusContract.AI_DRAFTED, TicketStatusContract.REJECTED),
            Arguments.of("REJECT_REVIEW", TicketStatusContract.REVIEW_REQUIRED, TicketStatusContract.REJECTED)
        );
    }
}
