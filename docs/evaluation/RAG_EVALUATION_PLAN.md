# RAG Evaluation Plan

本文记录 Enterprise Ticket RAG Copilot 的最小 RAG / Citation / Trace Evaluation 方案。当前评测只使用本地 synthetic demo 数据集和关键词检索脚本，不连接真实 Provider，不使用真实 API Key，不声明真实向量检索或真实模型效果提升。

## 1. 场景痛点

企业工单 Copilot 的难点不是“生成一段看起来像答案的回复”，而是让支持人员能判断：

- 工单是否命中了正确知识。
- 建议草稿是否引用了可追溯来源。
- 高风险动作是否进入 Human Review。
- 没有知识命中时是否诚实 fallback，而不是编造来源。
- Trace Evidence 是否能解释 Provider、fallback、citation 和人工审核边界。

因此，本项目需要一个能在本地复现的最小评测闭环，用来证明当前 demo 的 RAG Reference、Citation 和 Review Gate 是可解释的。

## 2. 为什么企业 RAG 工单助手需要评测

工单场景通常涉及权限、生产故障、数据修复、SLA、客户影响和知识沉淀。没有评测时，项目很容易停留在“页面像 AI 产品”或“后端能返回一段建议”的层面。

本轮评测优先回答三个问题：

- 检索是否找到了预期知识：例如 Redis timeout 是否命中 Redis 连接池知识。
- 引用是否可靠：答案里带的 citation 是否属于期望知识来源。
- 风险是否被拦截：P1、权限、生产回滚、缺知识等场景是否进入人工复核。

## 3. 自建评测集设计

评测集路径：

```text
data/eval/ticket_rag_eval_cases.jsonl
```

当前包含 16 条 synthetic enterprise ticket demo cases，不包含真实企业数据、真实用户数据或真实客户信息。样本覆盖：

- 登录失败 / SSO / MFA。
- 权限不足 / RBAC / 财务共享盘。
- 数据同步延迟 / 报表口径。
- MySQL 慢查询。
- Java 端口占用。
- Spring Boot BeanCreationException。
- Redis timeout。
- 接口 500 / TraceId。
- 部署环境变量缺失。
- 流程咨询转知识库 FAQ。
- SLA / P1 响应。
- 生产回滚等高风险动作。
- 缺知识时应该 fallback。
- 登录页 500 这类容易被关键词误判的相似问题。

## 4. 样本字段说明

每条 JSONL 样本包含：

| 字段 | 说明 |
| --- | --- |
| `id` | 评测样本编号 |
| `category` | 期望业务分类，复用当前 demo 分类口径 |
| `priority` | P1 / P2 / P3 |
| `user_question` | 用户问题 |
| `ticket_context` | 标题、描述、系统、日志和业务上下文 |
| `expected_knowledge_ids` | 期望命中的知识条目；空数组表示当前知识库应 fallback |
| `expected_keywords` | 期望 Top-K 上下文覆盖的关键词 |
| `expected_answer_points` | 期望答案应该覆盖的要点，用于后续真实模型评测 |
| `evaluation_dimensions` | 该样本重点评测维度 |
| `baseline_expected` | 是否需要 citation、Human Review 或 fallback |
| `failure_reason_if_miss` | 未命中时的风险解释 |
| `notes` | demo / synthetic 边界说明 |

## 5. 第一阶段指标定义

当前脚本路径：

```text
scripts/evaluate_rag_demo.py
```

第一阶段只实现不依赖真实模型的指标：

| 指标 | 定义 | 当前是否实现 |
| --- | --- | --- |
| Top-K Hit Rate | 有期望知识 ID 的样本中，Top-K 检索结果是否至少包含一个期望 ID | 已实现 |
| Context Recall@K | 期望关键词在 Top-K 知识上下文中的覆盖比例 | 已实现 |
| Citation Coverage | 需要引用的样本中，模拟答案是否附带 citation IDs | 已实现 |
| Citation Precision | citation IDs 中属于期望知识 ID 的比例 | 已实现 |
| Retrieval Latency | 本地内存关键词评分耗时 | 已实现 |
| Fallback Rate | 无知识命中或 Provider fallback 的比例 | 已实现并拆分 provider / knowledge miss |
| Failed Case Count | 检索未命中、引用错误、缺少 citation 或 fallback 引用错误的样本数 | 已实现 |
| Human Review Required Count | 高风险、低证据、缺知识或数据集要求人工复核的样本数 | 已实现 |
| MRR | 期望知识首次出现位置的倒数排名 | 可选，已实现 |
| NDCG@K | Top-K 二值相关性排序指标 | 可选，已实现 |

