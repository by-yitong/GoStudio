package appsdk

// DatePicker 日期选择器。
type DatePicker struct{ Widget }

// TimePicker 时间选择器。
type TimePicker struct{ Widget }

// CalendarView 日历。
type CalendarView struct{ Widget }

// NumberPicker 数字选择器。
type NumberPicker struct{ Widget }

func (a *App) DatePicker(id string) *DatePicker     { return &DatePicker{Widget{a, id}} }
func (a *App) TimePicker(id string) *TimePicker     { return &TimePicker{Widget{a, id}} }
func (a *App) CalendarView(id string) *CalendarView { return &CalendarView{Widget{a, id}} }
func (a *App) NumberPicker(id string) *NumberPicker { return &NumberPicker{Widget{a, id}} }

// SetDate 设置日期，格式 yyyy-MM-dd。
func (v *DatePicker) SetDate(date string) error { return v.setProperty("date", date) }

// GetDate 读取日期，格式 yyyy-MM-dd。
func (v *DatePicker) GetDate() (string, error) { return v.getProperty("date") }

// OnDateChange 注册日期变化。
func (v *DatePicker) OnDateChange(fn func(date string)) { v.app.OnDateChange(v.id, fn) }

// SetTime 设置时间，格式 HH:mm。
func (v *TimePicker) SetTime(time string) error { return v.setProperty("time", time) }

// GetTime 读取时间，格式 HH:mm。
func (v *TimePicker) GetTime() (string, error) { return v.getProperty("time") }

// OnTimeChange 注册时间变化。
func (v *TimePicker) OnTimeChange(fn func(time string)) { v.app.OnTimeChange(v.id, fn) }

// SetDate 设置日历日期，格式 yyyy-MM-dd。
func (v *CalendarView) SetDate(date string) error { return v.setProperty("date", date) }

// GetDate 读取日历日期，格式 yyyy-MM-dd。
func (v *CalendarView) GetDate() (string, error) { return v.getProperty("date") }

// SetRange 设置数字选择器范围。
func (v *NumberPicker) SetRange(min, max int) error {
	return v.invoke("set_range", []int{min, max})
}

// SetValue 设置数字选择器值。
func (v *NumberPicker) SetValue(value int) error { return v.setProperty("value", value) }

// GetValue 读取数字选择器值。
func (v *NumberPicker) GetValue() (int, error) {
	value, err := v.numberProperty("value")
	return int(value), err
}
