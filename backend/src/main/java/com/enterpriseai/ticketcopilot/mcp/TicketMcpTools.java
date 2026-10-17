package com.enterpriseai.ticketcopilot.mcp;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.mapper.SupportTicketMapper;
import com.enterpriseai.ticketcopilot.service.KnowledgeMatch;
import com.enterpriseai.ticketcopilot.service.KnowledgeMatchingService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP 工具集：把工单检索与知识检索能力暴露给 Cherry Studio 等 AI 客户端。
 */
@Component
public class TicketMcpTools {

    private static final int TICKET_LIMIT = 10;
    private static final int KNOWLEDGE_LIMIT = 5;
    private static final int SNIPPET_LENGTH = 160;

    private final SupportTicketMapper supportTicketMapper;
    private final KnowledgeMatchingService knowledgeMatchingService;

    public TicketMcpTools(SupportTicketMapper supportTicketMapper, KnowledgeMatchingService knowledgeMatchingService) {
        this.supportTicketMapper = supportTicketMapper;
        this.knowledgeMatchingService = knowledgeMatchingService;
    }

    @Tool(description = "按状态和关键词检索工单列表（最多返回 10 条，按更新时间倒序）。status 与 keyword 均可留空；keyword 匹配标题、描述或系统名。")
    public List<TicketBrief> searchTickets(
        @ToolParam(description = "工单状态筛选，可选值：PENDING_CLASSIFICATION / PENDING_PROCESS / IN_PROGRESS / RESOLVED / KNOWLEDGE_BASED，留空查全部", required = false) String status,
        @ToolParam(description = "关键词，模糊匹配标题/描述/系统名，留空查全部", required = false) String keyword
    ) {
        LambdaQueryWrapper<SupportTicket> wrapper = new LambdaQueryWrapper<SupportTicket>()
            .orderByDesc(SupportTicket::getUpdatedAt)
            .last("limit " + TICKET_LIMIT);
        if (status != null && !status.isBlank()) {
            wrapper.eq(SupportTicket::getStatus, status.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = keyword.trim();
            wrapper.and(w -> w.like(SupportTicket::getTitle, like)
                .or().like(SupportTicket::getDescription, like)
                .or().like(SupportTicket::getSystemName, like));
        }
        return supportTicketMapper.selectList(wrapper).stream()
            .map(ticket -> new TicketBrief(
                ticket.getTicketNo(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getUrgency(),
                ticket.getCategory(),
                ticket.getSystemName(),
                ticket.getRequester()
            ))
            .toList();
    }

    @Tool(description = "知识库混合检索（关键词 + 向量语义，各占 50%），返回最相关的已发布知识条目（最多 5 条）。适用于根据问题描述查找处理指引。")
    public List<KnowledgeBrief> searchKnowledge(
        @ToolParam(description = "检索问题或关键词，例如：无法登录邮箱 / 报表数据不一致") String query
    ) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return knowledgeMatchingService.searchByQuery(query, KNOWLEDGE_LIMIT).stream()
            .map(match -> new KnowledgeBrief(
                match.article().getArticleNo(),
                match.article().getTitle(),
                match.article().getCategory(),
                match.relevance(),
                match.article().getKeywords(),
                snippet(match.article().getContent())
            ))
            .toList();
    }

    private static String snippet(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        return content.length() > SNIPPET_LENGTH ? content.substring(0, SNIPPET_LENGTH) + "..." : content;
    }
}
