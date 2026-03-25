package com.termux.app.gostudio.ui
import android.util.Log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.material3.Snackbar
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termux.app.gostudio.GoStudioViewModel
import com.termux.app.gostudio.editor.GoExamples
import com.termux.app.gostudio.editor.GoSyntaxColors
import com.termux.app.gostudio.editor.highlightGo
import com.termux.app.gostudio.lsp.CompletionItem
import com.termux.app.gostudio.lsp.Diagnostic
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

/**
 * 主编辑器界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    viewModel: GoStudioViewModel,
    modifier: Modifier = Modifier
) {
    val termuxAvailable by viewModel.termuxAvailable.collectAsState()
    val goInstalled by viewModel.goInstalled.collectAsState()
    val goplsInstalled by viewModel.goplsInstalled.collectAsState()
    val outputLines by viewModel.outputLines.collectAsState()
    val outputExpanded by viewModel.outputExpanded.collectAsState()

    if (!termuxAvailable) {
        EnvSetupScreen(
            onInitialize = { viewModel.initializeEnvironment() },
            installing = viewModel.installing.collectAsState().value,
            installProgress = viewModel.installProgress.collectAsState().value,
            onRefresh = { viewModel.refreshEnvironment() },
            onShowLog = { viewModel.captureLogcat() },
            outputLines = viewModel.outputLines.collectAsState().value,
            onClearLog = { viewModel.clearLog() }
        )
        return
    }

    if (!goInstalled || !goplsInstalled) {
        GoSetupScreen(
            onInstallGo = { viewModel.installGo() },
            installing = viewModel.installing.collectAsState().value,
            installProgress = viewModel.installProgress.collectAsState().value,
            onRefresh = { viewModel.refreshEnvironment() },
            onSkip = { viewModel.skipGoCheck() },
            onShowLog = { viewModel.captureLogcat() },
            outputLines = viewModel.outputLines.collectAsState().value,
            onClearLog = { viewModel.clearLog() },
            goInstalled = viewModel.goInstalled.collectAsState().value,
            goplsInstalled = viewModel.goplsInstalled.collectAsState().value,
            installingGopls = viewModel.installingGopls.collectAsState().value,
            onInstallGopls = { viewModel.installGopls() }
        )
        return
    }

    val drawerState = rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val projectName by viewModel.projectName.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    // 主页：项目列表
    if (currentScreen == GoStudioViewModel.Screen.HOME) {
        ProjectHomeScreen(
            viewModel = viewModel,
            onOpenProject = { viewModel.openProject(it) },
            onCreateProject = { viewModel.showCreateProjectDialog() },
            onDeleteProject = { viewModel.deleteProject(it) },
            onRenameProject = { oldName, newName -> viewModel.renameProject(oldName, newName) }
        )
        // 监听创建项目对话框
        val showCreateDialog by viewModel.showCreateProjectDialog.collectAsState()
        if (showCreateDialog) {
            CreateProjectDialog(
                onDismiss = { viewModel.dismissCreateProjectDialog() },
                onCreate = { name, template ->
                    viewModel.createProject(name, template)
                    viewModel.dismissCreateProjectDialog()
                },
                getTemplates = { viewModel.getProjectTemplates() }
            )
        }
        return
    }

    val diagDrawerState = rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    var showTutorialDrawer by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FileDrawer(
                viewModel = viewModel,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
    // 诊断状态
    val diagnostics by viewModel.diagnostics.collectAsState()
    val currentFileName by viewModel.currentFileName.collectAsState()
    val currentDiags = diagnostics[currentFileName] ?: emptyList()
    val hasErrors = currentDiags.any { it.severityLevel == Diagnostic.Severity.ERROR }
    val hasWarnings = currentDiags.any { it.severityLevel == Diagnostic.Severity.WARNING }
    val hasDiags = currentDiags.isNotEmpty()

    ModalNavigationDrawer(
        drawerState = diagDrawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1E1E1E),
                drawerContentColor = Color(0xFFE0E0E0),
                modifier = Modifier.width(280.dp)
            ) {
                DiagnosticsDrawerContent(diagnostics = currentDiags, onClose = { scope.launch { diagDrawerState.close() } })
            }
        },
        gesturesEnabled = false
    ) {
    // 同步抽屉开关状态给 ViewModel
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) viewModel.onDrawerOpened() else viewModel.onDrawerClosed()
    }
    LaunchedEffect(diagDrawerState.isOpen) {
        if (diagDrawerState.isOpen) viewModel.onDrawerOpened() else viewModel.onDrawerClosed()
    }
    LaunchedEffect(showTutorialDrawer) {
        if (showTutorialDrawer) viewModel.onDrawerOpened() else viewModel.onDrawerClosed()
    }

    // 监听 Activity 发来的关闭抽屉请求（按优先级关闭）
    val closeReq by viewModel.closeDrawerRequest.collectAsState()
    LaunchedEffect(closeReq) {
        if (closeReq > 0) {
            viewModel.consumeCloseRequest()
            if (showTutorialDrawer) {
                showTutorialDrawer = false
            } else if (diagDrawerState.isOpen) {
                diagDrawerState.close()
            } else if (drawerState.isOpen) {
                drawerState.close()
            }
        }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                viewModel = viewModel,
                onToggleDrawer = { scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() } },
                hasDiags = hasDiags,
                hasErrors = hasErrors,
                hasWarnings = hasWarnings,
                onToggleDiagDrawer = { scope.launch { if (diagDrawerState.isClosed) diagDrawerState.open() else diagDrawerState.close() } },
                onToggleTutorialDrawer = { showTutorialDrawer = true }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // 查找替换栏（在编辑器上方）
            val showFindReplace by viewModel.showFindReplaceDialog.collectAsState()
            if (showFindReplace) {
                FindReplaceBarInScaffold(
                    viewModel = viewModel
                )
            }

            // 跳转行对话框
            val showGotoLine by viewModel.showGotoLineDialog.collectAsState()
            if (showGotoLine) {
                GotoLineDialog(
                    totalLines = (viewModel.getCurrentFileCode().count { it == '\n' } + 1),
                    onGoto = { viewModel.gotoLine(it) },
                    onDismiss = { viewModel.dismissGotoLine() }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GoCodeEditor(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )

                // 选中函数名提示卡片（悬浮在编辑器底部）
                val selectedWord by viewModel.selectedWord.collectAsState()
                if (selectedWord.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.BottomCenter)
                            .padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
                    ) {
                        Button(
                            onClick = {
                                val (start, end) = viewModel.lastSelection.value
                                Log.d("GoStudio", "跳转定义按钮 clicked, selection=$start..$end")
                                viewModel.gotoDefinitionDirect(start, end)
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2D2D30),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = selectedWord,
                                color = Color(0xFF4FC1FF),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "→ 跳转定义",
                                color = Color(0xFFAAAAAA),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // 日志面板切换按钮
            Surface(
                color = Color(0xFF1E1E1E),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleOutputPanel() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "输出日志",
                        fontSize = 11.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 清除按钮
                        Surface(
                            color = Color(0xFF333333),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "清除日志",
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(2.dp)
                                    .clickable { viewModel.clearLog() }
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF333333),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ) {
                            Icon(
                                if (outputExpanded) Icons.Default.KeyboardArrowDown
                                else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (outputExpanded) "关闭日志" else "打开日志",
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier.size(16.dp).padding(2.dp)
                            )
                        }
                    }
                }
            }

            if (outputExpanded) {
                val outputMaximized by viewModel.outputMaximized.collectAsState()
                HorizontalDivider(thickness = 1.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (outputMaximized) Modifier.weight(1f) else Modifier.height(100.dp))
                ) {
                    OutputPanel(
                        outputLines = outputLines,
                        modifier = Modifier.fillMaxSize()
                    )
                    // 右上角扩展/收起按钮
                    Surface(
                        color = Color(0xFF333333),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.TopEnd)
                            .padding(4.dp)
                            .clickable { viewModel.toggleOutputMaximize() }
                    ) {
                        Icon(
                            if (outputMaximized) Icons.Default.KeyboardArrowDown
                            else Icons.Default.OpenInFull,
                            contentDescription = if (outputMaximized) "收起" else "展开",
                            tint = Color(0xFFAAAAAA),
                            modifier = Modifier.size(16.dp).padding(2.dp)
                        )
                    }
                }
            }
        }
    } // Scaffold
    } // 诊断抽屉 ModalNavigationDrawer
    } // 文件抽屉 ModalNavigationDrawer

    // 右侧教程抽屉（覆盖层，放在最外层避免被 ModalNavigationDrawer 遮挡）
    if (showTutorialDrawer) {
        GoTutorialDrawer(
            visible = showTutorialDrawer,
            viewModel = viewModel,
            onDismiss = { showTutorialDrawer = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvSetupScreen(
    onInitialize: () -> Unit,
    installing: Boolean,
    installProgress: String,
    onRefresh: () -> Unit,
    onShowLog: () -> Unit,
    outputLines: List<GoStudioViewModel.OutputLine>,
    onClearLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onShowLog) { Text("查看日志") }
            TextButton(onClick = onClearLog) { Text("清除日志") }
        }

        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "初始化运行环境",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "首次使用需下载运行环境（约 50MB），无需安装 Termux。",
            style = MaterialTheme.typography.bodyLarge
        )

        Surface(
            color = Color(0xFF1E1E1E),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val scrollState = androidx.compose.foundation.rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(12.dp)
            ) {
                val displayLines = if (installing && installProgress.isNotEmpty()) {
                    outputLines + listOf(GoStudioViewModel.OutputLine(installProgress, GoStudioViewModel.OutputLine.Type.INFO))
                } else {
                    outputLines
                }
                for (line in displayLines.takeLast(100)) {
                    Text(
                        text = line.text,
                        color = when (line.type) {
                            GoStudioViewModel.OutputLine.Type.ERROR -> Color(0xFFFF6B6B)
                            GoStudioViewModel.OutputLine.Type.INFO -> Color(0xFF888888)
                            else -> Color(0xFFCCCCCC)
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (!installing) {
            ExtendedFloatingActionButton(
                onClick = onInitialize,
                icon = { Icon(Icons.Default.Download, "下载") },
                text = { Text("开始初始化") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        } else {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(installProgress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRefresh) { Text("刷新状态") }
    }
}

/**
 * Go 安装引导页
 * 自动安装 Go + gopls，完成后进入编辑器
 */
