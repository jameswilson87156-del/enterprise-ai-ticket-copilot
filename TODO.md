# Enterprise AI Ticket Copilot TODO 路线图

## 2026-09-07 — RELEASE_CANDIDATE_LOCAL_REAUDIT

- [x] `LOCAL_PASS`：后端 289/289、0 failures/errors/skipped，JAR 构建成功；前端 typecheck 和 production build 成功；生产依赖审计为 0 high/critical；Compose 普通/TLS 静态配置和正向隔离 fixture 通过。
- [ ] `BLOCKED`/`STAGING_PENDING`：干净 release commit、镜像 digest/SBOM、阿里云资源/Secret/OIDC/RDS/DNS/ICP/TLS 和公网 staging；等待独立资源与用户确认。


## 2026-09-05 — STAGING_PHASE_A_AUDIT

- 2026-09-06 Workbench 只读审计：现有阿里云轻量应用服务器约 1.0 GiB 可用内存，80/443 已被既有服务占用，并运行长期 SearXNG/Node/Nginx；作为两个项目共用 staging 主机记为 `BLOCKED`，不得停止、覆盖或重配置既有服务。
- [ ] `BLOCKED`：选择容量和端口隔离均合适的独立 staging 主机，或在明确批准后形成扩容/隔离方案；在此之前不执行公网部署。
- 本轮后端289/289、前端类型检查/构建、npm生产依赖审计、配置守卫、空库初始化和Compose静态检查：LOCAL_PASS；当前部署总状态BLOCKED。详见 [独立审计计划](docs/release/STAGING_AUDIT_PLAN_20260905.md)。旧数量及“无LLM/OIDC”等为历史快照，不代表当前代码与证据。
- [x] LOCAL_PASS：完成 T1–T5 的本地整改（Demo种子、schema-only版本化迁移、RDS变体、配置快速失败、Secret构建上下文）。
- [ ] BLOCKED/STAGING_PENDING：完成 T6 干净 release、镜像 digest/SBOM，并取得云端资源/Secret/OIDC/DNS/ICP/TLS 的独立验收证据。
- [ ] BLOCKED：用户自行登录后只读盘点本项目云资源、DNS管理权、ICP、证书和正式IdP。
- [ ] STAGING_PENDING：真实报价/外部操作确认、云部署、Provider失败矩阵、角色浏览器验收、MySQL恢复、监控和回滚；多副本前增加共享限流。保持TICKET_AI_FALLBACK_TO_LOCAL=false。

## 2026-09-05 — FRONTEND_CRAFT

- [x] 工单工作台视觉与交互精修：紧凑导航、三栏工作区、创建弹窗、完整证据/历史按需展开。
- [x] 前端 type-check / production build 通过；跨项目 53 组浏览器页面/状态回归通过，其中工单 Demo 创建、运行、批准和历史断言通过。
- [x] 补充 docs/design/FRONTEND_CRAFT.md；验收记录见 D:/workhome/frontend-craft-20260905/验收报告.md。
- [x] 本地 Phase 1 与真实 DeepSeek 合成成功路径已通过；真实 OIDC、云端数据库/Redis、staging 失败矩阵和公网部署仍按独立发布门禁验收。

## 2026-09-05 — PHASE_1_LOCAL_ACCEPTANCE

- [x] 完成本地 Phase 1：后端 `mvn -B test` 为 289/289；前端生产构建通过；跨项目浏览器验收 53 组通过。
- [x] 新增 `scripts/local/verify-http-smoke.ps1`，以 test profile + H2 schema-only 复现健康、登录、创建、Copilot、Trace、Reviewer Approve 的真实 HTTP 链路。
- [x] 真实 Provider、真实 IdP、MySQL/公网 staging 仍未冒充完成；下一阶段按 `D:/workhome/PROJECT_COMPLETION_ROADMAP_20260905.md` 独立推进。

## 2026-09-05 — PHASE_2_REAL_DEEPSEEK_SYNTHETIC

- [x] 新增 `scripts/local/verify-deepseek-synthetic.ps1`，使用 test profile + H2 schema-only、合成工单和 server-only runtime secret 验证真实 `deepseek-chat`。
- [x] 真实 Provider 运行结果：`SUCCESS`、`actualProvider=deepseek`、`fallbackUsed=false`、1 条 validated citation、结构化输出和 citation membership 均 `VALID`；Reviewer 后最终 `RESOLVED`。
- [x] 脱敏证据写入 `docs/evidence/deepseek-synthetic-smoke-20260905.md`；临时端口已释放，临时日志未发现 Provider Key。
- [ ] 真实 IdP token、staging 失败矩阵、云端备份恢复、DNS/TLS 和公网部署仍未完成。

