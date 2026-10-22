# Ticket Copilot DeepSeek 实测补充

日期：2026-10-22  
结论：**PASS（限本轮 10 条合成分类样本）**

## 本轮真实调用

- 仓库：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\worktrees\ticket-repair`
- 分支：`java-ai-upgrade`
- HEAD：`ff01d28ced3f13227cc5221b6080501338c983a0`
- 模型：`deepseek-flash`，请求端点主机 `api.deepseek.com`
- 数据：`v1-non-overlap-challenge-20` 中均衡抽取 10 条，每个类别 2 条；样本 ID 见原始结果 JSON。
- 请求边界：10 次上限、2048 max tokens、连接超时 5 秒、读取超时 15 秒、单次尝试；一条失败会停止后续样本。
- 调用：10 次真实 DeepSeek 请求，0 次 Provider/解析失败；未保存原始模型响应文本。
- 首次预检因数据集路径格式错误，在联网前失败、没有产生模型请求；修正为 classpath 资源路径后完成评测。两次日志均保留。

## 结果

| 评测对象 | 样本 | 正确 | Accuracy | Macro-F1 | 说明 |
|---|---:|---:|---:|---:|---|
| DeepSeek | 10 | 10 | 1.0000 | 1.0000 | 五类各 2/2 |
| 本地规则引擎（同一批 10 条） | 10 | 10 | 1.0000 | 1.0000 | 与模型样本完全一致 |
| 本地规则引擎（挑战集全量） | 20 | 20 | 1.0000 | 1.0000 | 五类各 4/4 |

本次样本上模型与规则的 Accuracy 差值为 0。挑战集是人工编写的合成非重叠样本，不是盲测、独立统计留出集或生产样本；结果不能据此宣称模型泛化能力或生产准确率。它也没有包含红队攻击样本，因此不是模型安全测试。

## 原始计分字段的解释

原始 DeepSeek JSON 与 Maven 输出把 `ruleBlindSpotAccuracy` 写为 `0.0`。本数据切片没有任何 `ruleMissExpected=true` 样本，盲点指标的分母为零，因此这个 `0.0` 没有定义，不能解释为失败分数。本轮随后用断言先复现了该问题，再将测试评测器修正为：零样本时存为 `null` 并打印 `N/A`；存在盲点样本时打印盲点分子/分母。新统一结果中该字段为 `null`，分母为 0。历史原始结果和日志保持原样以供审计。

## 回归与变更

本次本地改动仅涉及评测样本、测试评测器和证据文档；未修改生产分类代码或标签。针对计分器修正的测试先以退出码 1 复现 `expected null but was 0.0`，修改后专项测试通过。

| 命令（工作目录为 `backend`） | 退出码 | 结果 |
|---|---:|---|
| `mvn -o -Dtest=EvaluationTest#llmComparisonEvaluation test`（真实模型评测，显式启用 DeepSeek） | 0 | 1 项通过；实际请求 10 次；无失败、无跳过 |
| `mvn -o -Dtest=EvaluationTest#nonOverlappingChallengeV1IsBalancedAndScoresRuleBaseline test`（修复后） | 0 | 1 项通过；零分母指标为 N/A |
| `mvn -o test`（修复后完整回归） | 0 | 100 项，0 失败、0 错误、1 项跳过；跳过的是默认关闭的模型测试 |
| `git diff --check -- backend/src/test/java/com/enterpriseai/ticketcopilot/eval/EvaluationTest.java` | 0 | 无空白错误 |

## Key 与剩余工作

Key 只在隐藏交互输入中短暂注入当前执行进程环境，并在进程结束时清除；没有写入仓库、评测结果或运行日志。因为 Key 已经出现在聊天消息中，建议现在轮换该 Key。后续可继续增加独立人工复核样本与规则盲点样本，再比较模型与规则；当前 10 条成绩不足以作为简历里的泛化指标。

CommerceFlow 的 DeepSeek 结果、MOCK 对照、逐样本统一格式和两项目数据重叠审计见：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\worktrees\commerce-repair\docs\evidence\commerceflow-ai-deepseek-eval-20261022\` 与 `D:\面试冲刺包\_JOB_READY_SPRINT_20261021\deliverables\eval-lineage-audit-20261022\`。
