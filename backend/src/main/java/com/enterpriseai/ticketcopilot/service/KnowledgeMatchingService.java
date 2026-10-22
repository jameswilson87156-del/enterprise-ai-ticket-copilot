package com.enterpriseai.ticketcopilot.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

    /** 任意查询文本的知识检索（MCP searchKnowledge 使用）：类别通道不参与，仅关键词 + 向量 */
    public List<KnowledgeMatch> searchByQuery(String query, int limit) {
        return searchByQuery(query, limit, null);
    }

    /** 在指定的已发布知识集合内检索，供固定语料的离线评估使用 */
    public List<KnowledgeMatch> searchByQuery(String query, int limit, Collection<String> articleNos) {
        return searchByQuery(query, limit, articleNos, true);
    }

    /** 固定使用关键词评分，作为评测混合检索前的可复现基线 */
    public List<KnowledgeMatch> searchByQueryBaseline(String query, int limit, Collection<String> articleNos) {
        return searchByQuery(query, limit, articleNos, false);
    }

    /** Fail rather than silently shrinking the candidate set for a pinned offline evaluation. */
    public void requirePublishedArticles(Collection<String> articleNos) {
        if (articleNos == null || articleNos.isEmpty()) {
            throw new IllegalArgumentException("A pinned retrieval evaluation corpus is required");
        }
        List<KnowledgeArticle> published = knowledgeArticleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                .in(KnowledgeArticle::getArticleNo, articleNos)
        );
        Set<String> available = new HashSet<>();
        published.forEach(article -> available.add(article.getArticleNo()));
        Set<String> missing = new TreeSet<>(articleNos);
        missing.removeAll(available);
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                "Pinned retrieval evaluation articles are missing or unpublished: " + String.join(", ", missing));
        }
    }

    private List<KnowledgeMatch> searchByQuery(String query, int limit, Collection<String> articleNos, boolean useVector) {
        if (articleNos != null && articleNos.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<KnowledgeArticle> queryWrapper = new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getStatus, "PUBLISHED");
        if (articleNos != null) {
            queryWrapper.in(KnowledgeArticle::getArticleNo, articleNos);
        }
        List<KnowledgeArticle> articles = knowledgeArticleMapper.selectList(queryWrapper);
        String text = normalize(query);
        Map<String, Double> vectorHits = useVector && knowledgeIndexService.vectorAvailable()
            ? knowledgeIndexService.similaritySearch(text, articles.size())
            : Map.of();
        return articles.stream()
            .map(article -> new KnowledgeMatch(article, relevance(article, null, text, vectorHits)))
            .filter(match -> match.relevance() >= HybridKnowledgeScorer.RELEVANCE_CUTOFF)
            .sorted(Comparator.comparingInt(KnowledgeMatch::relevance).reversed()
                .thenComparing(match -> match.article().getArticleNo()))
            .limit(Math.max(1, limit))
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
