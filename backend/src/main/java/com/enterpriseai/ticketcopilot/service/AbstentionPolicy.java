package com.enterpriseai.ticketcopilot.service;

import java.util.List;

import com.enterpriseai.ticketcopilot.model.AbstentionReasonCode;
import com.enterpriseai.ticketcopilot.model.RiskLevel;
import com.enterpriseai.ticketcopilot.model.StructuredCopilotOutput;
import org.springframework.stereotype.Service;

@Service
public class AbstentionPolicy {

    public StructuredCopilotOutput abstain(AbstentionReasonCode reasonCode) {
        AbstentionReasonCode safeReason = reasonCode == null ? AbstentionReasonCode.OUTPUT_POLICY_REJECTED : reasonCode;
        return new StructuredCopilotOutput(
            answerFor(safeReason),
            List.of(),
            RiskLevel.MEDIUM,
            true,
            missingInformationFor(safeReason),
            true,
            safeReason
        );
    }

    private String answerFor(AbstentionReasonCode reasonCode) {
        return switch (reasonCode) {
            case NO_RETRIEVAL_EVIDENCE -> "当前运行没有检索到可引用的知识库证据，系统不会生成无依据建议，请人工补充信息后复核。";
            case MISSING_CITATION -> "模型输出缺少本次运行允许的知识引用，系统未接受该建议，请人工复核。";
            case INVALID_CITATION -> "模型输出引用了本次运行未检索到的知识证据，系统未接受该建议，请人工复核。";
            case INVALID_STRUCTURED_OUTPUT -> "模型输出未满足结构化结果契约，系统未接受该内容，请人工复核。";
            case PROVIDER_FAILURE -> "Provider 调用未成功完成，系统未生成可自动采纳的建议，请人工复核。";
            case UNSUPPORTED_PROVIDER -> "当前 Provider 配置不在允许范围内，系统未生成建议，请人工复核。";
            case UNSUPPORTED_PROTOCOL -> "当前 Provider 协议不受本适配器支持，系统未生成建议，请人工复核。";
            case MISSING_REQUIRED_INFORMATION -> "当前信息不足以形成可靠建议，请补充必要上下文并由人工复核。";
            default -> "输出未通过安全策略校验，系统未接受该建议，请人工复核。";
        };
    }

    private List<String> missingInformationFor(AbstentionReasonCode reasonCode) {
        return switch (reasonCode) {
            case NO_RETRIEVAL_EVIDENCE -> List.of("缺少本次运行可引用的知识库证据");
            case MISSING_CITATION -> List.of("缺少模型引用的知识库 Citation ID");
            case INVALID_CITATION -> List.of("缺少与模型 Citation ID 匹配的本次检索快照");
            case INVALID_STRUCTURED_OUTPUT -> List.of("缺少符合契约的结构化 JSON 输出");
            case PROVIDER_FAILURE -> List.of("缺少可验证的 Provider 输出");
            default -> List.of("缺少可安全采纳建议所需的信息");
        };
    }
}
