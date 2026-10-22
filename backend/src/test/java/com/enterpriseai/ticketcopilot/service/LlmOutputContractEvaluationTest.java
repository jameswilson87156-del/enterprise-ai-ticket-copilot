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

/**
 * Deterministic red-team contract evaluation for model output handling.
 * The ChatModel is a local stub; no provider request is made.
 */
class LlmOutputContractEvaluationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RuleClassificationService ruleService = new RuleClassificationService();

    @Test
    void evaluatesSyntheticOutputContractRedTeamFixture() throws Exception {
        JsonNode fixture;
        try (InputStream input = getClass().getResourceAsStream("/eval/llm-output-contract-redteam-v1.json")) {
            assertThat(input).as("output contract fixture must be available").isNotNull();
            fixture = objectMapper.readTree(input);
        }

        ArrayNode perSample = objectMapper.createArrayNode();
        int attackCases = 0;
        int attackRejected = 0;
        int controlCases = 0;
        int controlsAccepted = 0;

        for (JsonNode sample : fixture.path("cases")) {
            String sampleId = sample.path("id").asText();
            String group = sample.path("group").asText();
            String marker = sample.path("ticketMarker").asText("");
            String expectedSource = "attack".equals(group)
                ? RuleClassificationResult.SOURCE_RULE
                : RuleClassificationResult.SOURCE_LLM;

            SupportTicket ticket = ticket(marker);
            String response = responseFor(sample.path("field").asText(), sample.path("responseValue").asText());
            LlmClassificationService llm = new LlmClassificationService(
                prompt -> chatResponse(response), objectMapper, ruleService);
            AiClassificationFacade facade = new AiClassificationFacade(
                llm, ruleService, new AiAvailability("local-test-stub"));
            RuleClassificationResult result = facade.classify(ticket);
            boolean passed = expectedSource.equals(result.source());

            ObjectNode row = objectMapper.createObjectNode();
            row.put("id", sampleId);
            row.put("group", group);
            row.put("name", sample.path("name").asText());
            row.put("expectedSource", expectedSource);
            row.put("actualSource", result.source());
            row.put("passed", passed);
            perSample.add(row);

            if ("attack".equals(group)) {
                attackCases++;
                if (RuleClassificationResult.SOURCE_RULE.equals(result.source())) {
                    attackRejected++;
                }
            } else {
                controlCases++;
                if (RuleClassificationResult.SOURCE_LLM.equals(result.source())) {
                    controlsAccepted++;
                }
            }
        }

        long failed = perSample.findValuesAsText("passed").stream().filter("false"::equals).count();
        ObjectNode report = objectMapper.createObjectNode();
        report.put("datasetId", fixture.path("datasetId").asText());
        report.put("scope", fixture.path("scope").asText());
        report.put("realProviderRequests", 0);
        report.put("attackCases", attackCases);
        report.put("attackRejected", attackRejected);
        report.put("controlCases", controlCases);
        report.put("controlsAccepted", controlsAccepted);
        report.put("passedCases", perSample.size() - failed);
        report.put("failedCases", failed);
        report.set("samples", perSample);

        Path output = Path.of("target", "evaluation", "llm-output-contract-redteam-v1.json");
        Files.createDirectories(output.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);

        assertThat(attackCases).isEqualTo(9);
        assertThat(attackRejected).isEqualTo(9);
        assertThat(controlCases).isEqualTo(2);
        assertThat(controlsAccepted).isEqualTo(2);
        assertThat(failed).isZero();
    }

    @Test
    void evaluatesUnicodeConfusableMarkerEchoesAndBenignControls() throws Exception {
        JsonNode fixture;
        try (InputStream input = getClass().getResourceAsStream("/eval/ticket-output-confusable-redteam-v1.json")) {
            assertThat(input).as("Unicode confusable fixture must be available").isNotNull();
            fixture = objectMapper.readTree(input);
        }

        ArrayNode perSample = objectMapper.createArrayNode();
        int attackCases = 0;
        int attackRejected = 0;
        int controlCases = 0;
        int controlsAccepted = 0;

        for (JsonNode sample : fixture.path("cases")) {
            String group = sample.path("group").asText();
            String expectedSource = "attack".equals(group)
                ? RuleClassificationResult.SOURCE_RULE
                : RuleClassificationResult.SOURCE_LLM;
            String response = responseFor("reason", sample.path("responseValue").asText());
            LlmClassificationService llm = new LlmClassificationService(
                prompt -> chatResponse(response), objectMapper, ruleService);
            AiClassificationFacade facade = new AiClassificationFacade(
                llm, ruleService, new AiAvailability("local-test-stub"));
            RuleClassificationResult actual = facade.classify(ticket(sample.path("ticketMarker").asText("")));
            boolean passed = expectedSource.equals(actual.source());

            if ("attack".equals(group)) {
                attackCases++;
                if (passed) attackRejected++;
            } else {
                controlCases++;
                if (passed) controlsAccepted++;
            }

            var row = objectMapper.createObjectNode();
            row.put("id", sample.path("id").asText());
            row.put("group", group);
            row.put("expectedSource", expectedSource);
            row.put("actualSource", actual.source());
            row.put("passed", passed);
            perSample.add(row);
        }

        int failed = (int) perSample.findParents("passed").stream()
            .filter(row -> !row.path("passed").asBoolean()).count();
        var report = objectMapper.createObjectNode();
        report.put("datasetId", fixture.path("version").asText());
        report.put("scope", "synthetic Unicode confusable output-contract tests; mocked ChatModel");
        report.put("realProviderRequests", 0);
        report.put("attackCases", attackCases);
        report.put("attackRejected", attackRejected);
        report.put("controlCases", controlCases);
        report.put("controlsAccepted", controlsAccepted);
        report.put("passedCases", perSample.size() - failed);
        report.put("failedCases", failed);
        report.set("samples", perSample);

        Path output = Path.of("target", "evaluation", "ticket-output-confusable-redteam-v1.json");
        Files.createDirectories(output.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);

        assertThat(attackCases).isEqualTo(3);
        assertThat(attackRejected).isEqualTo(3);
        assertThat(controlCases).isEqualTo(2);
        assertThat(controlsAccepted).isEqualTo(2);
        assertThat(failed).isZero();
    }

    @Test
    void evaluatesEncodedInputInjectionRemovalAndBenignControls() throws Exception {
        JsonNode fixture;
        try (InputStream input = getClass().getResourceAsStream("/eval/ticket-input-encoded-redteam-v1.json")) {
            assertThat(input).as("encoded input fixture must be available").isNotNull();
            fixture = objectMapper.readTree(input);
        }

        var perSample = objectMapper.createArrayNode();
        int attackCases = 0;
        int attackRemoved = 0;
        int controlCases = 0;
        int controlsPreserved = 0;
        for (JsonNode sample : fixture.path("cases")) {
            String text = sample.path("text").asText();
            String sanitized = ruleService.stripExplicitInjectionSentences(text);
            boolean isAttack = "attack".equals(sample.path("group").asText());
            boolean retained = sanitized.contains(sample.path("retainedCue").asText());
            String marker = sample.path("removedMarker").asText("");
            boolean removed = marker.isBlank() || !sanitized.contains(marker);
            boolean controlPreserved = !isAttack && retained;
            boolean passed = retained && removed && (isAttack || controlPreserved);

            var row = objectMapper.createObjectNode();
            row.put("id", sample.path("id").asText());
            row.put("group", sample.path("group").asText());
            row.put("attackType", sample.path("attackType").asText());
            row.put("retainedCue", retained);
            row.put("markerRemoved", removed);
            row.put("controlPreserved", controlPreserved);
            row.put("passed", passed);
            perSample.add(row);

            if (isAttack) {
                attackCases++;
                if (passed) attackRemoved++;
            } else {
                controlCases++;
                if (passed) controlsPreserved++;
            }
        }
        int failed = (int) perSample.findParents("passed").stream()
            .filter(row -> !row.path("passed").asBoolean()).count();
        var report = objectMapper.createObjectNode();
        report.put("datasetId", fixture.path("version").asText());
        report.put("scope", fixture.path("scope").asText());
        report.put("realProviderRequests", 0);
        report.put("attackCases", attackCases);
        report.put("attackRemovedWithIssuePreserved", attackRemoved);
        report.put("controlCases", controlCases);
        report.put("controlsPreserved", controlsPreserved);
        report.put("passedCases", perSample.size() - failed);
        report.put("failedCases", failed);
        report.set("samples", perSample);
        Path output = Path.of("target", "evaluation", "ticket-input-encoded-redteam-v1.json");
        Files.createDirectories(output.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);

        assertThat(attackCases).isEqualTo(2);
        assertThat(attackRemoved).isEqualTo(2);
        assertThat(controlCases).isEqualTo(2);
        assertThat(controlsPreserved).isEqualTo(2);
        assertThat(failed).isZero();
    }

    @Test
    void evaluatesEncodedOutputMarkerEchoesAndBenignControls() throws Exception {
        JsonNode fixture;
        try (InputStream input = getClass().getResourceAsStream("/eval/ticket-output-encoded-redteam-v1.json")) {
            assertThat(input).as("encoded output fixture must be available").isNotNull();
            fixture = objectMapper.readTree(input);
        }

        var perSample = objectMapper.createArrayNode();
        int attackCases = 0;
        int attackRejected = 0;
        int controlCases = 0;
        int controlsAccepted = 0;
        for (JsonNode sample : fixture.path("cases")) {
            String group = sample.path("group").asText();
            String expectedSource = "attack".equals(group)
                ? RuleClassificationResult.SOURCE_RULE : RuleClassificationResult.SOURCE_LLM;
            String response = responseFor("reason", sample.path("responseValue").asText());
            LlmClassificationService llm = new LlmClassificationService(
                prompt -> chatResponse(response), objectMapper, ruleService);
            AiClassificationFacade facade = new AiClassificationFacade(
                llm, ruleService, new AiAvailability("local-test-stub"));
            RuleClassificationResult actual = facade.classify(ticket(sample.path("ticketMarker").asText("")));
            boolean passed = expectedSource.equals(actual.source());

            var row = objectMapper.createObjectNode();
            row.put("id", sample.path("id").asText());
            row.put("group", group);
            row.put("attackType", sample.path("attackType").asText());
            row.put("expectedSource", expectedSource);
            row.put("actualSource", actual.source());
            row.put("passed", passed);
            perSample.add(row);

            if ("attack".equals(group)) {
                attackCases++;
                if (passed) attackRejected++;
            } else {
                controlCases++;
                if (passed) controlsAccepted++;
            }
        }
        int failed = (int) perSample.findParents("passed").stream()
            .filter(row -> !row.path("passed").asBoolean()).count();
        var report = objectMapper.createObjectNode();
        report.put("datasetId", fixture.path("version").asText());
        report.put("scope", fixture.path("scope").asText());
        report.put("realProviderRequests", 0);
        report.put("attackCases", attackCases);
        report.put("attackRejectedAndRuleFallback", attackRejected);
        report.put("controlCases", controlCases);
        report.put("controlsAcceptedAsLlm", controlsAccepted);
        report.put("passedCases", perSample.size() - failed);
        report.put("failedCases", failed);
        report.set("samples", perSample);
        Path output = Path.of("target", "evaluation", "ticket-output-encoded-redteam-v1.json");
        Files.createDirectories(output.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);

        assertThat(attackCases).isEqualTo(2);
        assertThat(attackRejected).isEqualTo(2);
        assertThat(controlCases).isEqualTo(2);
        assertThat(controlsAccepted).isEqualTo(2);
        assertThat(failed).isZero();
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

    private SupportTicket ticket(String marker) {
        String title = "OA 登录失败，重置密码后仍返回 401。";
        if (!marker.isBlank()) {
            title += " 工单诊断标记 " + marker;
        } else {
            title = "Canary deployment 后 OA 登录失败，重置密码后仍返回 401。";
        }
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo("TCK-OUTPUT-REDTEAM");
        ticket.setTitle(title);
        ticket.setDescription(title);
        ticket.setErrorLog("");
        return ticket;
    }

    private ChatResponse chatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
