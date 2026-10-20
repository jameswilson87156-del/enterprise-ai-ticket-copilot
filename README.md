# Enterprise AI Ticket Copilot

企业 AI 工单处理与知识库助手，是一个面向内部支持团队的个人作品集 / 实习简历项目。它围绕“工单进入 -> AI 分类 -> 知识库匹配 -> 处理建议 -> 人工确认 -> 状态流转 -> 知识沉淀”组织可演示闭环，强调 AI 辅助判断与人工决策边界。

当前仓库用于本地开发和作品展示，不是生产系统，不包含真实客户数据，也不承诺真实大模型的稳定接入。AI 只提供分类与处理建议，状态变更和知识发布仍由人工最终确认。

## 项目简介

项目将工单队列、工单详情、规则分析、知识命中、处理建议、人工确认和状态历史整合到同一个企业 AI Copilot 控制台。前端提供可独立运行的 Demo 模式；后端提供基于 Spring Boot、MyBatis-Plus 和 MySQL 的本地数据闭环。

## 技术栈

- 前端：Vue 3、TypeScript、Vite、原生 CSS Design Tokens
- 后端：Java 17、Spring Boot 3、MyBatis-Plus、Maven
- 数据库：MySQL 8
- 演示与验收：本地 Demo/Mock 数据、Playwright Core 截图脚本

## 核心功能

- 工单录入与队列：提交问题描述、系统信息、错误日志和优先级，并按关键词与状态筛选。
- AI 辅助分类：使用当前规则引擎生成问题分类、置信度、原因和风险提示。
- 知识匹配：根据工单内容、分类和关键词返回已有知识条目命中结果。
- 处理建议：整理排查步骤、回复建议和下一步动作，明确标记为待人工确认。
- 状态流转：记录待分类、待处理、处理中、已解决、已沉淀等状态变化。
- 知识沉淀：从已解决工单生成知识草稿，经人工确认后再进入知识库记录。
- 过程留痕：展示状态历史与建议生成记录，便于演示人机协作边界。

> 当前实现是规则驱动的本地演示闭环，不等同于完整 RAG 平台，也不代表 AI 可自动处理全部工单。

## 页面截图

以下截图由仓库内 Playwright 脚本从真实 Vue 前端页面生成，页面内容使用本地 Demo/Mock 数据，不包含真实企业或客户信息。

### 企业 AI 工单工作台

![企业 AI 工单工作台](docs/images/dashboard.png)

### 工单详情与处理上下文

![工单详情](docs/images/ticket-detail.png)

### AI 分析与人工确认

![AI 分析](docs/images/ai-analysis.png)

### 知识匹配与知识沉淀

![知识库](docs/images/knowledge-base.png)

`docs/images/large/` 保存对应的 `1920x1200` 大屏版本，适合演示或作品集排版。

## 快速启动

### 前端 Demo 模式

无需启动后端或 MySQL，即可使用本地 Demo/Mock 数据体验完整界面：

```bash
cd frontend
npm install
npm run dev:demo
```

默认访问：`http://localhost:5173`

### 本地 MySQL 闭环

1. 初始化数据库和演示数据：

```bash
mysql -uroot -p < backend/src/main/resources/schema.sql
mysql -uroot -p enterprise_ai_ticket_copilot < backend/src/main/resources/demo-data.sql
```

2. 参考 `backend/src/main/resources/application-example.yml` 创建本地配置并修改数据库账号。

3. 启动后端：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

4. 启动前端：

```bash
cd frontend
npm install
npm run dev
```

后端默认地址为 `http://localhost:8080`，前端默认地址为 `http://localhost:5173`。

## Demo 模式说明

- `npm run dev:demo` 使用前端内置的本地 Demo/Mock 工单，不依赖后端和数据库。
- Demo 数据仅用于展示分类、知识匹配、人工确认、状态流转和知识沉淀交互。
- `npm run screenshots` 会启动 Demo 页面，通过真实浏览器自动生成 README 截图。
- Demo 中出现的团队、系统、日志和工单均为演示内容，不对应真实企业客户。

## MCP Server 接入（AI 客户端工具）

后端内置一个基于 Spring AI 的 MCP Server（WebMVC + SSE 传输），把工单检索与知识检索作为工具暴露给任意 MCP 客户端（Cherry Studio、Claude Desktop、Cursor 等）：