> 本文件用于后续 Claude / Codex 本地协作交接。每轮只处理一个明确、可验收的小任务；不要把未验证能力写成已完成，也不要把规则引擎包装成真实大模型。

## 2026-09-04 current state

- [x] OIDC/JWT Resource Server code path is implemented for staging/production, with issuer/JWKS verification, optional audience validation and application role mapping. Demo JWT remains local-only.
- [x] Vue Authorization Code + PKCE adapter and enterprise login control are implemented. The browser has no AI API key; provider secrets remain server-side.
- [x] MySQL + Java API + Nginx edge + optional Caddy TLS staging scaffold is present under `deploy/staging/` and passes Docker Compose config validation.
- [x] Security release gates are implemented: public-profile startup validation, Swagger disablement, per-user Copilot rate limiting, baseline edge response headers/HSTS and CI dependency/Compose checks. The in-process limiter is not a substitute for a shared Alibaba Cloud gateway/WAF or Redis limiter in multi-replica production.
- [x] Enterprise backend regression is 289/289; frontend type-check and production build pass; production dependency audit reports zero vulnerabilities.
- [x] A developer-configured DeepSeek `deepseek-chat` path was verified once with an isolated H2 backend and synthetic ticket only: the actual provider ran without fallback, structured output and citation membership were `VALID`, and human approval reached `RESOLVED`. See `docs/evidence/deepseek-isolated-smoke-20260904.md`; this is not a claim of public deployment readiness or real-data model quality.
- [ ] Real IdP token acceptance, DNS/TLS issuance, server deployment, backup/restore and monitoring still require external credentials/infrastructure. The historical shared compatible-provider HTTP 403 remains a failed endpoint/credential check, separate from the later DeepSeek success.

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
- schema.sql 与 demo-data.sql 提供 5 张业务表和演示数据。
- 工单状态机流转，包含待分类、待处理、处理中、已解决、已沉淀等状态；Phase 0-A 已集中约束状态边、审核 Run 前置条件、知识发布边界和条件更新并发冲突。
- ticket_status_history 和 generation_record 审计链。
- Bean Validation 参数校验。
- 规则引擎分类。
- 知识库匹配评分。
- 推荐内容由模板生成，并要求人工确认。
- Vue 3 + TypeScript 前端组件。
- 前端 Demo 模式与真实后端切换。
- Playwright 截图脚本和截图存档。
- SpringDoc OpenAPI / Swagger UI 接口文档。
- GitHub Actions CI workflow 已补充，覆盖后端测试和前端构建；远端 run `27929741126` 已确认通过。
- docs/API.md 已补充人工整理版 REST API 文档，覆盖接口列表、请求响应、统一错误响应和业务边界。
- README、docs 与前端 UI 可见文案已校准 AI 相关表述，统一为规则引擎辅助分类、知识库评分匹配和模板化建议草稿。
- 前端作品集展示 UI 与 README 截图区已重做，主图突出企业工单辅助处理工作台，辅助图使用两列作品集布局展示。
- 2026-06-27 已按 Enterprise SaaS UI Design Skill 新建 `TicketWorkbenchShowcaseView.vue`：Tickets 默认显示独立 ShowcaseView，只使用本地 demo 常量，CSS 使用 `showcase-` scoped 前缀，不复用旧 TicketQueue / TicketDetailPanel / AiRecommendationPanel；`ticket-detail.png` 已刷新并人工验收通过。
- 当前存在 6 个后端测试文件，合计 24 个 @Test 用例；docs/TEST_REPORT.md 已记录本地后端测试和前端构建证据。
- 本地 RAG / Citation / Trace Evaluation 最小闭环已补充，包含 `data/eval/ticket_rag_eval_cases.jsonl`、`scripts/evaluate_rag_demo.py`、`docs/evaluation/RAG_EVALUATION_PLAN.md`、`docs/metrics/rag_metrics_latest.json` 和 `docs/metrics/rag_metrics_snapshot.md`。
- 2026-07-04 已完成前端 Phase 1：统一 Showcase App Shell（当时采用深色方向，已由 2026-09-03 Phase 7 校正为浅色中性客服工作台），升级 Dashboard 首页，新建 Evaluation / Metrics 页面，同步本地评测数据到 `frontend/src/data/evaluationMetrics.ts`，并刷新本项目真实运行截图；未使用第三方截图，未接真实 API Key。
- 2026-07-04 已完成 Evaluation / Metrics 视觉精修：页面产品名调整为“评测指标中心”，8 个核心 KPI 与 4 个补充指标分层展示，Baseline / 实验计划与右侧评测上下文产品化，并刷新本项目标准与 large 真实截图；所有指标继续限定为 synthetic demo dataset + local keyword retrieval + citation gating + local-rule fallback。
- 2026-07-04 已完成 Evaluation / Metrics 左上角 Sidebar 品牌图标修复：App Shell 品牌区恢复 44px 品牌圆角容器 + 原创 ET/ticket inline SVG 标识，并只刷新 `evaluation-metrics` 标准与 large 真实截图；后续 Phase 7 将容器调整为当前浅色品牌样式，未修改 Evaluation 指标数据、RAG metrics 口径、README、后端或 `docs/frontend_reference*`。
- 2026-07-04 已完成前端 Phase 2：Ticket Workbench 升级为三栏企业工单 AI 处理工作台，首屏包含 Ticket Queue、Ticket Detail + AI Draft、Citation Evidence + Human Review + Trace；页面继续只使用 synthetic demo data、local-rule fallback、keyword retrieval 和 citation gating，不接真实 API Key，不修改后端逻辑，不更新 README。
- 2026-07-04 已完成前端 Phase 3：Trace Timeline 新增专用 Showcase 页面，首屏展示 Run Overview、Run List、Step Timeline、Step Detail、Retrieval / Citation Evidence、Provider skipped / local-rule fallback、Human Review gate 与 Raw JSON / Debug Detail；页面继续只使用 synthetic demo trace data，不接真实 API Key，不修改后端业务逻辑，不更新 README。
- 2026-07-04 已完成 GitHub README 作品集整合：README 首屏重新定位 Enterprise Ticket RAG Copilot，按 Dashboard、Ticket Workbench、Evaluation / Metrics、Trace Timeline、Knowledge Base、Human Review 顺序引用本项目真实截图，并补充项目定位、非普通 RAG demo 差异、Workflow、Evaluation / Metrics、Resume Bullets、Interview Talking Points 和 Honest Boundaries；本轮只改文档，不改前端/后端代码，不重新生成截图，不接真实 API Key，不引用 `.local/`、第三方截图或 Image2 图。
- 2026-07-04 已完成 README 发布后小修复：移除顶部外部 Portfolio Case Study 链接，避免跳转到可能滞后的作品集页面；Evaluation / Metrics 不再把 provider fallback 作为百分比质量指标展示，改为说明 no API key mode 下 expected local-rule fallback；Resume Bullets 和 Honest Boundaries 同步强调 optional provider path、synthetic demo dataset、keyword retrieval、citation gating 和 no real API key committed。
- 2026-07-04 已完成发布后 Sidebar 品牌图标视觉小修复：公共 App Shell 左上角品牌 mark 升级为 46px 原创 inline SVG ticket / ET / trace dot 标识，并刷新 README 当前使用的本项目真实截图及 large 版本；未修改 README 文案、后端代码、RAG metrics JSON，未使用第三方 Logo、第三方截图或 Image2 图，未接真实 API Key。

