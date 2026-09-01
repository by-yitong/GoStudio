// Package gostudio 是 GoStudio「App 运行」模式下的 UI / 生命周期 / 系统桥接 SDK。
//
// 布局由 GoStudio 宿主根据 layout.xml 渲染成原生界面；Go 程序负责业务逻辑，
// 双方通过 stdin/stdout 的 JSON 行协议通信：
//
//	app := gostudio.Start()
//	app.Button("btn").OnClick(func() { app.Toast("你好") })
//	app.OnResume(func() { app.Log("onResume") })
//	app.Run()
//
// fmt.Println 等普通 stdout 输出会被宿主识别为运行日志；协议消息仍使用 stdout 传输。
package gostudio

import (
	"bufio"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"sync"
	"time"
)

const call_timeout = 30 * time.Second

type message struct {
	Op       string          `json:"op"`
	Seq      int64           `json:"seq,omitempty"`
	Vid      string          `json:"vid,omitempty"`
	Event    string          `json:"event,omitempty"`
	Action   string          `json:"action,omitempty"`
	Text     string          `json:"text,omitempty"`
	Title    string          `json:"title,omitempty"`
	Number   float64         `json:"number,omitempty"`
	Boolean  bool            `json:"boolean,omitempty"`
	Duration int             `json:"duration,omitempty"`
	Value    json.RawMessage `json:"value,omitempty"`
	Ok       bool            `json:"ok,omitempty"`
}

// Event 是宿主转发给 Go 的事件。不同事件只使用对应字段。
type Event struct {
	ID      string
	Name    string
	Text    string
	Number  float64
	Boolean bool
}

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

// App 是与宿主界面通信的运行时句柄。
type App struct {
	writeMu   sync.Mutex
	seqMu     sync.Mutex
	callMu    sync.Mutex
	seq       int64
	pending   map[int64]chan message
	handlerMu sync.Mutex
	handlers  map[string]map[string]func(Event)
	done      chan struct{}
}

// Start 初始化桥接并启动事件读取循环，必须在其他调用之前执行一次。
func Start() *App {
	app := &App{
		pending:  make(map[int64]chan message),
		handlers: make(map[string]map[string]func(Event)),
		done:     make(chan struct{}),
	}
	go app.read_loop()
	return app
}

// On 注册任意事件。事件名可用 click、long_click、checked_change、text_change、
// progress_change、date_change、time_change 以及 create/start/resume/pause/stop/destroy。
func (a *App) On(id, event string, fn func(Event)) {
	a.handlerMu.Lock()
	if a.handlers[id] == nil {
		a.handlers[id] = make(map[string]func(Event))
	}
	a.handlers[id][event] = fn
	a.handlerMu.Unlock()
}

// OnClick 注册点击事件，id 对应 layout.xml 的 id 属性。
func (a *App) OnClick(id string, fn func()) {
	a.On(id, "click", func(Event) { fn() })
}

// OnLongClick 注册长按事件。
func (a *App) OnLongClick(id string, fn func()) {
	a.On(id, "long_click", func(Event) { fn() })
}

// OnCheckedChange 注册 CheckBox / RadioButton / Switch 的选中变化事件。
func (a *App) OnCheckedChange(id string, fn func(checked bool)) {
	a.On(id, "checked_change", func(e Event) { fn(e.Boolean) })
}

// OnTextChanged 注册 TextView / EditText 文本变化事件。
func (a *App) OnTextChanged(id string, fn func(text string)) {
	a.On(id, "text_change", func(e Event) { fn(e.Text) })
}

// OnProgressChange 注册 SeekBar / ProgressBar 变化事件。
func (a *App) OnProgressChange(id string, fn func(progress int)) {
	a.On(id, "progress_change", func(e Event) { fn(int(e.Number)) })
}

// OnRatingChange 注册 RatingBar 评分变化事件。
func (a *App) OnRatingChange(id string, fn func(rating float64)) {
	a.On(id, "rating_change", func(e Event) { fn(e.Number) })
}

// OnDateChange 注册 DatePicker 日期变化事件，格式 yyyy-MM-dd。
func (a *App) OnDateChange(id string, fn func(date string)) {
	a.On(id, "date_change", func(e Event) { fn(e.Text) })
}

