package io.github.rosemoe.sora.lsp.editor.hover

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import io.github.rosemoe.sora.lsp.R
import io.github.rosemoe.sora.lsp.editor.curvedTextScale
import io.github.rosemoe.sora.lsp.editor.text.SimpleMarkdownRenderer
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.Job
import org.eclipse.lsp4j.Hover

class DefaultHoverLayout : HoverLayout {
    private lateinit var window: HoverWindow
    private lateinit var root: View
    private lateinit var container: ScrollView
    private lateinit var hoverTextView: TextView
    private lateinit var loadingContainer: View
    private lateinit var loadingProgress: android.widget.ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var loadingClose: TextView
    private var textColor: Int = 0
    private var highlightColor: Int = 0
    private var codeTypeface: Typeface = Typeface.MONOSPACE
    private var baselineEditorTextSize: Float? = null
    private var baselineHoverTextSize: Float? = null
    private var latestEditorTextSize: Float? = null
    private var asyncRenderJob: Job? = null

    override fun attach(window: HoverWindow) {
        this.window = window
    }

    override fun createView(inflater: LayoutInflater): View {
        root = inflater.inflate(R.layout.hover_tooltip_window, null, false)
        container = root.findViewById(R.id.hover_scroll_container)
        hoverTextView = root.findViewById(R.id.hover_text)
        loadingContainer = root.findViewById(R.id.hover_loading_container)
        loadingProgress = root.findViewById(R.id.hover_loading_progress)
        loadingText = root.findViewById(R.id.hover_loading_text)
        loadingClose = root.findViewById(R.id.hover_loading_close)
        loadingClose.setOnClickListener { window.dismiss() }
        hoverTextView.movementMethod = LinkMovementMethod()
        baselineHoverTextSize = hoverTextView.textSize
        latestEditorTextSize?.let { applyEditorScale(it) }
        return root
    }

    override fun applyColorScheme(colorScheme: EditorColorScheme, typeface: Typeface) {
        val editor = window.editor
        textColor = colorScheme.getColor(EditorColorScheme.HOVER_TEXT_NORMAL)
        highlightColor = colorScheme.getColor(EditorColorScheme.HOVER_TEXT_HIGHLIGHTED)
        codeTypeface = typeface
        hoverTextView.setTextColor(textColor)
        loadingText.setTextColor(textColor)
        loadingClose.setTextColor(highlightColor)
        loadingClose.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor((highlightColor and 0x00FFFFFF) or 0x22000000)
            setStroke(
                (editor.dpUnit * 0.75f).coerceAtLeast(1f).toInt(),
                (highlightColor and 0x00FFFFFF) or 0x38000000
            )
        }
        loadingProgress.indeterminateTintList = ColorStateList.valueOf(highlightColor)

        val drawable = GradientDrawable().apply {
            cornerRadius = editor.dpUnit * 8
            setColor(colorScheme.getColor(EditorColorScheme.HOVER_BACKGROUND))
            val strokeWidth = editor.dpUnit.toInt().coerceAtLeast(1)
            setStroke(strokeWidth, colorScheme.getColor(EditorColorScheme.HOVER_BORDER))
        }
        root.background = drawable
    }

    override fun renderLoading() {
        asyncRenderJob?.cancel()
        asyncRenderJob = null
        hoverTextView.text = ""
        loadingContainer.visibility = View.VISIBLE
        container.visibility = View.GONE
    }

    override fun renderHover(hover: Hover) {
        loadingContainer.visibility = View.GONE
        container.visibility = View.VISIBLE
        val hoverText = buildHoverText(hover)
        hoverTextView.text = SimpleMarkdownRenderer.render(
            markdown = hoverText,
            boldColor = highlightColor,
            inlineCodeColor = highlightColor,
            codeTypeface = codeTypeface,
            linkColor = highlightColor
        )
        container.post { container.smoothScrollTo(0, 0) }
        asyncRenderJob?.cancel()
        asyncRenderJob = window.launchRender {
            hoverTextView.text = SimpleMarkdownRenderer.renderAsync(
                markdown = hoverText,
                boldColor = highlightColor,
                inlineCodeColor = highlightColor,
                codeTypeface = codeTypeface,
                linkColor = highlightColor
            )
        }.also { job ->
            job.invokeOnCompletion {
                if (asyncRenderJob === job) {
                    asyncRenderJob = null
                }
            }
        }
    }

    override fun onTextSizeChanged(oldSize: Float, newSize: Float) {
        if (!::hoverTextView.isInitialized) {
            return
        }
        if (newSize <= 0f) {
            return
        }
        if (baselineEditorTextSize == null) {
            if (oldSize <= 0f) {
                return
            }
            baselineEditorTextSize = oldSize
            baselineHoverTextSize = baselineHoverTextSize ?: hoverTextView.textSize
        }
        latestEditorTextSize = newSize
        applyEditorScale(newSize)
    }

    private fun applyEditorScale(targetEditorSize: Float) {
        val editorBaseline = baselineEditorTextSize ?: return
        val textBaseline = baselineHoverTextSize ?: hoverTextView.textSize
        val scale = targetEditorSize / editorBaseline
        if (scale <= 0f) {
            return
        }
        val curvedScale = curvedTextScale(scale)
        hoverTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textBaseline * curvedScale)
    }


    private fun buildHoverText(hover: Hover): String {
        val hoverContents = hover.contents ?: return ""
        return if (hoverContents.isLeft) {
            val items = hoverContents.left.orEmpty()
            items.joinToString("\n\n") { either -> formatMarkedStringEither(either) ?: "" }
        } else {
            val markup = hoverContents.right
            formatMarkupContent(markup) ?: ""
        }
    }

}
