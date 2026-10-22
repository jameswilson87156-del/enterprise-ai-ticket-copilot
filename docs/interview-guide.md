# 面试讲解指南

## 项目一句话

Enterprise AI Ticket Copilot 是一个 Java + Vue 的企业内部工单辅助处理工作台。它默认用本地规则分类，也提供显式开启的 OpenAI-compatible 模型分类；知识库仍按分类和关键词评分匹配，建议草稿保留人工确认门禁。

这里的 Copilot 是产品名，表示“面向客服 / 运维人员的辅助处理工作台”，不代表生产级无人值守自动处理。

## 可以突出什么

- 后端：Spring Boot 3、Java 17、MyBatis-Plus、MySQL 五表闭环。
- 工程质量：Bean Validation、全局异常处理、Swagger/OpenAPI、知识发布幂等保护；当前本地回归为后端 328 项、前端 7 项、Python sidecar 8 项通过。
- 前端：Vue 3 + Vite 双列 Copilot 工作台，不是普通 CRUD 表格后台。
- 可控模型分类：默认关闭；设置 `TICKET_AI_CLASSIFICATION_ENABLED=true` 且 Provider 配置齐全后才额外请求模型，坏 JSON、非法类别、危险输出或 Provider 失败都会回退本地规则。
- 安全回归：用本地 HTTP stub 和合成输入验证模型分类解析、类别白名单、置信度范围、提示注入过滤、canary 回显拒绝和规则降级；这不是新的 DeepSeek 真实评测结果。
- 安全边界：不自动授权、不自动回滚、不调用业务外部系统；模型分类需要单独显式开启，所有建议保留“待人工确认”。
- 产品闭环：提交工单 -> 规则辅助分类 -> 知识命中 -> 建议草稿 -> 人工处理 -> 状态历史 -> 知识草稿 -> 人工确认发布。

## 技术问答准备

### 面试官问：你这个 AI 是怎么实现的？

标准回答：

本项目默认用规则引擎分类，不会自动调用模型。需要实验模型分类时，显式启用开关后复用已有 OpenAI-compatible Provider；程序校验类别、置信度和输出风险，失败就回退本地规则。知识匹配仍用分类和关键词评分，模板草稿以及模型建议都必须经过人工确认。当前主干的新增分类回归使用本地 HTTP stub，没有因此产生真实模型评测数据。

可以补充：

- 分类门面在 `AiClassificationFacade`；规则逻辑在 `RuleClassificationService`，可选模型解析与输出校验在 `LlmClassificationService`。
- 知识匹配逻辑在 `KnowledgeMatchingService`。
- 建议草稿逻辑在 `RecommendationTemplateService`。
- 审计记录保存在 `generation_record`，状态流转保存在 `ticket_status_history`。

### 面试官问：为什么不用大模型？

标准回答：

默认规则路径便于可解释、可复现地演示工单闭环，也避免没有显式配置时把工单数据发送到外部。要验证模型分类时，必须开启独立开关并配置 Provider；工单字段会发到该 Provider，因此要先确认数据边界。模型类别与输出经过校验，失败回退规则；人工确认与审计链路保持不变。

### 面试官问：知识匹配是不是向量检索？

标准回答：

不是。当前没有 embedding，也没有向量数据库。知识匹配是基于工单分类、标题、描述、错误日志和知识条目关键词的评分公式。这样实现简单、可解释，面试时可以直接说明命中原因。后续如果要扩展向量检索，会作为新的 V2 能力重新设计，不会把当前版本说成已经实现。

### 面试官问：这个系统能不能生产使用？

标准回答：

当前仓库是个人作品集 / 实习简历项目，不是生产系统。它有可选 OIDC/JWT 与前端 PKCE、后端角色策略、CI 和 staging 配置骨架；真实 IdP、云端部署、生产监控告警、备份恢复和组织级数据脱敏仍未完成验收。因此我会把它定位为“有工程化部署骨架的企业工单工作流原型”，不夸大成已生产上线。

### MySQL 表怎么设计？

- `support_ticket`：工单主表。
- `knowledge_article`：知识库文章和草稿。
- `ticket_ai_analysis`：规则或模型辅助分类、置信度、知识命中和建议草稿。
- `generation_record`：规则/模板生成记录，方便审计。
- `ticket_status_history`：状态流转历史。

### 前端为什么做成 Copilot 工作台？

目标用户是内部支持人员和运维，不需要营销落地页，也不适合聊天页。双列主工作区将工单队列与入口、工单上下文与建议草稿分组展示，并把知识沉淀和状态历史放在后续区域，适合高频处理和演示。

## 可展示的接口

- `POST /api/tickets`
- `GET /api/tickets`
- `GET /api/tickets/{id}`
- `GET /api/tickets/{id}/ai-analysis`
- `POST /api/tickets/{id}/status`
- `POST /api/tickets/{id}/knowledge-draft`
- `POST /api/tickets/knowledge/{articleNo}/confirm`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`

## V2 方向

- 对可选模型分类增加独立留出集和跨 Provider 评测；向量检索或 MCP 需先单独设计鉴权与数据边界。
- 更精细的关键词权重、分类权重和人工采纳率统计。
- 多角色审核流。
- 操作审计与权限控制。
- SLA、告警联动和报表分析。
- 但仍保留人工确认，不做自动生产变更。
