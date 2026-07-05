package com.xc.code.ui.theme

import androidx.compose.ui.graphics.Color

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

val dark_app_colors = app_colors(
    // ===== 渐变 & 背景 =====
    gradient_start = Color(0xFF0B2058),     // 渐变起始色
    gradient_middle = Color(0xFF121A33),    // 渐变中间色
    gradient_end = Color(0xFF141622),       // 渐变结束色
    
    // ===== 文字 =====
    title_large = Color(0xFFE8E8F8),        // 大标题颜色
    title_highlight = Color(0xFFC0CCFF),    // 标题高亮色
    subtitle = Color(0xFF707486),           // 副标题颜色
    section_title = Color(0xFFE8E8F8),      // 区块标题颜色
    
    // ===== 卡片 =====
    card_bg = Color(0xFF1F2230),            // 卡片背景色
    card_pressed = Color(0xFF323446),       // 卡片按下背景色
    card_text_title = Color(0xFFE8E8F8),    // 卡片标题文字颜色
    card_text_subtitle = Color(0xFF88889C), // 卡片副标题文字颜色
    card_icon_bg = Color(0xFFC0CCFF),       // 卡片图标背景色
    card_chevron = Color(0xFF444556),       // 卡片右侧箭头颜色
    
    // ===== 顶部栏 & Logo =====
    logo_tint = Color(0xFFF5F5FF),          // Logo 图标颜色
    top_button_bg = Color(0x15FFFFFF),      // 顶部按钮背景色
    top_button_icon = Color(0xFFE8E8F8),    // 顶部按钮图标颜色
    
    // ===== 搜索 & 输入框 =====
    search_button_active = Color(0xFFC0CCFF),      // 搜索按钮激活时的文字/图标颜色
    search_button_bg_active = Color(0x26C0CCFF),   // 搜索按钮激活时的背景色
    input_hint = Color(0xFF707486),               // 输入框提示文字颜色
    input_text = Color(0xFFE8E8F8),               // 输入框输入文字颜色
    input_border = Color(0xFFC0CCFF),             // 输入框边框颜色（激活时）
    
    // ===== 弹窗 =====
    dialog_bg = Color(0xFF1E1E2A),                // 弹窗背景色
    dialog_text = Color(0xFFE1E1EF),              // 弹窗主要文字颜色
    dialog_hint = Color(0xFF787C8C),              // 弹窗提示/次要文字颜色
    dialog_icon = Color(0xFFC0CCFF),              // 弹窗图标颜色
    dialog_cancel = Color(0xFFC0CCFF),            // 弹窗取消按钮文字颜色
    dialog_clone_bg = Color(0xFFC0CCFF),          // 弹窗确认/克隆按钮背景色
    dialog_clone_text = Color(0xFF1A1A2E),        // 弹窗确认/克隆按钮文字颜色
    dialog_card_bg = Color(0xFF2A2A3A),           // 弹窗内部卡片背景色
    dialog_input_bg = Color(0xFF1E1E2A),          // 弹窗输入框背景色
    dialog_input_text = Color(0xFFE1E1EF),        // 弹窗输入框文字颜色
    dialog_input_hint = Color(0xFF787C8C),        // 弹窗输入框提示文字颜色
    dialog_input_border = Color(0xFFB6C4FF),      // 弹窗输入框边框颜色（激活时）
    dialog_input_icon = Color(0xFFB6C4FF),        // 弹窗输入框图标颜色（激活/有内容时）
    dialog_input_icon_hint = Color(0xFF787C8C),   // 弹窗输入框图标颜色（未激活/空状态时）
    
    // ===== 状态色 =====
    danger = Color(0xFFFF5F57),                   // 危险/错误/删除颜色
    danger_bg = Color(0x29FF5F57),                // 危险/错误/删除弱背景
    success = Color(0xFF35D07F),                  // 成功/已连接颜色
    success_bg = Color(0x2935D07F),               // 成功/已连接弱背景
    warning = Color(0xFFFFBD2E),                  // 警告颜色
    warning_bg = Color(0x29FFBD2E),               // 警告弱背景
    info = Color(0xFFC0CCFF),                     // 信息/强调颜色
    info_bg = Color(0x29C0CCFF),                  // 信息/强调弱背景
    
    // ===== 终端 =====
    terminal_cursor = 0xFFB6C4FF.toInt(),        // 终端光标颜色
    terminal_foreground = 0xFFFFFFFF.toInt(),     // 终端文字颜色
    terminal_background = 0xFF1E1E2A.toInt(),     // 终端背景颜色
    key_button_pressed_bg = Color(0xFF3A3A4A),    // 按钮按下背景色
    key_button_pressed_text = Color(0xFFFFFFFF),  // 按钮按下文字颜色
    key_button_normal_text = Color(0xFF88889C),   // 按钮普通文字颜色
    key_button_active_text = Color(0xFFC0CCFF),   // 按钮激活文字颜色
    terminal_tab_add_icon = Color(0xFFFFFFFF),     // 新建标签按钮颜色
    terminal_tab_separator = Color(0x2EFFFFFF), // 标签分隔线颜色
    terminal_tab_selected_bg = Color(0x00000000), // 选中标签背景色
    terminal_tab_unselected_bg = Color(0x1FFFFFFF), // 未选中标签背景色
    terminal_tab_selected_icon = Color(0xFFC0CCFF), // 选中标签图标颜色
    terminal_tab_selected_text = Color(0xFFFFFFFF), // 选中标签文字颜色
    terminal_tab_unselected_content = Color(0xFFB0B0BE), // 未选中标签文字/图标颜色
    
    // ===== 编辑器 =====
    editor_bg = Color(0xFF1E1E2A),                     // 编辑器页面主背景色
    editor_text = Color(0xFFE1E1EF),                   // 编辑器 UI 主文字颜色，不是代码语法文字
    editor_hint = Color(0xFF787C8C),                   // 编辑器次要文字/提示文字/弱图标颜色
    editor_icon = Color(0xFFC0CCFF),                   // 编辑器强调图标颜色，常用于可操作图标
    editor_toolbar_icon = Color(0xFFE8E8F8),           // 编辑器顶部工具栏图标颜色
    editor_panel_overlay = Color(0xD11E1E2A),          // 悬浮面板/抽屉覆盖层背景色
    editor_button_bg = Color(0x15FFFFFF),              // 编辑器圆形/小按钮背景色
    editor_tab_add_icon = Color(0xFFFFFFFF),           // Tab 栏新增/工具按钮图标颜色
    editor_tab_separator = Color(0x2EFFFFFF),          // Tab 栏底部分隔线/竖向分隔线颜色
    editor_tab_selected_bg = Color(0x00000000),        // 当前选中 Tab 背景色
    editor_tab_unselected_bg = Color(0x1FFFFFFF),      // 未选中 Tab 背景色
    editor_tab_selected_icon = Color(0xFFC0CCFF),      // 当前选中 Tab 图标/状态点颜色
    editor_tab_selected_text = Color(0xFFFFFFFF),      // 当前选中 Tab 文件名文字颜色
    editor_tab_unselected_content = Color(0xFFB0B0BE), // 未选中 Tab 文件名/图标颜色
    editor_sidebar_selected_bg = Color(0x29C0CCFF),    // 侧边栏选中项背景色
    editor_divider = Color(0x4D787C8C),                // 编辑器通用分隔线/文件树节点线颜色
    editor_line_divider = Color(0x2E787C8C)            // 编辑器较弱分隔线颜色
)

