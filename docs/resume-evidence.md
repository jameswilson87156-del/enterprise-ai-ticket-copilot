# 简历证据说明

## 一句话定位

Enterprise Ticket RAG Copilot 是一个企业工单知识库智能助手，用 Spring Boot + Vue 实现工单分类、关键词 RAG Reference、OpenAI-compatible Provider fallback、Trace Evidence 证据链、JWT + RBAC demo 和 Human Review 人工确认的可演示闭环。

## 为什么适合写进 Java / AI 应用开发实习简历

这个项目不是单纯 CRUD，也不是只做一个聊天页面。它围绕企业支持场景，把工单状态机、后端分层接口、数据库表设计、生成记录、知识引用、人工确认和前端工作台串成一条可以讲清楚的数据链路。

对 Java 实习岗位，可以展示 Spring Boot Controller / Service / Mapper 分层、MyBatis-Plus 数据访问、Bean Validation、统一异常处理、OpenAPI 文档、H2 集成测试和 demo RBAC。对 AI 应用开发方向，可以展示如何用 `local-rule fallback` 保证本地可运行，同时预留 OpenAI-compatible Provider 路径，并把 RAG Reference、generation record、fallbackReason 和 Human Review 证据展示出来。

## 技术亮点

- Spring Boot 后端接口设计：覆盖工单创建、队列查询、详情查询、规则分析、状态流转、知识草稿和 Trace Evidence 只读聚合。
- 工单状态流转：通过 `ticket_status_history` 记录待处理、处理中、已解决、已沉淀等人工确认后的状态变化。
- Trace Evidence 只读聚合接口：`GET /api/tickets/{id}/trace-evidence` 聚合已有业务表，不新增自动处理动作。
- OpenAI-compatible Provider 路径：通过临时环境变量配置 `/chat/completions`，未配置 Key 或失败时记录 fallback 并回退本地规则。
- JWT + RBAC demo：`ADMIN / AGENT / REVIEWER / VIEWER` 角色保护 run-copilot 和 review 操作。
- RAG Reference 证据链：展示知识标题、来源路径、命中关键词、相关度、片段和是否用于回复草稿。
- Human Review 机制：支持 Approve / Request Changes / Reject，状态流转、知识发布和处理结论都保留人工确认边界，避免无人值守自动闭环。
- Vue 高保真企业 SaaS 工作台：左侧导航、顶部运行边界、工单队列、详情、Copilot / RAG / Human Review 面板和底部证据区。
- Playwright 真实截图：`npm run screenshots` 通过真实浏览器生成 `docs/images/` 下的项目展示图。

## 可写进简历的 bullet

- 基于 Spring Boot 3 + MyBatis-Plus 设计企业工单辅助处理后端，完成工单创建、状态流转、知识草稿和统一错误响应等 REST API。
- 设计 `GET /api/tickets/{id}/trace-evidence` 只读聚合接口，整合 `ticket_ai_analysis`、`generation_record`、`ticket_status_history` 和 `knowledge_article`，用于展示生成记录、RAG 引用和人工确认证据链。
- 实现 OpenAI-compatible Provider 可选接入与 `local-rule fallback`，记录 provider/model/latency/fallbackReason，保证无 API Key 时仍可运行。
- 实现 JWT + RBAC demo 与 Human Review 审核接口，覆盖 `ADMIN / AGENT / REVIEWER / VIEWER` 角色和 Approve / Request Changes / Reject 状态闭环。
- 使用 Vue 3 + TypeScript 构建企业 SaaS 风格工单工作台，展示工单队列、详情、Trace 摘要、Generation Record、RAG Reference 和 Human Review 状态。
- 补充 H2 集成测试、Controller / Service 单元测试、Swagger / OpenAPI 文档和 GitHub Actions CI，后端 `mvn test` 覆盖 21 个测试用例。
- 使用 Playwright 截图脚本生成真实浏览器截图，并在 README 中作为作品集展示证据。

## 面试官可能追问

### 这是不是真实大模型？

默认不是。当前默认 Provider 是 `local-rule`，Model 显示为 `N/A (no LLM)`。如果本地临时配置 `TICKET_AI_*` 和 API Key，后端会调用 OpenAI-compatible `/chat/completions`；未配置或失败时自动 fallback。分类来自本地规则，知识匹配来自关键词评分，这样做是为了让演示项目在本地稳定运行，并且能解释每个建议的来源。

### 有没有向量数据库？

没有。当前没有 embedding，也没有向量数据库。RAG Reference 在这个项目里表示“知识引用展示”：通过关键词和分类匹配到知识条目，再展示标题、来源路径、命中关键词、相关度和片段。后续如果接向量库，需要新增真实检索链路和验证，不能把当前版本说成已经实现。

### Trace Evidence 怎么来的？

Trace Evidence 来自只读聚合接口 `/api/tickets/{id}/trace-evidence`。真实数据来源包括 `ticket_ai_analysis`、`generation_record`、`ticket_status_history` 和 `knowledge_article`。其中 `analysisId`、`recordId`、`latencyMs`、`status`、`promptSummary`、`responseSummary`、状态历史和知识条目都来自已有表或服务逻辑。

### 哪些字段是派生的？

`runId` / `traceId` 是基于工单号派生的展示标识，不是分布式 Trace / Span Runtime。`currentStep` 由工单状态映射，`totalLatency` 是 `generation_record.latency_ms` 求和，`Human Review` 是从状态历史中的人工 actor 推导，不是独立审核任务表。

### Human Review 怎么保证安全？

当前项目的安全边界是“人机协作，不自动执行”。系统只给出分类、知识引用和回复草稿；状态流转、处理结论、对外回复和知识发布都需要支持人员或知识负责人手动确认。项目没有自动授权、回滚、重启、通知外部系统或自动关闭工单的能力。

### 为什么不做全自动关闭工单？

因为企业工单通常涉及权限、资金、系统稳定性和用户影响，自动关闭会带来误判风险。这个项目选择 Human-in-the-loop 设计，把 Copilot 定位为辅助判断和整理证据，而不是替代人工决策。

### 这个项目和普通 CRUD 有什么区别？

普通 CRUD 只展示数据增删改查；这个项目有清晰的业务流程和证据链：工单进入后会触发规则分类、知识匹配、模板草稿、生成记录、状态历史、RAG Reference 和人工确认。面试时可以沿着一张工单讲清楚数据如何流过后端服务、数据库表、Trace Evidence 接口和前端工作台。

### local-rule fallback 的意义是什么？

它让项目在没有外部模型、API Key 或付费服务的情况下仍然可运行、可测试、可解释。本轮已经实现真实 Provider 代码路径和审计字段，但没有使用真实 Key 验证；如果后续完成真实调用，需要单独记录验证结果。

## 项目边界说明

- 当前是作品集和学习项目，不是已上线生产系统。
- 默认没有真实 LLM 调用；OpenAI-compatible Provider 代码路径已实现，本轮未使用真实 Key 验证。没有模型训练、embedding 或向量数据库。
- 当前没有完整 Multi-Agent Runtime、Tool Runtime 或自动规划执行链。
- 当前 JWT + RBAC 是 demo 级权限控制，不是生产级鉴权、审计登录、限流、脱敏、监控或 SLA。
- 当前不会无人值守自动处理、自动回复或自动关闭工单。
- 当前截图和演示数据均为本地构造，不包含真实企业或客户信息。
