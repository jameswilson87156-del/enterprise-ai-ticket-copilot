package com.enterpriseai.ticketcopilot.service;

import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiClassificationFacadeTest {

    private AiProviderService provider;
    private LlmClassificationService llm;
    private RuleClassificationService rules;
    private AiClassificationFacade facade;

    @BeforeEach
    void setUp() {
        provider = mock(AiProviderService.class);
        llm = mock(LlmClassificationService.class);
        rules = new RuleClassificationService();
        facade = new AiClassificationFacade(provider, llm, rules);
    }

    @Test
    void remainsRuleOnlyWhenClassificationIsDisabled() {
        when(provider.classificationEnabled()).thenReturn(false);

        RuleClassificationResult result = facade.classify(ticket("OA 登录失败，认证返回 401"));

        assertThat(result.source()).isEqualTo(RuleClassificationResult.SOURCE_RULE);
        assertThat(result.category()).isEqualTo("账号问题");
        verify(llm, never()).classify(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void usesLlmResultWhenExplicitlyEnabled() {
        when(provider.classificationRequested()).thenReturn(true);
        when(provider.classificationEnabled()).thenReturn(true);
        when(llm.classify(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new RuleClassificationResult("账号问题", 91, "认证失败", RuleClassificationResult.SOURCE_LLM));

        RuleClassificationResult result = facade.classify(ticket("synthetic"));

        assertThat(result.source()).isEqualTo(RuleClassificationResult.SOURCE_LLM);
        assertThat(result.confidence()).isEqualTo(91);
    }

    @Test
    void fallsBackToRuleAndDoesNotExposeProviderErrorText() {
        when(provider.classificationRequested()).thenReturn(true);
        when(provider.classificationEnabled()).thenReturn(true);
        when(llm.classify(org.mockito.ArgumentMatchers.any()))
            .thenThrow(new IllegalStateException("Authorization: synthetic-secret"));

        RuleClassificationResult result = facade.classify(ticket("OA 登录失败"));

        assertThat(result.source()).isEqualTo(RuleClassificationResult.SOURCE_RULE_FALLBACK);
        assertThat(result.category()).isEqualTo("账号问题");
        verify(llm).classify(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordsFallbackWhenClassificationWasRequestedButProviderConfigurationIsIncomplete() {
        when(provider.classificationRequested()).thenReturn(true);
        when(provider.classificationEnabled()).thenReturn(false);

        RuleClassificationResult result = facade.classify(ticket("OA 登录失败"));

        assertThat(result.source()).isEqualTo(RuleClassificationResult.SOURCE_RULE_FALLBACK);
        assertThat(result.reason()).contains("配置不完整");
        verify(llm, never()).classify(org.mockito.ArgumentMatchers.any());
    }

    private SupportTicket ticket(String description) {
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo("TCK-SYNTHETIC-1");
        ticket.setTitle("synthetic ticket");
        ticket.setDescription(description);
        ticket.setErrorLog("");
        return ticket;
    }
}
