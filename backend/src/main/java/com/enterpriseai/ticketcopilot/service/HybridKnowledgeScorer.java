package com.enterpriseai.ticketcopilot.service;

import java.util.Arrays;
import java.util.Locale;

/**
 * 知识库混合召回打分纯函数：关键词命中（0-100）与向量余弦相似度（0-1）各占 50%。
 * 不依赖 Spring 与网络，便于单元测试。
 */
public final class HybridKnowledgeScorer {

    /** 向量通道参与门槛：余弦相似度低于该值视为未命中 */
    public static final double VECTOR_SIMILARITY_THRESHOLD = 0.35;
    /** 关键词与向量的权重各半 */
    private static final double KEYWORD_WEIGHT = 0.5;
    private static final double VECTOR_WEIGHT = 0.5;
    /** 输出相关性下限（与原有纯关键词模式一致） */
    public static final int RELEVANCE_CUTOFF = 35;

    private HybridKnowledgeScorer() {
    }

    /** 混合打分：0-100 关键词分 与 0-1 余弦相似度 加权求和后映射回 0-100 */
    public static int hybridRelevance(int keywordScore, double cosineSimilarity) {
        double keyword = clamp(keywordScore, 0, 100) / 100.0;
        double vector = clamp(cosineSimilarity, 0.0, 1.0);
        return (int) Math.round((KEYWORD_WEIGHT * keyword + VECTOR_WEIGHT * vector) * 100);
    }

    /** 向量通道阈值判断 */
    public static boolean meetsVectorThreshold(double cosineSimilarity) {
        return cosineSimilarity >= VECTOR_SIMILARITY_THRESHOLD;
    }

    /**
     * 关键词打分（沿用原 KnowledgeMatchingService 逻辑）：
     * 类别命中 +25，每个关键词命中 +18，标题前缀命中 +10，封顶 96。
     */
    public static int keywordScore(String articleCategory, String articleKeywords, String articleTitle, String ticketCategory, String ticketText) {
        String text = normalize(ticketText);
        int score = ticketCategory != null && ticketCategory.equals(articleCategory) ? 25 : 0;
        String keywords = articleKeywords == null ? "" : articleKeywords;
        for (String keyword : Arrays.stream(keywords.split("[,，;；\\s]+")).filter(item -> !item.isBlank()).toList()) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                score += 18;
            }
        }
        String title = normalize(articleTitle);
        if (!title.isBlank() && text.contains(title.substring(0, Math.min(title.length(), 4)))) {
            score += 10;
        }
        return Math.min(96, score);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