- SSE 连接端点：`http://localhost:8080/sse`（消息回传 `POST /mcp/message`，由客户端自动协商）
- 暴露工具：
  - `searchTickets(status, keyword)`：按状态与关键词检索工单列表
  - `searchKnowledge(query)`：知识库混合检索（关键词 + 向量语义，各占 50%）

在 Cherry Studio 中连接步骤：

1. 打开「设置 -> MCP 服务器 -> 添加服务器」。
2. 类型选择 **SSE**，URL 填写 `http://localhost:8080/sse`。
3. 保存后启用，在对话中即可让模型调用 `searchTickets` / `searchKnowledge`。

说明：MCP 工具只读查询，不提供写入与状态变更能力；未配置模型 API Key 时 `searchKnowledge` 自动退化为纯关键词检索，服务本身照常可用。

## AI 能力配置（可选）

后端默认可在无任何 AI Key 的情况下启动（分类走规则引擎，检索走关键词）。配置真实模型后自动启用 LLM 能力：

```bash
export AI_DASHSCOPE_API_KEY=sk-xxxx   # 阿里云 DashScope API Key
# 可选覆盖：
# AI_DASHSCOPE_BASE_URL（默认 https://dashscope.aliyuncs.com/compatible-mode/v1）
# AI_DASHSCOPE_CHAT_MODEL（默认 qwen-plus）
# AI_DASHSCOPE_EMBEDDING_MODEL（默认 text-embedding-v3）
```

配置后：工单分类优先调用 qwen-plus（15s 超时，失败自动降级规则引擎，结果标注 `LLM` / `RULE` 来源）；知识库启动时全量向量化（text-embedding-v3）并参与混合召回。

### DeepSeek 配置

运行时可通过 OpenAI-compatible 接口将 ChatModel 切换为 DeepSeek。DeepSeek 官方文档当前给出的 API 地址是 `https://api.deepseek.com`，模型名使用 `deepseek-flash`。将本机已配置的 `DEEPSEEK_API_KEY` 传给应用通用变量：

```powershell
$env:AI_CHAT_API_KEY = $env:DEEPSEEK_API_KEY
$env:AI_CHAT_BASE_URL = 'https://api.deepseek.com'
$env:AI_CHAT_MODEL = 'deepseek-flash'
$env:AI_EMBEDDING_ENABLED = 'false'
```

当前共用的 `OpenAiApi` 负责 Chat 和 Embedding；DeepSeek Chat-only 配置需要关闭向量索引，知识检索走关键词路径。DashScope 配置仍保留 Chat 与 text-embedding-v3 混合检索。Key 只由本地环境变量提供，不要写入配置文件或 Git。

### 分类评估数据集

`backend/src/test/resources/eval/tickets-eval.json` 是 40 条均衡的合成工单，五类各 8 条，包含规则盲区样本；不含客户或生产工单。评估输出 accuracy、macro precision/recall/F1、分类别结果、混淆矩阵与盲区样本结果，机器可读报告写入 `backend/target/evaluation/`。

```powershell
cd backend
mvn -Dtest=EvaluationTest test
```

规则基线可离线运行。外部模型对比默认关闭；确认要发送合成样本并已在本地设置 `DEEPSEEK_API_KEY` 后，再显式设置 `AI_LLM_EVAL_PROVIDER=deepseek` 与 `AI_LLM_EVAL_ENABLED=true` 并运行同一命令。完整评测默认逐条调用模型 40 次，可能产生 API 费用；可用 `AI_LLM_EVAL_LIMIT` 做小样本联通检查，报告会标记样本数，不把部分运行当作完整评估。当前 DeepSeek 默认模型名为 `deepseek-flash`，可通过 `AI_DEEPSEEK_CHAT_MODEL` 覆盖。

本机规则基线结果：Accuracy **32/40（80.00%）**，Macro-F1 **79.21%**；挑战样本命中 **3/11（27.27%）**，暴露了权限与语义表达的规则盲区。结果只描述这批合成样本，不代表生产工单表现。完整类别指标和混淆矩阵见[分类评估证据](docs/evidence/classification-eval-v2/README.md)。DeepSeek 对比尚未运行，因此没有该模型的工单分类成绩。

