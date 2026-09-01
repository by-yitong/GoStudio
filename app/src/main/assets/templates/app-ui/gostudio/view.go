package appsdk

import (
	"encoding/json"
	"fmt"
	"strconv"
)

// Widget 是所有组件的公共句柄。layout.xml 中每个带 id 的组件都有对应工厂方法。
type Widget struct {
	app *App
	id  string
}

func (a *App) widget(id string) *Widget { return &Widget{a, id} }

// SetText 按 ID 设置文本，兼容 app.SetText("tv", "...") 写法。
func (a *App) SetText(id, text string) error { return a.widget(id).SetText(text) }

// GetText 按 ID 读取文本。
func (a *App) GetText(id string) (string, error) { return a.widget(id).GetText() }

// SetImage 按 ID 设置网络图片。
func (a *App) SetImage(id, url string) error { return a.widget(id).SetImage(url) }

// ID 返回组件 ID。
func (w *Widget) ID() string { return w.id }

// SetText 设置 TextView 系组件文本。
func (w *Widget) SetText(text string) error {
	return w.app.call(message{Op: "set_text", Vid: w.id, Text: text})
}

// GetText 读取 TextView 系组件文本。
func (w *Widget) GetText() (string, error) {
	reply, err := w.app.callResult(message{Op: "get_text", Vid: w.id})
	return reply.Text, err
}

// SetImage 设置网络图片。
func (w *Widget) SetImage(url string) error {
	return w.app.call(message{Op: "set_image", Vid: w.id, Text: url})
}

// SetVisibility 设置 visible / invisible / gone。
func (w *Widget) SetVisibility(visible bool) error {
	state := "visible"
	if !visible {
		state = "gone"
	}
	return w.setProperty("visibility", state)
}

// SetEnabled 设置组件是否可用。
func (w *Widget) SetEnabled(enabled bool) error { return w.setProperty("enabled", enabled) }

// SetSelected 设置组件选中态。
func (w *Widget) SetSelected(selected bool) error { return w.setProperty("selected", selected) }

// SetAlpha 设置透明度，0～1。
func (w *Widget) SetAlpha(alpha float64) error { return w.setProperty("alpha", alpha) }

// SetBackground 设置背景色，例如 #5CCFE6。
func (w *Widget) SetBackground(color string) error { return w.setProperty("background", color) }

// SetPadding 设置内边距，单位 dp。
func (w *Widget) SetPadding(left, top, right, bottom int) error {
	return w.app.call(message{
		Op: "invoke", Vid: w.id, Action: "set_padding",
		Value: mustJSON([]int{left, top, right, bottom}),
	})
}

func (w *Widget) setProperty(name string, value any) error {
	data, err := json.Marshal(value)
	if err != nil {
		return err
	}
	return w.app.call(message{Op: "set_property", Vid: w.id, Action: name, Value: data})
}

func (w *Widget) getProperty(name string) (string, error) {
	reply, err := w.app.callResult(message{Op: "get_property", Vid: w.id, Action: name})
	return reply.Text, err
}

func (w *Widget) invoke(action string, value any) error {
	var raw json.RawMessage
	if value != nil {
		data, err := json.Marshal(value)
		if err != nil {
			return err
		}
		raw = data
	}
	return w.app.call(message{Op: "invoke", Vid: w.id, Action: action, Value: raw})
}

func (w *Widget) boolProperty(name string) (bool, error) {
	text, err := w.getProperty(name)
	return err == nil && text == "true", err
}

func (w *Widget) numberProperty(name string) (float64, error) {
	text, err := w.getProperty(name)
	if err != nil {
		return 0, err
	}
	return strconv.ParseFloat(text, 64)
}

func (w *Widget) OnClick(fn func()) { w.app.OnClick(w.id, fn) }

func (w *Widget) OnLongClick(fn func()) { w.app.OnLongClick(w.id, fn) }

func mustJSON(value any) json.RawMessage {
	data, _ := json.Marshal(value)
	return data
}

func numberText(value float64) string {
	if value == float64(int(value)) {
		return fmt.Sprintf("%d", int(value))
	}
	return fmt.Sprintf("%v", value)
}
