# Claude / Codex 交接记录

使用规则：每轮把新记录追加在"历史记录"顶部，不覆盖旧记录。没有证据时不要写"测试通过"。

---

## 当前待处理交接

### 本轮审查时间

`2026-06-21（简历项目全量审查）`

### 审查对象

- 目的：大三实习简历项目（Java 后端实习 / AI 工具开发）全项目审查
- 审查文件范围：README、backend 全部源码（Controller/Service/Mapper/Entity/DTO/Test）、frontend（组件/API 层）、docs/ 全部文档、schema.sql、demo-data.sql、application.yml
- 审查者：Claude

---

## P0 问题（阻塞简历，必须先修）

### P0-1：application.yml 缺 spring.datasource，后端无法连接 MySQL

- **文件/位置**：`backend/src/main/resources/application.yml`
- **证据**：文件仅含 `server.port`、`logging`、`mybatis-plus` 三块，无任何 `spring.datasource` 配置段
- **影响**：任何人克隆项目后直接 `mvn spring-boot:run` 启动即报 `No qualifying bean of type 'DataSource'`，整个数据库层在仓库内无法复现
- **建议修改**：
  1. 新建 `backend/src/main/resources/application-local.yml.example`，写入 datasource 模板（不含真实密码）
  2. README 补"本地 MySQL 快速启动"章节，说明复制 example 文件、填写密码、加 `--spring.profiles.active=local` 启动
- **验收**：按 README 步骤执行后，`GET /api/health` 返回 200，`GET /api/tickets` 返回 8 条 DEMO 数据

### P0-2：knowledgeCoverage 指标为虚假公式

- **文件/位置**：`TicketWorkflowService.java` metrics() 方法，约第 116 行
- **证据**：`int coverage = total == 0 ? 0 : (int) Math.min(95, 40 + articles * 8);`，发布 7 篇知识文章覆盖率即达 96%，与工单数量无关
- **影响**：面试官追问该指标计算逻辑时立即穿帮
- **建议修改**：改为真实公式 `hit * 100 / total`（"有知识命中的工单数 / 总工单数"），或直接从前端删除该字段
- **验收**：`/api/tickets/metrics` 返回的 `knowledgeCoverage` 值与 `hit/total` 比例手工可验算；对应单元测试断言同步更新并通过

### P0-3：缺少 mvn test 执行证据

- **文件/位置**：`docs/images/`、`README.md`
- **证据**：README 声称自动化测试通过，但无截图、无 CI badge、无 surefire report
- **影响**：面试官要求看测试报告时无法回应
- **建议修改**：执行 `mvn test`，截图 BUILD SUCCESS（含 `Tests run` 行），存入 `docs/images/test-results.png`，README 引用该图
- **验收**：README 中可见测试截图，图中 Tests run 数量与实际 @Test 用例数量一致
- **Codex 实际结果**：2026-06-21 已新增 `docs/TEST_REPORT.md`，记录 `backend/` 下 `mvn test` 与 `frontend/` 下 `npm run build` 的真实输出摘要；README 已引用该报告。本轮未新增截图文件。

---

## P1 问题（建议优化，明显提升含金量）

### P1-1：缺 @RestControllerAdvice 全局异常处理器

- **文件/位置**：`api/` 目录下无 GlobalExceptionHandler
- **严重程度**：中
- **影响**：错误响应格式不统一，"企业级规范"印象减分
- **建议**：新建 `GlobalExceptionHandler.java`，统一返回 `{ "status": 400, "message": "..." }` 结构
- **验收**：`POST /api/tickets`（body 为 `{}`）返回 HTTP 400 且 body 含 `message` 字段
- **Codex 实际结果**：2026-06-21 已新增 `ApiErrorResponse` 与 `GlobalExceptionHandler`，统一错误响应为 `{ code, message, path, timestamp }`；参数校验失败和 `ResponseStatusException` 已补 Controller 测试覆盖。

### P1-2：缺 Swagger / OpenAPI 文档

- **文件/位置**：`backend/pom.xml`
- **严重程度**：中
- **建议**：加 `springdoc-openapi-starter-webmvc-ui` 依赖，启动后 `/swagger-ui/index.html` 可用
- **验收**：浏览器可访问 Swagger UI，8 个接口全部可见
- **Codex 实际结果**：2026-06-21 已添加 `springdoc-openapi-starter-webmvc-ui` 依赖、`OpenApiConfig` 和 Controller `@Tag` 分组；Swagger UI 地址为 `http://localhost:8080/swagger-ui/index.html`，OpenAPI JSON 地址为 `http://localhost:8080/v3/api-docs`。

