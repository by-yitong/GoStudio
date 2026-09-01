package com.jmwl.gostudio.shell

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RatingBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Space
import android.widget.Switch
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import java.io.File
import java.lang.reflect.Method

/**
 * AndLua 风格布局加载器（移植自 AndroLua loadlayout.lua，MIT License，
 * Copyright (C) 2011 Michal Kottman / 2015-2016 Nirenr）。
 *
 * 语法与 AndLua 相同：元素名即控件类名，属性不带 android: 前缀，
 * id="tv" 会被注册到控件表，供 Go 侧直接操作。
 *
 *   <LinearLayout orientation="vertical" gravity="center" padding="24dp">
 *       <TextView id="tv" text="你好" textSize="22sp"/>
 *       <Button id="btn" text="点我"/>
 *   </LinearLayout>
 */
class runtime_layout_loader(private val context: Context) {

    /** 加载结果：根视图 + 按 id 索引的控件表。 */
    fun load(file: File): Result {
        val parser = Xml.newPullParser().apply { setInput(file.inputStream(), null) }
        var event = parser.eventType
        var root: View? = null
        val views = mutableMapOf<String, View>()

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    check(root == null) { "布局根节点只能有一个" }
                    root = build_view(parser, parent = null, is_root = true, views = views)
                }
            }
            event = parser.next()
        }
        val view = root ?: error("布局为空")
        return Result(view, views)
    }

    // ---- 控件注册表：元素名 -> 构造 ----
    private val view_factories: Map<String, (Context) -> View> = mapOf(
        "LinearLayout" to { LinearLayout(it) },
        "FrameLayout" to { FrameLayout(it) },
        "RelativeLayout" to { RelativeLayout(it) },
        "ScrollView" to { ScrollView(it) },
        "HorizontalScrollView" to { HorizontalScrollView(it) },
        "TextView" to { MaterialTextView(it) },
        "Button" to { MaterialButton(it) },
        "EditText" to { EditText(it) },
        "ImageView" to { ImageView(it) },
        "ImageButton" to { ImageButton(it) },
        "CheckBox" to { CheckBox(it) },
        "RadioButton" to { RadioButton(it) },
        "Switch" to { Switch(it) },
        "ToggleButton" to { android.widget.ToggleButton(it) },
        "ProgressBar" to { ProgressBar(it) },
        "SeekBar" to { SeekBar(it) },
        "RatingBar" to { RatingBar(it) },
        "Space" to { Space(it) },
        "View" to { View(it) }
    )

    private fun build_view(
        parser: XmlPullParser,
        parent: ViewGroup?,
        is_root: Boolean,
        views: MutableMap<String, View>
    ): View {
        val factory = view_factories[parser.name]
            ?: error("不支持的控件: ${parser.name}")
        val view = factory(context)

        // 收集属性
        val attrs = mutableMapOf<String, String>()
        for (index in 0 until parser.attributeCount) {
            attrs[parser.getAttributeName(index)] = parser.getAttributeValue(index)
        }

        // 创建与父容器匹配的 LayoutParams（与 AndroLua 相同：默认 wrap_content，根节点 match_parent）
        val default_size = if (is_root) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
        val params: ViewGroup.LayoutParams = when (parent) {
            is LinearLayout -> LinearLayout.LayoutParams(default_size, default_size)
            is FrameLayout -> FrameLayout.LayoutParams(default_size, default_size)
            is RelativeLayout -> RelativeLayout.LayoutParams(default_size, default_size)
            else -> ViewGroup.LayoutParams(default_size, default_size)
        }

        // 先处理 layout_* 属性（需要写进 params）
        val layout_prefix = "layout_"
        attrs.filterKeys { it.startsWith(layout_prefix) }.forEach { (key, raw) ->
            apply_layout_attribute(params, key, raw, is_root)
        }

        // id 注册
        attrs["id"]?.takeIf { it.isNotBlank() }?.let { views[it] = view }

        // padding
        apply_padding(view, attrs)

        // background
        attrs["background"]?.let { background ->
            parse_color(background)?.let { view.setBackgroundColor(it) }
                ?: view.setBackgroundResource(android_resource(background))
        }

        // 通用属性：优先显式处理常用项，其余走反射 setXxx（与 AndroLua 相同策略）
        apply_common_attributes(view, attrs)

        view.layoutParams = params
        parent?.addView(view)

        // 递归子节点
        var event = parser.next()
        while (event != XmlPullParser.END_TAG) {
            if (event == XmlPullParser.START_TAG) {
                check(view is ViewGroup) { "${parser.name} 不能包含子控件" }
                build_view(parser, view, is_root = false, views = views)
            }
            event = parser.next()
        }
        return view
    }

    // ---- layout_* 属性 ----
    private fun apply_layout_attribute(params: ViewGroup.LayoutParams, key: String, raw: String, is_root: Boolean) {
        when (key) {
            "layout_width" -> params.width = dimension_or_size(raw, default = ViewGroup.LayoutParams.WRAP_CONTENT, is_root = is_root)
            "layout_height" -> params.height = dimension_or_size(raw, default = ViewGroup.LayoutParams.WRAP_CONTENT, is_root = is_root)
            "layout_weight" -> (params as? LinearLayout.LayoutParams)?.weight = raw.toFloatOrNull() ?: 1f
            "layout_gravity" -> (params as? LinearLayout.LayoutParams)?.gravity = gravity(raw)
            "layout_margin" -> set_margins(params, raw, raw, raw, raw)
            "layout_marginLeft", "layout_marginStart" -> set_margins(params, left = raw)
            "layout_marginTop" -> set_margins(params, top = raw)
            "layout_marginRight", "layout_marginEnd" -> set_margins(params, right = raw)
            "layout_marginBottom" -> set_margins(params, bottom = raw)
        }
    }

    private fun dimension_or_size(raw: String, default: Int, is_root: Boolean): Int {
        val value = keyword_int(raw)
        if (value != null) return value
        val px = dimension(raw)
        return if (px != null) px else default
    }

    private fun set_margins(
        params: ViewGroup.LayoutParams,
        all: String? = null, left: String? = null, top: String? = null,
        right: String? = null, bottom: String? = null
    ) {
        if (params !is android.view.ViewGroup.MarginLayoutParams) return
        params.setMargins(
            dimension(left ?: all) ?: params.leftMargin,
            dimension(top ?: all) ?: params.topMargin,
            dimension(right ?: all) ?: params.rightMargin,
            dimension(bottom ?: all) ?: params.bottomMargin
        )
    }

    // ---- padding ----
    private fun apply_padding(view: View, attrs: Map<String, String>) {
        val all = attrs["padding"]?.let { dimension(it) }
        val left = attrs["paddingLeft"]?.let { dimension(it) } ?: all ?: view.paddingLeft
        val top = attrs["paddingTop"]?.let { dimension(it) } ?: all ?: view.paddingTop
        val right = attrs["paddingRight"]?.let { dimension(it) } ?: all ?: view.paddingRight
        val bottom = attrs["paddingBottom"]?.let { dimension(it) } ?: all ?: view.paddingBottom
        if (all != null || attrs.keys.any { it.startsWith("padding") }) {
            view.setPadding(left, top, right, bottom)
        }
    }

    // ---- 常用属性 ----
    private fun apply_common_attributes(view: View, attrs: Map<String, String>) {
        attrs.forEach { (key, raw) ->
            when (key) {
                "id", "background" -> {}
                "padding", "paddingLeft", "paddingTop", "paddingRight", "paddingBottom" -> {}
                "text", "hint", "textSize", "textColor", "textColorHint", "gravity",
                "orientation", "visibility", "enabled", "singleLine", "lines", "maxLines",
                "inputType", "scaleType", "src", "ellipsize", "textStyle", "layout_weight" -> {
                    apply_known_attribute(view, key, raw)
                }
                else -> {
                    if (!key.startsWith("layout_")) apply_reflection_attribute(view, key, raw)
                }
            }
        }
    }

    private fun apply_known_attribute(view: View, key: String, raw: String) {
        when (key) {
            "text" -> (view as? TextView)?.text = raw
            "hint" -> (view as? TextView)?.hint = raw
            "textSize" -> {
                val text_view = view as? TextView
                val px = dimension(raw)
                if (text_view != null && px != null) {
                    // dimension() 已换算为 px，必须按 COMPLEX_UNIT_PX 设置，避免二次按 sp 换算
                    text_view.setTextSize(TypedValue.COMPLEX_UNIT_PX, px.toFloat())
                } else if (text_view != null) {
                    raw.toFloatOrNull()?.let { text_view.textSize = it }
                }
            }
            "textColor" -> parse_color(raw)?.let { (view as? TextView)?.setTextColor(it) }
            "textColorHint" -> parse_color(raw)?.let { (view as? TextView)?.setHintTextColor(it) }
            "gravity" -> {
                (view as? TextView)?.gravity = gravity(raw)
                (view as? LinearLayout)?.gravity = gravity(raw)
            }
            "orientation" -> (view as? LinearLayout)?.orientation =
                if (raw == "horizontal") LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            "visibility" -> view.visibility = when (raw) {
                "visible" -> View.VISIBLE; "invisible" -> View.INVISIBLE; "gone" -> View.GONE
                else -> View.VISIBLE
            }
            "enabled" -> view.isEnabled = raw == "true"
            "singleLine" -> if (raw == "true") (view as? TextView)?.setSingleLine()
            "lines", "maxLines" -> raw.toIntOrNull()?.let { count ->
                val text_view = view as? TextView ?: return@let
                if (key == "lines") text_view.setLines(count) else text_view.maxLines = count
            }
            else -> apply_reflection_attribute(view, key, raw)
        }
    }

    /** AndroLua 的兜底策略：属性名首字母大写后调用 setXxx。 */
    private fun apply_reflection_attribute(view: View, key: String, raw: String) {
        val method_name = "set" + key.replaceFirstChar { it.uppercase() }
        val candidates = view.javaClass.methods.filter { it.name == method_name && it.parameterCount == 1 }
        for (method: Method in candidates) {
            val value = coerce(method.parameterTypes[0], raw) ?: continue
            runCatching { method.invoke(view, value); return }
        }
    }

    private fun coerce(target: Class<*>, raw: String): Any? {
        return when {
            target == java.lang.CharSequence::class.java || target == String::class.java ||
                target == java.lang.String::class.java || target == CharSequence::class.java -> raw
            target == Int::class.java || target == java.lang.Integer::class.java ->
                keyword_int(raw) ?: dimension(raw) ?: parse_color(raw) ?: raw.toIntOrNull()
            target == Float::class.java || target == java.lang.Float::class.java ->
                dimension(raw)?.toFloat() ?: raw.toFloatOrNull()
            target == Boolean::class.java || target == java.lang.Boolean::class.java ->
                raw == "true"
            else -> null
        }
    }

    // ---- 值解析（对应 AndroLua 的 checkValue / toint 表）----
    private val keyword_table: Map<String, Int> = mapOf(
        "wrap_content" to -2, "wrap" to -2,
        "match_parent" to -1, "match" to -1, "fill_parent" to -1, "fill" to -1,
        "vertical" to 1, "horizontal" to 0,
        // gravity
        "center" to Gravity.CENTER, "center_horizontal" to Gravity.CENTER_HORIZONTAL,
        "center_vertical" to Gravity.CENTER_VERTICAL, "left" to Gravity.LEFT,
        "right" to Gravity.RIGHT, "top" to Gravity.TOP, "bottom" to Gravity.BOTTOM,
        "start" to Gravity.START, "end" to Gravity.END,
        "fill_horizontal" to Gravity.FILL_HORIZONTAL, "fill_vertical" to Gravity.FILL_VERTICAL,
        // visibility
        "visible" to 0, "invisible" to 4, "gone" to 8,
        "true" to 1, "false" to 0
    )

    private fun keyword_int(raw: String): Int? {
        var result = 0
        var matched = false
        raw.split("|").forEach { part ->
            val value = keyword_table[part.trim()] ?: return@forEach
            result = result or value
            matched = true
        }
        return if (matched) result else null
    }

    private fun gravity(raw: String): Int = keyword_int(raw) ?: Gravity.NO_GRAVITY

    /** "16dp" / "18sp" / "10px" -> px（AndroLua: TypedValue.applyDimension）。 */
    private fun dimension(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val match = Regex("^(-?[\\d.]+)(px|dp|sp|pt|in|mm)$").find(raw.trim()) ?: return null
        val number = match.groupValues[1].toFloatOrNull() ?: return null
        val unit = when (match.groupValues[2]) {
            "px" -> TypedValue.COMPLEX_UNIT_PX
            "dp" -> TypedValue.COMPLEX_UNIT_DIP
            "sp" -> TypedValue.COMPLEX_UNIT_SP
            "pt" -> TypedValue.COMPLEX_UNIT_PT
            "in" -> TypedValue.COMPLEX_UNIT_IN
            "mm" -> TypedValue.COMPLEX_UNIT_MM
            else -> return null
        }
        return TypedValue.applyDimension(unit, number, context.resources.displayMetrics).toInt()
    }

    /** "#RGB" / "#RRGGBB" / "#AARRGGBB"。 */
    private fun parse_color(raw: String): Int? {
        if (!raw.startsWith("#")) return null
        return runCatching { Color.parseColor(raw) }.getOrNull()
    }

    /** android.R.attr.xxx 之类的资源引用，先不支持资源引用，返回 0。 */
    private fun android_resource(raw: String): Int = 0

    data class Result(val root: View, val views: Map<String, View>)
}
