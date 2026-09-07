# Claude / Codex 交接记录

## 2026-09-07 — LOCAL_RELEASE_CANDIDATE_COMMITTED

- 已在独立分支 `release/ticket-copilot-local-20260907` 形成本地候选提交 `fd8102b119c80e3aed6e2b0707cebbe845d9875e`（`release: assemble ticket copilot staging candidate`）。提交包含后端/前端源码、测试、部署 Compose/Caddy/Nginx 模板、文档和不含真实凭据的 staging `.env.example`；真实 `.env`、构建产物与临时文件未纳入且未删除。
- 该提交只代表本地发布候选：没有 `git push`、没有合并远端 `main`、没有 Docker up/down、云资源、DNS、证书、数据库或公网操作。候选代码提交相对 `origin/main` 为 ahead 1 / behind 1；随后仅增加了本条记录，推送前仍需单独审阅远端差异并取得明确授权。
- 候选提交基于本轮 `LOCAL_PASS` 结果；云端资源/Secret/OIDC/RDS/DNS/ICP/TLS 与公网 staging 仍为 `BLOCKED`/`STAGING_PENDING`。

## 2026-09-07 — RELEASE_CANDIDATE_LOCAL_REAUDIT

- 本次只做本地发布前复验；工作区原有修改和未跟踪文件均保留，没有执行 commit、push、Docker up/down、DNS、证书、云资源或数据库操作。
- 后端 `mvn -B test package` 结果为 289/289、0 failures/errors/skipped，Spring Boot JAR repackage 成功；测试使用项目 test profile 和隔离 H2，不连接云端 MySQL 或工单数据。
- 前端没有独立 npm test script；`npm.cmd run typecheck` 和 `npm.cmd run build` 均成功（Vite 68 modules）。生产依赖 `npm.cmd audit --omit=dev --audit-level=high --json` 为 0 high/critical vulnerabilities。
- staging Compose 普通/TLS `config -q` 均退出 0；`.env.example` 被环境守卫按预期拒绝占位值，审计 fixture 正向通过。`git diff --check` 退出 0，仅有既存 PowerShell LF/CRLF 提示。
- 当前本地验收：`LOCAL_PASS`（后端、前端 typecheck/build、Compose、隔离守卫）；阿里云主机、RDS、OIDC、DeepSeek Secret、DNS/ICP/TLS 和公网 staging 仍 `BLOCKED`/`STAGING_PENDING`。本地 `main` 比 `origin/main` 落后 1 个提交，工作区有 58 个已跟踪状态条目和 44 个未跟踪状态条目；尚未形成可推送 release commit。


## 2026-09-05 — STAGING_PHASE_A_AUDIT

- 当前总状态BLOCKED；最新`backend/mvn -B test package`为289/289，0失败/错误/跳过，前端typecheck/production build及npm生产审计、配置守卫、schema-only MySQL、Compose普通/TLS静态校验为LOCAL_PASS；云端STAGING_PENDING。
- 已完成本地整改：移除Demo SQL和本地 MySQL 挂载，加入 schema-only 版本化基线与 checksum/备份流程，改为显式 RDS 私网配置，递归排除构建上下文 Secret，并让 staging/production 缺失数据库 URL 时快速失败；没有执行云端 SQL 或公网部署。
- 本地工单API Key仅做存在性分类，未输出值、未新调用Provider；不借用电商数据或验收。阿里云控制台跳转登录页，资源/ICP/证书BLOCKED。
- 独立报告：[STAGING_AUDIT_PLAN_20260905](docs/release/STAGING_AUDIT_PLAN_20260905.md)。日志：D:/workhome/deployment-audit-20260905-1800/ticket-*。PRD.md实际缺失，未编造；保留既有修改，无购买/DNS/证书/公网部署/提交/推送。旧历史中的未实现OIDC、84条测试等不代表当前审计版本。
- 本轮整改复验汇总：`D:/workhome/deployment-remediation-20260905/REMEDIATION_RESULTS.md`；源码/产物哈希清单已同步到 `D:/workhome/deployment-audit-20260905-1800/`。
- 用户提供的阿里云截图显示华东1（杭州）轻量应用服务器 `OpenClaw-astw` 与 `wzl8.top`“已备案”；两者仅作为只读候选证据，未确认主机可占用、未修改 DNS、未启动项目。
- 2026-09-06 Workbench 只读检查显示主机 Docker 24.0.9 active、约 1.0 GiB 可用内存，80/443 已被现有服务占用，且有长期运行的 `searxng` 容器；主机作为两个项目 staging 目标为 BLOCKED，不停止或覆盖现有 OpenClaw/SearXNG 服务。

使用规则：每轮把新记录追加在"历史记录"顶部，不覆盖旧记录。没有证据时不要写"测试通过"。

## 2026-09-05 — PHASE_2_REAL_DEEPSEEK_SYNTHETIC

- 已用 GitHub/官方文档调研得到的 Provider 边界、结构化输出和可观测性原则，补充 `scripts/local/verify-deepseek-synthetic.ps1`；脚本只接受本地未跟踪环境文件，把 API Key 注入临时 Spring Boot 进程，不写入前端、源码、日志或证据。
- 企业工单真实 DeepSeek 合成验收通过：`provider=deepseek`、`model=deepseek-chat`、`run=SUCCESS`、`fallbackUsed=false`、1 条 validated citation、结构化输出/Citation membership `VALID`，Reviewer 后 `RESOLVED`。
- 脱敏证据：`docs/evidence/deepseek-synthetic-smoke-20260905.md`。临时端口已释放，临时日志未发现 Provider Key。
- Phase 2 只证明两个项目的真实 Provider 成功路径；真实 IdP、staging 失败矩阵、云端备份恢复、DNS/TLS、监控和公网部署仍未完成。

## 2026-09-05 — PHASE_1_LOCAL_ACCEPTANCE

- 本地 Phase 1 已收口：后端 `mvn -B test` 为 289/289，前端构建通过，新增 `scripts/local/verify-http-smoke.ps1` 的 H2 HTTP smoke 通过创建 → Copilot → Trace → Reviewer Approve，最终 `RESOLVED`。
- 这条脚本使用 test profile、test classpath 和文件型 H2 schema，日志写入系统临时目录；临时 Java 进程按独立端口启动并在 finally 中清理。
- 双项目总路线见 [PROJECT_COMPLETION_ROADMAP_20260905.md](../PROJECT_COMPLETION_ROADMAP_20260905.md)。Phase 2 真实 Provider、Phase 3 真实 IdP 和后续阿里云部署仍是未验收边界。

## 2026-09-05 — FRONTEND_CRAFT

- 根据用户要求继续两项目的前端精修，工单限定为现有工作台与共享外壳，没有重构后端或更改认证/Provider。
- 新增 frontend/src/craft.css，main.ts 导入；工作台新增原生创建 dialog，完整证据和审核历史折叠；取消预填已核对证据的审核备注，默认表单改为中文合成样例。
- 已修复截图检查发现的队列内容挤窄问题。前端 npm run build 通过（含 vue-tsc，68 modules）。跨项目脚本 D:/workhome/frontend-craft-qa.cjs 最终 53 组页面/状态通过；工单 Demo 创建、Copilot、批准、历史、Escape 和焦点恢复断言通过。
- 设计规范见 docs/design/FRONTEND_CRAFT.md，主设计文档已指向该补充规范；详细验收见 D:/workhome/frontend-craft-20260905/验收报告.md。
- 保留全部既有未提交修改；未读/输出密钥、未调用真实模型、未启动数据库、未 commit/push/deploy。5295 工单 Demo 开发服务供本地查看；不是公网部署。

---

## 当前交接更新

### 2026-09-04 — Codex — REAL_DEEPSEEK_ISOLATED_PROVIDER_SMOKE

- 本机未跟踪的 `deploy/staging/.env` 已按 DeepSeek OpenAI-compatible 配置完成；密钥只在子进程运行时环境中使用，未读取到对话、日志、源码、文档或 Git 状态中。
- 先执行一条最小、全合成的 Chat Completions 请求：DeepSeek 返回 HTTP `200`，返回内容可解析为严格 JSON，且具备项目结构化输出合同所需字段。
- 再启动临时 Spring Boot `test` profile + H2 内存库，以 `KB-OPS-003` 作为测试知识证据，完整走 Demo 登录 → 创建合成工单 → `run-copilot` → Trace → Reviewer Approve。实际结果为：`requestedProvider=deepseek`、`actualProvider=deepseek`、`runStatus=SUCCESS`、`fallbackUsed=false`、`errorCategory=NONE`、1 条 retrieval snapshot、`VALID` structured output、`VALID` Citation membership、1 条 validated citation；审核后状态为 `RESOLVED`，review history 含 `APPROVED_RESOLUTION`。
- 临时进程和 28180 端口均已关闭/释放；没有修改 MySQL、Docker、域名、DNS、云主机或公网部署。详细且不含密钥/原文的证据见 `docs/evidence/deepseek-isolated-smoke-20260904.md`。
- 验证基线：后端全量 `mvn -B test` 为 289/289，0 failures/errors/skipped；此前前端 type-check/production build、production dependency audit 与 Compose 静态校验均已通过。仍未验证真实 IdP token、公网 HTTPS/DNS、真实 MySQL 迁移、备份/监控、多副本共享限流或生产数据质量；没有 commit、push 或 deploy。

### 2026-09-04 — Codex — SECURITY_HARDENING_AND_RELEASE_GATES

