package main

import (
	"context"
	"crypto/subtle"
	"encoding/json"
	"io"
	"log/slog"
	"net/http"
	"strings"
	"time"
)

type translateRequest struct {
	Text           string `json:"text"`
	SourceLanguage string `json:"source_language"`
	TargetLanguage string `json:"target_language"`
	Kind           string `json:"kind"`
}

type translateResponse struct {
	TranslatedText string `json:"translated_text"`
	Provider       string `json:"provider"`
	Model          string `json:"model"`
	Cached         bool   `json:"cached"`
}

type errorResponse struct {
	Error string `json:"error"`
}

type healthResponse struct {
	Status   string `json:"status"`
	Provider string `json:"provider"`
}

type server struct {
	cfg       config
	provider  translationProvider
	cache     *cache
	logger    *slog.Logger
	semaphore chan struct{}
}

func newServer(cfg config, provider translationProvider, logger *slog.Logger) *http.Server {
	backend := &server{
		cfg:       cfg,
		provider:  provider,
		cache:     newCache(cfg.MaxCacheEntries),
		logger:    logger,
		semaphore: make(chan struct{}, cfg.MaxConcurrentJobs),
	}
	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", backend.health)
	mux.HandleFunc("POST /v1/translate", backend.translate)
	mux.HandleFunc("POST /v1/translations", backend.translate)
	return &http.Server{
		Addr:              cfg.Addr,
		Handler:           backend.withCommonHeaders(mux),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       15 * time.Second,
		WriteTimeout:      60 * time.Second,
		IdleTimeout:       120 * time.Second,
	}
}

func (s *server) withCommonHeaders(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("Referrer-Policy", "no-referrer")
		next.ServeHTTP(w, r)
	})
}

func (s *server) health(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, healthResponse{Status: "ok", Provider: s.provider.Name()})
}

func (s *server) translate(w http.ResponseWriter, r *http.Request) {
	if !s.authorized(r) {
		writeJSON(w, http.StatusUnauthorized, errorResponse{Error: "invalid or missing backend API key"})
		return
	}

	var request translateRequest
	decoder := json.NewDecoder(io.LimitReader(r.Body, int64(s.cfg.MaxTextBytes+16*1024)))
	if err := decoder.Decode(&request); err != nil {
		writeJSON(w, http.StatusBadRequest, errorResponse{Error: "invalid JSON body"})
		return
	}
	request.Text = strings.TrimSpace(request.Text)
	request.SourceLanguage = strings.ToLower(strings.TrimSpace(request.SourceLanguage))
	request.TargetLanguage = strings.ToLower(strings.TrimSpace(request.TargetLanguage))
	request.Kind = strings.TrimSpace(request.Kind)

	if request.Text == "" {
		writeJSON(w, http.StatusBadRequest, errorResponse{Error: "text is required"})
		return
	}
	if len(request.Text) > s.cfg.MaxTextBytes {
		writeJSON(w, http.StatusRequestEntityTooLarge, errorResponse{Error: "text is too large"})
		return
	}
	if request.TargetLanguage == "" {
		request.TargetLanguage = "zh-cn"
	}
	if request.Kind == "" {
		request.Kind = "documentation"
	}
	if len(request.Kind) > 64 || !validLanguage(request.SourceLanguage) || !validLanguage(request.TargetLanguage) {
		writeJSON(w, http.StatusBadRequest, errorResponse{Error: "invalid source_language, target_language, or kind"})
		return
	}

	key := cacheKey(s.provider.Name(), s.provider.Model(), request.SourceLanguage, request.TargetLanguage, request.Kind, request.Text)
	if translated, ok := s.cache.get(key); ok {
		writeJSON(w, http.StatusOK, translateResponse{
			TranslatedText: translated,
			Provider:       s.provider.Name(),
			Model:          s.provider.Model(),
			Cached:         true,
		})
		return
	}

	select {
	case s.semaphore <- struct{}{}:
		defer func() { <-s.semaphore }()
	default:
		writeJSON(w, http.StatusTooManyRequests, errorResponse{Error: "backend is busy"})
		return
	}

	ctx, cancel := contextWithClientTimeout(r.Context(), 50*time.Second)
	defer cancel()
	translated, err := s.provider.Translate(ctx, request)
	if err != nil {
		if ctx.Err() != nil {
			writeJSON(w, http.StatusGatewayTimeout, errorResponse{Error: "translation provider timed out"})
			return
		}
		s.logger.Warn("translation failed", "provider", s.provider.Name(), "kind", request.Kind, "error", errString(err))
		writeJSON(w, http.StatusBadGateway, errorResponse{Error: "translation provider failed"})
		return
	}
	s.cache.set(key, translated)
	writeJSON(w, http.StatusOK, translateResponse{
		TranslatedText: translated,
		Provider:       s.provider.Name(),
		Model:          s.provider.Model(),
		Cached:         false,
	})
}

func (s *server) authorized(r *http.Request) bool {
	if len(s.cfg.APIKeys) == 0 {
		return true
	}
	key := strings.TrimSpace(r.Header.Get("X-GoStudio-Translation-Key"))
	for _, expected := range s.cfg.APIKeys {
		if key != "" && subtle.ConstantTimeCompare([]byte(key), []byte(expected)) == 1 {
			return true
		}
	}
	return false
}

func validLanguage(language string) bool {
	if language == "" || language == "auto" {
		return true
	}
	if len(language) < 2 || len(language) > 20 {
		return false
	}
	for _, char := range language {
		if !(char == '-' || char == '_' || (char >= 'a' && char <= 'z') || (char >= '0' && char <= '9')) {
			return false
		}
	}
	return true
}

func contextWithClientTimeout(parent context.Context, timeout time.Duration) (context.Context, context.CancelFunc) {
	if deadline, ok := parent.Deadline(); ok && time.Until(deadline) < timeout {
		return context.WithDeadline(parent, deadline)
	}
	return context.WithTimeout(parent, timeout)
}

func writeJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(value)
}
