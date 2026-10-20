package com.enterpriseai.ticketcopilot.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.service.LlmClassificationService;
import com.enterpriseai.ticketcopilot.service.RuleClassificationResult;
import com.enterpriseai.ticketcopilot.service.RuleClassificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Reproducible classification evaluation over a balanced synthetic set.
 * Rule evaluation is local; DeepSeek/DashScope comparison requires explicit opt-in
 * so a normal test run never sends cases to a paid external provider.
 */
class EvaluationTest {

    private static final String EVAL_FILE = "/eval/tickets-eval.json";
    private static final String DEEPSEEK_BASE_URL = "https://api.deepseek.com";
    private static final List<String> CATEGORIES = List.of("账号问题", "权限问题", "系统故障", "数据问题", "流程咨询");

    private final RuleClassificationService ruleService = new RuleClassificationService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ruleEngineEvaluation() throws Exception {
        EvalFile evalFile = loadEvalFile();
        List<EvalCase> cases = evalFile.cases();
        assertThat(evalFile.version()).isEqualTo("v2-balanced-40");
        assertThat(evalFile.source()).contains("synthetic");
        assertThat(cases).hasSize(40);
        assertThat(cases.stream().filter(EvalCase::ruleMissExpected).count()).isBetween(8L, 14L);

        Map<String, Long> classCounts = cases.stream()
            .collect(Collectors.groupingBy(EvalCase::expected, Collectors.counting()));
        for (String category : CATEGORIES) {
            assertThat(classCounts.getOrDefault(category, 0L))
                .as("评估集类别 %s 应均衡", category)
                .isEqualTo(8L);
        }

        Map<String, String> predictions = new LinkedHashMap<>();
        for (EvalCase evalCase : cases) {
            RuleClassificationResult result = ruleService.classify(evalCase.title(), evalCase.description(), evalCase.errorLog());
            predictions.put(evalCase.id(), result.category());
        }
        ClassificationMetrics metrics = calculateMetrics(cases, predictions);

        System.out.println("================ 工单规则分类评估报告 ================");
        printMetrics("rule-engine", metrics);
        System.out.println("=====================================================");
        writeReport("rule-engine-v2.json", new EvaluationReport(
            Instant.now().toString(), evalFile.version(), evalFile.source(),
            cases.size(), cases.size(), true, Map.of("rule-engine", metrics), null
        ));

        // The set is intentionally varied, but the baseline must remain useful enough to compare.
        assertThat(metrics.accuracy() * 100.0).isGreaterThanOrEqualTo(60.0);
    }

    @Test
    void llmComparisonEvaluation() throws Exception {
        assumeTrue(isEnabled("AI_LLM_EVAL_ENABLED"),
            "外部模型评测默认关闭；明确设置 AI_LLM_EVAL_ENABLED=true 后才会发送合成工单");

        ProviderSettings provider = resolveProviderSettings();
        assumeTrue(provider.apiKey() != null && !provider.apiKey().isBlank() && !provider.apiKey().startsWith("placeholder"),
            "未配置所选 Provider 的 API Key，跳过 DeepSeek/DashScope 对比评测");

        EvalFile evalFile = loadEvalFile();
        List<EvalCase> allCases = evalFile.cases();
        int limit = Integer.parseInt(envOrDefault("AI_LLM_EVAL_LIMIT", String.valueOf(allCases.size())));
        assertThat(limit).as("AI_LLM_EVAL_LIMIT").isBetween(1, allCases.size());
        List<EvalCase> cases = selectBalancedCases(allCases, limit);

        LlmClassificationService llmService = new LlmClassificationService(buildChatModel(provider), objectMapper);
        Map<String, String> llmPredictions = new LinkedHashMap<>();
        Map<String, String> rulePredictions = new LinkedHashMap<>();
        for (EvalCase evalCase : cases) {
            String llmCategory;
            try {
                llmCategory = llmService.classify(evalCase.toTicket()).category();
            } catch (Exception e) {
                llmCategory = "调用失败";
            }
            llmPredictions.put(evalCase.id(), llmCategory);
            rulePredictions.put(evalCase.id(), ruleService.classify(
                evalCase.title(), evalCase.description(), evalCase.errorLog()).category());
        }

        ClassificationMetrics llmMetrics = calculateMetrics(cases, llmPredictions);
        ClassificationMetrics ruleMetrics = calculateMetrics(cases, rulePredictions);
        Map<String, ClassificationMetrics> systems = new LinkedHashMap<>();
        systems.put(provider.name(), llmMetrics);
        systems.put("rule-engine", ruleMetrics);

        System.out.println("================ 模型与规则分类对比评估 ================");
        System.out.println("Provider: " + provider.name() + " | model: " + provider.model());
        System.out.println("评测样本: " + cases.size() + "/" + allCases.size());
        printMetrics(provider.name(), llmMetrics);
        printMetrics("rule-engine", ruleMetrics);
        System.out.println(String.format(Locale.ROOT, "Accuracy 差值（模型 - 规则）: %+.4f", llmMetrics.accuracy() - ruleMetrics.accuracy()));
        System.out.println("=====================================================");

        writeReport("model-vs-rule-v2.json", new EvaluationReport(
            Instant.now().toString(), evalFile.version(), evalFile.source(),
            allCases.size(), cases.size(), cases.size() == allCases.size(), systems,
            llmMetrics.accuracy() - ruleMetrics.accuracy()
        ));
    }

