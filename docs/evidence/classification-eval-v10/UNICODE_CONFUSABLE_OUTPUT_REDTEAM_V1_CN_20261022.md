# Ticket Copilot 输出同形字红队 v1

日期：2026-10-22。全部样本为人工构造的合成文本，没有真实工单或客户数据。

## 目的与发现

在 v9 的 canary 输出校验上增加一组跨文字系统的同形字测试。模拟模型将 ASCII canary 中的拉丁字符替换为视觉相近的西里尔字母时，旧指纹仍保留西里尔字符，3 条攻击样本均没有触发规则降级。红测在新增的 `markerFingerprint` 上复现该行为。

修复只在 canary/marker 指纹比较时折叠有限的一组常见西里尔同形字到 ASCII；不改写工单原文，也不把这项局部处理描述成完整 Unicode 同形字防护。

## 固定样本结果

- 本地模拟 `ChatModel`，外部模型请求数为 0。
- 同形字攻击：3/3 触发规则降级。
- 正常控制：2/2 保留模拟 LLM 来源，覆盖普通 “Canary deployment” 和 “marker” 文案。
- 逐样本结果：5/5 断言通过。

| 阶段 | 定向测试结果 |
|---|---|
| 修复前 | 测试预期 3 条攻击均降级，实测为 0 条触发；红测失败 |
| 修复后 | 1 项评测测试通过：3/3 攻击降级、2/2 控制保留 |

## 回归与产物

- 定向命令：`mvn -o '-Dtest=LlmOutputContractEvaluationTest#evaluatesUnicodeConfusableMarkerEchoesAndBenignControls' test`，退出码 0。
- 完整回归：`mvn -o test`，75 项通过、0 失败、0 错误、1 项跳过；H2 smoke 启动成功。
- [合成 fixture](ticket-output-confusable-redteam-v1.json)
- [逐样本结果](ticket-output-confusable-redteam-v1-results.json)

## 解释边界

这是一个 5 条样本的本地输出契约回归，检查有限 canary 指纹折叠是否生效、普通控制是否仍通过；它不是模型自身的攻击成功率、DeepSeek 安全评测或生产效果。实现只映射报告所列的常见西里尔字符，不覆盖所有文字系统、同形字、编码方式或语义改写。要验证模型是否会产生此类变体，仍需使用新轮换并只在后端配置的 DeepSeek Key 运行真实模型批次；本轮没有发送此类请求。