### P1-3：README 快速启动步骤不完整

- **文件/位置**：`README.md`
- **建议**：补全"本地 MySQL 闭环"启动步骤，包含建库命令、profile 启动命令
- **验收**：新人按 README 操作，2 分钟内前后端可联调

### P1-4："AI" 措辞需修正

- **文件/位置**：`README.md`
- **当前问题**：多处使用"AI 分类"，实际是关键词规则引擎，易引起面试误解
- **建议**：改为"规则引擎辅助分类（Rule-based Classification）"，正文补充说明设计取舍原因
- **验收**：README 中仅在说明“未接入真实 LLM / 未训练模型 / 非向量检索”时出现相关词，避免被误解为已实现能力
- **Codex 实际结果**：2026-06-21 已校准 README 与 docs 中的 AI 相关表述，统一为规则引擎辅助分类、知识库评分匹配、模板化建议草稿和人工确认边界；保留 Copilot 作为产品名，并解释为辅助处理工作台。

---

## P2 问题（锦上添花）

- P2-1：加 H2 内存库 `@SpringBootTest` 集成测试，验证 HTTP 到数据库全链路
- P2-2：加 GitHub Actions CI（`.github/workflows/ci.yml`），自动跑 `mvn test`，README 展示 badge
- P2-3：加 `docker-compose.yml`（MySQL + 后端），一键 `docker compose up` 启动演示环境

---

## 交给 Codex 的第一批任务

### Task-01：新增 application-local.yml.example + 更新 README

- **目标**：让任何人能跟着 README 在 5 分钟内跑通后端
- **涉及文件**：
  - 新建：`backend/src/main/resources/application-local.yml.example`
  - 修改：`README.md`（补"本地 MySQL 快速启动"章节）
- **不能破坏的行为**：`application.yml` 本身内容不变；前端 Demo 模式（`npm run dev:demo`）不受影响
- **验收命令**：
  1. 复制 example 文件为 `application-local.yml`，填写本地 MySQL 信息
  2. `mvn spring-boot:run -Dspring-boot.run.profiles=local`
  3. `curl http://localhost:8080/api/health` → 200
  4. `curl http://localhost:8080/api/tickets` → 含 DEMO-0001 的列表

**Codex 实际结果**：

- 2026-06-21，Codex：采用已有 `backend/src/main/resources/application-example.yml` 作为本地 MySQL 模板，未新增重复模板文件。
- 已将模板账号密码改为 `your_username` / `your_password`，并补充复制为 `application-local.yml`、不要提交真实密码的说明。
- 已补充 README 的本地运行前置条件、建库 SQL、schema/demo-data 导入说明、本地配置复制步骤、后端启动命令和接口验证命令。
- 已补充 `.gitignore`，避免任意位置的 `application-local.yml` 和 `.env.*` 误提交。
- 验证：在 `backend/` 执行 `mvn test`，结果为 `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 未修改 Java / Vue 业务代码，未写入真实数据库密码。

---

### Task-02：修复 knowledgeCoverage 假公式

- **目标**：将覆盖率改为"有知识命中工单数 / 总工单数"
- **涉及文件**：
  - `backend/src/main/java/com/enterpriseai/ticketcopilot/service/TicketWorkflowService.java`（metrics 方法）
  - `backend/src/test/java/com/enterpriseai/ticketcopilot/service/TicketWorkflowServiceTest.java`（同步更新断言）
- **不能破坏的行为**：`WorkbenchMetrics` 字段名 `knowledgeCoverage` 不变；其余三个指标逻辑不变
- **验收命令**：
  1. `mvn test` 全部通过
  2. `GET /api/tickets/metrics`，`knowledgeCoverage` 值与 `hit * 100 / total` 手工计算一致

**Codex 实际结果**：

- 修复时间：2026-06-21，Codex。
- 原问题：`TicketWorkflowService.metrics()` 使用 `Math.min(95, 40 + articles * 8)`，前端 Demo 使用 `Math.min(95, 72 + publishedKnowledge * 4)`，都属于没有业务依据的覆盖率。
- 新口径：`knowledgeCoverage = 有知识库命中 matched_knowledge_nos，或有关联知识草稿/发布 source_ticket_id 的去重工单数 / 总工单数 * 100`。
- 修改文件：`backend/src/main/java/com/enterpriseai/ticketcopilot/service/TicketWorkflowService.java`、`backend/src/test/java/com/enterpriseai/ticketcopilot/service/TicketWorkflowServiceTest.java`、`frontend/src/App.vue`、`frontend/src/data/demoTickets.ts`、`README.md`、`TODO.md`、`HANDOFF.md`。
- 测试命令：`mvn test`（在 `backend/`）、`npm run build`（在 `frontend/`）。
- 测试结果：后端 `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`；前端 typecheck 与 Vite build 通过。
- 剩余风险：字段名仍为 `knowledgeCoverage` 以保持接口兼容，但前端已展示为“知识关联率”；P0-3 的正式测试截图或报告仍未补。

---

### Task-03：新增 GlobalExceptionHandler

- **目标**：统一 API 错误响应格式
- **涉及文件**：
  - 新建：`backend/src/main/java/com/enterpriseai/ticketcopilot/api/GlobalExceptionHandler.java`
- **不能破坏的行为**：现有 Controller 逻辑不变；正常请求响应格式不变
- **验收命令**：
  1. `POST /api/tickets`（body 为 `{}`）→ HTTP 400，body 含 `message` 字段
  2. `GET /api/tickets/NOTEXIST` → HTTP 404，body 含 `message` 字段
  3. `mvn test` 全部通过

**Codex 实际结果**：（待填写）

- 2026-06-21，Codex：已添加 SpringDoc OpenAPI / Swagger UI。
- 修改文件：`backend/pom.xml`、`backend/src/main/java/com/enterpriseai/ticketcopilot/api/OpenApiConfig.java`、`TicketController.java`、`HealthController.java`、`README.md`、`TODO.md`、`HANDOFF.md`。
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`。
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`。
- 验证：在 `backend/` 执行 `mvn test`，结果见本轮历史记录。

---

### Task-04：加 SpringDoc OpenAPI 依赖

- **目标**：后端启动后可访问 Swagger UI
- **涉及文件**：`backend/pom.xml`（加 `springdoc-openapi-starter-webmvc-ui`，pinned version）
- **不能破坏的行为**：现有接口行为不变；`mvn test` 继续通过
- **验收命令**：
  1. `mvn spring-boot:run -Dspring-boot.run.profiles=local`
  2. 浏览器访问 `http://localhost:8080/swagger-ui/index.html`，8 个接口全部可见