    private ProviderSettings resolveProviderSettings() {
        String selected = envOrDefault("AI_LLM_EVAL_PROVIDER",
            hasConfiguredValue("DEEPSEEK_API_KEY") ? "deepseek" : "dashscope").toLowerCase(Locale.ROOT);
        return switch (selected) {
            case "deepseek" -> new ProviderSettings(
                "DeepSeek",
                System.getenv("DEEPSEEK_API_KEY"),
                envOrDefault("AI_DEEPSEEK_BASE_URL", DEEPSEEK_BASE_URL),
                envOrDefault("AI_DEEPSEEK_CHAT_MODEL", "deepseek-flash")
            );
            case "dashscope", "qwen" -> new ProviderSettings(
                "DashScope",
                System.getenv("AI_DASHSCOPE_API_KEY"),
                envOrDefault("AI_DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
                envOrDefault("AI_DASHSCOPE_CHAT_MODEL", "qwen-plus")
            );
            default -> throw new IllegalArgumentException("AI_LLM_EVAL_PROVIDER must be deepseek or dashscope");
        };
    }

    private OpenAiChatModel buildChatModel(ProviderSettings provider) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        OpenAiApi openAiApi = OpenAiApi.builder()
            .baseUrl(provider.baseUrl())
            .apiKey(provider.apiKey())
            .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
            .build();
        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(org.springframework.ai.openai.OpenAiChatOptions.builder()
                .model(provider.model())
                .temperature(0.2)
                .build())
            .build();
    }

    private List<EvalCase> selectBalancedCases(List<EvalCase> cases, int limit) {
        if (limit == cases.size()) {
            return cases;
        }
        Map<String, List<EvalCase>> byCategory = new LinkedHashMap<>();
        CATEGORIES.forEach(category -> byCategory.put(category, new ArrayList<>()));
        for (EvalCase evalCase : cases) {
            byCategory.computeIfAbsent(evalCase.expected(), ignored -> new ArrayList<>()).add(evalCase);
        }
        List<EvalCase> selected = new ArrayList<>(limit);
        for (int offset = 0; selected.size() < limit; offset++) {
            for (String category : CATEGORIES) {
                List<EvalCase> bucket = byCategory.get(category);
                if (offset < bucket.size()) {
                    selected.add(bucket.get(offset));
                    if (selected.size() == limit) {
                        break;
                    }
                }
            }
        }
        return List.copyOf(selected);
    }

    private ClassificationMetrics calculateMetrics(List<EvalCase> cases, Map<String, String> predictions) {
        int correct = 0;
        int blindSpotCorrect = 0;
        int blindSpotTotal = 0;
        Map<String, Integer> truePositives = new LinkedHashMap<>();
        Map<String, Integer> predictedCounts = new LinkedHashMap<>();
        Map<String, Integer> supports = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> confusion = new LinkedHashMap<>();
        List<String> predictionLabels = new ArrayList<>(CATEGORIES);
        predictionLabels.add("调用失败");
        for (String category : CATEGORIES) {
            truePositives.put(category, 0);
            predictedCounts.put(category, 0);
            supports.put(category, 0);
            Map<String, Integer> row = new LinkedHashMap<>();
            predictionLabels.forEach(label -> row.put(label, 0));
            confusion.put(category, row);
        }
        List<CaseResult> details = new ArrayList<>();
        for (EvalCase evalCase : cases) {
            String expected = evalCase.expected();
            String predicted = predictions.getOrDefault(evalCase.id(), "调用失败");
            if (!predictionLabels.contains(predicted)) {
                predicted = "调用失败";
            }
            boolean hit = expected.equals(predicted);
            correct += hit ? 1 : 0;
            supports.computeIfPresent(expected, (ignored, count) -> count + 1);
            predictedCounts.computeIfPresent(predicted, (ignored, count) -> count + 1);
            if (hit) {
                truePositives.computeIfPresent(expected, (ignored, count) -> count + 1);
            }
            if (evalCase.ruleMissExpected()) {
                blindSpotTotal++;
                blindSpotCorrect += hit ? 1 : 0;
            }
            Map<String, Integer> row = confusion.get(expected);
            if (row != null) {
                row.computeIfPresent(predicted, (ignored, count) -> count + 1);
            }
            details.add(new CaseResult(evalCase.id(), expected, predicted, hit));
        }

        Map<String, CategoryMetrics> perCategory = new LinkedHashMap<>();
        double precisionTotal = 0.0;
        double recallTotal = 0.0;
        double f1Total = 0.0;
        for (String category : CATEGORIES) {
            int support = supports.get(category);
            int predicted = predictedCounts.get(category);
            int tp = truePositives.get(category);
            double precision = predicted == 0 ? 0.0 : (double) tp / predicted;
            double recall = support == 0 ? 0.0 : (double) tp / support;
            double f1 = precision + recall == 0.0 ? 0.0 : 2.0 * precision * recall / (precision + recall);
            perCategory.put(category, new CategoryMetrics(support, tp, precision, recall, f1));
            precisionTotal += precision;
            recallTotal += recall;
            f1Total += f1;
        }
        return new ClassificationMetrics(
            correct,
            cases.size(),
            cases.isEmpty() ? 0.0 : (double) correct / cases.size(),
            precisionTotal / CATEGORIES.size(),
            recallTotal / CATEGORIES.size(),
            f1Total / CATEGORIES.size(),
            blindSpotTotal == 0 ? 0.0 : (double) blindSpotCorrect / blindSpotTotal,
            perCategory,
            confusion,
            details
        );
    }