val light_app_colors = app_colors(
    // ===== 渐变 & 背景 =====
    gradient_start = Color(0xFFEBEDFF),     // 渐变起始色
    gradient_middle = Color(0xFFF2F1FF),    // 渐变中间色
    gradient_end = Color(0xFFFAF8FF),       // 渐变结束色
    
    // ===== 文字 =====
    title_large = Color(0xFF2D2D3F),        // 大标题颜色
    title_highlight = Color(0xFF1F54E8),    // 标题高亮色
    subtitle = Color(0xFF7A7A8C),           // 副标题颜色
    section_title = Color(0xFF2D2D3F),      // 区块标题颜色
    
    // ===== 卡片 =====
    card_bg = Color(0xFFF1F0FA),            // 卡片背景色
    card_pressed = Color(0xFFE2E2F0),       // 卡片按下背景色
    card_text_title = Color(0xFF1A1A28),    // 卡片标题文字颜色
    card_text_subtitle = Color(0xFF666678), // 卡片副标题文字颜色
    card_icon_bg = Color(0xFF1F54E8),       // 卡片图标背景色
    card_chevron = Color(0xFFB0B0C2),       // 卡片右侧箭头颜色
    
    // ===== 顶部栏 & Logo =====
    logo_tint = Color(0xFF2D2D3F),          // Logo 图标颜色
    top_button_bg = Color(0x15000000),      // 顶部按钮背景色
    top_button_icon = Color(0xFF2D2D3F),    // 顶部按钮图标颜色
    
    // ===== 搜索 & 输入框 =====
    search_button_active = Color(0xFF1F54E8),      // 搜索按钮激活时的文字/图标颜色
    search_button_bg_active = Color(0x261F54E8),   // 搜索按钮激活时的背景色
    input_hint = Color(0xFF7A7A8C),               // 输入框提示文字颜色
    input_text = Color(0xFF2D2D3F),               // 输入框输入文字颜色
    input_border = Color(0xFF1F54E8),             // 输入框边框颜色（激活时）
    
    // ===== 弹窗 =====
    dialog_bg = Color(0xFFFFFFFF),                // 弹窗背景色
    dialog_text = Color(0xFF333333),              // 弹窗主要文字颜色
    dialog_hint = Color(0xFF888888),              // 弹窗提示/次要文字颜色
    dialog_icon = Color(0xFF004DEA),              // 弹窗图标颜色
    dialog_cancel = Color(0xFF004DEA),            // 弹窗取消按钮文字颜色
    dialog_clone_bg = Color(0xFF004DEA),          // 弹窗确认/克隆按钮背景色
    dialog_clone_text = Color(0xFFFFFFFF),        // 弹窗确认/克隆按钮文字颜色
    dialog_card_bg = Color(0xFFF5F5F5),           // 弹窗内部卡片背景色
    dialog_input_bg = Color(0xFFFFFFFF),          // 弹窗输入框背景色
    dialog_input_text = Color(0xFF333333),        // 弹窗输入框文字颜色
    dialog_input_hint = Color(0xFF888888),        // 弹窗输入框提示文字颜色
    dialog_input_border = Color(0xFF004DEA),      // 弹窗输入框边框颜色（激活时）
    dialog_input_icon = Color(0xFF004DEA),        // 弹窗输入框图标颜色（激活/有内容时）
    dialog_input_icon_hint = Color(0xFF888888),   // 弹窗输入框图标颜色（未激活/空状态时）
    
    // ===== 状态色 =====
    danger = Color(0xFFE5484D),                   // 危险/错误/删除颜色
    danger_bg = Color(0x1FE5484D),                // 危险/错误/删除弱背景
    success = Color(0xFF168A4A),                  // 成功/已连接颜色
    success_bg = Color(0x1F168A4A),               // 成功/已连接弱背景
    warning = Color(0xFFB7791F),                  // 警告颜色
    warning_bg = Color(0x1FB7791F),               // 警告弱背景
    info = Color(0xFF1F54E8),                     // 信息/强调颜色
    info_bg = Color(0x1F1F54E8),                  // 信息/强调弱背景
    
    // ===== 终端 =====
    terminal_cursor = 0xFF004DEA.toInt(),        // 终端光标颜色
    terminal_foreground = 0xFF1A1A28.toInt(),     // 终端文字颜色
    terminal_background = 0xFFFFFFFF.toInt(),     // 终端背景颜色
    key_button_pressed_bg = Color(0xFFEEEEEE),    // 按钮按下背景色
    key_button_pressed_text = Color(0xFF000000),  // 按钮按下文字颜色
    key_button_normal_text = Color(0xFF888888),   // 按钮普通文字颜色
    key_button_active_text = Color(0xFF1F54E8),   // 按钮激活文字颜色
    terminal_tab_add_icon = Color(0xFF000000),     // 新建标签按钮颜色
    terminal_tab_separator = Color(0xFFD8D5E0),     // 标签分隔线颜色
    terminal_tab_selected_bg = Color(0x00000000), // 选中标签背景色
    terminal_tab_unselected_bg = Color(0xFFF1EFF6), // 未选中标签背景色
    terminal_tab_selected_icon = Color(0xFF1F54E8), // 选中标签图标颜色
    terminal_tab_selected_text = Color(0xFF000000), // 选中标签文字颜色
    terminal_tab_unselected_content = Color(0xFF4A4A4A), // 未选中标签文字/图标颜色
    
    // ===== 编辑器 =====
    editor_bg = Color(0xFFFFFFFF),                     // 编辑器页面主背景色
    editor_text = Color(0xFF333333),                   // 编辑器 UI 主文字颜色，不是代码语法文字
    editor_hint = Color(0xFF888888),                   // 编辑器次要文字/提示文字/弱图标颜色
    editor_icon = Color(0xFF004DEA),                   // 编辑器强调图标颜色，常用于可操作图标
    editor_toolbar_icon = Color(0xFF2D2D3F),           // 编辑器顶部工具栏图标颜色
    editor_panel_overlay = Color(0xD1FFFFFF),          // 悬浮面板/抽屉覆盖层背景色
    editor_button_bg = Color(0x15000000),              // 编辑器圆形/小按钮背景色
    editor_tab_add_icon = Color(0xFF000000),           // Tab 栏新增/工具按钮图标颜色
    editor_tab_separator = Color(0xFFD8D5E0),          // Tab 栏底部分隔线/竖向分隔线颜色
    editor_tab_selected_bg = Color(0x00000000),        // 当前选中 Tab 背景色
    editor_tab_unselected_bg = Color(0xFFF1EFF6),      // 未选中 Tab 背景色
    editor_tab_selected_icon = Color(0xFF1F54E8),      // 当前选中 Tab 图标/状态点颜色
    editor_tab_selected_text = Color(0xFF000000),      // 当前选中 Tab 文件名文字颜色
    editor_tab_unselected_content = Color(0xFF4A4A4A), // 未选中 Tab 文件名/图标颜色
    editor_sidebar_selected_bg = Color(0x29004DEA),    // 侧边栏选中项背景色
    editor_divider = Color(0x4D888888),                // 编辑器通用分隔线/文件树节点线颜色
    editor_line_divider = Color(0x2E888888)            // 编辑器较弱分隔线颜色
)
