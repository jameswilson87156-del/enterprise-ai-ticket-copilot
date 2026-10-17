package com.enterpriseai.ticketcopilot.service;

import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;

public record KnowledgeMatch(
    KnowledgeArticle article,
    int relevance
) {
}
