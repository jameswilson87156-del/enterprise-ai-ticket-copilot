# Backend T2：工作流策略端口

> 状态：本轮已完成。日期：2026-09-03。本轮只抽出一个最小策略端口，不把 Provider、认证、数据库或部署混入重构。

## 1. 本轮目标

在 T1 的入站应用端口基础上，继续改变旧工作流的依赖方向：把“最终是否必须人工复核”从 `TicketWorkflowService` 对具体 `ReviewGate` 的依赖，抽成应用端口。

```text
TicketWorkflowService
        ↓
ReviewPolicy                  # application outbound port
        ↑
ReviewGate                    # current Spring implementation
```

这里的 `out` 表示工作流对策略实现的依赖方向，不表示 HTTP 或外部网络调用。

## 2. 本轮修改

### 新增 `ReviewPolicy`

`backend/src/main/java/com/enterpriseai/ticketcopilot/ticket/application/port/out/ReviewPolicy.java` 只接收 Copilot 最终输出、fallback 标志、Citation 校验状态、结构化输出校验状态和业务复核标志。

它没有暴露：

- MyBatis Entity。
- Mapper。
- `AiProviderService` 或其他 Provider 类型。
- 旧 `service` 包中的 `CitationValidationResult` / `ReviewGate` 类型。

### 迁移 `ReviewGate`

`ReviewGate` 现在实现 `ReviewPolicy`。原有 `finalHumanReviewRequired(... CitationValidationResult ...)` 保留为兼容方法，并把旧结果转换为端口需要的 `CitationValidationStatus`；新工作流代码只调用 `ReviewPolicy.requiresHumanReview(...)`。

因此，当前行为保持不变：

- 空输出、模型要求复核、高风险输出必须进入人工复核。
- fallback 必须进入人工复核。
- Citation 失败、结构化输出失败必须进入人工复核。
- abstention 和缺少必要信息必须进入人工复核。
- 业务规则要求复核时必须进入人工复核。

### 结构验收

新增 `WorkflowPortBoundaryTest`，固定：

1. `TicketWorkflowService` 的字段和构造器依赖 `ReviewPolicy`，不依赖 `ReviewGate`。
2. `ReviewPolicy` 的参数和返回值不泄漏旧 Service、Entity 或 Mapper 包类型。
3. 当前 `ReviewGate` 确实实现该端口，Spring 运行时仍有可用实现。

## 3. 明确不变的东西

- `/api/...` 路径和 Controller 入站端口不变。
- 数据库表、字段、Flyway 迁移和 MyBatis Mapper 不变。
- Provider 选择、local-rule fallback、结构化输出、Citation membership 校验和拒答原因不变。
- Trace、Copilot Run、Review history 和人工复核状态流转不变。
- 没有新增生产依赖、消息队列、Spring Modulith、ArchUnit、真实密钥或外部服务。

## 4. 验收

```text
cd backend
mvn test
```

退出条件：端口结构测试通过，旧 `ReviewGate` 语义测试通过，Controller / H2 / 工作流 / Provider / Citation 回归测试全部通过。

## 5. 下一步

T2 只证明一个最小策略端口已经可替换。后续可以在单独任务中继续抽取 `AiProviderPort`、`RetrievalPort` 和 `AuditRecorder`，但每次仍需保留当前 API、Trace/Citation 证据和人工复核安全门禁。
