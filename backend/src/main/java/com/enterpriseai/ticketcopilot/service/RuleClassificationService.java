package com.enterpriseai.ticketcopilot.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class RuleClassificationService {

    private static final List<RuleBucket> RULES = List.of(
        new RuleBucket("账号问题", List.of("账号", "登录", "登陆", "密码", "mfa", "sso", "锁定", "无法登录", "认证")),
        new RuleBucket("权限问题", List.of("权限", "授权", "访问", "审批", "角色", "共享盘", "rbac", "permission", "forbidden")),
        new RuleBucket("系统故障", List.of("报错", "故障", "502", "500", "timeout", "超时", "异常", "不可用", "宕机", "失败", "error")),
        new RuleBucket("数据问题", List.of("数据", "同步", "缺失", "重复", "不一致", "报表", "指标", "导入", "导出")),
        new RuleBucket("流程咨询", List.of("流程", "如何", "怎么", "咨询", "申请", "规范", "步骤", "指引"))
    );

    public RuleClassificationResult classify(String title, String description, String errorLog) {
        String text = normalize(title + " " + description + " " + errorLog);
        String bestCategory = "流程咨询";
        int bestScore = 0;
        for (RuleBucket rule : RULES) {
            int score = 0;
            for (String keyword : rule.keywords()) {
                if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestCategory = rule.category();
            }
        }
        int confidence = Math.min(92, 58 + bestScore * 11);
        return new RuleClassificationResult(bestCategory, confidence);
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private record RuleBucket(String category, List<String> keywords) {
    }
}
