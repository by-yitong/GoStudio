package com.jmwl.gostudio.ui.theme

import androidx.compose.ui.graphics.Color

// Hallmark · studied-DNA (source: CodeAssist 开源 IDE) · macrostructure: IDE 卡片流 + 底部三页导航
// theme: studied-DNA · paper #161719(dark)/#ECEBE7(light) · accent Teal #5CCFE6(dark)/#1C9BBD(light)
// 三阶表面色分层（surface/2/3），1px 分隔线（白/黑 9%），无渐变无阴影，圆角 12-18。
// 排版收敛：标题 28 bold，正文 16，层级靠字重不靠色。间距 4pt 制，卡片间距 12。
data class app_colors(
    // 渐变 & 背景（CodeAssist 无渐变 → 三段统一为 bg 纯色）
    val gradient_start: Color,
    val gradient_middle: Color,
    val gradient_end: Color,

    // 文字
    val title_large: Color,
    val title_highlight: Color,
    val subtitle: Color,
    val section_title: Color,

    // 卡片
    val card_bg: Color,
    val card_pressed: Color,
    val card_text_title: Color,
    val card_text_subtitle: Color,
    val card_icon_bg: Color,
    val card_chevron: Color,

    // 顶部栏 & Logo
    val logo_tint: Color,
    val top_button_bg: Color,
    val top_button_icon: Color,

    // 搜索 & 输入框
    val search_button_active: Color,
    val search_button_bg_active: Color,
    val input_hint: Color,
    val input_text: Color,
    val input_border: Color,

    // 弹窗
    val dialog_bg: Color,
    val dialog_text: Color,
    val dialog_hint: Color,
    val dialog_icon: Color,
    val dialog_cancel: Color,
    val dialog_clone_bg: Color,
    val dialog_clone_text: Color,
    val dialog_card_bg: Color,
    val dialog_input_bg: Color,
    val dialog_input_text: Color,
    val dialog_input_hint: Color,
    val dialog_input_border: Color,
    val dialog_input_icon: Color,
    val dialog_input_icon_hint: Color,

    // 状态色
    val danger: Color,
    val danger_bg: Color,
    val success: Color,
    val success_bg: Color,
    val warning: Color,
    val warning_bg: Color,
    val info: Color,
    val info_bg: Color,

    // 终端
    val terminal_cursor: Int,
    val terminal_foreground: Int,
    val terminal_background: Int,
    val key_button_pressed_bg: Color,
    val key_button_pressed_text: Color,
    val key_button_normal_text: Color,
    val key_button_active_text: Color,
    val terminal_tab_add_icon: Color,
    val terminal_tab_separator: Color,
    val terminal_tab_selected_bg: Color,
    val terminal_tab_unselected_bg: Color,
    val terminal_tab_selected_icon: Color,
    val terminal_tab_selected_text: Color,
    val terminal_tab_unselected_content: Color,

    // 编辑器
    val editor_bg: Color,
    val editor_text: Color,
    val editor_hint: Color,
    val editor_icon: Color,
    val editor_toolbar_icon: Color,
    val editor_panel_overlay: Color,
    val editor_button_bg: Color,
    val editor_tab_add_icon: Color,
    val editor_tab_separator: Color,
    val editor_tab_selected_bg: Color,
    val editor_tab_unselected_bg: Color,
    val editor_tab_selected_icon: Color,
    val editor_tab_selected_text: Color,
    val editor_tab_unselected_content: Color,
    val editor_sidebar_selected_bg: Color,
    val editor_divider: Color,
    val editor_line_divider: Color
)

