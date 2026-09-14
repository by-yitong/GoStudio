package appsdk

// Chronometer 计时器。
type Chronometer struct{ TextView }

// TextClock 文本时钟。
type TextClock struct{ TextView }

// VideoView 视频组件。
type VideoView struct{ Widget }

// WebView 网页组件。
type WebView struct{ Widget }

func (a *App) Chronometer(id string) *Chronometer { return &Chronometer{TextView{Widget{a, id}}} }
func (a *App) TextClock(id string) *TextClock     { return &TextClock{TextView{Widget{a, id}}} }
func (a *App) VideoView(id string) *VideoView     { return &VideoView{Widget{a, id}} }
func (a *App) WebView(id string) *WebView         { return &WebView{Widget{a, id}} }

// Start 启动计时器 / 播放视频。
func (w *Widget) Start() error { return w.invoke("start", nil) }

// Stop 停止计时器 / 播放视频。
func (w *Widget) Stop() error { return w.invoke("stop", nil) }

// SetFormat 设置计时器 / 时钟格式。
func (v *Chronometer) SetFormat(format string) error { return v.setProperty("format", format) }
func (v *TextClock) SetFormat(format string) error   { return v.setProperty("format", format) }

// SetVideo 设置视频地址。
func (v *VideoView) SetVideo(url string) error { return v.setProperty("video", url) }

// Pause 暂停视频。
func (w *Widget) Pause() error { return w.invoke("pause", nil) }

// Seek 跳转到指定位置，单位毫秒。
func (v *VideoView) Seek(ms int) error { return v.invoke("seek", ms) }

// Position 返回当前播放位置，单位毫秒。
func (v *VideoView) Position() (int, error) {
	n, err := v.numberProperty("position")
	return int(n), err
}

// Duration 返回视频总时长，单位毫秒；就绪前返回 0。
func (v *VideoView) Duration() (int, error) {
	n, err := v.numberProperty("duration")
	return int(n), err
}

// IsPlaying 返回是否正在播放。
func (v *VideoView) IsPlaying() (bool, error) { return v.boolProperty("is_playing") }

// ShowControls 显示内置控制面板（点视频也可呼出）。
func (v *VideoView) ShowControls() error { return v.invoke("show_controls", nil) }

// HideControls 隐藏内置控制面板。
func (v *VideoView) HideControls() error { return v.invoke("hide_controls", nil) }

// SetControlsEnabled 关闭内置面板与点击呼出——自己做控制面板时用。
func (v *VideoView) SetControlsEnabled(enabled bool) error {
	return v.invoke("set_controls", enabled)
}

// LoadURL 加载网页。
func (v *WebView) LoadURL(url string) error { return v.setProperty("url", url) }

// Reload 重新加载网页。
func (v *WebView) Reload() error { return v.invoke("reload", nil) }

// GoBack 网页后退。
func (v *WebView) GoBack() error { return v.invoke("go_back", nil) }

// GoForward 网页前进。
func (v *WebView) GoForward() error { return v.invoke("go_forward", nil) }

// PlayAudio 播放音频：source 支持网络 URL 与本地路径（项目内相对路径或绝对路径），
// 可选第二个参数传 true 循环播放。本地文件建议放项目 audio/ 目录并以
// "audio/xxx.mp3" 引用，打包 APK 时该目录会一并打进包。同一时间只有一段音频，
// 再次调用会替换当前播放。
func (a *App) PlayAudio(source string, loop ...bool) error {
	return a.call(message{Op: "system", Action: "audio_play", Text: source, Boolean: len(loop) > 0 && loop[0]})
}

// PauseAudio 暂停当前音频。
func (a *App) PauseAudio() error { return a.call(message{Op: "system", Action: "audio_pause"}) }

// ResumeAudio 继续播放已暂停的音频。
func (a *App) ResumeAudio() error { return a.call(message{Op: "system", Action: "audio_resume"}) }

// StopAudio 停止播放并释放当前音频。
func (a *App) StopAudio() error { return a.call(message{Op: "system", Action: "audio_stop"}) }
