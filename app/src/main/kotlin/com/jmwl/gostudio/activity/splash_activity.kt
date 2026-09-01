package com.jmwl.gostudio.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jmwl.gostudio.ui.screens.splash.splash_content
import com.jmwl.gostudio.ui.theme.app_theme_provider
import java.io.File

fun File.is_alpine_rootfs(): Boolean {
    if (!exists() || !isDirectory) return false
    val required_dirs = listOf("bin", "etc", "lib", "usr", "var", "tmp")
    // 注意 env 在 usr/bin/env（busybox applet 布局），bin/ 下没有 env
    val required_files = listOf("bin/busybox", "bin/sh", "usr/bin/env", "etc/apk")
    val has_all_dirs = required_dirs.all { dir ->
        File(this, dir).exists() && File(this, dir).isDirectory
    }
    // bin/sh、usr/bin/env 是指向 /bin/busybox 的【绝对】符号链接——在宿主文件系统上
    // 是断链（宿主没有 /bin/busybox），File.exists() 会跟随链接返回 false，导致有效
    // rootfs 被误判为未安装、触发无限重装。这里用 NOFOLLOW 只判定链接本身存在，
    // 链接目标的真实性由 bin/busybox 本体（普通文件）保证。
    val has_all_files = required_files.all { file ->
        java.nio.file.Files.exists(File(this, file).toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)
    }
    return has_all_dirs && has_all_files
}

class splash_activity : ComponentActivity() {

    private var has_navigated = false
    private var is_splash_ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            app_theme_provider {
                splash_content(
                    on_ready = {
                        if (!has_navigated) {
                            is_splash_ready = true
                            check_and_navigate()
                        }
                    }
                )
            }
        }
    }

    private fun check_and_navigate() {
        val alpine_path = File(filesDir, "home/gostudio/alpine-rootfs")
        // rootfs 完整且配置完成（基础包+工具链）才进主界面；rootfs 在但配置没跑完
        // （安装中途进程被杀）时回安装页续装，那里的守卫会跳过已完成的下载/解压。
        val configured = File(alpine_path, ".gostudio_installed").exists()
        if (alpine_path.is_alpine_rootfs() && configured) {
            navigate_to_main()
        } else {
            navigate_to_install()
        }
    }

    private fun navigate_to_main() {
        if (has_navigated) return
        has_navigated = true
        startActivity(Intent(this, main_activity::class.java))
        finish()
    }

    private fun navigate_to_install() {
        if (has_navigated) return
        has_navigated = true
        startActivity(Intent(this, install_activity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (!has_navigated && is_splash_ready) {
            check_and_navigate()
        }
    }
}
