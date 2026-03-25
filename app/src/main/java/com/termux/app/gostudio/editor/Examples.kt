package com.termux.app.gostudio.editor

/**
 * Go 示例代码库
 */
object GoExamples {

    data class GoExample(
        val name: String,
        val description: String,
        val files: Map<String, String>
    )

    val examples: List<GoExample> = listOf(
        GoExample(
            name = "Hello World",
            description = "最简单的 Go 程序",
            files = mapOf(
                "main.go" to """
                    |package main
                    |
                    |import "fmt"
                    |
                    |func main() {
                    |    fmt.Println("Hello, GoStudio!")
                    |    fmt.Println("Go is running on your phone!")
                    |}
                """.trimMargin()
            )
        ),
        GoExample(
            name = "HTTP Server",
            description = "简单的 HTTP 服务器",
            files = mapOf(
                "main.go" to """
                    |package main
                    |
                    |import (
                    |    "fmt"
                    |    "net/http"
                    |)
                    |
                    |func handler(w http.ResponseWriter, r *http.Request) {
                    |    fmt.Fprintf(w, "Hello from GoStudio HTTP Server!")
                    |}
                    |
                    |func main() {
                    |    http.HandleFunc("/", handler)
                    |    fmt.Println("Server starting on :8080...")
                    |    fmt.Println("Open http://localhost:8080 in your browser")
                    |    err := http.ListenAndServe(":8080", nil)
                    |    if err != nil {
                    |        fmt.Println("Server error:", err)
                    |    }
                    |}
                """.trimMargin()
            )
        ),
        GoExample(
            name = "Goroutine",
            description = "并发编程示例",
            files = mapOf(
                "main.go" to """
                    |package main
                    |
                    |import (
                    |    "fmt"
                    |    "sync"
                    |    "time"
                    |)
                    |
                    |func say(s string, wg *sync.WaitGroup) {
                    |    defer wg.Done()
                    |    for i := 0; i < 3; i++ {
                    |        fmt.Printf("%s %d\n", s, i)
                    |        time.Sleep(100 * time.Millisecond)
                    |    }
                    |}
                    |
                    |func main() {
                    |    var wg sync.WaitGroup
                    |
                    |    wg.Add(1)
                    |    go say("world", &wg)
                    |
                    |    wg.Add(1)
                    |    go say("hello", &wg)
                    |
                    |    wg.Add(1)
                    |    go func() {
                    |        defer wg.Done()
                    |        for i := 0; i < 3; i++ {
                    |            fmt.Printf("goroutine %d\n", i)
                    |            time.Sleep(150 * time.Millisecond)
                    |        }
                    |    }()
                    |
                    |    wg.Wait()
                    |    fmt.Println("All goroutines done!")
                    |}
                """.trimMargin()
            )
        ),
        GoExample(
            name = "Channel",
            description = "通道通信示例",
            files = mapOf(
                "main.go" to """
                    |package main
                    |
                    |import "fmt"
                    |
                    |func fibonacci(n int, c chan int) {
                    |    x, y := 0, 1
                    |    for i := 0; i < n; i++ {
                    |        c <- x
                    |        x, y = y, x+y
                    |    }
                    |    close(c)
                    |}
                    |
                    |func main() {
                    |    c := make(chan int, 10)
                    |    go fibonacci(cap(c), c)
                    |
                    |    fmt.Print("Fibonacci: ")
                    |    for i := range c {
                    |        fmt.Printf("%d ", i)
                    |    }
                    |    fmt.Println()
                    |
                    |    ch := make(chan string, 2)
                    |    ch <- "buffered"
                    |    ch <- "channel"
                    |    fmt.Println("Buffered:", <-ch, <-ch)
                    |}
                """.trimMargin()
            )
        ),
        GoExample(
            name = "HTTP 请求",
            description = "发起 HTTP GET 请求（验证网络/DNS）",
            files = mapOf(
                "main.go" to """
                    |package main
                    |
                    |import (
                    |    "fmt"
                    |    "io"
                    |    "net/http"
                    |    "time"
                    |)
                    |
                    |func main() {
                    |    client := &http.Client{Timeout: 10 * time.Second}
                    |
                    |    fmt.Println("=== 测试 HTTP 请求 ===")
                    |    resp, err := client.Get("https://httpbin.org/get")
                    |    if err != nil {
                    |        fmt.Println("请求失败:", err)
                    |        return
                    |    }
                    |    defer resp.Body.Close()
                    |
                    |    body, _ := io.ReadAll(resp.Body)
                    |    fmt.Printf("状态码: %d\n", resp.StatusCode)
                    |    fmt.Printf("响应长度: %d bytes\n", len(body))
                    |    fmt.Printf("内容预览: %s\n", string(body[:min(200, len(body))]))
                    |
                    |    fmt.Println("\n=== 测试 DNS 解析 (baidu.com) ===")
                    |    resp2, err := client.Get("https://www.baidu.com")
                    |    if err != nil {
                    |        fmt.Println("请求百度失败:", err)
                    |    } else {
                    |        fmt.Printf("百度响应状态码: %d\n", resp2.StatusCode)
                    |        resp2.Body.Close()
                    |    }
                    |
                    |    fmt.Println("\n网络和 DNS 均正常!")
                    |}
                """.trimMargin()
            )
        ),
        GoExample(
            name = "多文件项目",
            description = "多文件 + go mod 示例",
            files = mapOf(
                "main.go" to """
                    |package main
                    |
                    |import (
                    |    "fmt"
                    |    "gostudio/mypkg"
                    |)
                    |
                    |func main() {
                    |    fmt.Println(mypkg.Greet("GoStudio"))
                    |    fmt.Println("2 + 3 =", mypkg.Add(2, 3))
                    |}
                """.trimMargin(),
                "go.mod" to """
                    |module gostudio
                    |
                    |go 1.21
                """.trimMargin(),
                "mypkg/math.go" to """
                    |package mypkg
                    |
                    |func Add(a, b int) int {
                    |    return a + b
                    |}
                    |
                    |func Greet(name string) string {
                    |    return "Hello, " + name + "!"
                    |}
                """.trimMargin()
            )
        )
    )
}
