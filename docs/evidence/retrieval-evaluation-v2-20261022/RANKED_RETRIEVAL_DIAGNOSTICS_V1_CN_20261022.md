# Ticket Copilot 检索排序诊断 V1

日期：2026-10-22  
结论：**新增排序与问法分层诊断；没有新增或改写原始样本标签。**

## 数据与计算范围

- 输入是既有 `knowledge-retrieval-v2-h2.json`，数据集版本 `v2-full-corpus-qrels`：14 篇固定演示知识文章、18 条有 qrels 的正向问题、4 条无匹配控制。
- 使用原报告每条样本的 `relevantArticleNos` 作为二元相关性 qrels，以及实际返回的文章顺序；分析脚本只读取并派生指标，不调用模型、不更改原始评测结果。
- 新增 `binaryNdcgAt3`：取前 3 篇文章，相关文档增益为 1、不相关为 0，按排名位置折损，再除以该问题理想排序的 DCG。脚本和机器结果分别见 `analyze_ticket_retrieval_v2.py` 与 `derived-ranking-diagnostics-v1.json`。
- 原始结果 SHA-256：`362B88424A81C97884B95AFBB7F24772A21E705BD47173696BA8EA653FDED2EB`。

## 实际结果

| 查询分组 | 样本 | Hit@1 | Recall@3 | MRR | binary nDCG@3 |
|---|---:|---:|---:|---:|---:|
| 全部正向问题 | 18 | 14/18（77.8%） | 72.2% | 0.7778 | 0.7348 |
| direct | 8 | 8/8（100%） | 100% | 1.0000 | 1.0000 |
| paraphrase | 8 | 4/8（50%） | 37.5% | 0.5000 | 0.4033 |
| hard_negative | 2 | 2/2（100%） | 100% | 1.0000 | 1.0000 |
| no_match 控制 | 4 | — | — | — | specificity 3/4（75%） |

no_match 的误召回是 N01，返回 `KB-PROCESS-005`。正向漏召回为 R04、R06、R08、R14；多相关问题 R10、R12 只召回部分 qrels。分组和失败样本均保留，没有通过改标签、删样本或调整分母提高指标。

## 解读与限制

结果显示当前关键词基线能处理语义显式的 direct 问法，但对 paraphrase 问法的命中明显下降。nDCG@3 补充了 Hit@K 对前几位排序质量的诊断；它不会证明生成答案忠实、模型回答正确或系统具备语义检索能力。样本只有 18 条正向问题和 4 条无匹配控制，且在开发过程中已被查看，因此这是一份描述性诊断，不是独立 holdout、生产准确率或置信区间。

目前不继续往已查看的 V2 集合里塞改写问题来抬高分数。下一轮应保留 V2 为可追溯的已知集，并另建人工复核的 frozen holdout，覆盖查询改写、术语缩写、跨文章多相关、相似主题误召回和无答案拒答；只有相同语料、相同 qrels、相同运行模式下，才能做模型或检索方案对比。

## 公开方法参考

- [RAGAS Context Precision](https://github.com/vibrantlabsai/ragas/blob/main/docs/concepts/metrics/available_metrics/context_precision.md)：从检索上下文排序与相关性评估检索质量。本报告只复用了“检索阶段需要独立测量”的思路，没有运行 RAGAS 或 LLM judge。
- [Järvelin 与 Kekäläinen 的 cumulated gain 检索评测论文](https://doi.org/10.1145/582415.582418)：排序折损累计增益指标的原始研究来源。本报告使用二元 qrels，因此相关性 gain 为 0/1。
- [DeepEval 开源评测框架](https://github.com/confident-ai/deepeval)：公开示例以固定样本和逐项指标评价 LLM 应用。本项目本轮继续使用已有 Java/Python 离线评测链路，没有新增 DeepEval 依赖，也没有把 LLM judge 分数混入确定性检索指标。
- [OWASP LLM01:2025 Prompt Injection](https://genai.owasp.org/llmrisk/llm01-prompt-injection/) 与 [LLM02:2025 Sensitive Information Disclosure](https://genai.owasp.org/llmrisk/llm022025-sensitive-information-disclosure/)：为红队威胁分类提供参考；本轮攻击/控制结果仍是规则或 MOCK 契约回归，不是 OWASP 认证或真实模型安全率。

## 复现

从执行根目录运行：

```powershell
py -3.12 .\deliverables\analyze_ticket_retrieval_v2.py `
  --input .\worktrees\ticket-repair\docs\evidence\retrieval-evaluation-v2-20261022\knowledge-retrieval-v2-h2.json `
  --output .\worktrees\ticket-repair\docs\evidence\retrieval-evaluation-v2-20261022\derived-ranking-diagnostics-v1.json
```

实际本地报告：`D:\面试冲刺包\_JOB_READY_SPRINT_20261021\worktrees\ticket-repair\docs\evidence\retrieval-evaluation-v2-20261022\derived-ranking-diagnostics-v1.json`。
