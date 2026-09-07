# 测试执行报告

> 本报告是 Enterprise AI Ticket Copilot 的简历展示版测试证据，用于证明 README 中“测试通过”的描述来自真实命令输出。报告只记录本地自动化测试与构建结果，不表示项目已经生产部署。

## 基本信息

- 测试时间：2026-06-21 23:23:08 +08:00
- 当前分支：resume-optimization-v1
- 当前 commit：ac5b4602a5abbc7ce3a76c363d8a21d8a05f975d
- 项目定位：企业内部工单辅助处理系统 / 规则引擎辅助分类系统
- 能力边界：当前项目使用规则引擎辅助分类、关键词知识匹配和模板化建议草稿，不依赖真实 LLM API。

## 后端测试

- 运行目录：D:\workhome\enterprise-ai-ticket-copilot\backend
- 执行命令：`mvn test`
- 执行结果：通过
- 结果摘要：`Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`
- 构建结论：`BUILD SUCCESS`
- 备注：命令输出中出现 OpenJDK CDS sharing warning，不影响测试结果，未作为失败处理。

### 后端覆盖范围

- Controller 层：`TicketControllerTest` 覆盖接口请求、参数校验和响应边界。
- Service 层：`TicketWorkflowServiceTest` 覆盖工单创建、状态流转、人工确认边界、知识草稿发布幂等性。
- P0-2 指标口径：`metricsUseDistinctTicketsWithRealKnowledgeContext` 覆盖 `knowledgeCoverage` 新口径，验证按知识命中和知识草稿关联的去重工单数计算。
- 规则分类：`RuleClassificationServiceTest` 覆盖关键词规则分类。
- 知识匹配：`KnowledgeMatchingServiceTest` 覆盖知识条目评分与匹配逻辑。
- 建议模板：`RecommendationTemplateServiceTest` 覆盖模板化排查步骤、回复建议和风险提示生成。

## 前端构建

- 运行目录：D:\workhome\enterprise-ai-ticket-copilot\frontend
- 执行命令：`npm run build`
- 执行结果：通过
- 类型检查：`vue-tsc -b --pretty false` 通过
- 构建工具：Vite 6.4.3
- 构建摘要：27 个模块完成 transform，生产构建成功生成 `dist/` 产物。
- 提交说明：`dist/` 是构建产物，已由 `.gitignore` 排除，本报告不会提交构建产物。

## 未覆盖范围

- 没有真实 LLM 调用测试；当前项目不依赖真实 LLM API。
- 没有 embedding、向量检索或机器学习模型训练测试；当前知识匹配是关键词与评分公式。
- 没有生产级鉴权、权限压测或安全压测。
- 没有真实线上部署验收，也不声明已完成生产部署。
- 没有新增测试截图文件；本轮只新增 Markdown 测试报告。

## 追加验证记录：P1-1 全局异常处理

- 验证时间：2026-06-21 23:29:12 +08:00
- 验证分支：resume-optimization-v1
- 验证基准 commit：304fa60 docs: add test evidence report
- 运行目录：D:\workhome\enterprise-ai-ticket-copilot\backend
- 执行命令：`mvn test`
- 执行结果：通过
- 结果摘要：`Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`
- 构建结论：`BUILD SUCCESS`
- 覆盖新增内容：全局异常处理器统一参数校验错误和 `ResponseStatusException` 错误响应结构。
- 说明：本轮未修改前端代码，未重新运行 `npm run build`；前端构建证据见上方“前端构建”章节。

## 追加验证记录：P2-1 H2 内存库集成测试

- 验证时间：2026-06-22 10:59:14 +08:00
- 验证分支：resume-optimization-v1
- 验证基准 commit：3a168a9 docs: add API reference
- 运行目录：D:\workhome\enterprise-ai-ticket-copilot\backend
- 执行命令：`mvn test`
- 执行结果：通过
- 结果摘要：`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`
- 构建结论：`BUILD SUCCESS`
- 新增测试：`TicketWorkflowIntegrationTest`
- 新增 H2 测试资源：`application-test.yml`、`schema-h2.sql`
- 覆盖链路：HTTP 请求进入 `TicketController`，经过 `TicketWorkflowService`、MyBatis-Plus Mapper，最终读写 H2 内存数据库。
- 覆盖场景：创建工单并查询、查询规则引擎辅助分析、人工状态流转、生成知识草稿、人工确认发布知识草稿。
- 说明：本轮未修改前端代码，未重新运行 `npm run build`；H2 集成测试不依赖本地 MySQL、真实 LLM、Testcontainers 或外部服务。

