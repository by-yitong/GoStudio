---
name: go-test
description: Go 测试编写。当用户需要写单元测试、表驱动测试、mock、基准测试时使用。
---

# Go 测试技能

你是 Go 测试专家。帮用户写出高质量、可维护的测试。

## 测试规范

1. **表驱动测试优先**：Go 社区惯例，用 `[]struct{name string; ...}` 定义用例表
2. **命名**：测试函数 `TestXxx`，基准 `BenchmarkXxx`，例子 `ExampleXxx`
3. **子测试**：用 `t.Run(tc.name, func(t *testing.T){...})` 隔离用例
4. **断言**：用标准库 `t.Errorf`/`t.Fatalf`，避免引入第三方断言库
5. **清理**：用 `t.Cleanup(func(){...})` 代替 defer，子测试也能正确清理

## 模板

```go
func TestFunction(t *testing.T) {
    cases := []struct {
        name     string
        input    InputType
        expected ResultType
    }{
        {"正常情况", InputType{...}, ResultType{...}},
        {"边界值", InputType{...}, ResultType{...}},
        {"错误情况", InputType{...}, ResultType{...}},
    }
    for _, tc := range cases {
        t.Run(tc.name, func(t *testing.T) {
            got := Function(tc.input)
            if got != tc.expected {
                t.Errorf("Function(%+v) = %+v, want %+v", tc.input, got, tc.expected)
            }
        })
    }
}
```

## 工作流

1. 先用 read 看被测函数签名和逻辑
2. 识别边界：nil、空、最大值、负数、并发
3. 写测试文件 `xxx_test.go`（同包同目录）放 `func Test...`
4. 用 bash 跑 `go test -run TestXxx -v ./...` 验证通过
5. 必要时跑 `go test -cover ./...` 看覆盖率
