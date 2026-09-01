package gostudio

// LinearLayout 线性布局。
type LinearLayout struct{ Widget }

// FrameLayout 帧布局。
type FrameLayout struct{ Widget }

// RelativeLayout 相对布局。
type RelativeLayout struct{ Widget }

// GridLayout 网格布局。
type GridLayout struct{ Widget }

// TableLayout 表格布局。
type TableLayout struct{ Widget }

// TableRow 表格行。
type TableRow struct{ Widget }

// RadioGroup 单选组。
type RadioGroup struct{ Widget }

// ScrollView 垂直滚动。
type ScrollView struct{ Widget }

// HorizontalScrollView 水平滚动。
type HorizontalScrollView struct{ Widget }

// NestedScrollView 嵌套滚动。
type NestedScrollView struct{ Widget }

// ViewFlipper 翻页布局。
type ViewFlipper struct{ Widget }

func (a *App) LinearLayout(id string) *LinearLayout     { return &LinearLayout{Widget{a, id}} }
func (a *App) FrameLayout(id string) *FrameLayout       { return &FrameLayout{Widget{a, id}} }
func (a *App) RelativeLayout(id string) *RelativeLayout { return &RelativeLayout{Widget{a, id}} }
func (a *App) GridLayout(id string) *GridLayout         { return &GridLayout{Widget{a, id}} }
func (a *App) TableLayout(id string) *TableLayout       { return &TableLayout{Widget{a, id}} }
func (a *App) TableRow(id string) *TableRow             { return &TableRow{Widget{a, id}} }
func (a *App) RadioGroup(id string) *RadioGroup         { return &RadioGroup{Widget{a, id}} }
func (a *App) ScrollView(id string) *ScrollView         { return &ScrollView{Widget{a, id}} }
func (a *App) HorizontalScrollView(id string) *HorizontalScrollView {
	return &HorizontalScrollView{Widget{a, id}}
}
func (a *App) NestedScrollView(id string) *NestedScrollView {
	return &NestedScrollView{Widget{a, id}}
}
func (a *App) ViewFlipper(id string) *ViewFlipper { return &ViewFlipper{Widget{a, id}} }

// SetOrientation 设置布局方向：vertical / horizontal。
func (w *Widget) SetOrientation(vertical bool) error {
	value := "horizontal"
	if vertical {
		value = "vertical"
	}
	return w.setProperty("orientation", value)
}

// ShowNext 显示下一页。
func (v *ViewFlipper) ShowNext() error { return v.invoke("show_next", nil) }

// ShowPrevious 显示上一页。
func (v *ViewFlipper) ShowPrevious() error { return v.invoke("show_previous", nil) }
