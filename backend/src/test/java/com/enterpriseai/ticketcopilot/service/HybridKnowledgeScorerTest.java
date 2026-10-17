package com.enterpriseai.ticketcopilot.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 混合打分纯函数测试：不依赖网络与向量服务。
 */
class HybridKnowledgeScorerTest {

    @Test
    void hybridBlendIsFiftyFifty() {
        assertThat(HybridKnowledgeScorer.hybridRelevance(80, 0.6)).isEqualTo(70);
        assertThat(HybridKnowledgeScorer.hybridRelevance(0, 1.0)).isEqualTo(50);
        assertThat(HybridKnowledgeScorer.hybridRelevance(96, 0.0)).isEqualTo(48);
        assertThat(HybridKnowledgeScorer.hybridRelevance(50, 0.5)).isEqualTo(50);
    }

    @Test
    void hybridClampsOutOfRangeInputs() {
        assertThat(HybridKnowledgeScorer.hybridRelevance(200, 2.0)).isEqualTo(100);
        assertThat(HybridKnowledgeScorer.hybridRelevance(-10, -1.0)).isEqualTo(0);
    }

    @Test
    void vectorThresholdIsInclusiveAtPointThreeFive() {
        assertThat(HybridKnowledgeScorer.meetsVectorThreshold(0.35)).isTrue();
        assertThat(HybridKnowledgeScorer.meetsVectorThreshold(0.9)).isTrue();
        assertThat(HybridKnowledgeScorer.meetsVectorThreshold(0.3499)).isFalse();
        assertThat(HybridKnowledgeScorer.meetsVectorThreshold(0.0)).isFalse();
    }

    @Test
    void keywordScoreKeepsLegacySemantics() {
        // 类别命中 +25
        assertThat(HybridKnowledgeScorer.keywordScore("账号问题", "", "标题", "账号问题", "任意文本")).isEqualTo(25);
        // 类别 +25、关键词"登录" +18、标题前缀 +10
        assertThat(HybridKnowledgeScorer.keywordScore("账号问题", "登录,密码", "账号登录失败", "账号问题", "无法登录 账号登录失败")).isEqualTo(53);
        // 关键词大小写不敏感
        assertThat(HybridKnowledgeScorer.keywordScore("系统故障", "Timeout", "手册", "系统故障", "request timeout after 30s")).isEqualTo(43);
        // 封顶 96：25 + 18*5 = 115 -> 96
        assertThat(HybridKnowledgeScorer.keywordScore("账号问题", "账号,登录,密码,MFA,SSO", "t", "账号问题", "账号 登录 密码 mfa sso")).isEqualTo(96);
        // 完全未命中为 0
        assertThat(HybridKnowledgeScorer.keywordScore("数据问题", "同步,报表", "数据手册", "账号问题", "无法登录")).isEqualTo(0);
    }
}
