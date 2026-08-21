# GoStudio UI/UX 重设计 — Hallmark · modern-minimal

> 用户委托("你决定")，已推断：audience = 移动端 Go 开发者 · use = 手机上编辑/构建/调试 Go + AI 助手 · **tone = modern-minimal**（dev-tool，非 consumer）。方向 = **Studio · 现代极简**：冷调中性底 + 单一克制 Go-blue 强调色 + 发丝级边框 + 真正加载 Inter(JetBrains Mono for code)。

## 改什么 / 不改什么（非破坏性）

**改（视觉+交互层）：** 调色板 token 值、字体加载、design.md 系统文件、5 处硬编码颜色清理。
**不改（逻辑/架构）：** 路由树、Activity 结构、组件目录、`app_colors` 的字段 schema（只换值，不动 70 个字段名）、Sora 编辑器逻辑、AI agent loop。

这是 token-driven app — 换 `color.kt` 的值会自动级联到全部 ~30 个页面，是覆盖最大、diff 最小的路径。

---

## Step 1 — 写 `design.md`（项目根，锁定系统）

新建 `/run/media/admins/DATA/myitem/project/GoStudio/design.md`，内容为 Hallmark 多页面流的 locked system：genre=modern-minimal · macrostructure family（app=Workbench · 设置/列表=Long Document）· 完整 OKLCH 调色板 · Inter Tight+Inter+JetBrains Mono · 4pt spacing · motion minimal · CTA voice（pill / 6px radius / 填充式主按钮）· 每页允许的强调色占比(≤5%，功能态用) · exports 段（tokens.css / Tailwind / DTCG / shadcn 四格式，便于他项目复用）。

## Step 2 — 重写调色板 `app/src/main/kotlin/com/jmwl/gostudio/ui/theme/color.kt`

替换 `dark_app_colors` 与 `light_app_colors` 的全部 ~70 个 hex 值。**字段 schema 不变**（所以所有调用点零改动）。核心转变：

| token | 旧(深色) | 新(深色) | 说明 |
|---|---|---|---|
| gradient_start/middle/end | 三段深蓝渐变 `0B2058→121A33→141622` | 全部 `#17181D` | **杀死渐变**（modern-minimal gate）→ 纯净中性底 |
| editor_bg / paper | `1E1E2A` | `#17181D` | 冷调近黑，带极淡蓝调（非纯黑） |
| card_bg | `1F2230` | `#20232B` | 比纸高 ~3% lightness（elevation 规则） |
| title_large/ink | `E8E8F8`（偏紫） | `#E8EAEE` | 冷调中性 ink |
| subtitle/hint | `707486` | `#8A8F9A` | 冷调中性 500 |
| **title_highlight / accent 全族** | `C0CCFF`（藻紫） | `#5B8DEF` | **单一 Go-blue 强调色**（浅色模式锁定 `#1F54E8`） |
| danger/success/warning | `FF5F57/35D07F/FFBD2E` | `#F87171/#34D399/#FBBF24` | 略降饱和、对齐 Tailwind 调性 |
| terminal_background | `1E1E2A` | `#121317` | 比编辑器略深一层 |

浅色模式同理：纸 `#FAFBFC`、card `#F1F3F6`、ink `#1A1D23`、subtitle `#5F6571`、强调 `#1F54E8`。

## Step 3 — 修字体 `app/src/main/kotlin/com/jmwl/gostudio/ui/theme/type.kt`

当前 `jetbrains_mono = FontFamily(Font(R.font.roboto_regular))` 是 bug（加载了 Roboto）。重写为：
- `app_sans = FontFamily(Inter Regular/Medium/SemiBold/Bold)` → 用于全部 UI 文本（headline/title/body/label 12 个 style）
- `jetbrains_mono = FontFamily(Font(R.font.jetbrains_mono_regular))` → 修正，用于代码相关文本
- headline 字重收紧（Bold→SemiBold 更精致），letterSpacing 收紧到 display `-0.02em`、body `0`

## Step 4 — 下载 Inter 字体到 `app/src/main/res/font/`

用 curl 拉 4 个 Inter 字重（Regular 400 / Medium 500 / SemiBold 600 / Bold 700），命名符合 Android resource 规则：`inter_regular.ttf`、`inter_medium.ttf`、`inter_semibold.ttf`、`inter_bold.ttf`。JetBrains Mono 已在位，无需下载。约 +600KB APK（对 IDE 可忽略）。

## Step 5 — 对齐残留 Material3 scheme `theme.kt:23-33`

把 `dark_color_scheme`/`light_color_scheme` 的 primary/secondary/tertiary 从默认紫换成新调色板的 accent/neutral，消除 install_screen + editor_search_panel 那 5 处 `MaterialTheme.colorScheme.*` 的视觉割裂。

## Step 6 — 清理硬编码颜色（让全 app 真正服从系统）

8 个文件，把硬编码 `Color(0x…)` / `Color.White`/`Black`/`Gray` 换成 `app_theme_provider.colors.*` token：
- `ui/screens/splash/splash_screen.kt`（8 处）— 白→浅灰渐变 + 青"Go" → 改用 token，"Go"用 accent
- `ui/dialogs/splash/splash_permission_dialogs.kt`（6 处）
- `ui/screens/install/install_screen.kt`（12 处）— **移除 macOS 红黄绿小圆点假 chrome**（re-drawn chrome ban），终端 mock 改用 token
- `ui/screens/editor/editor_chrome.kt`（2 处）— 未保存点 `2F80FF`、stop `FF5252` → `accent` / `danger`
- `ui/screens/ai/ai_chat_panel.kt`（2 处 `Color.White`）→ `onAccent`/token
- `ui/screens/ai/ai_markdown_renderer.kt`（2 处）— 代码块底 `0x22808080`、链接蓝 `4A9EFF` → token
- `ui/screens/ai/ai_settings_screen.kt`（3 处 `Color.White`）→ token
- `activity/crash/crash_activity.kt`（10 处）— 套 `app_theme_provider`，红绿黄用 token

## Step 7 — Slop test 自检 + 收尾

跑 Hallmark 58-gate 心检：渐变死、纯黑纯白无、强调色单一且克制、字体真加载、假 chrome 移除、dark/light 双模对比度达标。在 `color.kt` 顶部加 Hallmark stamp 注释（macrostructure/tone/accent）。无需删任何文件、不动路由。

---

**Hallmark · 预览**
- **Macrostructure** · Workbench(app) / Long Document(列表设置页)
- **Theme** · custom modern-minimal · paper oklch(~16% 深/~98% 浅) 冷调中性 · accent Go-blue oklch(深 68% 0.14 / 浅 52% 0.19 @255°) · Inter Tight + Inter + JetBrains Mono
- **Enrichment** · none（typography only · app pages）
- **Motion** · 不新增；现状 tween fade/slide 保留，符合 minimal
- **Slop test** · 目标 58/58 ✓（build 后跑）
- **Diversification** · 首次 Hallmark run，无前序约束

**文件清单：**
- 新建：`design.md`、`app/src/main/res/font/inter_{regular,medium,semibold,bold}.ttf`
- 改值：`ui/theme/color.kt`、`ui/theme/type.kt`、`ui/theme/theme.kt`
- 清理硬编码：splash_screen.kt、splash_permission_dialogs.kt、install_screen.kt、editor_chrome.kt、ai_chat_panel.kt、ai_markdown_renderer.kt、ai_settings_screen.kt、crash_activity.kt
- **删除：无**