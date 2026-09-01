package appsdk

// ProgressBar 进度条。
type ProgressBar struct{ Widget }

// SeekBar 拖动条。
type SeekBar struct{ ProgressBar }

// RatingBar 评分条。
type RatingBar struct{ Widget }

func (a *App) ProgressBar(id string) *ProgressBar { return &ProgressBar{Widget{a, id}} }
func (a *App) SeekBar(id string) *SeekBar         { return &SeekBar{ProgressBar{Widget{a, id}}} }
func (a *App) RatingBar(id string) *RatingBar     { return &RatingBar{Widget{a, id}} }

// SetProgress 设置当前进度。
func (w *Widget) SetProgress(progress int) error { return w.setProperty("progress", progress) }

// GetProgress 读取当前进度。
func (w *Widget) GetProgress() (int, error) {
	value, err := w.numberProperty("progress")
	return int(value), err
}

// SetMax 设置最大进度。
func (w *Widget) SetMax(max int) error { return w.setProperty("max", max) }

// GetMax 读取最大进度。
func (w *Widget) GetMax() (int, error) {
	value, err := w.numberProperty("max")
	return int(value), err
}

// OnProgressChange 注册进度变化。
func (v *ProgressBar) OnProgressChange(fn func(progress int)) { v.app.OnProgressChange(v.id, fn) }

// SetRating 设置评分。
func (v *RatingBar) SetRating(rating float64) error { return v.setProperty("rating", rating) }

// GetRating 读取评分。
func (v *RatingBar) GetRating() (float64, error) { return v.numberProperty("rating") }

// SetNumStars 设置星星数量。
func (v *RatingBar) SetNumStars(count int) error { return v.setProperty("num_stars", count) }

// OnRatingChange 注册评分变化。
func (v *RatingBar) OnRatingChange(fn func(rating float64)) { v.app.OnRatingChange(v.id, fn) }