**Codex 实际结果**：（待填写）

---

## 历史记录

### 2026-06-22 — Codex — P2-1 H2 集成测试

- 任务：P2-1 H2 集成测试。
- 修改文件：`backend/pom.xml`、`backend/src/test/resources/application-test.yml`、`backend/src/test/resources/schema-h2.sql`、`backend/src/test/java/com/enterpriseai/ticketcopilot/TicketWorkflowIntegrationTest.java`、`docs/TEST_REPORT.md`、`TODO.md`、`HANDOFF.md`。
- 覆盖链路：HTTP → `TicketController` → `TicketWorkflowService` → MyBatis-Plus Mapper → H2 内存数据库。
- 覆盖场景：创建工单并查询、规则引擎辅助分析查询、人工状态流转、生成知识草稿、人工确认发布知识草稿。
- 测试命令：在 `backend/` 执行 `mvn test`。
- 测试结果：`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 是否修改业务逻辑：否；仅新增 test-scope H2 依赖、测试 profile、H2 schema 和集成测试。
- 是否依赖外部服务：否，不依赖本地 MySQL、真实 LLM、前端、Testcontainers 或外部 API。
- 剩余风险：H2 schema 是为测试最小改写的 MySQL 兼容版本，能验证核心链路，但不能完全替代真实 MySQL 方言和性能验证。
- 下一轮建议：处理 P2-2，增加 GitHub Actions CI，至少自动执行后端测试。

### 2026-06-22 — Codex — 补充 docs/API.md

- 任务：补充 `docs/API.md` 人工整理版接口文档。
- 修改文件：`docs/API.md`、`README.md`、`TODO.md`、`HANDOFF.md`。
- 文档覆盖内容：本地访问地址、统一错误响应、核心接口列表、请求 DTO 字段、主要响应结构、创建工单 / 查询规则引擎辅助分析 / 状态流转 / 知识草稿 / 错误响应示例、业务边界和面试说明。
- 是否修改业务代码：否，未修改 Java 业务代码、`pom.xml`、Swagger 配置、全局异常处理器、数据库配置或 `knowledgeCoverage` 逻辑。
- 是否修改前端：否，未修改 Vue 前端代码。
- 验证方式：本轮只修改 Markdown 文档；执行 `git diff --stat`、`git diff --check`、`git status`，并人工核对文档接口与 `TicketController`、DTO、响应模型、`ApiErrorResponse` 和 `GlobalExceptionHandler` 一致。
- 剩余风险：未启动后端访问 Swagger UI；`ai-analysis` 路径保留历史命名，但文档明确说明当前含义是规则引擎辅助分析，不是真实 LLM。
- 下一轮建议：处理 P2-1，增加 H2 内存库集成测试，验证 HTTP 到数据库的基础链路。

### 2026-06-22 — Codex — 前端 UI 文案校准

- 任务：前端 UI 文案校准。
- 修改文件：`frontend/src/App.vue`、`frontend/src/components/AiRecommendationPanel.vue`、`frontend/src/components/StatusTimeline.vue`、`frontend/src/components/TicketIntakePanel.vue`、`frontend/src/data/demoTickets.ts`、`TODO.md`、`HANDOFF.md`。
- 原风险：页面中存在“企业 AI 工单 Copilot”“企业 AI 工单作战台”“AI 已分析”“AI 建议记录”“AI 辅助分析”等可见文案，容易被误解为已接入真实 LLM、AI 模型或自动决策。
- 新口径：Copilot 保留为产品名，并在页面附近表述为企业工单辅助处理工作台；分类为规则引擎辅助分类；知识为知识库评分匹配；输出为模板化建议草稿；所有状态流转和知识入库均需人工确认。
- 是否修改业务逻辑：否，未修改后端 Java 业务逻辑、数据库配置、Swagger 配置、全局异常处理或 `knowledgeCoverage` 逻辑。
- 是否修改前端逻辑：否，仅修改前端可见文案和 Demo 展示文案，未改接口字段名、状态机或交互流程。
- 构建命令：`npm run build`（目录：`frontend/`）。
- 构建结果：通过，`vue-tsc` 类型检查完成，Vite 生产构建完成。
- 剩余风险：源码中仍保留 `AiAnalysis`、`fetchAiAnalysis`、`aiConfidence` 等接口兼容字段和组件命名；UI 风险词复搜仅剩产品名 `Copilot` 与“未调用真实 LLM”的边界说明。
- 下一轮建议：补接口文档，例如 `docs/API.md`。

### 2026-06-21 — Codex — P1-4 AI 表述校准

- 任务：校准 README 和相关文档中容易误导的 AI / Copilot / 大模型 / 向量检索表述。
- 修改文件：`README.md`、`docs/architecture.md`、`docs/product-design.md`、`docs/demo-script.md`、`docs/interview-guide.md`、`TODO.md`、`HANDOFF.md`。
- 原风险：文档中存在“AI 分类”“AI 生成”“AI Agent”“向量召回”等表述，容易被误解为已经接入真实 LLM、模型训练或向量检索。
- 新表述口径：Copilot 是辅助处理工作台；分类是规则引擎辅助分类；知识匹配是分类和关键词评分；建议是模板化处理建议草稿；所有动作需要人工确认。
- 是否修改业务代码：否。
- 验证方式：全文搜索 AI / LLM / embedding / 向量 / Agent 等风险关键词并人工复核；本轮只修改文档，未运行 `mvn test` 或 `npm run build`。
- 剩余风险：前端页面源码仍保留部分 AI 文案，本轮按边界未改 Vue；如要彻底统一 UI 文案，需要单独任务并运行前端构建。
- 下一轮建议：补接口文档，例如 `docs/API.md`。

### 2026-06-21 — Codex — P1-2 SpringDoc OpenAPI / Swagger

- 任务：添加 SpringDoc OpenAPI / Swagger，让后端 REST API 可通过 Swagger UI 展示。
- 修改文件：`backend/pom.xml`、`backend/src/main/java/com/enterpriseai/ticketcopilot/api/OpenApiConfig.java`、`backend/src/main/java/com/enterpriseai/ticketcopilot/api/TicketController.java`、`backend/src/main/java/com/enterpriseai/ticketcopilot/api/HealthController.java`、`README.md`、`TODO.md`、`HANDOFF.md`。
- Swagger UI 地址：`http://localhost:8080/swagger-ui/index.html`。
- OpenAPI JSON 地址：`http://localhost:8080/v3/api-docs`。
- 测试命令：在 `backend/` 执行 `mvn test`。
- 测试结果：`Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 剩余风险：未实际长期启动服务访问 Swagger 页面；仅通过编译和测试确认依赖与配置可用。
- 下一轮建议：处理 P1-4，校准 README 中容易误导的 AI 表述。

### 2026-06-21 — Codex — P1-1 全局异常处理

- 任务：添加 `@RestControllerAdvice` 全局异常处理器，统一 API 错误响应格式。
- 修改文件：`backend/src/main/java/com/enterpriseai/ticketcopilot/api/ApiErrorResponse.java`、`backend/src/main/java/com/enterpriseai/ticketcopilot/api/GlobalExceptionHandler.java`、`backend/src/test/java/com/enterpriseai/ticketcopilot/api/TicketControllerTest.java`、`README.md`、`TODO.md`、`HANDOFF.md`、`docs/TEST_REPORT.md`。
- 错误响应结构：`{ code, message, path, timestamp }`，不返回 Java 堆栈。
- 覆盖异常：`MethodArgumentNotValidException`、`BindException`、`ConstraintViolationException`、`ResponseStatusException`、`IllegalArgumentException`、`NoSuchElementException`、兜底 `Exception`。
- 测试命令：在 `backend/` 执行 `mvn test`。
- 测试结果：`Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 剩余风险：错误响应结构尚未形成独立 API 文档；前端没有针对错误弹窗做专项改造。
- 下一轮建议：处理 P1-2，添加 SpringDoc OpenAPI / Swagger。

