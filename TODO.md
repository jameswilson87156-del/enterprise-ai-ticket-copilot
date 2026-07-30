# Enterprise AI Ticket Copilot TODO 路线图

> 本文件用于后续 Claude / Codex 本地协作交接。每轮只处理一个明确、可验收的小任务；不要把未验证能力写成已完成，也不要把规则引擎包装成真实大模型。

## 1. 项目定位

本项目是一个企业内部工单辅助处理系统，适合作为大三实习简历的第二项目。推荐定位为：

- Spring Boot 3 + Vue 3 企业工单工作流系统
- 规则引擎辅助分类系统
- 企业内部支持团队的工单流转、知识匹配、建议草稿和人工确认演示项目

当前项目可以强调工程闭环、分层架构、数据表设计、状态流转、审计记录和前端交互演示；不要写成真实大模型项目、生产级 AI 平台或完整 RAG 系统。

## 2. 当前已完成能力

以下能力应以仓库源码、配置、文档或截图为准：

- Spring Boot 3.3.5 后端分层架构。
- Controller / Service / Mapper / Entity / DTO 分层。
- MyBatis-Plus 数据访问。
- schema.sql、Flyway migration 与 demo-data.sql 提供业务表和演示数据；当前包含 immutable run、retrieval hit、structured result/citation 与 review record。
- 工单状态机流转，包含待分类、待处理、处理中、已解决、已沉淀等状态。
- ticket_status_history 和 generation_record 审计链。
- Bean Validation 参数校验。
- 规则引擎分类。
- 知识库匹配评分。
- 推荐内容由模板生成，并要求人工确认。
- Vue 3 + TypeScript 前端组件。
- 前端 Demo 页面保留；Trace Timeline 另有经过既有 JWT 鉴权的 Real Run Evidence 只读入口，明确区分 `IMMUTABLE_RUN`、`LEGACY_DERIVED` 与 demo sample。
- Playwright 截图脚本和截图存档。
- SpringDoc OpenAPI / Swagger UI 接口文档。
- GitHub Actions CI workflow 已补充，当前顺序覆盖后端测试以及前端 `npm ci -> npm test -> npm run build`；本轮只验证本地 workflow 内容，未声称新的远端 run 已通过。
- docs/API.md 已补充人工整理版 REST API 文档，覆盖接口列表、请求响应、统一错误响应和业务边界。
- README、docs 与前端 UI 可见文案已校准 AI 相关表述，统一为规则引擎辅助分类、知识库评分匹配和模板化建议草稿。
- 前端作品集展示 UI 与 README 截图区已重做，主图突出企业工单辅助处理工作台，辅助图使用两列作品集布局展示。
- 2026-06-27 已按 Enterprise SaaS UI Design Skill 新建 `TicketWorkbenchShowcaseView.vue`：Tickets 默认显示独立 ShowcaseView，只使用本地 demo 常量，CSS 使用 `showcase-` scoped 前缀，不复用旧 TicketQueue / TicketDetailPanel / AiRecommendationPanel；`ticket-detail.png` 已刷新并人工验收通过。
- 后端当前 `mvn test` 覆盖单元与 H2 / 本地 HTTP stub 集成测试；2026-07-30 本轮实际结果为 86 tests、0 failures/errors/skipped。前端现有 6 个只读回放与安全错误处理测试。
- 本地 RAG / Citation / Trace Evaluation 最小闭环已补充，包含 `data/eval/ticket_rag_eval_cases.jsonl`、`scripts/evaluate_rag_demo.py`、`docs/evaluation/RAG_EVALUATION_PLAN.md`、`docs/metrics/rag_metrics_latest.json` 和 `docs/metrics/rag_metrics_snapshot.md`。
- 2026-07-04 已完成前端 Phase 1：统一深色 Showcase App Shell，升级 Dashboard 首页，新建 Evaluation / Metrics 页面，同步本地评测数据到 `frontend/src/data/evaluationMetrics.ts`，并刷新本项目真实运行截图；未使用第三方截图，未接真实 API Key。
- 2026-07-04 已完成 Evaluation / Metrics 视觉精修：页面产品名调整为“评测指标中心”，8 个核心 KPI 与 4 个补充指标分层展示，Baseline / 实验计划与右侧评测上下文产品化，并刷新本项目标准与 large 真实截图；所有指标继续限定为 synthetic demo dataset + local keyword retrieval + citation gating + local-rule fallback。
- 2026-07-04 已完成 Evaluation / Metrics 左上角 Sidebar 品牌图标修复：App Shell 品牌区恢复 44px 深色圆角容器 + 原创 ET/ticket inline SVG 标识，并只刷新 `evaluation-metrics` 标准与 large 真实截图；未修改 Evaluation 指标数据、RAG metrics 口径、README、后端或 `docs/frontend_reference*`。
- 2026-07-04 已完成前端 Phase 2：Ticket Workbench 升级为三栏企业工单 AI 处理工作台，首屏包含 Ticket Queue、Ticket Detail + AI Draft、Citation Evidence + Human Review + Trace；页面继续只使用 synthetic demo data、local-rule fallback、keyword retrieval 和 citation gating，不接真实 API Key，不修改后端逻辑，不更新 README。
- 2026-07-04 已完成前端 Phase 3：Trace Timeline 新增专用 Showcase 页面，首屏展示 Run Overview、Run List、Step Timeline、Step Detail、Retrieval / Citation Evidence、Provider skipped / local-rule fallback、Human Review gate 与 Raw JSON / Debug Detail；页面继续只使用 synthetic demo trace data，不接真实 API Key，不修改后端业务逻辑，不更新 README。
- 2026-07-04 已完成 GitHub README 作品集整合：README 首屏重新定位 Enterprise Ticket RAG Copilot，按 Dashboard、Ticket Workbench、Evaluation / Metrics、Trace Timeline、Knowledge Base、Human Review 顺序引用本项目真实截图，并补充项目定位、非普通 RAG demo 差异、Workflow、Evaluation / Metrics、Resume Bullets、Interview Talking Points 和 Honest Boundaries；本轮只改文档，不改前端/后端代码，不重新生成截图，不接真实 API Key，不引用 `.local/`、第三方截图或 Image2 图。
- 2026-07-04 已完成 README 发布后小修复：移除顶部外部 Portfolio Case Study 链接，避免跳转到可能滞后的作品集页面；Evaluation / Metrics 不再把 provider fallback 作为百分比质量指标展示，改为说明 no API key mode 下 expected local-rule fallback；Resume Bullets 和 Honest Boundaries 同步强调 optional provider path、synthetic demo dataset、keyword retrieval、citation gating 和 no real API key committed。
- 2026-07-04 已完成发布后 Sidebar 品牌图标视觉小修复：公共 App Shell 左上角品牌 mark 升级为 46px 原创 inline SVG ticket / ET / trace dot 标识，并刷新 README 当前使用的本项目真实截图及 large 版本；未修改 README 文案、后端代码、RAG metrics JSON，未使用第三方 Logo、第三方截图或 Image2 图，未接真实 API Key。

