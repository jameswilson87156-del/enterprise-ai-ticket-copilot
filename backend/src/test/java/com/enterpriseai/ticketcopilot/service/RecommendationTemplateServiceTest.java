package com.enterpriseai.ticketcopilot.service;

import java.util.List;

import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationTemplateServiceTest {

    private final RecommendationTemplateService service = new RecommendationTemplateService();

    @Test
    void systemFaultRecommendationIncludesKnowledgeAndHumanBoundary() {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setArticleNo("KB-OPS-003");

        RecommendationDraft draft = service.generate(
            new SupportTicket(),
            "系统故障",
            List.of(new KnowledgeMatch(article, 88))
        );

        assertThat(draft.troubleshootingSteps())
            .anyMatch(step -> step.contains("发布、告警、错误率"))
            .anyMatch(step -> step.contains("KB-OPS-003"))
            .last()
            .isEqualTo("以上建议为规则生成草案，处理动作必须由人工确认。");
        assertThat(draft.replySuggestion()).contains("系统故障").contains("1 条知识库候选");
        assertThat(draft.riskNotes()).anyMatch(note -> note.contains("线上服务稳定性"));
    }

    @Test
    void accountRecommendationKeepsIdentityActionsManual() {
        RecommendationDraft draft = service.generate(new SupportTicket(), "账号问题", List.of());

        assertThat(draft.troubleshootingSteps()).anyMatch(step -> step.contains("MFA/SSO"));
        assertThat(draft.riskNotes()).anyMatch(note -> note.contains("身份与访问控制"));
        assertThat(draft.replySuggestion()).contains("0 条知识库候选");
    }
}
