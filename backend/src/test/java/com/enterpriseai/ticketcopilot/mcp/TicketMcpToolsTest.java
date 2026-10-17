package com.enterpriseai.ticketcopilot.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.mapper.SupportTicketMapper;
import com.enterpriseai.ticketcopilot.service.KnowledgeMatch;
import com.enterpriseai.ticketcopilot.service.KnowledgeMatchingService;
import org.junit.jupiter.api.Test;

/**
 * MCP 工具纯逻辑测试：不启动 MCP 服务器，不依赖网络。
 */
class TicketMcpToolsTest {

    @Test
    void searchTicketsMapsTicketBrief() {
        SupportTicketMapper mapper = mock(SupportTicketMapper.class);
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo("TCK-0001");
        ticket.setTitle("无法登录邮箱");
        ticket.setStatus("PENDING_PROCESS");
        ticket.setUrgency("P2");
        ticket.setCategory("账号问题");
        ticket.setSystemName("mail-system");
        ticket.setRequester("李航");
        when(mapper.selectList(any())).thenReturn(List.of(ticket));

        List<TicketBrief> results = new TicketMcpTools(mapper, mock(KnowledgeMatchingService.class))
            .searchTickets("", "");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).ticketNo()).isEqualTo("TCK-0001");
        assertThat(results.get(0).category()).isEqualTo("账号问题");
    }

    @Test
    void searchKnowledgeMapsBriefWithSnippet() {
        SupportTicketMapper mapper = mock(SupportTicketMapper.class);
        KnowledgeMatchingService matchingService = mock(KnowledgeMatchingService.class);
        KnowledgeArticle article = new KnowledgeArticle();
        article.setArticleNo("KB-ACCOUNT-001");
        article.setTitle("账号登录失败与 MFA 重置处理流程");
        article.setCategory("账号问题");
        article.setKeywords("账号,登录,MFA");
        article.setContent("核验员工身份后检查账号锁定状态、MFA 绑定和 SSO 审计日志。任何重置动作必须由人工确认。");
        when(matchingService.searchByQuery(eq("无法登录邮箱"), eq(5))).thenReturn(List.of(new KnowledgeMatch(article, 72)));

        List<KnowledgeBrief> results = new TicketMcpTools(mapper, matchingService).searchKnowledge("无法登录邮箱");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).articleNo()).isEqualTo("KB-ACCOUNT-001");
        assertThat(results.get(0).relevance()).isEqualTo(72);
        assertThat(results.get(0).contentSnippet()).contains("MFA");
    }

    @Test
    void searchKnowledgeWithBlankQueryReturnsEmpty() {
        TicketMcpTools tools = new TicketMcpTools(mock(SupportTicketMapper.class), mock(KnowledgeMatchingService.class));
        assertThat(tools.searchKnowledge("  ")).isEmpty();
    }
}
