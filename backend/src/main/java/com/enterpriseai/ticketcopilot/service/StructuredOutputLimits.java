package com.enterpriseai.ticketcopilot.service;

public final class StructuredOutputLimits {
    public static final int ANSWER_MAX_LENGTH = 1200;
    public static final int CITATION_MAX_COUNT = 5;
    public static final int CITATION_ID_MAX_LENGTH = 64;
    public static final int CITATION_TEXT_MAX_LENGTH = 300;
    public static final int MISSING_INFORMATION_MAX_COUNT = 5;
    public static final int MISSING_INFORMATION_ITEM_MAX_LENGTH = 120;
    public static final int MISSING_INFORMATION_JSON_MAX_LENGTH = 600;

    private StructuredOutputLimits() {
    }
}
