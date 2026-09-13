package com.jmwl.gostudio.learn

/** GoStudio App 开发教程：只做概念说明，不配置实践练习。 */
internal fun gostudio_learn_tracks(): List<learn_track> = listOf(
    learn_track(
        id = "gostudio-app",
        title = "GoStudio App",
        subtitle = "AndLua 式布局、Go 逻辑、生命周期、系统 API、悬浮窗与常用组件",
        accent_color = 0xFF5CCFE6L,
        category = "GoStudio App",
        lessons = listOf(
            learn_lesson(
                id = "gostudio-app-basic",
                title = "App 项目结构",
                summary = "理解 layout.xml、main.go 与 gostudio SDK 的关系。",
                est_minutes = 4,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-app-basic-c",
                        "一个项目，两种语言",
                        listOf(
                            learn_block.text(
                                """
                                GoStudio App 项目由三部分组成：

                                - `layout.xml`：声明界面结构与属性，语法与 AndLua 一致；
                                - `main.go`：编写业务逻辑；
                                - `gostudio/`：内置 App SDK，导入名是 `appsdk`，负责与原生界面通信。
                                """.trimIndent()
                            ),
                            learn_block.code(
                                """
                                app := appsdk.Start()
                                tv := app.Text("tv")

                                app.Button("btn").OnClick(func() {
                                    tv.SetText("你好，GoStudio")
                                })

                                app.Run()
                                """
                            ),
                            learn_block.text(
                                """
                                点击「运行」时，GoStudio 会先编译 Go 二进制，再在宿主 App 内渲染布局并启动逻辑进程。打包 APK 时，同样的布局和二进制会被注入独立壳应用。
                                """.trimIndent()
                            )
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-app-events",
                title = "组件与事件",
                summary = "用可视化编辑器的事件功能快速绑定点击、长按、文本变化等事件。",
                est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-app-events-c",
                        "常见事件",
                        listOf(
                            learn_block.text("在 `layout.xml` 的可视化编辑器里选中带 `id` 的组件，点击「事件」即可按组件类型生成并跳转对应的事件代码。"),
                            learn_block.code(
                                """
                                app.OnClick("btn", func() {
                                    app.Toast("按钮被点击")
                                })

                                app.OnTextChanged("input", func(text string) {
                                    app.Log("输入: " + text)
                                })

                                app.OnCheckedChange("switch", func(checked bool) {
                                    app.Log("开关: ", checked)
                                })
                                """
                            ),
                            learn_block.text(
                                """
                                文本类组件支持 `text_change`；复选、单选、开关支持 `checked_change`；拖动条支持 `progress_change`；日期与时间选择器也各有变化事件。
                                """.trimIndent()
                            )
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-app-lifecycle",
                title = "App 生命周期",
                summary = "在 Go 中响应 create、resume、pause、destroy 等状态。",
                est_minutes = 5,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-app-lifecycle-c",
                        "生命周期回调",
                        listOf(
                            learn_block.text("GoStudio 会把宿主 Activity 的生命周期转发给 Go 进程。"),
                            learn_block.code(
                                """
                                app.OnCreate(func() { app.Log("created") })
                                app.OnResume(func() { app.Log("resumed") })
                                app.OnPause(func() { app.Log("paused") })
                                app.OnDestroy(func() { app.Log("destroyed") })
                                """
                            ),
                            learn_block.callout(
                                "note",
                                "生命周期回调运行在事件 goroutine 中。界面操作仍通过 SDK 消息回到主线程执行。"
                            )
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-app-floating",
                title = "系统悬浮窗",
                summary = "申请权限、显示拖动窗口、更新内容，以及加载自定义 XML 悬浮布局。",
                est_minutes = 8,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-app-floating-permission",
                        "权限与基础用法",
                        listOf(
                            learn_block.text("悬浮窗使用 Android 系统窗口权限。首次使用会跳转到系统设置，授权后返回 App 会触发回调。"),
                            learn_block.code(
                                """
                                app.OnFloatingWindowPermission(func(granted bool) {
                                    if !granted {
                                        app.Toast("未授予悬浮窗权限")
                                        return
                                    }

                                    app.ShowFloatingWindow(
                                        "note",
                                        appsdk.FloatTitle("提示"),
                                        appsdk.FloatText("GoStudio 悬浮窗"),
                                        appsdk.FloatPosition(24, 64),
                                        appsdk.FloatSize(220, 0),
                                        appsdk.FloatDraggable(true),
                                        appsdk.FloatCloseButton(true),
                                    )
                                })

                                if ok, _ := app.CanFloatingWindow(); ok {
                                    app.ShowFloatingWindow("note", appsdk.FloatText("已授权"))
                                } else {
                                    app.RequestFloatingWindowPermission()
                                }
                                """
                            ),
                            learn_block.text("`id` 用于后续更新、移动、关闭和事件回调；宽高、坐标单位都是 dp。")
                        )
                    ),
                    learn_step.concept(
                        "gostudio-app-floating-api",
                        "更新、移动与事件",
                        listOf(
                            learn_block.code(
                                """
                                app.SetFloatingWindowText("note", "内容已更新")
                                app.MoveFloatingWindow("note", 80, 120)

                                app.OnFloatingWindowClick("note", func(e appsdk.Event) {
                                    app.Toast("悬浮窗被点击")
                                })

                                app.OnFloatingWindowClose("note", func(e appsdk.Event) {
                                    app.Log("悬浮窗已关闭")
                                })

                                app.CloseFloatingWindow("note")
                                """
                            ),
                            learn_block.callout(
                                "note",
                                "关闭按钮和 CloseFloatingWindow 都会触发 float_close 回调；默认文本窗口支持 float_click 回调。"
                            )
                        )
                    ),
                    learn_step.concept(
                        "gostudio-app-floating-xml",
                        "自定义悬浮窗布局",
                        listOf(
                            learn_block.text("在项目 `floats` 目录中创建 XML，例如 `floats/note.xml`："),
                            learn_block.code(
                                """
                                <LinearLayout orientation="vertical" background="#F21B1C1F" padding="14dp">

                                    <TextView id="float_text" text="悬浮窗内容" textColor="#E6E6E6" textSize="14sp"/>
                                    <Button id="float_btn" text="操作" layout_marginTop="10dp"/>

                                </LinearLayout>
                                """
                            ),
                            learn_block.code(
                                """
                                app.ShowFloatingWindow(
                                    "panel",
                                    appsdk.FloatLayout("floats/note.xml"),
                                    appsdk.FloatSize(280, 0),
                                    appsdk.FloatFocusable(true),
                                )

                                app.Text("float_text").SetText("通过 Go 更新")
                                app.Button("float_btn").OnClick(func() {
                                    app.CloseFloatingWindow("panel")
                                })
                                """
                            ),
                            learn_block.text("悬浮布局里的控件仍会注册到宿主，因此可以继续使用 `app.Text`、`app.Button` 等组件句柄。独立 APK 打包时，`floats/` 目录会自动注入。")
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-app-native",
                title = "原生系统 API",
                summary = "Toast、振动、剪贴板、浏览器、分享与设备信息。",
                est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-app-native-c",
                        "系统桥接",
                        listOf(
                            learn_block.text("SDK 将常见 Android 系统 API 封装成 Go 方法；这些调用由宿主在主线程执行。"),
                            learn_block.code(
                                """
                                app.Toast("保存成功")
                                app.Vibrate(200)
                                app.SetClipboard("复制的文本")

                                info, err := app.DeviceInfo()
                                if err == nil {
                                    app.Log(info.Model, info.Android)
                                }
                                """
                            ),
                            learn_block.text("此外还支持 `OpenURL` 打开浏览器、`Share` 调起系统分享、`GetClipboard` 读取剪贴板。")
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-app-lists",
                title = "列表与下拉选择",
                summary = "ListView、GridView 与 Spinner：填充数据、响应选择。",
                est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-app-lists-c",
                        "填充与点击",
                        listOf(
                            learn_block.text("在布局里放一个下拉框和一个列表："),
                            learn_block.code(
                                """
                                <LinearLayout orientation="vertical" padding="16dp">

                                    <Spinner id="city"/>
                                    <ListView id="list" layout_height="240dp" layout_marginTop="12dp"/>

                                </LinearLayout>
                                """
                            ),
                            learn_block.text("Go 侧用 `SetItems` 填充字符串数组，用 `OnItemClick` 响应选择——列表的点击和下拉框的选中都会走这个回调："),
                            learn_block.code(
                                """
                                app.ListView("list").SetItems([]string{"苹果", "香蕉", "橙子"})
                                app.Spinner("city").SetItems([]string{"北京", "上海", "广州"})

                                app.ListView("list").OnItemClick(func(position int, text string) {
                                    app.Toast("选中: " + text)
                                })

                                app.Spinner("city").OnItemClick(func(position int, text string) {
                                    app.Log("城市:", text)
                                })
                                """
                            ),
                            learn_block.text("用 `Select(position)` 程序化选中某项，`GetSelection()` 读取当前选中项的下标。`GridView` 用法与 `ListView` 完全一致，只是以网格排列。")
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-app-pickers",
                title = "日期与时间选择器",
                summary = "DatePicker、TimePicker、CalendarView 与 NumberPicker。",
                est_minutes = 5,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-app-pickers-c",
                        "读值与监听变化",
                        listOf(
                            learn_block.text("日期、时间类组件在布局里声明后，Go 侧可以读写值并监听变化："),
                            learn_block.code(
                                """
                                <LinearLayout orientation="vertical" padding="16dp">

                                    <DatePicker id="date"/>
                                    <TimePicker id="time" layout_marginTop="12dp"/>
                                    <NumberPicker id="count" layout_marginTop="12dp"/>

                                </LinearLayout>
                                """
                            ),
                            learn_block.code(
                                """
                                // 日期：格式 YYYY-MM-DD
                                app.DatePicker("date").SetDate("2026-01-01")
                                if d, err := app.DatePicker("date").GetDate(); err == nil {
                                    app.Log("日期:", d)
                                }
                                app.DatePicker("date").OnDateChange(func(date string) {
                                    app.Log("新日期:", date)
                                })

                                // 时间：格式 HH:MM
                                app.TimePicker("time").SetTime("08:30")
                                app.TimePicker("time").OnTimeChange(func(t string) {
                                    app.Log("新时间:", t)
                                })

                                // 数字选择器：设范围和值
                                app.NumberPicker("count").SetRange(1, 100)
                                app.NumberPicker("count").SetValue(5)
                                if v, err := app.NumberPicker("count").GetValue(); err == nil {
                                    app.Log("数量:", v)
                                }
                                """
                            ),
                            learn_block.text("`CalendarView` 是月历视图，同样用 `SetDate`/`GetDate` 读写选中日期。也可以在可视化编辑器里选中组件，点「事件」自动生成这些回调。")
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-app-media",
                title = "WebView 与媒体组件",
                summary = "网页、视频、计时器与时钟。",
                est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-app-media-c",
                        "WebView",
                        listOf(
                            learn_block.text("`WebView` 在 App 内嵌一个浏览器，用 `LoadURL` 加载网页，配套导航方法控制前进后退："),
                            learn_block.code(
                                """
                                <WebView id="web" layout_width="match_parent" layout_height="300dp"/>
                                """
                            ),
                            learn_block.code(
                                """
                                wv := app.WebView("web")
                                wv.LoadURL("https://golang.google.cn")

                                app.Button("back").OnClick(func() { wv.GoBack() })
                                app.Button("forward").OnClick(func() { wv.GoForward() })
                                app.Button("reload").OnClick(func() { wv.Reload() })
                                """
                            )
                        )
                    ),
                    learn_step.concept(
                        "gostudio-app-media-video",
                        "视频与计时组件",
                        listOf(
                            learn_block.text("`VideoView` 播放视频：`SetVideo` 设置地址（本地路径或网络 URL），再 `Start`/`Pause`/`Stop` 控制："),
                            learn_block.code(
                                """
                                v := app.VideoView("video")
                                v.SetVideo("https://example.com/demo.mp4")
                                v.Start()
                                """
                            ),
                            learn_block.text("`Chronometer` 是秒表，`TextClock` 是走动的时钟，都用 `SetFormat` 自定义显示格式，`Start`/`Stop` 启停："),
                            learn_block.code(
                                """
                                app.Chronometer("timer").SetFormat("已用 %s 秒")
                                app.Chronometer("timer").Start()

                                app.TextClock("clock").SetFormat("HH:mm:ss")
                                """
                            ),
                            learn_block.callout(
                                "note",
                                "ImageView / ImageButton 用 SetImage(url) 加载本地或网络图片，SetScaleType 控制缩放方式——参数写法和 Android 一致（fitCenter、centerCrop 等）。"
                            )
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-app-dialog",
                title = "对话框与视图控制",
                summary = "Dialog 弹窗，以及隐藏、禁用、改背景等通用操作。",
                est_minutes = 5,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-app-dialog-c",
                        "Dialog 与 Alert",
                        listOf(
                            learn_block.text("`Dialog` 弹出带按钮的对话框，`OnDialog` 回调告诉你用户按了哪个按钮；`Alert` 是单个「确定」按钮的简化版："),
                            learn_block.code(
                                """
                                err := app.Dialog("删除确认", "确定要删除这条记录吗？", "取消", "删除")
                                if err != nil {
                                    app.Toast("弹窗失败: " + err.Error())
                                    return
                                }

                                app.OnDialog(func(button string) {
                                    if button == "删除" {
                                        app.Toast("已删除")
                                    }
                                })

                                app.Alert("提示", "保存成功")
                                """
                            ),
                            learn_block.text("任何组件都支持一组通用操作：显示隐藏、启用禁用、透明度、背景色："),
                            learn_block.code(
                                """
                                btn := app.Button("btn")

                                btn.SetVisibility(false)          // 隐藏（不再占位）
                                btn.SetEnabled(false)             // 禁用，变灰不可点
                                btn.SetAlpha(0.5)                 // 半透明
                                btn.SetBackground("#FF2D2D2D")    // 改背景色
                                btn.SetText("换个文案")
                                """
                            ),
                            learn_block.callout(
                                "tip",
                                "这些方法在所有组件句柄上都有（app.Text、app.ListView 拿到的句柄同样可以），因为它们都继承自同一个 Widget 基础类型。"
                            )
                        )
                    )
                )
            )
        )
    )
)