## 3. 当前不能夸大的能力

- 当前没有 LLM 调用。
- 当前没有真实 AI 模型训练。
- 分类是关键词规则引擎，不是机器学习模型。
- 知识匹配是评分公式，不是 embedding 向量检索。
- 推荐内容是模板生成，不是生成式 AI。
- knowledgeCoverage 已改为基于已有数据的真实知识关联率；不能再写成人为覆盖率、模型效果或向量检索能力。
- RAG Evaluation 结果只能写成本地 demo keyword retrieval + citation gating 的可复现指标，不能写成真实模型准确率、真实向量检索效果、Prompt 提升或生产效果。
- 当前 application.yml 缺少 spring.datasource，仓库内可复现启动存在风险；虽然已有 application-example.yml 和 README 启动说明，但仍需统一本地配置样例与验收闭环。
- 当前已有 Demo JWT + 应用层角色授权，但不能写成生产级鉴权/RBAC；OIDC/OAuth2、JWK、issuer、audience、租户隔离、生产密钥托管和资源级 RBAC 均未实现。

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

PHASE_9A、PHASE_9B、PHASE_9C、PHASE_9D、PHASE_9E、PHASE_9F、PHASE_9G 已完成；规划文档为 `docs/design/FRONTEND_REDESIGN_ROADMAP.md`。后端下一阶段设计已单独记录在 `docs/design/BACKEND_NEXT_PHASE_DESIGN.md`，实施时仍需另立一个明确的 T0 契约任务。

如果优先整理作品集对外展示，建议先在 GitHub 页面确认 README 截图、Mermaid 图和表格渲染效果；当前标准、大屏和 390px 移动端截图已生成，且 PHASE_9C～9G 的本地前端验收已记录。

