package gostudio

// View 通用占位视图句柄。
type View struct{ Widget }

// Space 空白占位句柄。
type Space struct{ Widget }

func (a *App) View(id string) *View   { return &View{Widget{a, id}} }
func (a *App) Space(id string) *Space { return &Space{Widget{a, id}} }
