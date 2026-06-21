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

## 结论

本次本地验证中，后端 `mvn test` 与前端 `npm run build` 均真实运行并通过。测试结果可以支撑 README 中关于当前自动化测试和前端构建通过的说明，但不代表完整生产级质量保证。