- 企业工单已补齐 staging/production 启动守卫：OIDC issuer、数据库账号、支持的 Provider、Provider base/model/key 均为必填，且 `TICKET_AI_FALLBACK_TO_LOCAL=true` 会直接阻止不安全启动；public profile 不会静默回退到 Demo 或 local-rule。
- Copilot 运行路由增加按已认证用户的进程内限流（默认 `6 / 60s`），耗尽时返回 `429`、`Retry-After` 和标准 rate-limit headers。多副本 Alibaba Cloud 部署仍必须在网关/WAF 或 Redis 再加一层共享限流，不能把本地内存限流宣称为分布式防护。
- staging/production 已关闭 SpringDoc/Swagger；Nginx 增加 CSP、`nosniff`、禁止 frame/object、Referrer/Permissions Policy，Caddy TLS profile 增加 HSTS。没有新增真实密钥或公开数据库端口。
- 前端 production dependency audit 已为 0 vulnerabilities，CI 新增 `npm audit --omit=dev --audit-level=high` 和 staging Compose interpolation gate。
- 验证：`backend/mvn test` 为 288/288（0 failures/errors/skipped）；`frontend/npm run build`、`npm audit --offline --omit=dev --audit-level=high`、`docker compose ... config -q` 均通过。没有真实 IdP/Provider 成功响应、云/SSH 凭据、commit、push 或 deploy。

## 当前交接更新

### 2026-09-04 — Codex — REAL_AUTH_PROVIDER_DEPLOYMENT_PREPARATION

- 在保留工作区原有未提交修改的前提下，补齐企业工单的可选 OIDC/JWT Resource Server：OIDC 模式使用 issuer discovery/JWKS 验证 bearer token，支持可选 audience 校验和角色映射；Demo 模式继续保留本地测试链路。Staging/production profile 已选择 OIDC，未配置 issuer 时不会静默回到 Demo。
- 前端增加 Authorization Code + PKCE 适配和企业登录控件；AI API Key 只通过后端运行时环境变量注入，未进入浏览器配置、源码或截图。
- 新增 `deploy/staging/` 的 MySQL、Java API、Nginx edge 和可选 Caddy TLS 部署骨架，新增 [真实认证、Provider 与部署说明](docs/REAL_AUTH_PROVIDER_DEPLOYMENT.md)。该骨架已通过 `docker compose --env-file deploy/staging/.env.example -f deploy/staging/docker-compose.yml config -q` 静态校验，但没有启动 Docker 服务或公网部署。
- 企业工单后端全量回归：283/283 通过，0 failures、0 errors、0 skipped；前端 type-check 与 production build 通过。
- 本机共享 OpenAI-compatible Provider 验证返回 HTTP 403，因此不能记录为真实模型成功；没有真实 IdP token、云/SSH 凭据或公网发布权限。没有执行 reset、checkout、clean、commit、push 或 deploy。

## 当前交接更新

### 2026-09-04 — Codex — PHASE_0-A_TICKET_WORKFLOW_STATE_AND_WRITE_AUTHORIZATION

- 本轮只实施企业工单 Phase 0-A；保留工作区原有未提交修改，没有执行 `reset`、`checkout`、`clean`、commit、push 或 deploy，没有修改 CommerceFlow、电商/前端视觉、真实 Provider、OIDC/OAuth2、公网 DNS/证书、Outbox/队列/Worker 或数据库 schema。
- 前置检查已执行：`git status --short`、`git diff --stat`、`git diff --check`；已阅读 `AGENTS.md`、当前 `README.md`、`HANDOFF.md`、`TODO.md`、`docs/TEST_REPORT.md`、`docs/architecture.md`、`docs/DESIGN.md` 及本轮指定后端源码/测试。`docs/PRD.md` 文件缺失，未编造 PRD。
- 新增 `backend/src/main/java/com/enterpriseai/ticketcopilot/ticket/application/policy/TicketStatusTransitionPolicy.java`：集中约束初始分类、Copilot 首次运行/重跑、审核通过/要求修改/驳回、四条既有合法手工状态边、知识草稿和知识发布；所有没有现有代码、架构说明或 Showcase 流程依据的已知状态边拒绝为 409。保留 `PENDING_PROCESS -> RESOLVED` 与 `RESOLVED -> IN_PROGRESS`，没有为了收敛矩阵而破坏现有流程。
- `TicketWorkflowService` 的 `updateStatus`、`applyReviewDecision`、`runCopilot` 状态变化、`createKnowledgeDraft`、`confirmKnowledgeDraft` 和 `publishDraft` 均调用集中策略。已发布知识确认继续幂等且不追加虚假历史；同一 Copilot Run 的审核决策不可重复；没有当前 Run 不能审核；Copilot 解决的工单在知识沉淀前必须有完成审核。
- 状态持久化增加 `support_ticket.id + expected status` 条件更新。竞争更新影响行数不是 1 时返回 409；事务边界保证失败请求不会留下状态历史、审核记录或知识发布副作用。条件更新同时保存 Copilot 更新后的 `category` 和 `ai_confidence`，避免回归旧成功路径。
- 新增 `backend/src/main/java/com/enterpriseai/ticketcopilot/ticket/application/policy/TicketAuthorizationPolicy.java`：集中定义 ADMIN、AGENT、REVIEWER、VIEWER 对创建工单、手工状态、Copilot、三种审核、创建草稿、确认/发布知识的权限。`confirm=true` 按发布权限校验；匿名写请求由现有拦截器返回 401，角色不足返回 403。`TicketController` 不再复制散落角色判断，读接口的现有认证边界保持不变。
- 新增 `backend/src/test/java/com/enterpriseai/ticketcopilot/TicketWorkflowPhase0AIntegrationTest.java`：59 个参数化/集成用例从真实 Demo 登录、拦截器、Controller 走到 H2，覆盖五类身份、所有写入口、HTTP 错误码、ticket/history/review/run/analysis/generation/knowledge 前后快照、非法转移和重复操作。
- 新增状态策略与授权策略单测；`TicketWorkflowServiceTest` 增加条件更新丢失竞争时的 409 断言，并更新知识发布 mock；已有的 `TicketModuleBoundaryTest` 因 Controller 合法增加授权策略依赖而同步边界预期，未删除或跳过原测试。
- 新增 `docs/design/TICKET_WORKFLOW_STATE_AND_AUTHORIZATION.md`，并同步 `docs/API.md`、`docs/architecture.md`、`TODO.md`；API 文档补充 401/403/409 语义，状态与授权说明明确当前字符串状态合同、合法边、拒绝策略、权限矩阵、条件更新和 Demo/生产认证边界。
- 验证：受影响测试集 `mvn -B '-Dtest=TicketWorkflowServiceTest,TicketWorkflowPhase0AIntegrationTest' test` 为 66/66；后端全量 `mvn -B test` 为 282/282，0 failures、0 errors、0 skipped，`BUILD SUCCESS`。中间首次全量因边界测试仍期待旧的单构造器依赖而失败，更新必要边界预期后已重新全量通过；首次服务单测遇到 MyBatis-Plus Lambda cache 的纯 Mockito 环境问题，改为字段名明确的 `UpdateWrapper` 后受影响集通过。
- 前端验证：`frontend/npm run build` 通过，包含 `vue-tsc`，Vite `63 modules transformed`；当前 `frontend/package.json` 没有 `test` script，因此没有执行不存在的 `npm test`。本轮没有修改前端源码或截图。
- 认证边界：当前仍是四个硬编码 Demo 用户 + 自定义 HMAC-SHA256 Bearer token，不能称为生产级认证/RBAC；OIDC/OAuth2、JWK/JWKS、issuer/audience、租户隔离、生产密钥托管和资源级 RBAC 均未实现。未接入真实 GPT/DeepSeek、未公网部署、未执行任何外部账号/发布操作。

### 2026-09-03 — Codex — PHASE_9G frontend release acceptance

- 本轮完成 `PHASE_9G` 前端发布验收，并补了一个由静态无障碍检查发现的语义修复：`TicketWorkbenchShowcaseView.vue` 的工单字段和 `HumanReviewShowcaseView.vue` 的复核意见框现在都有明确的 `label` / `id` 关联。
- 本轮未执行 `git reset`、`git checkout`、清理用户文件、commit、push 或 deploy；没有修改工单后端、数据库迁移、Provider、真实密钥或 `docs/metrics/`。
- 工单前端：`npm run typecheck` 通过；`npm run build` 通过，`vue-tsc -b --pretty false` 通过，Vite 完成 `63 modules transformed`。`SCREENSHOT_URL=http://127.0.0.1:5190 npm run screenshots` 通过，8 个目标页面的标准、大屏、移动截图已生成到 `docs/images/`、`docs/images/large/` 和 `docs/images/mobile/`。
- 工单后端回归：`backend/ mvn test` 通过，`84 tests, 0 failures, 0 errors, 0 skipped`，`BUILD SUCCESS`。本轮没有新增后端实现。
- 电商自动化回归：Admin `npm test` 为 45/45，H5 `npm test` 为 35/35，Java `mvnw.cmd -f apps/mall-api/pom.xml test` 为 53/53，Python `py -3 -m pytest` 为 11/11；Admin/H5 `npm run build`、H5 `npm run build:uni` 均通过。
- 电商运行验收：`scripts/showcase/verify.ps1 -IncludeMobile -IncludeOrderSmoke -IncludeRateLimitSmoke` 通过；订单幂等回放返回同一订单号、冲突返回 409，限流验证为 `allowed=5 blocked=1`，最终输出 `SHOWCASE_VERIFY_OK`。
- 电商浏览器验收：H5 在 390/768/1024/1440 宽度均无横向溢出、破图数为 0；搜索“托特”、分类筛选和价格排序可用。Admin 在同样宽度下无横向溢出、破图数为 0；AI 客服真实调用本地 Java API，返回库存回答、`MOCK` Provider 边界、证据和 Trace 信息，没有伪装成真实外部模型。
- 工单浏览器证据：此前 PHASE_9D 已完成 Demo 工单运行 → Approve → “已解决” → `APPROVED_RESOLUTION` 历史记录；此前隔离 H2 真实 API smoke 已验证 health、创建工单、无检索证据安全拒答、复核和状态闭环。本轮重新生成截图并在表单语义修复后复核构建；没有把当前截图验收写成真实 Provider 成功调用。
- 静态无障碍检查：修复前为 7 critical / 63 serious / 27 moderate，修复后为 `0 critical / 63 serious / 25 moderate`。剩余项主要是对独立 Vue 子组件运行启发式扫描时产生的 `main`、skip link、`h1`、`nav` 页面壳提示；实际 App 壳已有 skip link、`main#main-content` 和页面级 h1。本项目没有完整 axe/Lighthouse 持续测试套件，因此不宣称完整 WCAG 合规。
- 后端下一阶段已新增设计文档 `docs/design/BACKEND_NEXT_PHASE_DESIGN.md`，内容覆盖接口版本、身份/RBAC、知识检索、Provider SPI、不可变运行记录、可靠性和 staging 路线；它是下一阶段设计，不代表这些生产能力已经实现。
- 交接边界：当前本地前端、Demo 链路和已有隔离真实 API 链路达到展示/求职验收门槛；真实 DeepSeek/OpenAI/GPT Provider、公网 DNS/HTTPS、生产 MySQL、域名部署和监控仍需后续配置真实凭据与环境，不能在没有用户提供这些外部条件时伪造“已上线”。

