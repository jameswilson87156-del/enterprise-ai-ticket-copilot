<!-- Public copy: local paths and credential-environment details removed. Synthetic evaluation data only. -->

# Enterprise AI Ticket Copilot｜DeepSeek 双模型评测报告

日期：2026-10-22  
结论：**COMPLETE_WITH_FINDINGS**（主评测与回归已完成，保留真实失败项）

## 项目与配置

- 仓库：本地隔离工作区 <local workspace>
- 分支：java-ai-upgrade
- HEAD：ff01d28ced3f13227cc5221b6080501338c983a0
- Provider：DeepSeek OpenAI-compatible API；模型 deepseek-flash、deepseek-v4-pro；地址 https://api.deepseek.com
- 输入均为合成工单，不包含客户生产数据。
- 规则基线、模型主评测、提示词修改后的定向补测分开统计。主评测发生在最后一轮提示词修改之前；最后提示词只做了定向诊断，没有重跑完整 130 条，因此不把定向结果并入主成绩。

## 主评测结果

固定样本由 40 条均衡分类样本、20 条非重叠挑战样本、70 条 Unicode/格式红队分类样本组成。各集标签未因结果而修改。

| 模型 | 均衡集 | 挑战集 | 红队集 | 总计 |
| --- | --- | --- | --- | --- |
| deepseek-flash | 37/40，92.50%；Macro-F1 0.9223 | 19/20，95.00%；Macro-F1 0.9492 | 62/70，88.57%；Macro-F1 0.8799 | 118/130，90.77% |
| deepseek-v4-pro | 36/40，90.00%；Macro-F1 0.9018 | 18/20，90.00%；Macro-F1 0.8933 | 62/70，88.57%；Macro-F1 0.8756 | 116/130，89.23% |

规则引擎在这三组相同的合成样本上分别为 40/40、20/20、70/70。该集合参与过规则和提示词迭代，不是盲测，也不代表真实工单分布；规则结果不等同于模型质量。

## 合并混淆矩阵

行是期望类别，列是预测类别；“调用失败”单列保留。

### deepseek-flash

| 期望类别 | 账号问题 | 权限问题 | 系统故障 | 数据问题 | 流程咨询 | 调用失败 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 账号问题 | 15 | 0 | 11 | 0 | 0 | 0 |
| 权限问题 | 1 | 25 | 0 | 0 | 0 | 0 |
| 系统故障 | 0 | 0 | 26 | 0 | 0 | 0 |
| 数据问题 | 0 | 0 | 0 | 26 | 0 | 0 |
| 流程咨询 | 0 | 0 | 0 | 0 | 26 | 0 |

### deepseek-v4-pro

| 期望类别 | 账号问题 | 权限问题 | 系统故障 | 数据问题 | 流程咨询 | 调用失败 |
| --- | --- | --- | --- | --- | --- | ---: |
| 账号问题 | 12 | 0 | 13 | 0 | 0 | 1 |
| 权限问题 | 0 | 26 | 0 | 0 | 0 | 0 |
| 系统故障 | 0 | 0 | 26 | 0 | 0 | 0 |
| 数据问题 | 0 | 0 | 0 | 26 | 0 | 0 |
| 流程咨询 | 0 | 0 | 0 | 0 | 26 | 0 |

## 失败样本

- Flash：T01、T04、T19、HLD04、RT01、RT07、RT16、RT21、RT35、RT40、RT45、RT50。
  - 11 条账号问题误分为系统故障；RT07 将权限问题误分为账号问题。
- Pro：T01、T04、T17、T20、HLD01、HLD04、RT01、RT16、RT21、RT30、RT35、RT40、RT45、RT50。
  - 账号问题多被误分为系统故障；T20 是一次 Provider 超时/调用失败，按失败计入主评测。
- 两模型在红队合约集均为 62/70。该合约同时检查标签、调用完成状态和列出的禁止标记，是固定样本上的词法/行为断言，不是通用安全证明。

## 失败驱动的提示词修正与定向复测

最后一轮在 LLM 分类提示中补充账号、权限、系统故障的区分及优先级：账号凭据/登录/SSO/会话失败优先归账号问题；仅在已登录且特定资源/角色授权受限时判为权限问题；有服务不可用、程序或基础设施证据才判系统故障。用户输入仍明确作为不可信数据处理。

- 修改文件：backend/src/main/java/com/enterpriseai/ticketcopilot/service/LlmClassificationService.java
- 测试文件：backend/src/test/java/com/enterpriseai/ticketcopilot/service/LlmClassificationServiceTest.java
- 最终提示词版本的 6 条定向诊断：Flash 6/6，Pro 6/6，样本为 T01、T04、T17、T05、T11、RT07。
- **限制：没有在最终提示词版本上重跑完整 130 条主集。** 6/6 仅是失败边界回归，不能替代主评测准确率。

## 离线回归

| 状态 | 命令 | 工作目录 | 退出码与结果 | 日志 |
| --- | --- | --- | --- | --- |
| PASS | mvn -B test | <local workspace> | 0；100 项，0 失败，0 错误，1 跳过；BUILD SUCCESS | Maven 原始终端日志保留在本机交付包，未公开包含环境路径的日志。 |

Maven 输出有 Windows 控制台中文编码乱码，但 Surefire 汇总和退出码可读。完整测试副本隔离于有未提交改动的源码工作区；本次未连接 DeepSeek，不发送外部请求。

## 请求与 Token

Ticket Copilot 的主评测及提示词诊断共计 Flash 161 次、Pro 162 次请求尝试。全双项目汇总见 [AGGREGATED_RESULTS.json](AGGREGATED_RESULTS.json)；此处不把提示词诊断请求计入 130 条主成绩。

## 证据文件

- [结构化逐样本与批次结果](RAW_RESULTS.jsonl)：包含来源相对路径、原始文件 SHA-256 和解析后的 JSON/JSONL 记录；公开副本中的绝对 Windows 路径已替换为 `<REDACTED_LOCAL_PATH>`。
- [双项目统一汇总](AGGREGATED_RESULTS.json)。
- [源文件清单与 SHA-256](RESULTS_MANIFEST.json)；SHA-256 对应本机原始文件。


## 工作区与安全

本地工作区在开始时已有大量未提交修改与未跟踪评测数据。当前仍为 dirty；没有 reset、clean、提交、推送或部署。本轮只在现有工作树中修改上述服务与测试文件，并在交付目录写入报告和日志。

当前执行环境检查到 DEEPSEEK_API_KEY 未配置。API Key 不在源码、JSON 结果或新日志中；但该凭据曾被发在聊天中，且历史一次进程参数暴露过。应立即在 DeepSeek 控制台撤销并轮换，再由本人通过本机私密环境变量配置新 Key。
