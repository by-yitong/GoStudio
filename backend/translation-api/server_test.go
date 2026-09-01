package main

import (
	"bytes"
	"encoding/json"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"testing"
)

func newTestServer(t *testing.T) *httptest.Server {
	t.Helper()
	cfg := config{
		Provider:          providerMock,
		APIKeys:           []string{"test-key"},
		MaxTextBytes:      1024,
		MaxCacheEntries:   16,
		MaxConcurrentJobs: 2,
	}
	return httptest.NewServer(newServer(cfg, mockProvider{}, slog.New(slog.NewTextHandler(io.Discard, nil))).Handler)
}

func TestTranslateRequiresApiKey(t *testing.T) {
	server := newTestServer(t)
	defer server.Close()

	response, err := http.Post(server.URL+"/v1/translate", "application/json", bytes.NewBufferString(`{"text":"Add returns the sum.","target_language":"zh-cn"}`))
	if err != nil {
		t.Fatal(err)
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusUnauthorized {
		t.Fatalf("status = %d, want %d", response.StatusCode, http.StatusUnauthorized)
	}
}

func TestTranslateAndCache(t *testing.T) {
	server := newTestServer(t)
	defer server.Close()

	request, _ := json.Marshal(translateRequest{
		Text:           "Add returns the sum.",
		TargetLanguage: "zh-cn",
		Kind:           "gopls-hover",
	})

	call := func() translateResponse {
		t.Helper()
		req, err := http.NewRequest(http.MethodPost, server.URL+"/v1/translate", bytes.NewReader(request))
		if err != nil {
			t.Fatal(err)
		}
		req.Header.Set("Content-Type", "application/json")
		req.Header.Set("X-GoStudio-Translation-Key", "test-key")
		response, err := http.DefaultClient.Do(req)
		if err != nil {
			t.Fatal(err)
		}
		defer response.Body.Close()
		if response.StatusCode != http.StatusOK {
			t.Fatalf("status = %d", response.StatusCode)
		}
		var decoded translateResponse
		if err := json.NewDecoder(response.Body).Decode(&decoded); err != nil {
			t.Fatal(err)
		}
		return decoded
	}

	first := call()
	if first.Cached {
		t.Fatal("first response should not be cached")
	}
	second := call()
	if !second.Cached || second.TranslatedText != first.TranslatedText {
		t.Fatalf("second response should use cache: first=%q second=%q", first.TranslatedText, second.TranslatedText)
	}
}

func TestHealth(t *testing.T) {
	server := newTestServer(t)
	defer server.Close()

	response, err := http.Get(server.URL + "/healthz")
	if err != nil {
		t.Fatal(err)
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		t.Fatalf("status = %d", response.StatusCode)
	}
}
