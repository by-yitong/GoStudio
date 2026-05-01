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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.termux.app.gostudio.GoStudioViewModel
import com.termux.app.gostudio.ai.AiAssistantViewModel
import com.termux.app.gostudio.ui.AiAssistantPanel
import com.termux.app.gostudio.ui.CodeEditorScreen
import com.termux.app.gostudio.ui.theme.GoStudioTheme

/**
 * GoStudio 主界面 - Go 代码编辑器
 */
class GoEditorActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 通知权限申请结果，不需要特殊处理 */ }

    private lateinit var goStudioViewModel: GoStudioViewModel
    private lateinit var aiAssistantViewModel: AiAssistantViewModel

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
                goStudioViewModel = viewModel()
                aiAssistantViewModel = viewModel()
                val darkMode by goStudioViewModel.darkMode.collectAsState()
                GoStudioTheme(darkTheme = darkMode) {
                    CodeEditorScreen(
                        viewModel = goStudioViewModel,
                        aiViewModel = aiAssistantViewModel
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        val screen = goStudioViewModel.currentScreen.value
        if (screen == GoStudioViewModel.Screen.EDITOR) {
            // 编辑页：优先关闭抽屉
            if (goStudioViewModel.closeOneDrawer()) return
            // 无抽屉：双击返回主页
            if (goStudioViewModel.shouldExitToHome()) {
                goStudioViewModel.navigateToHome()
            } else {
                android.widget.Toast.makeText(this, "再按一次返回主页", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onBackPressed()
        }
    }
}
