# Ticket Copilot 当前离线验收记录

日期：2026-10-22

## 工作区

- 仓库：D:\面试冲刺包\_JOB_READY_SPRINT_20261021\worktrees\ticket-repair
- 分支：java-ai-upgrade
- HEAD：ff01d28ced3f13227cc5221b6080501338c983a0
- 本轮开始时工作区已有未提交修改和未跟踪评测数据；均予以保留。本轮没有改动业务源码，没有 reset、clean、提交或推送。
- 本次新增本报告和 maven-test.log，用于记录当前验收。

## 当前完整 Maven 回归

- 工作目录：backend/
- 命令：mvn test
- Maven 结果：BUILD SUCCESS（退出码 0）
- 汇总：99 tests，0 failures，0 errors，1 skipped。
- 唯一跳过为 EvaluationTest#llmComparisonEvaluation。该项要求显式设置 AI_LLM_EVAL_ENABLED=true 与 DeepSeek Provider 凭据。本次环境检查显示 DEEPSEEK_API_KEY、AI_DEEPSEEK_API_KEY、COMMERCEFLOW_AI_API_KEY 均未配置，评测开关 AI_LLM_EVAL_ENABLED 未设置；没有发起外部模型请求。
- 原始 Maven 输出：maven-test.log。

## 离线评测结果

本次完整 Maven 输出中可见多个版本化规则分类集：

| 数据集 | 样本数 | Accuracy | Macro-F1 |
|---|---:|---:|---:|
| 基线 | 40 | 1.0000 | 1.0000 |
| Red-team V1 | 25 | 1.0000 | 1.0000 |
| Red-team V2 | 50 | 1.0000 | 1.0000 |
| Red-team V3 | 55 | 1.0000 | 1.0000 |
| Red-team V4 | 60 | 1.0000 | 1.0000 |
| Red-team V5 | 70 | 1.0000 | 1.0000 |

上述数据集是版本化迭代集，样本存在复用；不得将各版本简单相加并称为独立盲测集。本次输出还包含各集的逐类 Precision、Recall 与 rule-blindspot-accuracy，均为 1.0000。

已有 classification-eval-v16-layered-redteam-20261022 逐样本报告覆盖 38 条合成用例：23 条攻击断言通过、15 条正常控制通过，外部请求 0。它验证规则清洗、模型输出守卫与规则回退契约，不代表真实 DeepSeek 安全率。

已有 Retrieval V4 证据为 22 条复用的开发样本：14 条正向查询 Hit@1/Hit@3 为 14/14，无匹配拒绝从 7/8 改进到 8/8；它不是独立留出集。当前完整 Maven 回归包含对应 H2 集成测试。

## DeepSeek 配置与边界

- 评测代码预设 DeepSeek endpoint host api.deepseek.com，默认模型标识 deepseek-flash；当前 Spring Boot 应用默认仍走 DashScope qwen-plus，两者是分开的配置路径。
- 受控 DeepSeek 评测要求显式启用 AI_LLM_EVAL_ENABLED=true，每次最多 10 条合成工单；默认请求上限等于选取样本数，单次请求 maxTokens=2048、连接超时 5 秒、读取超时 15 秒、重试 1 次（即不重试）。
- 本次没有真实 DeepSeek 结果，因此不可把规则 Accuracy/Macro-F1 标成模型效果。
- 先前在聊天中发送过的 API Key 不写入仓库或命令历史。若要执行真实补测，请先撤销旧 Key，并在本机安全设置新的 DEEPSEEK_API_KEY；完成后即可按该评测器的 10 条上限运行。

## 当前结论

Ticket Copilot 的离线后端测试、规则评测、H2 检索回归和合成红队契约有可复核证据。本轮未验证浏览器端到端流程、生产 MySQL/Redis 集成或真实 DeepSeek 行为，因此这些能力不能写成已验收。
