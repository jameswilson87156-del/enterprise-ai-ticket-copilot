package com.enterpriseai.ticketcopilot.model;

public enum OutputValidationStatus {
    VALID,
    NOT_APPLICABLE,
    EMPTY_RESPONSE,
    INVALID_JSON,
    MISSING_FIELD,
    INVALID_FIELD_TYPE,
    INVALID_ENUM,
    LIMIT_EXCEEDED,
    MULTIPLE_JSON_OBJECTS
}