## 6. 暂不实现的真实模型指标

以下指标不能在当前 demo/local-rule 结果中写成真实效果：

- Answer Relevance。
- Faithfulness。
- Hallucination Case Count。
- Token Cost。
- Prompt v1 vs Prompt v2 效果提升。
- 真实 Provider 的稳定响应质量。

这些指标必须等真实 Provider 小规模测试、有保存输入输出、有人工标注或 LLM-as-judge 方案后再写。

## 7. Baseline 设计

当前版本没有 BM25、embedding、Vector DB、Hybrid、Rerank。因此本轮 baseline 只覆盖本地 demo keyword retrieval 与 citation gating：

| Baseline | 说明 |
| --- | --- |
| `keyword_only` | 当前分类感知关键词检索，不强制答案带 citation |
| `naive_keyword_score` | 只按关键词计分，不使用分类加权 |
| `with_citation_required` | 分类感知关键词检索，并在模拟答案中附 citation IDs |
| `with_human_review_gate` | citation required + 高风险或低证据样本进入 Human Review |

BM25 / Vector / Hybrid / Rerank 只作为下一阶段实验，不在当前结果中声称。

## 8. 本地评测命令

在项目根目录运行：

```powershell
python .\scripts\evaluate_rag_demo.py
```

也可以指定 Top-K：

```powershell
python .\scripts\evaluate_rag_demo.py --top-k 3
```

脚本输出：

```text
docs/metrics/rag_metrics_latest.json
docs/metrics/rag_metrics_snapshot.md
```

脚本只读 `data/eval/ticket_rag_eval_cases.jsonl`，不连接数据库，不调用外部 API，不读取环境变量中的 Key。

## 9. 当前版本边界

- 当前评测集是 synthetic demo dataset，不是真实企业线上数据。
- 当前检索是关键词评分，不是 BM25，也不是 embedding / Vector DB。
- 当前答案是 simulated local-rule answer，只用于 citation gating 检查。
- Provider fallback 在本地 no-key 运行中预期为 100%，不能解释为模型质量指标。
- Human Review 是 demo gate，不是生产级审核任务平台。
- Trace Evidence 是聚合展示和证据字段设计，不是完整分布式 Trace / Span Runtime。

## 10. 下一阶段真实模型评测计划

下一阶段可以独立推进：

1. 固定 20 到 30 条评测样本，补人工期望答案。
2. 临时配置真实 OpenAI-compatible Provider，仅使用环境变量，不提交 Key。
3. 保存 provider 输入摘要、输出摘要、citation IDs 和人工判定。
4. 加入 Answer Relevance、Faithfulness、Hallucination Case Count。
5. 比较 prompt v1 / prompt v2，但只写有来源的实验结果。
6. 如果新增 BM25 / Vector / Hybrid / Rerank，再增加同一评测集上的 baseline 对比。

## 11. 简历和面试解释口径

推荐表达：

> 基于 16 条自建企业工单 demo 评测集，完成 keyword retrieval + citation gating 的本地评测，统计 Top-K Hit Rate、Context Recall@K、Citation Coverage、Citation Precision、Retrieval Latency 与 Human Review gate，用于验证 RAG Reference 和 Trace Evidence 的可解释性。

面试中应主动说明：

- 这是第一阶段本地评测，不是生产效果。
- 指标来自 demo 数据集和本地脚本，可复现。
- 当前没有真实向量检索，RAG 在本项目里表示知识引用与证据展示。
- 高风险工单不会自动处理，必须进入 Human Review。

不能说：

- “准确率 99%”。
- “向量 RAG 已上线”。
- “Prompt 优化提升 80%”。
- “真实模型已稳定接入生产”。
- “服务真实企业用户”。
