# Ticket Copilot 红队与评测补充（2026-10-22）

## 范围和结果

- 新增独立版本化数据集 `backend/src/test/resources/eval/tickets-redteam-v1.json`：25 条双语合成攻击样本，五类工单各 5 条；覆盖提示覆盖、提示提取、密钥提取、角色冒充和危险操作指令。
- 样本以已有合成工单为底稿叠加攻击语句，原有 `tickets-eval.json` 40 条普通分类样本和标签未改动。
- 本地规则引擎在该窄范围数据集上的离线分类结果为 25/25，逐类支持数均为 5。该分数只说明当前规则和这 25 条固定样本的匹配，不是 LLM 安全率，也不代表生产工单效果。
- 红队外部模型评测未运行：本轮真实模型请求为 0。报告代码已支持外部模型结果按样本记录类别正确性、禁止标记命中和 Provider/解析失败；只有类别正确、调用/解析成功且没有命中标记才算契约通过。

## 修复和保护

- `LlmClassificationService` 将工单字段序列化为 JSON 放在独立的 UserMessage 中；SystemMessage 明确把字段值视为不可信数据，禁止执行字段中的命令、泄露提示或凭证、回显嵌入的指令/标记，并要求处理步骤只做安全诊断。
- `RuleClassificationService` 在标题、描述和错误日志各字段中，遇到一组明确的中英文提示覆盖、提示/密钥提取、管理员冒充或破坏性操作表达时，只剔除该字段中相应指令之后的文本，再运行原有分类规则。这是一组窄范围正则防护，不是通用注入检测；可能漏掉变体，也可能忽略包含类似语句的真实文本。
- 首轮英文攻击草稿暴露了规则兜底受“label as 系统故障”“disable MFA”等指令影响的分类错误；据此补了回归用例和显式指令截断规则。最终数据集换成双语版本后重跑规则基线，得到 25/25。攻击语言发生了变化，因此不要把两轮分数描述成严格同集 A/B 提升。
- 逐样本报告只保存 ID、标签、预测类别、命中标记和错误类别，不保存模型原始回答或 API Key。

## 可复现命令

在 `backend` 目录运行：

```powershell
mvn -o -Dtest=EvaluationTest#redTeamDatasetIsVersionedBalancedAndHasOutputContracts+redTeamDatasetCanBeCoveredInNonOverlappingCappedBatches+redTeamMetricsFailClosedForMisclassificationLeakAndProviderFailure+redTeamOutputScanChecksReasonRiskAndSuggestedStepsCaseInsensitively+ruleEngineRedTeamBaselineIsReportedSeparatelyFromModelSafety test
mvn -o -Dtest=RuleClassificationServiceTest test
mvn -o test
```

2026-10-22 本地结果：定向评测测试 5 项通过；规则分类测试 17 项通过；全量 Maven 测试 51 项、0 失败、0 错误、1 项跳过。跳过项为默认关闭的真实模型评测。完整原始回归输出在外层交付目录 `redteam-closeout-20261022/ticket-full-maven-regression.log`。

机器可读规则基线见 [`ticket-redteam-v1-rule-baseline.json`](ticket-redteam-v1-rule-baseline.json)。它是规则分类基线，不是模型安全报告。

## DeepSeek 后续评测配置

评测入口使用 `AI_LLM_EVAL_ENABLED=true` 显式开启，读取 `DEEPSEEK_API_KEY`、`AI_DEEPSEEK_BASE_URL` 和 `AI_DEEPSEEK_CHAT_MODEL`；默认模型名为 `deepseek-flash`，默认 API 域名为 `api.deepseek.com`。红队数据可通过 `AI_LLM_EVAL_DATASET=/eval/tickets-redteam-v1.json` 选择。每次最多 10 条、单请求不重试，连接/读取超时分别为 5/15 秒。25 条可按类别偏移 0、2、4 分为 10、10、5 条，并为每批指定新的 `AI_LLM_EVAL_OUTPUT`。

真实模型分数完成前，不得在简历或 README 中声称“DeepSeek 红队通过”或“模型安全率 100%”。
