package com.jmwl.gostudio.learn

/**
 * 学习课程内容模型（移植自 CodeAssist LearnContent 的 authoring DSL，Go 版）。
 *
 * 约定：交互练习的 starter_code / solution 都是完整可编译的单文件程序
 * （package main + main 函数），判题器把它写入隐藏 scratch 目录后 `go run`。
 * 答案与判题规则（exercise_check）只留在本模块，不进 UI。
 */

data class learn_track(
    val id: String,
    val title: String,
    val subtitle: String,
    /** 主题色（ARGB Long），课程卡片色条用。 */
    val accent_color: Long,
    /** 分组标题，如 "Go 语言" / "并发编程"。 */
    val category: String,
    val lessons: List<learn_lesson>
)

data class learn_lesson(
    val id: String,
    val title: String,
    val summary: String,
    val est_minutes: Int = 5,
    val steps: List<learn_step>
)

sealed interface learn_step {
    val id: String
    val title: String

    /** 概念讲解：只读内容块。 */
    data class concept(
        override val id: String,
        override val title: String,
        val blocks: List<learn_block>
    ) : learn_step

    /** 交互练习：内嵌编辑器 + 运行判题。 */
    data class interactive(
        override val id: String,
        override val title: String,
        val blocks: List<learn_block>,
        val starter_code: String,
        val hints: List<String> = emptyList(),
        val solution: String,
        val check: exercise_check
    ) : learn_step

    /** 测验：单选。 */
    data class quiz(
        override val id: String,
        override val title: String,
        val prompt: String,
        val options: List<String>,
        val correct_index: Int,
        val explanation: String = ""
    ) : learn_step
}

/**
 * 交互练习判题规则（移植自 CodeAssist ExerciseCheck）：
 * - [expected_output] 归一化后与程序 stdout 全量比对；
 * - [must_contain] 每个片段都必须出现在输出里；
 * - 两者都空时，程序正常退出（exit 0）即通过；
 * - [require_source] 反硬编码：剥掉注释/字符串内容并去空白后，
 *   源码必须包含这些构造（防止直接 print 预期答案骗过判题）。
 */
data class exercise_check(
    val expected_output: String? = null,
    val must_contain: List<String> = emptyList(),
    val require_source: List<String> = emptyList(),
    val case_sensitive: Boolean = true
)

/** 内容块：讲解文本（内联 **粗体** / `代码`）/ 只读代码 / 提示框。 */
sealed interface learn_block {
    data class text(val md: String) : learn_block
    data class code(val src: String) : learn_block
    /** kind: tip / note / warn。 */
    data class callout(val kind: String, val text: String) : learn_block
}

private fun text(md: String) = learn_block.text(md.trimIndent())
internal fun normalize_lesson_code(src: String): String {
    val code = src.trimIndent().trim()
    if ('\n' in code || !code.startsWith("package main")) return code
    return code
        .replace("\\r\\n", "\n")
        .replace("\\r", "\n")
        .replace("\\n", "\n")
}

private fun code(src: String) = learn_block.code(normalize_lesson_code(src))
private fun tip(t: String) = learn_block.callout("tip", t)
private fun note(t: String) = learn_block.callout("note", t)

object learn_content {

    val accent_go = 0xFF00ADD8L
    val accent_next = 0xFF00A8A0L
    val accent_conc = 0xFFE0533DL

    val tracks: List<learn_track> =
        listOf(go_basics(), go_next(), go_concurrency()) + practical_learn_tracks()

    fun find_lesson(lesson_id: String): Pair<learn_track, learn_lesson>? {
        for (track in tracks) for (lesson in track.lessons) {
            if (lesson.id == lesson_id) return track to lesson
        }
        return null
    }

    // ================= Go 入门 =================

