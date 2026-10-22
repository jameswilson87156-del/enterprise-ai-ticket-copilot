# Ticket Copilot 检索元数据回归 V4

## 结论

**PASS：**在 H2 + 真实 MyBatis Mapper + 现有关键词评分器上，针对性 V4 回归由红转绿。V3 的打印机维修问题不再召回采购审批 FAQ；V3/V2 中三条采购 FAQ 正向查询仍可召回该知识。

该回归复用此前已经查看过的 V2/V3 合成开发样本，不是盲测、独立留出集或生产检索效果估计。没有连接 MySQL，没有发送 DeepSeek 或其他模型请求。

## 工作区与变更边界

- 项目目录：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\worktrees\ticket-repair`
- 分支：`java-ai-upgrade`
- HEAD：`ff01d28ced3f13227cc5221b6080501338c983a0`
- 本轮开始前工作区已有其他未提交修改和未跟踪文件；这些文件未清理、重置或覆盖。
- 本轮产品数据改动仅收窄 `KB-DRAFT-DEMO-0008` 的演示种子关键词：从 `采购,审批,FAQ,流程,材料` 改为 `采购,流程,FAQ,材料`。
- 未改关键词评分公式、35 分召回门槛、知识正文、检索 API、数据集标签或相关性 qrels。应用的默认评测数据集仍是 V2；V4 通过专用 H2 集成测试运行。

## 数据集与复现设计

V4 共 22 条：复用 V3 的 12 条正向查询和 8 条无匹配查询，再复用 V2 的 R15、R16 两条采购 FAQ 正向查询。只为避免 ID 冲突给原样本 ID 加 `V3-` / `V2-` 前缀，查询文本、期望文章、相关文章和标注理由均沿用原值。

保留 N02 的标签为无匹配。它询问财务办公室打印机卡纸维修，却提到采购审批单。旧词表让 `采购` 与 `审批` 各命中一次，得分 `18 + 18 = 36`，超过 35 分门槛。关键词清理后该查询只剩一个共享词，得分低于召回门槛；正向采购流程样本仍有 `采购` 与 `流程` 等词项。

## 逐样本指标对比

| 指标 | 修改前 | 修改后 |
|---|---:|---:|
| 总样本 | 22 | 22 |
| 正向查询 | 14 | 14 |
| 无匹配查询 | 8 | 8 |
| 正向 Hit Rate@1 | 14/14（100.0%） | 14/14（100.0%） |
| 正向 Hit Rate@3 | 14/14（100.0%） | 14/14（100.0%） |
| Macro Recall@1 | 78.6% | 78.6% |
| Macro Recall@3 | 100.0% | 100.0% |
| Precision@3 | 52.4% | 52.4% |
| MRR | 1.000 | 1.000 |
| 无匹配真阴性 | 7/8 | 8/8 |
| 无匹配误召回 | 1/8 | 0/8 |
| 无匹配特异度 | 87.5% | 100.0% |

修改前唯一无匹配误召回是 V3-N02，返回 `KB-DRAFT-DEMO-0008`，分数 36；修改后该样本返回空结果。V3-S08 与 V2-R15、V2-R16 仍召回采购 FAQ。没有更改标签以改善指标。

逐样本 JSON：[`retrieval-v4-before.json`](retrieval-v4-before.json)、[`retrieval-v4-after.json`](retrieval-v4-after.json)。

## 测试记录

以下 Maven 命令均在 `backend` 目录执行，并使用离线模式。

| 阶段 | 命令 | 退出码 | 结果 |
|---|---|---:|---|
| RED | `mvn -o '-Dtest=RetrievalEvaluationV4MetadataRefinementH2IntegrationTest' test` | 1 | 1 项测试、1 个预期断言失败、0 错误；N02 仍误召回采购 FAQ。结果 JSON 已保存。 |
| GREEN | 同一条 V4 定向测试 | 0 | 1 项通过、0 失败/错误/跳过；N02 无召回，S08/R15/R16 保留目标知识。 |
| V2/V3/V4 检索回归 | `mvn -o '-Dtest=RetrievalEvaluationServiceTest,RetrievalEvaluationH2IntegrationTest,RetrievalEvaluationV3H2IntegrationTest,RetrievalEvaluationV4MetadataRefinementH2IntegrationTest,HybridKnowledgeScorerTest' test` | 0 | 12 项通过、0 失败/错误/跳过。 |
| 完整后端回归 | `mvn -o test` | 0 | 99 项、0 失败、0 错误、1 项跳过；跳过项属于默认关闭的模型评测。 |

日志：[`mvn-v4-before-red.log`](mvn-v4-before-red.log)、[`mvn-v4-after-green.log`](mvn-v4-after-green.log)、[`mvn-retrieval-v2-v4-regression.log`](mvn-retrieval-v2-v4-regression.log)、[`mvn-full-regression.log`](mvn-full-regression.log)。

### 测试产物保护

运行完整 Maven 测试前，`backend/target/evaluation` 有 27 个文件；测试期间一个旧 JSON 被重写，并生成一个随机命名测试结果。执行结束后将 27 个原文件从测试前快照逐一恢复，原文件 27/27 SHA-256 一致，缺失 0。测试新生成的文件先复制到任务交付目录留存，再从 `target/evaluation` 移除。逐文件测试前清单见 [`target-evaluation-before.sha256.json`](target-evaluation-before.sha256.json)；任务交付目录中的备份位于 `D:\面试冲刺包\_JOB_READY_SPRINT_20261021\deliverables\ticket-retrieval-v4-metadata-refinement-20261022\target-evaluation-before`。

## DeepSeek 状态与限制

本轮真实模型请求数为 0。执行时 `DEEPSEEK_API_KEY`、`AI_DEEPSEEK_API_KEY` 与 `COMMERCEFLOW_AI_API_KEY` 均不存在于进程环境；评测使用合成查询和 H2。

这次修正只说明演示知识库中一个泛化关键词造成的已知误召回被消除，不能说明其他词面误召回已解决，也不能外推为独立数据集、向量检索、DeepSeek 或生产系统表现。下一步应增加独立标注的查询和负例，再分别比较关键词、Embedding 与混合召回。

## 证据 SHA-256

| 文件 | SHA-256 |
|---|---|
| V4 数据集 | `9ADF65ED42712E104B11E82E437B1DAB8BD736A20AFE80CC98CEF4DDD10F7584` |
| 修改前逐样本结果 | `D79CD043EA5BBF6A9F7E0D21BDEDBC3E45034CFBA97F9DB80C5B79A6B545AB81` |
| 修改后逐样本结果 | `0259B25CC06F85645AF63F941469AB7124FFE670B0116421904EDDDF91EFDCD4` |
| 完整 Maven 日志 | `C68C5F29519153FB02B6F5A358B73C12386BA60FB844661BE6CE0E631A4F9AD0` |
