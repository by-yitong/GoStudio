package appsdk

// OnClick 注册点击事件，任何带 id 的组件都可以使用。
func (a *App) OnClick(id string, fn func()) {
	a.On(id, "click", func(Event) { fn() })
}

// OnLongClick 注册长按事件。
func (a *App) OnLongClick(id string, fn func()) {
	a.On(id, "long_click", func(Event) { fn() })
}

// OnCheckedChange 注册 CheckBox / RadioButton / Switch / ToggleButton 选中变化。
func (a *App) OnCheckedChange(id string, fn func(checked bool)) {
	a.On(id, "checked_change", func(e Event) { fn(e.Boolean) })
}

// OnTextChanged 注册 TextView / EditText 文本变化。
func (a *App) OnTextChanged(id string, fn func(text string)) {
	a.On(id, "text_change", func(e Event) { fn(e.Text) })
}

// OnProgressChange 注册 ProgressBar / SeekBar 进度变化。
func (a *App) OnProgressChange(id string, fn func(progress int)) {
	a.On(id, "progress_change", func(e Event) { fn(int(e.Number)) })
}

// OnRatingChange 注册 RatingBar 评分变化。
func (a *App) OnRatingChange(id string, fn func(rating float64)) {
	a.On(id, "rating_change", func(e Event) { fn(e.Number) })
}

// OnDateChange 注册 DatePicker 日期变化，格式 yyyy-MM-dd。
func (a *App) OnDateChange(id string, fn func(date string)) {
	a.On(id, "date_change", func(e Event) { fn(e.Text) })
}

// OnTimeChange 注册 TimePicker 时间变化，格式 HH:mm。
func (a *App) OnTimeChange(id string, fn func(time string)) {
	a.On(id, "time_change", func(e Event) { fn(e.Text) })
}

// OnItemClick 注册 Spinner / ListView / GridView 条目点击。
func (a *App) OnItemClick(id string, fn func(position int, text string)) {
	a.On(id, "item_click", func(e Event) { fn(int(e.Number), e.Text) })
}
