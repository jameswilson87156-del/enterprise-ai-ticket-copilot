package com.enterpriseai.ticketcopilot.model;

public enum CitationRejectionReasonCode {
    NONE,
    NO_RETRIEVAL_EVIDENCE,
    CITATION_REQUIRED,
    CITATION_NOT_IN_RUN,
    CITATION_NOT_ALLOWED_FOR_ABSTENTION,
    LIMIT_EXCEEDED
}