// ===== 深色模式（CodeAssist darkColors · Teal 强调） =====
// bg #161719 · surface #232428/#2B2C31/#34353B · editor #1B1C1F
// ink #E9E9EC · ink-2 #A0A1AA · muted #6F7079 · accent #5CCFE6 · 分隔线 白 9%/14%/7%
val dark_app_colors = app_colors(
    // ===== 背景（纯色，无渐变） =====
    gradient_start = Color(0xFF161719),
    gradient_middle = Color(0xFF161719),
    gradient_end = Color(0xFF161719),

    // ===== 文字 =====
    title_large = Color(0xFFE9E9EC),
    title_highlight = Color(0xFF5CCFE6),
    subtitle = Color(0xFFA0A1AA),
    section_title = Color(0xFFA0A1AA),

    // ===== 卡片 =====
    card_bg = Color(0xFF232428),
    card_pressed = Color(0xFF34353B),
    card_text_title = Color(0xFFE9E9EC),
    card_text_subtitle = Color(0xFFA0A1AA),
    card_icon_bg = Color(0xFF5CCFE6),
    card_chevron = Color(0xFF6F7079),

    // ===== 顶部栏 & Logo =====
    logo_tint = Color(0xFFE9E9EC),
    top_button_bg = Color(0xFF2B2C31),
    top_button_icon = Color(0xFFA0A1AA),

    // ===== 搜索 & 输入框 =====
    search_button_active = Color(0xFF5CCFE6),
    search_button_bg_active = Color(0x295CCFE6),
    input_hint = Color(0xFFA0A1AA),
    input_text = Color(0xFFE9E9EC),
    input_border = Color(0xFF5CCFE6),

    // ===== 弹窗 =====
    dialog_bg = Color(0xFF2B2C31),
    dialog_text = Color(0xFFE9E9EC),
    dialog_hint = Color(0xFFA0A1AA),
    dialog_icon = Color(0xFF5CCFE6),
    dialog_cancel = Color(0xFF5CCFE6),
    dialog_clone_bg = Color(0xFF5CCFE6),
    dialog_clone_text = Color(0xFFFFFFFF),
    dialog_card_bg = Color(0xFF161719),
    dialog_input_bg = Color(0xFF161719),
    dialog_input_text = Color(0xFFE9E9EC),
    dialog_input_hint = Color(0xFFA0A1AA),
    dialog_input_border = Color(0xFF5CCFE6),
    dialog_input_icon = Color(0xFF5CCFE6),
    dialog_input_icon_hint = Color(0xFFA0A1AA),

    // ===== 状态色 =====
    danger = Color(0xFFFF6B63),
    danger_bg = Color(0x29FF6B63),
    success = Color(0xFF34D058),
    success_bg = Color(0x2934D058),
    warning = Color(0xFFFFB340),
    warning_bg = Color(0x29FFB340),
    info = Color(0xFF5AC8E0),
    info_bg = Color(0x295AC8E0),

    // ===== 终端 =====
    terminal_cursor = 0xFF5CCFE6.toInt(),
    terminal_foreground = 0xFFE9E9EC.toInt(),
    terminal_background = 0xFF161719.toInt(),
    key_button_pressed_bg = Color(0xFF34353B),
    key_button_pressed_text = Color(0xFFFFFFFF),
    key_button_normal_text = Color(0xFFA0A1AA),
    key_button_active_text = Color(0xFF5CCFE6),
    terminal_tab_add_icon = Color(0xFFE9E9EC),
    terminal_tab_separator = Color(0x17FFFFFF),
    terminal_tab_selected_bg = Color(0x00000000),
    terminal_tab_unselected_bg = Color(0xFF232428),
    terminal_tab_selected_icon = Color(0xFF5CCFE6),
    terminal_tab_selected_text = Color(0xFFE9E9EC),
    terminal_tab_unselected_content = Color(0xFFA0A1AA),

    // ===== 编辑器 =====
    editor_bg = Color(0xFF1B1C1F),
    editor_text = Color(0xFFE9E9EC),
    editor_hint = Color(0xFFA0A1AA),
    editor_icon = Color(0xFF5CCFE6),
    editor_toolbar_icon = Color(0xFFA0A1AA),
    editor_panel_overlay = Color(0xD1161719),
    editor_button_bg = Color(0xFF2B2C31),
    editor_tab_add_icon = Color(0xFFE9E9EC),
    editor_tab_separator = Color(0x17FFFFFF),
    editor_tab_selected_bg = Color(0x00000000),
    editor_tab_unselected_bg = Color(0xFF232428),
    editor_tab_selected_icon = Color(0xFF5CCFE6),
    editor_tab_selected_text = Color(0xFFE9E9EC),
    editor_tab_unselected_content = Color(0xFFA0A1AA),
    editor_sidebar_selected_bg = Color(0x295CCFE6),
    editor_divider = Color(0x1FFFFFFF),
    editor_line_divider = Color(0x12FFFFFF)
)

