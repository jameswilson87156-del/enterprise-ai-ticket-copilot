package com.enterpriseai.ticketcopilot.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
 * 可复现的分类效果评估入口：
 * 1) 规则引擎评估：不依赖 API key，输出 accuracy 与分类别统计报告；
 * 2) LLM 对比评估：仅在环境变量 AI_DASHSCOPE_API_KEY 存在时执行（assumeTrue 跳过）。
 */
class EvaluationTest {

    private static final String EVAL_FILE = "/eval/tickets-eval.json";

    private final RuleClassificationService ruleService = new RuleClassificationService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ruleEngineEvaluation() throws Exception {
        List<EvalCase> cases = loadCases();
        assertThat(cases.size()).isGreaterThanOrEqualTo(12);
        assertThat(cases.stream().filter(EvalCase::ruleMissExpected).count()).isBetween(3L, 5L);

        int correct = 0;
        Map<String, int[]> byCategory = new LinkedHashMap<>();
        List<String> details = new ArrayList<>();
        for (EvalCase evalCase : cases) {
            RuleClassificationResult result = ruleService.classify(evalCase.title(), evalCase.description(), evalCase.errorLog());
            boolean hit = result.category().equals(evalCase.expected());
            if (hit) {
                correct++;
            }
            byCategory.computeIfAbsent(evalCase.expected(), key -> new int[2]);
            byCategory.get(evalCase.expected())[0] += hit ? 1 : 0;
            byCategory.get(evalCase.expected())[1] += 1;
            details.add(String.format(Locale.ROOT, "  %-4s | 期望 %-5s | 规则 %-5s | %s%s",
                evalCase.id(), evalCase.expected(), result.category(), hit ? "命中" : "MISS",
                evalCase.ruleMissExpected() && !hit ? "（预埋的规则盲区样本）" : ""));
        }
        double accuracy = correct * 100.0 / cases.size();

        System.out.println("================ 工单规则分类评估报告 ================");
        System.out.println("样本总数: " + cases.size() + "（含 " + cases.stream().filter(EvalCase::ruleMissExpected).count() + " 条预埋规则盲区样本）");
        System.out.println("规则引擎 accuracy: " + String.format(Locale.ROOT, "%.1f%% (%d/%d)", accuracy, correct, cases.size()));
        System.out.println("分类别命中：");
        byCategory.forEach((category, count) ->
            System.out.println(String.format(Locale.ROOT, "  %-5s %d/%d", category, count[0], count[1])));
        System.out.println("逐条明细：");
        details.forEach(System.out::println);
        System.out.println("=====================================================");

        // 规则基线：允许低于 100%，但不应低于 60%（否则评估集或规则被意外改动）
        assertThat(accuracy).isGreaterThanOrEqualTo(60.0);
    }

    @Test
    void llmComparisonEvaluation() throws Exception {
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("placeholder"),
            "未配置 AI_DASHSCOPE_API_KEY，跳过 LLM 对比评估");

        List<EvalCase> cases = loadCases();
        LlmClassificationService llmService = new LlmClassificationService(buildChatModel(apiKey), objectMapper);

        int llmCorrect = 0;
        int ruleCorrect = 0;
        List<String> details = new ArrayList<>();
        for (EvalCase evalCase : cases) {
            SupportTicket ticket = evalCase.toTicket();
            String llmCategory;
            try {
                llmCategory = llmService.classify(ticket).category();
            } catch (Exception e) {
                llmCategory = "调用失败";
            }
            String ruleCategory = ruleService.classify(evalCase.title(), evalCase.description(), evalCase.errorLog()).category();
            boolean llmHit = llmCategory.equals(evalCase.expected());
            boolean ruleHit = ruleCategory.equals(evalCase.expected());
            llmCorrect += llmHit ? 1 : 0;
            ruleCorrect += ruleHit ? 1 : 0;
            details.add(String.format(Locale.ROOT, "  %-4s | 期望 %-5s | LLM %-5s | 规则 %-5s | %s",
                evalCase.id(), evalCase.expected(), llmCategory, ruleCategory,
                llmHit ? "LLM 命中" : (ruleHit ? "仅规则命中" : "双 MISS")));
        }
        System.out.println("================ LLM 对比评估报告（qwen-plus）================");
        System.out.println(String.format(Locale.ROOT, "LLM accuracy:    %.1f%% (%d/%d)", llmCorrect * 100.0 / cases.size(), llmCorrect, cases.size()));
        System.out.println(String.format(Locale.ROOT, "规则 accuracy:   %.1f%% (%d/%d)", ruleCorrect * 100.0 / cases.size(), ruleCorrect, cases.size()));
        System.out.println("逐条明细：");
        details.forEach(System.out::println);
        System.out.println("=====================================================");
    }

    /** 按生产同款超时（15s 只读）手工构建 OpenAiChatModel，避免拉起 Spring 上下文 */
    private OpenAiChatModel buildChatModel(String apiKey) {
        String baseUrl = envOrDefault("AI_DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1");
        String model = envOrDefault("AI_DASHSCOPE_CHAT_MODEL", "qwen-plus");
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        OpenAiApi openAiApi = OpenAiApi.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
            .build();
        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(org.springframework.ai.openai.OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.2)
                .build())
            .build();
    }

    private List<EvalCase> loadCases() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(EVAL_FILE)) {
            assertThat(input).as("评估集文件 %s 必须存在", EVAL_FILE).isNotNull();
            return objectMapper.readValue(input, EvalFile.class).cases();
        }
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

    record EvalFile(List<EvalCase> cases) {
    }
}