## 追加记录：P2-2 GitHub Actions CI workflow

- 记录时间：2026-06-22 11:29:22 +08:00
- 新增文件：`.github/workflows/ci.yml`
- 触发条件：`push`、`pull_request`
- 后端 job：`backend-tests`，使用 Java 17、Maven cache，在 `backend/` 执行 `mvn test`
- 前端 job：`frontend-build`，使用 Node.js 20、npm cache，在 `frontend/` 执行 `npm ci` 和 `npm run build`
- 本地后端验证：在 `backend/` 执行 `mvn test`，结果为 `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`
- 本地前端验证：在 `frontend/` 执行 `npm run build`，`vue-tsc` 类型检查通过，Vite 生产构建完成
- 远端说明：本地新增 workflow 不等于 GitHub Actions 已通过；需要 push 到 GitHub 后，在 Actions 页面确认远端运行结果。

## 追加验证记录：最终 CI 远端确认

- 验证时间：2026-06-22 12:32:48 +08:00
- 验证分支：resume-optimization-v1
- 验证 commit：10b51f4 ci: add GitHub Actions workflow
- 远端 run：`27929741126`
- 远端 run 地址：https://github.com/jameswilson87156-del/enterprise-ai-ticket-copilot/actions/runs/27929741126
- 验证命令：`gh run watch 27929741126 --exit-status`、`gh run view 27929741126 --json databaseId,status,conclusion,headSha,url,jobs`
- 远端结果：`status = completed`，`conclusion = success`
- 通过 job：`Backend tests`、`Frontend build`
- 本轮未新增截图文件；远端通过证据来自已登录 GitHub CLI 返回的 run 状态。
- 说明：远端 workflow 输出中出现 Node.js 20 deprecation annotation，这是 GitHub Actions runner 对官方 action 运行时的提示，不影响本次 CI 结论；后续如需消除提示，可单独评估升级 workflow 的 Node 版本。

## 追加验证记录：Trace Evidence 证据链增强

- 验证时间：2026-06-26 23:41:52 +08:00
- 验证分支：feat/enterprise-ticket-rag-copilot
- 验证基准 commit：27cb110 feat: redesign ticket workbench for enterprise rag copilot
- 后端运行目录：D:\workhome\enterprise-ai-ticket-copilot\backend
- 后端执行命令：`mvn test`
- 后端执行结果：通过
- 后端结果摘要：`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`
- 后端构建结论：`BUILD SUCCESS`
- 新增后端覆盖：`TicketWorkflowIntegrationTest.traceEvidenceExposesGenerationRecordsRagReferencesAndHumanReviewBoundary` 覆盖 `/api/tickets/{id}/trace-evidence`，验证 `generation_record`、RAG Reference、状态历史和 Human Review 推导字段。
- 前端运行目录：D:\workhome\enterprise-ai-ticket-copilot\frontend
- 前端执行命令：`npm run build`
- 前端执行结果：通过，`vue-tsc` 类型检查完成，Vite 生产构建完成。
- 截图执行命令：`npm run screenshots`
- 截图执行结果：通过，重新生成 `docs/images/*.png` 和 `docs/images/large/*.png`。
- 边界说明：本轮不接真实 LLM，不新增向量数据库，不新增 Tool Runtime，不实现完整 Multi-Agent Runtime，不做无人值守自动关闭工单。

## 追加验证记录：Provider fallback + RBAC demo + Human Review

- 验证时间：2026-06-27 00:41:22 +08:00
- 验证分支：feat/enterprise-ticket-rag-copilot
- 后端运行目录：D:\workhome\enterprise-ai-ticket-copilot\backend
- 后端执行命令：`mvn test`
- 后端执行结果：通过
- 后端结果摘要：`Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`
- 后端构建结论：`BUILD SUCCESS`
- 新增覆盖：
  - 无 API Key 时 OpenAI-compatible Provider 自动 fallback 到 local-rule，并在 Trace Evidence 暴露 `providerName`、`modelName`、`fallbackUsed`、`fallbackReason`。
  - `POST /api/tickets/{id}/run-copilot` 写入 `AI_PROVIDER` generation record。
  - Human Review `approve`、`request-changes`、`reject` 状态流转。
  - RBAC demo 阻止 `VIEWER` 执行 run-copilot 或 review 操作。