PHASE_9G 发布验收门槛已经完成；后端改造规划已创建，但真实 Provider、生产认证、DNS、公网部署仍应在独立任务中执行。如果暂时回到部署路线，仍建议先单独处理 P2-3：增加 Docker Compose，提供 MySQL + 后端的本地演示环境。

本轮已完成 `BACKEND-T0-API-CONTRACT-FREEZE`、`BACKEND-T1-MODULE-BOUNDARIES`、`BACKEND-T2-WORKFLOW-PORTS` 与 `PHASE_0-A_TICKET_WORKFLOW_STATE_AND_WRITE_AUTHORIZATION`；企业工单 T0-T2 + Phase 0-A 范围已完成并经过回归验证。后续如继续做真实 Provider、生产认证、Outbox 或公网部署，必须另立明确任务，不与电商后端 E0-E4 混在同一轮。

### 2026-09-03 — BACKEND-T0-API-CONTRACT-FREEZE

- [x] 新增 `backend/src/main/java/com/enterpriseai/ticketcopilot/contract/TicketStatusContract.java`，统一当前数据库/DTO 使用的状态字符串和通用人工状态接口允许的目标状态。
- [x] 将 `TicketWorkflowService` 的状态常量指向合同类；保留原有公开常量，避免破坏当前测试和调用方。
- [x] 新增 `ApiRouteContractTest`，固定 `/api/auth`、`/api/health` 和 `/api/tickets` 现有 GET/POST 路径，防止后续模块迁移悄悄破坏前端兼容接口。
- [x] 新增 `TicketStatusContractTest`，覆盖状态顺序、内部 intake 状态隔离、未知/大小写错误状态和公共目标状态白名单。
- [x] 新增 `docs/design/BACKEND_T0_API_CONTRACT.md`，记录旧 API、状态合同、Copilot/Trace/Review 不变量和 `/api/v1` 迁移边界。
- [x] 同步 `docs/API.md`、`docs/architecture.md`、`docs/TEST_REPORT.md`、`HANDOFF.md`。
- [x] 验证：`backend/ mvn test`，`Tests run: 90, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 不能声称：本轮没有实现完整状态转移矩阵、`/api/v1` 实际切换、完整模块包迁移、真实身份/RBAC、真实 Provider、Outbox 或公网部署。

### 2026-09-03 — BACKEND-T1-MODULE-BOUNDARIES

- [x] 新增 `ticket/application/port/in/TicketWorkflowUseCase` 入站应用端口，覆盖当前 Controller 的列表、详情、Copilot、复核、状态和知识草稿用例；不暴露 Entity、Mapper 或 Provider 类型。
- [x] 新增 `ticket/application/LegacyTicketWorkflowFacade` 过渡适配器，让旧 `TicketWorkflowService` 保留现有行为，同时把 REST 入口依赖改到应用端口。
- [x] `TicketController` 和 Controller 单测改为依赖/模拟入站端口；旧 `/api/...` 路径、DTO、数据库字段和运行证据不变。
- [x] 新增 `TicketModuleBoundaryTest`，保护 Controller → inbound port 依赖方向以及入站端口不泄漏持久化/旧 Service 类型。
- [x] 新增 `docs/design/BACKEND_T1_MODULE_BOUNDARIES.md`，记录第一条纵向切片、迁移边界和后续端口拆分路线。
- [x] 验证：`backend/ mvn test`，`Tests run: 92, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 不能声称：完整 `ticket/knowledge/retrieval/copilot/provider/audit/ops` 包迁移、T2 出站端口、真实身份/RBAC、真实 Provider、Outbox、数据库迁移或公网部署已经完成。

### 2026-09-03 — BACKEND-T2-WORKFLOW-PORTS