// OnTimeChange 注册 TimePicker 时间变化事件，格式 HH:mm。
func (a *App) OnTimeChange(id string, fn func(time string)) {
	a.On(id, "time_change", func(e Event) { fn(e.Text) })
}

// OnCreate 注册 App 生命周期 create。
func (a *App) OnCreate(fn func()) { a.On("", "create", func(Event) { fn() }) }

// OnStart 注册 App 生命周期 start。
func (a *App) OnStart(fn func()) { a.On("", "start", func(Event) { fn() }) }

// OnResume 注册 App 生命周期 resume。
func (a *App) OnResume(fn func()) { a.On("", "resume", func(Event) { fn() }) }

// OnPause 注册 App 生命周期 pause。
func (a *App) OnPause(fn func()) { a.On("", "pause", func(Event) { fn() }) }

// OnStop 注册 App 生命周期 stop。
func (a *App) OnStop(fn func()) { a.On("", "stop", func(Event) { fn() }) }

// OnDestroy 注册 App 生命周期 destroy。
func (a *App) OnDestroy(fn func()) { a.On("", "destroy", func(Event) { fn() }) }

// SetText 设置控件文本。
func (a *App) SetText(id, text string) error {
	return a.call(message{Op: "set_text", Vid: id, Text: text})
}

// GetText 读取控件文本（例如用户在输入框里填写的内容）。
func (a *App) GetText(id string) (string, error) {
	reply, err := a.call_result(message{Op: "get_text", Vid: id})
	if err != nil {
		return "", err
	}
	return reply.Text, nil
}

// SetImage 把 ImageView / ImageButton 的图片替换为网络 URL。
func (a *App) SetImage(id, url string) error {
	return a.call(message{Op: "set_image", Vid: id, Text: url})
}

// Log 在宿主界面的日志区输出一行信息。
func (a *App) Log(args ...any) {
	a.send(message{Op: "log", Text: fmt.Sprint(args...)})
}


// Alert 显示只有一个“确定”按钮的系统弹窗。
func (a *App) Alert(title, message string) error {
	return a.Dialog(title, message, "确定")
}

// Dialog 显示系统弹窗，buttons 是自定义按钮文本。
func (a *App) Dialog(title, content string, buttons ...string) error {
	labels := buttons
	if len(labels) == 0 {
		labels = []string{"确定"}
	}
	value, err := json.Marshal(labels)
	if err != nil {
		return err
	}
	return a.call(message{
		Op: "system", Action: "dialog", Title: title,
		Text: content, Value: value,
	})
}

// OnDialog 注册弹窗按钮回调，button 是被点击按钮的文本。
func (a *App) OnDialog(fn func(button string)) {
	a.On("", "dialog", func(e Event) { fn(e.Text) })
}

// Toast 显示系统短 Toast；duration 为 0 短 Toast，为 1 长 Toast。
func (a *App) Toast(text string, duration ...int) error {
	d := 0
	if len(duration) > 0 && duration[0] > 0 {
		d = 1
	}
	return a.call(message{Op: "system", Action: "toast", Text: text, Duration: d})
}

// Vibrate 振动，duration 为毫秒。
func (a *App) Vibrate(duration int) error {
	return a.call(message{Op: "system", Action: "vibrate", Duration: duration})
}

// SetClipboard 写入系统剪贴板。
func (a *App) SetClipboard(text string) error {
	return a.call(message{Op: "system", Action: "clipboard_set", Text: text})
}

// GetClipboard 读取系统剪贴板。
func (a *App) GetClipboard() (string, error) {
	reply, err := a.call_result(message{Op: "system", Action: "clipboard_get"})
	if err != nil {
		return "", err
	}
	return reply.Text, nil
}

// OpenURL 使用系统浏览器打开链接。
func (a *App) OpenURL(url string) error {
	return a.call(message{Op: "system", Action: "open_url", Text: url})
}

// Share 调起系统分享。
func (a *App) Share(title, text string) error {
	return a.call(message{Op: "system", Action: "share", Title: title, Text: text})
}

// DeviceInfo 获取设备与屏幕信息。
func (a *App) DeviceInfo() (DeviceInfo, error) {
	var info DeviceInfo
	reply, err := a.call_result(message{Op: "system", Action: "device_info"})
	if err != nil {
		return info, err
	}
	err = json.Unmarshal([]byte(reply.Text), &info)
	return info, err
}

