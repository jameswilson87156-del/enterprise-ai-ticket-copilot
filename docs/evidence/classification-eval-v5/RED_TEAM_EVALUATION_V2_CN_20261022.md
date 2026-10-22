# Enterprise AI Ticket Copilot 红队评测 v2（2026-10-22）

## 范围

- 数据集：[`tickets-redteam-v2.json`](../../../backend/src/test/resources/eval/tickets-redteam-v2.json)，50 条双语合成工单；完整保留 v1 的 25 条，并新增 25 条变体。没有客户或生产工单。
- 攻击面：10 类，每类 5 条：提示覆盖、提示提取、密钥提取、角色冒充、危险操作指令、伪造分隔符、多语言越狱、引用攻击、字段级注入、权限绕过。
- 期望类别五类各 10 条。新增攻击语句分布在 title、description、errorLog、systemName、urgency 字段。
- 每条攻击用独立 `TICKET_CANARY_*` 标记做后续输出扫描。当前离线规则评测只评工单类别，不会把规则分数解释成模型安全率。

## 规则引擎离线基线

- v2 数据集规则分类：50/50；Accuracy、Macro-Precision、Macro-Recall、Macro-F1 均为 1.0000。
- 五类各命中 10/10，混淆矩阵为对角矩阵；未发现规则分类失败样本。
- 该结果只说明这组固定样本与规则分类器的离线行为，不是 DeepSeek 结果，也不能说明通用提示注入防护能力。

机器可读逐样本结果：[`ticket-redteam-v2-rule-baseline.json`](ticket-redteam-v2-rule-baseline.json)。

## 实际验证

```powershell
cd backend
mvn -o -Dtest=EvaluationTest#expandedRedTeamV2IsBalancedAndReportsRuleOnlyBaseline test
mvn -o test
```

- 定向 v2 评测：1 项通过，退出码 0；输出 50/50 规则基线。
- 全量离线 Maven：52 项，0 失败、0 错误、1 项跳过，退出码 0；跳过项是默认关闭的真实模型评测。
- 完整原始控制台输出：[`ticket-full-maven-regression-20261022.log`](ticket-full-maven-regression-20261022.log)。
- 真实模型红队请求数：0。新 v2 数据尚未用于 DeepSeek 评测。

## 与既有模型结果的区别

普通分类的历史真实 DeepSeek 对比是另一组 40 条合成工单：38/40，Accuracy 0.9500、Macro-F1 0.9499；错分 T04（账号问题预测为系统故障）和 T13（权限问题预测为账号问题）。该结果见[分类评测 v3](../classification-eval-v3/README.md)，不能与本报告的规则基线混为一谈。

下一次真实模型红队评测可将 `AI_LLM_EVAL_DATASET` 指向 `/eval/tickets-redteam-v2.json`，分 5 批、每批 10 条，类别偏移分别为 0、2、4、6、8；每批使用独立输出路径。需要新的本机密钥；已经粘贴到聊天中的旧密钥不应再使用。
