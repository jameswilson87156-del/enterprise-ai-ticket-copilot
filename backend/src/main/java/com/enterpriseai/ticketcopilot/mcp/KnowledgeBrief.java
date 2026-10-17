package com.enterpriseai.ticketcopilot.mcp;

/** MCP searchKnowledge 返回的知识条目摘要 */
public record KnowledgeBrief(
    String articleNo,
    String title,
    String category,
    int relevance,
    String keywords,
    String contentSnippet
) {
}