### 2026-09-03 — Codex — BACKEND-T1-MODULE-BOUNDARIES

- 本轮只完成后端第一条模块边界纵向切片；保留工作区已有修改，没有执行 reset、checkout、clean、commit、push 或 deploy，没有新增生产依赖、数据库迁移、Provider、真实密钥或公网配置。
- 新增 `backend/src/main/java/com/enterpriseai/ticketcopilot/ticket/application/port/in/TicketWorkflowUseCase.java`，把当前 TicketController 使用的工单、Copilot、Trace、复核和知识草稿用例收敛到入站应用端口。
- 新增 `backend/src/main/java/com/enterpriseai/ticketcopilot/ticket/application/LegacyTicketWorkflowFacade.java` 作为过渡适配器；`TicketController` 现在依赖入站端口，不直接依赖旧 `TicketWorkflowService`。旧工作流实现、DTO、数据库字段和 `/api/...` 路径保持不变。
- 新增 `backend/src/test/java/com/enterpriseai/ticketcopilot/ticket/TicketModuleBoundaryTest.java`，验证 Controller 依赖方向和入站端口不泄漏 Entity、Mapper、旧 Service 类型。
- 新增 `docs/design/BACKEND_T1_MODULE_BOUNDARIES.md`，同步 `TODO.md`、`docs/TEST_REPORT.md` 与 `docs/architecture.md`。
- 验证：在 `backend/` 执行 `mvn test`，实际 `Tests run: 92, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。由于仓库没有 `mvnw.cmd`，本轮使用已安装 Maven CLI。
- 下一轮只进入 `BACKEND-T2-WORKFLOW-PORTS`，抽出一个最小策略/只读端口并继续用结构测试保护边界；完整包迁移、真实身份/RBAC、Provider、Outbox、生产 MySQL 和公网部署仍未完成。

### 2026-09-03 — Codex — BACKEND-T2-WORKFLOW-PORTS

- 本轮只完成工作流策略端口切片；保留工作区已有修改，没有执行 reset、checkout、clean、commit、push 或 deploy，没有新增生产依赖、数据库迁移、Provider、真实密钥或公网配置。
- 新增 `backend/src/main/java/com/enterpriseai/ticketcopilot/ticket/application/port/out/ReviewPolicy.java`。端口只使用结构化 Copilot 输出、Citation/结构化校验状态和业务复核标志，不暴露旧 Service、Entity 或 Mapper 类型。
- `TicketWorkflowService` 现在依赖 `ReviewPolicy`；`ReviewGate` 实现该端口。旧的 `finalHumanReviewRequired(... CitationValidationResult ...)` 保留为兼容包装，新工作流调用 `requiresHumanReview(...)`。
- 新增 `backend/src/test/java/com/enterpriseai/ticketcopilot/ticket/WorkflowPortBoundaryTest.java`，验证工作流不直接依赖 `ReviewGate`，端口签名不泄漏 legacy/persistence 类型，且 Spring 当前策略实现可赋值给端口。
- 新增 `docs/design/BACKEND_T2_WORKFLOW_PORTS.md`，同步 `TODO.md`、`docs/TEST_REPORT.md` 与 `docs/architecture.md`。企业工单 T0-T2 当前实现状态已记录为完成；完整模块迁移和生产化能力仍未完成。
- 验证：在 `backend/` 执行 `mvn test`，实际 `Tests run: 94, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 企业工单下一步不再继续混入本轮：按总路线转入电商后端独立 `E0`，或另立真实 Provider/认证/Outbox/部署任务；本轮没有声称这些能力已实现。

### 2026-09-03 — Codex — BACKEND-T0-API-CONTRACT-FREEZE

- 本轮只完成后端 T0 契约冻结，保留工作区已有修改；没有执行 reset、checkout、clean、commit、push 或 deploy，没有新增生产依赖、数据库迁移、Provider、真实密钥或公网配置。
- 新增 `backend/src/main/java/com/enterpriseai/ticketcopilot/contract/TicketStatusContract.java`，将现有状态字符串、完整状态词汇和通用人工状态接口的目标白名单集中为不可变合同；`TicketWorkflowService` 保留原有公开常量并改为引用合同，避免破坏现有调用方。
- 新增 `backend/src/test/java/com/enterpriseai/ticketcopilot/api/ApiRouteContractTest.java`，通过 Controller 注解反射固定 `/api/auth`、`/api/health`、`/api/tickets` 的旧路径和 HTTP 方法；新增 `backend/src/test/java/com/enterpriseai/ticketcopilot/contract/TicketStatusContractTest.java` 覆盖状态词汇、内部 intake 状态和未知值边界。
- 新增 `docs/design/BACKEND_T0_API_CONTRACT.md`，记录兼容 API、状态合同、Copilot/Trace/Review 不变量和 `/api/v1` 只作为后续迁移目标的边界；`docs/API.md` 与 `docs/architecture.md` 已链接该合同。
- 验证：在 `backend/` 执行 `mvn test`，实际 `Tests run: 90, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。新增 6 个测试（2 个 API 路径合同、4 个状态合同）与原有测试一起通过。
- 本轮没有实现完整状态转移矩阵、模块包迁移、真实身份/RBAC、真实 Provider、异步 job/Outbox 或 `/api/v1` 实际切换；下一轮只做 `BACKEND-T1-MODULE-BOUNDARIES`，继续以本轮合同测试作为回归门槛。

### 2026-09-03 — Codex — PHASE_9F legacy reference audit

- 本轮只完成 `PHASE_9F`，保留工作区已有未提交修改，没有执行 reset、checkout、clean、commit、push 或 deploy；没有修改 backend、数据库、Provider、评测数据或真实密钥。
- 新增 `docs/design/LEGACY_COMPONENT_REFERENCE_MAP.md`，通过源码引用结果区分当前壳层/UI 共享组件与 7 个当前 Showcase 路由未引用的 Legacy 业务组件。
- 当前页面已确认统一使用 `AppSidebar` / `AppTopbar` / `NavIcon` / `BrandMark` 与 `PageHeader` / `PanelHeader` / `StatusBadge` / `EmptyState` / `ErrorState` / `LoadingState`。Legacy 组件没有直接删除，因为旧 props、emit、数据 shape 与现有 `useTicketRealFlow()` 不兼容，且仍可能被历史入口或截图脚本依赖。
- CSS 债务沿用 9C 结果：静态 inline 样式改为语义 class，运行时百分比进度条保留；Evaluation 的局部 scoped CSS 使用全局 token 映射。
- 验证：引用清单通过 `rg` 复核；`frontend/npm run typecheck`、`frontend/npm run build` 通过。下一阶段是 `PHASE_9G` 前端发布验收；静态 a11y 候选仍不等同于完整 axe / Lighthouse 审计。

### 2026-09-03 — Codex — PHASE_9E language and progressive disclosure

- 本轮只完成 `PHASE_9E`，保留工作区已有未提交修改，没有执行 reset、checkout、clean、commit、push 或 deploy；没有修改 backend、数据库、Provider、Demo fixture、评测数据或真实密钥。
- Workbench、Human Review、Knowledge Base、Retrieval Evidence、Trace Timeline、Dashboard 的用户面文案和 aria label 改为中文优先；Trace、Provider、Citation、fallback 等需要保留的技术词放在中文解释之后或低频审计字段中。
- Human Review 的主要动作改为“批准 · Approve / 要求修改 · Request changes / 驳回 · Reject”，确认提示与必填原因提示同步更新，但提交给 flow 的 decision 枚举保持不变。
- Knowledge / Retrieval 文案明确区分知识命中、检索参考、已校验引用和 Citation membership validation；Workbench 的 runId / traceId / Provider 等低频字段继续通过 `details` 渐进展示。
- 验证：`frontend/npm run typecheck`、`frontend/npm run build` 通过。下一阶段是 `PHASE_9F` Legacy 组件与样式债务盘点；静态 a11y 候选仍不等同于完整 axe / Lighthouse 审计。

### 2026-09-03 — Codex — PHASE_9D mobile workflow

- 本轮只完成 `PHASE_9D`，保留工作区已有未提交修改，没有执行 reset、checkout、clean、commit、push 或 deploy；没有修改 backend、数据库、Provider、评测数据或真实密钥。
- `frontend/src/views/TicketWorkbenchShowcaseView.vue` 在 760px 以下增加队列、工单、建议与证据、复核四步当前页内导航；点击通过 `scrollIntoView` 定位，不会把应用级 hash 路由误切到 Dashboard，并尊重 reduced motion。
- `frontend/src/styles.css` 让 390px 的 Workbench 维持队列 → 工单 → Copilot → 复核单列；决策/复核按钮最小高度 44px，两个次级复核动作在窄屏纵向排列；768/1024/1440 既有两列/三列布局保留。
- 浏览器 Demo smoke 通过：390px 选择 `DEMO-0005` → 运行本地 Copilot → 查看建议、证据和风险 → Approve，最终状态为“已解决”；390、768、1024、1440 无横向溢出，破图数量为 0。
- 验证：`frontend/npm run typecheck`、`frontend/npm run build` 通过。下一阶段是 `PHASE_9E` 页面语言与渐进披露；静态 a11y 候选仍不等同于完整 axe / Lighthouse 审计。

### 2026-09-03 — Codex — PHASE_9C typography and token closure

- 本轮只完成 `PHASE_9C`，保留工作区已有未提交修改，没有执行 reset、checkout、clean、commit、push 或 deploy；没有修改 backend、数据库、Provider、评测数据或真实密钥。
- `frontend/src/styles.css` 新增共享字号、间距和 focus ring token；普通辅助说明、Copilot 解释、证据摘录和复核上下文不再统一压到 10px，技术 ID/评分等低频字段继续使用等宽小字号。
- `frontend/src/views/EvaluationMetricsShowcaseView.vue` 的评测语义色改为映射全局 `--app-*` token；`TicketWorkbenchShowcaseView.vue`、`TraceTimelineShowcaseView.vue`、`KnowledgeRagShowcaseView.vue`、`HumanReviewShowcaseView.vue` 的静态 inline 样式迁移到语义 class。动态进度条的百分比 `:style` 是运行时必要值，保留。
- 验证：`frontend/npm run typecheck` 通过；`frontend/npm run build` 通过（Vite 6.4.3，63 modules）；`git diff --check` 无 diff 错误。下一阶段是 `PHASE_9D` 移动端工作流；静态 a11y 候选仍不等同于完整 axe / Lighthouse 审计。

### 2026-09-03 — Codex — PHASE_9B Copilot decision center

- 本轮只完成 `PHASE_9B`，没有顺手进入字体 token、移动端专门工作流、legacy 清理或后端改造；保留工作区已有未提交修改，没有执行 reset、checkout、clean、commit、push 或 deploy。
- `frontend/src/views/TicketWorkbenchShowcaseView.vue`：右侧 Copilot 改为“决策建议 → 建议回复 → 关联证据 → 风险与复核门禁 → 人工复核 → 运行信息 / 审计字段”的任务顺序。新增决策主卡，首屏展示推荐处理、分类置信度、运行按钮和下一步提示；风险、Citation 数量和复核门禁独立成摘要卡。
- `frontend/src/styles.css`：新增决策主卡、风险摘要卡和移动端阅读样式；没有改变 API 路径、DTO、hash 路由、`data-e2e` 选择器、Demo / Real / Fallback 语义或审核状态流转。
- 验证：`frontend/npm run typecheck` 通过；`frontend/npm run build` 通过（Vite 6.4.3，63 modules）；`SCREENSHOT_URL=http://127.0.0.1:5190 npm run screenshots` 通过 8 个目标并生成标准、大屏、移动端截图。
- 浏览器验收：390px 无横向溢出且按队列 → 详情 → 决策中心纵向排列；768px、1024px 为两列内容加整行 Copilot；1440px 为三列。四个视口破图数量均为 0，页面 console error/warning 为 0。Demo `DEMO-0004` → 本地 Copilot → 确认 Approve 后，工单状态为“已解决”，review history 含 `APPROVED_RESOLUTION`，成功消息可见并重新读取 Trace。
- 静态可访问性审查：执行 `a11y_scanner.py frontend/src --format text`，得到 27 个文件、97 个候选（7 critical / 63 serious / 27 moderate）。结果包含组件级 landmark / label heuristic，不能等同于 axe / Lighthouse；本轮未把它写成完整合规结论，也未扩展为全站 a11y 重构。
- 本轮未修改 backend、数据库迁移、Provider、`docs/metrics/`、真实密钥、外部账号或部署配置；下一阶段仍是 `PHASE_9C`，后端改造门槛仍未达成。

