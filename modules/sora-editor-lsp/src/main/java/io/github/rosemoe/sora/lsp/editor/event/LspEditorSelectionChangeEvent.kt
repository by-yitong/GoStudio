package io.github.rosemoe.sora.lsp.editor.event

import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.Unsubscribe
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.highlight.DocumentHighlightEvent
import io.github.rosemoe.sora.lsp.events.highlight.documentHighlight
import io.github.rosemoe.sora.lsp.events.hover.hover
import io.github.rosemoe.sora.lsp.events.signature.signatureHelp
import io.github.rosemoe.sora.lsp.utils.isCursorInsideParens
import io.github.rosemoe.sora.lsp.utils.isCursorOnIdentifier
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.getComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LspEditorSelectionChangeEvent(private val editor: LspEditor) :
    EventReceiver<SelectionChangeEvent> {
    override fun onReceive(event: SelectionChangeEvent, unsubscribe: Unsubscribe) {
        if (!editor.isConnected) {
            return
        }

        // 输入字符产生的选区变化由 ContentChangeEvent 处理，此处跳过避免重复请求
        val causedByTextModification = event.cause == SelectionChangeEvent.CAUSE_TEXT_MODIFICATION

        // 先清掉旧的签名 / 悬浮显示
        editor.showSignatureHelp(null)
        editor.showHover(null)

        editor.coroutineScope.launch(Dispatchers.IO) {
            editor.eventManager.emitAsync(EventType.documentHighlight) {
                put(
                    DocumentHighlightEvent.DocumentHighlightRequest(
                        event.left.fromThis()
                    )
                )
            }
        }

        val originEditor = editor.editor ?: return

        // 在补全列表显示时不弹签名 / 悬浮，避免遮挡
        val isInCompletion = originEditor.getComponent<EditorAutoCompletion>().isShowing
        if (isInCompletion) return

        // 输入字符时交给 ContentChangeEvent 处理；IME 输入过程中的选区变化也不能触发文档悬浮。
        // 文档提示只响应明确的光标定位：点击、鼠标定位、物理键盘/代码控制移动。
        val canRequestHover = when (event.cause) {
            SelectionChangeEvent.CAUSE_TAP,
            SelectionChangeEvent.CAUSE_MOUSE_INPUT,
            SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE -> true
            else -> false
        }
        if (causedByTextModification || !canRequestHover) return

        val text = editor.editor?.text ?: return
        val cursorIndex = event.left.index

        editor.coroutineScope.launch(Dispatchers.IO) {
            if (isCursorInsideParens(text, cursorIndex)) {
                // 光标在 () 括号内 → 请求 signatureHelp（显示参数及类型，高亮当前参数）
                editor.eventManager.emitAsync(EventType.signatureHelp, event.left)
            } else if (isCursorOnIdentifier(text, cursorIndex)) {
                // 光标在标识符上（如函数名）→ 请求 hover（显示函数完整签名 + 文档）
                editor.eventManager.emitAsync(EventType.hover, event.left)
            }
        }
    }
}