@Composable
fun GoSetupScreen(
    onInstallGo: () -> Unit,
    installing: Boolean,
    installProgress: String,
    onRefresh: () -> Unit,
    onSkip: () -> Unit = {},
    onShowLog: () -> Unit,
    outputLines: List<GoStudioViewModel.OutputLine>,
    onClearLog: () -> Unit,
    goInstalled: Boolean,
    goplsInstalled: Boolean,
    installingGopls: Boolean,
    onInstallGopls: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 安装阶段：1=Go, 2=gopls, 3=完成
    val phase = when {
        !goInstalled -> 1
        !goplsInstalled && !installing -> 2
        goplsInstalled -> 3
        installing -> 1
        installingGopls -> 2
        else -> 1
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.5f))

        // Logo
        Text(
            text = "🦞",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "GoStudio",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "手机上的 Go 开发环境",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF888888)
        )

        Spacer(modifier = Modifier.weight(0.3f))

        // 安装步骤卡片
        Surface(
            color = Color(0xFF16213E),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Step 1: Go
                StepRow(
                    step = 1,
                    title = "安装 Go 语言",
                    status = when {
                        goInstalled -> StepStatus.DONE
                        installing -> StepStatus.RUNNING
                        else -> StepStatus.PENDING
                    }
                )

                HorizontalDivider(
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Step 2: gopls
                StepRow(
                    step = 2,
                    title = "安装 gopls (代码补全)",
                    status = when {
                        goplsInstalled -> StepStatus.DONE
                        installingGopls -> StepStatus.RUNNING
                        goInstalled && !installing -> StepStatus.PENDING
                        else -> StepStatus.WAITING
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 日志区域（紧凑）
        if (outputLines.isNotEmpty() || installing || installingGopls) {
            Surface(
                color = Color(0xFF0F3460),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val scrollState = androidx.compose.foundation.rememberScrollState()
                LaunchedEffect(outputLines.size) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(12.dp)
                ) {
                    for (line in outputLines.takeLast(30)) {
                        Text(
                            text = line.text,
                            color = when (line.type) {
                                GoStudioViewModel.OutputLine.Type.ERROR -> Color(0xFFFF6B6B)
                                GoStudioViewModel.OutputLine.Type.INFO -> Color(0xFF888888)
                                else -> Color(0xFFCCCCCC)
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(0.3f))

        // 底部操作
        when {
            phase == 3 -> {
                // 全部完成，自动跳转（由 ViewModel 处理）
                Text(
                    text = "✅ 安装完成，正在进入...",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            phase == 2 -> {
                ExtendedFloatingActionButton(
                    onClick = if (!installingGopls) onInstallGopls else {{}},
                    icon = {
                        if (installingGopls) CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        ) else Icon(Icons.Default.AutoFixHigh, "gopls", tint = Color.White)
                    },
                    text = { Text(if (installingGopls) "正在安装 gopls..." else "安装代码补全") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
            else -> {
                ExtendedFloatingActionButton(
                    onClick = if (!installing) onInstallGo else {{}},
                    icon = {
                        if (installing) CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        ) else Icon(Icons.Default.Download, "Go", tint = Color.White)
                    },
                    text = { Text(if (installing) "正在安装 Go..." else "开始安装") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRefresh) { Text("刷新", color = Color(0xFF888888)) }
            TextButton(onClick = onSkip) { Text("跳过", color = Color(0xFF888888)) }
        }
    }
}

/**
 * 空项目状态页 - 首次打开无项目时显示
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectHomeScreen(
    viewModel: GoStudioViewModel,
    onOpenProject: (String) -> Unit,
    onCreateProject: () -> Unit,
    onDeleteProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<String?>(null) }
    var contextMenuProject by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var showTutorialDrawer by remember { mutableStateOf(false) }

    // 删除确认对话框
    showDeleteConfirm?.let { projName ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除项目") },
            text = { Text("确定要删除项目 \"$projName\" 吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProject(projName)
                    showDeleteConfirm = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }

    // 长按上下文菜单
    contextMenuProject?.let { projName ->
        AlertDialog(
            onDismissRequest = { contextMenuProject = null },
            title = { Text(projName, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.width(160.dp)) {
                    TextButton(
                        onClick = {
                            contextMenuProject = null
                            showRenameDialog = projName
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("重命名", color = Color(0xFFD4D4D4), fontSize = 14.sp)
                    }
                    TextButton(
                        onClick = {
                            contextMenuProject = null
                            showDeleteConfirm = projName
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("删除", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { contextMenuProject = null }) { Text("取消") }
            }
        )
    }

    // 重命名项目对话框
    showRenameDialog?.let { oldName ->
        var newName by remember { mutableStateOf(oldName) }
        var renameError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("重命名项目") },
            text = {
                Column {
                    Text("原名称: $oldName", fontSize = 12.sp, color = Color(0xFF888888))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { input ->
                            newName = input
                            renameError = when {
                                input.isBlank() -> "名称不能为空"
                                input != oldName && projects.contains(input) -> "项目已存在"
                                else -> null
                            }
                        },
                        label = { Text("新名称") },
                        singleLine = true,
                        isError = renameError != null && newName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    renameError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isNotBlank() && (name == oldName || !projects.contains(name))) {
                        onRenameProject(oldName, name)
                        showRenameDialog = null
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("取消") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        // 顶栏
        TopAppBar(
            title = {
                Text(
                    text = "🦞 GoStudio",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF16213E)
            ),
            actions = {
                IconButton(onClick = { viewModel.toggleDarkMode() }) {
                    Icon(
                        if (viewModel.darkMode.collectAsState().value) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "切换主题",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { showTutorialDrawer = true }) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = "Go教程",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = {
                    val intent = android.content.Intent(context, com.termux.app.TermuxActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = "终端",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        if (projects.isEmpty()) {
            // 无项目时的空状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "🦞", fontSize = 72.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "欢迎使用 GoStudio",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "在手机上编写和运行 Go 代码",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF888888)
                )
                Spacer(modifier = Modifier.height(48.dp))
                ExtendedFloatingActionButton(
                    onClick = onCreateProject,
                    icon = {
                        Icon(Icons.Default.CreateNewFolder, "创建项目", tint = Color.White)
                    },
                    text = { Text("创建项目") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            // 项目列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "我的项目",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        IconButton(onClick = onCreateProject) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "创建项目",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                itemsIndexed(projects) { _, projName ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onOpenProject(projName) },
                                onLongClick = { contextMenuProject = projName }
                            ),
                        color = Color(0xFF16213E),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color(0xFF4FC3F7),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = projName,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // 教程抽屉（覆盖层）
    if (showTutorialDrawer) {
        GoTutorialDrawer(
            visible = showTutorialDrawer,
            viewModel = viewModel,
            onDismiss = { showTutorialDrawer = false }
        )
    }
}

private enum class StepStatus { DONE, RUNNING, PENDING, WAITING }

@Composable
private fun StepRow(step: Int, title: String, status: StepStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        when (status) {
            StepStatus.DONE -> Surface(
                color = Color(0xFF4CAF50),
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            StepStatus.RUNNING -> CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
            else -> Surface(
                color = Color(0xFF333333),
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("$step", color = Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = when (status) {
                StepStatus.DONE -> Color(0xFF4CAF50)
                StepStatus.RUNNING -> Color.White
                StepStatus.WAITING -> Color(0xFF555555)
                StepStatus.PENDING -> Color(0xFFCCCCCC)
            },
            fontSize = 14.sp,
            fontWeight = if (status == StepStatus.RUNNING) FontWeight.Bold else FontWeight.Normal
        )
        if (status == StepStatus.DONE) {
            Spacer(modifier = Modifier.width(8.dp))
            Text("已完成", color = Color(0xFF4CAF50), fontSize = 12.sp)
        }
    }
}

@Composable
fun DiagnosticsPanel(
    diagnostics: List<Diagnostic>,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E1E1E),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "诊断",
                color = Color(0xFF888888),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            diagnostics.forEach { diag ->
                val (icon, color) = when (diag.severityLevel) {
                    Diagnostic.Severity.ERROR -> ("\u2715" to Color(0xFFFF6B6B))
                    Diagnostic.Severity.WARNING -> ("\u26A0" to Color(0xFFFFCC00))
                    Diagnostic.Severity.INFO -> ("\u2139" to Color(0xFF4FC1FF))
                    Diagnostic.Severity.HINT -> ("\uD83D\uDCA1" to Color(0xFF888888))
                }
                val line = diag.range.start.line + 1
                val col = diag.range.start.character + 1
                Text(
                    text = "$icon $line:$col  ${diag.message}",
                    color = color,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsDrawerContent(
    diagnostics: List<Diagnostic>,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "诊断 (${diagnostics.size})",
                color = Color(0xFFE0E0E0),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "关闭", tint = Color(0xFF888888), modifier = Modifier.size(18.dp))
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Color(0xFF333333))
        // 诊断列表
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            if (diagnostics.isEmpty()) {
                Text("✅ 没有问题", color = Color(0xFF4CAF50), fontSize = 13.sp)
            }
            diagnostics.forEach { diag ->
                val (icon, color) = when (diag.severityLevel) {
                    Diagnostic.Severity.ERROR -> ("\u2715" to Color(0xFFFF6B6B))
                    Diagnostic.Severity.WARNING -> ("\u26A0" to Color(0xFFFFCC00))
                    Diagnostic.Severity.INFO -> ("\u2139" to Color(0xFF4FC1FF))
                    Diagnostic.Severity.HINT -> ("\uD83D\uDCA1" to Color(0xFF888888))
                }
                val line = diag.range.start.line + 1
                val col = diag.range.start.character + 1
                val kindLabel = when (diag.severityLevel) {
                    Diagnostic.Severity.ERROR -> "错误"
                    Diagnostic.Severity.WARNING -> "警告"
                    Diagnostic.Severity.INFO -> "信息"
                    Diagnostic.Severity.HINT -> "提示"
                }
                Surface(
                    color = Color(0xFF2D2D30),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = icon, color = color, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = kindLabel, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "$line:$col", color = Color(0xFF888888), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = diag.message,
                            color = Color(0xFFD0D0D0),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ===== 浮动快捷工具栏 =====

/**
 * 浮动工具栏：标题栏下方紧凑图标栏
 */
/**
 * 文件列表项（支持缩进）
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FileItemRow(
    fileName: String,
    displayName: String = fileName,
    indent: Int = 0,
    isCurrent: Boolean,
    onSwitch: () -> Unit,
    onLongPress: () -> Unit
) {
    Surface(
        color = if (isCurrent) Color(0xFF094771) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onSwitch() },
                    onLongClick = { onLongPress() }
                )
                .padding(start = (16 + indent).dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Edit, contentDescription = null,
                tint = if (isCurrent) Color(0xFF4FC1FF) else Color(0xFF858585),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayName,
                fontSize = 14.sp,
                color = if (isCurrent) Color.White else Color(0xFFD4D4D4),
                fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ===== 左侧文件抽屉（简化版） =====

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FileDrawer(
    viewModel: GoStudioViewModel,
    onCloseDrawer: () -> Unit
) {
    val projectName by viewModel.projectName.collectAsState()
    val currentFileName by viewModel.currentFileName.collectAsState()
    val files by viewModel.files.collectAsState()

    var showNewFile by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var newFilePrefix by remember { mutableStateOf("") }
    var newFolderPrefix by remember { mutableStateOf("") }
    // 长按菜单相关状态
    var contextMenuFile by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showCopyDialog by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<String?>(null) }
    val isFolder = contextMenuFile != null && files.keys.any {
        it != contextMenuFile && it.startsWith("$contextMenuFile/")
    }

    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF252526),
        drawerContentColor = Color(0xFFD4D4D4),
        modifier = Modifier.width(280.dp)
    ) {
        // 当前项目标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = Color(0xFF4FC1FF),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (projectName.isNotBlank()) projectName else "未命名项目",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 新建文件夹
            IconButton(onClick = { newFolderPrefix = ""; showNewFolder = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Folder, "新建文件夹", tint = Color(0xFF4FC1FF), modifier = Modifier.size(16.dp))
            }
            // 新建文件
            IconButton(onClick = { newFilePrefix = ""; showNewFile = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, "新建文件", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
            }
            // 关闭
            IconButton(onClick = onCloseDrawer, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "关闭", tint = Color(0xFF999999), modifier = Modifier.size(16.dp))
            }
        }

        HorizontalDivider(color = Color(0xFF3C3C3C))

        // 文件列表（支持文件夹分组显示）
        // 记录哪些文件夹处于展开状态
        val expandedFolders = remember { mutableStateOf(setOf<String>()) }
        val listScrollState = androidx.compose.foundation.rememberScrollState()

        Column(modifier = Modifier.verticalScroll(listScrollState).weight(1f)) {
        // 将文件分为根目录文件和子目录文件
        val rootFiles = files.keys.filter { !it.contains("/") }
        // 构建树形结构: Map<parentPath, Set<childPath>>
        val treeMap = mutableMapOf<String, MutableSet<String>>()
        val folderSet = mutableSetOf<String>()
        for (fileName in files.keys) {
            if (fileName.contains("/")) {
                val parts = fileName.split("/")
                var path = ""
                for (i in 0 until parts.size - 1) {
                    path = if (path.isEmpty()) parts[i] else "$path/${parts[i]}"
                    folderSet.add(path)
                    val parent = if (i == 0) "" else parts.subList(0, i).joinToString("/")
                    treeMap.getOrPut(parent) { linkedSetOf() }.add(path)
                }
                val parent = parts.subList(0, parts.size - 1).joinToString("/")
                treeMap.getOrPut(parent) { linkedSetOf() }.add(fileName)
            } else {
                treeMap.getOrPut("") { linkedSetOf() }.add(fileName)
            }
        }

        // 递归渲染文件树
        @Composable
        fun FileTreeNode(parentPath: String, depth: Int) {
            val children = (treeMap[parentPath] ?: emptyList()).sortedWith(compareBy<String> { !folderSet.contains(it) }.thenBy { it })
            for (child in children) {
                if (folderSet.contains(child)) {
                    // 文件夹
                    val isExpanded = expandedFolders.value.contains(child)
                    val folderName = child.substringAfterLast("/")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    val updated = if (isExpanded) expandedFolders.value - child else expandedFolders.value + child
                                    expandedFolders.value = updated
                                },
                                onLongClick = { contextMenuFile = child }
                            )
                            .padding(horizontal = (16 + depth * 20).dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF858585),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(0xFF4FC1FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = folderName,
                            fontSize = 14.sp,
                            color = Color(0xFFD4D4D4),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (isExpanded) {
                        FileTreeNode(parentPath = child, depth = depth + 1)
                    }
                } else {
                    // 文件
                    FileItemRow(
                        fileName = child,
                        displayName = child.substringAfterLast("/"),
                        indent = (depth + 1) * 20,
                        isCurrent = child == currentFileName,
                        onSwitch = { viewModel.switchFile(child); onCloseDrawer() },
                        onLongPress = { contextMenuFile = child }
                    )
                }
            }
        }

        FileTreeNode(parentPath = "", depth = 0)

        // 底部信息
        Text(
            text = "GoStudio v1.0",
            fontSize = 11.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(16.dp)
        )
        } // end scrollable Column

    }

    // 新建文件对话框
    if (showNewFile) {
        NewFileDialog(
            existingFiles = files.keys,
            prefix = newFilePrefix,
            onDismiss = { showNewFile = false; newFilePrefix = "" },
            onCreate = { name ->
                viewModel.createFile(name)
                showNewFile = false
                newFilePrefix = ""
                onCloseDrawer()
            }
        )
    }
    // 长按上下文菜单（用 AlertDialog 替代 DropdownMenu 避免 bug）
    contextMenuFile?.let { targetFile ->
        val targetIsFolder = files.keys.any { it != targetFile && it.startsWith("$targetFile/") }
        AlertDialog(
            onDismissRequest = { contextMenuFile = null },
            title = { Text(targetFile.substringAfterLast("/"), fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.width(160.dp)) {
                    // 复制（仅文件）
                    if (!targetIsFolder) {
                        TextButton(
                            onClick = {
                                contextMenuFile = null
                                showCopyDialog = targetFile
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("复制文件", color = Color(0xFFD4D4D4), fontSize = 14.sp)
                        }
                    }
                    // 文件夹专属：新建子文件夹、新建子文件
                    if (targetIsFolder) {
                        TextButton(
                            onClick = {
                                contextMenuFile = null
                                // 设置新建文件夹的默认路径为当前文件夹下
                                newFolderPrefix = "$targetFile/"
                                showNewFolder = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF4FC1FF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("新建文件夹", color = Color(0xFFD4D4D4), fontSize = 14.sp)
                        }
                        TextButton(
                            onClick = {
                                contextMenuFile = null
                                // 设置新建文件的默认路径为当前文件夹下
                                newFilePrefix = "$targetFile/"
                                showNewFile = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("新建文件", color = Color(0xFFD4D4D4), fontSize = 14.sp)
                        }
                    }
                    // 重命名
                    TextButton(
                        onClick = {
                            contextMenuFile = null
                            showRenameDialog = targetFile
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("重命名", color = Color(0xFFD4D4D4), fontSize = 14.sp)
                    }
                    // 删除（main.go 不可删除）
                    if (targetFile != "main.go") {
                        TextButton(
                            onClick = {
                                contextMenuFile = null
                                showDeleteConfirm = targetFile
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("删除", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { contextMenuFile = null }) { Text("取消") }
            }
        )
    }

    // 删除文件确认
    showDeleteConfirm?.let { targetFile ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除文件") },
            text = { Text("确定删除「${targetFile}」？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFile(targetFile)
                    showDeleteConfirm = null
                    onCloseDrawer()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }

    // 复制文件对话框
    showCopyDialog?.let { sourceFile ->
        var newFileName by remember { mutableStateOf(sourceFile) }
        var copyError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showCopyDialog = null },
            title = { Text("复制文件") },
            text = {
                Column {
                    Text("源文件: $sourceFile", fontSize = 12.sp, color = Color(0xFF888888))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { input ->
                            newFileName = input
                            copyError = when {
                                input.isBlank() -> "文件名不能为空"
                                input == sourceFile -> "新文件名不能与源文件相同"
                                files.keys.contains(input) -> "文件已存在"
                                else -> null
                            }
                        },
                        label = { Text("新文件名") },
                        singleLine = true,
                        isError = copyError != null && newFileName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    copyError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newFileName.trim()
                    if (name.isNotBlank() && name != sourceFile && !files.keys.contains(name)) {
                        viewModel.copyFile(sourceFile, name)
                        showCopyDialog = null
                        onCloseDrawer()
                    }
                }) { Text("复制") }
            },
            dismissButton = {
                TextButton(onClick = { showCopyDialog = null }) { Text("取消") }
            }
        )
    }

    // 重命名文件对话框
    showRenameDialog?.let { oldName ->
        var newFileName by remember { mutableStateOf(oldName) }
        var renameError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("重命名") },
            text = {
                Column {
                    Text("原名称: $oldName", fontSize = 12.sp, color = Color(0xFF888888))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { input ->
                            newFileName = input
                            renameError = when {
                                input.isBlank() -> "名称不能为空"
                                input != oldName && files.keys.contains(input) -> "文件已存在"
                                else -> null
                            }
                        },
                        label = { Text("新名称") },
                        singleLine = true,
                        isError = renameError != null && newFileName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    renameError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newFileName.trim()
                    if (name.isNotBlank() && (name == oldName || !files.keys.contains(name))) {
                        viewModel.renameFile(oldName, name)
                        showRenameDialog = null
                        onCloseDrawer()
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("取消") }
            }
        )
    }

    // 新建文件夹对话框
    if (showNewFolder) {
        var folderName by remember { mutableStateOf("") }
        var folderError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showNewFolder = false; newFolderPrefix = "" },
            title = { Text("新建文件夹") },
            text = {
                Column {
                    if (newFolderPrefix.isNotEmpty()) {
                        Text("路径: $newFolderPrefix", fontSize = 12.sp, color = Color(0xFF888888))
                    }
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it; folderError = null },
                        label = { Text("文件夹名") },
                        placeholder = { Text("例如 utils") },
                        singleLine = true,
                        isError = folderError != null,
                        supportingText = folderError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = folderName.trim()
                    if (name.isBlank()) {
                        folderError = "文件夹名不能为空"
                    } else if (name.contains("/") || name.contains("\\")) {
                        folderError = "文件夹名不能包含 / 或 \\"
                    } else {
                        val fullPath = if (newFolderPrefix.isNotEmpty()) "$newFolderPrefix$name" else name
                        viewModel.createFolder(fullPath)
                        showNewFolder = false
                        newFolderPrefix = ""
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolder = false; newFolderPrefix = "" }) { Text("取消") }
            }
        )
    }
}

// ===== 代码块选择弹窗 =====

/**
 * 代码块选择弹窗，列出预置 Go 代码模板
 */
@Composable
fun SnippetDialog(
    onDismiss: () -> Unit,
    onSelect: (GoStudioViewModel.CodeSnippet) -> Unit
) {
    val snippets = GoStudioViewModel.BUILTIN_SNIPPETS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("插入代码块") },
        text = {
            LazyColumn(
                modifier = Modifier.height(320.dp)
            ) {
                itemsIndexed(snippets) { _, snippet ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onSelect(snippet) },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = snippet.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = snippet.description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "→",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
/**
 * 顶部标题栏 — 简洁版
 * 左侧: ☰ 菜单按钮 | 项目名/文件名（点击弹出合并下拉菜单）
 * 右侧: 运行/停止 | 撤销 | 重做 | 编辑菜单 | 更多菜单
 */
@Composable
fun EditorTopBar(
    viewModel: GoStudioViewModel,
    onToggleDrawer: () -> Unit,
    hasDiags: Boolean = false,
    hasErrors: Boolean = false,
    hasWarnings: Boolean = false,
    onToggleDiagDrawer: () -> Unit = {},
    onToggleTutorialDrawer: () -> Unit = {}
) {
    val currentFileName by viewModel.currentFileName.collectAsState()
    val unsavedFiles by viewModel.unsavedFiles.collectAsState()
    val files by viewModel.files.collectAsState()
    val lspRunning by viewModel.lspRunning.collectAsState()
    val goplsInstalled by viewModel.goplsInstalled.collectAsState()
    val installingGopls by viewModel.installingGopls.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    // 菜单状态
    var showTitleMenu by remember { mutableStateOf(false) } // 合并的项目+文件菜单
    var showEditMenu by remember { mutableStateOf(false) }   // 编辑菜单
    var showMoreMenu by remember { mutableStateOf(false) }    // 更多菜单
    var showExamplesMenu by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    // 对话框
    if (showNewFileDialog) {
        NewFileDialog(
            existingFiles = files.keys,
            onDismiss = { showNewFileDialog = false },
            onCreate = { name ->
                viewModel.createFile(name)
                showNewFileDialog = false
            }
        )
    }

    showDeleteConfirm?.let { fileName ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除文件") },
            text = { Text("确定要删除 $fileName 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFile(fileName)
                    showDeleteConfirm = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }

    if (showExamplesMenu) {
        ExamplesDialog(
            onDismiss = { showExamplesMenu = false },
            onLoad = { example ->
                viewModel.loadExample(example.files)
                showExamplesMenu = false
            }
        )
    }

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onToggleDrawer, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "文件抽屉",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        title = {
            // 标题: 当前文件名，点击弹出文件菜单
            Row(
                modifier = Modifier.clickable { showTitleMenu = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentFileName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "文件选择",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 文件下拉菜单（不含项目）
            DropdownMenu(
                expanded = showTitleMenu,
                onDismissRequest = { showTitleMenu = false }
            ) {
                for (fileName in files.keys) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    fileName,
                                    fontWeight = if (fileName == currentFileName) FontWeight.Bold else FontWeight.Normal
                                )
                                if (fileName == currentFileName) {
                                    Text("  (当前)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        onClick = {
                            viewModel.switchFile(fileName)
                            showTitleMenu = false
                        },
                        trailingIcon = {
                            if (fileName != "main.go") {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "删除",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            showDeleteConfirm = fileName
                                            showTitleMenu = false
                                        },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }

                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("新建文件...") },
                    onClick = {
                        showTitleMenu = false
                        showNewFileDialog = true
                    },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                )
            }
        },
        actions = {
            // 1. 运行 / 停止 / 诊断警告
            if (hasDiags && !isRunning) {
                // 有诊断时显示警告图标
                IconButton(onClick = onToggleDiagDrawer, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (hasErrors) Icons.Default.Error else Icons.Default.Warning,
                        if (hasErrors) "错误" else "警告",
                        tint = if (hasErrors) Color(0xFFFF5252) else Color(0xFFFFCC00),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(onClick = { if (isRunning) viewModel.stopRunning() else viewModel.runCode() }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        if (isRunning) "停止" else "运行",
                        tint = if (isRunning) Color(0xFFFF5252) else Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(2.dp))
            // 2. 撤销
            IconButton(onClick = {
                viewModel.triggerUndo(viewModel.getCurrentFileCode(), viewModel.currentCursor.value)
            }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Undo, "撤销", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            // 3. 重做
            IconButton(onClick = {
                viewModel.triggerRedo(viewModel.getCurrentFileCode(), viewModel.currentCursor.value)
            }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Redo, "重做", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            // 4. 编辑
            Box {
                IconButton(onClick = { showEditMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showEditMenu,
                    onDismissRequest = { showEditMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("粘贴") },
                        onClick = {
                            showEditMenu = false
                            viewModel.performEditorAction(GoStudioViewModel.EditorAction.PASTE)
                        },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("查找替换") },
                        onClick = {
                            showEditMenu = false
                            viewModel.performEditorAction(GoStudioViewModel.EditorAction.FIND_REPLACE)
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("复制当前行") },
                        onClick = {
                            showEditMenu = false
                            viewModel.performEditorAction(GoStudioViewModel.EditorAction.COPY_LINE)
                        },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("删除当前行") },
                        onClick = {
                            showEditMenu = false
                            viewModel.performEditorAction(GoStudioViewModel.EditorAction.DELETE_LINE)
                        },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("跳转到行") },
                        onClick = {
                            showEditMenu = false
                            viewModel.performEditorAction(GoStudioViewModel.EditorAction.GOTO_LINE)
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("跳转定义") },
                        onClick = {
                            Log.d("GoStudio", "跳转定义 menu item onClick")
                            showEditMenu = false
                            viewModel.gotoDefinitionDirect(viewModel.currentCursor.value)
                        },
                        leadingIcon = { Icon(Icons.Default.ArrowForward, contentDescription = null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("代码块") },
                        onClick = {
                            showEditMenu = false
                            viewModel.triggerSnippetDialog()
                        },
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) }
                    )
                }
            }
            Spacer(modifier = Modifier.width(2.dp))
            // 5. Go 教程
            IconButton(onClick = onToggleTutorialDrawer, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.MenuBook,
                    "Go教程",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(2.dp))

            // 6. 更多
            Box {
                IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, "更多", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("格式化") },
                        onClick = {
                            showMoreMenu = false
                            viewModel.formatCode()
                        },
                        leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("保存项目") },
                        onClick = {
                            showMoreMenu = false
                            viewModel.saveCurrentProject()
                        },
                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("go mod tidy") },
                        onClick = {
                            showMoreMenu = false
                            viewModel.goModTidy()
                        },
                        leadingIcon = { Icon(Icons.Default.DownloadDone, contentDescription = null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("加载示例") },
                        onClick = {
                            showMoreMenu = false
                            showExamplesMenu = true
                        },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("新建文件") },
                        onClick = {
                            showMoreMenu = false
                            showNewFileDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("终端") },
                        onClick = {
                            showMoreMenu = false
                            viewModel.openTerminal()
                        },
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) }
                    )
                    HorizontalDivider()
                    // gopls 相关
                    if (lspRunning) {
                        DropdownMenuItem(
                            text = { Text("停止 gopls") },
                            onClick = {
                                showMoreMenu = false
                                viewModel.stopLsp()
                            },
                            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) }
                        )
                    } else {
                        if (!goplsInstalled) {
                            DropdownMenuItem(
                                text = { Text(if (installingGopls) "正在安装 gopls..." else "安装 gopls") },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.installGopls()
                                },
                                enabled = !installingGopls,
                                leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) }
                            )
                        }
                        if (goplsInstalled) {
                            DropdownMenuItem(
                                text = { Text("启动 gopls") },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.startLsp()
                                },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun NewFileDialog(
    existingFiles: Set<String>,
    prefix: String = "",
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件") },
        text = {
            Column {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { input ->
                        fileName = input
                        val fullName = if (prefix.isNotEmpty()) "$prefix$input" else input
                        val err = when {
                            input.isBlank() -> null
                            existingFiles.contains(fullName) -> "文件已存在"
                            !input.endsWith(".go") && !input.endsWith(".mod") && !input.endsWith(".sum") ->
                                "建议使用 .go / .mod / .sum 后缀"
                            else -> null
                        }
                        errorText = err
                    },
                    label = { Text("文件名") },
                    placeholder = { Text(if (prefix.isNotEmpty()) "例如: ${prefix}utils.go" else "例如: utils.go") },
                    singleLine = true,
                    isError = errorText != null && fileName.isNotBlank() && existingFiles.contains(if (prefix.isNotEmpty()) "$prefix$fileName" else fileName),
                    modifier = Modifier.fillMaxWidth()
                )
                val err = errorText
                if (err != null) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = fileName.trim()
                    val fullName = if (prefix.isNotEmpty()) "$prefix$name" else name
                    if (name.isNotBlank() && !existingFiles.contains(fullName)) {
                        onCreate(fullName)
                    }
                },
                enabled = fileName.trim().isNotBlank() && !existingFiles.contains(if (prefix.isNotEmpty()) "${prefix}${fileName.trim()}" else fileName.trim())
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ExamplesDialog(
    onDismiss: () -> Unit,
    onLoad: (GoExamples.GoExample) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加载示例") },
        text = {
            Column {
                GoExamples.examples.forEach { example ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onLoad(example) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = example.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = example.description,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "文件: ${example.files.keys.joinToString(", ")}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 创建项目对话框
 */
@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, template: String) -> Unit,
    getTemplates: () -> List<Pair<String, String>>
) {
    var projectName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(0) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val templates = remember { getTemplates() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建项目") },
        text = {
            Column {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { input ->
                        projectName = input
                        errorText = when {
                            input.isBlank() -> null
                            !input.matches(Regex("^[a-zA-Z0-9_-]+$")) -> "只允许字母、数字、下划线和连字符"
                            input.length > 50 -> "项目名太长"
                            else -> null
                        }
                    },
                    label = { Text("项目名称") },
                    placeholder = { Text("例如: my-app") },
                    singleLine = true,
                    isError = errorText != null && projectName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText != null && projectName.isNotBlank()) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "选择模板：",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                templates.forEachIndexed { index, (name, desc) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { selectedTemplate = index },
                        shape = MaterialTheme.shapes.small,
                        color = if (index == selectedTemplate) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = desc,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            if (index == selectedTemplate) {
                                Text(
                                    text = "✓",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = projectName.trim()
                    if (name.isNotBlank() && name.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
                        onCreate(name, templates[selectedTemplate].first)
                    }
                },
                enabled = projectName.trim().matches(Regex("^[a-zA-Z0-9_-]+$"))
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(FlowPreview::class)
@Composable
fun GoCodeEditor(
    viewModel: GoStudioViewModel,
    modifier: Modifier = Modifier
) {
    val currentFileName by viewModel.currentFileName.collectAsState()
    val files by viewModel.files.collectAsState()
    val currentCode = files[currentFileName] ?: ""
    val lspRunning by viewModel.lspRunning.collectAsState()
    val showCompletions by viewModel.showCompletions.collectAsState()
    val completions by viewModel.completions.collectAsState()
    val selectedIndex by viewModel.selectedCompletionIndex.collectAsState()
    val codeFontSize by viewModel.codeFontSize.collectAsState()
    val showSnippetDialog by viewModel.showSnippetDialog.collectAsState()

    var textFieldValue by remember(currentFileName) {
        mutableStateOf(TextFieldValue(currentCode))
    }
    // 当前插入的 snippet，供 GoCodeEditor 处理
    var pendingSnippet by remember { mutableStateOf<GoStudioViewModel.CodeSnippet?>(null) }

    // 监听选中文字变化，更新 ViewModel 的 selectedWord
    LaunchedEffect(textFieldValue.selection) {
        val sel = textFieldValue.selection
        viewModel.updateSelection(sel.start, sel.end)
        viewModel.updateLastSelection(sel.start, sel.end)
        if (sel.start != sel.end && sel.end - sel.start < 50) {
            val word = textFieldValue.text.substring(sel.start, sel.end)
            if (word.all { it.isLetterOrDigit() || it == '_' || it == '.' }) {
                viewModel.updateSelectedWord(word)
            } else {
                viewModel.updateSelectedWord("")
            }
        } else {
            viewModel.updateSelectedWord("")
        }
    }

    // 如果有待插入的 snippet，插入到光标位置
    LaunchedEffect(pendingSnippet) {
        val snippet = pendingSnippet ?: return@LaunchedEffect
        pendingSnippet = null
        val cursor = textFieldValue.selection.start
        val text = textFieldValue.text

        // 在光标前找到当前行的缩进
        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val indent = text.substring(lineStart, cursor).takeWhile { it == ' ' || it == '\t' }

        // 用缩进替换 snippet 中的行首空格
        val indentedBody = snippet.body.lines().joinToString("\n") { line ->
            val stripped = line.removePrefix("  ") // snippet 用 4 空格缩进
            if (stripped.isBlank()) stripped else indent + stripped
        }

        // 插入 snippet
        val newText = text.substring(0, cursor) + "\n" + indentedBody + "\n" + text.substring(cursor)

        // 找到第一个 TODO 的位置作为光标
        val todoPos = newText.indexOf("TODO", cursor)
        val newCursor = if (todoPos >= 0) todoPos else cursor + indentedBody.length + 1

        val newTfv = textFieldValue.copy(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newCursor, newCursor + 4) // 选中 TODO 方便替换
        )
        textFieldValue = newTfv
        viewModel.updateEditorCode(newTfv.text)
    }

    // 显示代码块弹窗
    if (showSnippetDialog) {
        SnippetDialog(
            onDismiss = { viewModel.dismissSnippetDialog() },
            onSelect = { snippet ->
                viewModel.dismissSnippetDialog()
                pendingSnippet = snippet
            }
        )
    }

    // 注册编辑器操作回调
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(currentFileName) {
        viewModel.setEditorActionCallback { action ->
            when (action) {
                GoStudioViewModel.EditorAction.PASTE -> {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = clipboard.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val pasteText = clip.getItemAt(0).text?.toString() ?: return@setEditorActionCallback
                        val cursor = textFieldValue.selection.start
                        val newText = textFieldValue.text.substring(0, cursor) + pasteText + textFieldValue.text.substring(cursor)
                        textFieldValue = textFieldValue.copy(
                            text = newText,
                            selection = androidx.compose.ui.text.TextRange(cursor + pasteText.length)
                        )
                        viewModel.updateEditorCode(newText)
                    }
                }
                GoStudioViewModel.EditorAction.COPY_LINE -> {
                    val (newCode, _) = viewModel.duplicateCurrentLine(textFieldValue.text, textFieldValue.selection.start)
                    // 不更新光标，只是复制到剪贴板
                    val lineStart = textFieldValue.text.lastIndexOf('\n', textFieldValue.selection.start - 1).let { if (it == -1) 0 else it + 1 }
                    var lineEnd = textFieldValue.text.indexOf('\n', textFieldValue.selection.start)
                    if (lineEnd == -1) lineEnd = textFieldValue.text.length
                    val lineContent = textFieldValue.text.substring(lineStart, lineEnd)
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("GoStudio", lineContent))
                }
                GoStudioViewModel.EditorAction.DELETE_LINE -> {
                    val (newCode, newCursor) = viewModel.deleteCurrentLine(textFieldValue.text, textFieldValue.selection.start)
                    textFieldValue = textFieldValue.copy(text = newCode, selection = androidx.compose.ui.text.TextRange(newCursor))
                    viewModel.updateEditorCode(newCode)
                }
                GoStudioViewModel.EditorAction.FIND_REPLACE -> {
                    viewModel.showFindReplace()
                }
                GoStudioViewModel.EditorAction.GOTO_LINE -> {
                    viewModel.showGotoLine()
                }
                GoStudioViewModel.EditorAction.GOTO_DEFINITION -> {
                    // 计算当前光标的行号和列号
                    val text = textFieldValue.text
                    val cursor = textFieldValue.selection.start
                    var line = 0
                    var lineStart = 0
                    for (i in 0 until cursor) {
                        if (text[i] == '\n') { line++; lineStart = i + 1 }
                    }
                    val col = cursor - lineStart
                    viewModel.gotoDefinition(line, col)
                }
            }
        }
    }

    // 注册粘贴回调（保留兼容）
    LaunchedEffect(currentFileName) {
        viewModel.setPasteCallback {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val pasteText = clip.getItemAt(0).text?.toString() ?: return@setPasteCallback
                val cursor = textFieldValue.selection.start
                val newText = textFieldValue.text.substring(0, cursor) + pasteText + textFieldValue.text.substring(cursor)
                textFieldValue = textFieldValue.copy(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(cursor + pasteText.length)
                )
                viewModel.updateEditorCode(newText)
            }
        }
    }

    // 注册撤销/重做回调
    LaunchedEffect(currentFileName) {
        viewModel.setUndoRedoCallback { code, cursor ->
            textFieldValue = textFieldValue.copy(
                text = code,
                selection = androidx.compose.ui.text.TextRange(cursor)
            )
            viewModel.updateEditorCode(code, markUnsaved = false)
        }
    }

    // 注册跳转定义回调
    LaunchedEffect(currentFileName) {
        viewModel.setGotoDefinitionCallback { targetFile, line, character ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val text = textFieldValue.text
                var pos = 0
                var currentLine = 0
                while (currentLine < line && pos < text.length) {
                    if (text[pos] == '\n') currentLine++
                    pos++
                }
                pos += character
                textFieldValue = textFieldValue.copy(
                    selection = androidx.compose.ui.text.TextRange(pos)
                )
            }
        }
    }

    // 注册查找/替换回调
    LaunchedEffect(currentFileName) {
        viewModel.setFindNextCallback { query, caseSensitive ->
            val start = textFieldValue.selection.end.coerceAtMost(textFieldValue.text.length)
            val matchPos = findTextInCode(textFieldValue.text, query, start, caseSensitive)
            if (matchPos >= 0) {
                textFieldValue = textFieldValue.copy(
                    selection = androidx.compose.ui.text.TextRange(matchPos, matchPos + query.length)
                )
            }
        }
        viewModel.setFindPrevCallback { query, caseSensitive ->
            val selStart = textFieldValue.selection.start
            val end = if (selStart > query.length) selStart - query.length else 0
            val text = textFieldValue.text
            // 从 end 往前找最后一个匹配
            var lastPos = -1
            var idx = 0
            while (idx <= end) {
                idx = findTextInCode(text, query, idx, caseSensitive)
                if (idx < 0 || idx > end) break
                lastPos = idx
                idx++
            }
            if (lastPos >= 0) {
                textFieldValue = textFieldValue.copy(
                    selection = androidx.compose.ui.text.TextRange(lastPos, lastPos + query.length)
                )
            }
        }
        viewModel.setReplaceCurrentCallback { query, replacement, caseSensitive ->
            val sel = textFieldValue.selection
            if (sel.start != sel.end && textFieldValue.text.substring(sel.start, sel.end).equals(query, !caseSensitive)) {
                val newText = textFieldValue.text.substring(0, sel.start) + replacement + textFieldValue.text.substring(sel.end)
                viewModel.pushUndo(textFieldValue.text, sel.start)
                textFieldValue = textFieldValue.copy(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(sel.start + replacement.length)
                )
                viewModel.updateEditorCode(newText)
            }
        }
        viewModel.setReplaceAllCallback { query, replacement, caseSensitive ->
            viewModel.pushUndo(textFieldValue.text, 0)
            val newText = replaceAllInCode(textFieldValue.text, query, replacement, caseSensitive)
            textFieldValue = textFieldValue.copy(text = newText)
            viewModel.updateEditorCode(newText)
        }
        viewModel.setGotoLineCallback { line ->
            val text = textFieldValue.text
            val lines = text.split('\n')
            var pos = 0
            for (i in 0 until (line - 1).coerceAtMost(lines.size - 1)) {
                pos += lines[i].length + 1
            }
            textFieldValue = textFieldValue.copy(
                selection = androidx.compose.ui.text.TextRange(pos)
            )
            viewModel.dismissGotoLine()
        }
    }

    LaunchedEffect(currentCode) {
        if (textFieldValue.text != currentCode) {
            textFieldValue = TextFieldValue(currentCode)
        }
    }

    LaunchedEffect(lspRunning, currentFileName) {
        if (lspRunning) {
            try {
                var lastText = textFieldValue.text
                snapshotFlow { Pair(textFieldValue.text, textFieldValue.selection.start) }
                    .debounce(300)
                    .collect { (currentText, cursorPos) ->
                        viewModel.updateCursor(cursorPos)
                        val textChanged = currentText != lastText
                        lastText = currentText
                        if (!textChanged) {
                            viewModel.hideCompletions()
                            return@collect
                        }
                        if (cursorPos > 0) {
                            val ch = currentText[cursorPos - 1]
                            if (ch.isLetterOrDigit() || ch == '_' || ch == '.') {
                                val textBeforeCursor = currentText.substring(0, cursorPos)
                                val line = textBeforeCursor.count { it == '\n' }
                                val column = cursorPos - textBeforeCursor.lastIndexOf('\n') - 1
                                viewModel.requestCompletions(line, column)
                            } else {
                                viewModel.hideCompletions()
                            }
                        } else {
                            viewModel.hideCompletions()
                        }
                    }
            } catch (e: Exception) {
                Log.w("GoStudio", "补全监听异常: ${e.message}")
            }
        }
    }

    // 括号匹配高亮
    var bracketHighlightRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var textLayoutResultState by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

    LaunchedEffect(textFieldValue.selection.start) {
        val text = textFieldValue.text
        val cursor = textFieldValue.selection.start
        if (cursor > 0 && cursor <= text.length) {
            val ch = text[cursor - 1]
            val match = findMatchingBracket(text, cursor - 1, ch)
            bracketHighlightRange = match
        } else {
            bracketHighlightRange = null
        }
    }

    val selectionColors = TextSelectionColors(
        handleColor = Color(0xFF1E90FF),
        backgroundColor = Color(0xFF264F78)
    )

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val cursorLine = textFieldValue.text.substring(0, textFieldValue.selection.start)
        .count { it == '\n' } + 1
    val totalLines = textFieldValue.text.count { it == '\n' } + 1

    val density = LocalDensity.current
    val lineNumberWidth = with(density) {
        (totalLines.toString().length * 10 + 8).dp
    }

    val fontSizeSp = with(density) { codeFontSize.sp }
    val lineHeightSp = (codeFontSize * 1.43f).sp

    val codeStyle = TextStyle(
        color = GoSyntaxColors.NORMAL,
        fontFamily = FontFamily.Monospace,
        fontSize = fontSizeSp,
        lineHeight = lineHeightSp
    )

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF1E1E1E))) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (showCompletions) Modifier.padding(bottom = 120.dp) else Modifier
                    )
                    // 双指缩放支持
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom: Float, _ ->
                            if (zoom != 1.0f) {
                                val current = viewModel.codeFontSize.value
                                val newSize = current * zoom
                                viewModel.updateCodeFontSize(newSize)
                            }
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(lineNumberWidth)
                        .verticalScroll(verticalScrollState)
                        .background(Color(0xFF252526))
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (lineNum in 1..totalLines) {
                        Text(
                            text = lineNum.toString(),
                            color = if (lineNum == cursorLine) Color.White else Color(0xFF858585),
                            fontSize = fontSizeSp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = lineHeightSp,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .then(
                                    if (lineNum == cursorLine) {
                                        Modifier.background(
                                            Color(0xFF214283),
                                            androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }

                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        val textChanged = newValue.text != textFieldValue.text
                        // 推入撤销栈（只在文字变化时）
                        if (textChanged) {
                            viewModel.pushUndo(textFieldValue.text, textFieldValue.selection.start)
                        }

                        // 如果补全弹窗打开且用户按了回车，选中当前补全项
                        if (showCompletions && completions.isNotEmpty()) {
                            // 检测是否是回车输入（换行符插入）
                            if (newValue.text.length > textFieldValue.text.length &&
                                newValue.text.substring(textFieldValue.selection.end, newValue.selection.end) == "\n") {
                                // 回车 → 确认补全，不插入换行
                                val selectedItem = viewModel.getSelectedCompletionItem()
                                if (selectedItem != null) {
                                    val insertText = viewModel.selectCompletion(selectedItem)
                                    if (insertText != null) {
                                        applyCompletion(textFieldValue, insertText, selectedItem.kind) { newTfv ->
                                            textFieldValue = newTfv
                                            viewModel.updateEditorCode(newTfv.text)
                                        }
                                    }
                                    return@BasicTextField
                                }
                            }
                        }
                        val processed = processEditorInput(textFieldValue, newValue, viewModel)
                        textFieldValue = processed
                        if (textChanged) {
                            viewModel.updateEditorCode(processed.text)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .horizontalScroll(horizontalScrollState)
                        .verticalScroll(verticalScrollState)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    // 双击选词
                                    val layout = textLayoutResultState ?: return@detectTapGestures
                                    val charOffset = layout.getOffsetForPosition(offset)
                                    val text = textFieldValue.text
                                    if (charOffset < 0 || charOffset >= text.length) return@detectTapGestures

                                    var start = charOffset
                                    var end = charOffset
                                    val ch = text[charOffset]
                                    if (ch.isLetterOrDigit() || ch == '_' || ch == '.') {
                                        while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '_' || text[start - 1] == '.')) start--
                                        while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '_' || text[end] == '.')) end++
                                    } else if (ch.isWhitespace()) {
                                        while (start > 0 && text[start - 1].isWhitespace()) start--
                                        while (end < text.length && text[end].isWhitespace()) end++
                                    }
                                    textFieldValue = textFieldValue.copy(
                                        selection = androidx.compose.ui.text.TextRange(start, end)
                                    )

                                    // 双击选中标识符后，自动尝试跳转定义
                                    if (end > start && (ch.isLetterOrDigit() || ch == '_' || ch == '.')) {
                                        val selectedWord = text.substring(start, end)
                                        Log.d("GoStudio", "双击选中: '$selectedWord'，尝试跳转定义")
                                        viewModel.gotoDefinitionDirect(start)
                                    }
                                }
                            )
                        }
                        .onKeyEvent { event ->
                            // 补全弹窗中按上下键选择
                            if (showCompletions && completions.isNotEmpty()) {
                                val nativeEvent = event.nativeKeyEvent
                                when (nativeEvent?.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                        viewModel.moveCompletionSelection(-1)
                                        true
                                    }
                                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        viewModel.moveCompletionSelection(1)
                                        true
                                    }
                                    android.view.KeyEvent.KEYCODE_ENTER,
                                    android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                        // Enter 确认补全（在 onValueChange 中处理）
                                        false
                                    }
                                    android.view.KeyEvent.KEYCODE_ESCAPE -> {
                                        viewModel.hideCompletions()
                                        true
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        }
                        .drawBehind {
                            val layout = textLayoutResultState ?: return@drawBehind
                            val range = bracketHighlightRange ?: return@drawBehind
                            val highlightColor = Color(0xFF515151)
                            // 高亮光标所在的括号
                            try {
                                val box = layout.getBoundingBox(textFieldValue.selection.start - 1)
                                drawRect(highlightColor, topLeft = androidx.compose.ui.geometry.Offset(box.left, box.top), size = androidx.compose.ui.geometry.Size(box.width, box.height))
                            } catch (_: Exception) {}
                            // 高亮匹配的括号
                            try {
                                val box = layout.getBoundingBox(range.first)
                                drawRect(highlightColor, topLeft = androidx.compose.ui.geometry.Offset(box.left, box.top), size = androidx.compose.ui.geometry.Size(box.width, box.height))
                            } catch (_: Exception) {}
                        },
                    textStyle = codeStyle,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFFFFF)),
                    onTextLayout = { textLayoutResultState = it },
                    visualTransformation = { text ->
                        val highlighted = highlightGo(text.text)
                        TransformedText(
                            text = highlighted,
                            offsetMapping = OffsetMapping.Identity
                        )
                    }
                )
            }

            // 补全面板：缩小到 120dp，字体 12sp，最多显示 5 项
            if (showCompletions && completions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                ) {
                    CompletionPopup(
                        completions = completions.take(5),
                        selectedIndex = selectedIndex,
                        onSelect = { item ->
                            val insertText = viewModel.selectCompletion(item)
                            if (insertText != null) {
                                applyCompletion(textFieldValue, insertText, item.kind) { newTfv ->
                                    textFieldValue = newTfv
                                    viewModel.updateEditorCode(newTfv.text)
                                }
                            }
                        },
                        onDismiss = { viewModel.hideCompletions() }
                    )
                }
            }
        }
        // 键盘快捷符号栏（放在 Box 外底部，imePadding 跟随键盘）
        QuickSymbolBar(
            onInsert = { symbol ->
                val cursor = textFieldValue.selection.start
                val text = textFieldValue.text
                val afterCursor = text.substring(cursor)

                // 自动补全配对符号：开括号类插入配对，闭括号类跳过已配对的
                val pairs = mapOf(
                    '"' to "\"\"", "'" to "''", '`' to "``",
                    '(' to "()", '{' to "{}", '[' to "[]"
                )
                val closingOf = mapOf(')' to '(', '}' to '{', ']' to '[')

                when {
                    pairs.containsKey(symbol) -> {
                        // 开括号/引号：插入配对，光标放中间
                        val pair = pairs[symbol]!!
                        val newText = text.substring(0, cursor) + pair + text.substring(cursor)
                        textFieldValue = textFieldValue.copy(
                            text = newText,
                            selection = androidx.compose.ui.text.TextRange(cursor + 1)
                        )
                        viewModel.updateEditorCode(newText)
                        return@QuickSymbolBar
                    }
                    closingOf.containsKey(symbol.firstOrNull()) -> {
                        // 闭括号：如果光标后已是配对的闭括号则跳过
                        val expected = symbol.firstOrNull()
                        if (afterCursor.startsWith(expected.toString())) {
                            // 跳过，只移动光标
                            textFieldValue = textFieldValue.copy(
                                selection = androidx.compose.ui.text.TextRange(cursor + 1)
                            )
                            return@QuickSymbolBar
                        }
                    }
                }

                // 其他符号（包括不匹配的闭括号）正常插入
                val newText = text.substring(0, cursor) + symbol + text.substring(cursor)
                textFieldValue = textFieldValue.copy(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(cursor + symbol.length)
                )
                viewModel.updateEditorCode(newText)
            }
        )
        } // Column
    } // CompositionLocalProvider
}

