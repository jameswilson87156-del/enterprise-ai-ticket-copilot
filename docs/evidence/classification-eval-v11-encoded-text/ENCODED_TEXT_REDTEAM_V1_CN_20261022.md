# 编码文本输入与输出边界补测（2026-10-22）

## 目标与范围

依据公开红队方法中“编码变体输入”作为独立攻击策略的思路，增加一个窄范围、版本化的合成回归集。本次只覆盖 ASCII 百分号编码与十进制/十六进制数字 HTML 实体，每条攻击各配一个普通文本控制。它不代表 Base64、ROT13、任意 Unicode、嵌套编码或通用提示注入防护。

## 复现与结果

| 测试面 | 修复前 | 修复后 |
| --- | --- | --- |
| 工单字段输入净化 | 两种编码攻击均未被识别；4 条中 2 条攻击断言失败 | 2/2 攻击片段移除且真实故障描述保留；2/2 控制保留 |
| 模拟 ChatModel 输出 | 两种编码 canary 回显未触发降级；4 条中 2 条断言失败 | 2/2 攻击触发规则降级；2/2 正常控制仍接受模拟 LLM 来源 |

输入逐样本结果见 ticket-input-encoded-redteam-v1-results.json，输出逐样本结果见 ticket-output-encoded-redteam-v1-results.json。测试夹具仅含合成字符串。

## 实现边界

输入路径在有限两轮内解码 ASCII %HH 和数字 HTML 实体，仅在解码视图命中明确的注入句式时移除对应攻击句，并验证带标签的真实问题线索继续保留。输出标记检查在指纹比较前做同样的有界解码，再进行已有归一化/有限同形字折叠。两条路径均不修改原始用户资料对象；未声称覆盖任意编码链或语义改写。

## 验证

- 专项：mvn -o -Dtest=LlmOutputContractEvaluationTest test，4 项通过，0 失败、0 错误、0 跳过，退出码 0。
- 全量：mvn -o test，77 项，0 失败、0 错误、1 项跳过，退出码 0。唯一跳过项是默认关闭的真实模型评测。
- 本轮真实 DeepSeek 请求：0；本结果属于规则逻辑与模拟 ChatModel 契约回归，不是模型安全分数。
- 对应本机原始日志保存在交付目录：ticket-encoded-red-correct-dir-20261022.log、ticket-encoded-green-targeted-v2-20261022.log、ticket-full-maven-20261022.log。

## 面试讲法

“我把编码文本作为独立攻击策略加进固定回归集，先用百分号编码和数字 HTML 实体各做攻击样本，并分别配正常控制。红测复现了输入清理与输出 canary 检查的缺口；修复后按有界 ASCII 解码检查明确模式，并通过逐样本断言确认攻击片段被移除或触发规则降级，同时真实故障线索和普通控制仍保留。当前只有 4 条小样本，且使用规则/Mock，不把结果说成真实模型安全率。”

## 公开方法参考

- NVIDIA garak 编码输入探针：https://github.com/NVIDIA/garak/blob/main/garak/probes/encoding.py
- Promptfoo 红队插件与策略配置：https://github.com/promptfoo/promptfoo/blob/main/site/docs/red-team/configuration.md