- 前端运行目录：D:\workhome\enterprise-ai-ticket-copilot\frontend
- 前端执行命令：`npm run build`
- 前端执行结果：通过，`vue-tsc` 类型检查完成，Vite 生产构建完成。
- 截图执行命令：`npm run screenshots`
- 截图执行结果：通过，刷新 `docs/images/*.png` 和 `docs/images/large/*.png`。
- 边界说明：本轮没有真实 API Key，不声明真实 Provider 调用已完成验证；JWT + RBAC 是 demo 级，不是生产鉴权系统。

## 结论

本次本地验证中，后端 `mvn test`、前端 `npm run build` 与前端截图脚本均真实运行并通过。最新后端验证已包含 H2 内存库集成测试、Trace Evidence 接口测试、Provider fallback、RBAC demo 和 Human Review 状态闭环测试，测试结果可以支撑 README 中关于当前自动化测试和前端构建通过的说明，但不代表完整生产级质量保证。

## 2026-07-30 PHASE_3_REAL_RUN_TRACE_FOUNDATION validation

- Backend: `backend/ mvn test` passed. Result: `Tests run: 41, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
- Frontend: `frontend/ npm run build` passed (`vue-tsc` + Vite production build). No frontend source changes were made in this phase.
- RAG eval: `python scripts/evaluate_rag_demo.py` passed with 16 synthetic cases; metrics remained local demo keyword retrieval/citation-gating metrics.
- Provider safety: no real Provider smoke was executed in this phase; real Provider requests = 0.

## 2026-07-30 ? PHASE_4_STRUCTURED_OUTPUT_CITATION_AND_ABSTENTION

- Backend: `mvn test` in `backend/` passed with `Tests run: 70, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.
- Frontend: `npm.cmd run build` in `frontend/` passed (`vue-tsc` + Vite production build).
- RAG baseline: existing 16-case evaluation preserved `top_k_hit_rate=100.00%`, `context_recall_at_k=90.00%`, `citation_coverage=100.00%`, `citation_precision=81.11%`, `failed_case_count=6`, `human_review_required_count=15`.
- Structured decision controlled policy fixture self-check: 10 offline fixture cases, expected-valid / expected-abstain / expected-review fields matched, `POLICY_FIXTURE_SELF_CHECK=true`, `JAVA_IMPLEMENTATION_EXECUTED=false`, `PRODUCTION_BENCHMARK=false`. Runtime implementation evidence comes from JUnit and integration tests, not from interpreting this Python fixture as production model accuracy.
- H2 migration validation: `V2__structured_output_citation_abstention.sql` applied after the frozen Phase 3 H2 schema and both new tables were queryable.
- Provider tests used local stubs/mocks only; no real Provider request was made.

## 2026-07-30 Evidence / frontend replay / CI closeout

- Backend: `backend/ mvn test` passed with `Tests run: 86, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.
- Deterministic runtime coverage: H2 + local HTTP stub tests cover valid and invalid citations, no-retrieval abstention, malformed structured output, Provider HTTP failure, persistence, review gate, and read-only `IMMUTABLE_RUN` replay. Automated tests made no real Provider request.
- Frontend: `frontend/ npm test` passed with 6 tests, including no manual Bearer Token control and no raw HTTP error-body exposure; `npm run build` passed. CI now executes `npm ci`, `npm test`, then `npm run build`.
- Offline policy fixture: `py scripts/evaluate_structured_decision_demo.py` passed 10 fixtures and printed `fixturePolicySelfCheck=true`, `JAVA_IMPLEMENTATION_EXECUTED=false`, `PRODUCTION_BENCHMARK=false`, `MODEL_QUALITY_METRIC=false`, and `REAL_PROVIDER_BENCHMARK=false`.
- RAG fixture evaluation: `py scripts/evaluate_rag_demo.py` passed 16 synthetic cases and refreshed `docs/metrics/rag_metrics_latest.json` plus `docs/metrics/rag_metrics_snapshot.md`. These are local fixture metrics, not real Provider/model quality or production performance.

## 2026-09-02 追加验证：PHASE_5 前端真实链路与 Demo 边界

本节记录本轮中断后续跑的实际命令和浏览器验收，不覆盖或改写历史记录。

### 前端构建与截图

- 运行目录：`D:\workhome\enterprise-ai-ticket-copilot\frontend`
- `npm run build`：通过；`vue-tsc -b --pretty false` 通过，Vite 6.4.3 生产构建通过。
- `npm run screenshots`：第一次因 5173 已被其他工作区 Vite 占用而未执行页面截图；随后在 5180 启动本项目 `npm run dev:demo -- --host 127.0.0.1 --port 5180 --strictPort`，使用 `SCREENSHOT_URL=http://127.0.0.1:5180 npm run screenshots` 通过。
- 截图脚本覆盖 8 个 Showcase 目标的标准与 `1920x1200` 截图，并通过 1366 桌面和 390 移动端横向溢出检查。

