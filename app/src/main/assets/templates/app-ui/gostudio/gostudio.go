// Package gostudio 是 GoStudio「App 运行」模式下的 UI 桥接 SDK。
//
// 布局由 GoStudio 宿主根据 layout.xml 渲染成原生界面；你的 Go 程序负责逻辑，
// 双方通过 stdin/stdout 的 JSON 行协议通信：
//
//	app := gostudio.Start()
//	app.OnClick("btn", func() { app.SetText("tv", "你好") })
//	app.Run() // 阻塞处理事件，直到界面关闭
//
// 注意：协议占用标准输出，业务日志请使用 app.Log()，不要直接 fmt.Println 到 stdout。
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
	Op   string `json:"op"`
	Seq  int64  `json:"seq,omitempty"`
	Vid  string `json:"vid,omitempty"`
	Text string `json:"text,omitempty"`
	Ok   bool   `json:"ok,omitempty"`
}

// App 是与宿主界面通信的运行时句柄。
type App struct {
	writeMu  sync.Mutex
	seqMu    sync.Mutex
	callMu   sync.Mutex
	seq      int64
	pending  map[int64]chan message
	handlerMu sync.Mutex
	handlers map[string]func()
	done     chan struct{}
}

// Start 初始化桥接并启动事件读取循环，必须在其他调用之前执行一次。
func Start() *App {
	app := &App{
		pending:  make(map[int64]chan message),
		handlers: make(map[string]func()),
		done:     make(chan struct{}),
	}
	go app.read_loop()
	return app
}

// OnClick 注册控件点击事件，id 对应 layout.xml 里的 id 属性。
func (a *App) OnClick(id string, fn func()) {
	a.handlerMu.Lock()
	a.handlers[id] = fn
	a.handlerMu.Unlock()
}

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

// Log 在宿主界面的日志区输出一行信息。
func (a *App) Log(args ...any) {
	a.send(message{Op: "log", Text: fmt.Sprint(args...)})
}

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
		case "click":
			a.dispatch_click(msg.Vid)
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

func (a *App) dispatch_click(id string) {
	a.handlerMu.Lock()
	fn := a.handlers[id]
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
		fn()
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
			return reply, errors.New("宿主操作失败: " + msg.Op)
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
