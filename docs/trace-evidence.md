# Trace Evidence 说明

本文记录 Enterprise Ticket RAG Copilot 的工单证据链展示口径。目标是让工作台能展示真实可追溯字段，同时不把当前项目包装成生产级 LLM、向量数据库或完整 Multi-Agent Runtime。

## 只读接口

`GET /api/tickets/{id}/trace-evidence`

`{id}` 是工单编号 `ticketNo`，例如 `DEMO-0005` 或 `TCK-...`。接口只读取已有业务表并聚合返回，不新增数据库表，不执行自动处理动作，也不触发外部工具调用。

## 聚合来源

| 数据源 | 当前用途 | 真实字段示例 |
| --- | --- | --- |
| `support_ticket` | 工单基础信息和当前状态 | `ticket_no`、`status`、`priority`、`category` |
| `ticket_ai_analysis` | 规则引擎辅助分析结果 | `id`、`classification`、`confidence`、`confirmation_state`、`created_at` |
| `generation_record` | 记录规则分类、关键词匹配和模板草稿的生成摘要 | `id`、`business_type`、`business_id`、`source_type`、`input_summary`、`output_summary`、`latency_ms`、`status`、`created_at` |
| `ticket_status_history` | 记录人工确认后的状态流转 | `from_status`、`to_status`、`actor`、`note`、`occurred_at` |
| `knowledge_article` | 提供关键词知识引用和片段 | `article_no`、`title`、`keywords`、`content`、`source_ticket_id` |

## 真实接口数据字段

以下字段来自后端真实表或现有服务逻辑：

- `ticketId`：来自 `support_ticket.ticket_no`。
- `aiAnalysis.analysisId`：来自 `ticket_ai_analysis.id`。
- `aiAnalysis.createdAt`：来自 `ticket_ai_analysis.created_at`。
- `generationRecords[].recordId`：来自 `generation_record.id`。
- `generationRecords[].businessType`：来自 `generation_record.business_type`。
- `generationRecords[].sourceType`：来自 `generation_record.source_type`。
- `generationRecords[].promptSummary`：来自 `generation_record.input_summary`。
- `generationRecords[].responseSummary`：来自 `generation_record.output_summary`。
- `generationRecords[].latencyMs`：来自 `generation_record.latency_ms`。
- `generationRecords[].status`：来自 `generation_record.status`。
- `generationRecords[].createdAt`：来自 `generation_record.created_at`。
- `statusHistory[]`：来自 `ticket_status_history`。
- `ragReferences[].knowledgeTitle`、`sourcePath`、`snippet`：来自 `knowledge_article`。

## 安全派生字段

以下字段不是独立生产系统字段，需要按当前 demo 边界理解：

- `runId` / `traceId`：由工单号派生，格式为 `RUN-{ticketNo}` / `TRACE-{ticketNo}`，用于 UI 关联展示，不代表分布式 Trace / Span Runtime。
- `traceMode`：标记当前 trace 来源是 ticket records。
- `currentStep`：由当前工单状态映射。
- `totalLatency`：对当前工单关联的 `generation_record.latency_ms` 求和。
- `aiAnalysis.provider`：固定说明为 `local-rule fallback`。
- `aiAnalysis.model`：固定说明为 `N/A (no LLM)`。
- `fallbackStrategy`：由 `generation_record.source_type` 映射为 rule classification、keyword reference 或 rule template draft。
- `ragReferences[].relevanceScore`：后端通过现有 `KnowledgeMatchingService` 按关键词重新计算，历史表中未单独持久化每次命中的 score。
- `ragReferences[].matchedKeyword`：从 `knowledge_article.keywords` 与当前工单文本交集推导；如果历史关键词没有保留，则显示安全空状态。
- `ragReferences[].usedInDraft`：根据当前知识命中和模板草稿上下文推导，用于说明是否被建议草稿引用。
- `humanReview`：从 `ticket_status_history` 中非 System / Rule Engine / Rule Template 的 actor 推导，不是独立审核任务表。

## generation_record 当前边界

`generation_record` 目前用于记录本地规则和模板输出的审计摘要，不是大模型调用日志。

- 当前没有真实 `provider` 字段；前端和接口统一展示为 `local-rule fallback`。
- 当前没有真实 `model` 字段；接口展示为 `N/A (no LLM)`。
- 当前没有独立 `error_message` 列；成功记录的 `errorMessage` 为空，失败时只能返回当前 schema 边界说明。
- 当前 `promptSummary` / `responseSummary` 是输入摘要和输出摘要，不是完整 prompt 或完整模型响应。

## Demo fallback

前端 Demo 模式的 `trace-evidence` fallback 集中在 `frontend/src/data/demoTickets.ts`，由当前 demo 工单、analysis 和 timeline 派生，不对应真实数据库行。它只用于无需后端即可展示工作台，不应写成生产接口返回。

## 明确不能夸大的能力

- 当前没有真实 LLM 调用。
- 当前没有 embedding 或向量数据库检索。
- 当前没有 Tool Runtime。
- 当前没有完整 Multi-Agent Runtime。
- 当前没有生产级权限、审计登录或 RBAC。
- 当前不会无人值守自动关闭工单。
- 当前 `Human Review` 是人工状态流转与知识发布边界，不是完整审核任务中心。

## 后续可扩展方向

后续如果继续增强，应作为独立任务处理并补充真实实现与验证：

- 将 `generation_record` 增加 `error_message`、`model_name`、`provider` 等真实字段。
- 将知识匹配 score 持久化到分析记录或独立引用表。
- 增加独立 Human Review 表，用于记录 reviewer、decision、comment 和 reviewed_at。
- 在保持人工确认边界的前提下评估真实 Provider 调用或向量检索。
- 单独处理 Docker Compose，提供 MySQL + 后端本地演示环境。
