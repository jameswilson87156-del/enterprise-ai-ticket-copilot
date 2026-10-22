package com.enterpriseai.ticketcopilot.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BenignSecurityVocabularyControlEvaluationTest {

    private static final String FIXTURE = "/eval/ticket-benign-security-controls-v1.json";
    private static final List<String> CATEGORIES = List.of("账号问题", "权限问题", "系统故障", "数据问题", "流程咨询");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RuleClassificationService service = new RuleClassificationService();

    @Test
    void benignSecurityVocabularyControlsPreserveTicketIssuesAndCategories() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(FIXTURE)) {
            assertThat(input).as("evaluation fixture %s", FIXTURE).isNotNull();
            Dataset dataset = objectMapper.readValue(input, Dataset.class);
            assertThat(dataset.version()).isEqualTo("v1-benign-security-vocabulary-controls-10");
            assertThat(dataset.source()).contains("Synthetic");
            assertThat(dataset.cases()).hasSize(10);
            assertThat(dataset.cases().stream().map(Case::id).distinct().count()).isEqualTo(10);
            assertThat(dataset.cases().stream().collect(Collectors.groupingBy(Case::expected, Collectors.counting())))
                .containsEntry("账号问题", 2L)
                .containsEntry("权限问题", 2L)
                .containsEntry("系统故障", 2L)
                .containsEntry("数据问题", 2L)
                .containsEntry("流程咨询", 2L);

            Map<String, Map<String, Integer>> confusion = new LinkedHashMap<>();
            for (String expected : CATEGORIES) {
                Map<String, Integer> row = new LinkedHashMap<>();
                for (String predicted : CATEGORIES) {
                    row.put(predicted, 0);
                }
                confusion.put(expected, row);
            }
            List<CaseResult> results = new ArrayList<>();
            for (Case evalCase : dataset.cases()) {
                RuleClassificationResult prediction = service.classify(
                    evalCase.title(), evalCase.description(), evalCase.errorLog());
                String sanitizedText = String.join("\n",
                    service.stripExplicitInjectionSentences(evalCase.title()),
                    service.stripExplicitInjectionSentences(evalCase.description()),
                    service.stripExplicitInjectionSentences(evalCase.errorLog()));
                boolean categoryCorrect = evalCase.expected().equals(prediction.category());
                boolean issueCuePreserved = sanitizedText.contains(evalCase.mustRetain());
                confusion.get(evalCase.expected()).compute(prediction.category(), (key, count) -> count == null ? 1 : count + 1);
                results.add(new CaseResult(evalCase.id(), evalCase.expected(), prediction.category(),
                    categoryCorrect, evalCase.mustRetain(), issueCuePreserved));
            }

            long correct = results.stream().filter(CaseResult::categoryCorrect).count();
            long issueCuesPreserved = results.stream().filter(CaseResult::issueCuePreserved).count();
            List<String> failedIds = results.stream().filter(result -> !result.categoryCorrect() || !result.issueCuePreserved())
                .map(CaseResult::id).toList();
            EvaluationReport report = new EvaluationReport(dataset.version(), dataset.source(), "RULE_ONLY",
                results.size(), correct, issueCuesPreserved, confusion, results, failedIds,
                "Fixed synthetic benign-context controls only; not an LLM safety result or production performance claim.");
            Path output = Path.of("target", "evaluation", "ticket-benign-security-controls-v1.json");
            Files.createDirectories(output.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);
            System.out.printf("ticket-benign-security-controls: categories=%d/%d issueCues=%d/%d failedIds=%s output=%s%n",
                correct, results.size(), issueCuesPreserved, results.size(), failedIds, output);

            assertThat(results).allSatisfy(result -> {
                assertThat(result.categoryCorrect()).as("category for %s", result.id()).isTrue();
                assertThat(result.issueCuePreserved()).as("issue cue for %s", result.id()).isTrue();
            });
        }
    }

    @Test
    void expandedBenignSecurityVocabularyControlsPreserveThirtyIssuesAndCategories() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/eval/ticket-benign-security-controls-v2.json")) {
            assertThat(input).as("expanded synthetic control fixture").isNotNull();
            Dataset dataset = objectMapper.readValue(input, Dataset.class);
            assertThat(dataset.version()).isEqualTo("v2-benign-security-vocabulary-controls-30");
            assertThat(dataset.source()).contains("Synthetic");
            assertThat(dataset.cases()).hasSize(30);
            assertThat(dataset.cases().stream().map(Case::id).distinct().count()).isEqualTo(30);
            assertThat(dataset.cases().stream().collect(Collectors.groupingBy(Case::expected, Collectors.counting())))
                .containsEntry("账号问题", 6L)
                .containsEntry("权限问题", 6L)
                .containsEntry("系统故障", 6L)
                .containsEntry("数据问题", 6L)
                .containsEntry("流程咨询", 6L);

            try (InputStream v1Input = getClass().getResourceAsStream(FIXTURE)) {
                Dataset v1 = objectMapper.readValue(v1Input, Dataset.class);
                assertThat(dataset.cases().subList(0, v1.cases().size())).containsExactlyElementsOf(v1.cases());
            }

            Map<String, Map<String, Integer>> confusion = new LinkedHashMap<>();
            for (String expected : CATEGORIES) {
                Map<String, Integer> row = new LinkedHashMap<>();
                for (String predicted : CATEGORIES) {
                    row.put(predicted, 0);
                }
                confusion.put(expected, row);
            }
            List<CaseResult> results = new ArrayList<>();
            for (Case evalCase : dataset.cases()) {
                RuleClassificationResult prediction = service.classify(
                    evalCase.title(), evalCase.description(), evalCase.errorLog());
                String sanitizedText = String.join("\n",
                    service.stripExplicitInjectionSentences(evalCase.title()),
                    service.stripExplicitInjectionSentences(evalCase.description()),
                    service.stripExplicitInjectionSentences(evalCase.errorLog()));
                confusion.get(evalCase.expected()).compute(prediction.category(),
                    (key, count) -> count == null ? 1 : count + 1);
                results.add(new CaseResult(evalCase.id(), evalCase.expected(), prediction.category(),
                    evalCase.expected().equals(prediction.category()), evalCase.mustRetain(),
                    sanitizedText.contains(evalCase.mustRetain())));
            }

            long correct = results.stream().filter(CaseResult::categoryCorrect).count();
            long issueCuesPreserved = results.stream().filter(CaseResult::issueCuePreserved).count();
            List<String> failedIds = results.stream().filter(result -> !result.categoryCorrect() || !result.issueCuePreserved())
                .map(CaseResult::id).toList();
            EvaluationReport report = new EvaluationReport(dataset.version(), dataset.source(), "RULE_ONLY",
                results.size(), correct, issueCuesPreserved, confusion, results, failedIds,
                "Expanded fixed synthetic benign-context controls; not an LLM safety result or production performance claim.");
            Path reportDirectory = Path.of("target", "evaluation");
            Files.createDirectories(reportDirectory);
            Path output = Files.createTempFile(reportDirectory, "ticket-benign-security-controls-v2-", ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);
            System.out.printf("ticket-benign-security-controls-v2: categories=%d/%d issueCues=%d/%d failedIds=%s output=%s%n",
                correct, results.size(), issueCuesPreserved, results.size(), failedIds, output);

            assertThat(results).allSatisfy(result -> {
                assertThat(result.categoryCorrect()).as("category for %s", result.id()).isTrue();
                assertThat(result.issueCuePreserved()).as("issue cue for %s", result.id()).isTrue();
            });
        }
    }

    record Dataset(String version, String source, String purpose, List<Case> cases) { }

    record Case(String id, String title, String description, String errorLog, String expected, String mustRetain) { }

    record CaseResult(String id, String expected, String predicted, boolean categoryCorrect,
                      String mustRetain, boolean issueCuePreserved) { }

    record EvaluationReport(String datasetVersion, String dataSource, String mode, int sampleCount,
                            long categoryCorrectCount, long issueCuePreservedCount,
                            Map<String, Map<String, Integer>> confusionMatrix,
                            List<CaseResult> caseResults, List<String> failedCaseIds, String interpretation) { }
}