### 2026-09-03 — Codex — PHASE_9A interaction and accessibility foundation

- 承接网络中断前的前端改造任务；先新增 `docs/design/FRONTEND_REDESIGN_ROADMAP.md`，把“前端全部改造”拆成 9A～9G 的可验收阶段。本轮只完成 9A，9B～9G 仍保持待开始，后端改造门槛尚未宣告达成。
- `frontend/src/App.vue`：命令中心现在会把焦点送入搜索框，支持 ↑ / ↓ 选择、Enter 导航、Escape 关闭并恢复触发控件焦点，Tab / Shift+Tab 在弹层内循环；补充 `combobox` / `listbox` / `option` / `aria-activedescendant` 语义，并复用 `NavIcon.vue` 渲染结果图标。
- `frontend/src/styles.css`：提高 `--app-muted`、`--app-amber`、`--app-red` 在白色面板上的可读性，增加当前项和 keyboard focus 的可见样式；`frontend/src/components/layout/AppSidebar.vue` 校正工作区 aria-label 的中文语义。
- 验证：`frontend/npm run build` 通过（Vite 6.4.3，63 modules）；`SCREENSHOT_URL=http://127.0.0.1:5190 npm run screenshots` 通过 8 个目标及标准 / 大屏 / 移动端截图、1366/390 横向溢出检查；一次性 Chrome smoke 通过命令中心焦点与键盘路径，以及 `DEMO-0002` Copilot → Approve → 已解决闭环，`consoleErrors=[]`、`pageErrors=[]`。
- 对比度仅完成静态核心 token 检查，没有把它写成完整 axe / Lighthouse 结果；前端没有独立持续测试 runner。本轮没有重跑真实 API 浏览器 smoke，Phase 6 历史证据继续保留；后端源码、数据库迁移、Provider、`docs/metrics/` 和 API 链路未改动，未新增依赖、密钥、外部资产、commit、push 或 deploy。

### 2026-09-03 — Codex — PHASE_8 reference-led premium brand polish

- 根据用户要求继续深看案例后完成第二轮视觉细修；保留已有工作区修改，没有执行 reset、checkout、清理、提交或推送。本轮新增 `NavIcon.vue`，重做自有 `BrandMark.vue`，并把颜色、字体和指标文案进一步产品化。
- 研究依据包括 Intercom Inbox 的团队 Inbox / 客户上下文 / Copilot / keyboard-first 入口，Zendesk Agent Workspace 的单工单 / 右侧 context panel / Knowledge panel，Jira Service Management 的优先级队列 / SLA / 知识库，ServiceNow CSM Workspace 的 case context / activity，以及 Linear Search 的快捷搜索和紧凑结果。另参考 Atlassian Design / Primer 对 token、typography、spacing、iconography、elevation、border 和 radius 的公开设计系统原则。
- 前端现在使用自绘票据 / 证据节点品牌 mark、统一笔画的 SVG 导航图标、`Aptos` / `Segoe UI Variable` 本地优先字体栈、温润中性色画布（`#f7f7f4`）、浅色侧栏（`#f0f1ee`）、克制靛蓝操作色和少量赤陶色品牌识别点。没有引入第三方 Logo、图片、字体文件、图标包或复制代码。
- Dashboard 指标和数据来源文案同步中文化；App Shell、Workbench、Dashboard、Knowledge、Retrieval、Trace、Human Review、Evaluation 继续共享同一套 token、状态 badge、边框和响应式规则。后端业务链路、API 路径、DTO、hash 路由、`data-e2e` 选择器和 Demo/Real/Fallback 语义未改变。
- `frontend/npm run build` 通过（Vite 6.4.3，`63 modules transformed`）；`SCREENSHOT_URL=http://127.0.0.1:5190 npm run screenshots` 通过并刷新标准、大屏、移动端截图；Demo browser smoke 通过队列加载 / P1 筛选 / Copilot / Approve / Ctrl+K 导航，console/page errors 为 0。
- 本轮是视觉与可见文案修订，没有重跑真实 API 浏览器服务；Phase 6 的真实 API smoke 保留为历史证据，不把 Demo、local-rule fallback 或截图写成真实模型成功接入。后端和 `docs/metrics/` diff 保持 clean；未公网部署、未新增依赖、未执行 Git commit/push。

### 2026-09-03 — Codex — PHASE_7_REFERENCE_LED_CALM_SAAS_VISUAL_REFINEMENT

- 根据用户对上一版“深色科技风”的明确反馈，完成参考案例导向的浅色中性客服工作台视觉纠偏；本轮保留已有工作区修改，没有执行 reset、checkout、清理、提交或推送。
- 视觉结构参考 Intercom Inbox 的队列 / 会话 / 客户上下文、Zendesk Agent Workspace 的单工单与右侧上下文、Jira Service Management 的优先级 / 队列 / 知识关联、ServiceNow 的活动与上下文工作区，以及 Linear 的紧凑检索与信息密度；只借鉴信息架构和交互语义，不复制品牌、Logo、图片、字体或产品页面。
- `frontend/src/styles.css` 改为浅灰画布、白色面板、克制蓝色主操作和低饱和状态色；移除渐变、霓虹、发光和装饰性科技背景。`BrandMark.vue`、`AppSidebar.vue`、`AppTopbar.vue`、`App.vue` 及七个 Showcase 页面同步调整为中文客服运营语义。
- Workbench 继续保持队列 → 工单上下文 → 处理建议 / 证据 / 人工复核的三栏结构；Dashboard 强调队列与健康状态；Knowledge、Retrieval、Trace、Human Review、Evaluation 页面共用同一浅色壳层。API 路径、DTO、hash 路由、`data-e2e` 选择器、Demo/Real/Fallback 边界和后端业务链路均未改变。
- 本轮涉及前端视觉、可见文案、截图和交付文档：`frontend/src/styles.css`、`frontend/src/App.vue`、`frontend/src/components/layout/BrandMark.vue`、`AppSidebar.vue`、`AppTopbar.vue`、`frontend/src/views/` 下相关 Showcase 页面、`README.md`、`TODO.md`、`docs/` 设计 / 架构 / 测试文档以及 `docs/images/` 标准、大屏、移动截图；未修改 backend 源码、数据库迁移、`docs/metrics/` 或另一个项目。
- 验证结果：`frontend/npm run build` 通过（`vue-tsc` + Vite 6.4.3，60 modules）；`SCREENSHOT_URL=http://127.0.0.1:5190 npm run screenshots` 通过 8 个目标、1440/1920/390 三套尺寸和横向溢出检查；Demo 浏览器 smoke 通过队列筛选、Copilot、Approve、Ctrl/Cmd+K 导航，console/page errors 均为 0；`backend/mvn test` 通过（84 tests，0 failures/errors/skipped）。
- 本轮是样式与文案修订，没有重新启动真实 API 服务；Phase 6 记录的真实 API smoke 作为历史证据保留，本轮不把 Demo 截图或本地规则 fallback 写成真实模型成功接入。未公网部署、未新增依赖、未添加真实密钥、未执行 Git commit/push。