### Demo 浏览器 smoke

- 使用 Chrome headless 访问 `http://127.0.0.1:5180/#ticket-detail`。
- 验证 `DEMO-0005` 点击本地 Demo Copilot 后生成 `DEMO-*` run、`DEMO_LOCAL` evidence 和可用复核按钮；点击 Approve 后详情状态为“已解决”，本地 review history 包含 `APPROVED_RESOLUTION`。
- 结论：截图模式的 Trace、运行、复核不再是空白静态页面，但所有 Demo 证据仍明确标记为非后端持久化数据。

### 真实 API 浏览器 smoke

- 使用现有测试 classpath，以 `test` profile + schema-only H2 启动 Spring Boot 后端端口 28080；未连接本地 MySQL、未调用真实 Provider、未使用真实数据。
- 使用 Vite 真实模式端口 5181，代理目标为 `http://127.0.0.1:28080`。
- Chrome headless 验证：`/api/health` 返回 HTTP 200；通过页面创建合成工单，运行后端 Copilot；空 H2 知识库按预期返回 `NO_RETRIEVAL_EVIDENCE` 并进入“待人工复核”；Approve 后详情状态为“已解决”，review history 可见。
- 该 schema-only smoke 没有伪造 Citation-positive 结果；已有后端 JUnit / H2 集成测试覆盖检索命中、结构化输出、Citation 校验和 `IMMUTABLE_RUN` Trace。

### 本轮未覆盖

- 前端仓库没有独立 Vitest / Playwright 测试 runner，本轮未新增生产依赖；浏览器 smoke 通过一次性 Playwright 脚本执行，不能表述为持续集成测试套件。
- 未用真实 MySQL 做前端联调；默认 8080、18080、18081 均有其他本地服务占用，因此使用隔离 28080 H2 实例完成真实 API 语义验证。
- 未执行真实 Provider 请求；未修改后端业务逻辑、数据库迁移或评测算法。

## 追加验证记录：2026-09-03 PHASE_6 前端全量重做与浏览器验收

本节记录网络中断后的重新验证结果，追加在历史报告末尾，不覆盖此前记录。后端源码、数据库迁移和评测算法未因本轮前端重做而修改。

### 前端构建

- 运行目录：`D:\workhome\enterprise-ai-ticket-copilot\frontend`
- 执行命令：`npm run build`
- 实际结果：通过；`vue-tsc -b --pretty false` 通过，Vite 6.4.3 完成生产构建，`60 modules transformed`。

### 截图与响应式验收

- 执行命令：`$env:SCREENSHOT_URL='http://127.0.0.1:5182'; npm run screenshots`
- 实际结果：通过；8 个 Showcase 目标均来自真实 Demo 页面浏览器渲染。
- 输出目录：
  - `docs/images/`：标准 `1440x960`。
  - `docs/images/large/`：大屏 `1920x1200`。
  - `docs/images/mobile/`：移动端 `390x844`。
- 脚本同时验证 `1366x900` 和 `390x844` 的 `document.scrollWidth`，未发现横向溢出；本轮还补充了截图失败时关闭 Chromium 的清理路径。
- 目视复核：Workbench、Dashboard、Human Review、Trace Timeline、Retrieval Evidence、Knowledge Base、Evaluation / Metrics 的截图均有内容、关键状态可见，未引入远程图片、Logo 或字体。

### Demo 浏览器 smoke

- 运行页面：`http://127.0.0.1:5182/#ticket-detail`
- 验证路径：本地 Copilot → `DEMO_LOCAL` evidence → Human Review Approve → `已解决` / `APPROVED_RESOLUTION`。
- 额外交互：队列搜索 `Redis` 得到 1 条匹配；Ctrl/Cmd+K 搜索并进入 `Trace Timeline`；Approve 确认对话框已接受。
- 实际结果：通过；`consoleErrors=[]`、`pageErrors=[]`。

### Real API 浏览器 smoke

