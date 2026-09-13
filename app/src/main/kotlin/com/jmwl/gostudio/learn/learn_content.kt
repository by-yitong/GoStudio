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
        listOf(go_basics(), go_next(), go_concurrency()) + gostudio_learn_tracks() + ide_learn_tracks() + practical_learn_tracks()

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
        id = "go-next", title = "Go 进阶", subtitle = "切片、map、结构体、接口、错误处理与文件 IO",
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
                id = "go-interface", title = "接口", summary = "不关心具体类型，只关心它能做什么。", est_minutes = 8,
                steps = listOf(
                    learn_step.concept(
                        "go-interface-c", "定义与实现",
                        listOf(
                            text("**接口**（interface）是一份方法清单。一个类型只要把清单上的方法**原样实现**出来（名字、参数、返回值一致），就自动满足这个接口，不需要写任何「我实现了它」的声明："),
                            code(
                                """
                                type Shape interface {
                                    Area() float64
                                }

                                type Square struct{ side float64 }

                                func (s Square) Area() float64 { return s.side * s.side }
                                """
                            ),
                            text("现在 `Square` 满足了 `Shape`，可以赋给 `Shape` 类型的变量或参数。调用方只依赖接口、不依赖具体类型——这就是 Go 的多态："),
                            code(
                                """
                                func printArea(s Shape) {
                                    fmt.Println(s.Area())
                                }

                                printArea(Square{side: 3})  // 9
                                """
                            ),
                            tip("标准库的 `error` 就是一个只有 `Error() string` 方法的接口——下一课的错误处理会天天和它打交道。")
                        )
                    ),
                    learn_step.interactive(
                        "go-interface-i", "用接口求面积",
                        listOf(text("定义接口 `Shape`（含方法 `Area() float64`），为 `Square` 实现它，然后打印边长 `3` 的面积（`9`）。")),
                        starter_code = """
                            package main

                            import "fmt"

                            // 1. 定义 Shape 接口
                            // 2. 定义 Square 结构体和它的 Area 方法

                            func main() {
                                // 3. 声明一个 Shape 变量并打印它的面积
                            }
                        """,
                        hints = listOf(
                            "type Shape interface { Area() float64 }",
                            "type Square struct{ side float64 }，方法接收者写 func (s Square) Area()",
                            "var s Shape = Square{side: 3}，然后 fmt.Println(s.Area())"
                        ),
                        solution = """
                            package main

                            import "fmt"

                            type Shape interface {
                                Area() float64
                            }

                            type Square struct{ side float64 }

                            func (s Square) Area() float64 { return s.side * s.side }

                            func main() {
                                var s Shape = Square{side: 3}
                                fmt.Println(s.Area())
                            }
                        """,
                        check = exercise_check(expected_output = "9", require_source = listOf("interface", "func (s Square) Area()", "Shape"))
                    ),
                    learn_step.quiz(
                        "go-interface-q", "小测",
                        prompt = "Go 里一个类型怎样「表明」自己实现了某接口？",
                        options = listOf(
                            "用 implements 关键字声明",
                            "实现接口里的全部方法即可，无需任何声明",
                            "从接口继承",
                            "在注册表里登记"
                        ),
                        correct_index = 1,
                        explanation = "Go 的接口实现是隐式的：方法签名匹配即满足，这是它和 Java 等语言最大的区别之一。"
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
            ),
            learn_lesson(
                id = "go-strings", title = "字符串与格式化", summary = "Sprintf、strings 与 strconv。", est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "go-strings-c", "格式化动词",
                        listOf(
                            text("`fmt.Sprintf` 按占位符（动词）拼接字符串，**不打印、只返回结果**："),
                            code(
                                """
                                s := fmt.Sprintf("%s 有 %d 个苹果", "Gopher", 3)
                                // s == "Gopher 有 3 个苹果"

                                fmt.Printf("圆周率约 %.2f\n", 3.14159)  // 3.14
                                """
                            ),
                            text("常用动词：`%s` 字符串、`%d` 整数、`%f`/`%.2f` 浮点、`%t` 布尔、`%v` 任意值通用。处理文本还离不开 `strings` 和 `strconv`："),
                            code(
                                """
                                strings.ToUpper("go")            // "GO"
                                strings.Contains("hello", "ell") // true
                                strings.Split("a,b,c", ",")      // ["a" "b" "c"]
                                strings.TrimSpace("  hi  ")      // "hi"

                                n, err := strconv.Atoi("42")      // 字符串 → int
                                strconv.Itoa(42)                  // int → 字符串
                                """
                            )
                        )
                    ),
                    learn_step.interactive(
                        "go-strings-i", "拼一句话",
                        listOf(text("用 `fmt.Sprintf` 把 `Gopher` 和 `5` 拼成一句话并打印：\n\n`Gopher 今年 5 岁`")),
                        starter_code = """
                            package main

                            import "fmt"

                            func main() {
                                name := "Gopher"
                                age := 5
                                // 用 Sprintf 生成 "Gopher 今年 5 岁" 并打印
                            }
                        """,
                        hints = listOf(
                            "fmt.Sprintf(\"%s 今年 %d 岁\", name, age)",
                            "结果用 fmt.Println 打印"
                        ),
                        solution = """
                            package main

                            import "fmt"

                            func main() {
                                name := "Gopher"
                                age := 5
                                msg := fmt.Sprintf("%s 今年 %d 岁", name, age)
                                fmt.Println(msg)
                            }
                        """,
                        check = exercise_check(expected_output = "Gopher 今年 5 岁", require_source = listOf("Sprintf", "%s", "%d"))
                    ),
                    learn_step.quiz(
                        "go-strings-q", "小测",
                        prompt = "把整数放进格式化字符串，用哪个动词？",
                        options = listOf("%s", "%d", "%f", "没有对应动词"),
                        correct_index = 1,
                        explanation = "%d 对应整数；%s 字符串、%f 浮点、%v 通用。"
                    )
                )
            ),
            learn_lesson(
                id = "go-defer", title = "defer 与 panic", summary = "延迟执行与崩溃恢复。", est_minutes = 5,
                steps = listOf(
                    learn_step.concept(
                        "go-defer-c", "defer：离开函数前必做",
                        listOf(
                            text("`defer` 把一个调用推迟到**函数返回前**执行，常用于关文件、解锁等收尾工作。多个 defer **后进先出**（像栈一样）："),
                            code(
                                """
                                func work() {
                                    defer fmt.Println("第一个 defer（最后执行）")
                                    defer fmt.Println("第二个 defer（先执行）")
                                    fmt.Println("正常逻辑")
                                }
                                // 输出：正常逻辑 → 第二个 defer → 第一个 defer
                                """
                            ),
                            text("`panic` 会让程序直接崩溃；`recover` 只能在 defer 的函数里调用，能把程序拉回来："),
                            code(
                                """
                                func safe() {
                                    defer func() {
                                        if r := recover(); r != nil {
                                            fmt.Println("恢复了:", r)
                                        }
                                    }()
                                    panic("出大事了")
                                }
                                """
                            ),
                            note("日常代码请用 error 而不是 panic；recover 主要用在库的边界或必须兜底的地方。")
                        )
                    ),
                    learn_step.interactive(
                        "go-defer-i", "逆序打印",
                        listOf(text("在循环里用 `defer` 打印 `1` 到 `3`，循环结束后打印 `开始`。观察 defer 的执行顺序，输出应为四行：\n\n`开始`\n`3`\n`2`\n`1`")),
                        starter_code = """
                            package main

                            import "fmt"

                            func main() {
                                // 1. for 循环 i 从 1 到 3，循环体里 defer fmt.Println(i)
                                // 2. 循环之后打印 开始
                            }
                        """,
                        hints = listOf(
                            "defer fmt.Println(i) 写在循环体内",
                            "fmt.Println(\"开始\") 写在循环之后"
                        ),
                        solution = """
                            package main

                            import "fmt"

                            func main() {
                                for i := 1; i <= 3; i++ {
                                    defer fmt.Println(i)
                                }
                                fmt.Println("开始")
                            }
                        """,
                        check = exercise_check(expected_output = "开始\n3\n2\n1", require_source = listOf("defer", "for"))
                    )
                )
            ),
            learn_lesson(
                id = "go-files", title = "文件读写", summary = "os.ReadFile、os.WriteFile 与逐行扫描。", est_minutes = 6,
                steps = listOf(
                    learn_step.concept(
                        "go-files-c", "一次读写整个文件",
                        listOf(
                            text("小文件用 `os.ReadFile` / `os.WriteFile` 一步到位，返回的是**字节切片**（`[]byte`）："),
                            code(
                                """
                                // 写文件：内容 + 权限
                                err := os.WriteFile("note.txt", []byte("你好"), 0644)

                                // 读文件
                                data, err := os.ReadFile("note.txt")
                                fmt.Println(string(data))  // 你好
                                """
                            ),
                            text("两个函数都返回 `error`，用之前先判断。第三个参数 `0644` 是文件权限——「自己可读写、别人只读」，新手阶段照抄这个数就行，不影响你读写自己写的文件。逐行处理大文件时用 `bufio.Scanner`："),
                            code(
                                """
                                f, err := os.Open("note.txt")
                                if err != nil {
                                    return
                                }
                                defer f.Close()

                                scanner := bufio.NewScanner(f)
                                for scanner.Scan() {
                                    fmt.Println(scanner.Text())  // 每次一行
                                }
                                """
                            ),
                            tip("相对路径相对程序运行目录。在 GoStudio 里点「运行」时就是项目目录，写出的文件会出现在项目文件树里。")
                        )
                    ),
                    learn_step.interactive(
                        "go-files-i", "写完再读",
                        listOf(text("把字符串 `hello gostudio` 写入 `hello.txt`，再读回来原样打印。")),
                        starter_code = """
                            package main

                            import (
                                "fmt"
                                "os"
                            )

                            func main() {
                                // 1. os.WriteFile 写入 hello.txt
                                // 2. os.ReadFile 读回来
                                // 3. fmt.Println(string(data))
                            }
                        """,
                        hints = listOf(
                            "os.WriteFile(\"hello.txt\", []byte(\"hello gostudio\"), 0644)",
                            "data, err := os.ReadFile(\"hello.txt\")，判断 err 后打印 string(data)"
                        ),
                        solution = """
                            package main

                            import (
                                "fmt"
                                "os"
                            )

                            func main() {
                                if err := os.WriteFile("hello.txt", []byte("hello gostudio"), 0644); err != nil {
                                    fmt.Println("写入失败:", err)
                                    return
                                }
                                data, err := os.ReadFile("hello.txt")
                                if err != nil {
                                    fmt.Println("读取失败:", err)
                                    return
                                }
                                fmt.Println(string(data))
                            }
                        """,
                        check = exercise_check(expected_output = "hello gostudio", require_source = listOf("os.WriteFile", "os.ReadFile"))
                    ),
                    learn_step.quiz(
                        "go-files-q", "小测",
                        prompt = "os.ReadFile 返回的是什么？",
                        options = listOf("字符串和 error", "字节切片和 error", "文件对象", "io.Reader"),
                        correct_index = 1,
                        explanation = "返回 ([]byte, error)，要当字符串用得先 string(data) 转一下。"
                    )
                )
            ),
            learn_lesson(
                id = "go-packages", title = "包与模块", summary = "package、import、可见性与 go.mod。", est_minutes = 5,
                steps = listOf(
                    learn_step.concept(
                        "go-packages-c", "项目是怎么组织的",
                        listOf(
                            text("每个 `.go` 文件开头用 `package` 声明自己属于哪个包，**同目录的文件必须同包**。要用其它包的代码，先 `import`："),
                            code(
                                """
                                package main

                                import (
                                    "fmt"     // 标准库
                                    "strings" // 标准库

                                    "myapp/util" // 自己项目里的包
                                )
                                """
                            ),
                            text("**可见性规则只有一条**：名字大写开头 = 导出（其它包可用），小写开头 = 私有（只限本包）："),
                            code(
                                """
                                package util

                                func Hello() string { return "hi" } // 外部可用
                                func helper() {}                     // 只限本包
                                """
                            ),
                            text("`go.mod` 是项目的身份证，记录模块名和依赖版本。新建项目时 GoStudio 会自动生成；代码里用了新的第三方包后运行一次，依赖会自动下载并记录进去。"),
                            note("在 GoStudio 里给项目加新文件：同目录的 `.go` 文件写上同样的 package 名，就能直接互相调用，不需要改任何配置。")
                        )
                    ),
                    learn_step.quiz(
                        "go-packages-q", "小测",
                        prompt = "别的包要用你写的函数，函数名必须？",
                        options = listOf("小写字母开头", "大写字母开头", "加 public 关键字", "登记进 go.mod"),
                        correct_index = 1,
                        explanation = "Go 用首字母大小写决定导出：大写导出、小写私有，没有 public/private 关键字。"
                    )
                )
            ),
        )
    )

    // ================= Go 并发 =================

    private fun go_concurrency() = learn_track(
        id = "go-conc", title = "Go 并发", subtitle = "goroutine、channel、WaitGroup、select 与锁",
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
            ),
            learn_lesson(
                id = "go-select", title = "select 与定时器", summary = "同时等多个 channel，等不到就超时。", est_minutes = 7,
                steps = listOf(
                    learn_step.concept(
                        "go-select-c", "select：多路等待",
                        listOf(
                            text("`select` 长得像 `switch`，但专门用来等 channel——哪个 case 先等到数据就执行哪个，都没数据就一起等："),
                            code(
                                """
                                select {
                                case msg := <-ch1:
                                    fmt.Println("收到", msg)
                                case ch2 <- "hello":
                                    fmt.Println("发送成功")
                                }
                                """
                            ),
                            text("配合 `time.After`（在指定时长后返回一个就绪的 channel）实现超时控制，是并发程序的标配写法："),
                            code(
                                """
                                select {
                                case msg := <-ch:
                                    fmt.Println("收到:", msg)
                                case <-time.After(100 * time.Millisecond):
                                    fmt.Println("超时")
                                }
                                """
                            ),
                            tip("`time.Ticker` 按固定间隔反复触发，适合定时任务：`for range ticker.C { ... }`，用完记得 `ticker.Stop()`。")
                        )
                    ),
                    learn_step.interactive(
                        "go-select-i", "等不到就超时",
                        listOf(text("创建一个**从不放数据**的 channel，用 `select` 等它或等 100 毫秒超时，超时后打印 `超时`。")),
                        starter_code = """
                            package main

                            import (
                                "fmt"
                                "time"
                            )

                            func main() {
                                ch := make(chan string)
                                // 用 select 等 ch 或 time.After(100 * time.Millisecond)
                                // 超时分支打印 超时
                            }
                        """,
                        hints = listOf(
                            "case <-time.After(100 * time.Millisecond):",
                            "另一个 case 从 ch 接收，但它永远不会就绪"
                        ),
                        solution = """
                            package main

                            import (
                                "fmt"
                                "time"
                            )

                            func main() {
                                ch := make(chan string)
                                select {
                                case msg := <-ch:
                                    fmt.Println("收到:", msg)
                                case <-time.After(100 * time.Millisecond):
                                    fmt.Println("超时")
                                }
                            }
                        """,
                        check = exercise_check(expected_output = "超时", require_source = listOf("select {", "time.After"))
                    ),
                    learn_step.quiz(
                        "go-select-q", "小测",
                        prompt = "select 的多个 case 同时就绪时会怎样？",
                        options = listOf("按书写顺序执行第一个", "随机挑一个执行", "两个都执行", "编译报错"),
                        correct_index = 1,
                        explanation = "Go 故意随机选择，防止代码依赖 case 顺序。"
                    )
                )
            ),
            learn_lesson(
                id = "go-mutex", title = "互斥锁", summary = "sync.Mutex 防止数据竞争。", est_minutes = 7,
                steps = listOf(
                    learn_step.concept(
                        "go-mutex-c", "竞争与加锁",
                        listOf(
                            text("多个 goroutine 同时读写同一个变量，结果会不可预测——这叫**数据竞争**。比如 1000 个 goroutine 各执行一次 `n++`，最后 `n` 往往不是 1000，因为「读-改-写」会互相踩踏。"),
                            text("`sync.Mutex`（互斥锁）保证同一时刻只有一个 goroutine 进入临界区："),
                            code(
                                """
                                var (
                                    mu sync.Mutex
                                    n  int
                                )

                                mu.Lock()
                                n++      // 临界区：同一时刻只有一个 goroutine 在这
                                mu.Unlock()
                                """
                            ),
                            tip("习惯写法：拿到锁之后立刻 defer mu.Unlock()，函数再长也不会忘记解锁。")
                        )
                    ),
                    learn_step.interactive(
                        "go-mutex-i", "安全数到一千",
                        listOf(text("启动 1000 个 goroutine 各执行一次 `n++`，用 `sync.Mutex` 保护计数，全部结束后打印 `1000`。脚手架里的 `var ( ... )` 只是把多个变量声明合在一起写的分组写法。")),
                        starter_code = """
                            package main

                            import (
                                "fmt"
                                "sync"
                            )

                            func main() {
                                var (
                                    mu sync.Mutex
                                    wg sync.WaitGroup
                                    n  int
                                )

                                for i := 0; i < 1000; i++ {
                                    wg.Add(1)
                                    go func() {
                                        defer wg.Done()
                                        // 加锁后 n++，再解锁
                                    }()
                                }

                                wg.Wait()
                                fmt.Println(n)
                            }
                        """,
                        hints = listOf(
                            "在 goroutine 里：mu.Lock() → n++ → mu.Unlock()",
                            "也可以 mu.Lock() 之后立刻 defer mu.Unlock()，再加 n++"
                        ),
                        solution = """
                            package main

                            import (
                                "fmt"
                                "sync"
                            )

                            func main() {
                                var (
                                    mu sync.Mutex
                                    wg sync.WaitGroup
                                    n  int
                                )

                                for i := 0; i < 1000; i++ {
                                    wg.Add(1)
                                    go func() {
                                        defer wg.Done()
                                        mu.Lock()
                                        n++
                                        mu.Unlock()
                                    }()
                                }

                                wg.Wait()
                                fmt.Println(n)
                            }
                        """,
                        check = exercise_check(expected_output = "1000", require_source = listOf("mu.Lock", "mu.Unlock", "wg.Wait"))
                    )
                )
            ),
        )
    )
}