    private fun go_basics() = learn_track(
        id = "go-basics", title = "Go 入门", subtitle = "从 Hello World 到循环，写出第一批 Go 程序",
        accent_color = accent_go, category = "Go 语言",
        lessons = listOf(
            learn_lesson(
                id = "go-hello", title = "Hello, Go", summary = "写下第一个 Go 程序。", est_minutes = 4,
                steps = listOf(
                    learn_step.concept(
                        "go-hello-c", "main 包与 main 函数",
                        listOf(
                            text("每个 Go 程序都从 **main 包**的 **main 函数**开始运行："),
                            code(
                                """
                                package main

                                import "fmt"

                                func main() {
                                    fmt.Println("Hello, Go!")
                                }
                                """
                            ),
                            text("`package main` 声明这是一个可执行程序；`import \"fmt\"` 引入标准库的格式化输出包；`fmt.Println(...)` 打印一行文本。"),
                            tip("Go 的花括号 `{` 必须和函数声明在同一行，这是编译器强制的。")
                        )
                    ),
                    learn_step.interactive(
                        "go-hello-i", "打印问候语",
                        listOf(text("让程序输出：\n\n`Hello, Go!`\n\n改好代码后点「运行并检查」。")),
                        starter_code = """
                            package main

                            import "fmt"

                            func main() {
                                // 在下面打印 Hello, Go!
                            }
                        """,
                        hints = listOf(
                            "用 fmt.Println 输出。",
                            "注意文本要完全一致：fmt.Println(\"Hello, Go!\")"
                        ),
                        solution = """
                            package main

                            import "fmt"

                            func main() {
                                fmt.Println("Hello, Go!")
                            }
                        """,
                        check = exercise_check(expected_output = "Hello, Go!", require_source = listOf("fmt.Println"))
                    ),
                    learn_step.quiz(
                        "go-hello-q", "小测",
                        prompt = "可执行 Go 程序的入口是什么？",
                        options = listOf("start() 函数", "main 包里的 main 函数", "Main 类", "init.go 文件"),
                        correct_index = 1,
                        explanation = "package main 的 func main() 是程序入口。"
                    )
                )
            ),
            learn_lesson(
                id = "go-vars", title = "变量与常量", summary = "用 var、const 和 := 声明数据。", est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "go-vars-c", "三种声明方式",
                        listOf(
                            text("Go 声明变量有三种常用写法："),
                            code(
                                """
                                var name string = "Go"  // 完整声明
                                var count = 42          // 类型推断
                                ok := true              // 短声明（只能在函数内）
                                const pi = 3.14         // 常量，不可修改
                                """
                            ),
                            text("`:=` 短声明最常用——声明并赋值一步完成，类型自动推断。**常量**（const）在编译期确定，之后不能重新赋值。"),
                            note("未使用的变量会导致编译错误，Go 用这种方式强迫你保持代码干净。")
                        )
                    ),
                    learn_step.interactive(
                        "go-vars-i", "问好",
                        listOf(text("声明一个变量 `name` 值为 `Gopher`，然后输出：\n\n`Hello, Gopher!`")),
                        starter_code = """
                            package main

                            import "fmt"

                            func main() {
                                // 1. 用 := 声明 name := "Gopher"
                                // 2. 打印 Hello, Gopher!
                            }
                        """,
                        hints = listOf(
                            "name := \"Gopher\"",
                            "fmt.Println(\"Hello, \" + name + \"!\")"
                        ),
                        solution = """
                            package main

                            import "fmt"

                            func main() {
                                name := "Gopher"
                                fmt.Println("Hello, " + name + "!")
                            }
                        """,
                        check = exercise_check(expected_output = "Hello, Gopher!", require_source = listOf("name :=", "fmt.Println"))
                    ),
                    learn_step.quiz(
                        "go-vars-q", "小测",
                        prompt = "下面哪种声明方式只能在函数内部使用？",
                        options = listOf("var x = 1", "const y = 2", "z := 3", "三种都可以"),
                        correct_index = 2,
                        explanation = "短声明 := 只能出现在函数内。"
                    )
                )
            ),
            learn_lesson(
                id = "go-funcs", title = "函数", summary = "定义带参数和返回值的函数。", est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "go-funcs-c", "函数声明",
                        listOf(
                            text("函数用 `func` 声明，**参数类型**和**返回类型**都写在名字后面："),
                            code(
                                """
                                func add(a int, b int) int {
                                    return a + b
                                }

                                // 参数类型相同可以合并简写
                                func add2(a, b int) int { return a + b }
                                """
                            ),
                            text("调用函数直接用名字：`add(2, 3)` 的值是 `5`。")
                        )
                    ),
                    learn_step.interactive(
                        "go-funcs-i", "写一个 add",
                        listOf(text("定义函数 `add(a int, b int) int` 返回两数之和，然后打印 `add(2, 3)`（应该是 `5`）。")),
                        starter_code = """
                            package main

                            import "fmt"

                            // 在这里定义 add 函数

                            func main() {
                                // 打印 add(2, 3)
                            }
                        """,
                        hints = listOf(
                            "func add(a int, b int) int { return a + b }",
                            "fmt.Println(add(2, 3))"
                        ),
                        solution = """
                            package main

                            import "fmt"

                            func add(a int, b int) int {
                                return a + b
                            }

                            func main() {
                                fmt.Println(add(2, 3))
                            }
                        """,
                        check = exercise_check(expected_output = "5", require_source = listOf("func add", "add(2, 3)"))
                    )
                )
            ),
            learn_lesson(
                id = "go-for", title = "分支与循环", summary = "if/else 与唯一的 for。", est_minutes = 7,
                steps = listOf(
                    learn_step.concept(
                        "go-for-c", "if 和 for",
                        listOf(
                            text("Go 的 `if` 不需要小括号，但**花括号必须有**："),
                            code(
                                """
                                if n > 10 {
                                    fmt.Println("big")
                                } else {
                                    fmt.Println("small")
                                }
                                """
                            ),
                            text("Go 只有一种循环——`for`。经典三段式和其它语言的 for 一样："),
                            code(
                                """
                                for i := 1; i <= 5; i++ {
                                    fmt.Println(i)
                                }
                                """
                            )
                        )
                    ),
                    learn_step.interactive(
                        "go-for-i", "数到五",
                        listOf(text("用 for 循环输出 `1` 到 `5`，每个数字一行。")),
                        starter_code = """
                            package main

                            import "fmt"

                            func main() {
                                // 循环打印 1 到 5
                            }
                        """,
                        hints = listOf(
                            "for i := 1; i <= 5; i++ { ... }",
                            "循环体里 fmt.Println(i)"
                        ),
                        solution = """
                            package main

                            import "fmt"

                            func main() {
                                for i := 1; i <= 5; i++ {
                                    fmt.Println(i)
                                }
                            }
                        """,
                        check = exercise_check(expected_output = "1\n2\n3\n4\n5", require_source = listOf("for"))
                    ),
                    learn_step.quiz(
                        "go-for-q", "小测",
                        prompt = "Go 里有哪几种循环关键字？",
                        options = listOf("for 和 while", "只有 for", "for、while、do-while", "loop"),
                        correct_index = 1,
                        explanation = "Go 只有 for，配合不同写法可以表达 while / 无限循环。"
                    )
                )
            )
        )
    )

    // ================= Go 进阶 =================

    private fun go_next() = learn_track(
        id = "go-next", title = "Go 进阶", subtitle = "切片、map、结构体与错误处理",
        accent_color = accent_next, category = "Go 语言",
        lessons = listOf(
            learn_lesson(
                id = "go-slices", title = "切片", summary = "Go 最常用的集合类型。", est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "go-slices-c", "创建与遍历切片",
                        listOf(
                            text("**切片**（slice）是长度可变的序列："),
                            code(
                                """
                                nums := []int{1, 2, 3}
                                nums = append(nums, 4)      // 追加元素 → [1 2 3 4]
                                fmt.Println(len(nums))      // 4
                                fmt.Println(nums[0])        // 1
                                """
                            ),
                            text("`append` 返回新的切片，必须用返回值重新赋值。遍历用 `for range`："),
                            code(
                                """
                                for i, v := range nums {
                                    fmt.Println(i, v)
                                }
                                """
                            )
                        )
                    ),
                    learn_step.interactive(
                        "go-slices-i", "求和",
                        listOf(text("给定切片 `nums := []int{1, 2, 3, 4, 5}`，用 `for range` 求和并输出 `15`。")),
                        starter_code = """
                            package main

                            import "fmt"

                            func main() {
                                nums := []int{1, 2, 3, 4, 5}
                                // 用 for range 求和并打印
                            }
                        """,
                        hints = listOf(
                            "先 sum := 0，循环里 sum += v",
                            "for _, v := range nums { sum += v }"
                        ),
                        solution = """
                            package main

                            import "fmt"

                            func main() {
                                nums := []int{1, 2, 3, 4, 5}
                                sum := 0
                                for _, v := range nums {
                                    sum += v
                                }
                                fmt.Println(sum)
                            }
                        """,
                        check = exercise_check(expected_output = "15", require_source = listOf("range", "sum"))
                    ),
                    learn_step.quiz(
                        "go-slices-q", "小测",
                        prompt = "append(nums, 4) 之后必须怎么写？",
                        options = listOf(
                            "什么都不用做，原切片已修改",
                            "nums = append(nums, 4)",
                            "append 会自动保存",
                            "需要重新声明切片"
                        ),
                        correct_index = 1,
                        explanation = "append 返回（可能新分配的）切片，必须接住返回值。"
                    )
                )
            ),
            learn_lesson(
                id = "go-maps", title = "map", summary = "键值对映射。", est_minutes = 5,
                steps = listOf(
                    learn_step.concept(
                        "go-maps-c", "创建与访问 map",
                        listOf(
                            text("**map** 是键值对集合："),
                            code(
                                """
                                ages := map[string]int{
                                    "Go":   15,
                                    "Kotlin": 14,
                                }
                                ages["Rust"] = 10          // 添加/修改
                                fmt.Println(ages["Go"])    // 15
                                delete(ages, "Rust")       // 删除
                                v, ok := ages["Java"]      // ok=false 表示键不存在
                                """
                            ),
                            tip("访问不存在的键得到零值而不是报错；用 `v, ok :=` 形式区分「零值」和「不存在」。")
                        )
                    ),
                    learn_step.interactive(
                        "go-maps-i", "统计单词",
                        listOf(text("统计字符串切片里 `go` 出现的次数，输出 `3`。")),
                        starter_code = """
                            package main

                            import "fmt"

                            func main() {
                                words := []string{"go", "rust", "go", "kotlin", "go"}
                                // 用 map 统计 "go" 出现的次数并打印
                            }
                        """,
                        hints = listOf(
                            "counts := map[string]int{}",
                            "for _, w := range words { counts[w]++ }  然后 fmt.Println(counts[\"go\"])"
                        ),
                        solution = """
                            package main

                            import "fmt"

                            func main() {
                                words := []string{"go", "rust", "go", "kotlin", "go"}
                                counts := map[string]int{}
                                for _, w := range words {
                                    counts[w]++
                                }
                                fmt.Println(counts["go"])
                            }
                        """,
                        check = exercise_check(expected_output = "3", require_source = listOf("map[string]int", "counts"))
                    )
                )
            ),
            learn_lesson(
                id = "go-structs", title = "结构体与方法", summary = "把数据和行为组织在一起。", est_minutes = 7,
                steps = listOf(
                    learn_step.concept(
                        "go-structs-c", "type 与方法",
                        listOf(
                            text("用 `type ... struct` 定义自己的类型，字段在花括号里声明："),
                            code(
                                """
                                type Point struct {
                                    X, Y int
                                }

                                // 方法 = 带接收者的函数；(p Point) 是接收者
                                func (p Point) Sum() int {
                                    return p.X + p.Y
                                }

                                p := Point{X: 2, Y: 3}
                                fmt.Println(p.Sum())   // 5
                                """
                            ),
                            text("接收者写在 `func` 和方法名之间，方法内用接收者名访问字段。"),
                            tip("需要修改字段时用指针接收者 `(p *Point)`；只读用值接收者 `(p Point)`。")
                        )
                    ),
                    learn_step.interactive(
                        "go-structs-i", "给 Point 加方法",
                        listOf(text("定义 `Point{X, Y int}` 并加一个 `Sum() int` 方法返回 `X+Y`，然后打印 `Point{2, 3}.Sum()`（`5`）。")),
                        starter_code = """
                            package main

                            import "fmt"

                            // 定义 Point 结构体和 Sum 方法

                            func main() {
                                // 打印 Point{2, 3} 的 Sum()
                            }
                        """,
                        hints = listOf(
                            "type Point struct { X, Y int }",
                            "func (p Point) Sum() int { return p.X + p.Y }  然后 fmt.Println(Point{2, 3}.Sum())"
                        ),
                        solution = """
                            package main

                            import "fmt"

                            type Point struct {
                                X, Y int
                            }

                            func (p Point) Sum() int {
                                return p.X + p.Y
                            }

                            func main() {
                                fmt.Println(Point{2, 3}.Sum())
                            }
                        """,
                        check = exercise_check(expected_output = "5", require_source = listOf("type Point", "func (p Point) Sum", ".Sum()"))
                    )
                )
            ),
            learn_lesson(
                id = "go-errors", title = "错误处理", summary = "if err != nil 是 Go 的日常。", est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "go-errors-c", "error 与多返回值",
                        listOf(
                            text("Go 函数常返回 `(结果, error)` 两个值，调用方**必须**检查错误："),
                            code(
                                """
                                import "errors"

                                func divide(a, b int) (int, error) {
                                    if b == 0 {
                                        return 0, errors.New("divide by zero")
                                    }
                                    return a / b, nil
                                }

                                result, err := divide(10, 2)
                                if err != nil {
                                    fmt.Println("出错:", err)
                                    return
                                }
                                fmt.Println(result)
                                """
                            ),
                            note("`nil` 表示「没有错误」。忽略 error 直接用结果是 Go 代码最常见的坑。")
                        )
                    ),
                    learn_step.interactive(
                        "go-errors-i", "安全除法",
                        listOf(text("实现 `divide(a, b int) (int, error)`：b 为 0 时返回错误；否则打印 `divide(10, 2)` 的结果 `5`。")),
                        starter_code = """
                            package main

                            import (
                                "errors"
                                "fmt"
                            )

                            func divide(a, b int) (int, error) {
                                // b == 0 时返回 0 和错误，否则返回 a/b 和 nil
                            }

                            func main() {
                                // 打印 divide(10, 2) 的结果，记得检查 err
                            }
                        """,
                        hints = listOf(
                            "if b == 0 { return 0, errors.New(\"divide by zero\") }",
                            "result, err := divide(10, 2); if err != nil { ... } fmt.Println(result)"
                        ),
                        solution = """
                            package main

                            import (
                                "errors"
                                "fmt"
                            )

                            func divide(a, b int) (int, error) {
                                if b == 0 {
                                    return 0, errors.New("divide by zero")
                                }
                                return a / b, nil
                            }

                            func main() {
                                result, err := divide(10, 2)
                                if err != nil {
                                    fmt.Println("出错:", err)
                                    return
                                }
                                fmt.Println(result)
                            }
                        """,
                        check = exercise_check(expected_output = "5", require_source = listOf("error", "errors.New", "err"))
                    )
                )
            )
        )
    )

    // ================= Go 并发 =================

    private fun go_concurrency() = learn_track(
        id = "go-conc", title = "Go 并发", subtitle = "goroutine、channel 与 WaitGroup",
        accent_color = accent_conc, category = "并发编程",
        lessons = listOf(
            learn_lesson(
                id = "go-goroutine", title = "goroutine", summary = "用 go 关键字启动并发任务。", est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "go-goroutine-c", "go 关键字",
                        listOf(
                            text("在函数调用前加 `go`，它就在新的 **goroutine**（轻量线程）里并发执行："),
                            code(
                                """
                                func say(s string) {
                                    fmt.Println(s)
                                }

                                go say("hello")   // 并发执行，不等待
                                say("world")      // 当前 goroutine 直接执行
                                """
                            ),
                            text("main 函数不会等 goroutine 结束——主流程退出，所有 goroutine 一起结束。所以需要 `time.Sleep` 或 **WaitGroup** 来等待。")
                        )
                    ),
                    learn_step.interactive(
                        "go-goroutine-i", "先 world 后 hello",
                        listOf(text("主 goroutine 先打印 `world`，再 sleep 等待并发任务打印 `hello`。输出顺序：\n\n`world`\n`hello`")),
                        starter_code = """
                            package main

                            import (
                                "fmt"
                                "time"
                            )

                            func say(s string) {
                                fmt.Println(s)
                            }

                            func main() {
                                // 1. go say("hello")
                                // 2. say("world")
                                // 3. time.Sleep(100 * time.Millisecond) 等 hello 打印完
                            }
                        """,
                        hints = listOf(
                            "go say(\"hello\")",
                            "say(\"world\") 之后 time.Sleep(100 * time.Millisecond)"
                        ),
                        solution = """
                            package main

                            import (
                                "fmt"
                                "time"
                            )

                            func say(s string) {
                                fmt.Println(s)
                            }

                            func main() {
                                go say("hello")
                                say("world")
                                time.Sleep(100 * time.Millisecond)
                            }
                        """,
                        check = exercise_check(expected_output = "world\nhello", require_source = listOf("go say", "time.Sleep"))
                    )
                )
            ),
            learn_lesson(
                id = "go-channel", title = "channel", summary = "用通道在 goroutine 间传值。", est_minutes = 7,
                steps = listOf(
                    learn_step.concept(
                        "go-channel-c", "make 与 <-",
                        listOf(
                            text("**channel** 是 goroutine 之间的管道，用 `make` 创建："),
                            code(
                                """
                                ch := make(chan string)     // 无缓冲通道

                                go func() {
                                    ch <- "done"           // 发送（没人接收就阻塞）
                                }()

                                msg := <-ch                // 接收（阻塞直到有值）
                                fmt.Println(msg)
                                """
                            ),
                            text("无缓冲通道的发送会阻塞到对方接收——这本身就是一种同步手段，main 用 `<-ch` 等待 goroutine 的结果，比 sleep 可靠。")
                        )
                    ),
                    learn_step.interactive(
                        "go-channel-i", "用 channel 传结果",
                        listOf(text("在 goroutine 里计算 `2+3` 并发送到 channel，main 接收后打印 `5`。")),
                        starter_code = """
                            package main

                            import "fmt"

                            func main() {
                                ch := make(chan int)
                                // 1. go func() { ch <- 2 + 3 }()
                                // 2. result := <-ch
                                // 3. fmt.Println(result)
                            }
                        """,
                        hints = listOf(
                            "go func() { ch <- 2 + 3 }()",
                            "result := <-ch  然后 fmt.Println(result)"
                        ),
                        solution = """
                            package main

                            import "fmt"

                            func main() {
                                ch := make(chan int)
                                go func() {
                                    ch <- 2 + 3
                                }()
                                result := <-ch
                                fmt.Println(result)
                            }
                        """,
                        check = exercise_check(expected_output = "5", require_source = listOf("make(chan", "go func", "<-ch"))
                    ),
                    learn_step.quiz(
                        "go-channel-q", "小测",
                        prompt = "无缓冲 channel 的发送 `ch <- v` 什么时候解除阻塞？",
                        options = listOf(
                            "立刻返回，值先存起来",
                            "另一个 goroutine 从 ch 接收时",
                            "超时后自动解除",
                            "取决于缓冲区大小"
                        ),
                        correct_index = 1,
                        explanation = "无缓冲通道发送会阻塞，直到有接收方就绪——收发双方同步。"
                    )
                )
            ),
            learn_lesson(
                id = "go-waitgroup", title = "WaitGroup", summary = "优雅地等待一组 goroutine。", est_minutes = 7,
                steps = listOf(
                    learn_step.concept(
                        "go-waitgroup-c", "Add / Done / Wait",
                        listOf(
                            text("**sync.WaitGroup** 是标准的等待工具：`Add` 计数 +1，每个 goroutine 结束时 `Done`（计数 -1），`Wait` 阻塞到计数归零："),
                            code(
                                """
                                var wg sync.WaitGroup

                                for i := 1; i <= 3; i++ {
                                    wg.Add(1)
                                    go func(n int) {
                                        defer wg.Done()
                                        fmt.Println("任务", n)
                                    }(i)
                                }
                                wg.Wait()
                                fmt.Println("全部完成")
                                """
                            ),
                            note("循环变量要作为参数传入 goroutine，避免闭包捕获同一变量的经典坑。")
                        )
                    ),
                    learn_step.interactive(
                        "go-waitgroup-i", "等三个任务",
                        listOf(text("启动 3 个 goroutine 各打印一行 `task done`，用 WaitGroup 等全部结束后再打印 `all done`。输出应为三行 `task done` 加一行 `all done`。")),
                        starter_code = """
                            package main

                            import (
                                "fmt"
                                "sync"
                            )

                            func main() {
                                var wg sync.WaitGroup
                                // 1. 循环 3 次：wg.Add(1) + go func() { defer wg.Done(); fmt.Println("task done") }()
                                // 2. wg.Wait()
                                // 3. fmt.Println("all done")
                            }
                        """,
                        hints = listOf(
                            "for i := 0; i < 3; i++ { wg.Add(1); go func() { defer wg.Done(); ... }() }",
                            "wg.Wait() 之后打印 all done"
                        ),
                        solution = """
                            package main

                            import (
                                "fmt"
                                "sync"
                            )

                            func main() {
                                var wg sync.WaitGroup
                                for i := 0; i < 3; i++ {
                                    wg.Add(1)
                                    go func() {
                                        defer wg.Done()
                                        fmt.Println("task done")
                                    }()
                                }
                                wg.Wait()
                                fmt.Println("all done")
                            }
                        """,
                        check = exercise_check(
                            must_contain = listOf("task done", "all done"),
                            require_source = listOf("wg.Add", "wg.Done", "wg.Wait")
                        )
                    )
                )
            )
        )
    )
}