- 后端：Spring Boot `test` profile + schema-only H2，端口 `28081`；前端：Vite 真实模式端口 `5184`，代理目标 `http://127.0.0.1:28081`。
- 数据边界：仅使用脚本默认的 synthetic ticket 字段；未连接本地 MySQL，未把真实用户、账号、IP、凭据或秘密写入工单。
- 验证路径：`GET /api/health=200` → UI `POST /api/tickets=200` → `POST /api/tickets/{id}/run-copilot=200` → 读取 detail、analysis、metrics、Trace → `POST /review/approve=200` → `RESOLVED`。
- 关键后端事实：Trace 返回 `evidenceSource=IMMUTABLE_RUN`；检索快照为 `KB-OPS-003`（1 条）；`outputValidationStatus=VALID`；`citationValidationStatus=VALID`；审核历史包含 `APPROVED_RESOLUTION`。
- Provider 边界：该进程继承了机器已有的可选 Provider 环境，因此后端尝试 OpenAI-compatible 路径并收到 HTTP 403，按现有策略记录 `PROVIDER_ERROR` / `PERMISSION_DENIED` 并安全 fallback 到 `local-rule`。这证明了 fallback 和人工复核路径，不证明真实模型调用成功；本轮没有新增、打印或提交 API Key。
- 实际结果：通过；浏览器捕获的 API 请求均返回预期 HTTP 状态，console/page error 均为 0。

### 后端回归

- 运行目录：`D:\workhome\enterprise-ai-ticket-copilot\backend`
- 执行命令：`mvn test`
- 实际结果：通过；`Tests run: 84, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

### 未覆盖与交付边界

- 前端项目没有独立 Vitest / Playwright 测试 runner；浏览器 smoke 是一次性可重复验收脚本，不表述为持续集成测试套件。
- 没有成功的真实模型 Provider 集成结果；当前仓库默认仍以 `local-rule` / fallback 为安全演示路径，真实 Provider 只通过后端可选配置和本轮失败后 fallback 语义验证。
- 未公网部署、未修改后端业务链路、未提交或推送 Git、未修改另一个项目。

## 2026-09-03 追加验证：PHASE_7 参考案例导向浅色客服工作台

本节记录针对用户反馈进行的视觉方向修订及其重新验收。该轮只调整前端视觉、可见文案、截图和文档，不改变后端业务链路、API、数据库迁移、Provider 或评测指标口径。

### 前端构建

- 运行目录：`D:\workhome\enterprise-ai-ticket-copilot\frontend`
- 执行命令：`npm run build`
- 实际结果：通过；`vue-tsc -b --pretty false` 通过，Vite 6.4.3 完成生产构建，`60 modules transformed`。

### 截图与响应式验收

- 启动：`npm run dev:demo -- --host 127.0.0.1 --port 5190 --strictPort`
- 执行命令：`$env:SCREENSHOT_URL='http://127.0.0.1:5190'; npm run screenshots`
- 实际结果：通过；8 个 Showcase 目标均由真实浏览器渲染并保存截图。
- 输出目录：`docs/images/`（1440×960）、`docs/images/large/`（1920×1200）、`docs/images/mobile/`（390×844）。
- 脚本检查了 1366×900 与 390×844 的 `document.scrollWidth`，未发现横向溢出；截图脚本捕获的 console/page error 均为空。
- 目视验收：Dashboard、Ticket Workbench、Knowledge Base、Retrieval Evidence、Trace Timeline、Human Review、Evaluation / Metrics 均有真实内容和明确状态；当前呈现为浅灰画布、白色面板、克制蓝色操作和低饱和状态色，没有渐变、霓虹发光、外部图片、Logo 或远程字体。

### Demo 浏览器 smoke

- 访问页面：`http://127.0.0.1:5190/#ticket-detail`
- 验证路径：队列加载 8 条 → P1 筛选 / 重置 → 选择 `DEMO-0002` → 本地 Copilot → Approve → 工单状态显示“已解决” → Ctrl/Cmd+K 搜索“人工”并进入 Human Review。
- 实际结果：通过；确认对话框已接受，`consoleErrors=[]`、`pageErrors=[]`。本地运行和证据仍明确属于 Demo 内存 fixture，不是后端持久化结果。

### 后端回归

