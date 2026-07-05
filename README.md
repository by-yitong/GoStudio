# XCode

## 中文

XCode 是一款运行在 Android 上的 C/C++ IDE，目标是在移动设备上完成本地 C/C++ 开发。它集成了代码编辑器、项目文件树、CMake/Ninja 构建流程、clangd 智能代码能力，以及基于 proot 的工具链运行环境，可以直接在 Android 设备上编辑、配置和构建 C/C++ 项目。

### 功能特性

- 创建 C/C++ 项目，支持可执行程序、静态库、动态库模板
- CMake 和 Ninja 项目配置、构建流程
- 基于 proot 的 Linux 工具链运行环境
- clangd 语言服务，支持补全、参数提示、符号高亮、诊断、悬浮提示和格式化
- 基于 Sora 的代码编辑器，支持标签页、文件树、搜索、编辑器设置和快捷符号栏
- 输出面板显示配置、构建和工具链日志
- 项目配置支持 ABI、C++ 标准、构建类型、CMake 参数和并行任务数
- 新建项目自动生成默认 `.clang-format`
- 新建项目模板会在 `CMakeLists.txt` 中生成 Release 构建后的 strip 规则

### 项目结构

```text
XCode/
├── app/                         Android 应用
├── modules/editor-core/         编辑器模型和共享状态
├── modules/project-file-tree/   项目文件树 UI/组件
├── modules/toolchain-runtime/   proot 命令和运行环境集成
├── modules/clangd-lsp/          clangd 语言服务桥接
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
- Android NDK `29.0.14206865`
- Android 8.0+ 设备或模拟器（`minSdk 26`）

当前应用 native 打包目标为 `arm64-v8a`。

### 构建

克隆仓库后使用 Gradle 构建：

```bash
./gradlew assembleDebug
```

也可以使用 Android Studio 打开仓库根目录，然后运行 `app` 配置。

### C/C++ 项目流程

XCode 会创建基于 CMake 的项目，并根据选择的模板写入默认 `CMakeLists.txt`。

新建项目默认包含：

- Android ABI 校验
- CMake 构建目录下的输出目录配置
- target include 目录配置
- 使用 `${CMAKE_STRIP}` 的 Release-only 构建后 strip 规则

保存项目根目录下的 `CMakeLists.txt` 后，可以触发 CMake configure 刷新，让 clangd 使用更新后的 `compile_commands.json`。

### clangd

clangd 通过工具链运行环境启动，用于提供 C/C++ 智能编辑能力。编辑器设置中提供补全、参数提示、符号高亮、格式化和悬浮提示等功能开关。诊断由语言服务流程统一处理。

### 说明

- 已有项目会保留自己的 `CMakeLists.txt` 和 `.clang-format`；模板变更只影响新建项目。
- 工具链命令会通过应用内的 proot 运行环境在设备上执行。
- 停止构建时会停止 Android 进程，并对活动的 CMake/Ninja 进程做兜底清理。

### 联系方式

- 邮箱：xiaochenzaine@qq.com

### 许可证

本项目基于 MIT License 开源，详见 [LICENSE](LICENSE)。

## English

XCode is an Android C/C++ IDE focused on local development on mobile devices. It combines a code editor, project file tree, CMake/Ninja build workflow, clangd language features, and a proot-based toolchain runtime so C/C++ projects can be edited, configured, and built directly on Android.

### Features

- C/C++ project creation with executable, static library, and shared library templates
- CMake and Ninja project configure/build workflow
- proot-based Linux toolchain runtime integration
- clangd language server support for completion, signature help, symbol highlight, diagnostics, hover, and formatting
- Sora-based code editor with tabs, file tree, search, editor settings, and shortcut symbol bar
- Output panel for configure/build/toolchain logs
- Project configuration for ABI, C++ standard, build type, CMake arguments, and parallel jobs
- Default `.clang-format` generation for new projects
- Release build strip rules generated in template `CMakeLists.txt`

### Project Structure

```text
XCode/
├── app/                         Android application
├── modules/editor-core/         Editor models and shared editor state
├── modules/project-file-tree/   Project tree UI/components
├── modules/toolchain-runtime/   proot command/runtime integration
├── modules/clangd-lsp/          clangd language server bridge
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
- Android NDK `29.0.14206865`
- Android device or emulator running Android 8.0+ (`minSdk 26`)

The app currently targets `arm64-v8a` native packaging.

### Build

Clone the repository and build with Gradle:

```bash
./gradlew assembleDebug
```

For Android Studio, open the repository root and run the `app` configuration.

### C/C++ Project Workflow

XCode creates CMake-based projects and writes a default `CMakeLists.txt` for the selected template.

New projects include:

- Android ABI validation
- output directories under the CMake build directory
- target include directory setup
- Release-only post-build strip rule using `${CMAKE_STRIP}`

Saving the root `CMakeLists.txt` can trigger a CMake configure refresh so clangd can use the updated `compile_commands.json`.

### clangd

clangd is started through the toolchain runtime and is used for C/C++ intelligent editing features. The editor settings expose user-facing feature switches such as completion, signature help, symbol highlight, formatting, and hover. Diagnostics are handled as part of the language server flow.

### Notes

- Existing projects keep their own `CMakeLists.txt` and `.clang-format`; template changes only affect newly created projects.
- Toolchain commands run through the app's proot runtime on device.
- Build cancellation stops the Android process and uses fallback cleanup for active CMake/Ninja processes.

### Contact

- Email: xiaochenzaine@qq.com

### License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