// ===== 浅色模式（CodeAssist lightColors · Teal 强调） =====
// bg #ECEBE7 暖米白 · surface #FFFFFF/#F4F3EF/#E8E7E1 · editor #FAF9F6
// ink #1D1E22 · ink-2 #62636B · muted #97989F · accent #1C9BBD · 分隔线 黑 9%/14%/6%
val light_app_colors = app_colors(
    // ===== 背景（纯色，无渐变） =====
    gradient_start = Color(0xFFECEBE7),
    gradient_middle = Color(0xFFECEBE7),
    gradient_end = Color(0xFFECEBE7),

    // ===== 文字 =====
    title_large = Color(0xFF1D1E22),
    title_highlight = Color(0xFF1C9BBD),
    subtitle = Color(0xFF62636B),
    section_title = Color(0xFF62636B),

    // ===== 卡片 =====
    card_bg = Color(0xFFFFFFFF),
    card_pressed = Color(0xFFE8E7E1),
    card_text_title = Color(0xFF1D1E22),
    card_text_subtitle = Color(0xFF62636B),
    card_icon_bg = Color(0xFF1C9BBD),
    card_chevron = Color(0xFF97989F),

    // ===== 顶部栏 & Logo =====
    logo_tint = Color(0xFF1D1E22),
    top_button_bg = Color(0xFFF4F3EF),
    top_button_icon = Color(0xFF62636B),

    // ===== 搜索 & 输入框 =====
    search_button_active = Color(0xFF1C9BBD),
    search_button_bg_active = Color(0x261C9BBD),
    input_hint = Color(0xFF62636B),
    input_text = Color(0xFF1D1E22),
    input_border = Color(0xFF1C9BBD),

    // ===== 弹窗 =====
    dialog_bg = Color(0xFFFCFBF9),
    dialog_text = Color(0xFF1D1E22),
    dialog_hint = Color(0xFF62636B),
    dialog_icon = Color(0xFF1C9BBD),
    dialog_cancel = Color(0xFF1C9BBD),
    dialog_clone_bg = Color(0xFF1C9BBD),
    dialog_clone_text = Color(0xFFFFFFFF),
    dialog_card_bg = Color(0xFFECEBE7),
    dialog_input_bg = Color(0xFFFFFFFF),
    dialog_input_text = Color(0xFF1D1E22),
    dialog_input_hint = Color(0xFF62636B),
    dialog_input_border = Color(0xFF1C9BBD),
    dialog_input_icon = Color(0xFF1C9BBD),
    dialog_input_icon_hint = Color(0xFF62636B),

    // ===== 状态色 =====
    danger = Color(0xFFDF4A45),
    danger_bg = Color(0x1FDF4A45),
    success = Color(0xFF29A847),
    success_bg = Color(0x1F29A847),
    warning = Color(0xFFD98300),
    warning_bg = Color(0x1FD98300),
    info = Color(0xFF2399B8),
    info_bg = Color(0x1F2399B8),

    // ===== 终端 =====
    terminal_cursor = 0xFF1C9BBD.toInt(),
    terminal_foreground = 0xFF1D1E22.toInt(),
    terminal_background = 0xFFFAF9F6.toInt(),
    key_button_pressed_bg = Color(0xFFE8E7E1),
    key_button_pressed_text = Color(0xFF1D1E22),
    key_button_normal_text = Color(0xFF62636B),
    key_button_active_text = Color(0xFF1C9BBD),
    terminal_tab_add_icon = Color(0xFF1D1E22),
    terminal_tab_separator = Color(0x17000000),
    terminal_tab_selected_bg = Color(0x00000000),
    terminal_tab_unselected_bg = Color(0xFFF4F3EF),
    terminal_tab_selected_icon = Color(0xFF1C9BBD),
    terminal_tab_selected_text = Color(0xFF1D1E22),
    terminal_tab_unselected_content = Color(0xFF62636B),

    // ===== 编辑器 =====
    editor_bg = Color(0xFFFAF9F6),
    editor_text = Color(0xFF1D1E22),
    editor_hint = Color(0xFF62636B),
    editor_icon = Color(0xFF1C9BBD),
    editor_toolbar_icon = Color(0xFF62636B),
    editor_panel_overlay = Color(0xD1ECEBE7),
    editor_button_bg = Color(0xFFF4F3EF),
    editor_tab_add_icon = Color(0xFF1D1E22),
    editor_tab_separator = Color(0x17000000),
    editor_tab_selected_bg = Color(0x00000000),
    editor_tab_unselected_bg = Color(0xFFF4F3EF),
    editor_tab_selected_icon = Color(0xFF1C9BBD),
    editor_tab_selected_text = Color(0xFF1D1E22),
    editor_tab_unselected_content = Color(0xFF62636B),
    editor_sidebar_selected_bg = Color(0x261C9BBD),
    editor_divider = Color(0x1F000000),
    editor_line_divider = Color(0x0F000000)
)
