package appsdk

import (
	"context"
	"net"
	"os"
	"strings"
	"time"
)

// 独立 APK 里 Go 进程由壳直接裸跑（无 proot），而 Android 没有
// /etc/resolv.conf 且根文件系统只读，纯 Go 解析器读不到配置时会退回
// 127.0.0.1:53（无任何监听），导致所有域名解析失败。
// 壳在启动时通过 GOSTUDIO_DNS 环境变量传入系统 DNS 服务器，
// 这里用它们接管全局解析器；未提供时（GoStudio 内预览等场景有
// proot 的 resolv.conf）保持 Go 默认行为。
func init() {
	var servers []string
	for _, item := range strings.Split(os.Getenv("GOSTUDIO_DNS"), ",") {
		item = strings.TrimSpace(item)
		if item == "" {
			continue
		}
		if ip := net.ParseIP(item); ip != nil {
			servers = append(servers, net.JoinHostPort(ip.String(), "53"))
		}
	}
	if len(servers) == 0 {
		return
	}
	net.DefaultResolver = &net.Resolver{
		PreferGo: true,
		Dial: func(ctx context.Context, network, _ string) (net.Conn, error) {
			var last_err error
			for _, server := range servers {
				conn, err := (&net.Dialer{Timeout: 3 * time.Second}).DialContext(ctx, network, server)
				if err == nil {
					return conn, nil
				}
				last_err = err
			}
			return nil, last_err
		},
	}
}
