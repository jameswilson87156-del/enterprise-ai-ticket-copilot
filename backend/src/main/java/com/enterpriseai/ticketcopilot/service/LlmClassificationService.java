package com.enterpriseai.ticketcopilot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

/**
 * LLM 智能分类：把工单上下文发给 qwen-plus，要求只输出约定 JSON，再解析为与规则引擎同构的结果。
 */
@Service
public class LlmClassificationService {

    /** 与规则引擎保持一致的候选类别 */
    static final Set<String> ALLOWED_CATEGORIES = Set.of("账号问题", "权限问题", "系统故障", "数据问题", "流程咨询");

    private static final String SYSTEM_PROMPT = """
        你是企业 IT 工单分类助手。请根据工单的标题、系统、优先级、描述和错误日志判断问题类别。
        类别只能从以下五类中选择一个：账号问题、权限问题、系统故障、数据问题、流程咨询。
        只输出一个 JSON 对象，不要输出任何解释、Markdown 或额外文本，格式如下：
        {"category":"类别","confidence":0.0,"reason":"判定原因","riskHint":"风险提示","suggestedSteps":["步骤1","步骤2"]}
        其中 confidence 取 0.0 到 1.0；suggestedSteps 给出 2 到 4 条可执行的排查步骤。""";

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public LlmClassificationService(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    /** 调用失败、超时或输出非法时直接抛出异常，由门面负责降级 */
    public RuleClassificationResult classify(SupportTicket ticket) {
        Prompt prompt = new Prompt(List.of(
            new SystemMessage(SYSTEM_PROMPT),
            new UserMessage(userContent(ticket))
        ));
        String raw = chatModel.call(prompt).getResult().getOutput().getText();
        return parse(raw);
    }

    /** 解析 LLM 输出（容忍代码块围栏与前后杂质），非法类别/字段视为失败 */
    RuleClassificationResult parse(String raw) {
        String json = extractJsonObject(raw);
        try {
            JsonNode node = objectMapper.readTree(json);
            String category = node.path("category").asText("").trim();
            if (!ALLOWED_CATEGORIES.contains(category)) {
                throw new IllegalArgumentException("LLM returned unknown category: " + category);
            }
            double confidence = clamp(node.path("confidence").asDouble(0.0), 0.0, 1.0);
            List<String> steps = new ArrayList<>();
            node.path("suggestedSteps").forEach(step -> {
                if (!step.asText("").isBlank()) {
                    steps.add(step.asText().trim());
                }
            });
            return new RuleClassificationResult(
                category,
                (int) Math.round(confidence * 100),
                node.path("reason").asText("").trim(),
                node.path("riskHint").asText("").trim(),
                List.copyOf(steps),
                RuleClassificationResult.SOURCE_LLM
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse LLM classification output: " + raw, e);
        }
    }

    private String userContent(SupportTicket ticket) {
        return "标题：" + safe(ticket.getTitle())
            + "\n系统：" + safe(ticket.getSystemName())
            + "\n优先级：" + safe(ticket.getUrgency())
            + "\n描述：" + safe(ticket.getDescription())
            + "\n错误日志：" + safe(ticket.getErrorLog());
    }

    /** 截取首个 '{' 到最后一个 '}' 之间的内容，剥离 Markdown 围栏等噪声 */
    static String extractJsonObject(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("LLM output is empty");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("LLM output contains no JSON object: " + raw);
        }
        return raw.substring(start, end + 1);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
