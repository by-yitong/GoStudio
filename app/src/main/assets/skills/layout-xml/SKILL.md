---
name: layout-xml
description: GoStudio 布局 XML 方言（AndLua 风格）。凡创建或修改 *.xml 界面文件、添加控件、绑定事件前必读；与标准安卓 XML 不兼容，android: 前缀写法无效。
---

# GoStudio 布局 XML 方言

GoStudio 应用的界面文件（项目根的 layout.xml 及其他 *.xml 页面）使用 AndLua 风格方言，
**不是标准安卓 XML**。写成安卓原生样式不会报错，但属性会被静默忽略，界面不生效、控件找不到。
必须严格遵守以下规则。

## 与标准安卓 XML 的关键差异

- 属性一律**不带** `android:` 前缀，不需要 xmlns 命名空间声明
- id 直接写字符串：`id="btn_send"`（不是 `@+id/btn_send`）
- 尺寸必须带单位：`16dp`、`22sp`、`100px`；裸数字在 layout_width/layout_height/padding/margin 上无效
- 颜色：`#RRGGBB` 或 `#AARRGGBB`
- XML 注释内不能出现 `--`（画分隔线会直接报错）
- 根节点只能有一个；根节点默认 match_parent，子控件默认 wrap_content

## 完整示例

```xml
<LinearLayout orientation="vertical" padding="16dp" background="#F5F6F8">
    <TextView id="tv_title" text="登录" textSize="22sp" textColor="#1A1A1A"
        layout_width="wrap_content" layout_gravity="center_horizontal"
        layout_marginTop="24dp"/>
    <EditText id="et_user" hint="用户名" inputType="text" layout_marginTop="16dp"/>
    <EditText id="et_pwd" hint="密码" inputType="textPassword" layout_marginTop="8dp"/>
    <Button id="btn_login" text="登录" layout_marginTop="24dp"/>
    <ListView id="list_main" layout_height="0dp" layout_weight="1" layout_marginTop="16dp"/>
</LinearLayout>
```

## 支持的控件

- 布局容器：LinearLayout、FrameLayout、RelativeLayout、GridLayout、TableLayout、TableRow、
  RadioGroup、ScrollView、HorizontalScrollView、NestedScrollView、ViewFlipper
- 基础控件：TextView、Button、EditText、AutoCompleteTextView、ImageView、ImageButton、
  CheckBox、RadioButton、Switch、ToggleButton、ProgressBar、SeekBar、RatingBar、Space、View
- 选择/展示：Spinner、ListView、GridView、DatePicker、TimePicker、CalendarView、
  NumberPicker、Chronometer、TextClock、VideoView、WebView

## 常用属性

- `layout_width` / `layout_height`：`match_parent`（可简写 match/fill_parent）、`wrap_content`（wrap）、带单位数字如 `100dp`
- `layout_weight`：数值，LinearLayout 中按权重分配剩余空间（列表撑满余下空间用 `layout_height="0dp" layout_weight="1"`）
- `layout_gravity`：子控件在父容器中的对齐；`gravity`：自身内容对齐。
  取值 center、left、right、top、bottom、start、end、center_horizontal、center_vertical，可用 `|` 组合如 `"center_horizontal|bottom"`
- `layout_margin`（四边）与 `layout_marginLeft` / `layout_marginTop` / `layout_marginRight` / `layout_marginBottom`
- `padding`（四边）与 `paddingLeft` / `paddingTop` / `paddingRight` / `paddingBottom`
- 文本类：`text`、`hint`、`textSize="18sp"`、`textColor`、`textColorHint`
- `inputType`：text、textMultiLine、textPassword、number、numberDecimal、phone
- 文本行为：`singleLine="true"`、`lines`、`maxLines`
- `orientation`：LinearLayout 方向，vertical（默认）/ horizontal
- `visibility`：visible / invisible / gone
- `background="#F5F6F8"`、`src="images/a.png"`（项目相对路径）
- 选择类：`checked="true"`（CheckBox/Switch/RadioButton）
- 进度类：`max`、`progress`（ProgressBar/SeekBar）；`numStars`、`rating`、`stepSize`（RatingBar）
- `numColumns`（GridView 列数）；`minValue` / `maxValue` / `value`（NumberPicker）
- `autoStart="true"`、`flipInterval="3000"`（ViewFlipper 自动轮播，毫秒）
- `scaleType`：fitXY、centerCrop、centerInside、fitCenter 等（ImageView）
- RelativeLayout 子控件定位：`layout_centerInParent`、`layout_centerHorizontal`、`layout_centerVertical`、
  `layout_alignParentTop`、`layout_alignParentBottom`、`layout_alignParentLeft`、`layout_alignParentRight`（值为 "true"）
