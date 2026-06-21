package com.enterpriseai.ticketcopilot.service;

import java.util.List;

import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.mapper.KnowledgeArticleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeMatchingServiceTest {

    @Mock
    private KnowledgeArticleMapper knowledgeArticleMapper;

    private KnowledgeMatchingService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeMatchingService(knowledgeArticleMapper);
    }

    @Test
    void ranksRelevantPublishedKnowledgeAndFiltersWeakMatches() {
        KnowledgeArticle primary = article("KB-API-500", "系统故障", "500,quote,error");
        KnowledgeArticle secondary = article("KB-QUOTE", "系统故障", "quote");
        KnowledgeArticle irrelevant = article("KB-REPORT", "数据问题", "报表,导出");
        when(knowledgeArticleMapper.selectList(any()))
            .thenReturn(List.of(secondary, irrelevant, primary));

        List<KnowledgeMatch> matches = service.match(ticket(), "系统故障");

        assertThat(matches)
            .extracting(match -> match.article().getArticleNo())
            .containsExactly("KB-API-500", "KB-QUOTE");
        assertThat(matches.get(0).relevance()).isGreaterThan(matches.get(1).relevance());
        verify(knowledgeArticleMapper).selectList(any());
    }

    @Test
    void returnsEmptyListWhenNoArticleReachesThreshold() {
        when(knowledgeArticleMapper.selectList(any()))
            .thenReturn(List.of(article("KB-UNRELATED", "流程咨询", "采购,审批")));

        assertThat(service.match(ticket(), "系统故障")).isEmpty();
    }

    private SupportTicket ticket() {
        SupportTicket ticket = new SupportTicket();
        ticket.setTitle("quote 接口返回 500");
        ticket.setDescription("报价失败");
        ticket.setErrorLog("HTTP 500 error");
        ticket.setSystemName("quote-center");
        return ticket;
    }

    private KnowledgeArticle article(String articleNo, String category, String keywords) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setArticleNo(articleNo);
        article.setTitle(articleNo + " 排查手册");
        article.setCategory(category);
        article.setKeywords(keywords);
        article.setStatus("PUBLISHED");
        return article;
    }
}
