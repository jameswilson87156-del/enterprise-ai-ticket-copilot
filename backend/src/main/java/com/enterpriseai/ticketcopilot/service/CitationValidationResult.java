package com.enterpriseai.ticketcopilot.service;

import java.util.List;

import com.enterpriseai.ticketcopilot.entity.RetrievalHit;
import com.enterpriseai.ticketcopilot.model.CitationRejectionReasonCode;
import com.enterpriseai.ticketcopilot.model.CitationValidationStatus;
import com.enterpriseai.ticketcopilot.model.StructuredCitation;

public record CitationValidationResult(
    CitationValidationStatus status,
    List<ValidatedCitation> validCitations,
    int rejectedCitationCount,
    CitationRejectionReasonCode rejectionReasonCode
) {
    public boolean accepted() {
        return status == CitationValidationStatus.VALID || status == CitationValidationStatus.NOT_APPLICABLE;
    }

    public static CitationValidationResult valid(List<ValidatedCitation> citations, int rejectedCitationCount) {
        return new CitationValidationResult(
            CitationValidationStatus.VALID,
            List.copyOf(citations),
            rejectedCitationCount,
            CitationRejectionReasonCode.NONE
        );
    }

    public static CitationValidationResult invalid(
        CitationValidationStatus status,
        int rejectedCitationCount,
        CitationRejectionReasonCode reasonCode
    ) {
        return new CitationValidationResult(status, List.of(), rejectedCitationCount, reasonCode);
    }

    public static CitationValidationResult notApplicable() {
        return new CitationValidationResult(CitationValidationStatus.NOT_APPLICABLE, List.of(), 0, CitationRejectionReasonCode.NONE);
    }

    public record ValidatedCitation(
        StructuredCitation citation,
        RetrievalHit retrievalHit
    ) {
    }
}
