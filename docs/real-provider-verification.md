# OpenAI-compatible Provider 楠岃瘉鎸囧崡

鏈枃鐢ㄤ簬鏈湴楠岃瘉鐪熷疄 Provider 浠ｇ爜璺緞銆備粨搴撲笉鎻愪氦鐪熷疄 API Key锛屼笉瑕佹眰 `.env`锛岀ず渚嬪叏閮ㄤ娇鐢ㄥ崰浣嶇銆?

## 涓存椂鐜鍙橀噺

PowerShell锛?

```powershell
$env:TICKET_AI_PROVIDER="openai-compatible"
$env:TICKET_AI_BASE_URL="<OPENAI_COMPATIBLE_BASE_URL>"
$env:TICKET_AI_MODEL="<MODEL_NAME>"
$env:TICKET_AI_API_KEY="<API_KEY>"
$env:TICKET_AI_PROTOCOL="chat-completions"
$env:TICKET_AI_FALLBACK_TO_LOCAL="true"
```

涓嶈鎶婁互涓婂€煎啓鍏?`.env`銆乣application-local.yml` 鎴栦换浣曟彁浜ゆ枃浠躲€?

## 鍚姩鍚庣

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 鐧诲綍骞惰繍琛?Copilot

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

## 妫€鏌?Trace Evidence

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/tickets/DEMO-0002/trace-evidence" `
  -Headers $headers
```

閲嶇偣妫€鏌ワ細

- `aiAnalysis.providerName`
- `aiAnalysis.modelName`
- `aiAnalysis.fallbackUsed`
- `aiAnalysis.fallbackReason`
- `aiAnalysis.latencyMs`
- `aiAnalysis.status`
- `generationRecords[].businessType = AI_PROVIDER`

濡傛灉娌℃湁鐪熷疄 Key锛屾湰杞彧鑳借鏄庘€淧rovider 浠ｇ爜璺緞宸插疄鐜帮紝寰呮湰鍦?Key 楠岃瘉鈥濓紝涓嶈兘鍐欌€滅湡瀹?Provider 璋冪敤宸查獙璇佲€濄€?

## 娓呯悊鐜鍙橀噺

```powershell
Remove-Item Env:TICKET_AI_PROVIDER -ErrorAction SilentlyContinue
Remove-Item Env:TICKET_AI_BASE_URL -ErrorAction SilentlyContinue
Remove-Item Env:TICKET_AI_MODEL -ErrorAction SilentlyContinue
Remove-Item Env:TICKET_AI_API_KEY -ErrorAction SilentlyContinue
Remove-Item Env:TICKET_AI_PROTOCOL -ErrorAction SilentlyContinue
Remove-Item Env:TICKET_AI_FALLBACK_TO_LOCAL -ErrorAction SilentlyContinue
```

## 瀹夊叏鎵弿

```powershell
rg -n "sk-|api_key|private key|TICKET_AI_API_KEY|secret" .
git status --short
git diff --check
```

`TICKET_AI_API_KEY` 鍙簲鍑虹幇鍦ㄩ厤缃鏄庛€佹枃妗ｅ崰浣嶇鎴栦唬鐮佽鍙栫幆澧冨彉閲忕殑浣嶇疆锛屼笉搴斿嚭鐜扮湡瀹?Key銆?
## Shared provider configuration

Ticket Copilot supports project-specific `TICKET_AI_*` configuration and shared `PORTFOLIO_AI_*` configuration. Project-specific variables take priority; shared variables are fallback only. If neither set is available, the safe default remains `local-rule`, so automated tests and CI do not require a real Provider.

The current adapter uses Chat Completions. When `PORTFOLIO_AI_PROTOCOL=both`, this project still selects Chat Completions. Responses API compatibility may be verified externally with synthetic demo prompts, but Ticket Copilot does not include a Responses Adapter yet.

Do not commit real Provider URLs, model names, keys, logs, screenshots, or business payloads. Real Provider verification must use synthetic demo data only and must not be described as stable production integration.
