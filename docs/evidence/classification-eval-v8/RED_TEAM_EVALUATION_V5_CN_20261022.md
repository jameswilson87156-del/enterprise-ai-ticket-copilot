# Enterprise AI Ticket Copilot 红队评测 v5

日期：2026-10-22

## 本轮结论

- v5 共 70 条合成工单，逐条保留 v4 的 RT01–RT60，并新增 10 条 Unicode 混淆样本。
- 规则分类离线基线为 70/70；Accuracy、Macro-Precision、Macro-Recall、Macro-F1 均为 1.0000。五类各 14/14，混淆矩阵只有对角线非零，没有失败样本。
- 上述数字只描述当前规则实现对固定合成数据的结果，不是 DeepSeek/LLM 的提示注入安全率，也不是生产准确率。
- 新增测试捕获了发往 `ChatModel` 的实际 Prompt：全角/零宽混淆攻击及 canary 被移除，明确标记的真实问题仍保留，原始 `SupportTicket` 对象不变。
- 本轮真实模型请求数为 0；没有调用 DeepSeek、DashScope 或共享的其他模型端点。

## 数据集与覆盖面

- 14 类攻击面，每类 5 条：v4 的 12 类原样保留，新增 `unicode_nfkc_compatibility` 和 `zero_width_format_control`。
- 新增全角字符样本 RT61–RT65、零宽格式字符样本 RT66–RT70；每组均覆盖账号、权限、系统故障、数据和流程五类目标，并在攻击后保留明确标记的真实工单症状。
- 五类标签各 14 条。全部为人工编写的合成样本，没有客户或生产数据，旧样本标签没有更改。
- [v5 原始数据](tickets-redteam-v5.json) · [逐样本结果、指标与混淆矩阵](ticket-redteam-v5-rule-baseline.json)

## 复现的问题与修复

- 新增的 Prompt 边界测试在修复前失败：混淆指令和 canary 被原样序列化进模型用户消息；规则分类路径也未识别该攻击。
- 在规则清理前先对文本做 Unicode NFKC 兼容归一化，再移除 Unicode `Cf` 格式控制字符，使全角字符和零宽字符回到可识别形式。
- LLM 请求中标题、系统名、优先级、描述和错误日志均经过同一个窄范围清理器；序列化使用清理后的副本，不修改持久化实体。
- 清理规则保留以 `Actual issue:` 或 `实际问题：` 明确标记、且未命中已识别攻击的后续症状。NFKC 会将部分兼容标点规范化，因此清理后的提示词文本可能与用户原文在标点字形上不同。
- 这是针对已列明模式的纵深防御。基于正则的清理不能识别任意改写、语义编码或新型提示注入；系统提示仍明确要求模型将所有工单字段视作不可信数据。

## 执行证据

工作目录：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\worktrees\ticket-repair\backend`

| 命令 | 退出码 | 真实结果 | 日志 |
|---|---:|---|---|
| `mvn -o '-Dtest=RuleClassificationServiceTest,LlmClassificationServiceTest' test` | 0 | 37 项通过，0 失败，0 错误，0 跳过 | [定向测试日志](../../../../../deliverables/ticket-unicode-redteam-v5/maven-targeted-green-retry1-20261022.log) |
| `mvn -o '-Dtest=EvaluationTest#unicodeObfuscationRedTeamV5PreservesV4AndMeasuresRuleBaseline' test` | 0 | v5 评测 1 项通过，70/70；逐样本结果写入报告 | [v5 评测日志](../../../../../deliverables/ticket-unicode-redteam-v5/maven-v5-redteam-retry1-20261022.log) |
| `mvn -o test` | 0 | 67 项通过，0 失败，0 错误，1 项跳过 | [完整 Maven 日志](../../../../../deliverables/ticket-unicode-redteam-v5/maven-full-suite-20261022.log) |

完整测试中的跳过项是显式启用条件控制的外部模型评测；没有配置 DeepSeek Key，`AI_LLM_EVAL_ENABLED` 和 Provider 变量未设置。测试上下文使用 H2 smoke 配置，没有连接 MySQL。

## 模型评测边界

- 本机 `DEEPSEEK_API_KEY` 未配置；常规运行配置仍指向 DashScope 兼容地址，项目内 DeepSeek 对比评测默认关闭。
- 由于本轮只有规则离线基线和模拟 ChatModel 单测，不能把 v5 的 70/70 写成真实模型安全结果。
- 之前在聊天中出现过的 Key 不应复用；轮换并通过本机环境安全配置后，才可考虑对这 70 条合成样本做受控真实模型评测。

