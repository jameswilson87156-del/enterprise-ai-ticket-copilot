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
- schema.sql 与 demo-data.sql 提供 5 张业务表和演示数据。
- 工单状态机流转，包含待分类、待处理、处理中、已解决、已沉淀等状态。
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
- 当前存在 5 个后端测试文件，合计 17 个 @Test 用例；docs/TEST_REPORT.md 已记录本地后端测试和前端构建证据。

## 3. 当前不能夸大的能力

- 当前没有 LLM 调用。
- 当前没有真实 AI 模型训练。
- 分类是关键词规则引擎，不是机器学习模型。
- 知识匹配是评分公式，不是 embedding 向量检索。
- 推荐内容是模板生成，不是生成式 AI。
- knowledgeCoverage 已改为基于已有数据的真实知识关联率；不能再写成人为覆盖率、模型效果或向量检索能力。
- 当前 application.yml 缺少 spring.datasource，仓库内可复现启动存在风险；虽然已有 application-example.yml 和 README 启动说明，但仍需统一本地配置样例与验收闭环。
- 不能写成生产级鉴权系统，因为当前没有鉴权层。

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
- 优化前端体验与截图素材，保证作品集展示统一。
- 补充面试 Q&A 文档，说明项目边界、规则引擎取舍、状态机设计和后续可扩展方向。

## 7. 下一轮建议任务

下一轮建议处理 P2-3：增加 Docker Compose，提供 MySQL + 后端的本地演示环境。

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
