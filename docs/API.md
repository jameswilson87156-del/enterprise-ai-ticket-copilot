# API 文档

> 本文档面向 GitHub 展示和面试快速阅读，整理后端真实 REST API 的设计、请求响应结构、统一错误格式和项目边界。完整机器可读接口仍以运行后的 Swagger / OpenAPI 为准。

## 1. API 概览

后端基于 Spring Boot 3，REST API 面向企业工单辅助处理流程，覆盖工单录入、队列查询、详情查看、规则引擎辅助分析、知识库评分匹配、状态流转、知识草稿生成与发布确认、健康检查等场景。

当前项目使用本地规则引擎辅助分类、关键词与评分公式做知识库匹配，并通过模板化方式生成建议草稿。系统不调用真实 LLM，不做模型训练，也不做 embedding / 向量检索。所有状态流转、对外回复和知识入库都需要人工确认。

## 2. 本地访问地址

后端端口来自 `backend/src/main/resources/application.yml` 的 `server.port`，当前默认是 `8080`。

- 默认后端地址：[http://localhost:8080](http://localhost:8080/)
- Swagger UI：[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- OpenAPI JSON：[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

本地 MySQL 运行配置可参考 `backend/src/main/resources/application-example.yml`。仓库不提交真实数据库账号密码。

## 3. 统一错误响应

后端通过 `GlobalExceptionHandler` 统一返回错误结构，响应体来自 `ApiErrorResponse`：

```json
{
  "code": 400,
  "message": "title is required",
  "path": "/api/tickets",
  "timestamp": "2026-06-21T23:29:00+08:00"
}
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `code` | HTTP 状态码，例如 `400`、`404`、`500` |
| `message` | 面向调用方的错误说明，不返回 Java 堆栈 |
| `path` | 当前请求路径 |
| `timestamp` | 服务端生成错误响应的时间 |

当前统一处理范围：

- 参数校验失败：`400 Bad Request`，例如 `title is required`、`urgency must be P1, P2 or P3`。
- 参数绑定失败：`400 Bad Request`。
- `ConstraintViolationException`：`400 Bad Request`。
- `IllegalArgumentException`：`400 Bad Request`。
- 资源不存在：`404 Not Found`。
- `ResponseStatusException`：沿用异常中指定的状态码和 reason。
- 未预期异常：`500 Internal Server Error`，只返回“服务器内部错误”，不泄露堆栈。

## 4. 核心接口列表

路径中的 `{id}` 是工单编号 `ticketNo`，例如 `TCK-260621233000-123` 或演示数据中的 `DEMO-0001`。`{articleNo}` 是知识条目编号，例如 `KB-DRAFT-...`。

| 方法 | 路径 | 功能 | 请求体或参数 | 响应说明 | 备注 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/health` | 健康检查 | 无 | 返回 `status`、`service`、`phase`、`timestamp` | 用于确认后端启动 |
| `GET` | `/api/tickets` | 查询工单队列 | 无查询参数 | 返回 `TicketSummary[]` | 当前后端不提供分页或筛选参数 |
| `POST` | `/api/tickets` | 创建工单，并同步生成规则分类、知识匹配和模板化建议草稿 | `CreateTicketRequest` | 返回 `TicketDetail` | DTO 使用 Bean Validation 校验 |
| `GET` | `/api/tickets/metrics` | 查询工作台指标 | 无 | 返回 `WorkbenchMetrics` | `knowledgeCoverage` 是兼容字段名，前端展示为知识关联率 |
| `GET` | `/api/tickets/{id}` | 查看工单详情、上下文、状态历史和知识草稿 | 路径参数 `id` | 返回 `TicketDetail` | 工单不存在返回统一 `404` |
| `GET` | `/api/tickets/{id}/ai-analysis` | 查询规则引擎辅助分析结果 | 路径参数 `id` | 返回 `AiAnalysis` | 路径保留历史命名；当前不是 LLM 分析 |
| `POST` | `/api/tickets/{id}/status` | 人工确认工单状态流转 | `UpdateTicketStatusRequest` | 返回更新后的 `TicketDetail` | 只接受源码允许的状态值 |
| `POST` | `/api/tickets/{id}/knowledge-draft` | 为已解决或已沉淀工单生成知识草稿，可选直接发布 | `CreateKnowledgeDraftRequest` | 返回 `KnowledgeDraft` | 非已解决工单会返回统一 `400` |
| `POST` | `/api/tickets/knowledge/{articleNo}/confirm` | 人工确认并发布知识草稿 | 路径参数 `articleNo` | 返回 `KnowledgeDraft` | 已发布草稿重复确认会直接返回当前结果 |

### 4.1 请求 DTO

`CreateTicketRequest`：

| 字段 | 类型 | 是否必填 | 校验 / 默认行为 |
| --- | --- | --- | --- |
| `title` | string | 是 | 非空，最长 180 字符 |
| `description` | string | 是 | 非空 |
| `systemName` | string | 否 | 最长 96 字符；空值时服务端默认 `unknown-system` |
| `errorLog` | string | 否 | 最长 20000 字符；空值时默认空字符串 |
| `urgency` | string | 否 | 只能是 `P1`、`P2`、`P3`；空值时默认 `P2` |
| `requester` | string | 否 | 最长 64 字符；空值时默认 `员工自助提交` |
| `requesterDepartment` | string | 否 | 最长 96 字符；空值时默认 `Internal` |

`UpdateTicketStatusRequest`：

| 字段 | 类型 | 是否必填 | 校验 / 默认行为 |
| --- | --- | --- | --- |
| `status` | string | 是 | 非空；业务上只接受 `PENDING_PROCESS`、`IN_PROGRESS`、`RESOLVED`、`KNOWLEDGE_BASED` |
| `actor` | string | 否 | 最长 64 字符；空值时默认 `Support Desk` |
| `note` | string | 否 | 最长 1000 字符；空值时默认 `人工确认状态流转。` |
| `resolvedSummary` | string | 否 | 最长 10000 字符 |

`CreateKnowledgeDraftRequest`：

| 字段 | 类型 | 是否必填 | 校验 / 默认行为 |
| --- | --- | --- | --- |
| `title` | string | 否 | 最长 180 字符；空值时默认 `【草稿】` + 工单标题 |
| `content` | string | 否 | 最长 20000 字符；空值时由规则模板基于工单信息生成草稿内容 |
| `owner` | string | 否 | 最长 96 字符；空值时默认 `Knowledge Owner` |
| `confirm` | boolean | 否 | `true` 表示生成后直接发布；未传时为 `false` |

### 4.2 主要响应结构

`TicketSummary` 用于工单队列：

```json
{
  "id": "TCK-260621233000-123",
  "title": "支付服务启动失败",
  "requester": "周冉",
  "team": "payment-service",
  "status": "PENDING_PROCESS",
  "priority": "P1",
  "category": "系统故障",
  "aiConfidence": 88,
  "updatedAt": "23:30"
}
```

`WorkbenchMetrics` 用于工作台指标：

```json
{
  "pendingTickets": 3,
  "aiHitRate": 80,
  "knowledgeCoverage": 60,
  "todayKnowledgeDrafts": 1
}
```

说明：`aiHitRate` 和 `knowledgeCoverage` 是当前接口兼容字段名。文案展示时应理解为规则命中率和知识关联率，不代表真实 AI 模型效果。

`AiAnalysis` 用于规则引擎辅助分析结果：

```json
{
  "ticketId": "TCK-260621233000-123",
  "classification": "系统故障",
  "classificationReason": "本地规则在标题、描述、系统名和错误日志中识别到相关关键词。",
  "confidence": 88,
  "confirmationState": "待人工确认",
  "knowledgeHits": [
    {
      "id": "KB-SPRING-BEAN",
      "title": "Spring Boot BeanCreationException 处理清单",
      "relevance": 88,
      "owner": "Java 平台组",
      "lastVerifiedAt": "2026-06-20"
    }
  ],
  "troubleshootingSteps": ["定位异常 bean 和缺失配置项"],
  "replySuggestion": "建议先核对相关配置和 profile，确认后再处理。",
  "riskNotes": ["配置变更需人工确认"]
}
```

## 5. 关键请求示例

### 5.1 创建工单

`POST /api/tickets`

```json
{
  "title": "Spring Boot 启动时报 BeanCreationException",
  "description": "payment-service 发布后启动失败，怀疑新增结算策略 bean 缺少配置。",
  "systemName": "payment-service",
  "errorLog": "BeanCreationException: Error creating bean with name settlementStrategy",
  "urgency": "P1",
  "requester": "周冉",
  "requesterDepartment": "支付研发"
}
```

响应为 `TicketDetail`，包含工单基础信息、系统上下文、错误日志、业务上下文、状态时间线和可选知识草稿：

```json
{
  "id": "TCK-260621233000-123",
  "title": "Spring Boot 启动时报 BeanCreationException",
  "requester": "周冉",
  "department": "支付研发",
  "status": "PENDING_PROCESS",
  "priority": "P1",
  "category": "系统故障",
  "description": "payment-service 发布后启动失败，怀疑新增结算策略 bean 缺少配置。",
  "systemContext": {
    "application": "payment-service",
    "environment": "internal",
    "region": "corp",
    "lastDeployment": "-",
    "affectedUsers": "核心业务或多团队受影响"
  },
  "errorLogs": ["BeanCreationException: Error creating bean with name settlementStrategy"],
  "businessContext": [
    "本阶段使用本地规则分类与模板建议，不调用真实 LLM。",
    "所有处理建议均等待人工确认后执行。"
  ],
  "timeline": [
    {
      "time": "23:30",
      "state": "PENDING_PROCESS",
      "actor": "Rule Engine",
      "note": "完成规则分类、知识库匹配和建议草案生成，等待人工确认。"
    }
  ],
  "knowledgeDraft": null
}
```

### 5.2 查询规则引擎辅助分析

`GET /api/tickets/{id}/ai-analysis`

该接口返回当前工单最近一次规则分类、知识库命中和模板化建议草稿。路径中的 `ai-analysis` 是历史命名，不表示接入真实 LLM。

如果工单不存在，返回统一 `404`；如果工单存在但没有分析记录，也返回统一 `404`，message 为 `AI analysis not found.`。

### 5.3 人工确认状态流转

`POST /api/tickets/{id}/status`

```json
{
  "status": "IN_PROGRESS",
  "actor": "Support Desk",
  "note": "支持人员已人工接手排查。",
  "resolvedSummary": ""
}
```

允许的 `status`：

- `PENDING_PROCESS`
- `IN_PROGRESS`
- `RESOLVED`
- `KNOWLEDGE_BASED`

传入其他状态会返回统一 `400`，例如 `Unsupported status: CLOSED`。

### 5.4 生成知识草稿

`POST /api/tickets/{id}/knowledge-draft`

```json
{
  "title": "【草稿】支付服务启动失败排查",
  "content": "来源工单：TCK-260621233000-123\n分类：系统故障\n处理摘要：补齐 UAT 配置后恢复。",
  "owner": "Java 平台组",
  "confirm": false
}
```

只有状态为 `RESOLVED` 或 `KNOWLEDGE_BASED` 的工单可以生成知识草稿。`confirm` 为 `true` 时会在生成草稿后直接发布，并把工单状态更新为 `KNOWLEDGE_BASED`；该动作仍表示“人工确认后发布”，不是系统自动决策。

### 5.5 统一错误响应示例

`POST /api/tickets` 传入空对象：

```json
{}
```

可能返回：

```json
{
  "code": 400,
  "message": "title is required",
  "path": "/api/tickets",
  "timestamp": "2026-06-21T23:29:00+08:00"
}
```

## 6. 业务边界

- 当前没有真实 LLM 调用。
- 当前没有真实 AI 模型训练。
- 当前没有 embedding / 向量检索。
- 当前没有生产级鉴权、角色权限或审计登录体系。
- 当前没有线上部署能力说明，也不声明生产 SLA。
- 当前规则引擎输出、模板化建议草稿、状态流转和知识发布都需要人工确认。
- Swagger / OpenAPI 用于接口展示和本地调试，不代表项目已经生产部署。
- 后端 `GET /api/tickets` 当前没有分页、排序、筛选查询参数；前端 Demo 模式中的搜索筛选属于前端交互能力。

## 7. 面试说明

这个项目中的 Copilot 定位是企业工单辅助处理工作台，核心是规则引擎辅助分类、知识库评分匹配、模板化建议草稿和人工确认闭环。没有接入真实 LLM，是为了降低演示环境依赖、保证规则可解释性和本地运行稳定性。

如果面试官追问“为什么接口路径里还有 `ai-analysis`”，可以说明：这是早期命名保留的兼容路径；当前文档、README 和前端 UI 已明确校准为规则引擎辅助分析，不把它包装成真实大模型能力。
