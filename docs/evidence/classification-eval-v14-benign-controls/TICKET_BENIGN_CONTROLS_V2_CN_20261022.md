# Ticket Copilot：安全词汇无害控制扩展 V2

日期：2026-10-22  
评估方式：`RULE_ONLY`，离线、本地合成数据  
结论：**PASS（仅限本固定控制集）**

## 评测范围

本版包含 30 条固定合成工单，沿用 V1 的 10 条作为前缀，新增 20 条。五类工单各 6 条，内容会在正常故障描述中提及被拦截的提示词、密钥、命令或 SQL 片段，用来检查安全清洗是否误删真实问题线索，以及规则分类是否仍然保持正确。

## 结果

|检查项|结果|
|---|---:|
|工单类别与期望标签一致|30/30|
|真实问题线索在清洗后保留|30/30|
|失败样本|0|
|各类别支持数|账号 6、权限 6、系统故障 6、数据 6、流程咨询 6|

逐样本类别、预测类别、线索保留结果和混淆矩阵见 [`ticket-benign-security-controls-v2-results.json`](ticket-benign-security-controls-v2-results.json)。固定输入见 [`ticket-benign-security-controls-v2.json`](ticket-benign-security-controls-v2.json)。

## 修复记录

扩展控制集初次运行时，TBC20 暴露了一个误删问题：工单描述以“任务不再执行”结尾，后续句子只是引用了 `execute rm -rf /` 安全测试文本。原命令检测表达式允许通配符跨过句号，导致清洗器把前面的真实故障描述也判成危险指令并删除。

修复将中英文破坏性命令匹配限制在同一句内，句号、问号、换行等边界不能再被通配符跨越。复测后 TBC20 的问题线索保留，30 条控制均通过。该规则是窄范围的纵深保护启发式，不等同于通用提示注入检测器。

## 复现与回归

工作目录：`backend/`

```powershell
mvn -o -Dtest=BenignSecurityVocabularyControlEvaluationTest test
mvn -o test
```

- 专项测试：2 项通过，0 失败、0 错误、0 跳过；退出码 0。
- 完整 Maven 回归：90 项，0 失败、0 错误、1 项外部模型评测跳过；退出码 0。
- 完整日志位于本轮交付目录：`ticket-benign-controls-v2-focused-green-20261022.log`、`ticket-full-maven-composite-control-final-20261022.log`。
- 本轮没有发送 DeepSeek 请求；这些指标只描述规则分类器在这 30 条固定合成控制上的结果。

## 限制

30 条样本不是线上准确率、泛化准确率或大模型安全得分。攻击样本的规则基线与这些无害控制的结果要分开报告；后续若做真实模型评测，应把模型逐样本输出另存，不能与 `RULE_ONLY` 数据合并成同一个分数。
