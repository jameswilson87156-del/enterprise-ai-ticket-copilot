package com.enterpriseai.ticketcopilot.service;

import com.enterpriseai.ticketcopilot.model.OutputValidationStatus;
import com.enterpriseai.ticketcopilot.model.StructuredCopilotOutput;

public record StructuredOutputParseResult(
    boolean valid,
    StructuredCopilotOutput output,
    OutputValidationStatus status
) {
    public static StructuredOutputParseResult valid(StructuredCopilotOutput output) {
        return new StructuredOutputParseResult(true, output, OutputValidationStatus.VALID);
    }

    public static StructuredOutputParseResult invalid(OutputValidationStatus status) {
        return new StructuredOutputParseResult(false, null, status);
    }
}