### 2026-06-21 — Codex — P0-3 测试执行证据

- 任务：补充正式测试执行证据，让 README 的测试通过描述可追溯到 `docs/TEST_REPORT.md`。
- 修改文件：`docs/TEST_REPORT.md`、`README.md`、`TODO.md`、`HANDOFF.md`。
- 运行命令：`mvn test`（目录：`backend/`）；`npm run build`（目录：`frontend/`）。
- 结果摘要：后端 `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`；前端 `vue-tsc` 类型检查通过，Vite 生产构建通过。
- 剩余风险：仍没有 CI badge、线上部署验收或生产级鉴权/压测证据；本报告不声明真实 LLM、向量检索或生产部署能力。
- 下一轮建议：处理 P1-1，添加全局异常处理器。

### 2026-06-21 — Codex — P0-2 修复 knowledgeCoverage 假指标

- 做了什么：移除后端和前端 Demo 中的人为覆盖率公式，改为基于 `matched_knowledge_nos` 与 `source_ticket_id` 的真实去重工单比例；前端指标卡改为“知识关联率”。
- 修改文件：`backend/src/main/java/com/enterpriseai/ticketcopilot/service/TicketWorkflowService.java`、`backend/src/test/java/com/enterpriseai/ticketcopilot/service/TicketWorkflowServiceTest.java`、`frontend/src/App.vue`、`frontend/src/data/demoTickets.ts`、`README.md`、`TODO.md`、`HANDOFF.md`。
- 验证证据：`backend/` 下 `mvn test` 通过，16 个测试、0 失败；`frontend/` 下 `npm run build` 通过。
- 未做事项：未处理 Swagger；未添加全局异常处理器；未处理 P0-3 测试截图；未修改数据库连接配置；未安装软件；未提交构建产物。
- 下一步建议：处理 P0-3，补充正式测试执行证据。