- [x] 新增 `ticket/application/port/out/ReviewPolicy`，将最终人工复核策略的输入收敛为结构化输出、fallback、Citation 校验状态、结构化输出校验状态和业务规则标志。
- [x] `TicketWorkflowService` 改为依赖 `ReviewPolicy`，不再直接依赖旧的 `ReviewGate`；`ReviewGate` 实现该端口，Spring 运行时实现和现有策略语义保持可用。
- [x] 保留 `ReviewGate.finalHumanReviewRequired(... CitationValidationResult ...)` 兼容方法，避免旧测试/内部调用方因为端口迁移破坏；新工作流路径统一调用 `requiresHumanReview(...)`。
- [x] 新增 `WorkflowPortBoundaryTest`，保护工作流字段/构造器依赖端口、端口签名不泄漏旧 Service/Entity/Mapper 类型，并确认当前策略实现确实实现端口。
- [x] 新增 `docs/design/BACKEND_T2_WORKFLOW_PORTS.md`，记录策略端口、保持不变的安全语义和后续端口拆分边界。
- [x] 验证：`backend/ mvn test`，`Tests run: 94, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 不能声称：完整模块包迁移、AiProviderPort、RetrievalPort、AuditRecorder、真实身份/RBAC、真实 Provider、Outbox、数据库迁移或公网部署已经完成。

### 2026-09-04 — PHASE_0-A_TICKET_WORKFLOW_STATE_AND_WRITE_AUTHORIZATION

- [x] 新增 `TicketStatusTransitionPolicy`，集中约束初始分类、Copilot 首次运行/重跑、四条已有合法手工状态边、三种审核决策、知识草稿和知识发布；所有未有明确产品依据的已知状态边保持拒绝。
- [x] `TicketWorkflowService.updateStatus`、审核动作、Copilot 状态变化、`createKnowledgeDraft`、`confirmKnowledgeDraft` 和 `publishDraft` 均接入同一套状态策略；保留 `PENDING_PROCESS -> RESOLVED`、`RESOLVED -> IN_PROGRESS` 等现有 Showcase 流程。
- [x] 新增条件状态更新：以 `id + expected status` 保存状态，竞争更新影响行数为 0 时返回 409，并依靠事务回滚本次历史/审核/知识副作用；Copilot 成功路径继续持久化分类和置信度字段。
- [x] 新增 `TicketAuthorizationPolicy`，集中约束 ADMIN、AGENT、REVIEWER、VIEWER 对所有当前 Ticket 写入口的权限；`confirm=true` 按知识发布权限处理，匿名请求为 401，越权请求为 403。
- [x] 新增 `docs/design/TICKET_WORKFLOW_STATE_AND_AUTHORIZATION.md`，记录真实状态、转移矩阵、角色动作矩阵、状态边推导依据、并发保护和 Demo/生产认证边界。
- [x] 新增并保留状态策略、授权策略、服务并发冲突、Controller 边界和 HTTP/H2 持久化副作用测试；全量后端 `mvn -B test` 为 282/282，0 failures/errors/skipped。
- [x] 前端未改动；`frontend/npm run build` 通过（`vue-tsc` + Vite，63 modules）。当前 `frontend/package.json` 没有独立 `test` script，未执行不存在的 `npm test`。
- 范围边界：未修改 CommerceFlow、电商/前端视觉、真实 Provider、OIDC/OAuth2、公网/DNS/证书、Outbox/队列/Worker、数据库 schema 或 Git 状态操作；`docs/PRD.md` 按前置检查结果记录为文件缺失，未自行编造。

企业工单后端 T0-T2 + Phase 0-A 验收完成；后续如继续做真实 Provider、生产认证、Outbox 或公网部署，应另立明确任务。

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
- [x] Next phase: PHASE_5_PORTFOLIO_WEBSITE_ENTERPRISE_TICKET_DETAIL（已完成本阶段前端真实链路审查与接入）。
- [x] Frontend real API wiring remains deferred and nonblocking（现已完成：真实 API 模式与 Demo 模式边界已明确）。

### 2026-09-02 — PHASE_5_ENTERPRISE_TICKET_FRONTEND_REAL_FLOW_AND_E2E_AUDIT

- [x] 完成前端 API DTO、共享状态、错误状态与真实后端调用链审查；真实模式无静默 Mock fallback。
- [x] 补齐本地 Demo 的 Trace、Copilot 运行、结构化结果和 Approve / Request changes / Reject 交互，并用 `DEMO_LOCAL` / `DEMO-*` 明确标识非持久化证据。
- [x] 完成真实模式浏览器 smoke：隔离 H2 后端创建合成工单、运行 Copilot、验证 `NO_RETRIEVAL_EVIDENCE` 安全拒答、Approve 后状态为已解决。
- [x] 完成 Demo 模式浏览器 smoke：本地运行、Trace 展示、Approve 后状态为已解决并生成本地 review history。
- [x] 运行 `frontend/npm run build` 与 `frontend/npm run screenshots`；截图覆盖桌面、大屏、移动宽度检查。
- [x] 同步 README、`docs/architecture.md`、`docs/frontend-real-flow-implementation.md`、`docs/TEST_REPORT.md` 与 `HANDOFF.md` 的真实边界和验证记录。
- 范围边界：未修改已冻结的后端业务逻辑、数据库迁移、Provider、RAG 算法或 Docker Compose；未添加生产依赖、真实密钥或外部服务。

### 2026-09-03 — PHASE_6_FRONTEND_FULL_REDESIGN_AND_BROWSER_ACCEPTANCE

- [x] 完成前端现状审查与逐页设计规格，记录真实 API、Demo fixture、Fallback、保留资产和验收基线。
- [x] 完成品牌化 App Shell、单层 Topbar、统一深色 token、共享 UI primitives 和响应式布局；保留 hash 路由与既有 `data-e2e` 选择器。
- [x] 完成 Dashboard、Ticket Workbench、Knowledge Base、Retrieval Evidence、Trace Timeline、Human Review 页面重做；Evaluation / Metrics 接入统一壳层并保留本地评测事实边界。
- [x] Workbench 增加队列搜索/状态/优先级筛选、建议优先的 Copilot 层级、Citation 分层、折叠审计字段、运行/审核 loading、disabled、success、error 状态；Demo 增加稳定待复核 fixture。
- [x] 完成真实浏览器验收：隔离 H2 + Vite 真实模式创建合成工单、调用后端 Copilot、读取 `IMMUTABLE_RUN` / retrieval / validated Citation、Approve 后变为 `RESOLVED`；本机 Provider 环境返回 HTTP 403 后由后端安全 fallback 到 local-rule，未把它记作真实模型成功接入。
- [x] 完成 Demo 浏览器 smoke：本地 Copilot、`DEMO_LOCAL` Trace/evidence、Human Review Approve、队列筛选和 Ctrl/Cmd+K 页面搜索；控制台与页面错误均为 0。
- [x] `frontend/npm run build`、`backend/mvn test`（84 tests）和 `frontend/npm run screenshots` 已执行；截图脚本生成标准、大屏、390px 移动端三套截图，并通过 1366/390 横向溢出检查。
- [x] 文档同步：`docs/design/FRONTEND_CURRENT_AUDIT.md`、`docs/design/FRONTEND_PAGE_SPECS.md`、`docs/frontend-real-flow-implementation.md`、`docs/architecture.md`、`README.md`、`docs/TEST_REPORT.md`、`HANDOFF.md`。
- 范围边界：未修改后端源码、数据库迁移、Provider/RAG 算法或另一个项目；未添加真实密钥、生产依赖、第三方 Logo/图片/字体、公网部署或 Git 提交/推送。

### 2026-09-03 — PHASE_7_REFERENCE_LED_CALM_SAAS_VISUAL_REFINEMENT

- [x] 根据用户反馈，将上一版深色科技风校正为参考案例导向的浅色中性客服工作台；主结构采用队列、工单上下文、处理建议、证据和人工复核的支持运营语义。
- [x] 完成 App Shell、品牌标识、顶部搜索、Dashboard、Ticket Workbench、Knowledge Base、Retrieval Evidence、Trace Timeline、Human Review、Evaluation / Metrics 的浅色 token、中文可见文案、状态色和响应式布局同步。
- [x] 参考 Intercom / Zendesk / Jira Service Management / ServiceNow / Linear 的公开信息架构和交互语义；未复制第三方品牌、Logo、图片、字体或页面代码。
- [x] 保留现有后端业务链路、API 路径、DTO、hash 路由、`data-e2e` 选择器、Demo/Real/Fallback 边界和旧组件资产；不新增生产依赖、Provider 调用、数据库变更或外部账号。
- [x] 重新生成标准、大屏、移动端浏览器截图，并完成 Demo 浏览器 smoke、前端生产构建和后端 Maven 回归。
- 验收证据：`frontend/npm run build` 通过（60 modules）；`SCREENSHOT_URL=http://127.0.0.1:5190 npm run screenshots` 通过 8 个目标及 1366/390 横向溢出检查；Demo browser smoke console/page errors 为 0；`backend/mvn test` 为 84/0/0/0，`BUILD SUCCESS`。
- 范围边界：本轮未重跑真实 API 浏览器服务，Phase 6 的真实 API 证据保持历史记录；没有成功的真实模型 Provider 结果、公网部署、Git commit 或 push。

