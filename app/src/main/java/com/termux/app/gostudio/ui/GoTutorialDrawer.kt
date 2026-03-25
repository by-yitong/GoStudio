package com.termux.app.gostudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ==================== Data Classes ====================

data class TutorialItem(
    val title: String,
    val description: String,
    val codeExamples: List<CodeExample>
)

data class CodeExample(
    val title: String,
    val code: String
)

data class TutorialCategory(
    val name: String,
    val items: List<TutorialItem>
)

// ==================== Tutorial Data ====================

val goTutorialCategories: List<TutorialCategory> = listOf(
    TutorialCategory("基础语法", listOf(
        TutorialItem("变量与常量", "Go 语言使用 var 声明变量，const 声明常量。支持类型推断和短变量声明 := 。", listOf(
            CodeExample("变量声明", """package main

import "fmt"

func main() {
    // var 声明变量
    var name string = "Go"
    var age int = 10

    // 类型推断
    var version = 1.22

    // 短变量声明（最常用）
    lang := "Golang"

    fmt.Printf("语言: %s, 年龄: %d, 版本: %.2f\n", name, age, float64(version))
    fmt.Println("简写:", lang)
}"""),
            CodeExample("常量与 iota", """package main

import "fmt"

const (
    Sunday = iota  // 0
    Monday         // 1
    Tuesday        // 2
    Wednesday      // 3
    Thursday       // 4
    Friday         // 5
    Saturday       // 6
)

const (
    _  = iota             // 0，丢弃
    KB = 1 << (10 * iota) // 1 << 10 = 1024
    MB                    // 1 << 20
    GB                    // 1 << 30
)

func main() {
    fmt.Println("今天是星期:", Monday)
    fmt.Printf("1MB = %d 字节\n", MB)
}""")
        )),
        TutorialItem("数据类型", "Go 是静态类型语言，基本类型包括 bool、int、float、string、complex 等。", listOf(
            CodeExample("基本数据类型", """package main

import "fmt"

func main() {
    // 布尔类型
    var isActive bool = true

    // 整数类型
    var a int = 42
    var b uint = 100  // 无符号
    var c int64 = 9999999999

    // 浮点类型
    var pi float64 = 3.14159

    // 字符串
    var msg string = "Hello, Go!"

    // 字节与 rune
    var ch rune = '中'  // rune = int32

    fmt.Printf("类型: %T, 值: %v\n", a, a)
    fmt.Printf("类型: %T, 值: %v\n", pi, pi)
    fmt.Printf("类型: %T, 值: %c\n", ch, ch)
    fmt.Println("字符串:", msg, "布尔:", isActive)
}"""),
            CodeExample("类型转换", """package main

import (
    "fmt"
    "strconv"
)

func main() {
    // 显式类型转换（Go 不会隐式转换）
    var i int = 42
    var f float64 = float64(i)
    var u uint = uint(f)

    fmt.Printf("int -> float64: %v\n", f)
    fmt.Printf("float64 -> uint: %v\n", u)

    // 字符串转换
    str := strconv.Itoa(i)       // int -> string
    num, _ := strconv.Atoi(str)  // string -> int
    fmt.Printf("int->str: %q, str->int: %d\n", str, num)

    // float -> string
    piStr := strconv.FormatFloat(3.14, 'f', 2, 64)
    fmt.Println("float->str:", piStr)
}""")
        )),
        TutorialItem("运算符", "Go 支持算术、比较、逻辑、位运算等常见运算符。", listOf(
            CodeExample("常用运算符", """package main

import "fmt"

func main() {
    a, b := 10, 3
    // 算术运算
    fmt.Printf("%d + %d = %d\n", a, b, a+b)
    fmt.Printf("%d * %d = %d\n", a, b, a*b)
    fmt.Printf("%d / %d = %d\n", a, b, a/b)
    fmt.Printf("%d %% %d = %d\n", a, b, a%b)

    // 比较运算
    fmt.Println(a == b)  // false
    fmt.Println(a != b)  // true
    fmt.Println(a > b)   // true

    // 逻辑运算
    fmt.Println(true && false)  // false
    fmt.Println(true || false)  // true
    fmt.Println(!true)          // false

    // 位运算
    fmt.Printf("a & b = %d\n", a&b)   // 2
    fmt.Printf("a | b = %d\n", a|b)   // 11
    fmt.Printf("a ^ b = %d\n", a^b)   // 9
    fmt.Printf("a << 1 = %d\n", a<<1) // 20
}""")
        )),
        TutorialItem("字符串操作", "Go 字符串是不可变的字节序列，使用 strings 和 strconv 包进行操作。", listOf(
            CodeExample("strings 包常用函数", """package main

import (
    "fmt"
    "strings"
)

func main() {
    s := "Hello, Go Language!"

    // 查找和包含
    fmt.Println("包含 'Go':", strings.Contains(s, "Go"))
    fmt.Println("索引 'Go':", strings.Index(s, "Go"))
    fmt.Println("以 Hello 开头:", strings.HasPrefix(s, "Hello"))
    fmt.Println("以 ! 结尾:", strings.HasSuffix(s, "!"))

    // 分割和连接
    parts := strings.Split(s, ", ")
    fmt.Println("分割:", parts)
    joined := strings.Join(parts, " | ")
    fmt.Println("连接:", joined)

    // 替换和修剪
    fmt.Println("替换:", strings.ReplaceAll(s, "Go", "Golang"))
    fmt.Println("修剪:", strings.TrimSpace("  hello  "))

    // 大小写
    fmt.Println("转大写:", strings.ToUpper(s))
    fmt.Println("转小写:", strings.ToLower(s))
}"""),
            CodeExample("字符串遍历与拼接", """package main

import (
    "fmt"
    "strings"
)

func main() {
    // 使用 strings.Builder 高效拼接
    var builder strings.Builder
    for i := 0; i < 5; i++ {
        builder.WriteString("Go ")
    }
    fmt.Println("拼接结果:", builder.String())

    // 遍历字符串（按 rune 遍历，支持中文）
    str := "你好Go"
    for i, r := range str {
        fmt.Printf("索引 %d: %c\n", i, r)
    }

    // 字符串长度
    fmt.Println("字节长度:", len(str))         // 8（UTF-8编码）
    fmt.Println("字符数量:", len([]rune(str))) // 4
}""")
        ))
    )),

    TutorialCategory("流程控制", listOf(
        TutorialItem("if/else", "Go 的 if/else 不需要括号，但大括号是必须的。支持初始化语句。", listOf(
            CodeExample("if/else 用法", """package main

import "fmt"

func main() {
    score := 85

    // 基本用法
    if score >= 90 {
        fmt.Println("优秀")
    } else if score >= 80 {
        fmt.Println("良好")
    } else if score >= 60 {
        fmt.Println("及格")
    } else {
        fmt.Println("不及格")
    }

    // if 初始化语句（作用域限于 if 块内）
    if num := 42; num%2 == 0 {
        fmt.Println(num, "是偶数")
    }
}""")
        )),
        TutorialItem("for 循环", "Go 只有一种循环关键字 for，但可以实现多种循环模式。", listOf(
            CodeExample("for 的多种写法", """package main

import "fmt"

func main() {
    // 经典 for 循环
    for i := 0; i < 5; i++ {
        fmt.Printf("i = %d\n", i)
    }

    // 类似 while 的 for
    n := 10
    for n > 0 {
        fmt.Print(n, " ")
        n /= 2
    }
    fmt.Println()

    // 无限循环 + break
    count := 0
    for {
        count++
        if count >= 3 {
            break
        }
        fmt.Println("无限循环中:", count)
    }

    // for-range 遍历
    fruits := []string{"苹果", "香蕉", "橙子"}
    for index, fruit := range fruits {
        fmt.Printf("第%d个: %s\n", index, fruit)
    }
}""")
        )),
        TutorialItem("switch", "Go 的 switch 默认不穿透（不需要 break），支持多种类型匹配。", listOf(
            CodeExample("switch 用法", """package main

import "fmt"

func main() {
    // 基本 switch
    day := "Wednesday"
    switch day {
    case "Monday", "Tuesday", "Wednesday", "Thursday", "Friday":
        fmt.Println("工作日")
    case "Saturday", "Sunday":
        fmt.Println("周末")
    default:
        fmt.Println("未知")
    }

    // 无表达式的 switch（类似 if-else）
    hour := 14
    switch {
    case hour < 12:
        fmt.Println("上午")
    case hour < 18:
        fmt.Println("下午")
    default:
        fmt.Println("晚上")
    }

    // type switch
    checkType(42)
    checkType("hello")
}

func checkType(v interface{}) {
    switch val := v.(type) {
    case int:
        fmt.Printf("整数: %d\n", val)
    case string:
        fmt.Printf("字符串: %s\n", val)
    default:
        fmt.Printf("其他类型: %T\n", val)
    }
}""")
        )),
        TutorialItem("defer/panic/recover", "defer 延迟执行，panic 触发运行时错误，recover 捕获 panic。", listOf(
            CodeExample("defer 与 panic/recover", """package main

import "fmt"

func main() {
    // defer: 函数返回前执行（LIFO 顺序）
    defer fmt.Println("第1个 defer")
    defer fmt.Println("第2个 defer")
    defer fmt.Println("第3个 defer")
    fmt.Println("正常执行")

    // panic 与 recover
    safeDivide(10, 0)
    fmt.Println("程序继续运行")

    // 使用 recover 恢复 panic
    result, err := safeDivide2(10, 0)
    if err != nil {
        fmt.Println("错误:", err)
    } else {
        fmt.Println("结果:", result)
    }
}

func safeDivide(a, b int) {
    defer func() {
        if r := recover(); r != nil {
            fmt.Println("捕获到 panic:", r)
        }
    }()
    fmt.Printf("%d / %d = %d\n", a, b, a/b)
}

func safeDivide2(a, b int) (result int, err error) {
    defer func() {
        if r := recover(); r != nil {
            err = fmt.Errorf("%v", r)
        }
    }()
    if b == 0 {
        panic("除数不能为零")
    }
    return a / b, nil
}""")
        ))
    )),

    TutorialCategory("函数", listOf(
        TutorialItem("函数定义", "Go 使用 func 关键字定义函数，支持命名返回值。", listOf(
            CodeExample("函数定义", """package main

import "fmt"

// 无返回值
func sayHello(name string) {
    fmt.Printf("你好, %s!\n", name)
}

// 单个返回值
func add(a int, b int) int {
    return a + b
}

// 命名返回值（自动返回）
func divide(a, b float64) (result float64, err error) {
    if b == 0 {
        err = fmt.Errorf("除数不能为零")
        return // 自动返回 result(0) 和 err
    }
    result = a / b
    return // 自动返回 result 和 err(nil)
}

func main() {
    sayHello("Go")
    fmt.Println("3 + 5 =", add(3, 5))

    r, e := divide(10, 3)
    if e != nil {
        fmt.Println("错误:", e)
    } else {
        fmt.Printf("10 / 3 = %.2f\n", r)
    }
}""")
        )),
        TutorialItem("多返回值", "Go 函数可以返回多个值，通常用于返回结果和错误。", listOf(
            CodeExample("多返回值", """package main

import (
    "errors"
    "fmt"
)

func divMod(a, b int) (int, int, error) {
    if b == 0 {
        return 0, 0, errors.New("除数不能为零")
    }
    return a / b, a % b, nil
}

func getCoords() (float64, float64) {
    return 39.9042, 116.4074
}

func main() {
    q, r, err := divMod(17, 5)
    if err != nil {
        fmt.Println("错误:", err)
    } else {
        fmt.Printf("17 ÷ 5 = %d 余 %d\n", q, r)
    }

    lat, _ := getCoords()
    fmt.Printf("纬度: %.4f\n", lat)
}""")
        )),
        TutorialItem("匿名函数与闭包", "Go 支持匿名函数和闭包，闭包可以捕获外部变量。", listOf(
            CodeExample("匿名函数与闭包", """package main

import "fmt"

func main() {
    // 匿名函数直接调用
    result := func(a, b int) int {
        return a + b
    }(3, 5)
    fmt.Println("3 + 5 =", result)

    // 赋值给变量
    add := func(a, b int) int { return a + b }
    fmt.Println("10 + 20 =", add(10, 20))

    // 闭包：捕获外部变量
    counter := makeCounter()
    fmt.Println("计数:", counter()) // 1
    fmt.Println("计数:", counter()) // 2
    fmt.Println("计数:", counter()) // 3
}

func makeCounter() func() int {
    count := 0
    return func() int {
        count++
        return count
    }
}""")
        )),
        TutorialItem("可变参数", "使用 ... 类型表示可变参数，本质上是一个切片。", listOf(
            CodeExample("可变参数", """package main

import "fmt"

func sum(nums ...int) int {
    total := 0
    for _, n := range nums {
        total += n
    }
    return total
}

func greet(prefix string, names ...string) {
    for _, name := range names {
        fmt.Printf("%s, %s!\n", prefix, name)
    }
}

func main() {
    fmt.Println("求和:", sum(1, 2, 3))        // 6
    fmt.Println("求和:", sum(1, 2, 3, 4, 5)) // 15

    nums := []int{10, 20, 30}
    fmt.Println("求和:", sum(nums...)) // 60

    greet("你好", "Alice", "Bob", "Charlie")
}""")
        ))
    )),

    TutorialCategory("数据结构", listOf(
        TutorialItem("数组与切片", "数组长度固定，切片（slice）长度可变，是 Go 中最常用的数据结构。", listOf(
            CodeExample("数组", """package main

import "fmt"

func main() {
    var arr1 [5]int           // [0 0 0 0 0]
    arr2 := [3]string{"Go", "Rust", "Python"}
    arr3 := [...]int{1, 2, 3} // 编译器推断长度

    fmt.Println("arr1:", arr1)
    fmt.Println("arr2:", arr2)
    fmt.Println("arr3:", arr3)

    for i, v := range arr2 {
        fmt.Printf("索引 %d: %s\n", i, v)
    }

    // 多维数组
    matrix := [2][3]int{
        {1, 2, 3},
        {4, 5, 6},
    }
    fmt.Println("矩阵:", matrix)
}"""),
            CodeExample("切片（Slice）", """package main

import "fmt"

func main() {
    s1 := []int{1, 2, 3, 4, 5}
    s2 := make([]int, 3, 10) // 长度3，容量10

    fmt.Println("s1:", s1, "len:", len(s1), "cap:", cap(s1))

    // 切片操作（左闭右开）
    fmt.Println("s1[1:3]:", s1[1:3])
    fmt.Println("s1[:3]:", s1[:3])
    fmt.Println("s1[2:]:", s1[2:])

    // 追加元素
    s1 = append(s1, 6, 7, 8, 9)
    fmt.Println("追加后:", s1)

    // 删除元素（删除索引2）
    s1 = append(s1[:2], s1[3:]...)
    fmt.Println("删除后:", s1)

    // 切片拷贝
    dst := make([]int, 3)
    copy(dst, []int{1, 2, 3})
    fmt.Println("拷贝:", dst)
}""")
        )),
        TutorialItem("Map", "Map 是键值对的无序集合，使用 make 创建或字面量初始化。", listOf(
            CodeExample("Map 基本操作", """package main

import "fmt"

func main() {
    ages := map[string]int{
        "Alice":   30,
        "Bob":     25,
        "Charlie": 35,
    }
    scores := make(map[string]float64)

    // 增删改查
    scores["数学"] = 95.5
    scores["英语"] = 88.0
    scores["数学"] = 98.0 // 修改

    // 通过 ok 判断 key 是否存在
    if age, ok := ages["Alice"]; ok {
        fmt.Println("Alice 年龄:", age)
    }

    delete(ages, "Bob")

    for name, age := range ages {
        fmt.Printf("%s: %d 岁\n", name, age)
    }
    fmt.Println("scores 长度:", len(scores))
}""")
        )),
        TutorialItem("结构体", "结构体是 Go 中面向对象编程的基础，支持嵌套和方法。", listOf(
            CodeExample("结构体定义与使用", """package main

import "fmt"

type Person struct {
    Name string
    Age  int
    City string
}

func (p Person) Greet() string {
    return fmt.Sprintf("我是 %s，今年 %d 岁", p.Name, p.Age)
}

func (p *Person) SetCity(city string) {
    p.City = city
}

// 结构体嵌套
type Employee struct {
    Person     // 匿名嵌套
    Department string
    Salary     float64
}

func main() {
    p1 := Person{Name: "张三", Age: 28, City: "北京"}
    fmt.Println(p1.Greet())
    p1.SetCity("深圳")
    fmt.Printf("搬到: %s\n", p1.City)

    emp := Employee{
        Person:     Person{Name: "王五", Age: 25, City: "广州"},
        Department: "技术部",
        Salary:     15000.0,
    }
    fmt.Printf("%s 在 %s 工作\n", emp.Name, emp.Department)
}""")
        )),
        TutorialItem("指针", "Go 指针用于直接操作内存地址，但比 C 语言更安全（不支持指针运算）。", listOf(
            CodeExample("指针基本操作", """package main

import "fmt"

func main() {
    x := 42
    p := &x
    fmt.Println("x 的值:", x)
    fmt.Println("x 的地址:", p)
    fmt.Println("通过指针访问:", *p)

    *p = 100
    fmt.Println("修改后 x:", x)

    // 指针作为函数参数
    a, b := 10, 20
    swap(&a, &b)
    fmt.Printf("交换后: a=%d, b=%d\n", a, b)

    ptr := new(int)
    *ptr = 99
    fmt.Println("new 创建:", *ptr)
}

func swap(a, b *int) {
    *a, *b = *b, *a
}""")
        ))
    )),

    TutorialCategory("接口与类型", listOf(
        TutorialItem("接口定义", "Go 接口是隐式实现的，只要类型实现了接口的所有方法，就自动满足接口。", listOf(
            CodeExample("接口定义与实现", """package main

import "fmt"

type Speaker interface {
    Speak() string
}

type ReadWriter interface {
    Speaker
    Write(content string)
}

type Dog struct{ Name string }
func (d Dog) Speak() string {
    return d.Name + ": 汪汪!"
}

type File struct{ content string }
func (f *File) Speak() string {
    return "文件内容: " + f.content
}
func (f *File) Write(s string) {
    f.content = s
}

func main() {
    var s Speaker = Dog{Name: "旺财"}
    fmt.Println(s.Speak())

    if dog, ok := s.(Dog); ok {
        fmt.Println("狗的名字:", dog.Name)
    }

    var rw ReadWriter = &File{content: "Hello"}
    fmt.Println(rw.Speak())
    rw.Write("World")
    fmt.Println(rw.Speak())
}""")
        )),
        TutorialItem("类型断言", "类型断言用于将接口值转换为具体类型，分为安全和非安全两种方式。", listOf(
            CodeExample("类型断言", """package main

import "fmt"

func describe(i interface{}) {
    switch v := i.(type) {
    case int:
        fmt.Printf("整数: %d\n", v)
    case string:
        fmt.Printf("字符串(长度%d): %s\n", len(v), v)
    case bool:
        fmt.Printf("布尔值: %v\n", v)
    default:
        fmt.Printf("未知类型: %T\n", v)
    }
}

func main() {
    describe(42)
    describe("Hello Go")
    describe(true)

    // comma-ok 断言
    var i interface{} = "hello"
    if s, ok := i.(string); ok {
        fmt.Println("是字符串:", s)
    }
}""")
        )),
        TutorialItem("类型嵌入", "Go 通过嵌入结构体实现类似继承的功能，嵌入的方法会被提升。", listOf(
            CodeExample("类型嵌入", """package main

import "fmt"

type Animal struct {
    Name string
}

func (a Animal) Eat() {
    fmt.Println(a.Name, "在吃东西")
}

type Pet struct {
    Animal      // 嵌入 Animal
    Owner string
}

// 覆盖嵌入的方法
func (p Pet) Eat() {
    fmt.Println(p.Name, "在吃宠物粮")
}

func main() {
    pet := Pet{
        Animal: Animal{Name: "小猫"},
        Owner:  "Alice",
    }

    pet.Eat()     // 调用覆盖的方法
    pet.Animal.Eat() // 调用嵌入的原始方法
    fmt.Println("主人:", pet.Owner)
    fmt.Println("名字:", pet.Name) // 提升的字段
}""")
        ))
    )),

    TutorialCategory("错误处理", listOf(
        TutorialItem("error 接口", "Go 使用 error 接口处理错误，函数通常返回 error 作为最后一个返回值。", listOf(
            CodeExample("error 接口", """package main

import (
    "errors"
    "fmt"
)

func divide(a, b float64) (float64, error) {
    if b == 0 {
        return 0, errors.New("除数不能为零")
    }
    return a / b, nil
}

func main() {
    result, err := divide(10, 3)
    if err != nil {
        fmt.Println("错误:", err)
        return
    }
    fmt.Printf("结果: %.2f\n", result)

    _, err = divide(10, 0)
    if err != nil {
        fmt.Println("错误:", err)
    }
}""")
        )),
        TutorialItem("自定义错误", "可以通过实现 error 接口创建自定义错误类型，携带更多上下文信息。", listOf(
            CodeExample("自定义错误", """package main

import (
    "fmt"
    "time"
)

// 自定义错误类型
type TimeoutError struct {
    Operation string
    Duration  time.Duration
}

func (e *TimeoutError) Error() string {
    return fmt.Sprintf("%s 超时 (%v)", e.Operation, e.Duration)
}

func fetchData(timeout time.Duration) (string, error) {
    if timeout < 2*time.Second {
        return "", &TimeoutError{
            Operation: "fetchData",
            Duration:  timeout,
        }
    }
    return "数据内容", nil
}

func main() {
    data, err := fetchData(1 * time.Second)
    if err != nil {
        // 类型断言获取详细信息
        if te, ok := err.(*TimeoutError); ok {
            fmt.Printf("超时详情: %s, 耗时: %v\n", te.Operation, te.Duration)
        }
        return
    }
    fmt.Println("获取数据:", data)
}""")
        )),
        TutorialItem("errors 包", "errors 包提供了创建和操作错误的工具函数，Go 1.13+ 支持 errors.Is 和 errors.As。", listOf(
            CodeExample("errors 包", """package main

import (
    "errors"
    "fmt"
)

var ErrNotFound = errors.New("资源未找到")
var ErrPermission = errors.New("权限不足")

func findUser(id int) (string, error) {
    if id <= 0 {
        return "", fmt.Errorf("findUser: %w", ErrNotFound)
    }
    if id == 403 {
        return "", fmt.Errorf("findUser: %w", ErrPermission)
    }
    return fmt.Sprintf("用户%d", id), nil
}

func main() {
    _, err := findUser(-1)
    if err != nil {
        // errors.Is 判断错误链中是否包含目标错误
        if errors.Is(err, ErrNotFound) {
            fmt.Println("处理未找到的情况")
        }
    }

    _, err = findUser(403)
    if err != nil {
        if errors.Is(err, ErrPermission) {
            fmt.Println("处理权限不足的情况")
        }
        // errors.Unwrap 解包错误
        fmt.Println("解包:", errors.Unwrap(err))
    }
}""")
        ))
    )),

    TutorialCategory("并发编程", listOf(
        TutorialItem("goroutine", "goroutine 是 Go 的轻量级线程，使用 go 关键字启动，由 Go 运行时调度。", listOf(
            CodeExample("goroutine 基本用法", """package main

import (
    "fmt"
    "sync"
    "time"
)

func say(s string, wg *sync.WaitGroup) {
    defer wg.Done()
    for i := 0; i < 3; i++ {
        fmt.Println(s, i)
        time.Sleep(100 * time.Millisecond)
    }
}

func main() {
    var wg sync.WaitGroup
    wg.Add(2)

    go say("世界", &wg) // 启动 goroutine
    go say("你好", &wg) // 启动 goroutine

    wg.Wait() // 等待所有 goroutine 完成
    fmt.Println("主 goroutine 结束")
}"""),
            CodeExample("匿名 goroutine", """package main

import (
    "fmt"
    "time"
)

func main() {
    // 匿名函数启动 goroutine
    go func() {
        fmt.Println("匿名 goroutine 启动")
        time.Sleep(200 * time.Millisecond)
        fmt.Println("匿名 goroutine 结束")
    }()

    fmt.Println("主函数继续执行")
    time.Sleep(500 * time.Millisecond) // 等待 goroutine
}""")
        )),
        TutorialItem("channel", "channel 用于 goroutine 之间的通信，遵循「不要通过共享内存来通信」的理念。", listOf(
            CodeExample("channel 基本用法", """package main

import "fmt"

func main() {
    // 创建无缓冲 channel
    ch := make(chan string)

    // goroutine 发送数据
    go func() {
        ch <- "Hello"
        ch <- "from"
        ch <- "channel"
        close(ch) // 关闭 channel
    }()

    // 接收数据
    for msg := range ch {
        fmt.Println(msg)
    }

    // 带缓冲的 channel
    buffered := make(chan int, 3)
    buffered <- 1
    buffered <- 2
    buffered <- 3
    // buffered <- 4 // 会阻塞，因为缓冲区已满

    fmt.Println(<-buffered) // 1
    fmt.Println(<-buffered) // 2
    fmt.Println(<-buffered) // 3
}"""),
            CodeExample("channel 方向", """package main

import "fmt"

// 只发送 channel
func sender(ch chan<- string) {
    ch <- "只发送"
}

// 只接收 channel
func receiver(ch <-chan string) {
    msg := <-ch
    fmt.Println("收到:", msg)
}

func main() {
    ch := make(chan string, 1)
    sender(ch)
    receiver(ch)
}""")
        )),
        TutorialItem("select", "select 用于处理多个 channel 操作，类似 switch 但专门用于 channel。", listOf(
            CodeExample("select 用法", """package main

import (
    "fmt"
    "time"
)

func main() {
    ch1 := make(chan string)
    ch2 := make(chan string)

    go func() {
        time.Sleep(100 * time.Millisecond)
        ch1 <- "来自 ch1"
    }()

    go func() {
        time.Sleep(200 * time.Millisecond)
        ch2 <- "来自 ch2"
    }()

    for i := 0; i < 2; i++ {
        select {
        case msg1 := <-ch1:
            fmt.Println("收到:", msg1)
        case msg2 := <-ch2:
            fmt.Println("收到:", msg2)
        case <-time.After(500 * time.Millisecond):
            fmt.Println("超时")
        }
    }
}""")
        )),
        TutorialItem("sync 包", "sync 包提供了互斥锁、等待组、原子操作等并发原语。", listOf(
            CodeExample("sync.Mutex 与 sync.WaitGroup", """package main

import (
    "fmt"
    "sync"
)

// 线程安全的计数器
type SafeCounter struct {
    mu    sync.Mutex
    value int
}

func (c *SafeCounter) Increment() {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.value++
}

func (c *SafeCounter) Value() int {
    c.mu.Lock()
    defer c.mu.Unlock()
    return c.value
}

func main() {
    counter := &SafeCounter{}
    var wg sync.WaitGroup

    for i := 0; i < 100; i++ {
        wg.Add(1)
        go func() {
            defer wg.Done()
            counter.Increment()
        }()
    }

    wg.Wait()
    fmt.Println("计数器:", counter.Value()) // 100
}"""),
            CodeExample("sync.Once 与 sync.Map", """package main

import (
    "fmt"
    "sync"
)

var instance *Config
var once sync.Once

type Config struct {
    Data string
}

func GetInstance() *Config {
    once.Do(func() {
        fmt.Println("初始化单例...")
        instance = &Config{Data: "配置数据"}
    })
    return instance
}

func main() {
    var wg sync.WaitGroup
    for i := 0; i < 5; i++ {
        wg.Add(1)
        go func() {
            defer wg.Done()
            cfg := GetInstance()
            fmt.Println("获取:", cfg.Data)
        }()
    }
    wg.Wait()

    // sync.Map（并发安全的 map）
    var m sync.Map
    m.Store("Go", 2009)
    m.Store("Rust", 2015)

    if v, ok := m.Load("Go"); ok {
        fmt.Println("Go 发布于:", v)
    }
    m.Delete("Rust")
}""")
        ))
    )),

    TutorialCategory("包与模块", listOf(
        TutorialItem("包管理", "Go 使用 package 组织代码，同一个目录下的文件属于同一个包。", listOf(
            CodeExample("包的基本用法", """// 文件: mathutils/mathutils.go
package mathutils

// 大写字母开头的标识符才能被外部包访问
func Add(a, b int) int {
    return a + b
}

func Max(a, b int) int {
    if a > b {
        return a
    }
    return b
}

// 小写开头，仅包内可见
func helper() {
    fmt.Println("仅包内可见")
}

---
// 文件: main.go
package main

import (
    "fmt"
    "myproject/mathutils"
)

func main() {
    result := mathutils.Add(3, 5)
    fmt.Println("3 + 5 =", result)

    // mathutils.helper() // 编译错误：不可访问
}""")
        )),
        TutorialItem("go mod", "Go Modules 是 Go 1.16+ 的官方依赖管理工具，替代了 GOPATH 模式。", listOf(
            CodeExample("go mod 常用命令", """# 初始化模块
go mod init example.com/myproject

# 下载依赖
go mod download

# 整理依赖（移除未使用的）
go mod tidy

# 查看依赖
go list -m all

# 升级依赖
go get -u github.com/gin-gonic/gin

# 添加指定版本
go get github.com/gin-gonic/gin@v1.9.1

# vendor 模式（将依赖复制到 vendor 目录）
go mod vendor

---
// go.mod 文件示例
module example.com/myproject

go 1.22

require (
    github.com/gin-gonic/gin v1.9.1
   gorm.io/gorm v1.25.5
)""")
        )),
        TutorialItem("依赖管理", "Go Modules 支持语义化版本控制、依赖替换和排除。", listOf(
            CodeExample("依赖管理进阶", """// go.mod 中的依赖管理

module example.com/myproject

go 1.22

require github.com/gin-gonic/gin v1.9.1

// 替换：使用本地模块（开发时常用）
replace github.com/my/lib => ../mylib

// 排除：不使用某个有问题的版本
exclude github.com/bad/lib v1.0.0

// indirect: 间接依赖
---
// 常用命令

# 创建 go.sum（锁定依赖校验和）
go mod tidy

# 验证依赖
go mod verify

# 查看为什么需要某个依赖
go mod why github.com/gin-gonic/gin

# 整理并下载
go mod download""")
        ))
    )),

    TutorialCategory("标准库", listOf(
        TutorialItem("fmt", "fmt 包实现了格式化 I/O，类似 C 的 printf 和 scanf。", listOf(
            CodeExample("fmt 格式化", """package main

import "fmt"

func main() {
    // Print, Println, Printf
    fmt.Print("Hello ")
    fmt.Println("Go")
    fmt.Printf("名称: %s, 年龄: %d\n", "Go", 10)

    // Sprintf 返回格式化字符串
    s := fmt.Sprintf("Pi = %.2f", 3.14159)
    fmt.Println(s)

    // Fprintf 写入 io.Writer
    fmt.Fprintf(&buf, "写入: %d", 42)

    // Sprint 系列（返回字符串）
    str := fmt.Sprint("a", "b", "c")
    fmt.Println(str)

    // 格式化动词
    fmt.Printf("%T\n", 42)       // 类型
    fmt.Printf("%v\n", []int{1,2,3}) // 默认格式
    fmt.Printf("%#v\n", []int{1,2,3}) // Go语法格式
    fmt.Printf("%+v\n", struct{X int}{1}) // 字段名
}"""),
            CodeExample("fmt 输入", """package main

import (
    "bufio"
    "fmt"
    "os"
)

func main() {
    // 从标准输入读取
    fmt.Print("请输入名字: ")
    reader := bufio.NewReader(os.Stdin)
    name, _ := reader.ReadString('\n')
    fmt.Printf("你好, %s", name)
}""")
        )),
        TutorialItem("os/io", "os 包提供平台无关的操作系统功能，io 包提供基本的 I/O 原语。", listOf(
            CodeExample("文件读写", """package main

import (
    "fmt"
    "os"
)

func main() {
    // 写入文件
    err := os.WriteFile("test.txt", []byte("Hello Go"), 0644)
    if err != nil {
        fmt.Println("写入失败:", err)
        return
    }

    // 读取文件
    data, err := os.ReadFile("test.txt")
    if err != nil {
        fmt.Println("读取失败:", err)
        return
    }
    fmt.Println("内容:", string(data))

    // 检查文件是否存在
    if _, err := os.Stat("test.txt"); os.IsNotExist(err) {
        fmt.Println("文件不存在")
    }

    // 创建目录
    os.MkdirAll("testdir/sub", 0755)
    os.RemoveAll("testdir") // 删除目录
}""")
        )),
        TutorialItem("net/http", "net/http 包提供了 HTTP 客户端和服务端实现。", listOf(
            CodeExample("HTTP 客户端", """package main

import (
    "encoding/json"
    "fmt"
    "io"
    "net/http"
    "time"
)

func main() {
    // 创建带超时的客户端
    client := &http.Client{
        Timeout: 10 * time.Second,
    }

    // GET 请求
    resp, err := client.Get("https://httpbin.org/get")
    if err != nil {
        fmt.Println("请求失败:", err)
        return
    }
    defer resp.Body.Close()

    body, _ := io.ReadAll(resp.Body)
    fmt.Println("状态码:", resp.StatusCode)
    fmt.Println("响应:", string(body)[:200])

    // POST 请求（JSON）
    jsonStr := `{"key": "value"}`
    resp, err = http.Post(
        "https://httpbin.org/post",
        "application/json",
        io.NopCloser(strings.NewReader(jsonStr)),
    )
    if err != nil {
        fmt.Println("POST 失败:", err)
        return
    }
    defer resp.Body.Close()
    fmt.Println("POST 状态:", resp.StatusCode)
}"""),
            CodeExample("HTTP 服务端", """package main

import (
    "fmt"
    "net/http"
)

func main() {
    // 路由处理
    http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
        fmt.Fprintf(w, "欢迎来到首页")
    })

    http.HandleFunc("/hello", func(w http.ResponseWriter, r *http.Request) {
        name := r.URL.Query().Get("name")
        if name == "" {
            name = "World"
        }
        fmt.Fprintf(w, "Hello, %s!", name)
    })

    fmt.Println("服务启动在 :8080")
    http.ListenAndServe(":8080", nil)
}""")
        )),
        TutorialItem("encoding/json", "encoding/json 包实现了 JSON 的编码和解码。", listOf(
            CodeExample("JSON 编解码", """package main

import (
    "encoding/json"
    "fmt"
)

type User struct {
    Name  string `json:"name"`
    Age   int    `json:"age"`
    Email string `json:"email,omitempty"`
}

func main() {
    // 结构体 -> JSON
    u := User{Name: "张三", Age: 28}
    data, err := json.MarshalIndent(u, "", "  ")
    if err != nil {
        fmt.Println("编码失败:", err)
        return
    }
    fmt.Println(string(data))

    // JSON -> 结构体
    jsonStr := `{"name":"李四","age":25,"email":"li@test.com"}`
    var u2 User
    err = json.Unmarshal([]byte(jsonStr), &u2)
    if err != nil {
        fmt.Println("解码失败:", err)
        return
    }
    fmt.Printf("解码: %+v\n", u2)

    // 处理动态 JSON
    var m map[string]interface{}
    json.Unmarshal([]byte(jsonStr), &m)
    fmt.Println("动态:", m["name"])
}""")
        )),
        TutorialItem("time", "time 包提供了时间和日期的测量与显示功能。", listOf(
            CodeExample("time 包", """package main

import (
    "fmt"
    "time"
)

func main() {
    // 当前时间
    now := time.Now()
    fmt.Println("当前时间:", now)
    fmt.Println("格式化:", now.Format("2006-01-02 15:04:05"))

    // 创建时间
    t := time.Date(2024, 1, 15, 10, 30, 0, 0, time.Local)
    fmt.Println("指定时间:", t)

    // 时间运算
    future := now.Add(24 * time.Hour)
    fmt.Println("明天:", future.Format("2006-01-02"))

    duration := future.Sub(now)
    fmt.Println("相差:", duration)

    // 定时器
    timer := time.NewTimer(2 * time.Second)
    <-timer.C
    fmt.Println("2秒到了")

    // Ticker 周期执行
    ticker := time.NewTicker(500 * time.Millisecond)
    for i := 0; i < 3; i++ {
        <-ticker.C
        fmt.Println("Tick", i)
    }
    ticker.Stop()
}""")
        ))
    )),

    TutorialCategory("常用框架", listOf(
        TutorialItem("Gin", "Gin 是 Go 最流行的 Web 框架，以高性能和易用著称。", listOf(
            CodeExample("Gin 基本路由", """package main

import "github.com/gin-gonic/gin"

func main() {
    r := gin.Default()

    // GET 请求
    r.GET("/ping", func(c *gin.Context) {
        c.JSON(200, gin.H{"message": "pong"})
    })

    // 路径参数
    r.GET("/users/:id", func(c *gin.Context) {
        id := c.Param("id")
        c.JSON(200, gin.H{"user_id": id})
    })

    // 查询参数
    r.GET("/search", func(c *gin.Context) {
        q := c.DefaultQuery("q", "默认值")
        c.JSON(200, gin.H{"query": q})
    })

    // POST 请求（JSON body）
    r.POST("/users", func(c *gin.Context) {
        var json struct {
            Name string `json:"name" binding:"required"`
            Age  int    `json:"age"`
        }
        if err := c.ShouldBindJSON(&json); err != nil {
            c.JSON(400, gin.H{"error": err.Error()})
            return
        }
        c.JSON(200, gin.H{"name": json.Name, "age": json.Age})
    })

    r.Run(":8080")
}"""),
            CodeExample("Gin 中间件与分组", """package main

import (
    "log"
    "time"
    "github.com/gin-gonic/gin"
)

func Logger() gin.HandlerFunc {
    return func(c *gin.Context) {
        start := time.Now()
        c.Next()
        log.Printf("[%s] %s - %v", c.Request.Method, c.Request.URL.Path, time.Since(start))
    }
}

func main() {
    r := gin.Default()
    r.Use(Logger())

    // 路由分组
    api := r.Group("/api/v1")
    {
        api.GET("/users", func(c *gin.Context) {
            c.JSON(200, gin.H{"users": []string{"Alice", "Bob"}})
        })
        api.POST("/users", func(c *gin.Context) {
            c.JSON(200, gin.H{"status": "created"})
        })
    }

    r.Run(":8080")
}""")
        )),
        TutorialItem("GORM", "GORM 是 Go 最流行的 ORM 框架，支持多种数据库。", listOf(
            CodeExample("GORM 基本用法", """package main

import (
    "fmt"
    "gorm.io/driver/sqlite"
    "gorm.io/gorm"
)

type User struct {
    gorm.Model
    Name  string
    Email string `gorm:"uniqueIndex"`
    Age   int
}

func main() {
    db, err := gorm.Open(sqlite.Open("test.db"), &gorm.Config{})
    if err != nil {
        panic("连接数据库失败")
    }

    // 自动迁移
    db.AutoMigrate(&User{})

    // 创建
    db.Create(&User{Name: "张三", Email: "zhang@test.com", Age: 28})

    // 查询
    var user User
    db.Where("email = ?", "zhang@test.com").First(&user)
    fmt.Printf("找到: %s (年龄 %d)\n", user.Name, user.Age)

    // 更新
    db.Model(&user).Update("age", 29)

    // 删除
    db.Delete(&user)

    // 列表查询
    var users []User
    db.Where("age > ?", 20).Find(&users)
    fmt.Printf("共 %d 个用户\n", len(users))
}""")
        )),
        TutorialItem("Cobra (CLI)", "Cobra 是创建强大的 CLI 应用的库，被 Docker、Kubernetes 等项目使用。", listOf(
            CodeExample("Cobra CLI 应用", """package main

import (
    "fmt"
    "os"
    "github.com/spf13/cobra"
)

var rootCmd = &cobra.Command{
    Use:   "myapp",
    Short: "我的 CLI 应用",
    Long:  "一个使用 Cobra 框架的示例 CLI 应用",
    Run: func(cmd *cobra.Command, args []string) {
        fmt.Println("欢迎使用 myapp!")
    },
}

var greetCmd = &cobra.Command{
    Use:   "greet [name]",
    Short: "打招呼",
    Args:  cobra.ExactArgs(1),
    Run: func(cmd *cobra.Command, args []string) {
        name := args[0]
        loud, _ := cmd.Flags().GetBool("loud")
        if loud {
            fmt.Printf("你好, %s!!!\n", name)
        } else {
            fmt.Printf("你好, %s\n", name)
        }
    },
}

func init() {
    greetCmd.Flags().BoolP("loud", "l", false, "大声打招呼")
    rootCmd.AddCommand(greetCmd)
}

func main() {
    if err := rootCmd.Execute(); err != nil {
        os.Exit(1)
    }
}

// 用法:
// myapp greet Alice
// myapp greet Alice -l  (或 --loud)""")
        )),
        TutorialItem("Echo", "Echo 是另一个高性能 Web 框架，API 简洁，中间件丰富。", listOf(
            CodeExample("Echo 基本用法", """package main

import (
    "net/http"
    "github.com/labstack/echo/v4"
    "github.com/labstack/echo/v4/middleware"
)

type User struct {
    Name  string `json:"name"`
    Email string `json:"email"`
}

func main() {
    e := echo.New()
    e.Use(middleware.Logger())
    e.Use(middleware.Recover())

    // 路由
    e.GET("/", func(c echo.Context) error {
        return c.String(http.StatusOK, "Hello, Echo!")
    })

    e.GET("/users/:id", func(c echo.Context) error {
        id := c.Param("id")
        return c.JSON(http.StatusOK, map[string]string{"id": id})
    })

    e.POST("/users", func(c echo.Context) error {
        u := new(User)
        if err := c.Bind(u); err != nil {
            return err
        }
        return c.JSON(http.StatusCreated, u)
    })

    // 分组
    g := e.Group("/api")
    g.GET("/health", func(c echo.Context) error {
        return c.JSON(http.StatusOK, map[string]string{"status": "ok"})
    })

    e.Logger.Fatal(e.Start(":8080"))
}""")
        ))
    ))
)

