package com.enterpriseai.ticketcopilot.service;

import com.enterpriseai.ticketcopilot.config.AiAvailability;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 分类门面：优先走 LLM，未配置 Key / 调用异常 / 超时时自动降级规则引擎，并标注来源。
 */
@Service
public class AiClassificationFacade {

    private static final Logger log = LoggerFactory.getLogger(AiClassificationFacade.class);

    private final LlmClassificationService llmClassificationService;
    private final RuleClassificationService ruleClassificationService;
    private final AiAvailability aiAvailability;

    public AiClassificationFacade(
        LlmClassificationService llmClassificationService,
        RuleClassificationService ruleClassificationService,
        AiAvailability aiAvailability
    ) {
        this.llmClassificationService = llmClassificationService;
        this.ruleClassificationService = ruleClassificationService;
        this.aiAvailability = aiAvailability;
    }

    public RuleClassificationResult classify(SupportTicket ticket) {
        if (!aiAvailability.llmConfigured()) {
            log.warn("模型 API Key 未配置，工单 {} 使用规则引擎分类", ticket.getTicketNo());
            return ruleClassificationService.classify(ticket.getTitle(), ticket.getDescription(), ticket.getErrorLog());
        }
        try {
            return llmClassificationService.classify(ticket);
        } catch (Exception e) {
            log.warn("LLM 分类失败，工单 {} 降级为规则引擎分类：{}", ticket.getTicketNo(), e.getMessage());
            return ruleClassificationService.classify(ticket.getTitle(), ticket.getDescription(), ticket.getErrorLog());
        }
    }

    public boolean llmEnabled() {
        return aiAvailability.llmConfigured();
    }
}