### 2026-09-03 — Codex — PHASE_6 frontend full redesign and browser acceptance recovery

- 承接网络中断后的未完成任务；保留工作区已有修改，没有执行 reset、checkout、清理、提交或推送。当前范围完成前端整体重做、真实 API 复验、响应式截图和文档收口。
- 新增 `docs/design/FRONTEND_CURRENT_AUDIT.md` 与 `docs/design/FRONTEND_PAGE_SPECS.md`，以实际源码/API 为准记录七个主要页面、Demo/Real/Fallback 边界、状态设计、桌面/390px 布局和截图标准。
- 前端重做：`App.vue` + `AppSidebar` / `AppTopbar` / `BrandMark` + `PageHeader` / `PanelHeader` / `StatusBadge` / `MetricCard` / `LoadingState` / `EmptyState` / `ErrorState` 形成统一深色审计型工作台；Dashboard、Ticket Workbench、Knowledge Base、Retrieval Evidence、Trace Timeline、Human Review 完成布局与信息层级重排，Evaluation / Metrics 接入新壳层。
- 保留 API 路径、DTO 字段、hash 路由兼容别名、`data-e2e` 选择器和现有后端业务链路；旧 `frontend/src/components/` 未被删除，未使用的旧组件标记为后续 legacy cleanup 候选。
- Demo 浏览器 smoke（Vite 5182）通过：本地 Copilot → `DEMO_LOCAL` 证据 → Human Review Approve → `已解决` / `APPROVED_RESOLUTION`；同时验证队列筛选、Ctrl/Cmd+K 页面搜索、确认对话框，console errors 和 page errors 均为 0。
- Real API 浏览器 smoke（隔离 H2 后端 28081 + Vite 5184）通过：`/api/health=200`，UI 创建合成工单，Copilot `POST=200`，返回一个 `KB-OPS-003` retrieval snapshot、`IMMUTABLE_RUN`、`VALID` structured output / Citation membership；Approve `POST=200` 后工单为 `RESOLVED`，Trace review history 有 `APPROVED_RESOLUTION`。本机已有 Provider 环境使后端尝试 OpenAI-compatible 路径并收到 HTTP 403，随后按现有后端策略安全 fallback 到 `local-rule`；这不是成功真实模型接入，也没有新增或提交任何密钥。
- `frontend/npm run build` 通过（`vue-tsc` + Vite 6.4.3，60 modules）；`backend/mvn test` 通过（84 tests，0 failures/errors/skipped）；截图脚本通过 8 个目标，并生成 `docs/images/` 标准、`docs/images/large/` 1920×1200、`docs/images/mobile/` 390×844 三套截图，1366/390 横向溢出检查通过。
- `frontend/scripts/capture-screenshots.mjs` 增加移动端截图落盘和 Chromium 异常清理，避免截图异常后遗留浏览器进程。当前未配置前端独立测试 runner；未公网部署；未改另一个项目。
- 文档已同步：`README.md`、`TODO.md`、`docs/TEST_REPORT.md`、`docs/architecture.md`、`docs/frontend-real-flow-implementation.md` 与新增设计审查/规格文档。

---

### 2026-09-02 — Codex — PHASE_5 frontend real-flow audit continuation

- 本轮承接中断前的前端真实链路任务；没有重新开始，也没有回滚已有未提交改动。范围限定为 Vue 前端真实 API / Demo 边界、浏览器验收和记录同步，已冻结的后端核心、数据库迁移、Provider、RAG 算法与 Docker Compose 未扩展。
- `frontend/src/api/tickets.ts`：真实模式继续通过 `/api` 调用后端并在内存保存登录会话；网络/登录异常统一转为安全错误；Trace、Copilot run、Approve / Request changes / Reject 在 `MODE=demo` 下显式走本地适配器，真实模式没有静默 Mock fallback。
- `frontend/src/data/demoTickets.ts`：补齐本地 Trace、结构化输出、检索引用、Citation membership fixture、运行记录和 append-only review history；所有本地证据使用 `DEMO_LOCAL` / `DEMO-*` 标识，不伪装成 `IMMUTABLE_RUN`。
- `frontend/src/composables/useTicketRealFlow.ts`：Demo 与真实模式统一读取 Trace，保持列表、详情、分析、运行、复核后的重新读取行为一致。
- 页面文案同步明确 `Local Demo Fixture` 与 `Backend API` 的数据源差异，避免截图中的本地数据被误读为后端持久化结果。
- Demo 截图验证：在 5180 启动本项目 `npm run dev:demo`，执行 `SCREENSHOT_URL=http://127.0.0.1:5180 npm run screenshots`；结果通过，8 个 Showcase 目标完成标准/1920x1200 截图，并通过 1366 桌面与 390 移动端横向溢出检查。
- Demo 浏览器 smoke：`DEMO-0005` → 本地 Demo Copilot → `DEMO_LOCAL` → Approve；详情状态变为“已解决”，生成 `APPROVED_RESOLUTION` review history。
- 真实浏览器 smoke：隔离 H2 test profile 后端运行在 28080，前端 Vite 运行在 5181 并代理到该后端；创建合成工单 → 后端 Copilot → `NO_RETRIEVAL_EVIDENCE` 安全拒答 → Approve，详情状态变为“已解决”，review history 可见。该 H2 实例为 schema-only，因此没有把空库伪写成 Citation-positive 证据。
- 当前前端没有独立测试 runner；本轮以 `npm run build`、截图脚本和上述两条可重复浏览器 smoke 作为验收证据。真实知识命中、Citation 校验和 IMMUTABLE_RUN 持久化仍由现有后端 JUnit / H2 集成测试覆盖。
- 文档已同步：`README.md`、`docs/architecture.md`、`docs/frontend-real-flow-implementation.md`、`docs/TEST_REPORT.md`、`TODO.md`。

---

### 2026-07-30 — Codex — Real Provider Evidence Closeout

- Shared Provider mapping 已完成：`TICKET_AI_*` 优先，`PORTFOLIO_AI_*` 作为 fallback，Ticket Copilot 当前运行 Adapter 为 Chat Completions。
- Synthetic real-provider E2E smoke 已完成：使用 `smoke` profile 和隔离 H2 in-memory 数据库，仅发送 synthetic 数据；Remote Provider Used=YES，Local Fallback Used=NO，generation_record 已持久化。
- Guaranteed-match RAG smoke 已完成：synthetic knowledge `KB-SYN-REDIS-CACHE-001` 与 synthetic ticket `TCK-260730130531-931` 命中，Retrieved Article Count=1，Used In Draft=YES。
- Evidence 文件：`docs/evidence/real-provider-synthetic-smoke-20260730.md`、`docs/evidence/real-provider-synthetic-smoke-20260730.json`。
- 当前分支：`feat/shared-provider-config-mapping`；本记录创建前 HEAD：`d51155989ff7a50ba71f65c428e9fb2adbd818cf`。
- 当前不能夸大：Provider citation validation 尚未实现；Trace evidence 尚不是不可变 run snapshot；Frontend showcase pages 尚未接入该真实后端链路；本验证只代表 controlled synthetic smoke，不代表生产稳定性。
- 下一阶段建议：`PHASE_3_REAL_RUN_TRACE_FOUNDATION`。

### 2026-07-04 — Codex — Sidebar Brand Mark Visual Polish

- 本轮任务：只修复公共 App Shell 左上角 Enterprise Ticket RAG Copilot 品牌图标，并刷新 README 当前引用的本项目真实截图；未改 README 文案、后端代码、RAG metrics JSON、评测脚本或业务页面内容。
- 前端改动：`frontend/src/App.vue` 将 Sidebar brand mark 调整为 46px 稳定容器，使用原创 inline SVG ticket / ET / trace dot 图形；增强深蓝黑底、青蓝描边和克制蓝紫渐变，品牌文字 selector 继续限定在文本区域，避免污染图标或导航文字。
- 截图刷新：通过本项目本地 Vite 页面刷新 `docs/images/dashboard.png`、`ticket-workbench.png`、`evaluation-metrics.png`、`trace-timeline.png`、`knowledge-base.png`、`human-review.png`、`trace-evidence.png` 及 `docs/images/large/` 对应大图；截图脚本同时刷新兼容别名 `ticket-detail` 标准与 large 图。
- 截图端口：`npm run screenshots` 默认 5173 端口被占用后，使用空闲端口 `41772` 与 `SCREENSHOT_URL=http://127.0.0.1:41772 npm run screenshots` 生成真实本地页面截图；临时 Vite 进程已停止。
- 目视检查：已人工检查 Dashboard、Ticket Workbench、Evaluation / Metrics、Trace Timeline、Knowledge Base、Human Review、Trace Evidence 及 large Dashboard，左上角图标清晰、未裁切、非空白块，品牌文字未错位，Sidebar 未见横向溢出。
- 验证：`npm run typecheck` 通过；`npm run build` 通过；`SCREENSHOT_URL=http://127.0.0.1:41772 npm run screenshots` 通过；本轮未运行后端 `mvn test` 或 RAG evaluate 脚本，因为未修改后端或 metrics。
- 边界：未使用第三方 Logo、OpenAI/GitHub/Linear/Langfuse/Vercel 等商标、第三方截图或 Image2 图；未提交 `.local/`，未接入真实 API Key，未 push。