## 3. 当前不能夸大的能力

- 默认运行仍是 local-rule；仓库仅有 controlled synthetic real-provider smoke，不能写成生产稳定性、真实企业数据效果、SLA 或模型准确率。
- 当前没有真实 AI 模型训练。
- 分类是关键词规则引擎，不是机器学习模型。
- 知识匹配是评分公式，不是 embedding 向量检索。
- structured output 与 citation validation 只约束当前 run 的引用 ID，不证明句子级事实蕴含。
- knowledgeCoverage 已改为基于已有数据的真实知识关联率；不能再写成人为覆盖率、模型效果或向量检索能力。
- RAG Evaluation 结果只能写成本地 demo keyword retrieval + citation gating 的可复现指标，不能写成真实模型准确率、真实向量检索效果、Prompt 提升或生产效果。
- JWT + RBAC 仅是 demo 鉴权，不是生产级账号、权限、审计登录或合规体系。
- Human Review 是人工状态与记录闭环，不是生产级审核任务平台。

## 4. P0 待办

### P0-1：补全数据库连接配置或说明文档（已完成）

- 新增 backend/src/main/resources/application-local.yml.example，不包含真实密码。
- README 增加或校准 MySQL 本地启动说明，覆盖建库、复制配置、填写账号密码、启用 local profile、健康检查和工单列表验收。
- 注意：不要提交真实 application-local.yml、.env、密码、Token 或个人本地配置。
- 验收方式：
  - 复制 example 为本地配置文件后可启动后端。
  - GET /api/health 返回 200。
  - GET /api/tickets 返回演示工单数据。
- 本轮结果（2026-06-21，Codex）：复用并完善 backend/src/main/resources/application-example.yml，补充 README 本地 MySQL 启动说明，并补充 .gitignore 防止真实本地配置误提交。

### P0-2：修复 knowledgeCoverage 假指标（已完成）

- 将指标改成真实公式，例如“有知识命中的工单数 / 总工单数”。
- 如果无法定义真实口径，则删除该指标或改为更诚实的字段。
- 同步更新相关单元测试断言。
- 验收方式：
  - GET /api/tickets/metrics 返回的 knowledgeCoverage 可手工复核。
  - 相关测试通过。
- 本轮结果（2026-06-21，Codex）：新口径为“有知识库命中 matched_knowledge_nos，或有关联知识草稿/发布 source_ticket_id 的去重工单数 / 总工单数 * 100”。保留接口字段 knowledgeCoverage，但前端展示改为“知识关联率”。

### P0-3：补充测试执行证据（已完成）

- 运行 mvn test。
- 保存 docs/TEST_REPORT.md 或 docs/images/test-results.png。
- README 引用测试证据。
- 验收方式：
  - 测试报告或截图能看到真实执行结果。
  - 如果测试失败，不能把 TODO 标记为完成。
