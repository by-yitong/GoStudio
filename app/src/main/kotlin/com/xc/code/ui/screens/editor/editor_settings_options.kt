package com.xc.code.ui.screens.editor

import com.xc.code.editor.model.editor_settings_state

internal data class editor_settings_switch_item(
    val title: String,
    val description: String,
    val checked: Boolean,
    val update: (Boolean) -> editor_settings_state
)

internal data class editor_settings_switch_group(
    val title: String,
    val items: List<editor_settings_switch_item>
)

internal fun editor_settings_switch_groups(settings: editor_settings_state): List<editor_settings_switch_group> {
    return listOf(
        editor_settings_switch_group(
            title = "行号",
            items = listOf(
                editor_settings_switch_item(
                    title = "显示行号",
                    description = "左侧显示行号",
                    checked = settings.line_numbers,
                    update = { value -> settings.copy(line_numbers = value) }
                )
            )
        ),
        editor_settings_switch_group(
            title = "显示效果",
            items = listOf(
                editor_settings_switch_item(
                    title = "当前行高亮",
                    description = "高亮光标所在行",
                    checked = settings.current_line_highlight,
                    update = { value -> settings.copy(current_line_highlight = value) }
                ),
                editor_settings_switch_item(
                    title = "括号匹配",
                    description = "标出配对括号",
                    checked = settings.bracket_pair_highlight,
                    update = { value -> settings.copy(bracket_pair_highlight = value) }
                ),
                editor_settings_switch_item(
                    title = "代码块线",
                    description = "显示代码块层级线",
                    checked = settings.block_lines,
                    update = { value -> settings.copy(block_lines = value) }
                ),
                editor_settings_switch_item(
                    title = "代码块结束提示",
                    description = "结尾提示代码块开头",
                    checked = settings.block_end_hints,
                    update = { value -> settings.copy(block_end_hints = value) }
                ),
                editor_settings_switch_item(
                    title = "粘滞滚动",
                    description = "顶部固定当前代码块",
                    checked = settings.sticky_scroll,
                    update = { value -> settings.copy(sticky_scroll = value) }
                ),
                editor_settings_switch_item(
                    title = "显示空白符号",
                    description = "显示空格和缩进标记",
                    checked = settings.whitespace_symbols,
                    update = { value -> settings.copy(whitespace_symbols = value) }
                ),
                editor_settings_switch_item(
                    title = "显示行尾符",
                    description = "显示每行换行标记",
                    checked = settings.line_separator,
                    update = { value -> settings.copy(line_separator = value) }
                ),
                editor_settings_switch_item(
                    title = "字体连字",
                    description = "合并显示常见符号",
                    checked = settings.font_ligatures,
                    update = { value -> settings.copy(font_ligatures = value) }
                )
            )
        ),
        editor_settings_switch_group(
            title = "编辑行为",
            items = listOf(
                editor_settings_switch_item(
                    title = "自动换行",
                    description = "长行自动折到下一行",
                    checked = settings.word_wrap,
                    update = { value -> settings.copy(word_wrap = value) }
                ),
                editor_settings_switch_item(
                    title = "手势缩放",
                    description = "双指缩放代码文字",
                    checked = settings.pinch_zoom,
                    update = { value -> settings.copy(pinch_zoom = value) }
                ),
                editor_settings_switch_item(
                    title = "光标闪烁",
                    description = "光标闪烁显示",
                    checked = settings.cursor_blink,
                    update = { value -> settings.copy(cursor_blink = value) }
                ),
                editor_settings_switch_item(
                    title = "自动缩进",
                    description = "换行时自动补缩进",
                    checked = settings.auto_indent,
                    update = { value -> settings.copy(auto_indent = value) }
                )
            )
        ),
        editor_settings_switch_group(
            title = "补全",
            items = listOf(
                editor_settings_switch_item(
                    title = "标识符补全",
                    description = "输入时显示补全建议",
                    checked = settings.auto_completion,
                    update = { value -> settings.copy(auto_completion = value) }
                )
            )
        ),
        editor_settings_switch_group(
            title = "clangd",
            items = listOf(
                editor_settings_switch_item(
                    title = "启用 clangd",
                    description = "启用 C/C++ 智能功能",
                    checked = settings.clangd_enabled,
                    update = { value -> settings.copy(clangd_enabled = value) }
                ),
                editor_settings_switch_item(
                    title = "补全",
                    description = "提供代码补全",
                    checked = settings.clangd_completion,
                    update = { value -> settings.copy(clangd_completion = value) }
                ),
                editor_settings_switch_item(
                    title = "参数提示",
                    description = "显示函数参数",
                    checked = settings.clangd_signature_help,
                    update = { value -> settings.copy(clangd_signature_help = value) }
                ),
                editor_settings_switch_item(
                    title = "符号高亮",
                    description = "标出同一符号",
                    checked = settings.clangd_document_highlight,
                    update = { value -> settings.copy(clangd_document_highlight = value) }
                ),
                editor_settings_switch_item(
                    title = "格式化",
                    description = "整理代码格式",
                    checked = settings.clangd_formatting,
                    update = { value -> settings.copy(clangd_formatting = value) }
                ),
                editor_settings_switch_item(
                    title = "悬浮提示",
                    description = "显示类型和文档",
                    checked = settings.clangd_hover,
                    update = { value -> settings.copy(clangd_hover = value) }
                )
            )
        )
    )
}
