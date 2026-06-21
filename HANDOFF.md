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
- **证据**：README 声称"15 项单元测试全部通过"，但无截图、无 CI badge、无 surefire report
- **影响**：面试官要求看测试报告时无法回应
- **建议修改**：执行 `mvn test`，截图 BUILD SUCCESS（含 `Tests run: 15` 行），存入 `docs/images/test-results.png`，README 引用该图
- **验收**：README 中可见测试截图，图中 Tests run 数量与实际测试文件数量一致

---

## P1 问题（建议优化，明显提升含金量）

### P1-1：缺 @RestControllerAdvice 全局异常处理器

- **文件/位置**：`api/` 目录下无 GlobalExceptionHandler
- **严重程度**：中
- **影响**：错误响应格式不统一，"企业级规范"印象减分
- **建议**：新建 `GlobalExceptionHandler.java`，统一返回 `{ "status": 400, "message": "..." }` 结构
- **验收**：`POST /api/tickets`（body 为 `{}`）返回 HTTP 400 且 body 含 `message` 字段

### P1-2：缺 Swagger / OpenAPI 文档

- **文件/位置**：`backend/pom.xml`
- **严重程度**：中
- **建议**：加 `springdoc-openapi-starter-webmvc-ui` 依赖，启动后 `/swagger-ui/index.html` 可用
- **验收**：浏览器可访问 Swagger UI，8 个接口全部可见

### P1-3：README 快速启动步骤不完整

- **文件/位置**：`README.md`
- **建议**：补全"本地 MySQL 闭环"启动步骤，包含建库命令、profile 启动命令
- **验收**：新人按 README 操作，2 分钟内前后端可联调

### P1-4："AI" 措辞需修正

- **文件/位置**：`README.md`
- **当前问题**：多处使用"AI 分类"，实际是关键词规则引擎，易引起面试误解
- **建议**：改为"规则引擎辅助分类（Rule-based Classification）"，正文补充说明设计取舍原因
- **验收**：README 中无"LLM"、"AI 模型"等容易引发误解的表述

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

**Codex 实际结果**：（待填写）

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
