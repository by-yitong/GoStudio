package com.jmwl.gostudio.runtime

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/**
 * 运行时「视频」控件：TextureView + MediaPlayer，底部带内置控制面板。
 *
 * 不用 android.widget.VideoView：它基于 SurfaceView，在部分 OEM 的 Android 16
 * ROM 上（实测 vivo V2232A / Android 16）应用窗口内的 SurfaceView 视频图层不会
 * 被合成到屏幕——音频正常播放、MediaPlayer 上报 RENDERING_START，但用户看到
 * 的只有黑屏。TextureView 走普通 View 渲染管线，合成路径在所有设备上可用。
 *
 * 对脚本暴露的行为与 VideoView 对齐：SetVideo 设置地址，Start/Pause/Stop 控制；
 * Start 在就绪前调用会挂起到 prepared 后自动开始（VideoView 同款语义）。
 *
 * 内置控制面板：点视频切换显隐，播放中 3 秒无操作自动隐藏，暂停时常驻；
 * 脚本可用 hide_controls / set_controls_enabled 关掉内置面板自己做。
 * 事件经 [on_event] 转发给脚本：video_prepared（时长）/ video_progress（位置
 * 毫秒，约 500ms 一次，仅播放中）/ video_end / video_error。
 */
class runtime_video_view(context: Context) : FrameLayout(context) {
    private val texture_view = TextureView(context)
    private var player: MediaPlayer? = null
    private var pending_uri: Uri? = null
    private var surface_ready = false
    private var want_playing = false

    private val control_panel = LinearLayout(context)
    private val play_button = ImageButton(context)
    private val seek_bar = SeekBar(context)
    private val time_text = TextView(context)
    private var controls_allowed = true
    private var scrubbing = false

    /** 宿主接线后转发给脚本，参数为事件名与数值（毫秒）。 */
    var on_event: ((event: String, number: Double) -> Unit)? = null

    private val hide_controls_cb = Runnable { hide_controls() }
    private val tick_cb = Runnable { tick() }

    init {
        setBackgroundColor(0xFF000000.toInt())
        addView(texture_view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        texture_view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                surface_ready = true
                pending_uri?.let { open(it) }
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                apply_fit_center()
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = true

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> apply_fit_center() }

        // 点视频本体 = 切换面板显隐 + 照常把 click 事件发给脚本
        texture_view.setOnClickListener {
            toggle_controls()
            performClick()
        }

        build_control_panel()
    }

    private fun build_control_panel() {
        control_panel.orientation = LinearLayout.HORIZONTAL
        control_panel.setBackgroundColor(0x88000000.toInt())
        val pad_h = dp(10)
        val pad_v = dp(6)
        control_panel.setPadding(pad_h, pad_v, pad_h, pad_v)

        play_button.apply {
            setImageResource(android.R.drawable.ic_media_play)
            background = null
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            setOnClickListener {
                if (is_playing()) pause() else start()
            }
        }
        seek_bar.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                gravity = Gravity.CENTER_VERTICAL
                leftMargin = dp(6)
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, from_user: Boolean) {}
                override fun onStartTrackingTouch(bar: SeekBar?) {
                    scrubbing = true
                    removeCallbacks(hide_controls_cb)
                }

