# Ticket Copilot 检索 V3 诊断评测记录

**状态：PASS（离线关键词基线与 H2 回归）；真实 DeepSeek 对比：BLOCKED（本机无可用密钥）。**

## 评测边界

- 项目工作区：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\worktrees\ticket-repair`
- 分支：`java-ai-upgrade`
- HEAD：`ff01d28ced3f13227cc5221b6080501338c983a0`
- V3 数据集：14 篇固定演示知识、12 条正向查询、8 条无匹配查询，共 20 条。
- V3 是查看 V2 失败后编写的合成诊断集，逐条 qrels 有语料依据；它不是盲测、独立留出集或 DeepEval 生成集，不含客户/生产数据。
- 检索模式为 `keyword-baseline`、`topK=5`。H2/MyBatis 集成测试使用固定演示语料夹具；未连接真实 MySQL。
- 没有发起真实 DeepSeek、其他模型或 Embedding API 请求。`DEEPSEEK_API_KEY`、`AI_DEEPSEEK_API_KEY`、`COMMERCEFLOW_AI_API_KEY`、`DEEPEVAL_API_KEY`、`CONFIDENT_API_KEY` 在进程、用户和机器环境均不存在；本机未安装 DeepEval。报告不把离线数据称作模型生成或模型评分。

## 发现、修复和结果

首次 V3 基线复现出两个问题：S07 对部署环境的改写没有召回知识；N02 因“采购审批单”词面命中而误召回采购 FAQ。检查 `demo-data.sql` 后，S07 的 ConfigMap、Secret、变量名和启动参数确实属于部署条目正文场景，但未进入用于关键词打分的 `keywords` 字段。将这些确有语料依据的词补进部署条目后，S07 首位命中。N02 保留为失败，没有调阈值或更改标签。

| 指标 | 修复前 | 修复后 |
|---|---:|---:|
| Hit Rate@1 | 11/12（91.7%） | 12/12（100%） |
| Hit Rate@3 | 11/12（91.7%） | 12/12（100%） |
| Macro Recall@1 | 77.8% | 86.1% |
| Macro Recall@3 | 91.7% | 100% |
| Precision@3 | 41.7% | 44.4% |
| MRR | 0.917 | 1.000 |
| 无匹配特异度 | 7/8（87.5%） | 7/8（87.5%） |

修复后的唯一已标注失败是 N02：`财务办公室打印机卡纸，采购审批单打印到一半停住了，怎么维修？`，误召回 `KB-DRAFT-DEMO-0008`。这是关键词召回的混合意图边界，后续应使用额外人工标注的独立数据研究主意图判断或重排；当前结果不支持宣称已经解决此类误召回。

## 代码与报告变更

- `RetrievalEvaluationService` 支持通过 `ticket-copilot.evaluation.retrieval.dataset` 选择版本化数据集。默认仍跑 V2；未显式配置输出路径时，V2 维持既有报告名，自定义数据集根据文件名写入独立报告，避免覆盖 V2 报告。
- `demo-data.sql` 补充 `KB-DEPLOY-ENV` 的检索别名；V3 标注 `demo knowledge seed v2 with deployment retrieval aliases`。V2 数据集和报告未重标、未覆盖。
- 新增 V3 H2/MyBatis 集成测试与独立数据快照；原 H2 V2 测试改为使用 JUnit 临时目录保存结果。
- README、检索评测演示说明和面试指南已同步 V2/V3 范围、指标和失败边界。
- 本轮未提交、未推送、未部署；原工作区其他未提交改动保持原样。

## 验证记录

| 命令 | 结果 |
|---|---|
| `mvn -o '-Dtest=RetrievalEvaluationV3H2IntegrationTest' test`（首次） | RED：配置指定 V3，实际读到 V2；证明数据集路径不可配置。日志：`red-v3-S07.log`。 |
| `mvn -o '-Dtest=RetrievalEvaluationV3H2IntegrationTest' test`（别名修复后） | 1 项通过，0 失败/错误；S07 进入相关结果。日志：`green-v3-aliases.log`。 |
| `mvn -o '-Dtest=RetrievalEvaluationServiceTest,RetrievalEvaluationH2IntegrationTest,RetrievalEvaluationV3H2IntegrationTest,HybridKnowledgeScorerTest' test` | 10 项通过，0 失败/错误/跳过；V2 既有指标断言保持通过。日志：`regression-v2-v3.log`。 |
| `mvn -o '-Dtest=RetrievalEvaluationServiceTest#customDatasetWithoutAnExplicitOutputDoesNotReuseTheV2ReportPath' test`（首次） | RED：自定义数据集的空输出配置未隔离。日志：`red-output-path.log`。 |
| `mvn -o '-Dtest=RetrievalEvaluationServiceTest' test`（修复后） | 4 项通过，0 失败/错误/跳过；V2 默认输出名不变，V3 得到独立输出名。日志：`green-output-path.log`。 |
| `mvn -o test` | 97 项测试，0 失败、0 错误、1 项跳过；BUILD SUCCESS。日志：`full-maven-final.log`。 |
| `git diff --check` | 退出码 0，无空白错误；有 Git 对 LF/CRLF 转换的既有提示。 |

完整 Maven 回归前将 `backend/target/evaluation/` 中的 24 个既有文件逐一快照，测试后恢复并逐文件校验；24/24 哈希一致。V2 H2 报告保持 SHA-256 `362B88424A81C97884B95AFBB7F24772A21E705BD47173696BA8EA653FDED2EB`。

## 证据文件 SHA-256

| 文件 | SHA-256 |
|---|---|
| `retrieval-v3-before-repair.json` | `D01626CE237BC5F9CC9C6E539E2DCAEAA29F1482FBAF84BBBF716D370EEDD6DF` |
| `retrieval-v3-after-repair.json` | `D4D87B1B92C1EE1097F56C51DE29BC68B83A8AE67AED184EFD23817986D80975` |
| `knowledge-retrieval-v3-stress.json`（修复前快照） | `C397AF045460CEC5809A12C709E41C577A9AFAF8C4599AD7EA40EA4EF71ADAF3` |
| `knowledge-retrieval-v3-stress-final.json`（修复后数据集快照） | `8610AF47ABE9DEDF19B64DFA7399D5D8F19B186C4A01D90E3D19EBC5AC32ECD5` |
| `full-maven-final.log` | `84E1D637BEC0EA638ACDD0B5E4CB5F2A09037069DE0F53850D047F2D47134B78` |

## 公开方法参考

RAGAS 文档把 Context Precision（相关内容在排序中的位置）与 Context Recall（相关内容覆盖率）分开定义。本项目的 Hit Rate、Macro Recall 和 Precision@3 是基于固定文章 ID/qrels 的离线指标，不是 RAGAS LLM 指标，也没有调用评判模型：

- [RAGAS Context Precision](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/context_precision.md)
- [RAGAS Context Recall](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/context_recall.md)
