# GoStudio

## 中文

GoStudio 是一款运行在 Android 上的 Go 语言集成开发环境，目标是在移动设备上完成本地 Go 开发。它集成了代码编辑器、项目文件树、基于 go.mod 的项目结构、gopls 智能代码能力，以及基于 proot + Alpine rootfs 的工具链运行环境，可以直接在 Android 设备上编辑、构建和调试 Go 项目。

### 交流

QQ 群：1095682100，欢迎加入交流反馈。

### 功能特性

- 从 GitHub/Gitee/GitLab 克隆 Go 项目（支持 HTTPS/SSH 地址、私有仓库登录、克隆进度与自动导入最近项目）
- 创建 Go 项目，内置多种项目模板（Hello World、HTTP 服务器、CLI 工具、Web API，以及 Gin/GORM/Cobra/Echo 等常用框架模板）
- AndLua 式 App 运行：XML 布局 + Go 逻辑，构建后在 GoStudio 宿主界面内直接运行，无需打包 APK
- 基于 go.mod 的模块化项目结构与依赖管理
- 基于 proot + Alpine rootfs 的 Go 工具链运行环境（内置 golang/gopls）
- gopls 语言服务，支持补全、参数提示、符号高亮、诊断、悬浮提示、跳转定义和格式化
- 基于 Sora 的代码编辑器，支持标签页、文件树、搜索、编辑器设置和快捷符号栏
- Git 登录支持 HTTPS Token、HTTPS 账号密码、SSH 私钥、GitHub OAuth 设备码与终端 Git 配置
- 输出面板显示构建和工具链日志
- 项目配置支持 Go 版本、构建目标、构建参数和并行任务数
- 新建项目自动生成默认 go.mod 模板
- 插件系统：通过 ZIP 导入数据包插件（manifest + 能力目录），当前支持 AI 技能扩展

### 项目结构

```text
GoStudio/
├── app/                         Android 应用
├── modules/editor-core/         编辑器模型和共享状态
├── modules/project-file-tree/   项目文件树 UI/组件
├── modules/toolchain-runtime/   proot + Alpine rootfs 运行环境集成
├── modules/gopls-lsp/           gopls 语言服务桥接
├── modules/sora-editor/         Sora 编辑器模块
├── modules/sora-editor-lsp/     Sora LSP 集成
├── modules/sora-language-textmate/
├── modules/sora-oniguruma-native/
├── modules/terminal-view/
├── modules/terminal-emulator/
├── examples/plugins/            插件示例
```

### 插件系统

GoStudio 支持数据包插件（不执行代码）。一个插件是一个含 `manifest.json` 的目录：

```text
com.example.go-skills/
├── manifest.json    # 必需
└── skills/          # AI 技能（每个子目录一个 SKILL.md）
    └── go-bench/SKILL.md
```

`manifest.json`：

```json
{
  "id": "com.example.go-skills",
  "name": "Go 进阶技能示例",
  "version": "1.0.0",
  "description": "描述",
  "author": "作者",
  "min_app_version": 104
}
```

- `id` 必须与目录名一致（点分命名，如 `com.example.xxx`）
- `version` 为语义化版本（x.y.z）
- 支持的能力目录：`skills/`（AI 技能）；`templates/`、`themes/` 预留

安装方式二选一：
- **插件市场**：「插件」页 →「浏览」→ 一键安装（索引来自官方仓库 [gostudio-plugins](https://github.com/by-yitong/gostudio-plugins)）
- **本地 ZIP**：把目录打成 ZIP（`manifest.json` 可在 ZIP 根或唯一子目录内），在「插件」页点击「安装」导入

插件默认启用，可随时禁用或卸载。本地开发示例见 `examples/plugins/`。

```bash
cd examples/plugins/com.example.go-skills
zip -r ../go-skills.zip .
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

gopls 通过 proot + Alpine rootfs 工具链运行环境启动，用于提供 Go 智能编辑能力。编辑器设置中提供补全、参数提示、符号高亮、格式化、跳转和悬浮提示等功能开关。诊断由语言服务流程统一处理。

### 说明

- 已有项目会保留自己的 `go.mod`；模板变更只影响新建项目。
- 工具链命令会通过应用内的 proot + Alpine rootfs 运行环境在设备上执行（golang、gopls 等均运行在该 rootfs 内）。
- 停止构建时会停止 Android 进程，并对活动的 go build / gopls 进程做兜底清理。

### 致谢

本项目参考了 [XCode](https://github.com/xiaochenzaine/XCode)（一款 Android 上的 C/C++ IDE），在其编辑器、proot 运行时与项目结构的基础上进行了 Go 语言方向的改造与重构。

### 许可证

本项目基于 MIT License 开源，详见 [LICENSE](LICENSE)。

## English

GoStudio is an Android IDE focused on local Go language development on mobile devices. It combines a code editor, project file tree, go.mod-based project structure, gopls language features, and a proot + Alpine rootfs toolchain runtime (with golang/gopls bundled) so Go projects can be edited, built, and debugged directly on Android.

### Community

QQ Group: 1095682100 — join for discussion and feedback.

### Features

- Clone Go projects from GitHub/Gitee/GitLab (HTTPS/SSH URLs, private-repository authentication, clone progress, and automatic recent-project import)
- Go project creation with multiple built-in templates (Hello World, HTTP server, CLI tool, Web API, plus Gin/GORM/Cobra/Echo framework templates)
- AndLua-style app running: XML layouts + Go logic, built once and executed directly inside the GoStudio host UI without repackaging an APK
- go.mod-based modular project structure and dependency management
- proot + Alpine rootfs based Go toolchain runtime (golang/gopls bundled)
- Plugin system: install data-pack plugins from ZIP (manifest + capability dirs), currently AI skills
- gopls language server support for completion, signature help, symbol highlight, diagnostics, hover, go-to-definition, and formatting
- Sora-based code editor with tabs, file tree, search, editor settings, and shortcut symbol bar
- Git authentication for HTTPS tokens, HTTPS credentials, SSH keys, GitHub OAuth device flow, and terminal Git configuration
- Output panel for build/toolchain logs
- Project configuration for Go version, build target, build arguments, and parallel jobs
- Default go.mod template generation for new projects

### Project Structure

```text
GoStudio/
├── app/                         Android application
├── modules/editor-core/         Editor models and shared editor state
├── modules/project-file-tree/   Project tree UI/components
├── modules/toolchain-runtime/   proot + Alpine rootfs command/runtime integration
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

gopls is started through the proot + Alpine rootfs toolchain runtime and is used for Go intelligent editing features. The editor settings expose user-facing feature switches such as completion, signature help, symbol highlight, formatting, go-to-definition, and hover. Diagnostics are handled as part of the language server flow.

### Notes

- Existing projects keep their own `go.mod`; template changes only affect newly created projects.
- Toolchain commands run through the app's proot + Alpine rootfs runtime on device (golang, gopls, etc. all run inside this rootfs).
- Build cancellation stops the Android process and uses fallback cleanup for active go build / gopls processes.

### Acknowledgements

This project references [XCode](https://github.com/xiaochenzaine/XCode) (an Android C/C++ IDE). GoStudio builds on its editor, proot runtime, and project structure, refactored for Go language development.

### License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
