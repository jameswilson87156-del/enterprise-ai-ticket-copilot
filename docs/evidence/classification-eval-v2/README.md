# 工单分类评估 v2

评估日期：2026-10-17（本地）
数据集：[`tickets-eval.json`](../../../backend/src/test/resources/eval/tickets-eval.json)
评估代码：[`EvaluationTest.java`](../../../backend/src/test/java/com/enterpriseai/ticketcopilot/eval/EvaluationTest.java)

## 数据与口径

- 40 条自建合成工单，账号、权限、系统故障、数据、流程咨询五类各 8 条。
- 其中 11 条标记为规则挑战样本，用于单独观察关键词规则的盲区表现。
- 不含客户工单或生产数据。该集合用于本地工程验证，不代表真实企业工单分布。
- 指标由 `EvaluationTest` 按样本逐条比对期望类别计算；Macro 指标先按类别计算，再对五类取平均。

## 规则引擎基线

运行命令：

```powershell
cd backend
mvn "-Dtest=EvaluationTest#ruleEngineEvaluation" test
```

| 指标 | 结果 |
| --- | ---: |
| Accuracy | 32 / 40 = 80.00% |
| Macro Precision | 81.88% |
| Macro Recall | 80.00% |
| Macro F1 | 79.21% |
| 规则挑战样本命中 | 3 / 11 = 27.27% |

| 类别 | Precision | Recall | F1 | 命中 / 样本 |
| --- | ---: | ---: | ---: | ---: |
| 账号问题 | 100.00% | 100.00% | 100.00% | 8 / 8 |
| 权限问题 | 66.67% | 50.00% | 57.14% | 4 / 8 |
| 系统故障 | 100.00% | 62.50% | 76.92% | 5 / 8 |
| 数据问题 | 72.73% | 100.00% | 84.21% | 8 / 8 |
| 流程咨询 | 70.00% | 87.50% | 77.78% | 7 / 8 |

完整逐样本结果、混淆矩阵和原始小数值由测试写入 `backend/target/evaluation/rule-engine-v2.json`。

## DeepSeek 对比状态

评测入口支持 DeepSeek 与 DashScope OpenAI-compatible ChatModel。设置 `DEEPSEEK_API_KEY` 后，还需显式设置 `AI_LLM_EVAL_PROVIDER=deepseek` 和 `AI_LLM_EVAL_ENABLED=true` 才会向外部模型发送合成样本。DeepSeek 默认配置为 `https://api.deepseek.com` 与 `deepseek-flash`，可用 `AI_LLM_EVAL_LIMIT` 做小样本联通检查；部分样本运行会在报告中标注。当前尚无 DeepSeek 工单分类评测结果。

## 本次本地测试

`mvn test`：25 项运行，0 失败、0 错误、1 项跳过。跳过项是默认关闭的外部模型对比评估；规则基线与其余本地测试均通过。
