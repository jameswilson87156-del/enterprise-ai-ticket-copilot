package com.enterpriseai.ticketcopilot.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.mapper.KnowledgeArticleMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeMatchingService {

    private final KnowledgeArticleMapper knowledgeArticleMapper;

    public KnowledgeMatchingService(KnowledgeArticleMapper knowledgeArticleMapper) {
        this.knowledgeArticleMapper = knowledgeArticleMapper;
    }

    public List<KnowledgeMatch> match(SupportTicket ticket, String category) {
        List<KnowledgeArticle> articles = knowledgeArticleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
        );
        String ticketText = normalize(ticket.getTitle() + " " + ticket.getDescription() + " " + ticket.getErrorLog() + " " + ticket.getSystemName());
        return articles.stream()
            .map(article -> new KnowledgeMatch(article, relevance(article, category, ticketText)))
            .filter(match -> match.relevance() >= 35)
            .sorted(Comparator.comparingInt(KnowledgeMatch::relevance).reversed())
            .limit(3)
            .toList();
    }

    private int relevance(KnowledgeArticle article, String category, String ticketText) {
        int score = category.equals(article.getCategory()) ? 25 : 0;
        String keywords = article.getKeywords() == null ? "" : article.getKeywords();
        for (String keyword : Arrays.stream(keywords.split("[,，;；\\s]+")).filter(item -> !item.isBlank()).toList()) {
            if (ticketText.contains(keyword.toLowerCase(Locale.ROOT))) {
                score += 18;
            }
        }
        String title = normalize(article.getTitle());
        if (!title.isBlank() && ticketText.contains(title.substring(0, Math.min(title.length(), 4)))) {
            score += 10;
        }
        return Math.min(96, score);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
