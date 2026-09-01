package com.jmwl.gostudio.learn

private fun practical_text(md: String) = learn_block.text(md.trimIndent())
private fun practical_code(src: String) = learn_block.code(normalize_lesson_code(src))
private fun practical_note(text: String) = learn_block.callout("note", text)
private fun practical_tip(text: String) = learn_block.callout("tip", text)

internal fun practical_learn_tracks(): List<learn_track> = listOf(
    learn_track(
        id = "go-practical",
        title = "Go 实战",
        subtitle = "HTTP 请求、服务端、数据库、CLI、Gin 与 GORM",
        accent_color = 0xFF7C5CFFL,
        category = "实战开发",
        lessons = listOf(
            learn_lesson(
                id = "go-http-request",
                title = "HTTP 请求与 JSON",
                summary = "用 net/http 请求天气 API 并解析 JSON。",
                est_minutes = 8,
                steps = listOf(
                    learn_step.concept(
                        "go-http-request-concept",
                        "请求天气 API",
                        listOf(
                            practical_text(
                                """
                                Go 标准库 `net/http` 可以直接发起 HTTP 请求。请求参数用 `net/url` 的 `Values` 构造，它会自动处理中文和特殊字符的 URL 编码。
                                """
                            ),
                            practical_code(
                                """
                                query := url.Values{}
                                query.Set("dz", "北京")
                                query.Set("return", "json")

                                api := "https://api.tangdouz.com/tq.php?" + query.Encode()
                                resp, err := http.Get(api)
                                if err != nil {
                                    fmt.Println("请求失败:", err)
                                    return
                                }
                                defer resp.Body.Close()
                                """
                            ),
                            practical_text(
                                """
                                网络请求可能失败，所以 `err` 必须先检查。`defer resp.Body.Close()` 会在 `main` 函数返回前关闭响应体。
                                """
                            ),
                            practical_code(
                                """
                                body, err := io.ReadAll(resp.Body)
                                if err != nil {
                                    fmt.Println("读取响应失败:", err)
                                    return
                                }

                                var weather weatherResponse
                                if err := json.Unmarshal(body, &weather); err != nil {
                                    fmt.Println("解析 JSON 失败:", err)
                                    return
                                }
                                """
                            ),
                            practical_text(
                                """
                                这个接口返回的 JSON 里，今天的数据叫 `"1"`，字段名不是合法的 Go 标识符，所以要用结构体标签建立映射：
                                """
                            ),
                            practical_code(
                                """
                                type weatherResponse struct {
                                    City string `json:"city"`
                                    Today weatherDay `json:"1"`
                                }

                                type weatherDay struct {
                                    Date    string `json:"date"`
                                    Weather string `json:"weather"`
                                    High    string `json:"high"`
                                    Low     string `json:"low"`
                                }
                                """
                            ),
                            practical_code(
                                """
                                fmt.Printf(
                                    "%s %s：%s，%s ~ %s\n",
                                    weather.City,
                                    weather.Today.Date,
                                    weather.Today.Weather,
                                    weather.Today.Low,
                                    weather.Today.High,
                                )
                                """
                            ),
                            practical_note("更严谨的做法还可以检查 `resp.StatusCode` 是否为 `http.StatusOK`。"),
                            practical_tip("这个示例的完整可运行版本可以在新建项目的「HTTP 请求」模板里找到。")
                        )
                    )
                )
            ),
            learn_lesson(
                id = "go-http-server",
                title = "原生 HTTP 服务端",
                summary = "用 net/http 注册路由、读取参数、返回 JSON。",
                est_minutes = 8,
                steps = listOf(
                    learn_step.concept(
                        "go-http-server-concept",
                        "标准库 HTTP 服务",
                        listOf(
                            practical_text(
                                """
                                `http.HandleFunc` 把一个路径和处理函数绑定起来。`http.ListenAndServe` 启动服务并监听端口：
                                """
                            ),
                            practical_code(
                                """
                                http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
                                    fmt.Fprintln(w, "Hello, Go!")
                                })

                                fmt.Println("服务已启动: http://localhost:8080")
                                if err := http.ListenAndServe(":8080", nil); err != nil {
                                    fmt.Println("服务启动失败:", err)
                                }
                                """
                            ),
                            practical_text(
                                """
                                处理函数的 `r` 是请求对象，`w` 是响应写入器。路径参数用 `r.URL.Query()` 读取：
                                """
                            ),
                            practical_code(
                                """
                                http.HandleFunc("/hello", func(w http.ResponseWriter, r *http.Request) {
                                    name := r.URL.Query().Get("name")
                                    if name == "" {
                                        name = "world"
                                    }
                                    fmt.Fprintf(w, "hello, %s", name)
                                })
                                """
                            ),
                            practical_text(
                                """
                                返回 JSON 前，应该声明 `Content-Type`。`json.NewEncoder(w).Encode(...)` 会把结构体写入响应体：
                                """
                            ),
                            practical_code(
                                """
                                type message struct {
                                    Text string `json:"text"`
                                }

                                http.HandleFunc("/api", func(w http.ResponseWriter, r *http.Request) {
                                    w.Header().Set("Content-Type", "application/json; charset=utf-8")
                                    json.NewEncoder(w).Encode(message{Text: "ok"})
                                })
                                """
                            ),
                            practical_note("`http.ListenAndServe` 会阻塞当前 goroutine，服务不退出时就一直等待请求。")
                        )
                    )
                )
            ),
            learn_lesson(
                id = "go-database",
                title = "数据库操作",
                summary = "database/sql 的连接、增删改查和事务。",
                est_minutes = 10,
                steps = listOf(
                    learn_step.concept(
                        "go-database-concept",
                        "database/sql 入门",
                        listOf(
                            practical_text(
                                """
                                Go 通过 `database/sql` 提供统一的数据库接口，具体数据库由驱动实现。下面以 SQLite 为例：
                                """
                            ),
                            practical_code(
                                """
                                import (
                                    "database/sql"
                                    _ "modernc.org/sqlite"
                                )

                                db, err := sql.Open("sqlite", "./app.db")
                                if err != nil {
                                    fmt.Println("打开数据库失败:", err)
                                    return
                                }
                                defer db.Close()
                                """
                            ),
                            practical_text(
                                """
                                下划线导入 `_ "modernc.org/sqlite"` 只执行驱动的初始化代码，不会直接调用包里的其他函数。
                                """
                            ),
                            practical_code(
                                """
                                _, err = db.Exec(`
                                    CREATE TABLE IF NOT EXISTS users (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        name TEXT NOT NULL,
                                        age INTEGER NOT NULL
                                    )
                                `)
                                """
                            ),
                            practical_text(
                                """
                                `Exec` 用于建表、插入、更新和删除。SQL 参数不要拼接字符串，应使用占位符：
                                """
                            ),
                            practical_code(
                                """
                                result, err := db.Exec(
                                    "INSERT INTO users (name, age) VALUES (?, ?)",
                                    "Alice", 20,
                                )
                                if err != nil {
                                    fmt.Println("插入失败:", err)
                                    return
                                }
                                id, _ := result.LastInsertId()
                                """
                            ),
                            practical_text(
                                """
                                查询先拿 `*sql.Rows`，再用 `Scan` 把每一行复制到变量里：
                                """
                            ),
                            practical_code(
                                """
                                rows, err := db.Query("SELECT id, name, age FROM users")
                                if err != nil {
                                    fmt.Println("查询失败:", err)
                                    return
                                }
                                defer rows.Close()

                                for rows.Next() {
                                    var id, age int64
                                    var name string
                                    if err := rows.Scan(&id, &name, &age); err != nil {
                                        fmt.Println("读取失败:", err)
                                        return
                                    }
                                    fmt.Println(id, name, age)
                                }
                                """
                            ),
                            practical_text(
                                """
                                多条 SQL 必须同时成功时，用事务包裹。出错就回滚：
                                """
                            ),
                            practical_code(
                                """
                                tx, err := db.Begin()
                                if err != nil {
                                    fmt.Println("开启事务失败:", err)
                                    return
                                }

                                if _, err := tx.Exec("UPDATE users SET age = age + 1"); err != nil {
                                    tx.Rollback()
                                    fmt.Println("事务失败:", err)
                                    return
                                }
                                if err := tx.Commit(); err != nil {
                                    fmt.Println("提交失败:", err)
                                }
                                """
                            ),
                            practical_tip("改用 MySQL、PostgreSQL 时，核心仍是 `database/sql`，主要变化只是驱动名、DSN 和占位符。")
                        )
                    )
                )
            ),
            learn_lesson(
                id = "go-cli",
                title = "CLI 工具",
                summary = "os.Args、flag 子命令和退出码。",
                est_minutes = 8,
                steps = listOf(
                    learn_step.concept(
                        "go-cli-concept",
                        "命令行程序",
                        listOf(
                            practical_text(
                                """
                                最简单的 CLI 可以直接读 `os.Args`。第 0 项是程序名，后面才是用户参数：
                                """
                            ),
                            practical_code(
                                """
                                if len(os.Args) < 2 {
                                    fmt.Println("usage: app <name>")
                                    os.Exit(1)
                                }
                                fmt.Printf("hello, %s\n", os.Args[1])
                                """
                            ),
                            practical_text(
                                """
                                参数更多时，标准库 `flag` 能自动解析选项：
                                """
                            ),
                            practical_code(
                                """
                                var name string
                                var count int
                                flag.StringVar(&name, "name", "world", "收件人名称")
                                flag.IntVar(&count, "count", 1, "重复次数")
                                flag.Parse()

                                for i := 0; i < count; i++ {
                                    fmt.Printf("hello, %s\n", name)
                                }
                                """
                            ),
                            practical_text(
                                """
                                实际工具通常按第一个参数分发子命令，每个子命令再创建自己的 `flag.FlagSet`：
                                """
                            ),
                            practical_code(
                                """
                                if len(os.Args) < 2 {
                                    fmt.Println("commands: add, list")
                                    os.Exit(1)
                                }

                                switch os.Args[1] {
                                case "add":
                                    cmd := flag.NewFlagSet("add", flag.ExitOnError)
                                    title := cmd.String("title", "", "任务标题")
                                    cmd.Parse(os.Args[2:])
                                    fmt.Println("add:", *title)
                                case "list":
                                    fmt.Println("list all tasks")
                                default:
                                    fmt.Println("unknown command:", os.Args[1])
                                    os.Exit(1)
                                }
                                """
                            ),
                            practical_note("`flag.ExitOnError` 表示解析参数失败时自动打印帮助并退出。")
                        )
                    )
                )
            ),
            learn_lesson(
                id = "go-gin",
                title = "Gin 框架",
                summary = "路由、路径参数、JSON 绑定和统一响应。",
                est_minutes = 9,
                steps = listOf(
                    learn_step.concept(
                        "go-gin-concept",
                        "Gin 快速入门",
                        listOf(
                            practical_text(
                                """
                                Gin 是常用的 Go Web 框架，路由和参数处理比原生 `net/http` 更简洁：
                                """
                            ),
                            practical_code(
                                """
                                r := gin.Default()

                                r.GET("/", func(c *gin.Context) {
                                    c.JSON(http.StatusOK, gin.H{"message": "ok"})
                                })

                                if err := r.Run(":8080"); err != nil {
                                    fmt.Println("启动失败:", err)
                                }
                                """
                            ),
                            practical_text(
                                """
                                `gin.H` 是 `map[string]any` 的别名，适合返回简单 JSON。`gin.Default()` 自带日志和恢复中间件。
                                """
                            ),
                            practical_code(
                                """
                                r.GET("/users/:id", func(c *gin.Context) {
                                    id := c.Param("id")
                                    c.JSON(http.StatusOK, gin.H{"id": id})
                                })

                                r.GET("/search", func(c *gin.Context) {
                                    keyword := c.Query("keyword")
                                    c.JSON(http.StatusOK, gin.H{"keyword": keyword})
                                })
                                """
                            ),
                            practical_text(
                                """
                                对 POST 请求，可以定义结构体并让 Gin 自动校验和绑定 JSON：
                                """
                            ),
                            practical_code(
                                """
                                type createUser struct {
                                    Name string `json:"name" binding:"required"`
                                    Age  int    `json:"age" binding:"gte=0"`
                                }

                                r.POST("/users", func(c *gin.Context) {
                                    var req createUser
                                    if err := c.ShouldBindJSON(&req); err != nil {
                                        c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
                                        return
                                    }
                                    c.JSON(http.StatusCreated, req)
                                })
                                """
                            ),
                            practical_text(
                                """
                                路由可以分组，公共前缀和中间件只写一次：
                                """
                            ),
                            practical_code(
                                """
                                api := r.Group("/api")
                                api.Use(func(c *gin.Context) {
                                    c.Set("version", "v1")
                                    c.Next()
                                })

                                api.GET("/ping", func(c *gin.Context) {
                                    c.JSON(http.StatusOK, gin.H{
                                        "version": c.GetString("version"),
                                    })
                                })
                                """
                            ),
                            practical_tip("Gin 项目需要先执行 `go mod tidy` 下载依赖，模板里已经写了主要依赖。")
                        )
                    )
                )
            ),
            learn_lesson(
                id = "go-gorm",
                title = "GORM 框架",
                summary = "模型定义、自动迁移和常见 CRUD。",
                est_minutes = 10,
                steps = listOf(
                    learn_step.concept(
                        "go-gorm-concept",
                        "GORM 快速入门",
                        listOf(
                            practical_text(
                                """
                                GORM 把数据库表映射成 Go 结构体。下面使用纯 Go 的 SQLite 驱动，适合在没有 CGO 的环境里学习：
                                """
                            ),
                            practical_code(
                                """
                                type User struct {
                                    ID   uint   `gorm:"primaryKey"`
                                    Name string `gorm:"size:64;not null"`
                                    Age  int
                                }

                                db, err := gorm.Open(sqlite.Open("app.db"), &gorm.Config{})
                                if err != nil {
                                    fmt.Println("连接数据库失败:", err)
                                    return
                                }
                                """
                            ),
                            practical_text(
                                """
                                `AutoMigrate` 会根据结构体创建或更新表结构：
                                """
                            ),
                            practical_code(
                                """
                                if err := db.AutoMigrate(&User{}); err != nil {
                                    fmt.Println("迁移失败:", err)
                                    return
                                }
                                """
                            ),
                            practical_text(
                                """
                                插入、查询、更新和删除都通过模型方法完成：
                                """
                            ),
                            practical_code(
                                """
                                user := User{Name: "Alice", Age: 20}
                                if err := db.Create(&user).Error; err != nil {
                                    fmt.Println("创建失败:", err)
                                }

                                var found User
                                err = db.First(&found, user.ID).Error
                                """
                            ),
                            practical_code(
                                """
                                db.Model(&found).Update("age", 21)
                                db.Model(&found).Updates(User{Name: "Alice", Age: 22})
                                db.Delete(&found)
                                """
                            ),
                            practical_text(
                                """
                                条件查询用 `Where`，结果放进切片：
                                """
                            ),
                            practical_code(
                                """
                                var users []User
                                err = db.
                                    Where("age > ?", 18).
                                    Order("age DESC").
                                    Limit(10).
                                    Find(&users).Error
                                """
                            ),
                            practical_note("GORM 的 `First` 找不到记录会返回 `gorm.ErrRecordNotFound`；`Find` 不会把空结果当成错误。")
                        )
                    )
                )
            )
        )
    )
)
