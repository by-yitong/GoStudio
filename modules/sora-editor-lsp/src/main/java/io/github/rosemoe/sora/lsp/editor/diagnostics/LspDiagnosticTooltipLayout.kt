package io.github.rosemoe.sora.lsp.editor.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorInt
import io.github.rosemoe.sora.lsp.R
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.curvedTextScale
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.utils.asLspPosition
import io.github.rosemoe.sora.lsp.utils.blendARGB
import io.github.rosemoe.sora.lsp.utils.createTextDocumentIdentifier
import io.github.rosemoe.sora.widget.component.DiagnosticTooltipLayout
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.abs

/**
 * Diagnostic tooltip layout tuned for LSP.
 *
 * 在诊断详情下方追加「快速修复」列表：显示诊断时同时请求 gopls 的 code action，
 * 把可用修复（如 Add import）渲染为可点击条目，点击即应用。
 */
class LspDiagnosticTooltipLayout(
    private val lspEditor: LspEditor? = null
) : DiagnosticTooltipLayout {

    private lateinit var window: EditorDiagnosticTooltipWindow
    private lateinit var root: View
    private lateinit var detailMessageText: TextView
    private lateinit var messagePanel: ViewGroup
    private lateinit var copyButton: ImageButton
    private lateinit var quickfixTitle: TextView
    private lateinit var quickfixContainer: ViewGroup
    private var quickfixTextColor: Int = 0

    private val backgroundDrawable = GradientDrawable()
    private val severityColors = mutableMapOf<Short, Int>()
    private var appliedRegion: DiagnosticRegion? = null
    private var baseBackgroundColor: Int = Color.TRANSPARENT
    private var borderWidthPx: Int = 1
    private var cornerRadiusDp: Float = 8f
    private var borderWidthDp: Float = 1f
    private var severityBlendRatio = 0.25f
    private var pointerOverPopup = false
    private var baselineEditorTextSizePx: Float? = null
    private var baselineDetailTextSizePx: Float? = null
    private var appliedDetailTextSizePx: Float = -1f
    private var pendingEditorTextSizePx: Float? = null

    init {
        installDefaultSeverityPalette()
    }

    override fun attach(window: EditorDiagnosticTooltipWindow) {
        this.window = window
    }

    override fun createView(inflater: LayoutInflater): View {
        val context = inflater.context
        val view = inflater.inflate(R.layout.lsp_diagnostic_tooltip_window, null)
        root = view
        root.clipToOutline = true

        root.setOnGenericMotionListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_ENTER -> pointerOverPopup = true
                MotionEvent.ACTION_HOVER_EXIT -> pointerOverPopup = false
            }
            false
        }

        detailMessageText = view.findViewById(R.id.diagnostic_tooltip_detailed_message)
        messagePanel = view.findViewById(R.id.diagnostic_container_message)
        copyButton = view.findViewById(R.id.diagnostic_copy_button)
        copyButton.setOnClickListener { copyDetailedMessageToClipboard() }
        copyButton.setOnHoverListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_ENTER -> pointerOverPopup = true
                MotionEvent.ACTION_HOVER_EXIT -> pointerOverPopup = false
            }
            false
        }
        copyButton.isEnabled = false
        copyButton.visibility = View.GONE
        updateCopyButtonTint(null)
        baselineDetailTextSizePx = detailMessageText.textSize
        val initialEditorSize = pendingEditorTextSizePx ?: window.editor.textSizePx
        applyDetailMessageTextSize(initialEditorSize)
        pendingEditorTextSizePx = null

        // 快速修复区：标题 + 列表容器，加进 messagePanel（ScrollView 内，自动可滚动）
        quickfixTitle = TextView(context).apply {
            visibility = View.GONE
            setPadding(0, (window.editor.dpUnit * 6).toInt(), 0, (window.editor.dpUnit * 4).toInt())
            text = "快速修复"
            textSize = detailMessageText.textSize / context.resources.displayMetrics.density
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        messagePanel.addView(quickfixTitle)

        quickfixContainer = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            visibility = View.GONE
        }
        messagePanel.addView(quickfixContainer)

        return view
    }

    override fun applyColorScheme(colorScheme: EditorColorScheme) {
        val editor = window.editor
        detailMessageText.setTextColor(colorScheme.getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_DETAILED_MSG))
        baseBackgroundColor = colorScheme.getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_BACKGROUND)
        borderWidthPx = (editor.dpUnit * borderWidthDp).toInt().coerceAtLeast(1)
        backgroundDrawable.cornerRadius = editor.dpUnit * cornerRadiusDp
        backgroundDrawable.setStroke(borderWidthPx, resolveBorderColor(appliedRegion))
        backgroundDrawable.setColor(resolveFillColor(appliedRegion))
        root.background = backgroundDrawable
        // 快速修复区文字色复用诊断详情文字色
        quickfixTextColor = colorScheme.getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_DETAILED_MSG)
        if (::quickfixTitle.isInitialized) quickfixTitle.setTextColor(quickfixTextColor)
        updateCopyButtonTint(appliedRegion)
    }

    override fun onTextSizeChanged(oldSizePx: Float, newSizePx: Float) {
        if (newSizePx <= 0f) {
            return
        }
        if (!::detailMessageText.isInitialized) {
            pendingEditorTextSizePx = newSizePx
            return
        }
        applyDetailMessageTextSize(newSizePx)
    }

    override fun renderDiagnostic(diagnostic: DiagnosticDetail?) {
        renderDiagnostic(diagnostic, appliedRegion)
    }

    override fun renderDiagnostic(diagnostic: DiagnosticDetail?, region: DiagnosticRegion?) {
        appliedRegion = region
        quickfixContainer.removeAllViews()
        quickfixContainer.visibility = View.GONE
        if (diagnostic == null) {
            detailMessageText.text = ""
            detailMessageText.visibility = View.GONE
            copyButton.isEnabled = false
            copyButton.visibility = View.GONE
            backgroundDrawable.setStroke(borderWidthPx, resolveBorderColor(null))
            backgroundDrawable.setColor(resolveFillColor(null))
            updateCopyButtonTint(null)
            return
        }
        val detailedMessage = diagnostic.detailedMessage
        if (detailedMessage.isNullOrEmpty()) {
            detailMessageText.text = ""
            detailMessageText.visibility = View.GONE
            copyButton.isEnabled = false
            copyButton.visibility = View.GONE
            updateCopyButtonTint(null)
        } else {
            detailMessageText.visibility = View.VISIBLE
            detailMessageText.text = detailedMessage
            copyButton.isEnabled = true
            copyButton.visibility = View.VISIBLE
            updateCopyButtonTint(region)
        }
        backgroundDrawable.setStroke(borderWidthPx, resolveBorderColor(region))
        backgroundDrawable.setColor(resolveFillColor(region))

        // 请求该诊断的 code action，填充快速修复列表
        requestQuickfix(region)
    }

    /**
     * 用诊断的 range 请求 gopls code action，把可用修复渲染为可点击条目。
     */
    private fun requestQuickfix(region: DiagnosticRegion?) {
        val editor = lspEditor ?: return
        val start = region?.startIndex
        val end = region?.endIndex
        if (start == null || end == null) return
        val originEditor = editor.editor ?: return
        val startPos = originEditor.text.getIndexer().getCharPosition(start)
        val endPos = originEditor.text.getIndexer().getCharPosition(end)
        val range = org.eclipse.lsp4j.Range(
            startPos.asLspPosition(),
            endPos.asLspPosition()
        )
        editor.coroutineScope.launch(Dispatchers.IO) {
            val diagnostics = editor.diagnosticsContainer.findDiagnostics(editor.uri, range) ?: emptyList()
            val params = org.eclipse.lsp4j.CodeActionParams(
                editor.uri.createTextDocumentIdentifier(),
                range,
                org.eclipse.lsp4j.CodeActionContext(diagnostics)
            )
            val future = editor.requestManager.codeAction(params)
            val list = try {
                withTimeout(2000) { future?.await().orEmpty() }
            } catch (_: Exception) {
                emptyList()
            }
            withContext(Dispatchers.Main) {
                renderQuickfixList(list.orEmpty())
            }
        }
    }

    private fun renderQuickfixList(actions: List<org.eclipse.lsp4j.jsonrpc.messages.Either<org.eclipse.lsp4j.Command, org.eclipse.lsp4j.CodeAction>>) {
        if (!::quickfixContainer.isInitialized) return
        quickfixContainer.removeAllViews()
        if (actions.isEmpty()) {
            quickfixContainer.visibility = View.GONE
            if (::quickfixTitle.isInitialized) quickfixTitle.visibility = View.GONE
            return
        }
        val context = root.context
        val editor = window.editor
        val hPad = (editor.dpUnit * 12).toInt()
        val vPad = (editor.dpUnit * 8).toInt()
        actions.forEach { either ->
            val title = when {
                either.isLeft -> either.left?.title?.ifBlank { either.left?.command } ?: either.left?.command ?: "<action>"
                else -> either.right?.title?.ifBlank { "action" } ?: "action"
            }
            val item = TextView(context).apply {
                text = title
                if (quickfixTextColor != 0) setTextColor(quickfixTextColor)
                setPadding(hPad, vPad, hPad, vPad)
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                val outValue = android.util.TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                setOnClickListener { applyCodeAction(either) }
            }
            quickfixContainer.addView(item)
        }
        if (::quickfixTitle.isInitialized) {
            if (quickfixTextColor != 0) quickfixTitle.setTextColor(quickfixTextColor)
            quickfixTitle.visibility = View.VISIBLE
        }
        quickfixContainer.visibility = View.VISIBLE
        // 修复列表异步加载完成，刷新窗口尺寸以容纳新增内容
        window.refreshWindowSize()
    }

    private fun applyCodeAction(either: org.eclipse.lsp4j.jsonrpc.messages.Either<org.eclipse.lsp4j.Command, org.eclipse.lsp4j.CodeAction>) {
        val editor = lspEditor ?: return
        val action = if (either.isRight) either.right else null
        val command = if (either.isLeft) either.left else action?.command
        val edit = action?.edit
        if (edit != null) {
            val params = org.eclipse.lsp4j.ApplyWorkspaceEditParams().apply {
                label = action.title
                this.edit = edit
            }
            // 文档修改（Content.replace）必须在主线程，否则光标动画崩溃
            editor.coroutineScope.launch(Dispatchers.Main) {
                editor.eventManager.emit("workspace/applyEdit", params)
            }
        } else if (command != null) {
            editor.coroutineScope.launch(Dispatchers.Main) {
                editor.eventManager.emit("workspace/executeCommand") {
                    put("command", command.command)
                    put("args", command.arguments ?: emptyList<Any>())
                }
            }
        }
        window.dismiss()
    }

    override fun measureContent(maxWidth: Int, maxHeight: Int): Pair<Int, Int> {
        root.measure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        )
        val messageHeight = root.measuredHeight.coerceAtMost(maxHeight)
        val dialogWidth = root.measuredWidth.coerceAtMost(maxWidth)
        return dialogWidth to messageHeight
    }

    override fun isPointerOverPopup(): Boolean = pointerOverPopup

    override fun isMenuShowing(): Boolean = false

    override fun onWindowDismissed() {
        pointerOverPopup = false
    }

    fun setCornerRadiusDp(radius: Float) {
        cornerRadiusDp = radius
        if (::window.isInitialized) {
            backgroundDrawable.cornerRadius = window.editor.dpUnit * cornerRadiusDp
        }
    }

    fun setBorderWidthDp(width: Float) {
        borderWidthDp = width
        if (::window.isInitialized) {
            borderWidthPx = (window.editor.dpUnit * borderWidthDp).toInt().coerceAtLeast(1)
            backgroundDrawable.setStroke(borderWidthPx, resolveBorderColor(appliedRegion))
        }
    }

    fun setSeverityBlendRatio(ratio: Float) {
        severityBlendRatio = ratio.coerceIn(0f, 1f)
        backgroundDrawable.setColor(resolveFillColor(appliedRegion))
        updateCopyButtonTint(appliedRegion)
    }

    fun setSeverityColor(severity: Short, @ColorInt color: Int) {
        severityColors[severity] = color
        backgroundDrawable.setStroke(borderWidthPx, resolveBorderColor(appliedRegion))
        backgroundDrawable.setColor(resolveFillColor(appliedRegion))
        updateCopyButtonTint(appliedRegion)
    }

    fun setSeverityColors(
        @ColorInt none: Int? = null,
        @ColorInt typo: Int? = null,
        @ColorInt warning: Int? = null,
        @ColorInt error: Int? = null
    ) {
        none?.let { severityColors[DiagnosticRegion.SEVERITY_NONE] = it }
        typo?.let { severityColors[DiagnosticRegion.SEVERITY_TYPO] = it }
        warning?.let { severityColors[DiagnosticRegion.SEVERITY_WARNING] = it }
        error?.let { severityColors[DiagnosticRegion.SEVERITY_ERROR] = it }
        backgroundDrawable.setStroke(borderWidthPx, resolveBorderColor(appliedRegion))
        backgroundDrawable.setColor(resolveFillColor(appliedRegion))
        updateCopyButtonTint(appliedRegion)
    }

    private fun resolveFillColor(region: DiagnosticRegion?): Int {
        val severityColor = resolveSeverityColor(region)
        return if (severityColor == null) {
            baseBackgroundColor
        } else {
            blendARGB(baseBackgroundColor, severityColor, severityBlendRatio)
        }
    }

    private fun resolveBorderColor(region: DiagnosticRegion?): Int {
        return resolveSeverityColor(region) ?: Color.TRANSPARENT
    }

    private fun resolveSeverityColor(region: DiagnosticRegion?): Int? {
        val severity = region?.severity ?: DiagnosticRegion.SEVERITY_NONE
        return severityColors[severity]
    }

    private fun installDefaultSeverityPalette() {
        severityColors[DiagnosticRegion.SEVERITY_NONE] = Color.parseColor("#FF94A3B8")
        severityColors[DiagnosticRegion.SEVERITY_TYPO] = Color.parseColor("#FF38BDF8")
        severityColors[DiagnosticRegion.SEVERITY_WARNING] = Color.parseColor("#FFFACC15")
        severityColors[DiagnosticRegion.SEVERITY_ERROR] = Color.parseColor("#FFFB7185")
    }

    private fun applyDetailMessageTextSize(sizePx: Float) {
        if (sizePx <= 0f) {
            return
        }
        if (!::detailMessageText.isInitialized) {
            pendingEditorTextSizePx = sizePx
            return
        }
        if (baselineEditorTextSizePx == null) {
            baselineEditorTextSizePx = sizePx
            baselineDetailTextSizePx = sizePx
        }
        val editorBaseline = baselineEditorTextSizePx ?: return
        if (editorBaseline <= 0f) {
            return
        }
        val detailBaseline = baselineDetailTextSizePx ?: detailMessageText.textSize
        val rawScale = sizePx / editorBaseline
        if (rawScale <= 0f) {
            return
        }
        val curvedScale = curvedTextScale(rawScale)
        val targetSize = detailBaseline * curvedScale
        if (abs(appliedDetailTextSizePx - targetSize) < 0.5f) {
            return
        }
        appliedDetailTextSizePx = targetSize
        detailMessageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetSize)
    }

    private fun copyDetailedMessageToClipboard() {
        val text = detailMessageText.text?.toString()?.takeIf { it.isNotBlank() } ?: return
        val context = window.editor.context
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val label = context.getString(android.R.string.copy)
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun updateCopyButtonTint(region: DiagnosticRegion?) {
        if (!::copyButton.isInitialized) {
            return
        }
        val tintColor = resolveBorderColor(region).takeUnless { it == Color.TRANSPARENT }
            ?: severityColors[DiagnosticRegion.SEVERITY_ERROR]
            ?: Color.parseColor("#FFFB7185")
        copyButton.imageTintList = ColorStateList.valueOf(tintColor)
    }
}
