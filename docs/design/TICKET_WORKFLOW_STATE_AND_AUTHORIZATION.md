# 企业 AI 工单状态转移与写接口授权（Phase 0-A）

> 更新日期：2026-09-04。本文只描述当前仓库已经实现并由测试验证的后端规则，不代表生产认证、生产 RBAC 或高并发能力。

## 1. 当前真实状态

状态仍以数据库和旧 REST DTO 使用的字符串合同为准，集中定义在 `TicketStatusContract`：

`PENDING_CLASSIFICATION`、`PENDING_PROCESS`、`AI_DRAFTED`、`REVIEW_REQUIRED`、`IN_PROGRESS`、`APPROVED`、`RESOLVED`、`REJECTED`、`KNOWLEDGE_BASED`。

创建工单时先由内部规则分类流程将 `PENDING_CLASSIFICATION` 写成 `PENDING_PROCESS`。正常运行 Copilot 后根据人工复核门禁进入 `AI_DRAFTED` 或 `REVIEW_REQUIRED`；Provider 硬失败时仍会保存运行错误证据，但工单保持原状态。审核通过进入 `RESOLVED`，要求修改回到 `REVIEW_REQUIRED`，驳回进入 `REJECTED`。已解决工单可以生成草稿，草稿经确认发布后工单进入 `KNOWLEDGE_BASED`。

`RESOLVED` 不是终态，因为现有架构图和 Showcase 流程明确支持 `RESOLVED -> IN_PROGRESS` 复核回退，以及 `RESOLVED -> KNOWLEDGE_BASED` 知识沉淀。当前策略把 `APPROVED`、`REJECTED`、`KNOWLEDGE_BASED` 视为终态；其中 `APPROVED` 是现有状态合同中的保留终态，当前没有新增进入它的 REST 动作。

## 2. 状态转移矩阵

所有手工状态更新、审核动作、Copilot 产生状态以及知识发布动作都经过 `TicketStatusTransitionPolicy`。策略同时接收当前状态、目标状态、操作类型、当前 Copilot Run 是否存在、当前 Run 是否已经审核完成和当前 Run 是否已有审核记录。

| 操作 | 合法来源 | 合法目标/结果 | 额外条件 |
| --- | --- | --- | --- |
| 初始分类（内部流程） | `PENDING_CLASSIFICATION` | `PENDING_PROCESS` | 只能由创建工单流程使用；不能由通用状态接口伪造 |
| 首次 Copilot | `PENDING_PROCESS` | `AI_DRAFTED` 或 `REVIEW_REQUIRED` | 当前 Run 由本次操作创建 |
| Copilot 重跑 | `AI_DRAFTED` 或 `REVIEW_REQUIRED` | `AI_DRAFTED` 或 `REVIEW_REQUIRED` | 必须已有当前持久化 Run；当前 Run 不能已完成最终审核；要求修改后需要新一轮 Run |
| Copilot Provider 硬失败 | 可运行的 Copilot 来源状态 | 状态不变 | 这是显式 no-op；只保存运行失败证据和同状态历史，不接受未经验证的模型输出 |
| 手工接手 | `PENDING_PROCESS` | `IN_PROGRESS` | 若存在当前 Copilot Run，必须先完成审核 |
| 手工确认解决 | `PENDING_PROCESS` | `RESOLVED` | 保留现有 Showcase 的简单问题人工确认路径；若存在当前 Run，必须先完成审核 |
| 手工处理完成 | `IN_PROGRESS` | `RESOLVED` | 同上 |
| 复核回退 | `RESOLVED` | `IN_PROGRESS` | 保留现有架构说明中的复核发现仍需处理路径 |
| 审核通过 | `AI_DRAFTED` 或 `REVIEW_REQUIRED` | `RESOLVED` | 必须有当前 Run，且当前 Run 尚无审核记录 |
| 要求修改 | `AI_DRAFTED` 或 `REVIEW_REQUIRED` | `REVIEW_REQUIRED` | 同一 Run 只能执行一次；之后必须重跑 Copilot 才能再次审核 |
| 审核驳回 | `AI_DRAFTED` 或 `REVIEW_REQUIRED` | `REJECTED` | 必须有当前 Run，且当前 Run 尚无审核记录 |
| 创建知识草稿 | `RESOLVED` 或 `KNOWLEDGE_BASED` | 工单状态不变 | `RESOLVED` 且存在 Copilot Run 时，必须已有完成审核；`confirm=false` 只创建 `DRAFT` |
| 确认/发布知识 | `RESOLVED` | `KNOWLEDGE_BASED` | 草稿必须是 `DRAFT`；Copilot 解决的工单必须已有完成审核 |
| 重复确认已发布草稿 | 已发布草稿 | 状态不变 | 保留既有幂等语义；不追加历史、不重复写工单状态 |

