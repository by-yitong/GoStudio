package com.jmwl.gostudio.ui.theme

import androidx.compose.ui.graphics.Color

// Hallmark · genre: modern-minimal · macrostructure: Workbench(app)/Long Document(list) ·
// design-system: design.md · designed-as-app · anchor hue: 255 (cool Go-blue) ·
// accent: oklch(68% 0.14 255) dark / oklch(52% 0.19 255) light · one restrained signal accent ≤5% ·
// fonts: Inter (UI) + JetBrains Mono (code). No pure black, no pure white, no gradients.
// All neutrals tinted cool toward the 255 anchor. Status colors desaturated to Tailwind register.

data class app_colors(
    // 渐变 & 背景
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

// ===== 深色模式 =====
// paper #17181D · surface #20232B · sunken #121317 · rule #343841
// ink #E8EAEE · ink-2 #C8CCD4 · muted #8A8F9A · accent #5B8DEF · accent-ink #FAFBFC
val dark_app_colors = app_colors(
    // ===== 渐变 & 背景（杀死渐变 → 三段统一为 paper） =====
    gradient_start = Color(0xFF17181D),
    gradient_middle = Color(0xFF17181D),
    gradient_end = Color(0xFF17181D),

    // ===== 文字 =====
    title_large = Color(0xFFE8EAEE),
    title_highlight = Color(0xFF5B8DEF),
    subtitle = Color(0xFF8A8F9A),
    section_title = Color(0xFFE8EAEE),

    // ===== 卡片 =====
    card_bg = Color(0xFF20232B),
    card_pressed = Color(0xFF2C3038),
    card_text_title = Color(0xFFE8EAEE),
    card_text_subtitle = Color(0xFF8A8F9A),
    card_icon_bg = Color(0xFF5B8DEF),
    card_chevron = Color(0xFF4A4F5A),

    // ===== 顶部栏 & Logo =====
    logo_tint = Color(0xFFE8EAEE),
    top_button_bg = Color(0xFF20232B),
    top_button_icon = Color(0xFFC8CCD4),

    // ===== 搜索 & 输入框 =====
    search_button_active = Color(0xFF5B8DEF),
    search_button_bg_active = Color(0x265B8DEF),
    input_hint = Color(0xFF8A8F9A),
    input_text = Color(0xFFE8EAEE),
    input_border = Color(0xFF5B8DEF),

    // ===== 弹窗 =====
    dialog_bg = Color(0xFF20232B),
    dialog_text = Color(0xFFE8EAEE),
    dialog_hint = Color(0xFF8A8F9A),
    dialog_icon = Color(0xFF5B8DEF),
    dialog_cancel = Color(0xFF5B8DEF),
    dialog_clone_bg = Color(0xFF5B8DEF),
    dialog_clone_text = Color(0xFFFAFBFC),
    dialog_card_bg = Color(0xFF17181D),
    dialog_input_bg = Color(0xFF17181D),
    dialog_input_text = Color(0xFFE8EAEE),
    dialog_input_hint = Color(0xFF8A8F9A),
    dialog_input_border = Color(0xFF5B8DEF),
    dialog_input_icon = Color(0xFF74A0FF),
    dialog_input_icon_hint = Color(0xFF8A8F9A),

    // ===== 状态色 =====
    danger = Color(0xFFF87171),
    danger_bg = Color(0x29F87171),
    success = Color(0xFF34D399),
    success_bg = Color(0x2934D399),
    warning = Color(0xFFFBBF24),
    warning_bg = Color(0x29FBBF24),
    info = Color(0xFF5B8DEF),
    info_bg = Color(0x295B8DEF),

    // ===== 终端（比编辑器略深一层） =====
    terminal_cursor = 0xFF74A0FF.toInt(),
    terminal_foreground = 0xFFE8EAEE.toInt(),
    terminal_background = 0xFF121317.toInt(),
    key_button_pressed_bg = Color(0xFF2C3038),
    key_button_pressed_text = Color(0xFFFAFBFC),
    key_button_normal_text = Color(0xFF8A8F9A),
    key_button_active_text = Color(0xFF5B8DEF),
    terminal_tab_add_icon = Color(0xFFE8EAEE),
    terminal_tab_separator = Color(0xFF343841),
    terminal_tab_selected_bg = Color(0x00000000),
    terminal_tab_unselected_bg = Color(0xFF20232B),
    terminal_tab_selected_icon = Color(0xFF5B8DEF),
    terminal_tab_selected_text = Color(0xFFE8EAEE),
    terminal_tab_unselected_content = Color(0xFF8A8F9A),

    // ===== 编辑器 =====
    editor_bg = Color(0xFF17181D),
    editor_text = Color(0xFFE8EAEE),
    editor_hint = Color(0xFF8A8F9A),
    editor_icon = Color(0xFF5B8DEF),
    editor_toolbar_icon = Color(0xFFC8CCD4),
    editor_panel_overlay = Color(0xD117181D),
    editor_button_bg = Color(0xFF20232B),
    editor_tab_add_icon = Color(0xFFE8EAEE),
    editor_tab_separator = Color(0xFF343841),
    editor_tab_selected_bg = Color(0x00000000),
    editor_tab_unselected_bg = Color(0xFF20232B),
    editor_tab_selected_icon = Color(0xFF5B8DEF),
    editor_tab_selected_text = Color(0xFFE8EAEE),
    editor_tab_unselected_content = Color(0xFF8A8F9A),
    editor_sidebar_selected_bg = Color(0x295B8DEF),
    editor_divider = Color(0x4D343841),
    editor_line_divider = Color(0x2E343841)
)

// ===== 浅色模式 =====
// paper #FAFBFC · surface #F1F3F6 · sunken #FFFFFF · rule #E0E3E8
// ink #1A1D23 · ink-2 #2C303A · muted #5F6571 · accent #1F54E8 · accent-ink #FFFFFF
val light_app_colors = app_colors(
    // ===== 渐变 & 背景（杀死渐变 → 三段统一为 paper） =====
    gradient_start = Color(0xFFFAFBFC),
    gradient_middle = Color(0xFFFAFBFC),
    gradient_end = Color(0xFFFAFBFC),

    // ===== 文字 =====
    title_large = Color(0xFF1A1D23),
    title_highlight = Color(0xFF1F54E8),
    subtitle = Color(0xFF5F6571),
    section_title = Color(0xFF1A1D23),

    // ===== 卡片 =====
    card_bg = Color(0xFFF1F3F6),
    card_pressed = Color(0xFFE5E8ED),
    card_text_title = Color(0xFF1A1D23),
    card_text_subtitle = Color(0xFF5F6571),
    card_icon_bg = Color(0xFF1F54E8),
    card_chevron = Color(0xFFA8AEB8),

    // ===== 顶部栏 & Logo =====
    logo_tint = Color(0xFF1A1D23),
    top_button_bg = Color(0xFFF1F3F6),
    top_button_icon = Color(0xFF2C303A),

    // ===== 搜索 & 输入框 =====
    search_button_active = Color(0xFF1F54E8),
    search_button_bg_active = Color(0x261F54E8),
    input_hint = Color(0xFF5F6571),
    input_text = Color(0xFF1A1D23),
    input_border = Color(0xFF1F54E8),

    // ===== 弹窗 =====
    dialog_bg = Color(0xFFFFFFFF),
    dialog_text = Color(0xFF1A1D23),
    dialog_hint = Color(0xFF5F6571),
    dialog_icon = Color(0xFF1F54E8),
    dialog_cancel = Color(0xFF1F54E8),
    dialog_clone_bg = Color(0xFF1F54E8),
    dialog_clone_text = Color(0xFFFFFFFF),
    dialog_card_bg = Color(0xFFF1F3F6),
    dialog_input_bg = Color(0xFFFFFFFF),
    dialog_input_text = Color(0xFF1A1D23),
    dialog_input_hint = Color(0xFF5F6571),
    dialog_input_border = Color(0xFF1F54E8),
    dialog_input_icon = Color(0xFF1F54E8),
    dialog_input_icon_hint = Color(0xFF5F6571),

    // ===== 状态色 =====
    danger = Color(0xFFE5484D),
    danger_bg = Color(0x1FE5484D),
    success = Color(0xFF168A4A),
    success_bg = Color(0x1F168A4A),
    warning = Color(0xFFB7791F),
    warning_bg = Color(0x1FB7791F),
    info = Color(0xFF1F54E8),
    info_bg = Color(0x1F1F54E8),

    // ===== 终端 =====
    terminal_cursor = 0xFF1F54E8.toInt(),
    terminal_foreground = 0xFF1A1D23.toInt(),
    terminal_background = 0xFFFFFFFF.toInt(),
    key_button_pressed_bg = Color(0xFFE5E8ED),
    key_button_pressed_text = Color(0xFF1A1D23),
    key_button_normal_text = Color(0xFF5F6571),
    key_button_active_text = Color(0xFF1F54E8),
    terminal_tab_add_icon = Color(0xFF1A1D23),
    terminal_tab_separator = Color(0xFFE0E3E8),
    terminal_tab_selected_bg = Color(0x00000000),
    terminal_tab_unselected_bg = Color(0xFFF1F3F6),
    terminal_tab_selected_icon = Color(0xFF1F54E8),
    terminal_tab_selected_text = Color(0xFF1A1D23),
    terminal_tab_unselected_content = Color(0xFF5F6571),

    // ===== 编辑器 =====
    editor_bg = Color(0xFFFFFFFF),
    editor_text = Color(0xFF1A1D23),
    editor_hint = Color(0xFF5F6571),
    editor_icon = Color(0xFF1F54E8),
    editor_toolbar_icon = Color(0xFF2C303A),
    editor_panel_overlay = Color(0xD1FAFBFC),
    editor_button_bg = Color(0xFFF1F3F6),
    editor_tab_add_icon = Color(0xFF1A1D23),
    editor_tab_separator = Color(0xFFE0E3E8),
    editor_tab_selected_bg = Color(0x00000000),
    editor_tab_unselected_bg = Color(0xFFF1F3F6),
    editor_tab_selected_icon = Color(0xFF1F54E8),
    editor_tab_selected_text = Color(0xFF1A1D23),
    editor_tab_unselected_content = Color(0xFF5F6571),
    editor_sidebar_selected_bg = Color(0x291F54E8),
    editor_divider = Color(0x4DE0E3E8),
    editor_line_divider = Color(0x2EE0E3E8)
)