### 2026-09-03 — PHASE_8_REFERENCE_LED_PREMIUM_BRAND_POLISH

- [x] 深入复核 Intercom、Zendesk、Jira Service Management、ServiceNow、Linear 以及 Atlassian / Primer 的公开设计系统资料，提取队列、上下文、搜索、token、字体层级、图标和状态语义。
- [x] 将案例启发转为项目自有视觉：新增 `frontend/src/components/layout/NavIcon.vue` 自绘线性图标，重做票据 / 证据节点 `BrandMark.vue`，引入本地优先的 `Aptos` / `Segoe UI Variable` 字体栈与温润中性色 / 靛蓝 / 赤陶色角色。
- [x] 将 Dashboard 的指标和数据来源文案统一为中文业务语义，保留现有 API、状态计算、Demo fixture、`data-e2e` 选择器和复核流程。
- [x] 重新生成标准、大屏、移动端浏览器截图；完成页面视觉检查和 Demo 浏览器 smoke。
- 验收证据：`frontend/npm run build` 通过（63 modules）；`SCREENSHOT_URL=http://127.0.0.1:5190 npm run screenshots` 通过 8 个目标及 1366/390 横向溢出检查；Demo browser smoke 通过，console/page errors 为 0；`agent-reach check-update` 显示 v1.5.0 已是最新。
- 范围边界：只改前端视觉 / 可见文案、截图和文档；未修改后端源码、数据库迁移、Provider、RAG 指标、另一个项目或外部账号；未引入第三方 Logo、图片、字体文件、代码、生产依赖、公网部署或 Git commit/push。