### 2026-06-21 — Codex — P0-1 本地 MySQL 配置闭环

- 做了什么：完善 `backend/src/main/resources/application-example.yml` 示例配置，补充 README 本地 MySQL 启动说明，并最小补充 `.gitignore`。
- 修改文件：`README.md`、`.gitignore`、`backend/src/main/resources/application-example.yml`、`TODO.md`、`HANDOFF.md`。
- 验证证据：在 `backend/` 执行 `mvn test`，15 个测试通过，`BUILD SUCCESS`。
- 未做事项：未修改 Java / Vue 业务代码；未修复 `knowledgeCoverage`；未添加 Swagger；未添加全局异常处理器；未安装 MySQL 或任何软件；未写入真实密码。
- 下一步建议：处理 P0-2，修复 `knowledgeCoverage` 假指标。

### 2026-06-21 — Claude — 大三实习简历项目全量审查

- 做了什么：全量读取 backend（Controller/Service/Mapper/Entity/DTO/Test）、frontend（组件/API 层）、docs/、schema.sql、demo-data.sql、application.yml，输出完整审查报告
- 主要发现：
  1. application.yml 无 datasource 配置（P0）
  2. knowledgeCoverage 为假公式（P0）
  3. 无测试执行证据（P0）
  4. "AI"实为规则引擎关键词匹配，无 LLM 调用（需准备面试话术）
  5. 缺 @ControllerAdvice、Swagger、鉴权（P1）
- 修改文件：本 HANDOFF.md（首次填写，原为空模板）
- 验证证据：纯读审查，未执行任何构建或测试命令
- 下一步：按 Task-01 → Task-02 → Task-03 → Task-04 顺序交给 Codex 处理
