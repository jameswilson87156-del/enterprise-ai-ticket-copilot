# Ticket Copilot：安全术语正常控制集 v1

日期：2026-10-22  
运行模式：`RULE_ONLY`  
真实模型请求：0

## 评测问题

规则红队集主要验证注入内容能否被识别和隔离。这次单独补了 10 条正常工单控制：描述里会引用或记录安全术语，但真正要处理的故障仍是普通工单。样本覆盖五种分类，每类两条；每条还定义一个必须保留的真实问题线索。

## 实际结果

| 指标 | 结果 |
| --- | ---: |
| 规则类别正确 | 10 / 10 |
| 真实问题线索在净化后保留 | 10 / 10 |
| 失败样本 | 无 |
| 混淆矩阵 | 五类均为对角线 2 / 2 |

以上是固定合成控制集上的规则回归结果，不是 DeepSeek 或其他 LLM 的分类、安全或生产效果指标。报告保留逐样本的 expected/predicted 类别和必留线索布尔值；不保存外部模型响应。

## 复现

工作目录：`backend/`。

```text
mvn -o -Dtest=BenignSecurityVocabularyControlEvaluationTest test
```

完整回归使用 `mvn -o test`。本轮完整结果为 89 项，0 失败、0 错误、1 项默认关闭的外部模型评测跳过。原始日志位于本机交付目录 `ticket-benign-controls-full-maven-20261022.log`。

## 数据文件

- 固定样本：`backend/src/test/resources/eval/ticket-benign-security-controls-v1.json`
- 测试生成逐样本结果：`backend/target/evaluation/ticket-benign-security-controls-v1.json`

样本全为自拟内容，不含客户、生产工单、密钥或个人信息。