未列出的已知状态边均拒绝并返回 HTTP 409。`PENDING_CLASSIFICATION` 是内部 intake 状态，虽然属于已知状态，但不能作为通用手工目标。完全未知的状态字符串仍按输入不支持返回 HTTP 400。现有代码、架构说明和 Showcase 测试没有给出其他状态边的唯一业务依据，因此这些边暂不开放，待产品确认后另立任务。

状态写入使用 `support_ticket.id + expected status` 条件更新。受影响行数不是 1 时返回 409，事务会回滚本次状态历史、审核记录、知识发布等副作用；因此并发请求不会静默覆盖先到请求的状态。

## 3. 写接口授权矩阵

授权集中在 `TicketAuthorizationPolicy`，Controller 不再复制角色判断。读接口仍由现有 `AuthInterceptor` 要求 Bearer 身份；VIEWER 可以执行当前已允许的认证读操作，但不能执行下表写操作。

| 写动作 | ADMIN | AGENT | REVIEWER | VIEWER | 匿名 |
| --- | --- | --- | --- | --- | --- |
| 创建工单 | 允许 | 允许 | 拒绝 | 拒绝 | 401 |
| 手工更新状态 | 允许 | 允许 | 拒绝 | 拒绝 | 401 |
| 运行 Copilot | 允许 | 允许 | 拒绝 | 拒绝 | 401 |
| 审核通过 / 要求修改 / 驳回 | 允许 | 拒绝 | 允许 | 拒绝 | 401 |
| 创建知识草稿（`confirm=false`） | 允许 | 允许 | 拒绝 | 拒绝 | 401 |
| 确认/发布知识草稿（含 `confirm=true`） | 允许 | 拒绝 | 允许 | 拒绝 | 401 |

角色不足统一返回 HTTP 403，并沿用项目统一错误响应中的 `code=403`。`confirm=true` 不会沿用普通草稿创建权限，而是按发布权限检查；随后服务层仍会再次执行知识发布状态策略。

## 4. Demo 认证边界

当前仍是 Demo Auth：`AuthService` 使用仓库内既有的四个演示账号和自定义 HMAC-SHA256 Bearer token，默认签名 key 也是演示配置。Phase 0-A 只加固应用层操作授权和工作流状态边界，没有把它升级为生产身份系统。

以下能力明确未实现：OIDC/OAuth2 登录、JWK/JWKS 密钥发现与轮换、issuer/audience 校验、生产会话/刷新机制、组织/租户隔离、资源级 RBAC、审计告警和生产密钥托管。因此当前实现不能称为生产级认证或生产级 RBAC。

本轮未修改真实 GPT/DeepSeek Provider、前端视觉、数据库 schema、Outbox/消息队列/Worker、公网部署或另一个项目。

## 5. 本轮验证

- `backend/`：`mvn -B test`，282/282 通过，0 failures、0 errors、0 skipped。
- `frontend/`：`npm run build` 通过，包含 `vue-tsc`，Vite 完成 63 modules；`package.json` 没有 `test` script，因此没有执行不存在的前端测试命令。
- 新增 HTTP/持久化授权与状态测试见 `backend/src/test/java/com/enterpriseai/ticketcopilot/TicketWorkflowPhase0AIntegrationTest.java`；策略单测见 `ticket/application/policy/`。
