package appsdk

// Spinner 下拉框。
type Spinner struct{ Widget }

// ListView 列表。
type ListView struct{ Widget }

// GridView 网格列表。
type GridView struct{ Widget }

func (a *App) Spinner(id string) *Spinner   { return &Spinner{Widget{a, id}} }
func (a *App) ListView(id string) *ListView { return &ListView{Widget{a, id}} }
func (a *App) GridView(id string) *GridView { return &GridView{Widget{a, id}} }

// SetItems 设置简单文本数据源。
func (w *Widget) SetItems(items []string) error { return w.invoke("set_items", items) }

// Select 选中指定下标。
func (w *Widget) Select(position int) error { return w.setProperty("selection", position) }

// GetSelection 读取选中下标。
func (w *Widget) GetSelection() (int, error) {
	value, err := w.numberProperty("selection")
	return int(value), err
}

// OnItemClick 注册条目点击事件。
func (w *Widget) OnItemClick(fn func(position int, text string)) {
	w.app.OnItemClick(w.id, fn)
}
