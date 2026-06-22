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

## 结论

本次本地验证中，后端 `mvn test` 与前端 `npm run build` 均真实运行并通过。最新后端验证已包含 H2 内存库集成测试，测试结果可以支撑 README 中关于当前自动化测试和前端构建通过的说明，但不代表完整生产级质量保证。
