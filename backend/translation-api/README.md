# GoStudio Translation Backend

为 GoStudio Android 端提供 gopls 文档翻译服务。后端持有模型或翻译厂商密钥，App 只配置一个后端地址。

## API

### `GET /healthz`

```json
{"status":"ok","provider":"openai"}
```

### `POST /v1/translate`

请求：

```json
{
  "text": "```go\\nfunc Add(a, b int) int\\n```\\n\\nAdd returns the sum of a and b.",
  "source_language": "en",
  "target_language": "zh-cn",
  "kind": "gopls-hover"
}
```

响应：

```json
{
  "translated_text": "```go\\nfunc Add(a, b int) int\\n```\\n\\nAdd 返回 a 和 b 的和。",
  "provider": "openai",
  "model": "gpt-4o-mini",
  "cached": false
}
```

可选鉴权：设置 `TRANSLATION_BACKEND_API_KEYS` 后，请求必须带：

```text
X-GoStudio-Translation-Key: <key>
```

## Provider 配置

### OpenAI-compatible LLM（OpenAI / DeepSeek / Kimi / Ollama 等）

```bash
export TRANSLATION_PROVIDER=openai
export LLM_BASE_URL=https://api.openai.com/v1
export LLM_MODEL=gpt-4o-mini
export LLM_API_KEY=sk-xxx
export TRANSLATION_BACKEND_API_KEYS=app-key
go run .
```

Ollama 示例：

```bash
TRANSLATION_PROVIDER=openai \
LLM_BASE_URL=http://127.0.0.1:11434/v1 \
LLM_MODEL=qwen2.5:7b \
LLM_API_KEY=ollama \
go run .
```

### Google Translate v2

```bash
TRANSLATION_PROVIDER=google GOOGLE_TRANSLATE_API_KEY=xxx go run .
```

### DeepL

```bash
TRANSLATION_PROVIDER=deepl DEEPL_API_KEY=xxx go run .
```

付费 DeepL 可设置 `DEEPL_API_ENDPOINT=https://api.deepl.com/v2/translate`。

### 本地联调

```bash
TRANSLATION_PROVIDER=mock TRANSLATION_BACKEND_ADDR=:8080 go run .
```

在 App「编辑器设置 → gopls」开启“中文文档翻译”，后端地址填写局域网地址，例如 `http://192.168.1.10:8080`。

## Docker

```bash
docker build -t gostudio-translation-api .
docker run --rm -p 8080:8080 \
  -e TRANSLATION_PROVIDER=openai \
  -e LLM_BASE_URL=https://api.openai.com/v1 \
  -e LLM_MODEL=gpt-4o-mini \
  -e LLM_API_KEY=sk-xxx \
  -e TRANSLATION_BACKEND_API_KEYS=app-key \
  gostudio-translation-api
```
