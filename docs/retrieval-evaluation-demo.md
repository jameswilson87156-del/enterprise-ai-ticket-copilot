# 检索评测演示说明

## 当前评测版本

前端「RAG 检索评测」调用：

`Vue 评测页 → POST /api/evaluations/retrieval/run → RetrievalEvaluationController → RetrievalEvaluationService → KnowledgeMatchingService → MyBatis/MySQL`

评测读取 `backend/src/main/resources/eval/knowledge-retrieval-v2.json`，将候选固定为仓库 `schema.sql` 和 `demo-data.sql` 中的 14 篇已发布演示知识。数据集包含 18 条正向查询（其中包含多相关 qrels 和易混淆查询）及 4 条无匹配查询。运行前会校验 14 篇文章都存在且状态为 `PUBLISHED`；缺少文章时明确失败，不会静默缩小候选语料。

`knowledge-retrieval-v1.json` 保留为历史版本。当前服务不再以每个问题的目标文章拼出候选集。逐样本结果写入 `backend/target/evaluation/knowledge-retrieval-v2.json`。评测固定走项目关键词评分基线，不调用 ChatModel、Embedding API 或 DeepSeek。

V3 诊断压力集位于 `backend/src/main/resources/eval/knowledge-retrieval-v3-stress.json`，沿用同一 14 篇候选知识，包含 12 条正向查询和 8 条无匹配查询。它是在检查 V2 失败后整理的合成诊断集，不是盲测或独立留出集。V3 对应的演示知识种子版本补充了部署条目的检索别名；默认数据集仍为 V2。切换 V3 时，未指定输出路径会按数据集文件名生成独立报告路径，显式指定 `ticket-copilot.evaluation.retrieval.output` 时使用指定路径。

V4 是检索元数据定向回归，合并未改 qrels 的 V3 全部 20 条样本和 V2 中两条采购 FAQ 正向查询，共 14 条正向、8 条无匹配。它验证采购 FAQ 种子关键词收窄后是否既消除打印机误召回，又保留采购流程查询的相关知识。V4 复用已有开发集，不能作为盲测或独立留出集；Web 页面默认评测仍为 V2，V4 通过专用 H2/MyBatis 集成测试运行。

## 指标定义

- **Hit Rate@1 / @3**：正向查询中，至少一个相关知识出现在第一位 / 前三位的比例。
- **Macro Recall@1 / @3**：先对每条正向查询计算相关 qrels 的召回比例，再对查询取平均；一条问题有多篇相关知识时会逐篇计数。
- **Precision@3**：正向查询前三候选中相关知识占 3 个位置的比例，再按查询取平均；候选不足 3 篇时空位计为不相关。
- **MRR**：对正向查询取首篇相关知识排名的倒数，再按查询平均；未召回记 0。
- **无匹配特异度**：无匹配查询中没有返回候选的比例。误召回数量单独报告，不与正向召回合并。

## 可复现测试

后端专项测试：

```powershell
cd backend
mvn -o "-Dtest=RetrievalEvaluationServiceTest,RetrievalEvaluationH2IntegrationTest,RetrievalEvaluationV3H2IntegrationTest,RetrievalEvaluationV4MetadataRefinementH2IntegrationTest" test
```

H2 集成测试会通过真实 MyBatis Mapper 和 `HybridKnowledgeScorer` 跑固定候选语料，使用从演示 SQL 种子整理的文章元数据；它不是实际 MySQL 实例或生产知识库验收。测试输出写入 JUnit 临时目录，避免覆盖仓库已有报告。需要验证本地演示数据库时，先按 README 初始化独立的本地 MySQL 数据库并导入 `schema.sql`、`demo-data.sql`，再启动后端运行页面评测。

前端普通 API 模式启动：

```powershell
cd frontend
npm run dev
```

打开 `http://localhost:5173` 并进入「RAG 检索评测」。`npm run dev:demo` 使用静态 Demo 数据，会禁用后端评测按钮。当前评测没有必要配置模型密钥。

## 2026-10-22 H2 基线结果

本次在 14 篇固定演示文章上真实执行了关键词检索与逐样本计算：

| 指标 | 结果 |
|---|---:|
| 正向查询 Hit Rate@1 / @3 | 14 / 18，77.8% |
| 正向查询 Macro Recall@1 | 59.3% |
| 正向查询 Macro Recall@3 | 72.2% |
| 正向查询 Precision@3 | 37.0% |
| MRR | 0.778 |
| 二元 nDCG@3 | 0.735 |
| 无匹配特异度 | 3 / 4，75.0% |

逐样本报告及失败项记录在 `docs/evidence/retrieval-evaluation-v2-20261022/`。分组诊断为：direct 8/8 首位命中，paraphrase 4/8，hard negative 2/2；no-match 3/4 正确拒绝。二元 nDCG@3 按固定 qrels 对前三位做 0/1 gain 计算。指标只描述固定的小型合成集和 H2 测试语料，不能称作生产检索准确率、独立留出集结果或 DeepSeek 效果。

本次失败样例揭示了具体边界：R04、R06、R08、R14 的改写问句未命中；R10、R12 命中了另一篇相关知识但未覆盖主标注或全部 qrels；N01 的咖啡机报修问题误召回了通用流程文章 `KB-PROCESS-005`。这些失败被保留，没有为提高分数修改标签。RAGAS 的公开资料也把 context precision 和 context recall 分开衡量，分别关注相关上下文的排序精确性与相关信息的覆盖；本项目当前实现的是小型检索排序基线，不宣称已经实现 RAGAS 指标或模型评估。

## 2026-10-22 V3 诊断压力集结果

