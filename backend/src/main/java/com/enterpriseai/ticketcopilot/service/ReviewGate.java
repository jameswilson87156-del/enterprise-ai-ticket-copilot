package com.enterpriseai.ticketcopilot.service;

import com.enterpriseai.ticketcopilot.model.CitationValidationStatus;
import com.enterpriseai.ticketcopilot.model.OutputValidationStatus;
import com.enterpriseai.ticketcopilot.model.RiskLevel;
import com.enterpriseai.ticketcopilot.model.StructuredCopilotOutput;
import com.enterpriseai.ticketcopilot.ticket.application.port.out.ReviewPolicy;
import org.springframework.stereotype.Service;

@Service
public class ReviewGate implements ReviewPolicy {

    @Override
    public boolean requiresHumanReview(
        StructuredCopilotOutput output,
        boolean fallbackUsed,
        CitationValidationStatus citationValidationStatus,
        OutputValidationStatus outputValidationStatus,
        boolean businessRuleRequiresReview
    ) {
        boolean modelRequiresReview = output == null || output.humanReviewRequired();
        boolean highRisk = output == null || output.riskLevel() == RiskLevel.HIGH;
        boolean abstained = output == null || output.abstained();
        boolean missingInformation = output != null && output.missingInformation() != null && !output.missingInformation().isEmpty();
        boolean citationFailed = citationValidationStatus != null
            && citationValidationStatus != CitationValidationStatus.VALID
            && citationValidationStatus != CitationValidationStatus.NOT_APPLICABLE;
        boolean structuredOutputFailed = outputValidationStatus != null
            && outputValidationStatus != OutputValidationStatus.VALID
            && outputValidationStatus != OutputValidationStatus.NOT_APPLICABLE;
        return modelRequiresReview
            || highRisk
            || fallbackUsed
            || abstained
            || missingInformation
            || citationFailed
            || structuredOutputFailed
            || businessRuleRequiresReview;
    }

    /**
     * Compatibility method for existing service-package callers and tests.
     * New application code depends on {@link ReviewPolicy} instead.
     */
    public boolean finalHumanReviewRequired(
        StructuredCopilotOutput output,
        boolean fallbackUsed,
        CitationValidationResult citationValidationResult,
        OutputValidationStatus outputValidationStatus,
        boolean businessRuleRequiresReview
    ) {
        return requiresHumanReview(
            output,
            fallbackUsed,
            citationValidationResult == null ? null : citationValidationResult.status(),
            outputValidationStatus,
            businessRuleRequiresReview
        );
    }
}