                override fun onStopTrackingTouch(bar: SeekBar?) {
                    scrubbing = false
                    seek_to(bar?.progress ?: 0)
                    schedule_auto_hide()
                }
            })
        }
        time_text.apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            text = "00:00/00:00"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                leftMargin = dp(8)
            }
        }
        control_panel.addView(play_button)
        control_panel.addView(seek_bar)
        control_panel.addView(time_text)
        control_panel.visibility = View.GONE
        addView(
            control_panel,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
        )
    }

    fun set_video_uri(uri: Uri) {
        stop_playback()
        pending_uri = uri
        if (surface_ready) open(uri)
    }

    fun start() {
        want_playing = true
        try {
            player?.takeIf { it.isPlaying.not() }?.start()
        } catch (e: IllegalStateException) {
            // prepared 前调用 start：挂起，onPrepared 里统一开始
            Log.d(TAG, "start before prepared: ${e.message}")
        }
        update_play_icon()
        schedule_auto_hide()
    }

    fun pause() {
        want_playing = false
        try {
            player?.takeIf { it.isPlaying }?.pause()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "pause before prepared: ${e.message}")
        }
        update_play_icon()
        // 暂停时面板常驻，方便继续操作
        removeCallbacks(hide_controls_cb)
    }

    /** 停止并释放；想再播需重新 SetVideo（与 VideoView.stopPlayback 一致）。 */
    fun stop_playback() {
        want_playing = false
        player?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
            }
            try {
                it.release()
            } catch (_: Exception) {
            }
        }
        player = null
        removeCallbacks(tick_cb)
        removeCallbacks(hide_controls_cb)
        seek_bar.max = 1
        seek_bar.progress = 0
        time_text.text = "00:00/00:00"
        update_play_icon()
        hide_controls()
    }

    fun seek_to(ms: Int) {
        try {
            player?.seekTo(ms.coerceAtLeast(0))
            tick()
        } catch (e: Exception) {
            Log.d(TAG, "seek before prepared: ${e.message}")
        }
    }

    fun current_position(): Int = try { player?.currentPosition ?: 0 } catch (_: Exception) { 0 }

    fun duration(): Int = try { player?.duration ?: 0 } catch (_: Exception) { 0 }

    fun is_playing(): Boolean = try { player?.isPlaying == true } catch (_: Exception) { false }

    /** 关掉内置面板并禁用点击呼出——脚本完全自定义控制 UI 时用。 */
    fun set_controls_enabled(enabled: Boolean) {
        controls_allowed = enabled
        if (!enabled) hide_controls()
    }

    fun show_controls() {
        if (!controls_allowed) return
        control_panel.visibility = View.VISIBLE
        schedule_auto_hide()
    }

    fun hide_controls() {
        control_panel.visibility = View.GONE
        removeCallbacks(hide_controls_cb)
    }

    override fun onDetachedFromWindow() {
        stop_playback()
        super.onDetachedFromWindow()
    }

    private fun toggle_controls() {
        if (!controls_allowed) return
        if (control_panel.visibility == View.VISIBLE) hide_controls() else show_controls()
    }

    private fun schedule_auto_hide() {
        removeCallbacks(hide_controls_cb)
        if (is_playing()) postDelayed(hide_controls_cb, HIDE_DELAY_MS)
    }

    private fun update_play_icon() {
        play_button.setImageResource(
            if (is_playing()) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    /** 每 500ms 刷新面板并给脚本推进度；拖动中不回写进度条。
     *  先移除再重排，保证 seek_to 手动触发时也不会叠出第二条定时器链。 */
    private fun tick() {
        removeCallbacks(tick_cb)
        val mp = player ?: return
        val position = current_position()
        val total = duration()
        if (!scrubbing) {
            seek_bar.max = total.coerceAtLeast(1)
            seek_bar.progress = position.coerceIn(0, seek_bar.max)
            time_text.text = "${format_ms(position)}/${format_ms(total)}"
        }
        if (is_playing()) on_event?.invoke("video_progress", position.toDouble())
        postDelayed(tick_cb, TICK_MS)
    }

    private fun open(uri: Uri) {
        stop_playback()
        val mp = MediaPlayer()
        try {
            mp.setDataSource(context, uri)
            mp.setSurface(Surface(texture_view.surfaceTexture))
            mp.setOnPreparedListener { m ->
                player = m
                apply_fit_center()
                seek_bar.max = m.duration.coerceAtLeast(1)
                update_play_icon()
                on_event?.invoke("video_prepared", m.duration.toDouble())
                post(tick_cb)
                if (want_playing) {
                    m.start()
                    update_play_icon()
                }
                show_controls()
            }
            mp.setOnCompletionListener {
                want_playing = false
                update_play_icon()
                removeCallbacks(hide_controls_cb)
                control_panel.visibility = View.VISIBLE
                on_event?.invoke("video_end", 0.0)
            }
            mp.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "MediaPlayer error what=$what extra=$extra uri=$uri")
                on_event?.invoke("video_error", 0.0)
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.w(TAG, "open failed: ${e.message} uri=$uri")
            try {
                mp.release()
            } catch (_: Exception) {
            }
        }
    }

    /** 视频等比裁剪填满控件（center-crop）。
     *  TextureView 会先把画面拉伸铺满控件，setTransform 是在拉伸结果之上叠加的
     *  矩阵——所以这里只做宽高比差量的居中缩放，不能按像素比缩放（那样等于
     *  二次缩放，画面会缩小居中留边）。 */
    private fun apply_fit_center() {
        val mp = player ?: return
        val video_w = mp.videoWidth
        val video_h = mp.videoHeight
        if (video_w <= 0 || video_h <= 0 || width <= 0 || height <= 0) return
        val view_aspect = width.toFloat() / height
        val video_aspect = video_w.toFloat() / video_h
        val matrix = Matrix()
        if (view_aspect > video_aspect) {
            // 控件比视频更宽：左右撑满、上下超出裁掉
            matrix.postScale(1f, view_aspect / video_aspect, width / 2f, height / 2f)
        } else {
            // 控件比视频更高（含竖屏全屏）：横向撑满、上下超出裁掉
            matrix.postScale(video_aspect / view_aspect, 1f, width / 2f, height / 2f)
        }
        texture_view.setTransform(matrix)
    }

    private fun format_ms(ms: Int): String {
        val total_seconds = ms / 1000
        val hours = total_seconds / 3600
        val minutes = (total_seconds % 3600) / 60
        val seconds = total_seconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%02d:%02d".format(minutes, seconds)
    }

    private fun dp(value: Int): Int =
        (context.resources.displayMetrics.density * value + 0.5f).toInt()

    private companion object {
        const val TAG = "runtime_video_view"
        const val HIDE_DELAY_MS = 3000L
        const val TICK_MS = 500L
    }
}
