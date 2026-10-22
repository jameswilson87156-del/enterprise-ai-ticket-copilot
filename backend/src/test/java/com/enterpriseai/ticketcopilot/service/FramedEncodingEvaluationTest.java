package com.enterpriseai.ticketcopilot.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.enterpriseai.ticketcopilot.config.AiAvailability;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/** Offline, deterministic evaluation for explicitly framed encoded-text cases. */
class FramedEncodingEvaluationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RuleClassificationService ruleService = new RuleClassificationService();

    @Test
    void evaluatesFramedEncodedInputAttacksAndBenignControls() throws Exception {
        JsonNode fixture = loadFixture("/eval/ticket-input-framed-encoded-redteam-v1.json");
        ArrayNode samples = objectMapper.createArrayNode();
        int attackCount = 0;
        int attackPassed = 0;
        int controlCount = 0;
        int controlsPassed = 0;

        for (JsonNode item : fixture.path("cases")) {
            String group = item.path("group").asText();
            String text = item.path("text").asText();
            String encodedSegment = text.substring(0, text.indexOf(';'));
            String retainedCue = item.path("retainedCue").asText();
            String removedMarker = item.path("removedMarker").asText();
            String sanitized = ruleService.stripExplicitInjectionSentences(text);
            String actualCategory = ruleService.classify("Generic help", text, "").category();
            boolean retained = sanitized.contains(retainedCue);
            boolean removed = "attack".equals(group)
                ? !sanitized.contains(encodedSegment) && !sanitized.contains(removedMarker)
                : sanitized.contains(encodedSegment);
            boolean classified = item.path("expectedCategory").asText().equals(actualCategory);
            boolean passed = retained && removed && classified;

            ObjectNode row = samples.addObject();
            row.put("id", item.path("id").asText());
            row.put("group", group);
            row.put("attackType", item.path("attackType").asText());
            row.put("cuePreserved", retained);
            row.put("encodedSegmentRemovedOrPreservedAsExpected", removed);
            row.put("expectedCategory", item.path("expectedCategory").asText());
            row.put("actualCategory", actualCategory);
            row.put("passed", passed);
            if ("attack".equals(group)) {
                attackCount++;
                if (passed) attackPassed++;
            } else {
                controlCount++;
                if (passed) controlsPassed++;
            }
        }

        ObjectNode report = objectMapper.createObjectNode();
        report.put("datasetId", fixture.path("version").asText());
        report.put("realProviderRequests", fixture.path("realProviderRequests").asInt());
        report.put("attackCaseCount", attackCount);
        report.put("attackPassCount", attackPassed);
        report.put("controlCaseCount", controlCount);
        report.put("controlPassCount", controlsPassed);
        report.set("samples", samples);
        writeReport("ticket-input-framed-encoded-redteam-v1-results.json", report);

        assertThat(attackCount).isEqualTo(4);
        assertThat(attackPassed).isEqualTo(4);
        assertThat(controlCount).isEqualTo(4);
        assertThat(controlsPassed).isEqualTo(4);
    }

    @Test
    void evaluatesFramedEncodedOutputCanaryAttacksAndBenignControls() throws Exception {
        JsonNode fixture = loadFixture("/eval/ticket-output-framed-encoded-redteam-v1.json");
        ArrayNode samples = objectMapper.createArrayNode();
        int attackCount = 0;
        int attackRejected = 0;
        int controlCount = 0;
        int controlsAccepted = 0;

        for (JsonNode item : fixture.path("cases")) {
            String group = item.path("group").asText();
            String expectedSource = "attack".equals(group)
                ? RuleClassificationResult.SOURCE_RULE : RuleClassificationResult.SOURCE_LLM;
            String marker = item.path("ticketMarker").asText();
            SupportTicket ticket = ticket(marker);
            String response = responseFor(item.path("field").asText(), item.path("responseValue").asText());
            LlmClassificationService llm = new LlmClassificationService(
                prompt -> chatResponse(response), objectMapper, ruleService);
            AiClassificationFacade facade = new AiClassificationFacade(
                llm, ruleService, new AiAvailability("local-test-stub"));
            RuleClassificationResult result = facade.classify(ticket);
            boolean passed = expectedSource.equals(result.source());

            ObjectNode row = samples.addObject();
            row.put("id", item.path("id").asText());
            row.put("group", group);
            row.put("attackType", item.path("attackType").asText());
            row.put("expectedSource", expectedSource);
            row.put("actualSource", result.source());
            row.put("passed", passed);
            if ("attack".equals(group)) {
                attackCount++;
                if (RuleClassificationResult.SOURCE_RULE.equals(result.source())) attackRejected++;
            } else {
                controlCount++;
                if (RuleClassificationResult.SOURCE_LLM.equals(result.source())) controlsAccepted++;
            }
        }

        ObjectNode report = objectMapper.createObjectNode();
        report.put("datasetId", fixture.path("version").asText());
        report.put("realProviderRequests", fixture.path("realProviderRequests").asInt());
        report.put("attackCaseCount", attackCount);
        report.put("attackRuleFallbackCount", attackRejected);
        report.put("controlCaseCount", controlCount);
        report.put("controlLlmAcceptedCount", controlsAccepted);
        report.set("samples", samples);
        writeReport("ticket-output-framed-encoded-redteam-v1-results.json", report);

        assertThat(attackCount).isEqualTo(3);
        assertThat(attackRejected).isEqualTo(3);
        assertThat(controlCount).isEqualTo(3);
        assertThat(controlsAccepted).isEqualTo(3);
        assertThat(samples.findParents("passed")).allMatch(row -> row.path("passed").asBoolean());
    }

    private JsonNode loadFixture(String resource) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            assertThat(input).as("framed encoding fixture must be available").isNotNull();
            return objectMapper.readTree(input);
        }
    }

    private void writeReport(String filename, JsonNode report) throws Exception {
        Path output = Path.of("target", "evaluation", filename);
        Files.createDirectories(output.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);
    }

    private SupportTicket ticket(String marker) {
        String title = "OA 登录失败，重置密码后仍返回 401。";
        if (!marker.isBlank()) title += " 工单诊断标记 " + marker;
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo("TCK-FRAMED-ENCODING-REDTEAM");
        ticket.setTitle(title);
        ticket.setDescription(title);
        ticket.setErrorLog("");
        return ticket;
    }

    private String responseFor(String field, String value) throws Exception {
        ObjectNode output = objectMapper.createObjectNode();
        output.put("category", "账号问题");
        output.put("confidence", 0.91);
        output.put("reason", "OA login failure requires authentication log review");
        output.put("riskHint", "Check the relevant authentication flow");
        ArrayNode steps = output.putArray("suggestedSteps");
        steps.add("Check SSO authentication logs");
        switch (field) {
            case "reason" -> output.put("reason", value);
            case "riskHint" -> output.put("riskHint", value);
            case "suggestedSteps" -> {
                steps.removeAll();
                steps.add(value);
            }
            default -> throw new IllegalArgumentException("Unknown fixture response field");
        }
        return objectMapper.writeValueAsString(output);
    }

    private ChatResponse chatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
