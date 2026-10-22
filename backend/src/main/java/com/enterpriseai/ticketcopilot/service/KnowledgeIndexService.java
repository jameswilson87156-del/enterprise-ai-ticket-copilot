package com.enterpriseai.ticketcopilot.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterpriseai.ticketcopilot.config.AiAvailability;
import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;
import com.enterpriseai.ticketcopilot.mapper.KnowledgeArticleMapper;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 知识库向量索引：启动时把已发布知识全量向量化写入 SimpleVectorStore（RAG-lite）。
 * 未配置 API Key 时索引保持为空，检索自动退化为纯关键词。
 */
@Service
public class KnowledgeIndexService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexService.class);
    /** 单条知识参与向量化的内容长度上限，避免长文超额消耗 token */
    private static final int MAX_CONTENT_LENGTH = 600;

    private final KnowledgeArticleMapper knowledgeArticleMapper;
    private final EmbeddingModel embeddingModel;
    private final AiAvailability aiAvailability;
    private final boolean embeddingEnabled;

    private volatile SimpleVectorStore vectorStore;

    public KnowledgeIndexService(
        KnowledgeArticleMapper knowledgeArticleMapper,
        EmbeddingModel embeddingModel,
        AiAvailability aiAvailability,
        @Value("${ticket-copilot.ai.embedding-enabled:true}") boolean embeddingEnabled
    ) {
        this.knowledgeArticleMapper = knowledgeArticleMapper;
        this.embeddingModel = embeddingModel;
        this.aiAvailability = aiAvailability;
        this.embeddingEnabled = embeddingEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            reindex();
        } catch (Exception e) {
            // 索引失败不阻断启动，检索自动退化为纯关键词
            log.warn("知识库向量索引初始化失败，检索退化为纯关键词（{}）", e.getClass().getSimpleName());
        }
    }

    /** 全量重建向量索引；未配置 Key 或知识为空时只清空索引 */
    public synchronized void reindex() {
        if (!embeddingEnabled) {
            log.info("知识库向量索引已关闭（关键词检索不受影响）");
            this.vectorStore = null;
            return;
        }
        if (!aiAvailability.llmConfigured()) {
            log.info("模型 API Key 未配置，跳过知识库向量索引（关键词检索不受影响）");
            this.vectorStore = null;
            return;
        }
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        List<KnowledgeArticle> articles = knowledgeArticleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getStatus, "PUBLISHED"));
        for (KnowledgeArticle article : articles) {
            store.add(List.of(new Document(articleText(article), Map.of(
                "articleNo", article.getArticleNo(),
                "category", article.getCategory() == null ? "" : article.getCategory()
            ))));
        }
        this.vectorStore = store;
        log.info("知识库向量索引完成，共向量化 {} 条已发布知识", articles.size());
    }

    /** 向量通道是否可用（已配置 Key 且索引非空） */
    public boolean vectorAvailable() {
        return embeddingEnabled && vectorStore != null && aiAvailability.llmConfigured();
    }

    /** 余弦相似度检索：返回 articleNo -> 相似度（仅保留达到 0.35 阈值的结果） */
    public Map<String, Double> similaritySearch(String query, int topK) {
        SimpleVectorStore store = this.vectorStore;
        if (store == null || query == null || query.isBlank()) {
            return Map.of();
        }
        Map<String, Double> hits = new HashMap<>();
        for (Document document : store.similaritySearch(SearchRequest.builder()
            .query(query)
            .topK(Math.max(1, topK))
            .similarityThreshold(HybridKnowledgeScorer.VECTOR_SIMILARITY_THRESHOLD)
            .build())) {
            Object articleNo = document.getMetadata().get("articleNo");
            double score = document.getScore() == null ? 0.0 : document.getScore();
            if (articleNo != null && HybridKnowledgeScorer.meetsVectorThreshold(score)) {
                hits.put(String.valueOf(articleNo), score);
            }
        }
        return hits;
    }

    private String articleText(KnowledgeArticle article) {
        String content = article.getContent() == null ? "" : article.getContent();
        String truncated = content.length() > MAX_CONTENT_LENGTH ? content.substring(0, MAX_CONTENT_LENGTH) : content;
        return "标题：" + safe(article.getTitle())
            + "\n类别：" + safe(article.getCategory())
            + "\n关键词：" + safe(article.getKeywords())
            + "\n内容：" + truncated;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
