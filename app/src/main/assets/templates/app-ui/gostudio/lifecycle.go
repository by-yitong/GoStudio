package gostudio

// OnCreate 注册 App 生命周期 create。
func (a *App) OnCreate(fn func()) { a.On("", "create", func(Event) { fn() }) }

// OnStart 注册 App 生命周期 start。
func (a *App) OnStart(fn func()) { a.On("", "start", func(Event) { fn() }) }

// OnResume 注册 App 生命周期 resume。
func (a *App) OnResume(fn func()) { a.On("", "resume", func(Event) { fn() }) }

// OnPause 注册 App 生命周期 pause。
func (a *App) OnPause(fn func()) { a.On("", "pause", func(Event) { fn() }) }

// OnStop 注册 App 生命周期 stop。
func (a *App) OnStop(fn func()) { a.On("", "stop", func(Event) { fn() }) }

// OnDestroy 注册 App 生命周期 destroy。
func (a *App) OnDestroy(fn func()) { a.On("", "destroy", func(Event) { fn() }) }
