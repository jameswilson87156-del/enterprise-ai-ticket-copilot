package com.enterpriseai.ticketcopilot.service;

import java.util.List;

import com.enterpriseai.ticketcopilot.entity.RetrievalHit;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.model.AbstentionReasonCode;
import com.enterpriseai.ticketcopilot.model.RiskLevel;
import com.enterpriseai.ticketcopilot.model.StructuredCitation;
import com.enterpriseai.ticketcopilot.model.StructuredCopilotOutput;
import org.springframework.stereotype.Service;

@Service
public class LocalRuleStructuredOutputFactory {

    private final AbstentionPolicy abstentionPolicy;

    public LocalRuleStructuredOutputFactory(AbstentionPolicy abstentionPolicy) {
        this.abstentionPolicy = abstentionPolicy;
    }

    public StructuredCopilotOutput create(
        SupportTicket ticket,
        String classification,
        List<RetrievalHit> retrievalHits,
        RecommendationDraft draft
    ) {
        List<RetrievalHit> hits = retrievalHits == null ? List.of() : retrievalHits;
        if (hits.isEmpty()) {
            return abstentionPolicy.abstain(AbstentionReasonCode.NO_RETRIEVAL_EVIDENCE);
        }
        List<StructuredCitation> citations = hits.stream()
            .limit(StructuredOutputLimits.CITATION_MAX_COUNT)
            .map(hit -> new StructuredCitation(
                hit.getKnowledgeArticleNo(),
                "本地规则建议仅引用本次检索快照。",
                "LOCAL_RULE_RETRIEVAL_REFERENCE",
                truncate(hit.getExcerptSnapshot(), StructuredOutputLimits.CITATION_TEXT_MAX_LENGTH)
            ))
            .toList();
        RiskLevel riskLevel = riskLevel(ticket, draft);
        boolean modelReviewRequired = riskLevel == RiskLevel.HIGH || !draft.riskNotes().isEmpty();
        return new StructuredCopilotOutput(
            truncate(defaultText(draft.replySuggestion(), "已生成本地规则建议，等待人工复核。"), StructuredOutputLimits.ANSWER_MAX_LENGTH),
            citations,
            riskLevel,
            modelReviewRequired,
            missingInformation(ticket, classification),
            false,
            AbstentionReasonCode.NONE
        );
    }

    private RiskLevel riskLevel(SupportTicket ticket, RecommendationDraft draft) {
        String urgency = ticket == null ? "" : defaultText(ticket.getUrgency(), "");
        if ("P1".equalsIgnoreCase(urgency)) {
            return RiskLevel.HIGH;
        }
        if ("P2".equalsIgnoreCase(urgency) || (draft != null && !draft.riskNotes().isEmpty())) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private List<String> missingInformation(SupportTicket ticket, String classification) {
        if (ticket == null) {
            return List.of("缺少工单上下文");
        }
        if (ticket.getDescription() == null || ticket.getDescription().isBlank()) {
            return List.of("缺少工单问题描述");
        }
        if ("系统故障".equals(classification) && (ticket.getErrorLog() == null || ticket.getErrorLog().isBlank())) {
            return List.of("缺少错误日志或 traceId");
        }
        return List.of();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
