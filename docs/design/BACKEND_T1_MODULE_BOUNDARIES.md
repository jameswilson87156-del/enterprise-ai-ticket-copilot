# Backend T1：工单模块边界第一条纵向切片

> 状态：本轮已完成第一条边界切片。日期：2026-09-03。本轮不声称整个后端已经完成模块迁移；旧工作流实现仍作为兼容适配器保留。

## 1. 本轮目标

在 T0 API/状态合同已经冻结后，先把 REST 入口和工单工作流之间的依赖方向改成应用端口：

```text
TicketController
      ↓
TicketWorkflowUseCase        # inbound application port
      ↓
LegacyTicketWorkflowFacade   # transitional adapter
      ↓
TicketWorkflowService        # existing implementation
```

这样可以先保护 API 和数据证据链，再逐步把旧的全能 Service 拆到 `ticket`、`knowledge`、`retrieval`、`copilot` 和 `audit` 模块。

## 2. 本轮修改

### 新增应用端口

`backend/src/main/java/com/enterpriseai/ticketcopilot/ticket/application/port/in/TicketWorkflowUseCase.java` 暴露当前 Controller 所需的用例：

- 工单队列、详情、规则分析和 Trace 查询。
- 工单创建和状态更新。
- Copilot 运行。
- Approve、Request changes、Reject。
- 知识草稿创建和确认。

端口只使用当前兼容 DTO 和 read model，不暴露 MyBatis Entity、Mapper 或 Provider 实现。

### 新增兼容适配器

`LegacyTicketWorkflowFacade` 是一个暂时的 Spring Service，内部委托现有 `TicketWorkflowService`。它的作用是先改变依赖方向，不在这一轮复制或重写 1600 行左右的工作流逻辑。

后续工作可以继续把这个适配器中的具体能力替换为 `TicketCommandPort`、`RetrievalPort`、`AiProviderPort` 和 `AuditRecorder`；本轮 T2 已先将 `ReviewPolicy` 从旧工作流中抽出，过渡委托仍需等完整模块迁移任务收尾后删除。

### 新增边界测试

`TicketModuleBoundaryTest` 固定两条规则：

1. Controller 的构造依赖必须是 `TicketWorkflowUseCase`，不能直接注入旧 `TicketWorkflowService`。
2. 应用端口的方法签名不能泄漏 persistence Entity、Mapper 或旧 Service 类型。

这不是完整的架构扫描器，但它是当前无新增架构测试依赖情况下，能直接保护第一条真实依赖边界的可重复检查。

## 3. 明确不变的东西

- 旧 `/api/...` 路径不变。
- 当前请求/响应 DTO 不变。
- `copilot_run`、`retrieval_hit`、`copilot_result`、`copilot_result_citation`、`review_record` 和状态历史不变。
- Demo Auth、关键词检索、local-rule fallback 和同步 Copilot 不变。
- 不新增 Spring Modulith、ArchUnit、消息队列、数据库迁移或 Provider 依赖。

## 4. 验收

```text
cd backend
mvn test
```

退出条件：模块边界测试通过，原有工作流/Provider/Citation/H2/Controller 测试全部通过，且旧 API 契约测试继续通过。

## 5. 下一条切片

T2 已完成最小策略端口切片。后续继续拆 Provider、检索或审计端口时，应另立明确任务；不与认证、向量检索、Outbox、真实 Provider 或公网部署混在同一轮。
