package com.enterpriseai.ticketcopilot.service;

import com.enterpriseai.ticketcopilot.model.AbstentionReasonCode;
import com.enterpriseai.ticketcopilot.model.OutputValidationStatus;
import com.enterpriseai.ticketcopilot.model.RiskLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredOutputParserTest {

    private final StructuredOutputParser parser = new StructuredOutputParser(new ObjectMapper());

    @Test
    void parsesValidJsonStructuredOutput() {
        StructuredOutputParseResult result = parser.parse(validJson());

        assertThat(result.valid()).isTrue();
        assertThat(result.status()).isEqualTo(OutputValidationStatus.VALID);
        assertThat(result.output().answer()).isEqualTo("??????????");
        assertThat(result.output().citations()).hasSize(1);
        assertThat(result.output().citations().get(0).knowledgeArticleId()).isEqualTo("KB-OPS-003");
        assertThat(result.output().riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.output().abstentionReasonCode()).isEqualTo(AbstentionReasonCode.NONE);
    }

    @Test
    void stripsSingleMarkdownCodeFence() {
        StructuredOutputParseResult result = parser.parse("```json\n" + validJson() + "\n```");

        assertThat(result.valid()).isTrue();
        assertThat(result.output().citations().get(0).knowledgeArticleId()).isEqualTo("KB-OPS-003");
    }

    @Test
    void rejectsNonJsonText() {
        assertThat(parser.parse("not-json").status()).isEqualTo(OutputValidationStatus.INVALID_JSON);
    }

    @Test
    void rejectsMissingAnswer() {
        String json = validJson().replace("\"answer\":\"??????????\",", "");
        assertThat(parser.parse(json).status()).isEqualTo(OutputValidationStatus.MISSING_FIELD);
    }

    @Test
    void rejectsMissingCitations() {
        String json = validJson().replace("\"citations\":[{\"knowledgeArticleId\":\"KB-OPS-003\",\"supportedClaim\":\"claim\",\"evidenceExcerpt\":\"excerpt\"}],", "");
        assertThat(parser.parse(json).status()).isEqualTo(OutputValidationStatus.MISSING_FIELD);
    }

    @Test
    void rejectsUnknownRiskLevel() {
        assertThat(parser.parse(validJson().replace("MEDIUM", "CRITICAL")).status()).isEqualTo(OutputValidationStatus.INVALID_ENUM);
    }

    @Test
    void rejectsUnknownAbstentionReasonCode() {
        assertThat(parser.parse(validJson().replace("NONE", "MADE_UP_REASON")).status()).isEqualTo(OutputValidationStatus.INVALID_ENUM);
    }

    @Test
    void rejectsAbstainedOutputWithNoneReason() {
        String json = validJson()
            .replace("\"abstained\":false", "\"abstained\":true")
            .replace("\"humanReviewRequired\":true", "\"humanReviewRequired\":true");

        assertThat(parser.parse(json).status()).isEqualTo(OutputValidationStatus.INVALID_FIELD_TYPE);
    }

    @Test
    void rejectsNonAbstainedOutputWithAbstentionReason() {
        assertThat(parser.parse(validJson().replace("\"abstentionReasonCode\":\"NONE\"", "\"abstentionReasonCode\":\"OUTPUT_POLICY_REJECTED\"")).status())
            .isEqualTo(OutputValidationStatus.INVALID_FIELD_TYPE);
    }

    @Test
    void rejectsAbstainedOutputWhenModelTriesToDisableReview() {
        String json = "{"
            + "\"answer\":\"malicious provider refusal text\","
            + "\"citations\":[],"
            + "\"riskLevel\":\"LOW\","
            + "\"humanReviewRequired\":false,"
            + "\"missingInformation\":[],"
            + "\"abstained\":true,"
            + "\"abstentionReasonCode\":\"MISSING_REQUIRED_INFORMATION\""
            + "}";

        assertThat(parser.parse(json).status()).isEqualTo(OutputValidationStatus.INVALID_FIELD_TYPE);
    }

    @Test
    void rejectsAnswerOverLimit() {
        String longAnswer = "A".repeat(StructuredOutputLimits.ANSWER_MAX_LENGTH + 1);
        assertThat(parser.parse(validJson().replace("??????????", longAnswer)).status()).isEqualTo(OutputValidationStatus.LIMIT_EXCEEDED);
    }

    @Test
    void rejectsCitationCountOverLimit() {
        String citations = "[" + "{\"knowledgeArticleId\":\"KB-1\"},".repeat(StructuredOutputLimits.CITATION_MAX_COUNT)
            + "{\"knowledgeArticleId\":\"KB-OVER\"}]";
        String json = validJson().replace("[{\"knowledgeArticleId\":\"KB-OPS-003\",\"supportedClaim\":\"claim\",\"evidenceExcerpt\":\"excerpt\"}]", citations);
        assertThat(parser.parse(json).status()).isEqualTo(OutputValidationStatus.LIMIT_EXCEEDED);
    }

    @Test
    void rejectsMissingInformationOverLimit() {
        String missing = "[" + ("\"" + "M".repeat(StructuredOutputLimits.MISSING_INFORMATION_ITEM_MAX_LENGTH + 1) + "\"") + "]";
        String json = validJson().replace("\"missingInformation\":[]", "\"missingInformation\":" + missing);
        assertThat(parser.parse(json).status()).isEqualTo(OutputValidationStatus.LIMIT_EXCEEDED);
    }

    @Test
    void rejectsMissingInformationWhenSerializedJsonWouldExceedColumnLimit() {
        String item = "M".repeat(StructuredOutputLimits.MISSING_INFORMATION_ITEM_MAX_LENGTH);
        String missing = "[\"" + item + "\",\"" + item + "\",\"" + item + "\",\"" + item + "\",\"" + item + "\"]";
        String json = validJson().replace("\"missingInformation\":[]", "\"missingInformation\":" + missing);

        assertThat(parser.parse(json).status()).isEqualTo(OutputValidationStatus.LIMIT_EXCEEDED);
    }

    @Test
    void rejectsMultipleJsonObjects() {
        assertThat(parser.parse(validJson() + validJson()).status()).isEqualTo(OutputValidationStatus.MULTIPLE_JSON_OBJECTS);
    }

    @Test
    void rejectsExplanationWrappedJsonAndDoesNotTreatRawTextAsAnswer() {
        StructuredOutputParseResult result = parser.parse("Here is the result: " + validJson());

        assertThat(result.valid()).isFalse();
        assertThat(result.output()).isNull();
    }

    private String validJson() {
        return "{"
            + "\"answer\":\"??????????\","
            + "\"citations\":[{\"knowledgeArticleId\":\"KB-OPS-003\",\"supportedClaim\":\"claim\",\"evidenceExcerpt\":\"excerpt\"}],"
            + "\"riskLevel\":\"MEDIUM\","
            + "\"humanReviewRequired\":true,"
            + "\"missingInformation\":[],"
            + "\"abstained\":false,"
            + "\"abstentionReasonCode\":\"NONE\""
            + "}";
    }
}
