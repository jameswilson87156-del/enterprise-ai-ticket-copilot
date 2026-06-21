package com.enterpriseai.ticketcopilot.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleClassificationServiceTest {

    private final RuleClassificationService service = new RuleClassificationService();

    @Test
    void classifiesSystemFaultAndCapsConfidence() {
        RuleClassificationResult result = service.classify(
            "报价接口返回 500",
            "服务故障并持续失败",
            "timeout error"
        );

        assertThat(result.category()).isEqualTo("系统故障");
        assertThat(result.confidence()).isEqualTo(92);
    }

    @Test
    void prefersAccountRulesWhenAccountKeywordsAreStronger() {
        RuleClassificationResult result = service.classify(
            "员工无法登录 SSO",
            "账号被锁定，需要检查 MFA",
            "authentication failed"
        );

        assertThat(result.category()).isEqualTo("账号问题");
        assertThat(result.confidence()).isGreaterThanOrEqualTo(80);
    }

    @Test
    void fallsBackToProcessConsultationWithBaselineConfidence() {
        RuleClassificationResult result = service.classify("会议室位置", "请告知位置", "");

        assertThat(result.category()).isEqualTo("流程咨询");
        assertThat(result.confidence()).isEqualTo(58);
    }
}
