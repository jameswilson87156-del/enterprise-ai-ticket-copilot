# Ticket Copilot：显式编码文本红队补测 V1

## 范围

新增合成输入集和模拟模型输出集，分别覆盖显式标记的 Base64、Base64URL、HEX 与 ROT13。每种输入攻击配一个正常控制；输出侧用 3 种编码回显 canary，并配 3 个正常控制。数据不含客户工单。

输入夹具：[ticket-input-framed-encoded-redteam-v1.json](ticket-input-framed-encoded-redteam-v1.json)；逐样本结果：[ticket-input-framed-encoded-redteam-v1-results.json](ticket-input-framed-encoded-redteam-v1-results.json)。输出夹具：[ticket-output-framed-encoded-redteam-v1.json](ticket-output-framed-encoded-redteam-v1.json)；逐样本结果：[ticket-output-framed-encoded-redteam-v1-results.json](ticket-output-framed-encoded-redteam-v1-results.json)。

## 结果

| 检查面 | 攻击 | 正常控制 | 观察结果 |
| --- | ---: | ---: | --- |
| 工单输入 | 4/4 通过 | 4/4 通过 | 编码攻击句被移除，标记的真实故障线索保留；正常编码控制保留 |
| 输出契约 | 3/3 触发规则回退 | 3/3 仍接受模拟 LLM 来源 | Base64、HEX、ROT13 编码的输入 canary 回显被识别 |

输出测试使用本地 `ChatModel` stub，不调用模型 Provider。两个数据集的报告都记录 `realProviderRequests: 0`。这些数字只代表固定的 14 条合成样本，不是 DeepSeek 成绩或通用提示注入防护率。

## 修复点

- 增加有界、严格 UTF-8 解码，只处理显式 `BASE64`/`BASE64URL`/`HEX`/`ROT13` 前缀；编码 token 和解码文本均设长度上限，解码最多两轮。
- 输入检测命中显式注入时移除攻击句段，同时保留 `Actual issue` 后的真实工单症状。
- 输出 canary 检查必须先按原始大小写解码 Base64，再做指纹归一化。红测发现先转小写会破坏 Base64；ROT13 token 也需允许下划线及空格。
- 正常控制仍通过，以防把所有编码文本一概删除或把正常模型输出全部回退。

## 验证

| 命令 | 结果 |
| --- | --- |
| `mvn -o '-Dtest=FramedEncodingRedTeamTest,FramedEncodingEvaluationTest' test`（目录：`backend`） | 退出码 0；11 项通过，0 失败、0 错误、0 跳过 |
| `mvn -o test`（目录：`backend`） | 退出码 0；88 项，0 失败、0 错误、1 项跳过 |

完整日志：[定向评测](../../../../../deliverables/ticket-framed-encoding-green-evaluation-20261022.log) · [Maven 全量回归](../../../../../deliverables/ticket-full-maven-framed-encodings-20261022.log)。此前单独的初始探索性红测日志：[初始输入红测](../../../../../deliverables/ticket-framed-encoding-red-20261022.log)；该日志只代表当时的 ROT13 断言，不作为所有攻击类型的基线成绩。

## 面试讲法

“我把 Base64、Base64URL、十六进制和显式 ROT13 标记文本拆成独立合成用例，每类都配正常控制。输入侧验证攻击句被剔除但故障症状留下；输出侧用本地 ChatModel stub 验证模型把 canary 编码回显时会触发规则引擎降级。测试还暴露了 Base64 大小写被过早归一化的问题。当前结果仅是 14 条离线规则/Mock 契约用例，没有调用真实 DeepSeek，也不宣称通用模型安全率。”

## 局限与参考

未覆盖无前缀的任意 Base64、长链嵌套、语义改写、图片/音频、多轮攻击或真实模型的生成表现；规则解码只作为输入及输出边界的纵深防护。编码探针参考 [NVIDIA garak encoding probes](https://github.com/NVIDIA/garak/blob/main/garak/probes/encoding.py)；测试技术与数据分离的思路参考 [Microsoft PyRIT scanner documentation](https://github.com/microsoft/PyRIT/blob/main/doc/scanner/0_scanner.md)。
