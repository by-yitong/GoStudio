package gostudio

// TextView 文本、输入、自动补成、计时器、时钟等文本类组件句柄。
type TextView struct{ Widget }

// EditText 输入框。
type EditText struct{ TextView }

// AutoCompleteTextView 自动补全输入框。
type AutoCompleteTextView struct{ EditText }

// Text 是 TextView 的短别名。
type Text struct{ TextView }

// TextView 返回文本组件句柄。
func (a *App) TextView(id string) *TextView { return &TextView{Widget{a, id}} }

// EditText 返回输入框句柄。
func (a *App) EditText(id string) *EditText { return &EditText{TextView{Widget{a, id}}} }

// AutoCompleteTextView 返回自动补全输入框句柄。
func (a *App) AutoCompleteTextView(id string) *AutoCompleteTextView {
	return &AutoCompleteTextView{EditText{TextView{Widget{a, id}}}}
}

// Text 返回文本组件句柄，兼容已有项目。
func (a *App) Text(id string) *Text { return &Text{TextView{Widget{a, id}}} }

// SetHint 设置提示文本。
func (v *TextView) SetHint(hint string) error { return v.setProperty("hint", hint) }

// GetHint 读取提示文本。
func (v *TextView) GetHint() (string, error) { return v.getProperty("hint") }

// SetTextSize 设置字号，单位 sp。
func (v *TextView) SetTextSize(size float64) error { return v.setProperty("text_size", size) }

// OnTextChanged 注册文本变化事件。
func (v *TextView) OnTextChanged(fn func(text string)) { v.app.OnTextChanged(v.id, fn) }