### 2026-07-04 — Codex — README Post-publish Boundary Fix

- 本轮任务：只做 README 发布后小修复；未修改前端代码、后端代码、截图、metrics JSON、评测脚本或临时本地文件，未 push。
- README 外链：移除顶部 `Portfolio Case Study` 外部链接，避免 GitHub README 跳转到可能尚未同步最新截图的作品集页面；本项目截图仍全部使用仓库内 `docs/images/` 相对路径。
- Provider fallback：Evaluation / Metrics 不再把 provider fallback 作为百分比质量指标展示，改为独立说明 `Local fallback path: enabled`、`No API key mode: expected local-rule fallback`、`Provider path: local-rule fallback`、`Real provider: not configured`。
- Resume / Boundary：Resume Bullets 补充 local-rule fallback 作为无 API Key 环境下的安全演示路径；Honest Boundaries 保留 synthetic demo dataset、keyword retrieval、citation gating、OpenAI-compatible Provider optional path、no real API key committed、not production data / traffic / model quality result。
- 验证：本轮不运行 build、test、screenshots 或 evaluate；仅执行 git status、文档 diff、README 外链与风险词检查、git diff --check。

### 2026-07-04 — Codex — GitHub README Portfolio Integration

- 本轮任务：只整合 GitHub README 作品集展示；未修改前端代码、后端代码、截图文件、metrics 产物或 `.local/`。
- README 改动：首屏重新定位 `Enterprise Ticket RAG Copilot`，明确这是本地 demo / showcase 项目；默认链路为 `local-rule fallback`、`keyword retrieval`、`citation gating` 和 `synthetic demo dataset`，OpenAI-compatible Provider 仅作为可选配置路径。
- 截图展示：README 按 Dashboard、Ticket Workbench、Evaluation / Metrics、Trace Timeline、Knowledge Base、Human Review 顺序引用本项目真实截图；只引用 `docs/images/*.png` 和对应 `docs/images/large/*.png`，未引用 `.local/`、第三方截图、Image2 图或外部参考图。
- 新增/强化章节：项目定位、为什么不是普通 RAG demo、Core Features、Tech Stack、Architecture / Workflow Mermaid、Evaluation / Metrics、Baseline / Scope、Local Run、Resume Bullets、Interview Talking Points、Honest Boundaries 和延伸材料。
- Metrics 口径：README 使用 `docs/metrics/rag_metrics_latest.json` 最新值，Avg Retrieval Latency 为 `0.0437 ms`；指标继续限定为 synthetic demo dataset + local keyword retrieval + citation gating，不声明真实向量检索、真实模型质量、线上效果或公司内部数据效果。
- 文档记录：`TODO.md` 已记录本轮 README 作品集整合结果和下一步建议。
- 验证：本轮按要求未运行 `npm run build`、`npm run screenshots`、`py .\scripts\evaluate_rag_demo.py` 或 `mvn test`；只执行 Git / diff / README 图片路径 / README 风险词检查。
- 未做事项：未 push，未接真实 API Key，未提交 `.local/`，未使用第三方截图，未使用 Image2 图，未新增前端/后端功能。

### 2026-07-04 — Codex — Phase 3 Trace Timeline Showcase

- 本轮任务：只产品化 `Trace Timeline` 页面；未重写 Dashboard、Evaluation / Metrics、Ticket Workbench、Knowledge Base、Human Review，未修改 README 或后端业务逻辑。
- 前端改动：新增 `frontend/src/views/TraceTimelineShowcaseView.vue`，将 `trace-timeline` 路由切到专用页面；保留旧 `TraceShowcaseView.vue` 给 Retrieval Evidence 兼容路径使用。
- 页面能力：首屏展示 Run Overview、Run List / Trace Summary、Step Timeline、Step Detail、Retrieval / Citation Evidence、Provider Call skipped / local-rule fallback、Human Review gate 和 Raw JSON / Debug Detail。
- 数据边界：新增 trace demo constants 已标注为 showcase demo trace data；仅用于作品集前端展示，不代表真实生产日志、真实用户流量、真实 LLM 调用、真实向量 RAG 或完整 Agent Runtime。
- 截图脚本：`frontend/scripts/capture-screenshots.mjs` 新增 `trace-timeline` 目标；旧 `trace-evidence` 目标沿用并指向 Trace Timeline 页面，生成 `docs/images/trace-timeline.png`、`docs/images/large/trace-timeline.png`，同时刷新 Trace 兼容截图 `trace-evidence` 标准与 large 版本。全量截图脚本生成的非 Trace 页面截图已恢复，未纳入本轮改动。
- 截图端口：默认 5173 端口被占用，本轮使用 `http://127.0.0.1:41760` 启动本项目 Vite demo，并通过 `SCREENSHOT_URL=http://127.0.0.1:41760 npm run screenshots` 生成真实本地页面截图；临时进程已停止。
- 验证：`npm run typecheck` 通过；`npm run build` 通过；`SCREENSHOT_URL=http://127.0.0.1:41760 npm run screenshots` 通过；`py .\scripts\evaluate_rag_demo.py` 通过（16 cases，Top-K 100.00%，Context Recall@K 90.00%，Citation Coverage 100.00%，Citation Precision 81.11%，Failed 6，Review 15）；`mvn test` 通过（24 tests，0 failures / errors / skipped）。
- 未做事项：未改 README，未接真实 API Key，未使用 Image2，未提交 `.local/`，未提交第三方截图，未声明真实生产链路或真实模型稳定接入。

### 2026-07-04 — Codex — Phase 2 Ticket Workbench Showcase

- 本轮任务：只深化 Ticket Workbench 页面在当前深色 App Shell 下的企业级首屏表现；未重写 Dashboard、Evaluation / Metrics、Knowledge、Trace Timeline 或 Human Review 独立页面，未修改后端逻辑或指标口径。
- 前端改动：`frontend/src/views/TicketWorkbenchShowcaseView.vue` 重做为三栏工作台：左侧 Ticket Queue，中间 Ticket Detail + AI 分析结果 + AI 回复草稿，右侧 Citation Evidence + Human Review + Trace + Retrieval / Evaluation 摘要。
- 数据边界：页面使用本地 synthetic demo 工单常量，展示 local-rule fallback、keyword retrieval、citation gating、manual review gate；没有接真实 API Key，没有声明真实线上 AI 自动处理、真实企业数据或生产流量。
- 截图脚本：`frontend/scripts/capture-screenshots.mjs` 新增 `ticket-workbench` 目标，同时保留既有 `ticket-detail` 目标；本轮刷新 `docs/images/ticket-workbench.png`、`docs/images/large/ticket-workbench.png`，并同步刷新既有 Workbench 截图 `ticket-detail` 标准与 large 版本。脚本全量生成的非 Workbench 页面截图已恢复，未纳入本轮改动。
- 截图端口：默认 5173 端口被其他本地进程占用，本轮复用本项目本地 Vite 端口 41749，通过 `SCREENSHOT_URL=http://127.0.0.1:41749 npm run screenshots` 生成真实本地页面截图。
- 验证：`npm run typecheck` 通过；`npm run build` 通过；`SCREENSHOT_URL=http://127.0.0.1:41749 npm run screenshots` 通过；`py .\scripts\evaluate_rag_demo.py` 通过（16 cases，Top-K 100.00%，Context Recall@K 90.00%，Citation Coverage 100.00%，Citation Precision 81.11%，Failed 6，Review 15）；`mvn test` 通过（24 tests，0 failures / errors / skipped）。
- 未做事项：未改 README，未改后端，未改 `docs/frontend_reference*`，未提交 `.local/`，未使用第三方截图，未使用 Image2 图进入项目，未接真实 API Key。

### 2026-07-04 — Codex — Restore Sidebar Brand Mark

- 本轮任务：仅修复 App Shell / Sidebar 左上角 Enterprise Ticket RAG Copilot 品牌图标，避免 Evaluation / Metrics 截图中品牌区弱化；未重构布局，未修改 Evaluation 页面指标逻辑。
- 前端改动：`frontend/src/App.vue` 将原 42px 纯文本 `ET` 标识替换为 44px 深色圆角容器 + 原创 inline SVG ticket / ET 标识；同步收窄品牌文字 CSS selector，避免 `.portfolio-shell__brand span` 覆盖品牌图标样式。
- 视觉边界：延续深蓝黑背景、低饱和面板、青蓝高亮与蓝紫轻微渐变；没有使用第三方 Logo，没有使用 Image2 图，没有接入真实 API Key。
- 截图：`docs/images/evaluation-metrics.png`、`docs/images/large/evaluation-metrics.png` 均由本项目本地页面重新生成；截图脚本默认 5173 端口被占用后，改用空闲端口 41739 与 `SCREENSHOT_URL=http://127.0.0.1:41739` 通过。脚本全量生成的其他页面截图已恢复，未纳入本轮改动。
- 验证：`npm run typecheck` 通过；`npm run build` 通过；`npm run screenshots` 默认端口 5173 被占用失败，随后使用 `SCREENSHOT_URL=http://127.0.0.1:41739 npm run screenshots` 通过，并人工目视复核 Evaluation 截图左上角品牌图标清晰可见。
- 未做事项：未改 README，未改后端，未改 `docs/frontend_reference*`，未改 `frontend/src/data/evaluationMetrics.ts`，未改 RAG metrics 口径，未提交 `.local/` 或 Image2 图。

### 2026-07-04 — Codex — Evaluation / Metrics 视觉精修

