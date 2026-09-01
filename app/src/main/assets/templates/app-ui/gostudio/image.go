package gostudio

// ImageView 图片组件句柄。
type ImageView struct{ Widget }

// ImageButton 图片按钮句柄。
type ImageButton struct{ ImageView }

// Image 是 ImageView 的短别名。
type Image struct{ ImageView }

// ImageView 返回图片组件句柄。
func (a *App) ImageView(id string) *ImageView { return &ImageView{Widget{a, id}} }

// ImageButton 返回图片按钮句柄。
func (a *App) ImageButton(id string) *ImageButton {
	return &ImageButton{ImageView{Widget{a, id}}}
}

// Image 返回图片组件句柄。
func (a *App) Image(id string) *Image { return &Image{ImageView{Widget{a, id}}} }

// SetImage 设置网络图片。
func (v *ImageView) SetImage(url string) error { return v.Widget.SetImage(url) }

// SetSrc 与 layout.xml 的 src 属性语义一致，是 SetImage 的别名。
func (v *ImageView) SetSrc(url string) error { return v.SetImage(url) }

// SetScaleType 设置缩放方式：fitCenter / centerCrop / fitXY 等。
func (v *ImageView) SetScaleType(scaleType string) error {
	return v.setProperty("scale_type", scaleType)
}
