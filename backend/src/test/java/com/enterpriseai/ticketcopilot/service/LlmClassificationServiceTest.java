package com.enterpriseai.ticketcopilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * LLM 分类解析纯逻辑测试：不依赖网络与真实模型。
 */
class LlmClassificationServiceTest {

    private final LlmClassificationService service =
        new LlmClassificationService(prompt -> chatResponse("{\"category\":\"账号问题\",\"confidence\":0.87,\"reason\":\"描述指向登录失败\",\"riskHint\":\"涉及账号解锁需审批\",\"suggestedSteps\":[\"核对登录日志\",\"检查 MFA 绑定\"]}"), new ObjectMapper());

    @Test
    void parsePlainJson() {
        RuleClassificationResult result = service.parse("{\"category\":\"数据问题\",\"confidence\":0.66,\"reason\":\"报表口径不一致\",\"riskHint\":\"\",\"suggestedSteps\":[]}");
        assertThat(result.category()).isEqualTo("数据问题");
        assertThat(result.confidence()).isEqualTo(66);
        assertThat(result.source()).isEqualTo(RuleClassificationResult.SOURCE_LLM);
        assertThat(result.suggestedSteps()).isEmpty();
    }

    @Test
    void parseToleratesMarkdownFenceAndNoise() {
        String raw = "好的，以下是分类结果：\n```json\n{\"category\":\"系统故障\",\"confidence\":1.0,\"reason\":\"502\",\"riskHint\":\"\",\"suggestedSteps\":[\"查网关\"]}\n```";
        RuleClassificationResult result = service.parse(raw);
        assertThat(result.category()).isEqualTo("系统故障");
        assertThat(result.confidence()).isEqualTo(100);
    }

    @Test
    void parseRejectsUnknownCategory() {
        assertThatThrownBy(() -> service.parse("{\"category\":\"魔法类别\",\"confidence\":0.9}"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseRejectsGarbage() {
        assertThatThrownBy(() -> service.parse("抱歉我无法分类")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.parse("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseClampsConfidenceRange() {
        RuleClassificationResult result = service.parse("{\"category\":\"流程咨询\",\"confidence\":7.5,\"reason\":\"\",\"riskHint\":\"\",\"suggestedSteps\":[]}");
        assertThat(result.confidence()).isEqualTo(100);
    }

    @Test
    void classifyBuildsPromptAndParsesResult() {
        SupportTicket ticket = new SupportTicket();
        ticket.setTitle("无法登录邮箱");
        ticket.setDescription("输入密码后提示认证失败");
        ticket.setSystemName("mail");
        ticket.setUrgency("P2");
        ticket.setErrorLog("auth failure x3");
        RuleClassificationResult result = service.classify(ticket);
        assertThat(result.category()).isEqualTo("账号问题");
        assertThat(result.confidence()).isEqualTo(87);
        assertThat(result.suggestedSteps()).hasSize(2);
        assertThat(result.source()).isEqualTo(RuleClassificationResult.SOURCE_LLM);
    }

    private static ChatResponse chatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