- 本轮任务：仅精修 App Shell 与 Evaluation / Metrics 页面，按本地 ignored 参考图 `.local/design_targets/evaluation_metrics_target_v1.png` 落地真实 Vue 页面；未改 Ticket Workbench、Knowledge、Trace、Human Review 业务页面。
- 前置边界：已确认 `.local/` 由 `.gitignore` 忽略；参考图只用于视觉对照，未复制到 `docs/images/`、未放入 README、未加入 Git。参考图与两张真实截图的 SHA-256 均不同。
- App Shell：导航副标题改为中文业务名；顶部状态栏收敛为 Showcase Demo、Provider、Retrieval、Eval Dataset 四项；移除顶部与侧栏重复的强 Demo Boundary，Evaluation 路由使用专属右侧上下文面板。
- Evaluation：主标题改为“评测指标中心”，副标题与说明中文产品化；保留 8 个核心 KPI，增加失败案例 / Review 轻量入口；MRR、NDCG@K、本地路径状态、知识缺失回退降权为紧凑补充指标。
- Baseline / Plan：Baseline 表格改为中文主名 + 英文低对比 key，仅展示 3 个产品化策略；下一阶段实验计划明确标记“规划中”，并说明当前不包含 BM25、embedding、Vector DB、Hybrid、Rerank 或真实模型质量评测。
- 右侧上下文：新增“当前评测结论”，并将 Provider 与检索范围改为键值对；保留评测快照、最近 Trace / Review、Demo 边界说明，明确 synthetic cases、local keyword retrieval、citation gating、local-rule fallback 边界。
- 响应式：浏览器复核 1920、1440、390 视口，无横向溢出；移动端导航改为双列，页面标题可在首屏范围内出现。
- 真实截图：`docs/images/evaluation-metrics.png`、`docs/images/large/evaluation-metrics.png` 均由本项目本地页面生成；截图脚本因 5173 / 5174 已被其他本地项目占用，改用空闲端口 41739 与 `SCREENSHOT_URL=http://127.0.0.1:41739` 后通过。
- 验证：`npm run typecheck` 通过；`npm run build` 通过；`npm run screenshots` 通过；`py .\scripts\evaluate_rag_demo.py` 通过（16 cases，Top-K 100.00%，Context Recall@K 90.00%，Citation Coverage 100.00%，Citation Precision 81.11%，Failed 6，Review 15）；`mvn test` 通过（24 tests，0 failures / errors / skipped）。
- 安全边界：未使用第三方截图，未提交第三方截图，未将 Image2 参考图写入 `docs/images/` 或 README，未接入真实 API Key，未声明真实向量 RAG、真实模型准确率、生产数据、真实用户流量或生产可用。

---

### 2026-07-04 — Codex — Frontend Phase 1 Showcase Shell + Evaluation Dashboard

- 本轮任务：严格按 `docs/frontend_reference_research.md`、`docs/frontend_moodboard.md`、`docs/frontend_showcase_design.md` 落地 Phase 1 前端升级，范围限定为 App Shell、Dashboard、Evaluation / Metrics，并保持其他页面可访问。
- App Shell：`frontend/src/App.vue` 改为统一深色企业 SaaS showcase shell，保留 hash route，新增 `Evaluation / Metrics` 路由；顶部状态栏展示 Project Mode、Provider、Retrieval、Eval Dataset 和 Demo Boundary；右侧上下文面板仅在 Dashboard / Evaluation 显示，避免压窄旧页面。
- Dashboard：重写 `frontend/src/views/DashboardShowcaseView.vue`，首屏展示 Enterprise Ticket RAG Copilot、demo 边界、KPI、Evaluation Snapshot、Recent Ticket Runs、Top Knowledge Sources、Provider/Fallback、Human Review Queue 和 Trace/Audit Activity。
- Evaluation：新增 `frontend/src/views/EvaluationMetricsShowcaseView.vue`，展示本地评测指标、baseline/scope、next-stage、样本表、失败类型分布、复现命令和能力边界。
- 数据来源：新增 `frontend/src/data/evaluationMetrics.ts`，同步自 `docs/metrics/rag_metrics_latest.json`、`docs/metrics/rag_metrics_snapshot.md` 和 `data/eval/ticket_rag_eval_cases.jsonl`；文件注释已说明 synthetic demo dataset + local keyword retrieval + citation gating 边界。
- 截图：`frontend/scripts/capture-screenshots.mjs` 新增 `evaluation-metrics` 目标；已生成本项目本地运行截图到 `docs/images/` 和 `docs/images/large/`，没有使用或提交 `.local/reference_screenshots/` 下第三方截图。
- 验证命令：
  - `frontend/` 下执行 `npm ci`，结果：0 vulnerabilities。
  - `frontend/` 下执行 `npm run typecheck`，结果：通过。
  - `frontend/` 下执行 `npm run build`，结果：通过，Vite build 完成。
  - `frontend/` 下执行 `npm run screenshots`，首次因 5173/5174 被其他本地服务占用失败；改用空闲端口 5291 + `SCREENSHOT_URL` 后通过，并完成 1366 / 390 无横向溢出检查。
  - 根目录执行 `py .\scripts\evaluate_rag_demo.py`，结果：16 cases，Top-K=3，Top-K Hit Rate 100.00%，Context Recall@K 90.00%，Citation Coverage 100.00%，Citation Precision 81.11%，Failed Case Count 6，Human Review Required Count 15。
  - `backend/` 下执行 `mvn test`，结果：`Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 边界说明：未修改后端业务逻辑，未接真实 API Key，未写入密钥，未声明真实向量 RAG、真实模型准确率、真实线上用户或生产可用；前端所有评测数字均标注为本地 demo / local evaluation 来源。

---

### 2026-07-04 — Codex — RAG Evaluation Metrics Baseline

- 本轮任务：补一个可复现、可解释、适合 README 和简历引用的最小 RAG / Citation / Trace Evaluation 体系。
- 新增评测集：`data/eval/ticket_rag_eval_cases.jsonl`，包含 16 条 synthetic enterprise ticket demo cases，覆盖 SSO/MFA、RBAC、数据同步、慢查询、接口 500、部署配置、SLA、高风险回滚、缺知识 fallback 和易误判相似问题。
- 新增脚本：`scripts/evaluate_rag_demo.py`，仅使用 Python 标准库，模拟当前 demo keyword retrieval 评分口径，不连接 MySQL，不调用真实 Provider，不读取 API Key。
- 新增文档：`docs/evaluation/RAG_EVALUATION_PLAN.md`，说明评测目标、样本字段、指标定义、baseline、运行命令、当前边界和下一阶段真实模型评测计划。
- 新增指标产物：`docs/metrics/rag_metrics_latest.json`、`docs/metrics/rag_metrics_snapshot.md`。
- README 更新：新增 `Evaluation / Metrics` 章节，包含 Demo Dataset、指标定义、运行命令、当前结果快照、baseline 边界、local-rule / mock 边界、简历可写表达和不能夸大的点。
- 架构文档更新：`docs/architecture.md` 增加 Evaluation artifacts 说明，明确评测不属于线上运行链路。
- TODO 更新：记录本地 Evaluation 闭环和当前 24 个后端测试用例的真实现状。
- 验证命令：
  - `backend/` 下执行 `mvn test`，结果：`Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
  - 项目根目录执行 `py .\scripts\evaluate_rag_demo.py`，结果：16 cases，Top-K=3，Top-K Hit Rate 100.00%，Context Recall@K 90.00%，Citation Coverage 100.00%，Citation Precision 81.11%，Failed Case Count 6，Human Review Required Count 15。
- 边界说明：这些指标只来自 synthetic demo dataset + local keyword retrieval + citation gating；不声明真实向量 RAG、真实模型准确率、Prompt 提升、生产可用或服务真实用户。
- 未做事项：未修改前端 UI，未生成新截图，未接真实 API Key，未新增生产依赖，未实现 BM25 / Vector / Hybrid / Rerank。

---

### 2026-06-27 — Codex — Ticket Workbench ShowcaseView 单页改造

- 本轮任务：按 Enterprise SaaS UI Design Skill 与 playbook 只改 Ticket Workbench 页面，不改 backend、不改数据库、不改其他页面源码。
- 前端改动：新增 `frontend/src/views/TicketWorkbenchShowcaseView.vue`，内部只使用本地 demo 常量，CSS 使用 `showcase-` scoped 前缀；没有复用旧 `TicketQueue`、`TicketDetailPanel`、`AiRecommendationPanel`。
- App 入口：`frontend/src/App.vue` 中 Tickets 默认区域已切到 `TicketWorkbenchShowcaseView`。
- 截图脚本：`frontend/scripts/capture-screenshots.mjs` 的 Ticket Workbench 等待选择器最小适配为 ShowcaseView 的 `.showcase-metadata-grid` 与 `.showcase-signal-grid`。
- 验收记录：`frontend/` 下 `npm run build` 通过；`frontend/` 下 `npm run screenshots` 通过；`docs/images/ticket-detail.png` 已人工验收通过，首屏可见左队列 / 中详情 / 右 Copilot。

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

### 2026-06-26 — Codex — Enterprise RAG UI visual references

* 分支：feat/enterprise-ticket-rag-copilot
* 任务：归档 5 张 AI-generated UI visual references
* 新增目录：docs/design/references/
* 新增图片：

  * 01-dashboard-ai-concept-cn.png
  * 02-ticket-workbench-ai-concept-cn.png
  * 03-ticket-trace-ai-concept-cn.png
  * 04-knowledge-base-ai-concept-cn.png
  * 05-human-review-ai-concept-cn.png
* 新增说明：docs/design/references/README.md
* 未修改 backend
* 未修改 frontend 真实代码
* 未修改 README 项目真实截图区
* 这些图片只作为前端设计参考，不是真实运行截图
* 下一步建议：先选择 02-ticket-workbench-ai-concept-cn.png 作为第一版真实前端重构参考，再按现有真实接口改造前端工作台

### 2026-06-22 — Codex — 重新制作前端作品集展示效果

