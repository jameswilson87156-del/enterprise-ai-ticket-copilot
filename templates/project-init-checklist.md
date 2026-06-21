# 新项目初始化检查清单

复制整个新手包并改成项目名称后，按顺序勾选。不要在母版上直接开发。

## A. 工具只读验证

- [ ] 在 PowerShell 运行 `claude --version`，记录实际结果。
- [ ] 运行 `claude doctor`，检查是否存在安装或配置问题。
- [ ] 由用户本人运行 `claude` 并完成登录；Agent 不替用户登录。
- [ ] 确认 Codex 能打开当前项目文件夹。
- [ ] 确认 VS Code 当前是否需要；新手第一轮不是必须安装。

如果 `claude` 不可用：

- [ ] 确认桌面文件是否只是未运行的安装包。
- [ ] 重新运行可信安装包，或由用户本人决定是否使用官方安装命令。
- [ ] 重新打开 PowerShell。
- [ ] 运行 `Get-Command claude -ErrorAction SilentlyContinue` 检查命令是否可见。
- [ ] 检查 PATH 是否生效，但不让 Agent 自动修改系统环境变量。

## B. 项目文件

- [ ] `README.md` 存在。
- [ ] `AGENTS.md` 存在，Codex 工作规则已阅读。
- [ ] `CLAUDE.md` 存在，并通过 `@AGENTS.md` 引用共同规则。
- [ ] `docs/PRD.md` 存在。
- [ ] `docs/DESIGN.md` 存在。
- [ ] `docs/ARCHITECTURE.md` 存在。
- [ ] `TODO.md` 和 `HANDOFF.md` 存在。
- [ ] 所有中文 Markdown 使用 UTF-8 with BOM。

## C. Git 初始化

```powershell
cd "你的项目文件夹"
git init
git status
```

- [ ] 已运行 `git init`，且命令没有报错。
- [ ] 已创建适合技术栈的 `.gitignore`。
- [ ] `.env`、密钥、依赖、构建输出和缓存不会进入 Git。
- [ ] 首次提交前已运行 `git status` 和 `git diff --cached`。

## D. 三份核心文档

- [ ] PRD 写清目标用户、核心功能、页面、业务流程和验收标准。
- [ ] DESIGN 写清布局、关键交互、表单、按钮及各种状态。
- [ ] ARCHITECTURE 写清真实技术栈、目录、运行、测试和部署方式。
- [ ] 未确认信息写“待确认”，没有被 AI 当成事实。
- [ ] 第一版明确列出“暂不做事项”。

## E. 第一轮协作

- [ ] Claude 完成需求采访，结果已写入 `HANDOFF.md`。
- [ ] 用户已经确认 PRD、设计和架构方向。
- [ ] Codex 只生成分阶段 TODO，没有提前写业务代码。
- [ ] 当前只选择了一个小 TODO。
- [ ] 已确定本轮测试或手工验收方式。

## F. 基线验收

- [ ] 项目可以按 `docs/ARCHITECTURE.md` 的命令运行。
- [ ] 可用测试/构建命令已实际执行并记录结果。
- [ ] 无法执行的验证已明确说明。
- [ ] `HANDOFF.md` 已记录当前状态和下一步。
- [ ] 验证通过后再创建基线 Git 提交。

