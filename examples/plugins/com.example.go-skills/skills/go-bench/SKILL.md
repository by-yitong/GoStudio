---
name: go-bench
description: 编写和运行 Go 基准测试，分析 ns/op 与内存分配
---

# Go 基准测试

为热点函数编写基准测试并运行分析。

## 编写

```go
func BenchmarkFoo(b *testing.B) {
    for i := 0; i < b.N; i++ {
        Foo()
    }
}
```

## 运行

```bash
go test -bench=. -benchmem ./...
```

关注输出中的 `ns/op`、`B/op`、`allocs/op` 三列；对比优化前后结果时使用 `benchstat`。
