package main

import (
	"fmt"
	"os"
	"strings"
)

const (
	providerMock   = "mock"
	providerLLM    = "openai"
	providerGoogle = "google"
	providerDeepL  = "deepl"
)

type config struct {
	Addr              string
	Provider          string
	APIKeys           []string
	LLMBaseURL        string
	LLMModel          string
	LLMAPIKey         string
	LLMAPIKeyHeader   string
	GoogleAPIKey      string
	DeepLAPIKey       string
	DeepLEndpoint     string
	MaxTextBytes      int
	MaxCacheEntries   int
	MaxConcurrentJobs int
}

func loadConfig() (config, error) {
	cfg := config{
		Addr:              getenv("TRANSLATION_BACKEND_ADDR", ":8080"),
		Provider:          strings.ToLower(strings.TrimSpace(getenv("TRANSLATION_PROVIDER", providerLLM))),
		LLMBaseURL:        strings.TrimRight(strings.TrimSpace(getenv("LLM_BASE_URL", "https://api.openai.com/v1")), "/"),
		LLMModel:          getenv("LLM_MODEL", "gpt-4o-mini"),
		LLMAPIKeyHeader:   getenv("LLM_API_KEY_HEADER", "Authorization"),
		GoogleAPIKey:      os.Getenv("GOOGLE_TRANSLATE_API_KEY"),
		DeepLAPIKey:       os.Getenv("DEEPL_API_KEY"),
		MaxTextBytes:      64 * 1024,
		MaxCacheEntries:   20_000,
		MaxConcurrentJobs: 8,
	}
	cfg.LLMAPIKey = os.Getenv("LLM_API_KEY")
	if cfg.Provider == providerDeepL {
		cfg.DeepLEndpoint = getenv("DEEPL_API_ENDPOINT", "https://api-free.deepl.com/v2/translate")
	}

	for _, value := range strings.Split(os.Getenv("TRANSLATION_BACKEND_API_KEYS"), ",") {
		if key := strings.TrimSpace(value); key != "" {
			cfg.APIKeys = append(cfg.APIKeys, key)
		}
	}

	switch cfg.Provider {
	case providerMock:
		return cfg, nil
	case providerLLM:
		if cfg.LLMAPIKey == "" {
			return cfg, fmt.Errorf("TRANSLATION_PROVIDER=openai requires LLM_API_KEY")
		}
		if !strings.Contains(cfg.LLMBaseURL, "://") {
			return cfg, fmt.Errorf("LLM_BASE_URL must be an absolute URL")
		}
	case providerGoogle:
		if cfg.GoogleAPIKey == "" {
			return cfg, fmt.Errorf("TRANSLATION_PROVIDER=google requires GOOGLE_TRANSLATE_API_KEY")
		}
	case providerDeepL:
		if cfg.DeepLAPIKey == "" {
			return cfg, fmt.Errorf("TRANSLATION_PROVIDER=deepl requires DEEPL_API_KEY")
		}
	default:
		return cfg, fmt.Errorf("unsupported TRANSLATION_PROVIDER %q (mock, openai, google, deepl)", cfg.Provider)
	}

	if cfg.MaxTextBytes < 256 || cfg.MaxCacheEntries < 16 || cfg.MaxConcurrentJobs < 1 {
		return cfg, fmt.Errorf("invalid backend capacity configuration")
	}
	return cfg, nil
}

func getenv(name, fallback string) string {
	if value := strings.TrimSpace(os.Getenv(name)); value != "" {
		return value
	}
	return fallback
}