### 2026-09-03 — PHASE_9A_INTERACTION_AND_ACCESSIBILITY_FOUNDATION

- [x] 新增并落地前端总改造规划：`docs/design/FRONTEND_REDESIGN_ROADMAP.md`；将后续前端工作拆为 9A～9G，每阶段单独验收，未把待做阶段写成已完成。
- [x] 为 `App.vue` 命令中心补齐焦点进入、Escape 恢复、Tab / Shift+Tab 循环、↑ / ↓ 当前项、Enter 导航以及 `combobox` / `listbox` / `option` / `aria-activedescendant` 语义。
- [x] 命令中心结果图标复用自有 `NavIcon.vue`；当前项、hover 和 keyboard focus 样式可见，顶部快捷键提示与实际能力一致。
- [x] 调整 `--app-muted`、`--app-amber`、`--app-red` 及导航弱化文本的核心 token，完成白色面板上的静态对比度复核；未把静态检查描述为完整 axe / Lighthouse 审计。
- [x] 验证 `frontend/npm run build`、`SCREENSHOT_URL=http://127.0.0.1:5190 npm run screenshots`、一次性 Chrome smoke；后者通过命令中心键盘路径和 `DEMO-0002` Copilot → Approve → 已解决闭环，console/page errors 均为 0。
- [x] 范围边界：未修改后端源码、数据库迁移、Provider、RAG 指标、`docs/metrics/`、真实密钥或生产依赖；未执行 Git commit/push/deploy。真实 API smoke 未在本轮重跑，Phase 6 历史证据仍保留。

### 2026-09-03 — PHASE_9B_COPILOT_DECISION_CENTER

- [x] 将 Workbench 右侧收口为“Copilot 决策建议 → 建议回复 → 关联证据 → 风险与复核门禁 → 人工复核 → 运行信息 / 审计字段”。
- [x] 新增决策主卡，展示推荐处理、分类置信度、运行入口和下一步提示；将风险、Citation 数量和复核门禁独立为风险摘要卡。
- [x] 保留既有 API、Demo / Real / Fallback 语义、hash 路由、`data-e2e` 选择器和人工审核逻辑；未修改 backend、数据库、Provider 或生产配置。
- [x] 验证 `frontend/npm run typecheck`、`frontend/npm run build` 和 `SCREENSHOT_URL=http://127.0.0.1:5190 npm run screenshots`；截图脚本 8 个目标通过，标准 / 大屏 / 移动截图均生成。
- [x] 浏览器验收覆盖 390 / 768 / 1024 / 1440：无横向溢出、破图数量为 0、页面 console error/warning 为 0；Demo `DEMO-0004` 运行 Copilot → Approve 后状态为“已解决”，history 含 `APPROVED_RESOLUTION`。
- [x] 运行静态 accessibility scanner；当前剩余候选主要是组件级 landmark / label heuristic，未把该结果写成完整 axe / Lighthouse 合规结论。
- [x] 未执行 Git commit、push 或部署；下一阶段为 PHASE_9C。

### 2026-09-03 — PHASE_9C_TYPOGRAPHY_AND_TOKEN_CLOSURE

- [x] 共享 `--app-size-*`、间距和 focus ring token；普通辅助说明调整为 12px，9～10px 继续只用于技术标识、评分和紧凑元数据。
- [x] Evaluation 页面 `--eval-*` 颜色语义改为映射全局 token，保留评测指标密度和原有数据口径。
- [x] 将静态 inline margin/padding/字体声明迁移为语义 class；动态进度条百分比样式保留为运行时必要的 `:style`。
- [x] 验证 `frontend/npm run typecheck`、`frontend/npm run build` 和 `git diff --check`；未修改 backend、数据库、Provider、真实密钥、评测数据或部署配置。
- [x] 下一阶段为 PHASE_9D；未执行 Git commit、push 或部署。

