package com.enterpriseai.ticketcopilot.service;

import com.enterpriseai.ticketcopilot.model.CitationValidationStatus;
import com.enterpriseai.ticketcopilot.model.OutputValidationStatus;
import com.enterpriseai.ticketcopilot.model.RiskLevel;
import com.enterpriseai.ticketcopilot.model.StructuredCopilotOutput;
import org.springframework.stereotype.Service;

@Service
public class ReviewGate {

    public boolean finalHumanReviewRequired(
        StructuredCopilotOutput output,
        boolean fallbackUsed,
        CitationValidationResult citationValidationResult,
        OutputValidationStatus outputValidationStatus,
        boolean businessRuleRequiresReview
    ) {
        boolean modelRequiresReview = output == null || output.humanReviewRequired();
        boolean highRisk = output == null || output.riskLevel() == RiskLevel.HIGH;
        boolean abstained = output == null || output.abstained();
        boolean citationFailed = citationValidationResult != null
            && citationValidationResult.status() != CitationValidationStatus.VALID
            && citationValidationResult.status() != CitationValidationStatus.NOT_APPLICABLE;
        boolean structuredOutputFailed = outputValidationStatus != null
            && outputValidationStatus != OutputValidationStatus.VALID
            && outputValidationStatus != OutputValidationStatus.NOT_APPLICABLE;
        return modelRequiresReview
            || highRisk
            || fallbackUsed
            || abstained
            || citationFailed
            || structuredOutputFailed
            || businessRuleRequiresReview;
    }
}