- GridLayout 子格：`layout_row`、`layout_column`、`layout_rowSpan`、`layout_columnSpan`
- 其余属性走反射兜底：属性名首字母大写找单参数 setXxx（参数为 String/int/float/boolean），
  例如 `alpha="0.5"`；找不到对应方法的属性被静默忽略

## Go 侧事件绑定（appsdk）

```go
app.Button("btn_login").OnClick(func() { /* 点击 */ })
app.On("btn_login", "click", func(appsdk.Event) { /* 等价的通用写法 */ })
app.View("tv_title").OnLongClick(func() { /* 长按，任意控件 */ })
app.EditText("et_user").OnTextChanged(func(text string) { /* 输入变化 */ })
app.Switch("sw_auto").OnCheckedChange(func(checked bool) { /* 开关 */ })
app.SeekBar("sb_size").OnProgressChange(func(progress int) { /* 拖动 */ })
app.RatingBar("rb_star").OnRatingChange(func(rating float64) { /* 评分 */ })
```

**AdapterView（Spinner/ListView/GridView）没有 click 事件**，绑 click 不生效，必须用条目事件：

```go
app.ListView("list_main").OnItemClick(func(position int, text string) { ... })
```

生命周期回调：app.OnCreate / OnStart / OnResume / OnPause / OnStop / OnDestroy(func() {...})。

## Go 侧运行时操作

```go
app.SetText("tv_title", "新标题")          // 或 app.Text("tv_title").SetText("新标题")
text, _ := app.Text("et_user").GetText()
app.View("panel_more").SetVisibility(false) // 隐藏（true=显示）；多面板切换页面可用它
app.View("btn_login").SetEnabled(false)
app.View("tv_title").SetBackground("#FFEEEE")
app.ListView("list_main").SetItems([]string{"苹果", "香蕉"}) // Spinner/GridView 同
pos, _ := app.Spinner("sp_type").GetSelection()
app.ProgressBar("pb_load").SetProgress(50)
app.Switch("sw_auto").SetChecked(true)
on, _ := app.Switch("sw_auto").IsChecked()
app.Toast("已保存", 1)
app.Alert("提示", "内容")
app.Dialog("标题", "内容", "取消", "确定")
app.OnDialog(func(button string) { /* Dialog 按钮回调 */ })
```

## 多页面

- 项目根的 layout.xml 是首页；在项目根再建 `page2.xml`、`settings.xml` 等同级 xml 文件即为新页面
  （必须与 layout.xml 同目录，不要放子文件夹）
- 跳转（压栈）：`app.ShowPage("page2.xml")`；返回（弹栈）：`app.Back()`；系统返回键也会自动弹出页面
- 页面切换时控件表自动切换，不同页面可以有同名 id，但建议避免混淆
- ViewFlipper 可做同屏轮播（子视图 + autoStart/flipInterval），Go 侧 `app.ViewFlipper("vf").ShowNext()` / `ShowPrevious()`

## 常见坑（真实事故）

1. 写 `android:text="..."` → 被静默忽略，文字不显示。去掉 android: 前缀。
2. 写 `id="@+id/btn"` → id 未注册，Go 侧 On/Text 找不到控件。直接 `id="btn"`。
3. 写 `layout_width="100"`（无单位）→ 被忽略回退默认值。必须 `100dp`。
4. 给 ListView/Spinner/GridView 绑 OnClick → 不生效，用 OnItemClick。
5. 注释里写 `<!--` 分隔线 `-->` → 解析报错。注释内不能出现连续两个减号。
6. 用 `<layout>` 作为根节点 → 不支持的控件。根节点必须是具体容器（推荐 LinearLayout）。
