package com.enterpriseai.ticketcopilot.service;

import java.util.List;

/**
 * 分类结果统一载体：规则引擎与 LLM 输出同构，source 标注来源（LLM / RULE）。
 */
public record RuleClassificationResult(
    String category,
    int confidence,
    String reason,
    String riskHint,
    List<String> suggestedSteps,
    String source
) {

    public static final String SOURCE_RULE = "RULE";
    public static final String SOURCE_LLM = "LLM";

    /** 兼容原有两参构造：规则引擎路径无 reason/riskHint/建议步骤 */
    public RuleClassificationResult(String category, int confidence) {
        this(category, confidence, "", "", List.of(), SOURCE_RULE);
    }
}
