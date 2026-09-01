package gostudio

// Button 按钮句柄。
type Button struct{ Widget }

// Button 返回按钮句柄。
func (a *App) Button(id string) *Button { return &Button{Widget{a, id}} }