- 运行目录：`D:\workhome\enterprise-ai-ticket-copilot\backend`
- 执行命令：`mvn test`
- 实际结果：通过；`Tests run: 84, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

### 本轮未覆盖与边界

- 本轮是样式与可见文案修订，没有重新启动真实 API 浏览器服务；Phase 6 的真实 API smoke 记录继续作为历史证据保留。
- 本轮没有成功的真实模型 Provider 集成结果；Demo 仍使用合成数据、keyword retrieval / local-rule fallback / citation gating 语义，不能写成真实模型效果。
- 前端没有独立 Vitest / Playwright 持续测试 runner；本轮使用一次性可重复 Playwright smoke 和截图脚本验收。
- 未公网部署、未新增生产依赖、未添加真实密钥、未修改后端源码或 `docs/metrics/`、未提交或推送 Git。

## 2026-09-03 追加验证：PHASE_8 参考案例导向品牌细修

本节记录第二轮案例研究后的前端品牌 / 图标 / 字体 / 色彩细修。只改前端视觉和可见文案，不改变 API、后端工单流转、Demo fixture 或评测口径。

### 公开案例研究

- 通过 `agent-reach` 网页通道和备用网页检索阅读了 Intercom Inbox、Zendesk Agent Workspace、Jira Service Management、ServiceNow CSM Workspace、Linear Search，以及 Atlassian Design / Primer 的公开资料。
- 提取的可迁移原则是：队列先于装饰、单工单上下文、右侧关联信息、优先级语义、活动流、搜索快捷入口、设计 token、清晰的 typography / spacing / border / elevation 和统一 iconography。
- 未下载、嵌入或复制第三方 Logo、产品截图、字体文件、图标包、源码或品牌页面；实现使用项目自绘 `BrandMark.vue` 和 `NavIcon.vue`。

### 前端构建

- 运行目录：`D:\workhome\enterprise-ai-ticket-copilot\frontend`
- 执行命令：`npm run build`
- 实际结果：通过；`vue-tsc -b --pretty false` 通过，Vite 6.4.3 完成生产构建，`63 modules transformed`。

### 浏览器截图与视觉验收

- 启动：`npm run dev:demo -- --host 127.0.0.1 --port 5190 --strictPort`
- 执行命令：`$env:SCREENSHOT_URL='http://127.0.0.1:5190'; npm run screenshots`
- 实际结果：通过；8 个目标页面的 1440×960、1920×1200 和 390×844 截图已重新生成。
- 通过 1366×900 与 390×844 横向溢出检查；截图脚本采集的 console/page error 均为空。
- 目视确认：品牌 mark、七组导航线性图标、温润中性色画布、靛蓝主操作、赤陶色识别点、中文指标文案和 Workbench 三栏关系均正常；移动端导航与队列内容没有明显截断或重叠。

### Demo 浏览器 smoke

- 访问：`http://127.0.0.1:5190/#ticket-detail`
- 验证：等待 8 条 Demo 队列 → P1 筛选得到 3 条并恢复全部 → 选择 `DEMO-0002` → 运行 Copilot → Approve → 工单状态 badge 为“已解决” → Ctrl/Cmd+K 搜索“人工” → 进入“人工复核中心”。
- 实际结果：通过；`consoleErrors=[]`、`pageErrors=[]`，确认对话框已接受。

### 工具与范围检查

- `agent-reach check-update`：当前 v1.5.0，已是最新版本。
- `git diff --check`：退出码 0；仅有 Git 的 LF / CRLF 提示，没有空白错误。
- `git diff --name-only -- backend docs/metrics`：无输出，后端和评测指标目录未被本轮修改。
- 本轮未重跑真实 API 浏览器服务；Phase 6 的真实 API smoke 记录继续作为历史证据。未公网部署、未新增生产依赖、未添加真实密钥、未执行 Git commit/push。

## 2026-09-03 追加验证：PHASE_9A 交互与可访问性基础

本节记录网络中断后继续执行的前端规划与第一阶段实现；只覆盖命令中心交互、核心可读性 token 和验收，不把后续 9B～9G 写成已完成，也不改变后端业务链路。

### 前端构建与截图

- 运行目录：`D:\workhome\enterprise-ai-ticket-copilot\frontend`
- `npm run build`：通过；`vue-tsc -b --pretty false` 通过，Vite 6.4.3 完成生产构建，`63 modules transformed`。
- `SCREENSHOT_URL=http://127.0.0.1:5190 npm run screenshots`：通过；8 个目标页面均重新生成标准、大屏和 390×844 移动端截图。
- 截图脚本通过 1366×900 与 390×844 横向溢出检查；截图产物位于 `docs/images/`、`docs/images/large/` 和 `docs/images/mobile/`。
- 目视复核了 Dashboard、Ticket Workbench 和移动端 Workbench；没有看到由本轮交互语义 / token 修改引入的截断、重叠或布局回归。

### 浏览器 smoke

