package com.termux.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.termux.app.gostudio.ui.CodeEditorScreen
import com.termux.app.gostudio.ui.theme.GoStudioTheme

/**
 * GoStudio 主界面 - Go 代码编辑器
 */
class GoEditorActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 通知权限申请结果，不需要特殊处理 */ }

    private lateinit var viewModel: com.termux.app.gostudio.GoStudioViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 申请通知权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 先确保 Termux bootstrap 完成，再进入编辑器
        TermuxInstaller.setupBootstrapIfNeeded(this) {
            setContent {
                GoStudioTheme {
                    viewModel = viewModel()
                    CodeEditorScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onBackPressed() {
        when (viewModel.currentScreen.value) {
            com.termux.app.gostudio.GoStudioViewModel.Screen.EDITOR -> viewModel.navigateToHome()
            else -> super.onBackPressed()
        }
    }
}
