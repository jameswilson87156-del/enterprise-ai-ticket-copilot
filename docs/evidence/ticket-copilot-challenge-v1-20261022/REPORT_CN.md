# Ticket Copilot 非重叠挑战集离线验收

> **历史离线阶段记录。** 本文件形成于真实 DeepSeek 补测之前；其中“未发起远程请求”的说明仅描述当时的离线阶段。后续真实模型结果、计分器修正和最新完整回归见同目录 [DEEPSEEK_EVAL_ADDENDUM_CN.md](DEEPSEEK_EVAL_ADDENDUM_CN.md)。

日期：2026-10-22  
结论：**PASS（仅限本地确定性规则分类挑战集；不代表大模型效果或盲测泛化能力）**

## 工作区与边界

- 仓库：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\worktrees\ticket-repair`
- 分支：`java-ai-upgrade`
- 基线提交：`ff01d28ced3f13227cc5221b6080501338c983a0`
- 执行时工作区已有多项未提交修改和未跟踪评测文件；本次保留原状，没有 reset、clean、切换分支、提交或推送。
- 本轮新增 20 条人工编写的合成挑战样本，五个类别各 4 条；没有使用客户、生产或真实工单数据。
- 挑战集和现有 `tickets-eval.json`、`tickets-redteam-v1..v5.json` 的 ID 与规范化输入指纹均不重合。它是人工编写的非重叠挑战集，不是盲测集、独立统计留出集或生产样本。
- 本轮没有发起 DeepSeek 或其他远程模型请求。结果来自确定性本地规则引擎。

## 变更文件

- `backend/src/test/resources/eval/tickets-challenge-v1.json`：新增合成挑战集。
- `backend/src/test/java/com/enterpriseai/ticketcopilot/eval/EvaluationTest.java`：新增平衡性、非重叠性及规则基线评测断言。
- `docs/evidence/ticket-copilot-challenge-v1-20261022/results-rule.json`：逐样本机器可读结果。

没有修改生产分类实现、标签或已有数据集预期。

## 实际命令与结果

工作目录均为 `D:\面试冲刺包\_JOB_READY_SPRINT_20261021\worktrees\ticket-repair\backend`。

| 命令 | 退出码 | 结果 |
|---|---:|---|
| `mvn -o -Dtest=EvaluationTest#nonOverlappingChallengeV1IsBalancedAndScoresRuleBaseline test` | 0 | 1 项通过，0 失败、0 错误、0 跳过。20/20 正确；Accuracy、Macro Precision、Macro Recall、Macro-F1 均为 1.0000；五类各 4/4；混淆矩阵对角线为 4，其余为 0。 |
| `mvn -o test` | 0 | 100 项，0 失败、0 错误、1 项跳过；`BUILD SUCCESS`。跳过项是默认关闭的真实模型评测。 |
| `git diff --check -- backend/src/test/java/com/enterpriseai/ticketcopilot/eval/EvaluationTest.java backend/src/test/resources/eval/tickets-challenge-v1.json` | 0 | 无空白错误。 |

规则盲点准确率为 **N/A**：本挑战集没有标注 `ruleMissExpected=true` 的样本，因此不存在该指标的分母。不能把 N/A 写成 0 或 100%。

## 日志说明

- `maven-targeted-v2.log` 是修正指标展示后的最终专项日志。
- `maven-full.log` 是本轮最终完整 Maven 日志。
- `maven-targeted.log` 保留首次运行的原始日志。首次日志把零分母盲点指标显示成 `0.0000`；发现后已修正测试输出为 `N/A` 并重跑。首份日志仅作审计轨迹，不作为最终指标证据。

## DeepSeek 状态与剩余条件

执行时进程环境未设置 `DEEPSEEK_API_KEY`，`AI_LLM_EVAL_ENABLED` 也未启用；项目的真实模型评测没有执行。用户在聊天中发送过的 Key 不写入仓库或报告。应在本机安全设置新的、轮换后的密钥，再通过项目既有显式开关运行受限样本；此报告没有 DeepSeek 分数。

因此，本报告只证明这 20 条确定性合成样本上的本地规则结果和全量 Maven 回归结果，不证明真实模型效果、盲测泛化、线上数据表现或生产环境行为。
