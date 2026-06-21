# 架构说明

Enterprise AI Ticket Copilot 是一个面向企业内部员工、IT 支持、运维和业务支持团队的 AI 工单与知识库助手。第 2 阶段仍不接真实 LLM，所有分析结果来自本地规则、关键词匹配和模板生成，并且必须经过人工确认后进入状态流转或知识沉淀。

## 前后端架构图

```mermaid
flowchart LR
  user["内部员工 / IT 支持 / 运维"] --> web["Vue 3 + Vite<br/>AI 工单 Copilot 工作台"]

  subgraph frontend["Frontend"]
    web --> queue["工单队列"]
    web --> detail["工单详情与时间线"]
    web --> ai_panel["AI 建议与人工确认面板"]
  end

  subgraph backend["Spring Boot 3 Backend"]
    api["REST Controller"] --> workflow["TicketWorkflowService"]
    workflow --> classifier["RuleClassificationService"]
    workflow --> matcher["KnowledgeMatchingService"]
    workflow --> template["RecommendationTemplateService"]
    workflow --> mapper["MyBatis-Plus Mapper"]
  end

  subgraph mysql["MySQL"]
    support_ticket["support_ticket"]
    knowledge_article["knowledge_article"]
    ticket_ai_analysis["ticket_ai_analysis"]
    generation_record["generation_record"]
    ticket_status_history["ticket_status_history"]
  end

  web -->|/api/tickets| api
  mapper --> support_ticket
  mapper --> knowledge_article
  mapper --> ticket_ai_analysis
  mapper --> generation_record
  mapper --> ticket_status_history
```

## 工单状态流转图

```mermaid
stateDiagram-v2
  [*] --> PENDING_CLASSIFICATION: 员工提交工单
  PENDING_CLASSIFICATION --> PENDING_PROCESS: 本地规则分类 + 知识匹配
  PENDING_PROCESS --> IN_PROGRESS: 支持人员人工接手
  IN_PROGRESS --> RESOLVED: 人工确认已解决
  RESOLVED --> KNOWLEDGE_BASED: 生成草稿并人工确认入库
  KNOWLEDGE_BASED --> [*]

  PENDING_PROCESS --> RESOLVED: 简单问题人工确认解决
  RESOLVED --> IN_PROGRESS: 复核发现仍需处理
```

## AI 分析 + 人工确认流程图

```mermaid
flowchart TD
  intake["提交工单：标题、描述、系统、日志、紧急程度"] --> classify["本地规则分类<br/>账号 / 权限 / 系统故障 / 数据 / 流程咨询"]
  classify --> match["知识库关键词匹配"]
  match --> recommend["模板生成排查步骤、回复建议、风险提示"]
  recommend --> pending["写入 ticket_ai_analysis<br/>confirmation_state = 待人工确认"]
  pending --> human{"人工确认?"}
  human -- 接手处理 --> in_progress["状态流转：处理中"]
  human -- 需要补充 --> pending
  human -- 确认解决 --> resolved["状态流转：已解决"]
```

## 知识沉淀流程图

```mermaid
flowchart TD
  resolved["已解决工单"] --> draft["生成知识库草稿<br/>status = DRAFT"]
  draft --> review{"知识负责人审核?"}
  review -- 退回补充 --> draft
  review -- 人工确认发布 --> publish["保存为 PUBLISHED 知识条目"]
  publish --> ticket_state["工单状态更新为已沉淀"]
  publish --> reuse["后续工单关键词匹配复用"]
```

## 边界约束

- 不连接真实 LLM，不发送工单内容到外部模型。
- 不自动执行授权、回滚、重启、通知、爬虫或外部系统操作。
- 规则分析、处理建议、知识草稿都只作为人工确认前的辅助信息。
- `generation_record` 保存规则或模板输出来源、输入摘要、输出摘要、耗时和状态，便于审计。