// ==================== UI Components ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoTutorialDrawer(
    visible: Boolean,
    viewModel: com.termux.app.gostudio.GoStudioViewModel,
    onDismiss: () -> Unit
) {
    if (!visible) return

    Box(modifier = Modifier.fillMaxSize()) {
        // 遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    onDismiss()
                }
                .background(Color.Black.copy(alpha = 0.4f))
        )
        // 右侧面板
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .align(Alignment.CenterEnd)
                .background(Color(0xFF1E1E1E))
        ) {
            TutorialDrawerContent(viewModel = viewModel, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun TutorialDrawerContent(
    viewModel: com.termux.app.gostudio.GoStudioViewModel,
    onDismiss: () -> Unit
) {
    var selectedItem: TutorialItem? by remember { mutableStateOf(null) }

    viewModel.tutorialDetailOpen = selectedItem != null

    val closeDetailReq by viewModel.closeTutorialDetailRequest.collectAsState()
    LaunchedEffect(closeDetailReq) {
        if (closeDetailReq > 0 && selectedItem != null) {
            viewModel.consumeCloseTutorialDetail()
            selectedItem = null
        }
    }

    if (selectedItem != null) {
        TutorialDetailScreen(
            item = selectedItem!!,
            onBack = { selectedItem = null }
        )
    } else {
        TutorialCategoryList(
            onItemClick = { selectedItem = it }
        )
    }
}

@Composable
private fun TutorialCategoryList(
    onItemClick: (TutorialItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = "📚 Go 教程",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD4D4D4),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            goTutorialCategories.forEachIndexed { catIndex, category ->
                item(key = "cat_$catIndex") {
                    Column {
                        Text(
                            text = category.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64B5F6),
                            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp)
                        )
                        Box {
                            LazyRow(
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(category.items, key = { "item_${catIndex}_${it.title}" }) { item ->
                                    Card(
                                        modifier = Modifier
                                            .width(110.dp)
                                            .clickable { onItemClick(item) },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                                        shape = RoundedCornerShape(10.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp)
                                        ) {
                                            Text(
                                                text = item.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFFD4D4D4),
                                                maxLines = 2,
                                                lineHeight = 16.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.description.take(20) + if (item.description.length > 20) "..." else "",
                                                fontSize = 10.sp,
                                                color = Color(0xFF777777),
                                                maxLines = 1,
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                            if (category.items.size > 2) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .width(24.dp)
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color.Transparent, Color(0xFF121212))
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TutorialDetailScreen(
    item: TutorialItem,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = item.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = Color(0xFFD4D4D4)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color(0xFFD4D4D4)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1E1E1E)
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = item.description,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = Color(0xFFBBBBBB),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (item.codeExamples.isNotEmpty()) {
                Text(
                    text = "💬 代码示例",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64B5F6),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            item.codeExamples.forEachIndexed { index, example ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
                Text(
                    text = "▸ ${example.title}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFD4D4D4),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                CodeBlock(code = example.code)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    val annotatedCode = remember(code) { highlightGoCode(code) }
    val context = LocalContext.current
    var showCopied by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = annotatedCode,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
        }
        IconButton(
            onClick = {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("code", code))
                showCopied = true
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(28.dp)
        ) {
            Icon(
                imageVector = if (showCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = if (showCopied) "已复制" else "复制",
                tint = if (showCopied) Color(0xFF4CAF50) else Color(0xFF888888),
                modifier = Modifier.size(14.dp)
            )
        }
    }

    if (showCopied) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            showCopied = false
        }
    }
}

/** 简单的 Go 语法高亮 */
private fun highlightGoCode(code: String): AnnotatedString {
    val defaultColor = SpanStyle(color = Color(0xFFD4D4D4))
    val builder = AnnotatedString.Builder()
    builder.pushStyle(defaultColor)

    val stringRegex = """"[^"\\]*(\\.[^"\\]*)*"|`[^`]*`""".toRegex()
    val keywordColor = SpanStyle(color = Color(0xFFFF7B72))
    val stringColor = SpanStyle(color = Color(0xFFA5D6FF))
    val commentColor = SpanStyle(color = Color(0xFF8B949E))
    val typeColor = SpanStyle(color = Color(0xFFFFA657))
    val funcColor = SpanStyle(color = Color(0xFFD2A8FF))
    val numberColor = SpanStyle(color = Color(0xFF79C0FF))

    val keywords = listOf(
        "package", "import", "func", "var", "const", "type", "struct", "interface",
        "map", "chan", "go", "defer", "return", "if", "else", "for", "range", "switch", "case",
        "default", "break", "continue", "select", "fallthrough", "goto"
    )
    val builtins = listOf(
        "string", "int", "int8", "int16", "int32", "int64", "uint", "uint8",
        "uint16", "uint32", "uint64", "float32", "float64", "bool", "byte", "rune", "error",
        "nil", "true", "false", "make", "len", "cap", "append", "new", "fmt", "log", "os",
        "ioutil", "net/http", "encoding/json", "time", "context", "io", "strings", "strconv"
    )

    val lines = code.lines()
    for (lineIndex in lines.indices) {
        val line = lines[lineIndex]
        if (lineIndex > 0) builder.append("\n")

        val trimmed = line.trimStart()
        if (trimmed.startsWith("//")) {
            builder.pushStyle(commentColor)
            builder.append(line)
            builder.pop()
            continue
        }

        var i = 0
        while (i < line.length) {
            val stringMatch = stringRegex.find(line, i)
            if (stringMatch != null && stringMatch.range.first == i) {
                builder.pushStyle(stringColor)
                builder.append(stringMatch.value)
                builder.pop()
                i = stringMatch.range.last + 1
                continue
            }
            if (line[i] == '/' && i + 1 < line.length && line[i + 1] == '/') {
                builder.pushStyle(commentColor)
                builder.append(line.substring(i))
                builder.pop()
                break
            }
            if (line[i].isLetter() || line[i] == '_') {
                val start = i
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_' || line[i] == '.')) i++
                val word = line.substring(start, i)
                val style = when {
                    keywords.contains(word) -> keywordColor
                    builtins.contains(word) -> typeColor
                    word.firstOrNull()?.isUpperCase() == true -> funcColor
                    word.matches(Regex("\\d+")) -> numberColor
                    else -> null
                }
                if (style != null) {
                    builder.pushStyle(style)
                    builder.append(word)
                    builder.pop()
                } else {
                    builder.append(word)
                }
                continue
            }
            if (line[i].isDigit()) {
                val start = i
                while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i] == 'x' || line[i] in 'a'..'f' || line[i] in 'A'..'F')) i++
                builder.pushStyle(numberColor)
                builder.append(line.substring(start, i))
                builder.pop()
                continue
            }
            builder.append(line[i])
            i++
        }
    }
    builder.pop()
    return builder.toAnnotatedString()
}
