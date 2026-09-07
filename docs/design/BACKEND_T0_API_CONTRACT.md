# Backend T0：API 契约冻结

> 状态：本轮已完成。日期：2026-09-03。本文冻结当前兼容接口和关键状态词汇，不代表 `/api/v1`、真实身份、异步任务或生产部署已经实现。

## 1. 本轮目标

在进入模块化包迁移前，先把当前后端最容易被重构破坏的部分固定下来：

- 现有 REST 路径和 HTTP 方法。
- 工单状态字符串及其持久化表示。
- 通用人工状态接口允许的目标状态。
- Copilot、Review、Trace 和知识沉淀的现有兼容入口。
- 旧 `/api/...` 与未来 `/api/v1/...` 的迁移边界。

本轮不改变业务返回 DTO、不切换数据库、不添加真实 Provider、不引入 Spring Modulith 依赖，也不迁移包目录。

## 2. 当前冻结的兼容 API

### 2.1 Auth / Health

| 方法 | 路径 | 语义 |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Demo JWT + RBAC 登录 |
| `GET` | `/api/auth/me` | 读取当前用户 |
| `GET` | `/api/health` | 健康检查 |

### 2.2 Ticket

| 方法 | 路径 | 语义 |
| --- | --- | --- |
| `GET` | `/api/tickets` | 工单队列 |
| `POST` | `/api/tickets` | 创建工单并生成当前规则/知识/模板结果 |
| `GET` | `/api/tickets/metrics` | 工作台指标 |
| `GET` | `/api/tickets/{id}` | 工单详情 |
| `GET` | `/api/tickets/{id}/ai-analysis` | 兼容命名的规则辅助分析 |
| `GET` | `/api/tickets/{id}/trace-evidence` | Trace / Evidence 只读聚合 |
| `POST` | `/api/tickets/{id}/run-copilot` | 运行 Copilot |
| `POST` | `/api/tickets/{id}/review/approve` | 审核通过并进入当前解决闭环 |
| `POST` | `/api/tickets/{id}/review/request-changes` | 要求补充修改 |
| `POST` | `/api/tickets/{id}/review/reject` | 拒绝建议 |
| `POST` | `/api/tickets/{id}/status` | 人工状态流转 |
| `POST` | `/api/tickets/{id}/knowledge-draft` | 创建知识草稿 |
| `POST` | `/api/tickets/knowledge/{articleNo}/confirm` | 人工确认知识草稿 |

这些路径由 `backend/src/test/java/com/enterpriseai/ticketcopilot/api/ApiRouteContractTest.java` 通过 Controller 注解反射测试固定。未来包重构必须先让该测试继续通过。

## 3. 状态合同

当前数据库和 DTO 仍使用字符串状态。唯一状态词汇由 `TicketStatusContract` 提供：

```text
PENDING_CLASSIFICATION
PENDING_PROCESS
AI_DRAFTED
REVIEW_REQUIRED
IN_PROGRESS
APPROVED
RESOLVED
REJECTED
KNOWLEDGE_BASED
```

当前通用 `POST /api/tickets/{id}/status` 可以接受：

```text
PENDING_PROCESS
AI_DRAFTED
REVIEW_REQUIRED
IN_PROGRESS
APPROVED
RESOLVED
REJECTED
KNOWLEDGE_BASED
```

`PENDING_CLASSIFICATION` 只由创建工单的内部流程写入，不能由客户端通过通用状态接口任意设置。当前接口仍保留历史上较宽的状态更新语义；真正的合法状态转移矩阵会在后续 T1/T2 独立任务中实现，不能在本轮假装已经完成。

## 4. 当前不变量

- 旧 `/api/...` 路径继续兼容，`/api/v1/...` 只作为下一阶段目标，不在本轮偷偷切换。
- `run-copilot` 每次执行仍须产生对应的运行证据；已有 `copilot_run`、`retrieval_hit`、`copilot_result`、`copilot_result_citation` 和 `review_record` 不能因为包迁移而丢失。
- 无检索证据仍然走 `NO_RETRIEVAL_EVIDENCE` 安全拒答，不凭空生成 Citation。
- Approve、Request changes、Reject 的对外路径和历史 decision 枚举不变。
- 当前 Demo Auth、关键词匹配、local-rule fallback 和同步 Copilot 都保留，直到后续任务明确完成替换并通过回归。
- 任何新字段必须先判断是兼容字段、版本化字段还是内部字段，不能直接把规划字段加入当前响应并声称功能已经存在。

## 5. 下一阶段入口

T1 模块边界任务可以在这份合同上继续：

1. 按 `identity`、`ticket`、`knowledge`、`retrieval`、`copilot`、`provider`、`audit`、`ops` 整理包目录。
2. 把 Controller 依赖的用例入口抽成模块公开 facade。
3. 把 Provider、检索和 Review gate 变成 port；保留现有实现作为 adapter。
4. 加入模块依赖检查；如果采用 Spring Modulith，使用 `ApplicationModules.verify()`，否则先使用轻量架构测试。
5. 每完成一个模块迁移，必须重新执行本合同测试和全量 Maven 测试。

目标接口 `/api/v1`、身份/RBAC、租户、幂等键、异步 `202 + runId`、Outbox、真实 Provider 和 staging 部署，继续以 [后端下一阶段设计](BACKEND_NEXT_PHASE_DESIGN.md) 为规划，不在本轮写成已实现。

## 6. 本轮验收

```text
backend/ mvn test
```

必须满足：新增 API 路由合同测试和状态合同测试通过，原有测试全部通过，且没有修改 API 路径、数据库迁移、Provider 配置或真实密钥。