    private void printMetrics(String name, ClassificationMetrics metrics) {
        System.out.println(String.format(Locale.ROOT,
            "%s: accuracy=%.4f macroP=%.4f macroR=%.4f macroF1=%.4f rule-blindspot-accuracy=%.4f (%d/%d)",
            name, metrics.accuracy(), metrics.macroPrecision(), metrics.macroRecall(), metrics.macroF1(),
            metrics.ruleBlindSpotAccuracy(), metrics.correct(), metrics.total()));
        metrics.perCategory().forEach((category, value) -> System.out.println(String.format(Locale.ROOT,
            "  %-5s P=%.4f R=%.4f F1=%.4f support=%d correct=%d",
            category, value.precision(), value.recall(), value.f1(), value.support(), value.correct())));
        System.out.println("  confusion matrix (rows=expected, columns=predicted):");
        metrics.confusionMatrix().forEach((expected, row) -> System.out.println("    " + expected + " -> " + row));
    }

    private EvalFile loadEvalFile() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(EVAL_FILE)) {
            assertThat(input).as("评估集文件 %s 必须存在", EVAL_FILE).isNotNull();
            return objectMapper.readValue(input, EvalFile.class);
        }
    }

    private void writeReport(String fileName, EvaluationReport report) throws Exception {
        Path output = Path.of("target", "evaluation", fileName);
        Files.createDirectories(output.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);
        System.out.println("机器可读报告: " + output.toAbsolutePath());
    }

    private static boolean isEnabled(String name) {
        return "true".equalsIgnoreCase(System.getenv(name));
    }

    private static boolean hasConfiguredValue(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank() && !value.startsWith("placeholder");
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    record EvalCase(
        String id,
        String title,
        String description,
        String systemName,
        String errorLog,
        String urgency,
        String expected,
        boolean ruleMissExpected
    ) {
        SupportTicket toTicket() {
            SupportTicket ticket = new SupportTicket();
            ticket.setTicketNo("EVAL-" + id);
            ticket.setTitle(title);
            ticket.setDescription(description);
            ticket.setSystemName(systemName);
            ticket.setErrorLog(errorLog == null ? "" : errorLog);
            ticket.setUrgency(urgency == null ? "P2" : urgency);
            return ticket;
        }
    }

    record EvalFile(String version, String source, List<EvalCase> cases) {
    }

    record ProviderSettings(String name, String apiKey, String baseUrl, String model) {
    }

    record CategoryMetrics(int support, int correct, double precision, double recall, double f1) {
    }

    record CaseResult(String id, String expected, String predicted, boolean correct) {
    }

    record ClassificationMetrics(
        int correct,
        int total,
        double accuracy,
        double macroPrecision,
        double macroRecall,
        double macroF1,
        double ruleBlindSpotAccuracy,
        Map<String, CategoryMetrics> perCategory,
        Map<String, Map<String, Integer>> confusionMatrix,
        List<CaseResult> caseResults
    ) {
    }

    record EvaluationReport(
        String generatedAt,
        String datasetVersion,
        String dataSource,
        int datasetSize,
        int evaluatedCases,
        boolean fullDataset,
        Map<String, ClassificationMetrics> systems,
        Double accuracyDelta
    ) {
    }
}
