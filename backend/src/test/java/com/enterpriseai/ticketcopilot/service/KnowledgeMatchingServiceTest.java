package com.enterpriseai.ticketcopilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.enterpriseai.ticketcopilot.config.AiAvailability;
import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.mapper.KnowledgeArticleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * 混合召回集成测试：用确定性假向量模型（无网络）验证 关键词 / 向量 / 降级 三条路径。
 */
class KnowledgeMatchingServiceTest {

    @Test
    void degradesToPureKeywordWhenKeyMissing() {
        KnowledgeArticleMapper mapper = mock(KnowledgeArticleMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(article("KB-ACCOUNT-001", "账号登录失败与 MFA 重置处理流程", "账号问题", "账号,登录,密码,MFA,SSO,认证,锁定")));
        KnowledgeIndexService indexService = new KnowledgeIndexService(mapper, fakeEmbeddingModel(), new AiAvailability("placeholder-not-configured"), true);
        indexService.reindex();
        KnowledgeMatchingService service = new KnowledgeMatchingService(mapper, indexService);

        List<KnowledgeMatch> matches = service.match(ticket("账号 登录 失败", "账号问题"), "账号问题");
        assertThat(indexService.vectorAvailable()).isFalse();
        assertThat(matches).hasSize(1);
        // 类别 25 + 关键词(账号,登录) 36 = 61
        assertThat(matches.get(0).article().getArticleNo()).isEqualTo("KB-ACCOUNT-001");
        assertThat(matches.get(0).relevance()).isEqualTo(61);
    }

    @Test
    void hybridBlendsKeywordAndVectorChannels() {
        KnowledgeArticleMapper mapper = mock(KnowledgeArticleMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
            article("KB-ACCOUNT-001", "账号登录失败与 MFA 重置处理流程", "账号问题", "账号,登录,密码,MFA,SSO,认证,锁定"),
            article("KB-DATA-004", "数据同步异常与报表口径排查", "数据问题", "数据,同步,报表,不一致,缺失,导入,导出,指标")
        ));
        KnowledgeIndexService indexService = new KnowledgeIndexService(mapper, fakeEmbeddingModel(), new AiAvailability("sk-test"), true);
        indexService.reindex();
        KnowledgeMatchingService service = new KnowledgeMatchingService(mapper, indexService);

        List<KnowledgeMatch> matches = service.match(ticket("账号 登录 失败", "账号问题"), "账号问题");
        assertThat(indexService.vectorAvailable()).isTrue();
        assertThat(matches).hasSize(1);
        // 关键词 61，向量通道同域相似度 1.0 -> (0.61 + 1.0) / 2 = 80.5 -> 四舍五入 81
        assertThat(matches.get(0).article().getArticleNo()).isEqualTo("KB-ACCOUNT-001");
        assertThat(matches.get(0).relevance()).isEqualTo(81);
    }

    @Test
    void vectorChannelRecallsBeyondKeywords() {
        KnowledgeArticleMapper mapper = mock(KnowledgeArticleMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
            article("KB-ACCOUNT-001", "账号登录失败与 MFA 重置处理流程", "账号问题", "账号,登录,密码,MFA,SSO,认证,锁定")
        ));
        KnowledgeIndexService indexService = new KnowledgeIndexService(mapper, fakeEmbeddingModel(), new AiAvailability("sk-test"), true);
        indexService.reindex();
        KnowledgeMatchingService service = new KnowledgeMatchingService(mapper, indexService);

        // "签到入口进不去"：关键词全部未命中，但假向量模型把它映射到与账号知识相同的语义方向
        List<KnowledgeMatch> matches = service.match(ticket("签到入口进不去", "流程咨询"), "流程咨询");
        assertThat(matches).hasSize(1);
        // 关键词 0 + 向量 1.0 -> 50，仍高于 35 的截断线
        assertThat(matches.get(0).article().getArticleNo()).isEqualTo("KB-ACCOUNT-001");
        assertThat(matches.get(0).relevance()).isEqualTo(50);
    }

    @Test
    void skipsVectorIndexWhenEmbeddingIsDisabledForChatOnlyProvider() {
        KnowledgeArticleMapper mapper = mock(KnowledgeArticleMapper.class);
        KnowledgeIndexService indexService = new KnowledgeIndexService(
            mapper, fakeEmbeddingModel(), new AiAvailability("sk-test"), false);

        indexService.reindex();

        assertThat(indexService.vectorAvailable()).isFalse();
    }

    private static SupportTicket ticket(String title, String category) {
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo("TCK-KB-TEST");
        ticket.setTitle(title);
        ticket.setDescription(title);
        ticket.setErrorLog("");
        ticket.setSystemName("dummy-system");
        return ticket;
    }

    private static KnowledgeArticle article(String articleNo, String title, String category, String keywords) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setArticleNo(articleNo);
        article.setTitle(title);
        article.setCategory(category);
        article.setKeywords(keywords);
        article.setContent("演示内容 " + title);
        article.setStatus("PUBLISHED");
        return article;
    }

    /** 确定性假向量：账号/登录/签到域 -> [1,0,0]，数据/报表域 -> [0,1,0]，其余 -> [0,0,1] */
    private static EmbeddingModel fakeEmbeddingModel() {
        return new EmbeddingModel() {
            @Override
            public float[] embed(Document document) {
                return vector(document.getText());
            }

            @Override
            public float[] embed(String text) {
                return vector(text);
            }

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                throw new UnsupportedOperationException("not used in test");
            }

            private float[] vector(String text) {
                String lower = text == null ? "" : text.toLowerCase();
                if (lower.contains("账号") || lower.contains("登录") || lower.contains("签到") || lower.contains("mfa")) {
                    return new float[] {1f, 0f, 0f};
                }
                if (lower.contains("数据") || lower.contains("报表") || lower.contains("同步")) {
                    return new float[] {0f, 1f, 0f};
                }
                return new float[] {0f, 0f, 1f};
            }
        };
    }
}