### 2026-09-03 — PHASE_9D_MOBILE_WORKFLOW

- [x] Workbench 在 760px 以下增加当前页内的队列 → 工单 → 建议与证据 → 复核流程导航；点击不会修改应用级 hash 路由。
- [x] 390px 下核心区域单列，决策与复核按钮保持 44px 最小触达高度，Request changes / Reject 改为纵向动作组；桌面三列和 768/1024 两列结构保留。
- [x] 浏览器 Demo smoke 完成：移动端选择合成工单、运行本地 Copilot、查看决策/证据、Approve 后状态为“已解决”；多视口无横向溢出、破图为 0。
- [x] 通过 `frontend/npm run typecheck`、`frontend/npm run build`；发现并修复了流程锚点与 hash 路由冲突。未修改 backend、数据库、Provider、真实密钥或部署。
- [x] 下一阶段为 PHASE_9E；未执行 Git commit、push 或部署。

### 2026-09-03 — PHASE_9E_LANGUAGE_AND_DISCLOSURE

- [x] 用户面文案改为中文优先，技术词在必要处以括号保留；Workbench、Human Review、Knowledge、Retrieval、Trace、Dashboard 的 aria label 和主要空态同步调整。
- [x] 审核动作统一为批准 / 要求修改 / 驳回优先，确认提示和必填原因提示同步更新；不改变 `approve` / `request-changes` / `reject` API decision 值。
- [x] 明确区分检索命中、检索参考、已校验引用与 Citation membership validation；低频 run/trace/provider/fallback 字段保持审计可读，Workbench 继续折叠低频字段。
- [x] 通过 `frontend/npm run typecheck`、`frontend/npm run build`；未修改 backend、数据库、Provider、Demo fixture、评测数据或部署配置。
- [x] 下一阶段为 PHASE_9F；未执行 Git commit、push 或部署。

### 2026-09-03 — PHASE_9F_LEGACY_REFERENCE_AUDIT

- [x] 新增 `docs/design/LEGACY_COMPONENT_REFERENCE_MAP.md`，核对 `src/components` 的实际引用关系、共享 UI 原语和 7 个当前无路由引用的 Legacy 业务组件。
- [x] 确认当前页面统一使用 layout/UI 共享组件；Legacy 组件暂不删除，已记录其 props/emit/data shape 不兼容和后续删除条件。
- [x] 复核 CSS 债务边界：静态 inline 样式已在 9C 迁移，动态百分比进度条保留；没有执行不透明的大规模目录重构。
- [x] 通过 `frontend/npm run typecheck`、`frontend/npm run build`；未修改 backend、数据库、Provider、真实密钥或部署。
- [x] 下一阶段为 PHASE_9G；未执行 Git commit、push 或部署。

### 2026-09-03 — PHASE_9G_FRONTEND_RELEASE_ACCEPTANCE

- [x] 两项目前端构建和回归已执行：电商 Admin 45/45、H5 35/35、Java Mall API 53/53、FastAPI 11/11；工单后端 84/84；Admin、H5 和工单前端生产构建均通过，H5 `build:uni` 通过。
- [x] 电商 Showcase optional verification 通过：`SHOWCASE_VERIFY_OK`、`ORDER_SMOKE_OK`、`RATE_LIMIT_SMOKE_OK`；覆盖真实商品/购物车/订单/AI Evidence/Trace、幂等冲突和 Redis 429 边界。
- [x] 电商真实浏览器验收完成：H5 390/768/1024/1440 无横向溢出、破图为 0，搜索/分类/排序可用；Admin 四视口无横向溢出并实际完成 Java API → Mock Provider → Evidence/Trace 闭环。
- [x] 工单 `frontend/npm run screenshots` 重新执行并生成标准、大屏、移动截图；Demo / Real API 历史 smoke、移动流程导航和人工复核闭环均有证据记录。
- [x] 工单表单显式补充 `id/for` 后，静态 a11y 候选从 7 critical 降为 0 critical；剩余 88 个候选主要来自未挂载 Legacy 组件的页面级 landmark/h1/nav 启发式，不能等同完整 axe/Lighthouse 合规。
- [x] 当前 9G 结论：前端本地发布验收门槛完成，可以进入后端改造设计；新增 `docs/design/BACKEND_NEXT_PHASE_DESIGN.md`，只记录目标，不把目标能力写成已实现。
- [x] 范围边界：没有调用真实 Provider、没有添加 API Key、没有改 DNS、没有公网部署、没有提交或推送；保留本地 Showcase 运行数据和 UniApp 既有 `uni-stat` warning 说明。
