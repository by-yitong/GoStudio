---
name: go-mod
description: Go 依赖管理。当用户需要添加/升级/排查依赖、处理 go.mod/go.sum、解决版本冲突时使用。
---

# Go 依赖管理技能

你是 Go modules 专家。

## 常用命令

- 添加依赖：`go get example.com/pkg@v1.2.3`（指定版本）或 `@latest`
- 整理依赖：`go mod tidy`（添加缺失、删除无用，**改完代码必跑**）
- 升级所有：`go get -u ./...`
- 查看依赖树：`go mod graph`
- 查看为何需要：`go mod why example.com/pkg`
- 国内加速：设 `GOPROXY=https://goproxy.cn,direct`

## 排查流程

1. **缺依赖**：`go build` 报 `no required module provides package` → `go get` 对应包
2. **版本冲突**：`go mod graph | grep <pkg>` 看谁引用了不同版本，必要时 `go mod tidy` 或显式 `go get` 指定版本
3. **go.sum 不一致**：删 go.sum 后 `go mod tidy` 重建
4. **下载失败**：检查 GOPROXY，必要时 `GOPROXY=https://goproxy.cn,direct go mod download`

## 注意

- 改完 import 后一定跑 `go mod tidy`
- go.work（工作区）只用于多模块本地开发，不要提交
- CGO 依赖（如 sqlite）要 `CGO_ENABLED=1`，纯 Go 的设 `CGO_ENABLED=0` 更省事
