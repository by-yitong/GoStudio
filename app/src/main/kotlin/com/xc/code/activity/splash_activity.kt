package com.xc.code.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.xc.code.ui.dialogs.splash.manage_storage_dialog
import com.xc.code.ui.dialogs.splash.permission_denied_dialog
import com.xc.code.ui.dialogs.splash.permission_rationale_dialog
import com.xc.code.ui.screens.splash.splash_content
import com.xc.code.ui.theme.app_theme_provider
import permissions.dispatcher.*
import java.io.File

fun File.is_ubuntu_rootfs(): Boolean {
    if (!exists() || !isDirectory) return false
    val required_dirs = listOf("home", "bin", "etc", "lib", "usr", "var", "run")
    val required_files = listOf("bin/dpkg", "bin/bash", "bin/apt", "bin/env", "bin/ls")
    val has_all_dirs = required_dirs.all { dir ->
        File(this, dir).exists() && File(this, dir).isDirectory
    }
    val has_all_files = required_files.all { file ->
        File(this, file).exists() && File(this, file).isFile
    }
    return has_all_dirs && has_all_files
}

@RuntimePermissions
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
                            check_and_request_permission()
                        }
                    }
                )
            }
        }
    }

    private fun check_and_request_permission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                check_and_navigate()
            } else {
                show_manage_storage_dialog()
            }
        } else {
            if (check_permission()) {
                check_and_navigate()
            } else {
                request_permission_with_checkWithPermissionCheck()
            }
        }
    }

    private fun check_permission(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun check_and_navigate() {
        val ubuntu_path = File(filesDir, "home/xcode/ubuntu-base")
        if (ubuntu_path.is_ubuntu_rootfs()) {
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

    @NeedsPermission(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    fun request_permission_with_check() {
        if (!has_navigated && check_permission()) {
            check_and_navigate()
        }
    }

    @OnShowRationale(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    fun show_rationale(request: PermissionRequest) {
        show_rationale_dialog(
            on_confirm = { request.proceed() },
            on_deny = { finish() }
        )
    }

    @OnPermissionDenied(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    fun on_permissions_denied() {
        if (!has_navigated) {
            show_permission_denied_dialog()
        }
    }

    @OnNeverAskAgain(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    fun on_never_ask_again() {
        if (!has_navigated) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            finish()
        }
    }

    private fun show_manage_storage_dialog() {
        setContent {
            app_theme_provider {
                manage_storage_dialog(
                    on_confirm = {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    },
                    on_deny = { finish() }
                )
            }
        }
    }

    private fun show_rationale_dialog(on_confirm: () -> Unit, on_deny: () -> Unit) {
        setContent {
            app_theme_provider {
                permission_rationale_dialog(on_confirm, on_deny)
            }
        }
    }

    private fun show_permission_denied_dialog() {
        setContent {
            app_theme_provider {
                permission_denied_dialog(
                    on_retry = { request_permission_with_checkWithPermissionCheck() },
                    on_exit = { finish() }
                )
            }
        }
    }

    @Deprecated("Deprecated but needed for Android 10 and below compatibility")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onResume() {
        super.onResume()
        if (!has_navigated && is_splash_ready) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    check_and_navigate()
                }
            } else {
                if (check_permission()) {
                    check_and_navigate()
                }
            }
        }
    }
}