- 任务：重新制作前端作品集展示效果，并重做 README 截图展示区。
- 修改文件：`frontend/src/App.vue`、`frontend/src/styles.css`、`frontend/scripts/capture-screenshots.mjs`、`README.md`、`docs/frontend-style.md`、`docs/acceptance-checklist.md`、`TODO.md`、`HANDOFF.md`、`docs/images/*.png`、`docs/images/large/*.png`。
- 视觉改动摘要：首屏改为企业 SaaS 控制台风格，左上角展示项目名，右侧展示 CI workflow、OpenAPI documented、Frontend build、No real LLM 等可信标签；指标区调整为今日工单、待处理、规则已分析、知识关联率、待人工确认；主体改为三栏工作台，左侧工单队列、中间工单详情、右侧规则引擎辅助分析与模板化建议草稿。
- README 截图区改动：保留 `docs/images/dashboard.png` 作为精选主图，辅助截图改为 HTML table 两列布局，展示工单详情、规则引擎辅助分析、知识库评分匹配和大尺寸控制台截图。
- 截图脚本改动：普通主图使用 1440x960；辅助截图使用 1100x960 聚焦视口；`docs/images/large/` 继续生成 1920x1200 大图。
- 构建命令：在 `frontend/` 执行 `npm run build`。
- 截图命令：在 `frontend/` 执行 `npm run screenshots`，覆盖 `docs/images/` 和 `docs/images/large/` 下 README 使用截图。
- 结果：前端构建通过，截图脚本通过；截图脚本完成 Demo 交互检查和 1366/390 宽度无横向溢出断言。
- 旧文案复核：前端 UI 和新截图未出现“企业 AI 工单作战台 / AI 已分析 / AI 建议 / AI 生成 / 智能分类 / Agent 自动处理”等旧误导文案；保留项目名、兼容接口路径和“不接真实 LLM/大模型”的边界说明。
- 是否修改后端业务代码：否。未修改 Java 后端、数据库 schema、Swagger、全局异常处理、knowledgeCoverage、Docker 或 CI workflow。
- 剩余风险：本轮未运行后端 `mvn test`；README 中仍有项目名 `Enterprise AI Ticket Copilot` 和 `/ai-analysis` 兼容路径说明，但文档明确其不是真实 LLM 能力。

### 2026-06-22 — Codex — 刷新前端展示截图

- 任务：刷新 README 使用的前端展示截图，确保截图文案与当前规则引擎辅助分类、知识库评分匹配、模板化建议草稿口径一致。
- 修改文件：`docs/images/dashboard.png`、`docs/images/ticket-detail.png`、`docs/images/ai-analysis.png`、`docs/images/knowledge-base.png`、`docs/images/large/dashboard.png`、`docs/images/large/ticket-detail.png`、`docs/images/large/ai-analysis.png`、`docs/images/large/knowledge-base.png`、`HANDOFF.md`。
- 源码复核：`frontend/src` 中未发现“企业 AI 工单作战台 / AI 已分析 / AI 建议 / 知识覆盖率”等旧 UI 文案；仅保留“不调用真实 LLM”的边界说明。
- 构建命令：在 `frontend/` 执行 `npm run build`。
- 截图命令：在 `frontend/` 执行 `npm run screenshots`，覆盖 `docs/images/` 和 `docs/images/large/` 下 README 使用截图。
- 结果：构建通过，截图脚本通过；目视复核主截图与规则分析截图均为新口径。
- 剩余风险：未运行后端测试；本轮未修改后端代码和前端逻辑，仅刷新截图与交接记录。

### 2026-06-22 — Codex — 最终 CI 收口

- 任务：push `resume-optimization-v1` 到 GitHub，确认远端 GitHub Actions CI 两个 job 跑绿，并补充 CI 证据。
- 本地最终验证：`backend/` 下 `mvn test` 通过，`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`；`frontend/` 下 `npm run build` 通过，`vue-tsc` 类型检查和 Vite 生产构建完成。
- push 结果：已执行 `git push -u origin resume-optimization-v1`，远端分支创建成功。
- 远端 CI 结果：GitHub Actions run `27929741126` 已完成，`conclusion = success`。
- 通过 job：`Backend tests`、`Frontend build`。
- 修改文件：`README.md`、`docs/TEST_REPORT.md`、`TODO.md`、`HANDOFF.md`。
- 是否新增截图：否；本轮通过 `gh run watch 27929741126 --exit-status` 和 `gh run view` 确认远端 CI 成功。
- 剩余风险：GitHub Actions 输出包含 Node.js 20 deprecation annotation，不影响本次通过结果；后续如需消除提示，可单独评估升级 workflow 的 Node 版本。
- 下一步建议：可以创建 PR 并合并回 `main`；合并前可在 PR 页面再次确认 CI 状态。

### 2026-06-22 — Codex — P2-2 GitHub Actions CI

- 任务：P2-2 GitHub Actions CI。
- 修改文件：`.github/workflows/ci.yml`、`README.md`、`docs/TEST_REPORT.md`、`TODO.md`、`HANDOFF.md`。
- CI 触发条件：`push`、`pull_request`。
- CI job 内容：`backend-tests` 使用 Java 17 + Maven cache，在 `backend/` 执行 `mvn test`；`frontend-build` 使用 Node.js 20 + npm cache，在 `frontend/` 执行 `npm ci` 和 `npm run build`。
- 本地验证命令：`mvn test`（目录：`backend/`）；`npm run build`（目录：`frontend/`）；`git diff --stat`、`git diff --check`、`git status`。
- 本地验证结果：后端 `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`；前端 `vue-tsc` 类型检查通过，Vite 生产构建完成。
- 是否修改业务代码：否，未修改 Java / Vue 业务代码、数据库 schema、Swagger 配置、全局异常处理器或 `knowledgeCoverage` 逻辑。
- 剩余风险：本地只能验证 workflow 文件和项目命令；尚未 push 到 GitHub，不能确认远端 GitHub Actions 已通过。
- 下一轮建议：处理 P2-3，增加 Docker Compose，提供 MySQL + 后端的本地演示环境。

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

### 2026-07-30 — Codex — PHASE_3_REAL_RUN_TRACE_FOUNDATION

- Task: persist immutable Copilot run trace evidence for backend runtime audits.
- Branch: `feat/immutable-copilot-run-trace` from `main` at merge commit `a861c9ef3453c7834a4cf582af13e4ed66e00bba`.
- Schema artifacts: added `copilot_run`, `retrieval_hit`, `review_record` to `backend/src/main/resources/schema.sql`, mirrored H2 test schema in `backend/src/test/resources/schema-h2.sql`, and added SQL migration artifact `backend/src/main/resources/db/migration/V1__immutable_copilot_run_trace.sql`. The repository still uses SQL init/H2 schema for current automated tests; no new Flyway runtime dependency was introduced.
- Backend behavior: `runCopilot` now creates one immutable `copilot_run` per execution, stores retrieval snapshots in `retrieval_hit`, records human decisions in `review_record`, separates requested Provider/protocol/model from actual Provider/protocol, and stores sanitized Provider error categories.
- Trace behavior: `/api/tickets/{id}/trace-evidence` returns `evidenceSource=IMMUTABLE_RUN` when a persisted run exists and replays RAG evidence from immutable snapshots; tickets without runs keep `LEGACY_DERIVED` compatibility.
- Tests added/updated: immutable run persistence, distinct run/trace ids across repeated runs, immutable retrieval snapshot replay after knowledge title mutation, review record run linkage, Provider actual protocol/provider fields and sanitized error categories.
- Validation completed: `backend/ mvn test` passed with 41 tests, 0 failures, 0 errors, 0 skipped.
- Not done by design: no frontend changes, no screenshots, no Citation Validation, no RAG algorithm or eval dataset changes, no real Provider request, no push/PR/merge.- Additional validation completed after documentation update: `frontend/ npm run build` passed; `python scripts/evaluate_rag_demo.py` passed with 16 synthetic demo cases. These validation commands did not execute a real Provider request.

### 2026-07-30 ? Codex ? PHASE_4_STRUCTURED_OUTPUT_CITATION_AND_ABSTENTION

- Branch: `feat/structured-output-citation-abstention` from `main` at `5c115ee8e448c4706cabc71bc37e92881cb36fa6`.
- Implemented structured output contract with finite `riskLevel`, `abstentionReasonCode`, output validation status, missing-information bounds, and final human-review gating.
- Provider prompt now uses current-run immutable retrieval snapshots only (`knowledgeArticleId`, title/category snapshot, bounded excerpt, score) and asks for strict JSON; full prompt and raw Provider response are not persisted.
- Added Citation validation against current `retrieval_hit` snapshots and persisted accepted citations in `copilot_result_citation`; validation is ID-set validation, not sentence-level entailment.
- Added safe abstention for no evidence, invalid structured output, missing Citation, and invalid Citation; no-evidence runs skip remote Provider calls.
- Added V2 migration, main schema, H2 schema, entities, mappers, parser, validator, local-rule structured output, review gate, API/Trace structured fields, docs, tests, and a small offline structured-decision evaluation artifact.
- No frontend source changes, no screenshots, no vector database, no Responses adapter, no push/PR/merge, and no real Provider request.

### 2026-07-30 - Codex - PHASE_4C_SYNTHETIC_REAL_STRUCTURED_OUTPUT_SMOKE_EVIDENCE_AND_FREEZE

- Completed a sanitized synthetic real Provider structured-output smoke through backend runCopilot using the smoke profile and H2 in-memory database.
- Evidence files: docs/evidence/real-structured-output-smoke-20260730.md and docs/evidence/real-structured-output-smoke-20260730.json.
- Real Provider business request count: 1; Responses API requests: 0; retries: 0; no raw prompt, Provider response, model answer, Base URL, model name, API Key, Authorization header, project data, logs, or screenshots recorded.
- Ticket Copilot backend core is frozen: no more vector DB, multi-agent workflow, Responses Adapter, queue runtime, distributed Trace, or backend capability expansion for the portfolio branch.
- Next phase: PHASE_5_PORTFOLIO_WEBSITE_ENTERPRISE_TICKET_DETAIL; frontend real API wiring is deferred and nonblocking.
