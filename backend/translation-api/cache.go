package main

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"sync"
)

type cache struct {
	mu      sync.RWMutex
	entries map[string]string
	max     int
}

func newCache(max int) *cache {
	return &cache{entries: make(map[string]string), max: max}
}

func (c *cache) get(key string) (string, bool) {
	c.mu.RLock()
	value, ok := c.entries[key]
	c.mu.RUnlock()
	return value, ok
}

func (c *cache) set(key, value string) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if len(c.entries) >= c.max {
		for oldKey := range c.entries {
			delete(c.entries, oldKey)
			if len(c.entries) < c.max {
				break
			}
		}
	}
	c.entries[key] = value
}

func cacheKey(provider, model, sourceLanguage, targetLanguage, kind, text string) string {
	sum := sha256.Sum256([]byte(provider + "\x00" + model + "\x00" + sourceLanguage + "\x00" + targetLanguage + "\x00" + kind + "\x00" + text))
	return hex.EncodeToString(sum[:])
}

func errString(err error) string {
	if err == nil {
		return ""
	}
	return fmt.Sprint(err)
}
