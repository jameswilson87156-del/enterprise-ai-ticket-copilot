package com.enterpriseai.ticketcopilot.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.enterpriseai.ticketcopilot.config.AiAvailability;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * 分类门面降级策略测试：无 Key / LLM 异常 / LLM 正常三条路径。
 */
class AiClassificationFacadeTest {

    private final RuleClassificationService ruleService = new RuleClassificationService();

    @Test
    void fallsBackToRuleWhenKeyMissing() {
        AiClassificationFacade facade = new AiClassificationFacade(
            new LlmClassificationService(prompt -> {
                throw new IllegalStateException("should not be called");
            }, new ObjectMapper()),
            ruleService, new AiAvailability("placeholder-not-configured"));

        RuleClassificationResult result = facade.classify(ticket("登录失败 密码错误"));
        assertThat(result.source()).isEqualTo(RuleClassificationResult.SOURCE_RULE);
        assertThat(result.category()).isEqualTo("账号问题");
        assertThat(facade.llmEnabled()).isFalse();
    }

    @Test
    void fallsBackToRuleOnLlmFailure() {
        AiClassificationFacade facade = new AiClassificationFacade(
            new LlmClassificationService(prompt -> {
                throw new RuntimeException("read timeout after 15s");
            }, new ObjectMapper()),
            ruleService, new AiAvailability("sk-real-key"));

        RuleClassificationResult result = facade.classify(ticket("报表数据不一致"));
        assertThat(result.source()).isEqualTo(RuleClassificationResult.SOURCE_RULE);
        assertThat(result.category()).isEqualTo("数据问题");
    }

    @Test
    void usesLlmWhenAvailable() {
        AiClassificationFacade facade = new AiClassificationFacade(
            new LlmClassificationService(prompt -> chatResponse(
                "{\"category\":\"权限问题\",\"confidence\":0.92,\"reason\":\"403 且涉及共享盘授权\",\"riskHint\":\"涉及权限变更\",\"suggestedSteps\":[\"核对审批链路\"]}"),
                new ObjectMapper()),
            ruleService, new AiAvailability("sk-real-key"));

        RuleClassificationResult result = facade.classify(ticket("访问共享盘被拒绝 403"));
        assertThat(result.source()).isEqualTo(RuleClassificationResult.SOURCE_LLM);
        assertThat(result.category()).isEqualTo("权限问题");
        assertThat(result.reason()).contains("403");
        assertThat(facade.llmEnabled()).isTrue();
    }

    private static SupportTicket ticket(String description) {
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo("TCK-TEST-001");
        ticket.setTitle(description);
        ticket.setDescription(description);
        ticket.setErrorLog("");
        return ticket;
    }

    private static ChatResponse chatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
