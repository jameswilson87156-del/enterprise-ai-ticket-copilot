package com.enterpriseai.ticketcopilot.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.mapper.KnowledgeArticleMapper;
import org.springframework.stereotype.Service;

/**
 * 知识匹配：关键词 + 向量余弦相似度混合打分（各占 50%）。
 * 未配置 API Key 时向量通道不可用，自动退化为纯关键词模式（行为与升级前一致）。
 */
@Service
public class KnowledgeMatchingService {

    private final KnowledgeArticleMapper knowledgeArticleMapper;
    private final KnowledgeIndexService knowledgeIndexService;

    public KnowledgeMatchingService(KnowledgeArticleMapper knowledgeArticleMapper, KnowledgeIndexService knowledgeIndexService) {
        this.knowledgeArticleMapper = knowledgeArticleMapper;
        this.knowledgeIndexService = knowledgeIndexService;
    }

    public List<KnowledgeMatch> match(SupportTicket ticket, String category) {
        List<KnowledgeArticle> articles = knowledgeArticleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
        );
        String ticketText = normalize(ticket.getTitle() + " " + ticket.getDescription() + " " + ticket.getErrorLog() + " " + ticket.getSystemName());
        Map<String, Double> vectorHits = knowledgeIndexService.vectorAvailable()
            ? knowledgeIndexService.similaritySearch(ticketText, articles.size())
            : Map.of();
        return articles.stream()
            .map(article -> new KnowledgeMatch(article, relevance(article, category, ticketText, vectorHits)))
            .filter(match -> match.relevance() >= HybridKnowledgeScorer.RELEVANCE_CUTOFF)
            .sorted(Comparator.comparingInt(KnowledgeMatch::relevance).reversed())
            .limit(3)
            .toList();
    }

    /** 向量通道可用时走混合打分，否则纯关键词打分 */
    private int relevance(KnowledgeArticle article, String category, String ticketText, Map<String, Double> vectorHits) {
        int keywordScore = HybridKnowledgeScorer.keywordScore(article.getCategory(), article.getKeywords(), article.getTitle(), category, ticketText);
        if (vectorHits.isEmpty()) {
            return keywordScore;
        }
        double cosine = vectorHits.getOrDefault(article.getArticleNo(), 0.0);
        return HybridKnowledgeScorer.hybridRelevance(keywordScore, cosine);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
