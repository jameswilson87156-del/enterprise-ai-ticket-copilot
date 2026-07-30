package com.enterpriseai.ticketcopilot.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.enterpriseai.ticketcopilot.model.AbstentionReasonCode;
import com.enterpriseai.ticketcopilot.model.OutputValidationStatus;
import com.enterpriseai.ticketcopilot.model.RiskLevel;
import com.enterpriseai.ticketcopilot.model.StructuredCitation;
import com.enterpriseai.ticketcopilot.model.StructuredCopilotOutput;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class StructuredOutputParser {

    private final ObjectMapper objectMapper;

    public StructuredOutputParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public StructuredOutputParseResult parse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.EMPTY_RESPONSE);
        }
        String normalized = stripSingleCodeFence(rawResponse.trim()).trim();
        if (!normalized.startsWith("{") || !normalized.endsWith("}")) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_JSON);
        }
        JsonNode root;
        try (JsonParser parser = objectMapper.getFactory().createParser(normalized)) {
            root = objectMapper.readValue(parser, JsonNode.class);
            JsonToken trailing = parser.nextToken();
            if (trailing != null) {
                return StructuredOutputParseResult.invalid(OutputValidationStatus.MULTIPLE_JSON_OBJECTS);
            }
        } catch (JsonProcessingException exception) {
            String message = exception.getOriginalMessage() == null ? "" : exception.getOriginalMessage().toLowerCase();
            if (message.contains("trailing")) {
                return StructuredOutputParseResult.invalid(OutputValidationStatus.MULTIPLE_JSON_OBJECTS);
            }
            return StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_JSON);
        } catch (IOException exception) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_JSON);
        }
        if (!root.isObject()) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_JSON);
        }
        return parseObject(root);
    }

    private StructuredOutputParseResult parseObject(JsonNode root) {
        String answer = requiredText(root, "answer");
        if (answer == null || answer.isBlank()) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.MISSING_FIELD);
        }
        if (answer.length() > StructuredOutputLimits.ANSWER_MAX_LENGTH) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.LIMIT_EXCEEDED);
        }

        JsonNode citationsNode = root.get("citations");
        if (citationsNode == null || citationsNode.isNull()) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.MISSING_FIELD);
        }
        if (!citationsNode.isArray()) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_FIELD_TYPE);
        }
        if (citationsNode.size() > StructuredOutputLimits.CITATION_MAX_COUNT) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.LIMIT_EXCEEDED);
        }
        List<StructuredCitation> citations = new ArrayList<>();
        for (JsonNode citationNode : citationsNode) {
            if (!citationNode.isObject()) {
                return StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_FIELD_TYPE);
            }
            String knowledgeArticleId = requiredText(citationNode, "knowledgeArticleId");
            if (knowledgeArticleId == null || knowledgeArticleId.isBlank()) {
                return StructuredOutputParseResult.invalid(OutputValidationStatus.MISSING_FIELD);
            }
            if (knowledgeArticleId.length() > StructuredOutputLimits.CITATION_ID_MAX_LENGTH || looksLikeUrl(knowledgeArticleId)) {
                return StructuredOutputParseResult.invalid(OutputValidationStatus.LIMIT_EXCEEDED);
            }
            String supportedClaim = optionalBoundedText(citationNode, "supportedClaim", StructuredOutputLimits.CITATION_TEXT_MAX_LENGTH);
            String reason = optionalBoundedText(citationNode, "reason", StructuredOutputLimits.CITATION_TEXT_MAX_LENGTH);
            String evidenceExcerpt = optionalBoundedText(citationNode, "evidenceExcerpt", StructuredOutputLimits.CITATION_TEXT_MAX_LENGTH);
            if (supportedClaim == null && citationNode.hasNonNull("supportedClaim")) {
                return StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_FIELD_TYPE);
            }
            if (reason == null && citationNode.hasNonNull("reason")) {
                return StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_FIELD_TYPE);
            }
            if (evidenceExcerpt == null && citationNode.hasNonNull("evidenceExcerpt")) {
                return StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_FIELD_TYPE);
            }
            citations.add(new StructuredCitation(knowledgeArticleId.trim(), supportedClaim, reason, evidenceExcerpt));
        }

        RiskLevel riskLevel = enumValue(root, "riskLevel", RiskLevel.class);
        if (riskLevel == null) {
            return missingOrInvalidEnum(root, "riskLevel");
        }

        JsonNode modelReview = root.get("humanReviewRequired");
        if (modelReview == null || !modelReview.isBoolean()) {
            return modelReview == null ? StructuredOutputParseResult.invalid(OutputValidationStatus.MISSING_FIELD)
                : StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_FIELD_TYPE);
        }

        JsonNode missingNode = root.get("missingInformation");
        if (missingNode == null || missingNode.isNull()) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.MISSING_FIELD);
        }
        if (!missingNode.isArray()) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_FIELD_TYPE);
        }
        if (missingNode.size() > StructuredOutputLimits.MISSING_INFORMATION_MAX_COUNT) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.LIMIT_EXCEEDED);
        }
        List<String> missingInformation = new ArrayList<>();
        for (JsonNode item : missingNode) {
            if (!item.isTextual()) {
                return StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_FIELD_TYPE);
            }
            String value = item.asText().trim();
            if (value.length() > StructuredOutputLimits.MISSING_INFORMATION_ITEM_MAX_LENGTH) {
                return StructuredOutputParseResult.invalid(OutputValidationStatus.LIMIT_EXCEEDED);
            }
            if (!value.isBlank()) {
                missingInformation.add(value);
            }
        }

        JsonNode abstainedNode = root.get("abstained");
        if (abstainedNode == null || !abstainedNode.isBoolean()) {
            return abstainedNode == null ? StructuredOutputParseResult.invalid(OutputValidationStatus.MISSING_FIELD)
                : StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_FIELD_TYPE);
        }

        AbstentionReasonCode reasonCode = enumValue(root, "abstentionReasonCode", AbstentionReasonCode.class);
        if (reasonCode == null) {
            return missingOrInvalidEnum(root, "abstentionReasonCode");
        }

        return StructuredOutputParseResult.valid(new StructuredCopilotOutput(
            answer.trim(),
            List.copyOf(citations),
            riskLevel,
            modelReview.asBoolean(),
            List.copyOf(missingInformation),
            abstainedNode.asBoolean(),
            reasonCode
        ));
    }

    private String stripSingleCodeFence(String value) {
        if (!value.startsWith("```")) {
            return value;
        }
        int firstNewline = value.indexOf('\n');
        if (firstNewline < 0 || !value.endsWith("```")) {
            return value;
        }
        String body = value.substring(firstNewline + 1, value.length() - 3);
        return body.strip();
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null) {
            return null;
        }
        return child.isTextual() ? child.asText() : null;
    }

    private String optionalBoundedText(JsonNode node, String field, int maxLength) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        if (!child.isTextual() || child.asText().length() > maxLength) {
            return null;
        }
        String value = child.asText().trim();
        return value.isBlank() ? null : value;
    }

    private <T extends Enum<T>> T enumValue(JsonNode node, String field, Class<T> type) {
        JsonNode child = node.get(field);
        if (child == null || !child.isTextual()) {
            return null;
        }
        try {
            return Enum.valueOf(type, child.asText().trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private StructuredOutputParseResult missingOrInvalidEnum(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.MISSING_FIELD);
        }
        if (!child.isTextual()) {
            return StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_FIELD_TYPE);
        }
        return StructuredOutputParseResult.invalid(OutputValidationStatus.INVALID_ENUM);
    }

    private boolean looksLikeUrl(String value) {
        String normalized = value.toLowerCase();
        return normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.contains("://");
    }
}
