# 检索评测演示说明

## 这次升级做了什么

增加了一条可以从页面实际运行的检索基线评测链路：

`Vue 评测页 → POST /api/evaluations/retrieval/run → RetrievalEvaluationController → RetrievalEvaluationService → KnowledgeMatchingService → MySQL`。

后端读取 `backend/src/main/resources/eval/knowledge-retrieval-v1.json`，只在这组样例涉及的已发布知识编号中执行关键词检索，计算 Recall@1、Recall@3、MRR 和逐条目标排名，并把同一份 JSON 报告写到 `backend/target/evaluation/knowledge-retrieval-v1.json`。前端展示返回的指标、评测时间、查询、标注目标、候选文章及失败诊断。

评测服务固定走关键词基线，不调用 DeepSeek 或 Embedding API。工单 ChatModel 是另一条链路；不能把评测页的结果解释成 DeepSeek 成绩。

## 本地启动

1. 按 README 的「本地 MySQL 闭环」初始化数据库，并导入 `schema.sql` 与 `demo-data.sql`。演示数据库中需要包含评测集引用的 `KB-*` 已发布知识。
2. 在 `backend/src/main/resources/` 配置 `application-local.yml`，参考 `application-example.yml` 填写数据库连接。评测本身不需要模型密钥。
3. 如果本地设置了 DeepSeek Chat 密钥，设置 `AI_EMBEDDING_ENABLED=false`，然后启动后端：

   ```powershell
   $env:AI_CHAT_API_KEY = $env:DEEPSEEK_API_KEY
   $env:AI_CHAT_BASE_URL = 'https://api.deepseek.com'
   $env:AI_CHAT_MODEL = 'deepseek-flash'
   $env:AI_EMBEDDING_ENABLED = 'false'
   cd backend
   mvn spring-boot:run "-Dspring-boot.run.profiles=local"
   ```

   若只演示关键词评测且没有配置模型密钥，跳过 DeepSeek 环境变量即可。
4. 另开终端启动普通 API 模式前端：

   ```powershell
   cd frontend
   npm run dev
   ```

   打开 `http://localhost:5173`，进入「RAG 检索评测」，点击「运行检索评测」。`npm run dev:demo` 是静态 Demo 模式，会禁用后端评测按钮。

也可以直接调用接口：

```powershell
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/evaluations/retrieval/run'
```

首次运行前页面只展示空状态，不显示预设成绩。成功后，报告还会保存到后端的 `target/evaluation/knowledge-retrieval-v1.json`。

## 如何解释指标

- **Recall@1**：目标知识排在第一位的查询比例。
- **Recall@3**：目标知识进入前三的查询比例。
- **MRR**：逐条计算目标排名倒数后取平均，目标越靠前分值越高，未召回按 0 计。
- **逐条失败类型**：`EXPECTED_ARTICLE_NOT_RETRIEVED` 表示目标文章未召回；`EXPECTED_ARTICLE_RANKED_BELOW_FIRST` 表示有召回但没有排第一；`NONE` 表示排第一。

当前每条查询只有一个标注目标，所以前两个 Recall 都是按查询统计的命中比例。评测语料只有 16 条合成问句、覆盖 8 条演示知识，而且候选限定在这 8 条知识中；它适合当固定小样本基线，不代表全库误召回或生产效果。

## 面试现场讲法

> 我给知识检索加了一个可以从前端触发的离线评测页。仓库里维护固定的合成问句和期望知识编号，页面调用 Spring Boot 接口后，评测 Service 复用现有知识匹配服务，在限定语料内跑关键词基线，返回 Recall@1、Recall@3、MRR 和每条查询的排名，同时落一份 JSON 报告。这样我能从失败样例区分没召回和排序靠后的问题。当前集子比较小，也只测关键词基线，所以我会把这个结果称为回归基线，不称为 DeepSeek 或生产效果。

面试官问「下一步怎么升级」时可以回答：

> 我会先补充更多人工标注查询、未命中问题和容易混淆的负例，再把评测从限定的演示语料扩展到完整发布知识库。之后在 Embedding 服务可用时，保持同一数据集，对关键词、向量和混合检索做对照，记录数据集版本和每条失败结果；确认收益后再考虑加重排。这样每次改检索策略都能用固定样例回归，而不是凭几条手工演示判断。
