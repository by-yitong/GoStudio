// GoStudio APK 中转服务：解决 QQ 官方服务器拉不动 GitHub Release 资产的问题。
//
// 流程：CI 打包发布后 POST /mirror 告知 GitHub 资产地址 -> 服务端下载落盘 ->
// 返回本机公网 URL -> CI 把该 URL 交给 QQ 推送服务（URL 模式），QQ 从本服务拉取。
//
//	POST /mirror  {"url":"https://github.com/by-yitong/GoStudio/releases/download/..x.apk"}
//	              需鉴权（X-Relay-Token 头或 ?token=），仅放行 GoStudio 仓库的 .apk 资产
//	GET  /f/<文件名>  公开下载（支持 Range 断点，浏览器/QQ 直链可用）
//	GET  /healthz     健康检查
//
// 环境变量：RELAY_TOKEN（必填，mirror 鉴权）
// 参数：-addr 监听地址（默认 :8312） -data 存储目录 -public 对外基地址 -keep 保留文件数
package main

import (
	"crypto/subtle"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
	"time"
)

// 只中转 GoStudio 仓库的 Release 资产，防止被当成任意下载代理
const allowedPrefix = "https://github.com/by-yitong/GoStudio/releases/download/"

var fileNamePattern = regexp.MustCompile(`^[A-Za-z0-9._-]+\.apk$`)

const maxDownloadBytes = 200 << 20 // 与 QQ 官方富媒体上限对齐

func main() {
	addr := flag.String("addr", ":8312", "监听地址")
	dataDir := flag.String("data", "/var/lib/gostudio-relay", "APK 存储目录")
	publicBase := flag.String("public", "", "对外基地址，如 http://1.2.3.4:8312（mirror 响应里拼接用）")
	keep := flag.Int("keep", 5, "最多保留的 APK 个数，超出删最旧")
	flag.Parse()

	token := os.Getenv("RELAY_TOKEN")
	if token == "" {
		log.Fatal("RELAY_TOKEN 未设置")
	}
	if *publicBase == "" {
		log.Fatal("-public 未设置（mirror 返回的下载地址需要它）")
	}
	if err := os.MkdirAll(*dataDir, 0o755); err != nil {
		log.Fatalf("创建存储目录失败: %v", err)
	}

	client := &http.Client{Timeout: 15 * time.Minute}

	mux := http.NewServeMux()
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		fmt.Fprintln(w, "ok")
	})

	mux.HandleFunc("/mirror", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}
		got := r.Header.Get("X-Relay-Token")
		if got == "" {
			got = r.URL.Query().Get("token")
		}
		if subtle.ConstantTimeCompare([]byte(got), []byte(token)) != 1 {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		var req struct {
			URL  string `json:"url"`
			Name string `json:"name"` // 可选：落盘文件名（QQ 会显示 URL 里的名字）
		}
		if err := json.NewDecoder(io.LimitReader(r.Body, 4<<10)).Decode(&req); err != nil {
			http.Error(w, "bad json", http.StatusBadRequest)
			return
		}
		if !strings.HasPrefix(req.URL, allowedPrefix) {
			http.Error(w, "url not allowed", http.StatusForbidden)
			return
		}
		name := filepath.Base(req.URL)
		if req.Name != "" {
			name = req.Name
		}
		if !fileNamePattern.MatchString(name) {
			http.Error(w, "bad file name", http.StatusBadRequest)
			return
		}

		log.Printf("mirror 开始: %s", req.URL)
		dl, err := client.Get(req.URL)
		if err != nil {
			http.Error(w, "download failed: "+err.Error(), http.StatusBadGateway)
			return
		}
		defer dl.Body.Close()
		if dl.StatusCode != http.StatusOK {
			http.Error(w, fmt.Sprintf("github status %d", dl.StatusCode), http.StatusBadGateway)
			return
		}
		if dl.ContentLength > maxDownloadBytes {
			http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
			return
		}

		// 先写临时文件再改名，避免半截文件被对外提供
		tmp, err := os.CreateTemp(*dataDir, ".tmp-*")
		if err != nil {
			http.Error(w, "storage error", http.StatusInternalServerError)
			return
		}
		size, err := io.Copy(tmp, io.LimitReader(dl.Body, maxDownloadBytes+1))
		closeErr := tmp.Close()
		if err != nil || closeErr != nil {
			os.Remove(tmp.Name())
			http.Error(w, "save failed", http.StatusInternalServerError)
			return
		}
		if size > maxDownloadBytes {
			os.Remove(tmp.Name())
			http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
			return
		}
		target := filepath.Join(*dataDir, name)
		if err := os.Rename(tmp.Name(), target); err != nil {
			os.Remove(tmp.Name())
			http.Error(w, "save failed", http.StatusInternalServerError)
			return
		}
		log.Printf("mirror 完成: %s (%d bytes)", name, size)
		evictOld(*dataDir, *keep)

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]any{
			"url":  strings.TrimRight(*publicBase, "/") + "/f/" + name,
			"size": size,
		})
	})

	// 公开下载：QQ 官方服务器与用户浏览器都从这里拉
	mux.HandleFunc("/f/", func(w http.ResponseWriter, r *http.Request) {
		name := filepath.Base(strings.TrimPrefix(r.URL.Path, "/f/"))
		if !fileNamePattern.MatchString(name) {
			http.NotFound(w, r)
			return
		}
		path := filepath.Join(*dataDir, name)
		file, err := os.Open(path)
		if err != nil {
			http.NotFound(w, r)
			return
		}
		defer file.Close()
		w.Header().Set("Content-Type", "application/vnd.android.package-archive")
		w.Header().Set("Content-Disposition", `attachment; filename="`+name+`"`)
		http.ServeContent(w, r, name, time.Now(), file) // 自带 Range/断点支持
	})

	log.Printf("apk-relay 监听 %s，存储 %s，对外地址 %s", *addr, *dataDir, *publicBase)
	log.Fatal(http.ListenAndServe(*addr, mux))
}

// 只保留最新 keep 个 APK，磁盘不无限涨
func evictOld(dataDir string, keep int) {
	entries, err := os.ReadDir(dataDir)
	if err != nil {
		return
	}
	var files []string
	for _, e := range entries {
		if !e.IsDir() && fileNamePattern.MatchString(e.Name()) {
			files = append(files, e.Name())
		}
	}
	sort.Strings(files) // 文件名带版本号，字典序近似时间序
	for _, name := range files[:max(0, len(files)-keep)] {
		if os.Remove(filepath.Join(dataDir, name)) == nil {
			log.Printf("清理旧文件: %s", name)
		}
	}
}
