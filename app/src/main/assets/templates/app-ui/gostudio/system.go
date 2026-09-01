package gostudio

import (
	"encoding/json"
	"fmt"
	"time"
)

// DeviceInfo 描述当前设备的基础信息。
type DeviceInfo struct {
	Manufacturer string  `json:"manufacturer"`
	Model        string  `json:"model"`
	Android      string  `json:"android"`
	SDK          int     `json:"sdk"`
	PackageName  string  `json:"package_name"`
	VersionName  string  `json:"version_name"`
	Width        int     `json:"width"`
	Height       int     `json:"height"`
	Density      float64 `json:"density"`
}

// Log 输出运行日志。
func (a *App) Log(args ...any) {
	a.send(message{Op: "log", Text: fmt.Sprint(args...)})
}

// Toast 显示系统提示。
func (a *App) Toast(text string, duration ...int) error {
	d := 0
	if len(duration) > 0 && duration[0] > 0 {
		d = 1
	}
	return a.call(message{Op: "system", Action: "toast", Text: text, Duration: d})
}

// Alert 显示只有一个确定按钮的系统弹窗。
func (a *App) Alert(title, message string) error { return a.Dialog(title, message, "确定") }

// Dialog 显示系统弹窗。
func (a *App) Dialog(title, content string, buttons ...string) error {
	labels := buttons
	if len(labels) == 0 {
		labels = []string{"确定"}
	}
	value, err := json.Marshal(labels)
	if err != nil {
		return err
	}
	return a.call(message{Op: "system", Action: "dialog", Title: title, Text: content, Value: value})
}

// OnDialog 注册弹窗按钮回调。
func (a *App) OnDialog(fn func(button string)) {
	a.On("", "dialog", func(e Event) { fn(e.Text) })
}

// Vibrate 振动。
func (a *App) Vibrate(duration int) error {
	return a.call(message{Op: "system", Action: "vibrate", Duration: duration})
}

// SetClipboard 写剪贴板。
func (a *App) SetClipboard(text string) error {
	return a.call(message{Op: "system", Action: "clipboard_set", Text: text})
}

// GetClipboard 读剪贴板。
func (a *App) GetClipboard() (string, error) {
	reply, err := a.callResult(message{Op: "system", Action: "clipboard_get"})
	return reply.Text, err
}

// OpenURL 打开浏览器。
func (a *App) OpenURL(url string) error {
	return a.call(message{Op: "system", Action: "open_url", Text: url})
}

// Share 调起系统分享。
func (a *App) Share(title, text string) error {
	return a.call(message{Op: "system", Action: "share", Title: title, Text: text})
}

// DeviceInfo 获取设备信息。
func (a *App) DeviceInfo() (DeviceInfo, error) {
	var info DeviceInfo
	reply, err := a.callResult(message{Op: "system", Action: "device_info"})
	if err != nil {
		return info, err
	}
	err = json.Unmarshal([]byte(reply.Text), &info)
	return info, err
}

// Quit 关闭当前运行界面。
func (a *App) Quit() error { return a.call(message{Op: "quit"}) }

// Sleep 便利方法，单位毫秒。
func Sleep(milliseconds int) { time.Sleep(time.Duration(milliseconds) * time.Millisecond) }
