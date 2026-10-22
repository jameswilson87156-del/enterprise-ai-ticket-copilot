package com.enterpriseai.ticketcopilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.enterpriseai.ticketcopilot.config.AiAvailability;
import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.mapper.KnowledgeArticleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ProviderFailureLoggingTest {

    private static final String SENSITIVE_PROVIDER_SENTINEL = "SENSITIVE_PROVIDER_RESPONSE_SENTINEL";

    @Test
    void classificationFallbackDoesNotLogProviderExceptionMessage(CapturedOutput output) {
        AiClassificationFacade facade = new AiClassificationFacade(
            new LlmClassificationService(prompt -> {
                throw new IllegalStateException(SENSITIVE_PROVIDER_SENTINEL);
            }, new ObjectMapper()),
            new RuleClassificationService(), new AiAvailability("local-test-key"));

        RuleClassificationResult result = facade.classify(ticket());

        assertThat(result.source()).isEqualTo(RuleClassificationResult.SOURCE_RULE);
        assertThat(output.getAll()).doesNotContain(SENSITIVE_PROVIDER_SENTINEL);
    }

    @Test
    void embeddingFallbackDoesNotLogProviderExceptionMessage(CapturedOutput output) {
        KnowledgeArticleMapper mapper = mock(KnowledgeArticleMapper.class);
        KnowledgeArticle article = new KnowledgeArticle();
        article.setArticleNo("KB-LOG-001");
        article.setTitle("OA 登录排查");
        article.setCategory("账号问题");
        article.setKeywords("账号,登录");
        article.setContent("排查步骤");
        article.setStatus("PUBLISHED");
        when(mapper.selectList(any())).thenReturn(List.of(article));

        EmbeddingModel failingEmbeddingModel = new EmbeddingModel() {
            @Override
            public float[] embed(Document document) {
                throw new IllegalStateException(SENSITIVE_PROVIDER_SENTINEL);
            }

            @Override
            public float[] embed(String text) {
                throw new IllegalStateException(SENSITIVE_PROVIDER_SENTINEL);
            }

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                throw new IllegalStateException(SENSITIVE_PROVIDER_SENTINEL);
            }
        };
        KnowledgeIndexService indexService = new KnowledgeIndexService(
            mapper, failingEmbeddingModel, new AiAvailability("local-test-key"), true);

        indexService.onApplicationReady();

        assertThat(indexService.vectorAvailable()).isFalse();
        assertThat(output.getAll()).doesNotContain(SENSITIVE_PROVIDER_SENTINEL);
    }

    private static SupportTicket ticket() {
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo("TCK-LOG-TEST");
        ticket.setTitle("OA 登录失败");
        ticket.setDescription("OA 登录失败");
        ticket.setErrorLog("");
        return ticket;
    }
}
