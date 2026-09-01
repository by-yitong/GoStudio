package appsdk

// CheckBox 复选框。
type CheckBox struct{ Widget }

// RadioButton 单选框。
type RadioButton struct{ Widget }

// Switch 开关。
type Switch struct{ Widget }

// ToggleButton 开关按钮。
type ToggleButton struct{ Widget }

func (a *App) CheckBox(id string) *CheckBox         { return &CheckBox{Widget{a, id}} }
func (a *App) RadioButton(id string) *RadioButton   { return &RadioButton{Widget{a, id}} }
func (a *App) Switch(id string) *Switch             { return &Switch{Widget{a, id}} }
func (a *App) ToggleButton(id string) *ToggleButton { return &ToggleButton{Widget{a, id}} }

// SetChecked 设置选中状态。
func (w *Widget) SetChecked(checked bool) error { return w.setProperty("checked", checked) }

// IsChecked 读取选中状态。
func (w *Widget) IsChecked() (bool, error) { return w.boolProperty("checked") }

// Toggle 反转选中状态。
func (w *Widget) Toggle() error { return w.invoke("toggle", nil) }

// OnCheckedChange 注册选中变化。
func (w *Widget) OnCheckedChange(fn func(checked bool)) { w.app.OnCheckedChange(w.id, fn) }
