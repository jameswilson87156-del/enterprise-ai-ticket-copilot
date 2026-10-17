# 面试讲解指南

## 项目一句话

Enterprise AI Ticket Copilot 是一个 Java + Vue 的企业内部 AI 工单指挥台，用本地规则和知识库匹配完成工单分类、处理建议、状态流转和知识沉淀，并且所有 AI 建议必须经过人工确认。

## 可以突出什么

- 后端：Spring Boot 3、Java 17、MyBatis-Plus、MySQL 五表闭环。
- 前端：Vue 3 + Vite 三栏工作台，不是普通 CRUD 表格后台。
- AI 设计：当前不接真实 LLM，通过规则分类、关键词匹配和模板生成模拟企业可控 AI 流程。
- 安全边界：不自动授权、不自动回滚、不调用外部系统，所有建议保留“待人工确认”。
- 产品闭环：提交工单 -> 分类 -> 知识命中 -> 建议生成 -> 人工处理 -> 状态历史 -> 知识草稿 -> 人工确认发布。

## 技术问答准备

### 为什么第 2 阶段不接真实 LLM？

因为这个项目先验证业务闭环和人机协同边界。企业内部工具最重要的是可审计、可回退、可解释。规则引擎和模板先把流程打通，V2 再接私有模型或 RAG，风险更可控。

### MySQL 表怎么设计？

- `support_ticket`：工单主表。
- `knowledge_article`：知识库文章和草稿。
- `ticket_ai_analysis`：规则分类、置信度、知识命中和建议内容。
- `generation_record`：规则/模板生成记录，方便审计。
- `ticket_status_history`：状态流转历史。

### 如何体现 AI Agent 应用？

它不是完全自动执行的 Agent，而是受控的 Copilot。系统完成“理解问题、检索知识、生成建议、准备知识草稿”的认知任务，执行动作交给人工确认。

### 前端为什么做成指挥台？

目标用户是内部支持人员和运维，不需要营销落地页，也不适合聊天页。三栏布局可以同时看到队列、当前工单上下文和 AI 建议，适合高频处理和演示。

## 可展示的接口

- `POST /api/tickets`
- `GET /api/tickets`
- `GET /api/tickets/{id}`
- `GET /api/tickets/{id}/ai-analysis`
- `POST /api/tickets/{id}/status`
- `POST /api/tickets/{id}/knowledge-draft`
- `POST /api/tickets/knowledge/{articleNo}/confirm`

## V2 方向

- 企业私有 LLM 或本地 RAG 检索。
- 向量库知识匹配与重排。
- 多角色审核流。
- 操作审计与权限控制。
- SLA、告警联动和报表分析。
- 但仍保留人工确认，不做自动生产变更。
