// Package appsdk 是 GoStudio「App 运行」模式下的 UI / 生命周期 / 系统桥接 SDK。
//
// 布局由 GoStudio 宿主根据 layout.xml 渲染成原生界面；Go 程序负责业务逻辑，
// 双方通过 stdin/stdout 的 JSON 行协议通信。
package appsdk

import (
	"bufio"
	"encoding/json"
	"errors"
	"os"
	"sync"
	"time"
)

const callTimeout = 30 * time.Second

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

// Event 是宿主转发给 Go 的事件。
type Event struct {
	ID      string
	Name    string
	Text    string
	Number  float64
	Boolean bool
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

// Start 初始化桥接并启动事件读取循环。
func Start() *App {
	app := &App{
		pending:  make(map[int64]chan message),
		handlers: make(map[string]map[string]func(Event)),
		done:     make(chan struct{}),
	}
	go app.readLoop()
	return app
}

// On 注册任意事件。
func (a *App) On(id, event string, fn func(Event)) {
	a.handlerMu.Lock()
	if a.handlers[id] == nil {
		a.handlers[id] = make(map[string]func(Event))
	}
	a.handlers[id][event] = fn
	a.handlerMu.Unlock()
}

// Run 阻塞处理宿主事件，直到界面被关闭或标准输入结束。
func (a *App) Run() { <-a.done }

func (a *App) readLoop() {
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
			a.dispatchAck(msg)
		case "event":
			a.dispatchEvent(msg.Vid, msg.Event, Event{
				ID: msg.Vid, Name: msg.Event, Text: msg.Text,
				Number: msg.Number, Boolean: msg.Boolean,
			})
		case "close":
			return
		}
	}
}

func (a *App) dispatchAck(msg message) {
	a.callMu.Lock()
	ch := a.pending[msg.Seq]
	delete(a.pending, msg.Seq)
	a.callMu.Unlock()
	if ch != nil {
		ch <- msg
	}
}

func (a *App) dispatchEvent(id, event string, e Event) {
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
	_, err := a.callResult(msg)
	return err
}

func (a *App) callResult(msg message) (message, error) {
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
	case <-time.After(callTimeout):
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