- 本轮结果（2026-06-21，Codex）：已新增 docs/TEST_REPORT.md，记录 backend 目录下的 mvn test 和 frontend 目录下的 npm run build 结果；README 已引用该报告。

## 5. P1 待办

- 添加 @ControllerAdvice / @RestControllerAdvice 全局异常处理器，统一 API 错误响应。（已完成：新增 ApiErrorResponse 和 GlobalExceptionHandler，统一参数校验与业务异常响应）
- 添加 SpringDoc OpenAPI / Swagger，方便展示接口文档。（已完成：新增 SpringDoc 依赖、OpenApiConfig 和 Swagger UI 访问说明）
- README 补齐或校准快速启动 MySQL 闭环步骤。
- README 中把容易误导的 AI 描述改为“规则引擎辅助分类”“模板化建议草稿”等真实表述。（已完成：README、architecture、product-design、demo-script、interview-guide 已统一口径）
- 前端 UI 可见文案已校准为规则引擎辅助分类、知识库评分匹配、模板化建议草稿。（已完成：App.vue、AiRecommendationPanel、Demo 数据与相关展示组件已统一口径）
- 补接口文档，例如 docs/API.md。（已完成：新增 docs/API.md，覆盖核心 REST API、DTO 字段、错误响应和边界说明）

## 6. P2 待办

- 增加 H2 内存库集成测试，验证 HTTP 到数据库的基础链路。（已完成：新增 `TicketWorkflowIntegrationTest`，使用 test profile + H2 覆盖创建工单、规则引擎辅助分析、状态流转和知识草稿确认）
- 增加 GitHub Actions CI，至少自动执行后端测试。（已完成：新增 `.github/workflows/ci.yml`，在 `push` / `pull_request` 时运行后端 `mvn test` 和前端 `npm run build`）
- 增加 Docker Compose，提供 MySQL + 后端的本地演示环境。
- 优化前端体验与截图素材，保证作品集展示统一。（已完成：重做首屏控制台视觉、三栏工作台布局、README 主图与两列辅助截图展示，并重新生成截图）
- 补充面试 Q&A 文档，说明项目边界、规则引擎取舍、状态机设计和后续可扩展方向。
- 如后续要比较 BM25 / Vector / Hybrid / Rerank，必须先实现真实链路并复用当前评测集跑出结果；不能提前写进当前能力。

## 7. 下一轮建议任务

下一轮如继续前端改造，建议从 Knowledge Base 或 Human Review 中只选一个页面做同等深度的首屏产品化，不要同时重写多个页面。

如果优先整理作品集对外展示，建议先在 GitHub 页面确认 README 截图、Mermaid 图和表格渲染效果，再决定是否补独立 `docs/SHOWCASE_GUIDE.md`；不要在同一轮继续改前端页面或重新截图。

如果回到后端/部署路线，建议处理 P2-3：增加 Docker Compose，提供 MySQL + 后端的本地演示环境。

不要同时处理 CI、Docker Compose、README 大改或其他 P1/P2 优化。下一轮仍然只做一个明确、可验收的小任务。

## 8. 任务记录格式

后续每个任务按以下格式追加或拆分：

- [ ] 任务名称

  - 优先级：
  - 背景：
  - 涉及文件：
  - 不可破坏：
  - 验收方式：
  - 建议 commit message：

- 2026-07-30 已完成后端 Trace Foundation：`run-copilot` 持久化 `copilot_run`、`retrieval_hit`、`review_record`，Trace Evidence 支持 `IMMUTABLE_RUN` 回放并保留 `LEGACY_DERIVED` 兼容路径；本阶段未修改前端、未接入向量检索、未新增 Citation Validation、未发起真实 Provider 请求。

### 2026-07-30 ? Codex ? PHASE_4_STRUCTURED_OUTPUT_CITATION_AND_ABSTENTION

- Added bounded structured Copilot output semantics with answer, citations, risk level, missing information, abstention code, and model/final human review fields.
- Added current-run Citation ID validation against immutable `retrieval_hit` snapshots and persisted validated citations separately from retrieval references.
- Added safe abstention for no retrieval evidence, invalid structured output, missing Citation, and invalid Citation; no-retrieval runs skip remote Provider calls.
- Added `copilot_result` and `copilot_result_citation` schema artifacts plus `V2__structured_output_citation_abstention.sql`; did not modify V1 migration.
- Added offline parser, citation, review-gate, persistence, and trace tests; real Provider requests remain disabled in automated tests.

### 2026-07-30 - PHASE_4C completion / backend freeze

- [x] Record sanitized synthetic real Provider structured-output smoke evidence in docs/evidence/real-structured-output-smoke-20260730.md and docs/evidence/real-structured-output-smoke-20260730.json.
- [x] Freeze Ticket Copilot backend core for portfolio use: no vector DB, multi-agent workflow, Responses Adapter, queue runtime, distributed Trace, or backend capability expansion.
- [ ] Next phase: PHASE_5_PORTFOLIO_WEBSITE_ENTERPRISE_TICKET_DETAIL.
- [ ] Frontend real API wiring remains deferred and nonblocking.