复现命令（在 `backend` 目录运行）：

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--ticket-copilot.evaluation.retrieval.enabled=true --ticket-copilot.evaluation.retrieval.dataset=classpath:eval/knowledge-retrieval-v3-stress.json --ticket-copilot.evaluation.retrieval.output=target/evaluation/knowledge-retrieval-v3-stress-results.json --ticket-copilot.ai.embedding-enabled=false"
```

| 指标 | 修复前 | 别名补充后 |
|---|---:|---:|
| 正向查询 Hit Rate@1 / @3 | 11/12、11/12 | 12/12、12/12 |
| Macro Recall@1 / @3 | 77.8% / 91.7% | 86.1% / 100.0% |
| Precision@3 | 41.7% | 44.4% |
| MRR | 0.917 | 1.000 |
| 无匹配特异度 | 7/8，87.5% | 7/8，87.5% |

修复前 S07 查询用 ConfigMap、Secret 和启动参数描述部署配置，知识条目的关键词缺少这些已有正文概念，因而没有召回。将这些别名补进 `KB-DEPLOY-ENV` 的演示种子后，S07 进入首位相关结果。N02“打印采购审批单时打印机卡纸”仍误召回采购 FAQ `KB-DRAFT-DEMO-0008`，这是混合意图下的词面误召回，保留在逐样本结果中。V3 数据集是在 V2 失败分析后编写，不能当成独立测试集；结果只表示 H2 中的关键词基线，不是 DeepSeek、Embedding 或生产效果。

逐样本基线与修复后 JSON、数据集快照、RED/GREEN 和完整 Maven 日志保存在 `docs/evidence/retrieval-evaluation-v3-stress-20261022/`。修复前后分别是 V3 诊断基线与别名补充后的结果；V2 数据及历史报告没有重标或覆盖。

## 2026-10-22 V4 采购 FAQ 元数据回归

V3 把打印机卡纸、采购审批单打印失败的 N02 标为无匹配，但基线把 `KB-DRAFT-DEMO-0008` 排在候选中，分数为 36。查看条目内容后发现 `审批` 是过宽的独立关键词。改动只发生在演示知识种子：移除这个泛词，保留 `采购`、`流程`、`FAQ` 和 `材料`；评分公式、35 分阈值和样本标签均不变。

V4 重用 V3 20 条样本并追加 V2 的 R15、R16 两条相关采购问句。变更前后结果如下：

| 指标 | 关键词变更前 | 关键词变更后 |
|---|---:|---:|
| 正向查询 Hit Rate@1 | 14/14 | 14/14 |
| 正向查询 Macro Recall@1 | 78.6% | 78.6% |
| 正向查询 Macro Recall@3 | 100.0% | 100.0% |
| MRR | 1.000 | 1.000 |
| 无匹配误召回 | 1/8 | 0/8 |
| 无匹配特异度 | 7/8（87.5%） | 8/8（100.0%） |

V3 正向案例 S08 和 V2 正向案例 R15、R16 仍能召回采购 FAQ；N02 不再返回候选。这个集合由已看过的 V2/V3 开发样本组成，结果用于回归已知知识元数据变更，不代表新留出数据上的泛化能力。

复现命令（在 `backend` 目录运行）：

```powershell
mvn -o "-Dtest=RetrievalEvaluationV4MetadataRefinementH2IntegrationTest" test
```

测试先在旧关键词上因 N02 误召回而失败，收紧关键词后通过。该 H2 集成测试使用真实 MyBatis Mapper 和关键词评分器，但未连接 MySQL，也未调用模型。完整逐样本前后 JSON、RED/GREEN 日志及 99 项 Maven 回归日志保存在 `docs/evidence/retrieval-evaluation-v4-metadata-refinement-20261022/`。

## 面试讲法

> 我把知识检索评测从“只在答案文章里找目标”扩成了固定候选语料评测。数据集固定引用仓库 14 篇演示知识，正向问题使用一个或多个 qrels，另设无匹配问题；运行前会校验候选文章都已发布。评测服务复用真实的 MyBatis 查询和关键词评分逻辑，生成逐样本排名、Hit Rate、宏平均 Recall、Precision@3、MRR 和负例特异度。H2 基线里 18 条正向问题有 14 条首位命中，4 条无匹配问题有 1 条误召回；我保留这些失败，像咖啡机报修误召回通用流程文章就说明词面匹配的边界。当前结果只代表合成数据和关键词基线，后续要补独立人工标注集，再在同一 qrels 上对比关键词、Embedding 与混合检索。

### 采购 FAQ 元数据问题怎么修？

“V3 的 N02 是打印机维修问题，却因同一句里出现‘采购审批单’而误召回采购 FAQ。我检查到知识条目把‘采购’和‘审批’作为两个独立关键词，各加 18 分后越过 35 分门槛。这个例子应由具体的知识元数据修正，所以我只移除了泛化的‘审批’关键词，保留‘采购、流程、FAQ、材料’，没有改全局阈值或标签。V4 重跑 22 条已标注的 V2/V3 合成回归后，8 条无匹配查询全部无召回，14 条正向查询仍有相关知识首位命中；V4 是定向开发回归，不是独立测试集。”

## 后续评测原则

1. 继续增加有标注依据的合成查询和独立留出集，保持数据集版本可追溯；不要把调过的开发集当成泛化效果。
2. 保持关键词基线、Embedding、混合检索和真实 LLM 结果分别记录，不混合统计。
3. 多相关 qrels 要由知识内容支持并留下注释理由；无匹配负例用于监控误召回。
4. 真实 DeepSeek 对比须使用已轮换的新密钥、合成输入、明确模型名及请求预算，并单独保存 provider、模型、参数、耗时和逐样本结果；本轮没有发出真实模型请求。

## 参考资料

- [RAGAS Context Precision](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/context_precision.md)
- [RAGAS Context Recall](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/context_recall.md)
