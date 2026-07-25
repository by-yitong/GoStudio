# GoStudio

## 中文

GoStudio 是一款运行在 Android 上的 Go 语言集成开发环境，目标是在移动设备上完成本地 Go 开发。它集成了代码编辑器、项目文件树、基于 go.mod 的项目结构、gopls 智能代码能力，以及基于 proot + Ubuntu rootfs 的工具链运行环境，可以直接在 Android 设备上编辑、构建和调试 Go 项目。

### 功能特性

- 创建 Go 项目，支持 application 和 library 模板
- 基于 go.mod 的模块化项目结构与依赖管理
- 基于 proot + Ubuntu rootfs 的 Go 工具链运行环境（内置 golang/gopls）
- gopls 语言服务，支持补全、参数提示、符号高亮、诊断、悬浮提示、跳转定义和格式化
- 基于 Sora 的代码编辑器，支持标签页、文件树、搜索、编辑器设置和快捷符号栏
- 输出面板显示构建和工具链日志
- 项目配置支持 Go 版本、构建目标、构建参数和并行任务数
- 新建项目自动生成默认 go.mod 模板

### 项目结构

```text
GoStudio/
├── app/                         Android 应用
├── modules/editor-core/         编辑器模型和共享状态
├── modules/project-file-tree/   项目文件树 UI/组件
├── modules/toolchain-runtime/   proot + Ubuntu rootfs 运行环境集成
├── modules/gopls-lsp/           gopls 语言服务桥接
├── modules/sora-editor/         Sora 编辑器模块
├── modules/sora-editor-lsp/     Sora LSP 集成
├── modules/sora-language-textmate/
├── modules/sora-oniguruma-native/
├── modules/terminal-view/
└── modules/terminal-emulator/
```

### 环境要求

- Android Studio 或兼容的 Gradle 构建环境
- JDK 17
- Android SDK，compile SDK 36
- Android 8.0+ 设备或模拟器（`minSdk 26`）

当前应用 native 打包目标为 `arm64-v8a`。

### 构建

克隆仓库后使用 Gradle 构建：

```bash
./gradlew assembleDebug
```

也可以使用 Android Studio 打开仓库根目录，然后运行 `app` 配置。

### Go 项目流程

GoStudio 会创建基于 go.mod 的 Go 项目，并根据选择的模板写入默认的 `go.mod` 和入口文件。

新建项目默认包含：

- go.mod 模块声明与 Go 版本配置
- 默认入口（main package / 库 package）模板
- 基本的依赖占位结构

保存项目根目录下的 `go.mod` 后，可以触发模块刷新，让 gopls 使用更新后的依赖信息。

### gopls

gopls 通过 proot + Ubuntu rootfs 工具链运行环境启动，用于提供 Go 智能编辑能力。编辑器设置中提供补全、参数提示、符号高亮、格式化、跳转和悬浮提示等功能开关。诊断由语言服务流程统一处理。

### 说明

- 已有项目会保留自己的 `go.mod`；模板变更只影响新建项目。
- 工具链命令会通过应用内的 proot + Ubuntu rootfs 运行环境在设备上执行（golang、gopls 等均运行在该 rootfs 内）。
- 停止构建时会停止 Android 进程，并对活动的 go build / gopls 进程做兜底清理。

### 联系方式

- 邮箱：xiaochenzaine@qq.com

### 许可证

本项目基于 MIT License 开源，详见 [LICENSE](LICENSE)。

## English

GoStudio is an Android IDE focused on local Go language development on mobile devices. It combines a code editor, project file tree, go.mod-based project structure, gopls language features, and a proot + Ubuntu rootfs toolchain runtime (with golang/gopls bundled) so Go projects can be edited, built, and debugged directly on Android.

### Features

- Go project creation with application and library templates
- go.mod-based modular project structure and dependency management
- proot + Ubuntu rootfs based Go toolchain runtime (golang/gopls bundled)
- gopls language server support for completion, signature help, symbol highlight, diagnostics, hover, go-to-definition, and formatting
- Sora-based code editor with tabs, file tree, search, editor settings, and shortcut symbol bar
- Output panel for build/toolchain logs
- Project configuration for Go version, build target, build arguments, and parallel jobs
- Default go.mod template generation for new projects

### Project Structure

```text
GoStudio/
├── app/                         Android application
├── modules/editor-core/         Editor models and shared editor state
├── modules/project-file-tree/   Project tree UI/components
├── modules/toolchain-runtime/   proot + Ubuntu rootfs command/runtime integration
├── modules/gopls-lsp/           gopls language server bridge
├── modules/sora-editor/         Sora editor module
├── modules/sora-editor-lsp/     Sora LSP integration
├── modules/sora-language-textmate/
├── modules/sora-oniguruma-native/
├── modules/terminal-view/
└── modules/terminal-emulator/
```

### Requirements

- Android Studio or compatible Gradle environment
- JDK 17
- Android SDK with compile SDK 36
- Android device or emulator running Android 8.0+ (`minSdk 26`)

The app currently targets `arm64-v8a` native packaging.

### Build

Clone the repository and build with Gradle:

```bash
./gradlew assembleDebug
```

For Android Studio, open the repository root and run the `app` configuration.

### Go Project Workflow

GoStudio creates go.mod-based projects and writes a default `go.mod` and entry files for the selected template.

New projects include:

- go.mod module declaration and Go version setup
- default entry-point template (main package / library package)
- basic dependency placeholder structure

Saving the root `go.mod` can trigger a module refresh so gopls can use the updated dependency information.

### gopls

gopls is started through the proot + Ubuntu rootfs toolchain runtime and is used for Go intelligent editing features. The editor settings expose user-facing feature switches such as completion, signature help, symbol highlight, formatting, go-to-definition, and hover. Diagnostics are handled as part of the language server flow.

### Notes

- Existing projects keep their own `go.mod`; template changes only affect newly created projects.
- Toolchain commands run through the app's proot + Ubuntu rootfs runtime on device (golang, gopls, etc. all run inside this rootfs).
- Build cancellation stops the Android process and uses fallback cleanup for active go build / gopls processes.

### Contact

- Email: xiaochenzaine@qq.com

### License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