// View 是布局中控件的句柄，对应 layout.xml 里的 id 属性。
type View struct {
	app *App
	id  string
}

// Button 返回按钮句柄。
func (a *App) Button(id string) *Button { return &Button{View{a, id}} }

// Image 返回图片控件句柄。
func (a *App) Image(id string) *Image { return &Image{View{a, id}} }

// Text 返回文本控件句柄（TextView / EditText 通用）。
func (a *App) Text(id string) *Text { return &Text{View{a, id}} }

// Button 按钮。
type Button struct{ View }

// Image 图片控件。
type Image struct{ View }

// Text 文本控件。
type Text struct{ View }

// OnClick 注册点击事件。
func (v *View) OnClick(fn func()) { v.app.OnClick(v.id, fn) }

// OnLongClick 注册长按事件。
func (v *View) OnLongClick(fn func()) { v.app.OnLongClick(v.id, fn) }

// SetText 设置控件文本。
func (v *View) SetText(text string) error { return v.app.SetText(v.id, text) }

// GetText 读取控件文本。
func (v *View) GetText() (string, error) { return v.app.GetText(v.id) }

// SetImage 设置网络图片，支持 http/https URL。
func (v *Image) SetImage(url string) error { return v.app.SetImage(v.id, url) }

// SetSrc 是 SetImage 的别名，语义对应 layout.xml 的 src 属性。
func (v *Image) SetSrc(url string) error { return v.SetImage(url) }

// Quit 请求宿主关闭当前运行界面。
func (a *App) Quit() error {
	return a.call(message{Op: "quit"})
}

// Run 阻塞处理宿主事件，直到界面被关闭或标准输入结束。
func (a *App) Run() {
	<-a.done
}

func (a *App) read_loop() {
	defer close(a.done)
	scanner := bufio.NewScanner(os.Stdin)
	scanner.Buffer(make([]byte, 0, 64*1024), 1024*1024)
	for scanner.Scan() {
		var msg message
		if err := json.Unmarshal(scanner.Bytes(), &msg); err != nil {
			continue
		}
		switch msg.Op {
		case "ack":
			a.dispatch_ack(msg)
		case "event":
			a.dispatch_event(msg.Vid, msg.Event, Event{
				ID: msg.Vid, Name: msg.Event, Text: msg.Text,
				Number: msg.Number, Boolean: msg.Boolean,
			})
		case "close":
			return
		}
	}
}

func (a *App) dispatch_ack(msg message) {
	a.callMu.Lock()
	ch := a.pending[msg.Seq]
	delete(a.pending, msg.Seq)
	a.callMu.Unlock()
	if ch != nil {
		ch <- msg
	}
}

func (a *App) dispatch_event(id, event string, e Event) {
	a.handlerMu.Lock()
	fn := a.handlers[id][event]
	a.handlerMu.Unlock()
	if fn == nil {
		return
	}
	go func() {
		defer func() {
			if r := recover(); r != nil {
				a.Log("panic: ", r)
			}
		}()
		fn(e)
	}()
}

func (a *App) call(msg message) error {
	_, err := a.call_result(msg)
	return err
}

func (a *App) call_result(msg message) (message, error) {
	a.seqMu.Lock()
	a.seq++
	msg.Seq = a.seq
	a.seqMu.Unlock()

	ch := make(chan message, 1)
	a.callMu.Lock()
	a.pending[msg.Seq] = ch
	a.callMu.Unlock()

	if err := a.send(msg); err != nil {
		a.callMu.Lock()
		delete(a.pending, msg.Seq)
		a.callMu.Unlock()
		return message{}, err
	}

	select {
	case reply := <-ch:
		if !reply.Ok {
			detail := msg.Op
			if msg.Action != "" {
				detail += ":" + msg.Action
			}
			return reply, errors.New("宿主操作失败: " + detail)
		}
		return reply, nil
	case <-time.After(call_timeout):
		a.callMu.Lock()
		delete(a.pending, msg.Seq)
		a.callMu.Unlock()
		return message{}, errors.New("宿主响应超时: " + msg.Op)
	}
}

func (a *App) send(msg message) error {
	a.writeMu.Lock()
	defer a.writeMu.Unlock()
	data, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	data = append(data, '\n')
	_, err = os.Stdout.Write(data)
	return err
}
