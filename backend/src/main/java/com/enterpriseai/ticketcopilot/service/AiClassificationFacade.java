package com.enterpriseai.ticketcopilot.service;

import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Keeps deterministic local classification available when the optional model path is off or fails. */
@Service
public class AiClassificationFacade {

    private static final Logger log = LoggerFactory.getLogger(AiClassificationFacade.class);

    private final AiProviderService aiProviderService;
    private final LlmClassificationService llmClassificationService;
    private final RuleClassificationService ruleClassificationService;

    public AiClassificationFacade(
        AiProviderService aiProviderService,
        LlmClassificationService llmClassificationService,
        RuleClassificationService ruleClassificationService
    ) {
        this.aiProviderService = aiProviderService;
        this.llmClassificationService = llmClassificationService;
        this.ruleClassificationService = ruleClassificationService;
    }

    public RuleClassificationResult classify(SupportTicket ticket) {
        if (!aiProviderService.classificationRequested()) {
            return classifyWithRules(ticket);
        }
        if (!aiProviderService.classificationEnabled()) {
            log.warn("LLM classification configuration is incomplete for ticket {}; using local rules", ticket.getTicketNo());
            return classifyWithRules(ticket, RuleClassificationResult.SOURCE_RULE_FALLBACK,
                "LLM 分类配置不完整，已回退本地规则");
        }
        try {
            return llmClassificationService.classify(ticket);
        } catch (RuntimeException exception) {
            log.warn("LLM classification failed for ticket {}; using local rules ({})",
                ticket.getTicketNo(), exception.getClass().getSimpleName());
            return classifyWithRules(ticket, RuleClassificationResult.SOURCE_RULE_FALLBACK,
                "LLM 分类未通过校验或请求失败，已回退本地规则");
        }
    }

    private RuleClassificationResult classifyWithRules(SupportTicket ticket) {
        return classifyWithRules(ticket, RuleClassificationResult.SOURCE_RULE, "");
    }

    private RuleClassificationResult classifyWithRules(SupportTicket ticket, String source, String reason) {
        RuleClassificationResult result = ruleClassificationService.classify(
            ticket.getTitle(), ticket.getDescription(), ticket.getErrorLog()
        );
        return new RuleClassificationResult(result.category(), result.confidence(), reason, source);
    }
}
