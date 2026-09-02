package appsdk

import "encoding/json"

// FloatingWindowConfig 描述悬浮窗显示参数；尺寸和坐标单位均为 dp。
type FloatingWindowConfig struct {
	Layout    string `json:"layout,omitempty"`
	Title     string `json:"title,omitempty"`
	Text      string `json:"text,omitempty"`
	X         int    `json:"x,omitempty"`
	Y         int    `json:"y,omitempty"`
	Width     int    `json:"width,omitempty"`
	Height    int    `json:"height,omitempty"`
	Draggable bool   `json:"draggable"`
	Focusable bool   `json:"focusable"`
	ShowClose bool   `json:"show_close"`
}

// FloatingWindowOption 用于配置 ShowFloatingWindow。
type FloatingWindowOption func(*FloatingWindowConfig)

func floating_defaults(id string) FloatingWindowConfig {
	return FloatingWindowConfig{
		Title:     id,
		X:         24,
		Y:         64,
		Draggable: true,
		ShowClose: true,
	}
}

// FloatLayout 使用项目内 XML 布局作为悬浮窗内容，例如 floats/note.xml。
func FloatLayout(path string) FloatingWindowOption {
	return func(c *FloatingWindowConfig) { c.Layout = path }
}

// FloatTitle 设置悬浮窗标题。
func FloatTitle(title string) FloatingWindowOption {
	return func(c *FloatingWindowConfig) { c.Title = title }
}

// FloatText 设置默认文本悬浮窗内容；使用 XML 布局时忽略。
func FloatText(text string) FloatingWindowOption {
	return func(c *FloatingWindowConfig) { c.Text = text }
}

// FloatPosition 设置初始位置，单位 dp。
func FloatPosition(x, y int) FloatingWindowOption {
	return func(c *FloatingWindowConfig) { c.X, c.Y = x, y }
}

// FloatSize 设置窗口尺寸，单位 dp；0 表示自适应内容。
func FloatSize(width, height int) FloatingWindowOption {
	return func(c *FloatingWindowConfig) { c.Width, c.Height = width, height }
}

// FloatDraggable 设置标题栏是否可拖动。
func FloatDraggable(enabled bool) FloatingWindowOption {
	return func(c *FloatingWindowConfig) { c.Draggable = enabled }
}

// FloatFocusable 允许悬浮窗获取焦点和键盘输入。
func FloatFocusable(enabled bool) FloatingWindowOption {
	return func(c *FloatingWindowConfig) { c.Focusable = enabled }
}

// FloatCloseButton 设置是否显示右上角关闭按钮。
func FloatCloseButton(enabled bool) FloatingWindowOption {
	return func(c *FloatingWindowConfig) { c.ShowClose = enabled }
}

// CanFloatingWindow 判断是否已有系统悬浮窗权限。
func (a *App) CanFloatingWindow() (bool, error) {
	reply, err := a.callResult(message{Op: "system", Action: "float_can"})
	if err != nil {
		return false, err
	}
	return reply.Text == "true", nil
}

// RequestFloatingWindowPermission 打开系统悬浮窗授权页。
func (a *App) RequestFloatingWindowPermission() error {
	return a.call(message{Op: "system", Action: "float_request_permission"})
}

// OnFloatingWindowPermission 注册授权状态回调。
func (a *App) OnFloatingWindowPermission(fn func(granted bool)) {
	a.On("", "float_permission_change", func(e Event) { fn(e.Boolean) })
}

// ShowFloatingWindow 显示系统悬浮窗。id 用于后续移动、关闭和事件回调。
func (a *App) ShowFloatingWindow(id string, options ...FloatingWindowOption) error {
	config := floating_defaults(id)
	for _, option := range options {
		option(&config)
	}
	value, err := json.Marshal(config)
	if err != nil {
		return err
	}
	return a.call(message{
		Op:     "system",
		Action: "float_show",
		Vid:    id,
		Value:  value,
	})
}

// SetFloatingWindowText 更新默认文本悬浮窗内容。
func (a *App) SetFloatingWindowText(id, text string) error {
	return a.call(message{Op: "system", Action: "float_set_text", Vid: id, Text: text})
}

// MoveFloatingWindow 移动悬浮窗，坐标单位 dp。
func (a *App) MoveFloatingWindow(id string, x, y int) error {
	return a.call(message{Op: "system", Action: "float_move", Vid: id, X: x, Y: y})
}

// CloseFloatingWindow 关闭指定悬浮窗。
func (a *App) CloseFloatingWindow(id string) error {
	return a.call(message{Op: "system", Action: "float_close", Vid: id})
}

// OnFloatingWindowClick 注册默认文本悬浮窗点击事件。
func (a *App) OnFloatingWindowClick(id string, fn func(Event)) {
	a.On(id, "float_click", fn)
}

// OnFloatingWindowClose 注册悬浮窗关闭事件。
func (a *App) OnFloatingWindowClose(id string, fn func(Event)) {
	a.On(id, "float_close", fn)
}
