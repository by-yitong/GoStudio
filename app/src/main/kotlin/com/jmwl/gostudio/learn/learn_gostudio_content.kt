package com.jmwl.gostudio.learn

/** GoStudio App 开发教程：只做概念说明，不配置实践练习。 */
internal fun gostudio_learn_tracks(): List<learn_track> = listOf(
    learn_track(
        id = "gostudio-app",
        title = "GoStudio App",
        subtitle = "AndLua 式布局、Go 逻辑、生命周期与系统 API",
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
                                - `gostudio/`：内置桥接 SDK，负责与原生界面通信。
                                """
                            ),
                            learn_block.code(
                                """
                                app := gostudio.Start()
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
                                """
                            )
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-app-events",
                title = "组件与事件",
                summary = "用生成代码功能快速绑定点击、长按、文本变化等事件。",
                est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-app-events-c",
                        "常见事件",
                        listOf(
                            learn_block.text("工作区右上角「更多 → 生成代码」会读取 `layout.xml` 中带 `id` 的组件，并按组件类型生成事件代码。"),
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
                                """
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
            )
        )
    )
)