- 使用本机 Chrome headless 运行一次性 Playwright 脚本访问 `http://127.0.0.1:5190` Demo 页面。
- 命令中心：验证打开后焦点进入 input，7 个结果图标由 `NavIcon` 渲染，↑ / ↓ 更新 `aria-activedescendant`，Tab / Shift+Tab 在弹层内循环，Escape 关闭并恢复搜索按钮焦点，输入“人工”后 Enter 跳转 `#human-review`。
- Demo 工单：选择 `DEMO-0002`，运行本地 Copilot，接受 Approve 确认后状态变为“已解决”。
- 实际结果：通过；`consoleErrors=[]`、`pageErrors=[]`。浏览器脚本按真实 Demo 延迟等待状态完成，未把原生 `window.confirm` 误判成页面内 ARIA dialog。

### 静态可读性与边界

- 核心 token 静态检查已覆盖白色面板上的 muted / amber / red 文字角色；目标是降低此前弱化文本对比度风险。
- 本轮未运行 axe、Lighthouse 或独立无障碍测试 runner，因此不能把静态 token 检查表述为完整 WCAG 合规结论。
- `backend/mvn test` 在本轮文档收口后重新执行并通过 84 tests、0 failures / errors / skipped，`BUILD SUCCESS`；本轮没有改后端源码、数据库迁移、Provider、`docs/metrics/` 或 API。
- 本轮没有重跑真实 API 浏览器 smoke；Phase 6 的隔离 H2 + Vite 真实模式证据仍为历史记录。未新增生产依赖、真实密钥、外部品牌资产、commit、push 或 deploy。

## 2026-09-03 PHASE_9G 前端发布验收追加记录

### 本轮范围

本轮是两套项目的最终前端验收和交接收口。工单项目只做前端表单语义补强、构建、截图和回归验证；电商项目只复用现有业务链路做全量回归与浏览器验收。没有改工单后端、数据库迁移、Provider、真实密钥或 `docs/metrics/`，没有执行 commit、push 或 deploy。

### 自动化结果

| 项目 | 命令/范围 | 实际结果 |
| --- | --- | --- |
| 工单前端 | `npm run typecheck`、`npm run build` | 通过；`vue-tsc` 通过，Vite `63 modules transformed` |
| 工单后端 | `backend/ mvn test` | 84 tests，0 failures，0 errors，0 skipped；`BUILD SUCCESS` |
| 工单截图 | `SCREENSHOT_URL=http://127.0.0.1:5190 npm run screenshots` | 通过；8 个目标页面生成标准、大屏、移动截图 |
| 工单静态 a11y | `a11y_scanner.py frontend/src` | 修复表单关联后为 0 critical、63 serious、25 moderate；剩余为组件壳级启发式提示，不等同 axe/Lighthouse 结果 |
| 电商 Admin | `npm test`、`npm run build` | 45/45 通过；生产构建通过 |
| 电商 H5 | `npm test`、`npm run build`、`npm run build:uni` | 35/35 通过；H5 与 UniApp H5 构建通过 |
| 电商 Java | `mvnw.cmd -f apps/mall-api/pom.xml test` | 53/53 通过；构建成功 |
| 电商 Python | `py -3 -m pytest` | 11/11 通过 |
| 电商展示验收 | `scripts/showcase/verify.ps1 -IncludeMobile -IncludeOrderSmoke -IncludeRateLimitSmoke` | `SHOWCASE_VERIFY_OK`；订单回放同订单号、冲突 409、限流 5 次允许/1 次拦截 |

### 浏览器与边界结果

- 电商 H5 在 390、768、1024、1440 宽度下均无横向溢出，破图数为 0；搜索、分类和价格排序可用。
- 电商 Admin 在 390、768、1024、1440 宽度下均无横向溢出，破图数为 0；AI 客服请求通过本地 Java API 返回库存回答，并展示 `MOCK` Provider、证据和 Trace 边界。
- 工单 Demo 的运行、Approve、状态变为“已解决”和 `APPROVED_RESOLUTION` 历史记录已在 PHASE_9D 浏览器 smoke 中通过；隔离 H2 真实 API smoke 已覆盖无检索证据时的安全拒答和人工复核闭环。本轮截图在表单语义修复后重新生成。
- 当前没有真实 DeepSeek/OpenAI/GPT Provider 成功请求、公网 DNS/HTTPS、生产 MySQL 或部署证据；下一阶段后端接口、身份/RBAC、Provider 和 staging 路线已写入 `docs/design/BACKEND_NEXT_PHASE_DESIGN.md`，但仍属于设计，不应写成已经上线的能力。

## 2026-09-03 BACKEND-T0 API 契约冻结追加记录

### 本轮实现