### RAG 检索评估基线

backend/src/main/resources/eval/knowledge-retrieval-v1.json 提供 16 条合成检索问题，覆盖仓库演示知识库的 8 条知识，每条有期望知识编号。评估只在数据集列出的目标知识条目中检索，可用于检查知识召回和排序；数据集不含客户或生产资料。

评测固定走关键词基线，不调用 DeepSeek 或 Embedding API。使用本地 MySQL 并导入 schema.sql 与 demo-data.sql 后，启动后端和普通 API 模式的前端，在「RAG 检索评测」页点击「运行检索评测」；接口 `POST /api/evaluations/retrieval/run` 返回逐条结果，并把报告写入 `backend/target/evaluation/knowledge-retrieval-v1.json`。前端 `npm run dev:demo` 是静态演示模式，会禁用运行按钮。

也可以用命令行入口运行同一评测。启动时自动评测默认关闭，需要显式开启：

<pre>cd backend
mvn spring-boot:run "-Dspring-boot.run.arguments=--ticket-copilot.evaluation.retrieval.enabled=true --ticket-copilot.evaluation.retrieval.top-k=5 --ticket-copilot.ai.embedding-enabled=false"</pre>

报告包含 Recall@1、Recall@3、MRR、逐条命中排名和未命中原因。网页只在接口成功返回后显示真实计算结果，不预填或推测检索成绩。评测语料是仓库中的合成问句和演示知识条目；分数只能说明这个固定小样本上的关键词基线表现，不能代表 DeepSeek 效果或生产效果。

## 测试与验收结果

前端验收结果记录于项目截图与验收文档；当前 Spring AI 分支的后端测试与评测以本机运行结果为准：

| 检查项 | 结果 | 说明 |
| --- | --- | --- |
| `npm install` | 通过 | 依赖安装完成，审计结果为 0 个漏洞 |
| `npm run build` | 通过 | Vue TypeScript 检查与 Vite 生产构建完成 |
| `npm run screenshots` | 通过 | 生成 4 张标准截图和 4 张 `1920x1200` 截图 |
| `mvn -v` | 通过 | Maven 3.9.11、Java 17 可用 |
| `mvn -Dtest=EvaluationTest test` | 2 项，1 项跳过 | 规则基线通过；外部模型对比因默认关闭而跳过 |
| `mvn test` | 25 项，0 失败，1 项跳过 | 默认不调用外部模型；测试记录见分类评估证据 |

## 项目边界

- 这是个人作品集 / 实习简历项目，不是已上线的生产系统。
- 仓库与截图不包含真实客户数据，演示数据均为本地构造。
- 已接入 Spring AI OpenAI-compatible Chat/Embedding；DashScope qwen-plus 与 text-embedding-v3 为默认配置，Chat 可配置为 DeepSeek；未配置 Key 时自动降级为规则引擎与关键词检索；不承诺生产级 SLA。
- 知识检索为 RAG-lite（SimpleVectorStore 内存向量 + 混合打分），不宣称为完整 RAG 平台。
- AI 只生成建议；工单状态变更、对外回复和知识发布需要人工确认。
- MCP Server 仅暴露只读检索工具，不含写入能力。
- Stitch Design Taste 仅作为本轮界面视觉设计参考，不属于项目业务功能或商业 UI 资产。

## 简历亮点

- 从工单录入到知识沉淀，设计并实现了具备人工确认边界的企业 AI 工单闭环。
- 使用 Vue 3 + TypeScript 构建轻量企业 Copilot 工作台，并用 Playwright 固化真实页面截图验收。
- 使用 Spring Boot + MyBatis-Plus + MySQL 组织工单、状态历史、建议记录与知识草稿的数据关系。
- 在 README 中明确区分 Demo、规则能力、真实大模型与生产边界，避免将演示能力包装为生产能力。

## 项目文档

- [产品设计](docs/product-design.md)
- [前端风格规范](docs/frontend-style.md)
- [架构说明](docs/architecture.md)
- [演示脚本](docs/demo-script.md)
- [面试讲解指南](docs/interview-guide.md)
- [检索评测演示说明](docs/retrieval-evaluation-demo.md)
- [验收清单](docs/acceptance-checklist.md)

