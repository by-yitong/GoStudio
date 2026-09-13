package com.jmwl.gostudio.learn

/** GoStudio IDE 上手教程：教新用户用好编辑器本身（建项目 → 写代码 → 调 AI → 装插件 → 打包）。 */
internal fun ide_learn_tracks(): List<learn_track> = listOf(
    learn_track(
        id = "gostudio-ide",
        title = "GoStudio 上手",
        subtitle = "从新建项目到打包 APK，用好编辑器本身",
        accent_color = 0xFF9C6ADEL,
        category = "GoStudio App",
        lessons = listOf(
            learn_lesson(
                id = "gostudio-ide-projects",
                title = "新建与导入项目",
                summary = "选模板建第一个项目，或把电脑上的项目导进来。",
                est_minutes = 5,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-ide-projects-c",
                        "两种起点",
                        listOf(
                            learn_block.text("打开 GoStudio 首页，点**新建项目**：填写项目名、选择模板即可创建。模板会生成 `go.mod` 和对应的示例代码："),
                            learn_block.text(
                                """
                                - **Hello World**：最简 main.go，打印一句问候；
                                - **CLI 工具**：读取命令行参数的示例；
                                - **数据库**：database/sql 配合 SQLite 的增删改查；
                                - **Gin / GORM**：Web 框架与 ORM 的起步代码；
                                - **App 界面**：AndLua 式 `layout.xml` + Go 逻辑 + 内置 gostudio SDK，在手机上直接画出界面。
                                """.trimIndent()
                            ),
                            learn_block.text("已有项目？点**导入项目**，会拉起系统文件管理器，选中项目目录后 GoStudio 把它复制进来管理。"),
                            learn_block.callout(
                                "tip",
                                "不确定选哪个模板就选 Hello World——学会运行后再到「学习」里跟着 GoStudio App 课程做一个真正有界面的应用。"
                            )
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-ide-editor",
                title = "编写与运行",
                summary = "编辑器顶栏、代码补全与运行输出。",
                est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-ide-editor-c",
                        "顶栏四件事",
                        listOf(
                            learn_block.text("打开项目进入编辑器，顶栏从左到右依次是：抽屉菜单（文件树）、**保存**、**运行**、**AI 助手**和更多菜单。"),
                            learn_block.text("编辑器内置 Go 语言服务：输入时自动补全包名、函数和字段，写错了会有红色标记；点运行输出面板里的错误条目，可以查看完整错误信息和快速修复建议。"),
                            learn_block.text("点**运行**编译当前项目，输出显示在底部的运行面板里。第一次运行会下载依赖，稍等片刻属于正常现象；`fmt.Println` 打印的内容就出现在这里。"),
                            learn_block.callout(
                                "note",
                                "记得先保存再运行——顶栏的保存按钮或更多菜单里都可以，未保存的修改不会参与编译。"
                            )
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-ide-designer",
                title = "可视化布局设计器",
                summary = "拖组件、改属性、一键生成事件代码。",
                est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-ide-designer-c",
                        "所见即所得",
                        listOf(
                            learn_block.text("App 界面项目打开 `layout.xml` 时，可以进入**可视化设计器**：像拼积木一样添加按钮、文本、列表等组件，右侧调整宽高、边距、颜色等属性，不再需要手写 XML。"),
                            learn_block.text("选中一个带 `id` 的组件，点**事件**，GoStudio 会自动在 `main.go` 里生成对应的事件回调并跳转过去——你只需要填业务逻辑："),
                            learn_block.code(
                                """
                                app.Button("btn").OnClick(func() {
                                    // 设计器生成的回调，填入你的逻辑
                                    app.Toast("被点了")
                                })
                                """
                            ),
                            learn_block.callout(
                                "tip",
                                "给需要操作的组件都设上 id，否则 Go 代码里拿不到它的句柄。"
                            )
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-ide-ai",
                title = "AI 助手",
                summary = "解释报错、生成代码、边问边学。",
                est_minutes = 5,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-ide-ai-c",
                        "把 AI 当同学",
                        listOf(
                            learn_block.text("编辑器顶栏点 **AI 助手** 打开对话面板。它可以结合你当前打开的代码回答问题，常用的三种问法："),
                            learn_block.text(
                                """
                                - **解释**：「这段代码在做什么」「这个报错是什么意思」；
                                - **生成**：「写一个函数判断回文字符串」「把这段改成并发版本」；
                                - **学习**：「slice 和数组有什么区别」「举一个 select 的例子」。
                                """.trimIndent()
                            ),
                            learn_block.text("生成满意后把代码复制进编辑器，点运行验证。AI 偶尔会写错，**运行结果**永远比回答本身更可信。"),
                            learn_block.callout(
                                "note",
                                "AI 助手需要先在设置里配置可用的服务；对话不会自动改动你的项目文件，所有修改都要经你确认。"
                            )
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-ide-plugins",
                title = "插件与工具",
                summary = "插件市场装扩展，内置 JSON→Go 结构体转换。",
                est_minutes = 4,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-ide-plugins-c",
                        "给编辑器装能力",
                        listOf(
                            learn_block.text("首页进入**插件**，可以在插件市场浏览、安装扩展，为编辑器增加新能力。"),
                            learn_block.text("有些工具不需要装插件——GoStudio 已经内置。比如**JSON 转 Go 结构体**：在编辑器里打开工具，粘贴一段 JSON，立刻得到带 `json` tag 的结构体定义，直接复制进项目使用："),
                            learn_block.code(
                                """
                                // 粘贴 {"name":"gopher","age":5}
                                // 得到：
                                type AutoGenerated struct {
                                    Name string `json:"name"`
                                    Age  int    `json:"age"`
                                }
                                """
                            ),
                            learn_block.callout(
                                "tip",
                                "调 HTTP 接口前先用它把响应示例转成结构体，反序列化就只剩 json.Unmarshal 一行了。"
                            )
                        )
                    )
                )
            ),
            learn_lesson(
                id = "gostudio-ide-export",
                title = "打包导出 APK",
                summary = "把 App 界面项目变成可安装分享的 APK。",
                est_minutes = 5,
                steps = listOf(
                    learn_step.concept(
                        "gostudio-ide-export-c",
                        "从项目到 APK",
                        listOf(
                            learn_block.text("App 界面项目调试满意后，在编辑器**更多菜单**里选**打包 APK**：GoStudio 会编译 Go 二进制、连同布局一起注入独立的壳应用，生成可以安装和分享的 APK 文件。"),
                            learn_block.text("新建 App 界面项目时填写的**应用名**和**包名**会成为 APK 的名称与唯一标识；悬浮窗布局（`floats/` 目录）也会自动打进包里。"),
                            learn_block.callout(
                                "warn",
                                "打包前先在 GoStudio 里完整运行一遍，确认逻辑没有报错——打包只是把能跑的程序装进壳里，不会修复问题。"
                            )
                        )
                    )
                )
            )
        )
    )
)
