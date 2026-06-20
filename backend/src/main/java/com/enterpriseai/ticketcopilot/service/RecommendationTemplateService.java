package com.enterpriseai.ticketcopilot.service;

import java.util.ArrayList;
import java.util.List;

import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import org.springframework.stereotype.Service;

@Service
public class RecommendationTemplateService {

    public RecommendationDraft generate(SupportTicket ticket, String category, List<KnowledgeMatch> matches) {
        List<String> steps = new ArrayList<>();
        steps.add("复核工单描述、系统名称、错误日志与影响范围，确认是否需要升级处理。");
        switch (category) {
            case "账号问题" -> {
                steps.add("核对账号状态、最近登录失败原因、MFA/SSO 绑定状态。");
                steps.add("确认身份与审批记录后，由人工执行解锁或重置流程。");
            }
            case "权限问题" -> {
                steps.add("核验申请人与审批链路，确认最小权限角色。");
                steps.add("检查用户所在组、RBAC 策略和目标资源访问审计。");
            }
            case "系统故障" -> {
                steps.add("比对故障时间窗口内的发布、告警、错误率和依赖服务延迟。");
                steps.add("由值班工程师确认是否需要降级、扩容或回滚。");
            }
            case "数据问题" -> {
                steps.add("核对数据来源、同步批次、报表口径和最近变更。");
                steps.add("抽样比对原始记录与目标系统，确认是否存在一致性风险。");
            }
            default -> {
                steps.add("定位对应流程负责人和知识库指引。");
                steps.add("确认用户诉求是否需要审批、补充材料或跨团队协作。");
            }
        }
        if (!matches.isEmpty()) {
            steps.add("参考命中的知识库条目：" + matches.stream().map(match -> match.article().getArticleNo()).toList());
        }
        steps.add("以上建议为规则生成草案，处理动作必须由人工确认。");

        String reply = "已根据本地规则将该工单识别为「" + category + "」，并匹配到 "
            + matches.size() + " 条知识库候选。支持人员将先进行人工确认，再执行后续处理动作。";

        List<String> risks = new ArrayList<>();
        risks.add("规则建议不等同于最终处理结论，必须由人工确认。");
        risks.add("涉及权限、生产配置、数据修复或客户影响的动作不得自动执行。");
        if ("系统故障".equals(category)) {
            risks.add("可能影响线上服务稳定性，需检查发布窗口和回滚风险。");
        }
        if ("权限问题".equals(category) || "账号问题".equals(category)) {
            risks.add("涉及身份与访问控制，需保留审批和审计记录。");
        }
        return new RecommendationDraft(steps, reply, risks);
    }
}
