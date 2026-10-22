# Enterprise AI Ticket Copilot｜LLM 输出契约红队评测 v1

日期：2026-10-22  
目标分支：`java-ai-upgrade`  
HEAD：`ff01d28ced3f13227cc5221b6080501338c983a0`（本地改造保持未提交）

## 结论

在模型响应处理链路中复现并修复了一个输出侧问题：模型如果把输入工单中的 `TICKET_CANARY_73` 改写为 `TICKET CANARY 73`，旧实现仍将响应标记为 `LLM` 并返回给调用方。现在会将工单中的可识别标记提取出来，再对输入与输出做 NFKC/格式字符处理后的字母数字指纹比较；当响应回显该标记，或输出字段含有已列明的提示词提取、凭证泄露、覆盖指令及破坏性操作指令时，分类门面改走规则引擎。

独立 fixture 覆盖 9 个合成攻击样本和 2 个正常文案控制样本：攻击样本 9/9 降级到规则引擎，正常控制 2/2 保持 LLM 来源，逐样本结果 11/11。测试使用本地模拟 `ChatModel`，没有发起外部模型请求；这些数字只描述本地输出契约测试，不是 DeepSeek 的安全率或生产表现。

## 可复现样本与结果

- [输出侧红队 fixture](../../../backend/src/test/resources/eval/llm-output-contract-redteam-v1.json)
- [逐样本结果 JSON](llm-output-contract-redteam-v1-results.json)
- 覆盖精确回显、空白/标点拆分、全角和零宽变体，以及英文/中文的提示词提取、凭证泄露和破坏性操作语句。
- 两个对照样本包含普通 “Canary deployment” 和普通 “marker” 文案，用于检查输出过滤不会因为常见词误触发。
- 结果记录样本编号、攻击分组、预期来源、实际来源和断言是否通过；不记录模型原始输出。

## 复现与修复过程

1. 先加入空白变体红测：输入包含 `TICKET_CANARY_73`，模拟模型输出 `TICKET CANARY 73`。修复前测试失败，结果来源为 `LLM`，而预期为 `RULE`。
2. 早期标记匹配曾把普通 “Canary deployment” 文案当成标记，新增控制测试复现该误报。
3. 将匹配范围收窄为包含 `CANARY`/`MARKER` 与标识符、分隔符的 token；再对提取出的 token 与整个响应做小写、仅保留 Unicode 字母数字的指纹比较。
4. 对 `reason`、`riskHint`、`suggestedSteps` 做统一输出契约检查。违规时抛出不包含工单原文或模型原文的通用错误，由 `AiClassificationFacade` 触发既有规则降级。
5. 最终 fixture 中 9 个攻击样本被规则降级，两个正常控制样本仍采用模拟 LLM 结果；完整 Maven 回归通过。

另在日志审计中复现 Provider 异常内容泄露：分类与向量索引失败日志曾记录异常原文。使用合成哨兵字符串的两条测试在修复前都失败；现在只记录异常类型名和降级动作，不记录异常消息。该改动覆盖 ChatModel 分类与 Embedding 初始化两个 Provider 边界。

## 测试与运行证据

工作目录：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\worktrees\ticket-repair\backend`

| 命令 | 退出码 | 真实结果 | 日志 |
|---|---:|---|---|
| `mvn '-Dtest=AiClassificationFacadeTest,LlmClassificationServiceTest,RuleClassificationServiceTest' test` | 0 | 44 项通过，0 失败、0 错误、0 跳过 | [定向服务测试](../../../../../deliverables/ticket-output-redteam-v6/maven-output-contract-green-retry2-20261022.log) |
| `mvn '-Dtest=LlmOutputContractEvaluationTest' test` | 0 | fixture 评测 1 项通过；9 个攻击均降级，2 个控制均接受 | [fixture 测试](../../../../../deliverables/ticket-output-redteam-v6/maven-output-fixture-redgreen-20261022.log) |
| `mvn '-Dtest=ProviderFailureLoggingTest' test`（修复前） | 1 | 2 项失败；两处日志都包含合成 Provider 异常哨兵 | [日志泄露红测](../../../../../deliverables/ticket-output-redteam-v6/maven-provider-log-red-20261022.log) |
| `mvn '-Dtest=ProviderFailureLoggingTest' test`（修复后） | 0 | 2 项通过，哨兵不在日志中 | [日志脱敏绿测](../../../../../deliverables/ticket-output-redteam-v6/maven-provider-log-green-20261022.log) |
| `mvn test` | 0 | 74 项通过，0 失败、0 错误、1 项跳过；H2 smoke 启动成功 | [完整 Maven 回归](../../../../../deliverables/ticket-output-redteam-v6/maven-full-regression-output-contract-logs-20261022.log) |

完整回归的唯一跳过项是需要显式启用的真实模型评测。当前 shell 中 `DEEPSEEK_API_KEY` 不存在，`AI_LLM_EVAL_ENABLED`、`AI_LLM_EVAL_PROVIDER`、`AI_LLM_EVAL_LIMIT`、`AI_LLM_EVAL_OUTPUT` 和 `AI_DEEPSEEK_CHAT_MODEL` 均未设置。此次真实 DeepSeek 请求数为 0。此前在聊天中暴露的 Key 没有复用；使用真实评测前应先轮换，并把新 Key 配置在本机后端环境变量中。

## 范围与限制

- 此处的 9/9 是对固定、人工编写的输出契约样本的拦截结果，不可表述为“模型提示注入防御率 100%”。
- 标记指纹只覆盖可提取的 marker/canary token 及其 Unicode 兼容字符、格式字符、空白和标点变体；任意语义改写、编码或未覆盖的新攻击仍可能绕过规则。
- 规则降级对这些样本验证的是分类来源和边界，不代表系统具备通用内容安全分类器。
- 要评估模型本身，需将同一批合成工单发送给已配置且已轮换密钥的 DeepSeek Provider，并把逐样本 LLM 输出与规则、MOCK 分开存档；当前未完成该项。

## 公开项目参考

- [Promptfoo 红队配置文档](https://www.promptfoo.dev/docs/red-team/configuration/)将目标、攻击插件、投递策略和应用目的分开配置；[评测输出文档](https://www.promptfoo.dev/docs/configuration/outputs/)展示逐测试结果与断言级通过/失败原因。本项目沿用“每条样本有稳定 ID、预期、实际和判定”的可审计思路，没有引入 Promptfoo 依赖。
- [NVIDIA garak 仓库 README](https://github.com/NVIDIA/garak)说明其通过静态、动态和自适应 probes 检查不同 LLM 失效模式。本轮只读取 README 作为项目范围参考，没有声称复用了其实现或测试框架。