- 新增 `TicketStatusContract`，集中管理当前持久化/DTO 状态字符串和通用人工状态接口目标白名单。
- `TicketWorkflowService` 继续保留兼容常量，但底层引用统一合同，避免后续包迁移造成状态值漂移。
- 新增 `ApiRouteContractTest`，锁定现有 Auth、Health、Ticket Controller 的 class mapping、HTTP 方法和路径。
- 新增 `TicketStatusContractTest`，覆盖状态顺序、`PENDING_CLASSIFICATION` 的内部 intake 边界、未知/大小写错误值和公共目标状态。
- 新增 [BACKEND_T0_API_CONTRACT.md](design/BACKEND_T0_API_CONTRACT.md)，同步 API/架构文档中的迁移边界。

### 实际验证

```text
cd backend
mvn test
```

实际结果：`Tests run: 90, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。其中新增合同测试 6 个，原有工单工作流、Provider、Citation、结构化输出、H2 集成和 Controller 测试均继续通过。

本轮只冻结旧 `/api/...` 合同，没有实际切换 `/api/v1`，没有实现完整状态转移矩阵、模块包迁移、真实身份/RBAC、真实 Provider、Outbox 或公网部署；这些仍按 [后端下一阶段设计](design/BACKEND_NEXT_PHASE_DESIGN.md) 进入后续独立任务。

## 2026-09-03 BACKEND-T1 模块边界追加记录

### 本轮实现

- 新增 `TicketWorkflowUseCase` 入站应用端口，覆盖当前工单 Controller 的列表、创建、详情、分析、Trace、Copilot、人工复核、状态更新和知识草稿用例。
- 新增 `LegacyTicketWorkflowFacade` 过渡适配器。REST 入口改为依赖端口，旧 `TicketWorkflowService` 暂时继续承载原有实现，避免在模块迁移中复制或改变工作流结果。
- 新增 `TicketModuleBoundaryTest`，通过反射固定 Controller → inbound port 的依赖方向，并禁止应用端口方法签名泄漏 persistence Entity、Mapper 和旧 Service 类型。
- 旧 `/api/...` 路径、请求/响应 DTO、数据库字段、Copilot Trace/Citation/Review 证据链均未改变。
- 详细设计见 [BACKEND_T1_MODULE_BOUNDARIES.md](design/BACKEND_T1_MODULE_BOUNDARIES.md)。

### 实际验证

```text
cd backend
mvn test
```

实际结果：`Tests run: 92, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。其中包含新增的 2 个模块边界测试，T0 API/状态合同测试和原有工单工作流、Provider、Citation、结构化输出、H2 集成及 Controller 测试均继续通过。

本轮只完成入站边界第一条纵向切片，没有声称完整模块包迁移、出站端口、真实身份/RBAC、真实 Provider、Outbox、数据库迁移或公网部署已经完成；下一轮是独立的 `BACKEND-T2-WORKFLOW-PORTS`。

## 2026-09-03 BACKEND-T2 工作流策略端口追加记录

### 本轮实现

- 新增 `ReviewPolicy` 应用端口，把最终人工复核策略需要的输入限制为结构化输出、Provider fallback、Citation 校验状态、结构化输出校验状态和业务规则标志。
- `TicketWorkflowService` 改为依赖 `ReviewPolicy`，不再直接依赖具体 `ReviewGate`；当前 `ReviewGate` 实现该端口，现有 Spring wiring 继续成立。
- 旧 `ReviewGate.finalHumanReviewRequired(... CitationValidationResult ...)` 保留为兼容包装；新工作流调用 `ReviewPolicy.requiresHumanReview(...)`，并继续覆盖空输出、高风险、fallback、abstention、缺少信息、Citation 失败、结构化输出失败和业务规则复核门禁。
- 新增 `WorkflowPortBoundaryTest`，验证字段/构造器依赖端口、端口签名不泄漏 legacy/persistence 类型，以及当前实现满足端口。
- 详细设计见 [BACKEND_T2_WORKFLOW_PORTS.md](design/BACKEND_T2_WORKFLOW_PORTS.md)。

### 实际验证

```text
cd backend
mvn test
```

实际结果：`Tests run: 94, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。新增的 2 个工作流端口结构测试、原有 6 个 ReviewGate 策略测试、T0/T1 合同和边界测试，以及工作流/Provider/Citation/结构化输出/H2 集成测试均通过。

本轮只完成一个可替换的策略端口，没有实现完整模块迁移、`AiProviderPort`、`RetrievalPort`、`AuditRecorder`、真实身份/RBAC、真实 Provider、Outbox、数据库迁移或公网部署。企业工单后端 T0-T2 范围已完成，后续进入独立电商 E0 或另立生产化任务。
