# OpenAI-compatible Provider 验证指南

本文用于本地验证真实 Provider 代码路径。仓库不提交真实 API Key，不要求 `.env`，示例全部使用占位符。

## 临时环境变量

PowerShell：

```powershell
$env:TICKET_AI_PROVIDER="openai-compatible"
$env:TICKET_AI_BASE_URL="https://api.example.com/v1"
$env:TICKET_AI_MODEL="gpt-4o-mini"
$env:TICKET_AI_API_KEY="<YOUR_API_KEY>"
$env:TICKET_AI_FALLBACK_TO_LOCAL="true"
```

不要把以上值写入 `.env`、`application-local.yml` 或任何提交文件。

## 启动后端

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 登录并运行 Copilot

```powershell
$login = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"agent","password":"agent123"}'

$headers = @{ Authorization = "Bearer $($login.token)" }

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/tickets/DEMO-0002/run-copilot" `
  -Headers $headers
```

## 检查 Trace Evidence

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/tickets/DEMO-0002/trace-evidence" `
  -Headers $headers
```

重点检查：

- `aiAnalysis.providerName`
- `aiAnalysis.modelName`
- `aiAnalysis.fallbackUsed`
- `aiAnalysis.fallbackReason`
- `aiAnalysis.latencyMs`
- `aiAnalysis.status`
- `generationRecords[].businessType = AI_PROVIDER`

如果没有真实 Key，本轮只能说明“Provider 代码路径已实现，待本地 Key 验证”，不能写“真实 Provider 调用已验证”。

## 清理环境变量

```powershell
Remove-Item Env:TICKET_AI_PROVIDER -ErrorAction SilentlyContinue
Remove-Item Env:TICKET_AI_BASE_URL -ErrorAction SilentlyContinue
Remove-Item Env:TICKET_AI_MODEL -ErrorAction SilentlyContinue
Remove-Item Env:TICKET_AI_API_KEY -ErrorAction SilentlyContinue
Remove-Item Env:TICKET_AI_FALLBACK_TO_LOCAL -ErrorAction SilentlyContinue
```

## 安全扫描

```powershell
rg -n "sk-|api_key|private key|TICKET_AI_API_KEY|secret" .
git status --short
git diff --check
```

`TICKET_AI_API_KEY` 只应出现在配置说明、文档占位符或代码读取环境变量的位置，不应出现真实 Key。