/**
 * 应用补全文本到编辑器
 * 对于函数类型（kind == 3 或 2），光标放在括号内
 */
private fun applyCompletion(
    currentValue: TextFieldValue,
    insertText: String,
    kind: Int,
    onApplied: (TextFieldValue) -> Unit
) {
    val text = currentValue.text
    val cursor = currentValue.selection.start

    var wordStart = cursor
    while (wordStart > 0) {
        val ch = text[wordStart - 1]
        if (ch.isLetterOrDigit() || ch == '_') {
            wordStart--
        } else {
            break
        }
    }

    val newText = text.substring(0, wordStart) + insertText + text.substring(cursor)
    // 函数/方法类型：光标放在 () 内
    val newCursor = if ((kind == 3 || kind == 2) && insertText.endsWith("()")) {
        wordStart + insertText.length - 1
    } else {
        wordStart + insertText.length
    }
    onApplied(currentValue.copy(
        text = newText,
        selection = androidx.compose.ui.text.TextRange(newCursor)
    ))
}

@Composable
fun CompletionPopup(
    completions: List<CompletionItem>,
    selectedIndex: Int,
    onSelect: (CompletionItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF252526),
        shape = MaterialTheme.shapes.extraSmall,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3C3C3C)),
        modifier = modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(completions) { index, item ->
                val isSelected = index == selectedIndex
                Surface(
                    color = if (isSelected) Color(0xFF094771) else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = CompletionItem.kindIcon(item.kind),
                            color = Color(0xFF858585),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = item.label,
                            color = if (isSelected) Color.White else Color(0xFFD4D4D4),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        if (!item.detail.isNullOrBlank()) {
                            Text(
                                text = item.detail,
                                color = Color(0xFF9CDCFE),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===== 括号匹配 =====

/**
 * 查找与指定位置括号匹配的另一个括号的位置
 * 返回匹配的括号位置，如果不匹配返回 null
 */
private fun findMatchingBracket(text: String, pos: Int, char: Char): Pair<Int, Int>? {
    val pairs = mapOf('(' to ')', ')' to '(', '{' to '}', '}' to '{', '[' to ']', ']' to '[')
    val match = pairs[char] ?: return null
    val isOpen = char == '(' || char == '{' || char == '['

    var depth = 0
    var i = if (isOpen) pos else pos
    val end = if (isOpen) text.length else -1
    val step = if (isOpen) 1 else -1

    while (i != end) {
        val c = text[i]
        if (c == char) depth++
        else if (c == match) depth--
        if (depth == 0) return Pair(pos, i)
        i += step
    }
    return null
}

// ===== 键盘快捷符号栏 =====

/**
 * 键盘上方快捷符号栏
 * 默认显示 Go 常用符号，上滑切换到 Go 关键字
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickSymbolBar(
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val symbols = remember {
        listOf("(", ")", "{", "}", "[", "]", "\"", "'", "`", ",", ";", ".", ":", "//", "/*", "*/", "+", "-", "*", "/", "%", "=", "!=", "==", "<", ">", "<=", ">=", "&", "|", "^", "~", "&&", "||", "<<", ">>", "++", "--", ":=", "->", "<-", "...", "\t", "\\n")
    }
    val scrollState = rememberScrollState()

    Surface(
        color = Color(0xFF2D2D30),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            for (item in symbols) {
                val displayText = when (item) {
                    "\\n" -> "↵"
                    "\\t" -> "⇥"
                    else -> item
                }
                val insertText = when (item) {
                    "\\n" -> "\n"
                    "\\t" -> "  "
                    else -> item
                }
                Surface(
                    color = Color(0xFF3C3C3C),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = displayText,
                        fontSize = 13.sp,
                        color = Color(0xFFE0E0E0),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { onInsert(insertText) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun processEditorInput(
    oldValue: TextFieldValue,
    newValue: TextFieldValue,
    viewModel: GoStudioViewModel? = null
): TextFieldValue {
    val oldText = oldValue.text
    val newText = newValue.text
    val oldEnd = oldValue.selection.end
    val newEnd = newValue.selection.end

    // Tab 键 → 插入4个空格
    if (newText.length == oldText.length + 1 && newText.substring(newEnd - 1, newEnd) == "\t") {
        val result = newText.substring(0, newEnd - 1) + "  " + newText.substring(newEnd)
        return newValue.copy(
            text = result,
            selection = androidx.compose.ui.text.TextRange(newEnd + 3)
        )
    }

    // 回车 → 自动缩进
    if (newText.length == oldText.length + 1 && newText.substring(newEnd - 1, newEnd) == "\n") {
        val lineStart = newText.lastIndexOf('\n', newEnd - 2).let { if (it == -1) 0 else it + 1 }
        val currentLine = newText.substring(lineStart, newEnd - 1)
        val indent = currentLine.takeWhile { it == ' ' || it == '\t' }

        val trimmedLine = currentLine.trimEnd()
        val extraIndent = if (trimmedLine.endsWith("{")) "  " else ""

        val insert = indent + extraIndent
        val result = newText.substring(0, newEnd) + insert + newText.substring(newEnd)
        return newValue.copy(
            text = result,
            selection = androidx.compose.ui.text.TextRange(newEnd + insert.length)
        )
    }

    // 自动补全括号
    if (newText.length == oldText.length + 1 && newEnd >= 1) {
        val insertedChar = newText[newEnd - 1]
        val closeChar = when (insertedChar) {
            '(' -> ')'
            '{' -> '}'
            '"' -> '"'
            '`' -> '`'
            '[' -> ']'
            else -> null
        }
        if (closeChar != null) {
            val result = newText.substring(0, newEnd) + closeChar + newText.substring(newEnd)
            return newValue.copy(
                text = result,
                selection = androidx.compose.ui.text.TextRange(newEnd) // 光标在两个括号之间
            )
        }
    }

    // 如果输入的是右括号/引号，且下一个字符是相同的闭合符，直接跳过（不重复输入）
    if (newText.length == oldText.length + 1 && newEnd < newText.length) {
        val insertedChar = newText[newEnd - 1]
        val nextChar = newText[newEnd]
        if (insertedChar == nextChar && ")}]\"`".contains(insertedChar)) {
            // 跳过闭合符：保持原来的文本，光标右移
            return newValue.copy(
                selection = androidx.compose.ui.text.TextRange(newEnd + 1)
            )
        }
    }

    return newValue
}

@Composable
fun OutputPanel(
    outputLines: List<GoStudioViewModel.OutputLine>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var copiedToast by remember { mutableStateOf(false) }

    LaunchedEffect(outputLines.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        color = Color(0xFF1E1E1E),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "输出",
                    color = Color(0xFF888888),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (outputLines.isNotEmpty()) {
                    Text(
                        text = if (copiedToast) "已复制" else "复制全部",
                        color = if (copiedToast) Color(0xFF4CAF50) else Color(0xFF858585),
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            val text = outputLines.joinToString("\n") { it.text }
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("GoStudio Log", text))
                            copiedToast = true
                        }
                    )
                }
            }
            if (outputLines.isEmpty()) {
                Text(
                    text = "点击运行按钮执行代码...",
                    color = Color(0xFF666666),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            outputLines.forEach { line ->
                val color = when (line.type) {
                    GoStudioViewModel.OutputLine.Type.STDOUT -> Color(0xFFD4D4D4)
                    GoStudioViewModel.OutputLine.Type.STDERR -> Color(0xFFFF6B6B)
                    GoStudioViewModel.OutputLine.Type.ERROR -> Color(0xFFFF6B6B)
                    GoStudioViewModel.OutputLine.Type.INFO -> Color(0xFF888888)
                }
                Text(
                    text = line.text,
                    color = color,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("GoStudio Log", line.text))
                    }
                )
            }
        }
    }
}

// ===== Scaffold 内查找替换栏（通过 ViewModel 操作编辑器） =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FindReplaceBarInScaffold(
    viewModel: GoStudioViewModel
) {
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var currentMatchIndex by remember { mutableIntStateOf(1) }

    val currentCode by viewModel.files.collectAsState()
    val currentFileName by viewModel.currentFileName.collectAsState()
    val text = currentCode[currentFileName] ?: ""
    val cursor by viewModel.currentCursor.collectAsState()

    // 匹配数
    var matchCount by remember { mutableStateOf(-1) }
    LaunchedEffect(findQuery, text, caseSensitive) {
        matchCount = if (findQuery.isEmpty()) -1 else {
            var count = 0
            var idx = 0
            while (true) {
                idx = findTextInCode(text, findQuery, idx, caseSensitive)
                if (idx < 0) break
                count++
                idx++
            }
            count
        }
    }

    Surface(
        color = Color(0xFF2D2D30),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                // 查找输入框 — 用 BasicTextField 更紧凑
                Surface(
                    color = Color(0xFF3C3C3C),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f).height(30.dp)
                ) {
                    BasicTextField(
                        value = findQuery,
                        onValueChange = { findQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                // 当前/总数
                Text(
                    text = if (matchCount > 0) "$currentMatchIndex/$matchCount" else if (matchCount == 0) "0/0" else "",
                    fontSize = 10.sp,
                    color = if (matchCount > 0) Color(0xFF4FC1FF) else Color(0xFFFF6B6B),
                    modifier = Modifier.width(30.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(2.dp))
                // 上一个
                Surface(
                    color = Color(0xFF3C3C3C),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.clickable {
                        viewModel.findPrev(findQuery, caseSensitive)
                        currentMatchIndex = (currentMatchIndex - 2).coerceAtLeast(0) + 1
                    }
                ) {
                    Text("↑", color = Color(0xFFCCCCCC), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.width(2.dp))
                // 下一个
                Surface(
                    color = Color(0xFF3C3C3C),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.clickable {
                        viewModel.findNext(findQuery, caseSensitive)
                        currentMatchIndex = (currentMatchIndex % matchCount) + 1
                    }
                ) {
                    Text("↓", color = Color(0xFFCCCCCC), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.width(2.dp))
                Surface(
                    color = if (caseSensitive) Color(0xFF4FC1FF) else Color(0xFF3C3C3C),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.clickable { caseSensitive = !caseSensitive }
                ) {
                    Text("Aa", color = if (caseSensitive) Color.White else Color(0xFF888888), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.width(2.dp))
                Surface(
                    color = Color(0xFF3C3C3C),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.clickable { viewModel.dismissFindReplace() }
                ) {
                    Text("✕", color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 3.dp)) {
                // 替换输入框
                Surface(
                    color = Color(0xFF3C3C3C),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f).height(30.dp)
                ) {
                    BasicTextField(
                        value = replaceQuery,
                        onValueChange = { replaceQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    color = Color(0xFF3C3C3C),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.clickable { viewModel.replaceCurrent(findQuery, replaceQuery, caseSensitive) }
                ) {
                    Text("替换", color = Color(0xFFCCCCCC), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.width(2.dp))
                Surface(
                    color = Color(0xFF3C3C3C),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.clickable { viewModel.replaceAll(findQuery, replaceQuery, caseSensitive) }
                ) {
                    Text("全部", color = Color(0xFFCCCCCC), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

// ===== 查找替换栏（旧版，GoCodeEditor 内用，保留但不再使用） =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FindReplaceBar(
    text: String,
    selection: Int,
    onFind: (query: String, caseSensitive: Boolean) -> Int,
    onReplace: (query: String, replacement: String, caseSensitive: Boolean) -> Unit,
    onReplaceAll: (query: String, replacement: String, caseSensitive: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var matchCount by remember { mutableStateOf(-1) }

    // 计算匹配数
    LaunchedEffect(findQuery, text, caseSensitive) {
        matchCount = if (findQuery.isEmpty()) -1 else {
            var count = 0
            var idx = 0
            while (true) {
                idx = findTextInCode(text, findQuery, idx, caseSensitive)
                if (idx < 0) break
                count++
                idx++
            }
            count
        }
    }

    Surface(
        color = Color(0xFF2D2D30),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // 第一行：查找
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = findQuery,
                    onValueChange = { findQuery = it },
                    placeholder = { Text("查找", fontSize = 13.sp, color = Color(0xFF888888)) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4FC1FF),
                        unfocusedBorderColor = Color(0xFF555555),
                        cursorColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                // 匹配数
                Text(
                    text = if (matchCount >= 0) "$matchCount" else "",
                    fontSize = 11.sp,
                    color = if (matchCount > 0) Color(0xFF4FC1FF) else Color(0xFFFF6B6B),
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(4.dp))
                // 查找下一个
                Surface(
                    color = Color(0xFF3C3C3C),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.clickable { onFind(findQuery, caseSensitive) }
                ) {
                    Text("↓", color = Color(0xFFCCCCCC), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                // 大小写切换
                Surface(
                    color = if (caseSensitive) Color(0xFF4FC1FF) else Color(0xFF3C3C3C),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.clickable { caseSensitive = !caseSensitive }
                ) {
                    Text("Aa", color = if (caseSensitive) Color.White else Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                // 关闭
                Surface(
                    color = Color(0xFF3C3C3C),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.clickable { onDismiss() }
                ) {
                    Text("✕", color = Color(0xFF888888), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                }
            }

            // 第二行：替换
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = { replaceQuery = it },
                    placeholder = { Text("替换", fontSize = 13.sp, color = Color(0xFF888888)) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4FC1FF),
                        unfocusedBorderColor = Color(0xFF555555),
                        cursorColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    color = Color(0xFF3C3C3C),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.clickable { onReplace(findQuery, replaceQuery, caseSensitive) }
                ) {
                    Text("替换", color = Color(0xFFCCCCCC), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    color = Color(0xFF3C3C3C),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.clickable { onReplaceAll(findQuery, replaceQuery, caseSensitive) }
                ) {
                    Text("全部", color = Color(0xFFCCCCCC), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

// ===== 跳转行对话框 =====

@Composable
private fun GotoLineDialog(
    totalLines: Int,
    onGoto: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var lineText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳转到行", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = lineText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) lineText = input
                    },
                    placeholder = { Text("输入行号 (1-$totalLines)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val line = lineText.toIntOrNull() ?: return@TextButton
                    if (line in 1..totalLines) onGoto(line)
                },
                enabled = lineText.toIntOrNull()?.let { it in 1..totalLines } ?: false
            ) { Text("跳转") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ===== 查找替换辅助函数 =====

private fun findTextInCode(text: String, query: String, startFrom: Int, caseSensitive: Boolean): Int {
    if (query.isEmpty()) return -1
    val searchText = if (caseSensitive) text else text.lowercase()
    val searchQuery = if (caseSensitive) query else query.lowercase()
    val idx = searchText.indexOf(searchQuery, startFrom)
    return if (idx >= 0 && idx < text.length) idx else -1
}

private fun replaceAllInCode(text: String, query: String, replacement: String, caseSensitive: Boolean): String {
    if (query.isEmpty()) return text
    return if (caseSensitive) text.replace(query, replacement)
    else text.replace(Regex(Regex.escape(query), RegexOption.IGNORE_CASE), replacement)
}