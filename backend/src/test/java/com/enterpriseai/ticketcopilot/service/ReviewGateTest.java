package com.enterpriseai.ticketcopilot.service;

import java.util.List;

import com.enterpriseai.ticketcopilot.model.AbstentionReasonCode;
import com.enterpriseai.ticketcopilot.model.CitationRejectionReasonCode;
import com.enterpriseai.ticketcopilot.model.CitationValidationStatus;
import com.enterpriseai.ticketcopilot.model.OutputValidationStatus;
import com.enterpriseai.ticketcopilot.model.RiskLevel;
import com.enterpriseai.ticketcopilot.model.StructuredCitation;
import com.enterpriseai.ticketcopilot.model.StructuredCopilotOutput;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewGateTest {

    private final ReviewGate gate = new ReviewGate();

    @Test
    void highRiskForcesHumanReviewWhenModelSaysFalse() {
        assertThat(gate.finalHumanReviewRequired(
            output(RiskLevel.HIGH, false, false),
            false,
            CitationValidationResult.valid(List.of(), 0),
            OutputValidationStatus.VALID,
            false
        )).isTrue();
    }

    @Test
    void fallbackForcesHumanReview() {
        assertThat(gate.finalHumanReviewRequired(
            output(RiskLevel.LOW, false, false),
            true,
            CitationValidationResult.valid(List.of(), 0),
            OutputValidationStatus.VALID,
            false
        )).isTrue();
    }

    @Test
    void citationValidationFailureForcesHumanReview() {
        assertThat(gate.finalHumanReviewRequired(
            output(RiskLevel.LOW, false, false),
            false,
            CitationValidationResult.invalid(CitationValidationStatus.INVALID_CITATION, 1, CitationRejectionReasonCode.CITATION_NOT_IN_RUN),
            OutputValidationStatus.VALID,
            false
        )).isTrue();
    }

    @Test
    void normalLowRiskWithValidCitationCanAvoidExtraReviewWhenBusinessRuleAllows() {
        assertThat(gate.finalHumanReviewRequired(
            output(RiskLevel.LOW, false, false),
            false,
            CitationValidationResult.valid(List.of(), 0),
            OutputValidationStatus.VALID,
            false
        )).isFalse();
    }

    @Test
    void missingInformationForcesHumanReviewWhenModelSaysFalse() {
        StructuredCopilotOutput output = new StructuredCopilotOutput(
            "answer",
            List.of(new StructuredCitation("KB-OPS-003", null, null, null)),
            RiskLevel.LOW,
            false,
            List.of("missing deployment window"),
            false,
            AbstentionReasonCode.NONE
        );

        assertThat(gate.finalHumanReviewRequired(
            output,
            false,
            CitationValidationResult.valid(List.of(), 0),
            OutputValidationStatus.VALID,
            false
        )).isTrue();
    }

    @Test
    void abstentionRequiresHumanReview() {
        assertThat(gate.finalHumanReviewRequired(
            output(RiskLevel.MEDIUM, true, true),
            false,
            CitationValidationResult.notApplicable(),
            OutputValidationStatus.VALID,
            false
        )).isTrue();
    }

    private StructuredCopilotOutput output(RiskLevel riskLevel, boolean reviewRequired, boolean abstained) {
        return new StructuredCopilotOutput(
            "answer",
            abstained ? List.of() : List.of(new StructuredCitation("KB-OPS-003", null, null, null)),
            riskLevel,
            reviewRequired,
            List.of(),
            abstained,
            abstained ? AbstentionReasonCode.NO_RETRIEVAL_EVIDENCE : AbstentionReasonCode.NONE
        );
    }
}
