package appsdk

// ShowPage 切换到项目内另一个布局文件（页面），如 app.ShowPage("page2.xml")。
// 新页面压入页面栈：系统返回键或 Back() 会回到上一页。
// 页面 xml 与 layout.xml 同级放在项目根目录，控件 id 全局共享。
func (a *App) ShowPage(layout string) error {
	return a.call(message{Op: "system", Action: "show_page", Text: layout})
}

// Back 返回上一个页面；已在最后一页时退出应用界面。
func (a *App) Back() error {
	return a.call(message{Op: "system", Action: "back_page"})
}
