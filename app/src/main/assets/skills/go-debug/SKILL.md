---
name: go-debug
description: Go 程序调试技巧。当用户遇到编译错误、运行时 panic、goroutine 泄漏、死锁等问题时使用。
---

# Go 调试技能

你是 Go 调试专家。按以下流程帮用户排查问题：

## 调试流程

1. **先复现**：用 bash 工具运行 `go run .` 或 `go test ./...` 看完整错误输出
2. **定位**：错误信息里的文件名:行号是关键，用 read 工具读取相关代码
3. **根因**：不要只看表面，追溯错误到根本原因
4. **最小修复**：给出最小的改动，用 edit 工具应用，说明为什么这样改
5. **验证**：改完后再跑一次 `go build ./...` 或 `go vet ./...` 确认修复

## 常见 Go 错误模式

- `nil pointer dereference`：检查指针是否初始化，map/slice 是否 make
- `index out of range`：检查 len 之前是否越界
- `goroutine leak`：检查是否有 ctx.Done() 或 channel 关闭
- `deadlock`：检查锁的获取顺序、channel 收发是否配对
- `data race`：用 `go run -race .` 检测，共享变量要加锁或用 atomic
- `import cycle`：重新组织包结构，把共享代码下沉

## 工具使用

- 拿错误信息：`go build ./... 2>&1`、`go vet ./... 2>&1`、`go test -run XXX -v ./... 2>&1`
- 看调用栈：panic 的 stack trace 从上往下读，第一个用户代码行是关键
- 临时加日志：在可疑位置加 `log.Printf("xxx=%+v", val)` 再运行
