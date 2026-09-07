package com.enterpriseai.ticketcopilot.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketStatusContractTest {

    @Test
    void exposesTheStableStatusVocabularyInWorkflowOrder() {
        assertThat(TicketStatusContract.allStatuses()).containsExactly(
            "PENDING_CLASSIFICATION",
            "PENDING_PROCESS",
            "AI_DRAFTED",
            "REVIEW_REQUIRED",
            "IN_PROGRESS",
            "APPROVED",
            "RESOLVED",
            "REJECTED",
            "KNOWLEDGE_BASED"
        );
    }

    @Test
    void keepsInternalIntakeStateOutOfGenericManualUpdates() {
        assertThat(TicketStatusContract.manualTransitionTargets()).containsExactly(
            "PENDING_PROCESS",
            "AI_DRAFTED",
            "REVIEW_REQUIRED",
            "IN_PROGRESS",
            "APPROVED",
            "RESOLVED",
            "REJECTED",
            "KNOWLEDGE_BASED"
        );
        assertThat(TicketStatusContract.isSupportedManualTarget("PENDING_CLASSIFICATION")).isFalse();
    }

    @Test
    void rejectsUnknownNullAndCaseChangedValues() {
        assertThat(TicketStatusContract.isKnown("CLOSED")).isFalse();
        assertThat(TicketStatusContract.isKnown(null)).isFalse();
        assertThat(TicketStatusContract.isKnown("resolved")).isFalse();
        assertThat(TicketStatusContract.isSupportedManualTarget("AUTO_CLOSED")).isFalse();
    }

    @Test
    void acceptsEveryPublicManualTransitionTarget() {
        assertThat(TicketStatusContract.manualTransitionTargets())
            .allMatch(TicketStatusContract::isKnown)
            .allMatch(TicketStatusContract::isSupportedManualTarget);
    }
}
