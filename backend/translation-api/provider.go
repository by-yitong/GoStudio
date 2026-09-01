package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"time"
)

type translationProvider interface {
	Name() string
	Model() string
	Translate(ctx context.Context, request translateRequest) (string, error)
}

func newProvider(cfg config) (translationProvider, error) {
	switch cfg.Provider {
	case providerMock:
		return mockProvider{}, nil
	case providerLLM:
		return &llmProvider{
			baseURL:    cfg.LLMBaseURL,
			model:      cfg.LLMModel,
			apiKey:     cfg.LLMAPIKey,
			keyHeader:  cfg.LLMAPIKeyHeader,
			httpClient: &http.Client{Timeout: 45 * time.Second},
		}, nil
	case providerGoogle:
		return &googleProvider{
			apiKey:     cfg.GoogleAPIKey,
			httpClient: &http.Client{Timeout: 30 * time.Second},
		}, nil
	case providerDeepL:
		return &deeplProvider{
			apiKey:     cfg.DeepLAPIKey,
			endpoint:   cfg.DeepLEndpoint,
			httpClient: &http.Client{Timeout: 30 * time.Second},
		}, nil
	default:
		return nil, fmt.Errorf("unsupported provider %q", cfg.Provider)
	}
}

type mockProvider struct{}

func (mockProvider) Name() string  { return providerMock }
func (mockProvider) Model() string { return "" }

func (mockProvider) Translate(_ context.Context, request translateRequest) (string, error) {
	return "【测试翻译】\n\n" + request.Text, nil
}

type llmProvider struct {
	baseURL    string
	model      string
	apiKey     string
	keyHeader  string
	httpClient *http.Client
}

func (p *llmProvider) Name() string  { return providerLLM }
func (p *llmProvider) Model() string { return p.model }

func (p *llmProvider) Translate(ctx context.Context, request translateRequest) (string, error) {
	payload := map[string]any{
		"model": p.model,
		"messages": []map[string]string{
			{
				"role":    "system",
				"content": "You translate Go documentation returned by gopls. Translate to the requested target language. Keep Markdown, code blocks, links, identifiers, function names, type names, parameter names, and import paths unchanged. Return only the translated Markdown, with no commentary.",
			},
			{
				"role":    "user",
				"content": "Target language: " + request.TargetLanguage + "\\n\\n" + request.Text,
			},
		},
		"temperature": 0,
	}
	body, _ := json.Marshal(payload)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, p.baseURL+"/chat/completions", bytes.NewReader(body))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/json")
	if p.apiKey != "" {
		if strings.EqualFold(p.keyHeader, "Authorization") {
			req.Header.Set("Authorization", "Bearer "+p.apiKey)
		} else {
			req.Header.Set(p.keyHeader, p.apiKey)
		}
	}

	var decoded struct {
		Choices []struct {
			Message struct {
				Content string `json:"content"`
			} `json:"message"`
		} `json:"choices"`
		Error *struct {
			Message string `json:"message"`
		} `json:"error"`
	}
	if err := doJSON(p.httpClient, req, &decoded); err != nil {
		return "", err
	}
	if decoded.Error != nil {
		return "", fmt.Errorf("llm provider: %s", decoded.Error.Message)
	}
	if len(decoded.Choices) == 0 || strings.TrimSpace(decoded.Choices[0].Message.Content) == "" {
		return "", fmt.Errorf("llm provider returned no content")
	}
	return strings.TrimSpace(decoded.Choices[0].Message.Content), nil
}

type googleProvider struct {
	apiKey     string
	httpClient *http.Client
}

func (p *googleProvider) Name() string  { return providerGoogle }
func (p *googleProvider) Model() string { return "v2" }

func (p *googleProvider) Translate(ctx context.Context, request translateRequest) (string, error) {
	payload := map[string]string{
		"q":      request.Text,
		"target": request.TargetLanguage,
		"format": "html",
	}
	if request.SourceLanguage != "" && request.SourceLanguage != "auto" {
		payload["source"] = request.SourceLanguage
	}
	body, _ := json.Marshal(payload)
	endpoint := "https://translation.googleapis.com/language/translate/v2?key=" + url.QueryEscape(p.apiKey)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, endpoint, bytes.NewReader(body))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/json")

	var decoded struct {
		Data struct {
			Translations []struct {
				TranslatedText string `json:"translatedText"`
			} `json:"translations"`
		} `json:"data"`
		Error *struct {
			Message string `json:"message"`
		} `json:"error"`
	}
	if err := doJSON(p.httpClient, req, &decoded); err != nil {
		return "", err
	}
	if decoded.Error != nil {
		return "", fmt.Errorf("google provider: %s", decoded.Error.Message)
	}
	if len(decoded.Data.Translations) == 0 {
		return "", fmt.Errorf("google provider returned no translations")
	}
	return decoded.Data.Translations[0].TranslatedText, nil
}

type deeplProvider struct {
	apiKey     string
	endpoint   string
	httpClient *http.Client
}

func (p *deeplProvider) Name() string  { return providerDeepL }
func (p *deeplProvider) Model() string { return "v2" }

func (p *deeplProvider) Translate(ctx context.Context, request translateRequest) (string, error) {
	form := url.Values{}
	form.Set("text", request.Text)
	form.Set("target_lang", deeplLanguage(request.TargetLanguage))
	if len(request.SourceLanguage) >= 2 && request.SourceLanguage != "auto" {
		form.Set("source_lang", strings.ToUpper(request.SourceLanguage[:2]))
	}
	form.Set("preserve_formatting", "1")

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, p.endpoint, strings.NewReader(form.Encode()))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	req.Header.Set("Authorization", "DeepL-Auth-Key "+p.apiKey)

	var decoded struct {
		Translations []struct {
			Text string `json:"text"`
		} `json:"translations"`
		Message string `json:"message"`
	}
	if err := doJSON(p.httpClient, req, &decoded); err != nil {
		return "", err
	}
	if decoded.Message != "" {
		return "", fmt.Errorf("deepl provider: %s", decoded.Message)
	}
	if len(decoded.Translations) == 0 {
		return "", fmt.Errorf("deepl provider returned no translations")
	}
	return decoded.Translations[0].Text, nil
}

func deeplLanguage(language string) string {
	upper := strings.ToUpper(language)
	if strings.HasPrefix(upper, "ZH") {
		return "ZH"
	}
	if len(upper) >= 2 {
		return upper[:2]
	}
	return upper
}

func doJSON(client *http.Client, request *http.Request, output any) error {
	response, err := client.Do(request)
	if err != nil {
		return err
	}
	defer response.Body.Close()
	body, err := io.ReadAll(io.LimitReader(response.Body, 2<<20))
	if err != nil {
		return err
	}
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		message := strings.TrimSpace(string(body))
		if len(message) > 512 {
			message = message[:512]
		}
		if message == "" {
			message = response.Status
		}
		return fmt.Errorf("%s request failed: %s", request.URL.Host, message)
	}
	if err := json.Unmarshal(body, output); err != nil {
		return fmt.Errorf("decode provider response: %w", err)
	}
	return nil
}
