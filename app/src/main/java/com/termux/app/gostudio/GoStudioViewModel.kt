package com.termux.app.gostudio

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termux.app.gostudio.executor.TermuxShellExecutor
import com.termux.app.gostudio.lsp.CompletionItem
import com.termux.app.gostudio.lsp.Diagnostic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.io.File

/**
 * GoStudio 主 ViewModel
 * 直接使用 Termux shell 执行 Go 代码
 */
class GoStudioViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "GoStudioVM"

        val DEFAULT_CODE = """
            package main

            import "fmt"

            func main() {
                fmt.Println("Hello, GoStudio!")
                fmt.Println("Go is running on your phone!")
            }
        """.trimIndent()

        val DEFAULT_GO_MOD = "module gostudio\n\ngo 1.21"

        /** 预置 Go 代码块 */
        val BUILTIN_SNIPPETS = listOf(
            CodeSnippet("main", "main 函数框架", """
                |package main
                |
                |import "fmt"
                |
                |func main() {
                |    fmt.Println("TODO")
                |}""".trimMargin()),
            CodeSnippet("iferr", "if err != nil", """
                |if err != nil {
                |    TODO
                |    return err
                |}""".trimMargin()),
            CodeSnippet("forr", "for range 循环", """
                |for i, v := range TODO {
                |    
                |}""".trimMargin()),
            CodeSnippet("forn", "for i := 0; i < n; i++", """
                |for i := 0; i < TODO; i++ {
                |    
                |}""".trimMargin()),
            CodeSnippet("func", "函数定义", """
                |func TODO(params) returnType {
                |    
                |}""".trimMargin()),
            CodeSnippet("httph", "HTTP handler", """
                |func TODO(w http.ResponseWriter, r *http.Request) {
                |    w.Header().Set("Content-Type", "application/json")
                |    
                |}""".trimMargin()),
            CodeSnippet("struct", "结构体定义", """
                |type TODO struct {
                |    
                |}""".trimMargin()),
            CodeSnippet("goroutine", "goroutine", """
                |go func() {
                |    TODO
                |}()""".trimMargin())
        )
    }

    /** 代码块模板，label 为触发词，body 为插入内容，placeholder 标记光标位置 */
    data class CodeSnippet(
        val label: String,
        val description: String,
        val body: String
    )

    private val shellExecutor = TermuxShellExecutor(getApplication())

    // 运行目录 - 直接指向当前项目目录
    private val runDir: String
        get() {
            val name = _projectName.value
            return if (name.isNotEmpty()) "$projectsDir/$name" else ""
        }

    // ===== 状态 =====

    // Go 是否已安装
    private val _goInstalled = MutableStateFlow(false)
    val goInstalled: StateFlow<Boolean> = _goInstalled.asStateFlow()

    // 代码编辑区字体大小（支持缩放）
    private val _codeFontSize = MutableStateFlow(14f)
    val codeFontSize: StateFlow<Float> = _codeFontSize.asStateFlow()

    /** 调整代码字体大小，限制在 10sp ~ 28sp */
    fun updateCodeFontSize(newSize: Float) {
        _codeFontSize.value = newSize.coerceIn(10f, 28f)
    }

    // 代码块弹窗触发信号
    private val _showSnippetDialog = MutableStateFlow(false)
    val showSnippetDialog: StateFlow<Boolean> = _showSnippetDialog.asStateFlow()

    fun triggerSnippetDialog() { _showSnippetDialog.value = true }
    fun dismissSnippetDialog() { _showSnippetDialog.value = false }

    // 兼容旧 UI
    val termuxAvailable: StateFlow<Boolean> = MutableStateFlow(true)
    val envInitialized: StateFlow<Boolean> = MutableStateFlow(true)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _outputLines = MutableStateFlow<List<OutputLine>>(emptyList())
    val outputLines: StateFlow<List<OutputLine>> = _outputLines.asStateFlow()

    private val _outputExpanded = MutableStateFlow(true)
    val outputExpanded: StateFlow<Boolean> = _outputExpanded.asStateFlow()
    private val _outputMaximized = MutableStateFlow(false)
    val outputMaximized: StateFlow<Boolean> = _outputMaximized.asStateFlow()
    fun toggleOutputMaximize() { _outputMaximized.value = !_outputMaximized.value }

    private val _installingGopls = MutableStateFlow(false)
    val installingGopls: StateFlow<Boolean> = _installingGopls.asStateFlow()

    private val _goplsInstallProgress = MutableStateFlow("")
    val goplsInstallProgress: StateFlow<String> = _goplsInstallProgress.asStateFlow()

    private val _goplsInstalled = MutableStateFlow(false)
    val goplsInstalled: StateFlow<Boolean> = _goplsInstalled.asStateFlow()

    // LSP
    private val _lspRunning = MutableStateFlow(false)
    val lspRunning: StateFlow<Boolean> = _lspRunning.asStateFlow()

    private val _diagnostics = MutableStateFlow<Map<String, List<Diagnostic>>>(emptyMap())
    val diagnostics: StateFlow<Map<String, List<Diagnostic>>> = _diagnostics.asStateFlow()

    private val _completions = MutableStateFlow<List<CompletionItem>>(emptyList())
    val completions: StateFlow<List<CompletionItem>> = _completions.asStateFlow()

    private val _showCompletions = MutableStateFlow(false)
    val showCompletions: StateFlow<Boolean> = _showCompletions.asStateFlow()

    private val _selectedCompletionIndex = MutableStateFlow(0)
    val selectedCompletionIndex: StateFlow<Int> = _selectedCompletionIndex.asStateFlow()

    // gopls 路径
    private val goplsPath = "${com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH}/go/bin/gopls"
    private var lspService: com.termux.app.gostudio.lsp.LspService? = null
    private val openFiles = mutableMapOf<String, String>()

    // 粘贴回调（由 UI 层设置）
    private var pasteCallback: (() -> Unit)? = null
    fun setPasteCallback(callback: () -> Unit) { pasteCallback = callback }
    fun pasteFromCodeEditor() { pasteCallback?.invoke() }

    // 撤销/重做栈（同时保存光标位置）
    internal data class UndoState(val code: String, val cursor: Int)
    private val undoStack = java.util.ArrayDeque<UndoState>()
    private val redoStack = java.util.ArrayDeque<UndoState>()
    private var lastPushedCode: String = ""
    private var _currentCursor = MutableStateFlow(0)
    val currentCursor: StateFlow<Int> = _currentCursor.asStateFlow()

    fun updateCursor(pos: Int) { _currentCursor.value = pos }
    private val _currentSelectionEnd = MutableStateFlow(0)
    val currentSelectionEnd: StateFlow<Int> = _currentSelectionEnd.asStateFlow()
    fun updateSelection(start: Int, end: Int) { _currentCursor.value = start; _currentSelectionEnd.value = end }
    // 最近一次有效选区的起止位置（供跳转定义用）
    private val _lastSelection = MutableStateFlow(Pair(0, 0))
    val lastSelection: StateFlow<Pair<Int, Int>> = _lastSelection.asStateFlow()
    fun updateLastSelection(start: Int, end: Int) { _lastSelection.value = Pair(start, end) }

    /** 推入撤销历史（在编辑前调用） */
    fun pushUndo(code: String, cursor: Int) {
        if (code != lastPushedCode) {
            undoStack.addLast(UndoState(code, cursor))
            if (undoStack.size > 100) undoStack.removeFirst()
            redoStack.clear()
            lastPushedCode = code
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    /** 撤销 */
    internal fun undo(currentCode: String, currentCursor: Int): UndoState? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(UndoState(currentCode, currentCursor))
        return undoStack.removeLast()
    }

    /** 重做 */
    internal fun redo(currentCode: String, currentCursor: Int): UndoState? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(UndoState(currentCode, currentCursor))
        return redoStack.removeLast()
    }

    // 撤销/重做回调
    private var undoRedoCallback: ((code: String, cursor: Int) -> Unit)? = null
    fun setUndoRedoCallback(callback: (String, Int) -> Unit) { undoRedoCallback = callback }
    fun triggerUndo(currentCode: String, currentCursor: Int) {
        val state = undo(currentCode, currentCursor) ?: return
        undoRedoCallback?.invoke(state.code, state.cursor)
    }
    fun triggerRedo(currentCode: String, currentCursor: Int) {
        val state = redo(currentCode, currentCursor) ?: return
        undoRedoCallback?.invoke(state.code, state.cursor)
    }

    // 编辑器操作回调
    private var editorActionCallback: ((EditorAction) -> Unit)? = null
    fun setEditorActionCallback(callback: (EditorAction) -> Unit) { editorActionCallback = callback }
    fun performEditorAction(action: EditorAction) { Log.d(TAG, "performEditorAction: $action"); editorActionCallback?.invoke(action) }

    enum class EditorAction {
        PASTE, COPY_LINE, DELETE_LINE, FIND_REPLACE, GOTO_LINE, GOTO_DEFINITION
    }

    // 查找替换对话框
    private val _showFindReplaceDialog = MutableStateFlow(false)
    val showFindReplaceDialog: StateFlow<Boolean> = _showFindReplaceDialog.asStateFlow()
    fun showFindReplace() { _showFindReplaceDialog.value = true }
    fun dismissFindReplace() { _showFindReplaceDialog.value = false }

    // 跳转行对话框
    private val _showGotoLineDialog = MutableStateFlow(false)
    val showGotoLineDialog: StateFlow<Boolean> = _showGotoLineDialog.asStateFlow()
    fun showGotoLine() { _showGotoLineDialog.value = true }
    fun dismissGotoLine() { _showGotoLineDialog.value = false }

    // 跳转到定义
    private var gotoDefinitionCallback: ((targetFile: String, line: Int, character: Int) -> Unit)? = null
    fun setGotoDefinitionCallback(callback: (String, Int, Int) -> Unit) { gotoDefinitionCallback = callback }

    fun gotoDefinition(cursorLine: Int, cursorColumn: Int) = gotoDefinitionInternal(cursorLine, cursorColumn)

    fun gotoDefinitionDirect(cursorPos: Int, selectionEnd: Int = cursorPos) {
        val code = _files.value[_currentFileName.value] ?: return
        val midPos = if (selectionEnd > cursorPos) (cursorPos + selectionEnd) / 2 else cursorPos
        var line = 0
        var lineStart = 0
        for (i in 0 until midPos) {
            if (code[i] == '\n') { line++; lineStart = i + 1 }
        }
        // 找到行尾，限制列号不超出
        var lineEnd = code.indexOf('\n', lineStart)
        if (lineEnd == -1) lineEnd = code.length
        var col = (midPos - lineStart).coerceAtMost(lineEnd - lineStart - 1).coerceAtLeast(0)
        Log.d("GoStudio", "gotoDefinitionDirect: cursor=$cursorPos, mid=$midPos, line=$line, col=$col, lineEnd=$lineEnd")
        gotoDefinitionInternal(line, col)
    }

    private fun gotoDefinitionInternal(cursorLine: Int, cursorColumn: Int) {
        val fileName = _currentFileName.value
        val lsp = lspService ?: run { Log.w("GoStudio", "gotoDefinition: lspService is null"); return }
        val baseDir = runDir
        val fileUri = "file://$baseDir/$fileName"
        Log.d("GoStudio", "gotoDefinition: projectName='${_projectName.value}', baseDir=$baseDir, fileName=$fileName, uri=$fileUri")
        lsp.getDefinition(fileUri, cursorLine, cursorColumn) { locations ->
            Log.d("GoStudio", "gotoDefinition: got ${locations.size} locations")
            if (locations.isNotEmpty()) {
                val loc = locations[0]
                val targetFile = loc.uri.removePrefix("file://").removePrefix("$baseDir/")
                val targetLine = loc.range.start.line
                val targetChar = loc.range.start.character
                Log.d("GoStudio", "gotoDefinition: target=$targetFile, line=$targetLine, char=$targetChar")

                if (targetFile != fileName && _files.value.containsKey(targetFile)) {
                    switchFile(targetFile)
                }

                gotoDefinitionCallback?.invoke(targetFile, targetLine, targetChar)
            } else {
                Log.d("GoStudio", "gotoDefinition: no locations found")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(getApplication(), "未找到定义", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 查找下一个（通过编辑器回调）
    private var findNextCallback: ((query: String, caseSensitive: Boolean) -> Unit)? = null
    fun setFindNextCallback(callback: (String, Boolean) -> Unit) { findNextCallback = callback }
    fun findNext(query: String, caseSensitive: Boolean) { findNextCallback?.invoke(query, caseSensitive) }

    private var findPrevCallback: ((query: String, caseSensitive: Boolean) -> Unit)? = null
    fun setFindPrevCallback(callback: (String, Boolean) -> Unit) { findPrevCallback = callback }
    fun findPrev(query: String, caseSensitive: Boolean) { findPrevCallback?.invoke(query, caseSensitive) }

    // 选中词提示
    private val _selectedWord = MutableStateFlow("")
    val selectedWord: StateFlow<String> = _selectedWord.asStateFlow()
    fun updateSelectedWord(word: String) { _selectedWord.value = word }

    private var currentMatchCallback: (() -> Int)? = null
    fun setCurrentMatchCallback(callback: () -> Int) { currentMatchCallback = callback }
    fun getCurrentMatchIndex(): Int = currentMatchCallback?.invoke() ?: -1

    // 替换当前选中
    private var replaceCurrentCallback: ((query: String, replacement: String, caseSensitive: Boolean) -> Unit)? = null
    fun setReplaceCurrentCallback(callback: (String, String, Boolean) -> Unit) { replaceCurrentCallback = callback }
    fun replaceCurrent(query: String, replacement: String, caseSensitive: Boolean) { replaceCurrentCallback?.invoke(query, replacement, caseSensitive) }

    // 全部替换
    private var replaceAllCallback: ((query: String, replacement: String, caseSensitive: Boolean) -> Unit)? = null
    fun setReplaceAllCallback(callback: (String, String, Boolean) -> Unit) { replaceAllCallback = callback }
    fun replaceAll(query: String, replacement: String, caseSensitive: Boolean) { replaceAllCallback?.invoke(query, replacement, caseSensitive) }

    // 跳转行
    private var gotoLineCallback: ((line: Int) -> Unit)? = null
    fun setGotoLineCallback(callback: (Int) -> Unit) { gotoLineCallback = callback }
    fun gotoLine(line: Int) { gotoLineCallback?.invoke(line) }

    // 长按菜单：显示跳转定义
    private val _showContextMenu = MutableStateFlow(false)
    private val _contextMenuPosition = MutableStateFlow(android.graphics.PointF(0f, 0f))
    val showContextMenu: StateFlow<Boolean> = _showContextMenu.asStateFlow()
    val contextMenuPosition: StateFlow<android.graphics.PointF> = _contextMenuPosition.asStateFlow()
    fun showContextMenu(x: Float, y: Float) { _contextMenuPosition.value = android.graphics.PointF(x, y); _showContextMenu.value = true }
    fun dismissContextMenu() { _showContextMenu.value = false }

    // 导航历史（上一步/下一步）
    private val navHistory = java.util.ArrayDeque<Int>()
    private val navFuture = java.util.ArrayDeque<Int>()
    fun pushNavPosition(pos: Int) { navHistory.addLast(pos); navFuture.clear() }
    fun canGoBack(): Boolean = navHistory.isNotEmpty()
    fun canGoForward(): Boolean = navFuture.isNotEmpty()
    fun goBack(): Int? = if (navHistory.isNotEmpty()) { navFuture.addLast(navHistory.removeLast()); navFuture.last } else null
    fun goForward(): Int? = if (navFuture.isNotEmpty()) { val pos = navFuture.removeLast(); navHistory.addLast(pos); pos } else null

    // 导航跳转回调（由 UI 层设置）
    private var goBackCallback: (() -> Unit)? = null
    private var goForwardCallback: (() -> Unit)? = null
    fun setNavCallbacks(onBack: () -> Unit, onForward: () -> Unit) {
        goBackCallback = onBack
        goForwardCallback = onForward
    }
    fun triggerGoBack() { goBackCallback?.invoke() }
    fun triggerGoForward() { goForwardCallback?.invoke() }

    // 创建项目对话框触发
    private val _showCreateProjectDialog = MutableStateFlow(false)
    val showCreateProjectDialog: StateFlow<Boolean> = _showCreateProjectDialog.asStateFlow()
    fun showCreateProjectDialog() { _showCreateProjectDialog.value = true }
    fun dismissCreateProjectDialog() { _showCreateProjectDialog.value = false }

    private val _installing = MutableStateFlow(false)
    val installing: StateFlow<Boolean> = _installing.asStateFlow()

    private val _installProgress = MutableStateFlow("")
    val installProgress: StateFlow<String> = _installProgress.asStateFlow()

    // 多文件
    private val _files = MutableStateFlow<Map<String, String>>(
        linkedMapOf("main.go" to DEFAULT_CODE, "go.mod" to DEFAULT_GO_MOD)
    )
    val files: StateFlow<Map<String, String>> = _files.asStateFlow()

    private val _currentFileName = MutableStateFlow("main.go")
    val currentFileName: StateFlow<String> = _currentFileName.asStateFlow()
    private val _unsavedFiles = MutableStateFlow(setOf<String>())
    val unsavedFiles: StateFlow<Set<String>> = _unsavedFiles.asStateFlow()
    val hasUnsavedChanges: StateFlow<Boolean> = _unsavedFiles.map { it.isNotEmpty() }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun getFileNames(): List<String> = _files.value.keys.toList()
    val currentFileCode: StateFlow<String> = MutableStateFlow(DEFAULT_CODE)

    private var currentProcess: TermuxShellExecutor.ShellProcess? = null

    // ===== 项目管理 =====
    private val projectsDir: String
        get() = "${com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH}/gostudio_projects"

    private val _projectName = MutableStateFlow("")
    val projectName: StateFlow<String> = _projectName.asStateFlow()

    // 导航状态：HOME=项目主页, EDITOR=编辑器
    enum class Screen { HOME, EDITOR }
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // 暗色模式
    private val _darkMode = MutableStateFlow(true) // 默认暗色
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()
    fun toggleDarkMode() { _darkMode.value = !_darkMode.value }

    // 抽屉状态（由 Compose 层同步）
    private val _drawerOpenCount = MutableStateFlow(0)
    fun onDrawerOpened() { _drawerOpenCount.value = _drawerOpenCount.value + 1 }
    fun onDrawerClosed() { if (_drawerOpenCount.value > 0) _drawerOpenCount.value -= 1 }
    val hasOpenDrawer: Boolean get() = _drawerOpenCount.value > 0

    // 教程详情页状态（由 Compose 层同步）
    var tutorialDetailOpen = false
    private val _closeTutorialDetailRequest = MutableStateFlow(0)
    val closeTutorialDetailRequest: StateFlow<Int> = _closeTutorialDetailRequest.asStateFlow()
    fun requestCloseTutorialDetail() {
        _closeTutorialDetailRequest.value = _closeTutorialDetailRequest.value + 1
    }
    fun consumeCloseTutorialDetail() {
        _closeTutorialDetailRequest.value = 0
    }

    // 关闭一个抽屉（优先级：教程详情 → 教程抽屉 → 诊断抽屉 → 文件抽屉）
    private val _closeDrawerRequest = MutableStateFlow(0)
    val closeDrawerRequest: StateFlow<Int> = _closeDrawerRequest.asStateFlow()
    fun closeOneDrawer(): Boolean {
        if (tutorialDetailOpen) {
            // 教程详情页打开，请求回到目录
            requestCloseTutorialDetail()
            return true
        }
        if (_drawerOpenCount.value > 0) {
            _closeDrawerRequest.value = _closeDrawerRequest.value + 1
            return true
        }
        return false
    }
    fun consumeCloseRequest() {
        _closeDrawerRequest.value = 0
    }

    fun navigateToHome() { _currentScreen.value = Screen.HOME }
    fun navigateToEditor() { _currentScreen.value = Screen.EDITOR }

    // 双击返回主页
    private var _lastBackTime = 0L
    fun shouldExitToHome(): Boolean {
        val now = System.currentTimeMillis()
        if (now - _lastBackTime < 2000) {
            _lastBackTime = 0L
            return true
        }
        _lastBackTime = now
        return false
    }

    private val _projects = MutableStateFlow<List<String>>(emptyList())
    val projects: StateFlow<List<String>> = _projects.asStateFlow()

    init {
        checkEnvironment()
        // 如果 gopls 已安装，自动启动 LSP
        if (_goplsInstalled.value) {
            startLsp()
        }
        // 加载项目列表
        refreshProjects()
    }

    private fun checkEnvironment() {
        _goInstalled.value = shellExecutor.isGoInstalled()
        _goplsInstalled.value = shellExecutor.isGoplsInstalled()
    }

    // ===== 项目管理方法 =====

    /** 刷新项目列表 */
    fun refreshProjects() {
        try {
            val dir = java.io.File(projectsDir)
            if (dir.exists() && dir.isDirectory) {
                _projects.value = dir.listFiles()
                    ?.filter { it.isDirectory && java.io.File(it, "go.mod").exists() }
                    ?.map { it.name }
                    ?.sorted()
                    ?: emptyList()
            } else {
                _projects.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "刷新项目列表失败", e)
            _projects.value = emptyList()
        }
    }

    /** 获取项目模板列表 */
    fun getProjectTemplates(): List<Pair<String, String>> = listOf(
        "Hello World" to "基础 main.go + go.mod",
        "HTTP 服务器" to "net/http 服务器 + go.mod",
        "CLI 工具" to "os.Args 解析 + go.mod",
        "Web API" to "标准库 HTTP API + go.mod"
    )

    /** 获取模板内容 */
    fun getProjectTemplateContent(templateName: String): Map<String, String> {
        return when (templateName) {
            "Hello World" -> mapOf(
                "main.go" to """package main

import "fmt"

func main() {
    fmt.Println("Hello, GoStudio!")
    fmt.Println("Go is running on your phone!")
}""",
                "go.mod" to "module placeholder\n\ngo 1.21"
            )
            "HTTP 服务器" -> mapOf(
                "main.go" to """package main

import (
    "fmt"
    "net/http"
)

func handler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprintf(w, "Hello from GoStudio HTTP Server!")
}

func main() {
    http.HandleFunc("/", handler)
    fmt.Println("Server starting on :8080...")
    fmt.Println("Open http://localhost:8080 in your browser")
    err := http.ListenAndServe(":8080", nil)
    if err != nil {
        fmt.Println("Server error:", err)
    }
}""",
                "go.mod" to "module placeholder\n\ngo 1.21"
            )
            "CLI 工具" -> mapOf(
                "main.go" to """package main

import (
    "flag"
    "fmt"
    "os"
)

func main() {
    // 定义命令行参数
    name := flag.String("name", "World", "你的名字")
    count := flag.Int("count", 1, "问候次数")
    help := flag.Bool("help", false, "显示帮助信息")

    flag.Parse()

    if *help {
        fmt.Println("用法: cli-tool [选项]")
        fmt.Println("选项:")
        flag.PrintDefaults()
        os.Exit(0)
    }

    for i := 0; i < *count; i++ {
        fmt.Printf("Hello, %s! (%d/%d)\n", *name, i+1, *count)
    }
}""",
                "go.mod" to "module placeholder\n\ngo 1.21"
            )
            "Web API" -> mapOf(
                "main.go" to """package main

import (
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "strconv"
    "sync"
)

// Todo 待办事项
type Todo struct {
    ID    int
    Title string
    Done  bool
}

var (
    todos  = []Todo{}
    nextID = 1
    mu     sync.Mutex
)

func main() {
    // 路由
    http.HandleFunc("/api/todos", handleTodos)
    http.HandleFunc("/api/todos/", handleTodoByID)

    fmt.Println("Web API 启动在 :8080")
    fmt.Println("GET  /api/todos     - 获取所有待办")
    fmt.Println("POST /api/todos     - 创建待办 (JSON body)")
    fmt.Println("DELETE /api/todos/1 - 删除待办")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func handleTodos(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    switch r.Method {
    case http.MethodGet:
        json.NewEncoder(w).Encode(todos)
    case http.MethodPost:
        var todo Todo
        if err := json.NewDecoder(r.Body).Decode(&todo); err != nil {
            http.Error(w, "无效的 JSON", http.StatusBadRequest)
            return
        }
        mu.Lock()
        todo.ID = nextID
        nextID++
        todos = append(todos, todo)
        mu.Unlock()
        w.WriteHeader(http.StatusCreated)
        json.NewEncoder(w).Encode(todo)
    default:
        http.Error(w, "方法不允许", http.StatusMethodNotAllowed)
    }
}

func handleTodoByID(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    idStr := r.URL.Path[len("/api/todos/"):]
    id, err := strconv.Atoi(idStr)
    if err != nil {
        http.Error(w, "无效的 ID", http.StatusBadRequest)
        return
    }
    if r.Method == http.MethodDelete {
        mu.Lock()
        defer mu.Unlock()
        for i, t := range todos {
            if t.ID == id {
                todos = append(todos[:i], todos[i+1:]...)
                w.WriteHeader(http.StatusNoContent)
                return
            }
        }
        http.Error(w, "未找到", http.StatusNotFound)
    } else {
        http.Error(w, "方法不允许", http.StatusMethodNotAllowed)
    }
}""",
                "go.mod" to "module placeholder\n\ngo 1.21"
            )
            else -> mapOf(
                "main.go" to DEFAULT_CODE,
                "go.mod" to DEFAULT_GO_MOD
            )
        }
    }

    /** 创建项目 */
    fun createProject(name: String, templateName: String): Boolean {
        if (name.isBlank()) return false
        // 项目名只允许字母、数字、下划线、连字符
        if (!name.matches(Regex("^[a-zA-Z0-9_-]+$"))) return false
        val projectDir = java.io.File("$projectsDir/$name")
        if (projectDir.exists()) return false

        projectDir.mkdirs()
        val templateFiles = getProjectTemplateContent(templateName)
        for ((fileName, content) in templateFiles) {
            val fileContent = if (fileName == "go.mod") {
                "module $name\n\ngo 1.21"
            } else {
                content
            }
            java.io.File(projectDir, fileName).writeText(fileContent)
        }

        refreshProjects()
        openProject(name)
        return true
    }

    /** 保存当前项目所有文件到磁盘 */
    private fun saveCurrentProjectFiles() {
        val name = _projectName.value
        if (name.isEmpty()) return
        val projectDir = java.io.File("$projectsDir/$name")
        if (!projectDir.exists()) return
        for ((fileName, content) in _files.value) {
            val file = java.io.File(projectDir, fileName)
            file.parentFile?.mkdirs()
            file.writeText(content)
        }
        _unsavedFiles.value = emptySet()
    }

    /** 打开项目 */
    fun openProject(name: String): Boolean {
        // 先保存当前项目的未保存文件
        saveCurrentProjectFiles()

        val projectDir = java.io.File("$projectsDir/$name")
        if (!projectDir.exists() || !java.io.File(projectDir, "go.mod").exists()) return false

        // 加载项目文件到编辑器
        val loadedFiles = linkedMapOf<String, String>()
        projectDir.walkTopDown().forEach { file ->
            if (file.isFile && (file.name.endsWith(".go") || file.name.endsWith(".mod") || file.name.endsWith(".sum"))) {
                loadedFiles[file.relativeTo(projectDir).path] = file.readText()
            }
        }

        if (loadedFiles.isEmpty()) return false

        _files.value = loadedFiles
        _currentFileName.value = "main.go"
        _projectName.value = name
        _showCompletions.value = false
        _diagnostics.value = emptyMap()

        // 切换项目需要重启 LSP（rootUri 变了）
        if (lspService != null) {
            lspService?.shutdown()
            lspService = null
            _lspRunning.value = false
            startLsp()
        }

        appendOutput(OutputLine("✅ 已打开项目: $name", OutputLine.Type.INFO))
        _currentScreen.value = Screen.EDITOR

        // 记住上次打开的项目
        try {
            val prefsFile = java.io.File(com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH, ".gostudio_last_project")
            prefsFile.writeText(name)
        } catch (_: Exception) {}

        return true
    }

    /** 删除项目 */
    fun deleteProject(name: String): Boolean {
        val projectDir = java.io.File("$projectsDir/$name")
        if (!projectDir.exists()) return false

        projectDir.deleteRecursively()
        refreshProjects()

        // 如果删除的是当前项目，重置
        if (_projectName.value == name) {
            _projectName.value = ""
            _files.value = linkedMapOf("main.go" to DEFAULT_CODE, "go.mod" to DEFAULT_GO_MOD)
            _currentFileName.value = "main.go"
        }

        appendOutput(OutputLine("🗑️ 已删除项目: $name", OutputLine.Type.INFO))
        return true
    }

    /** 保存当前项目到磁盘 */
    fun saveCurrentProject() {
        val name = _projectName.value
        if (name.isBlank()) return
        val projectDir = java.io.File("$projectsDir/$name")
        if (!projectDir.exists()) projectDir.mkdirs()

        for ((fileName, content) in _files.value) {
            // 如果文件名包含路径分隔符，先创建子目录
            val file = java.io.File(projectDir, fileName)
            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
            file.writeText(content)
        }
        _unsavedFiles.value = emptySet()
    }

    /** 加载上次的项目 */
    private fun loadLastProject() {
        try {
            // 读取上次打开的项目名
            val prefsFile = java.io.File(com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH, ".gostudio_last_project")
            if (!prefsFile.exists()) return
            val lastName = prefsFile.readText().trim()
            if (lastName.isEmpty()) return

            // 检查项目是否存在
            val projectDir = java.io.File("$projectsDir/$lastName")
            if (!projectDir.exists() || !java.io.File(projectDir, "go.mod").exists()) return

            // 直接用 openProject 打开
            openProject(lastName)
        } catch (e: Exception) {
            Log.e(TAG, "加载上次项目失败", e)
        }
    }

    fun refreshEnvironment() { checkEnvironment() }
    fun skipGoCheck() { _goInstalled.value = true }
    fun initializeEnvironment() { /* Termux 环境已就绪 */ }

    /**
     * 安装 Go（通过 pkg）
     */
    fun installGo() {
        viewModelScope.launch {
            _installing.value = true
            appendOutput(OutputLine("── 安装 Go ──", OutputLine.Type.INFO))

            currentProcess = shellExecutor.createStreamingProcess(
                command = "pkg install -y golang gopls 2>&1",
                onOutput = { for (l in it.lines()) appendOutput(OutputLine(l, OutputLine.Type.STDOUT)) },
                onComplete = { code ->
                    _goInstalled.value = shellExecutor.isGoInstalled()
                    if (code == 0 && _goInstalled.value) {
                        appendOutput(OutputLine("✅ Go 安装完成", OutputLine.Type.INFO))
                        // 自动安装 gopls
                        if (!_goplsInstalled.value) {
                            installGopls()
                        }
                    } else {
                        appendOutput(OutputLine("❌ Go 安装失败 (退出码: $code)", OutputLine.Type.ERROR))
                    }
                    _installing.value = false
                    currentProcess = null
                }
            )
        }
    }

    fun installGoToolchain() { installGo() }

    // ===== gopls =====

    fun installGopls() {
        viewModelScope.launch {
            _installingGopls.value = true
            appendOutput(OutputLine("── 安装 gopls ──", OutputLine.Type.INFO))

            currentProcess = shellExecutor.createStreamingProcess(
                command = "pkg install -y gopls 2>&1",
                onOutput = { for (l in it.lines()) appendOutput(OutputLine(l, OutputLine.Type.STDOUT)) },
                onComplete = { code ->
                    _goplsInstalled.value = shellExecutor.isGoplsInstalled()
                    if (code == 0 && _goplsInstalled.value) {
                        appendOutput(OutputLine("✅ gopls 安装成功", OutputLine.Type.INFO))
                        // 自动启动 LSP
                        if (!_lspRunning.value) {
                            startLsp()
                        }
                    } else {
                        appendOutput(OutputLine("❌ gopls 安装失败 (退出码: $code)", OutputLine.Type.ERROR))
                    }
                    _installingGopls.value = false
                    currentProcess = null
                }
            )
        }
    }

    // ===== LSP =====

    private var completionJob: kotlinx.coroutines.Job? = null

    fun startLsp() {
        viewModelScope.launch {
            appendOutput(OutputLine("── 启动 gopls ──", OutputLine.Type.INFO))
            try {
                val service = com.termux.app.gostudio.lsp.LspService(goplsPath)
                service.onDiagnostics = { uri, diags ->
                    // uri 格式: file:///data/data/com.jmwl.gostudio/files/home/gostudio_projects/项目名/file.go
                    val prefix = "file://$runDir/"
                    val fileName = if (uri.startsWith(prefix)) uri.removePrefix(prefix) else uri.substringAfterLast("/")
                    _diagnostics.value = _diagnostics.value.toMutableMap().apply { put(fileName, diags) }
                }

                if (!service.start()) {
                    appendOutput(OutputLine("❌ gopls 启动失败", OutputLine.Type.ERROR))
                    return@launch
                }

                // 初始化
                val rootUri = "file://$runDir"
                service.initialize(rootUri) { success ->
                    if (success) {
                        lspService = service
                        _lspRunning.value = true
                        appendOutput(OutputLine("✅ gopls 已启动", OutputLine.Type.INFO))
                        // 延迟注册所有已加载的文件到 gopls，避免竞态
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(500)
                            _files.value.forEach { (name, content) ->
                                openFileInLsp(name, content)
                                kotlinx.coroutines.delay(100) // 每个文件间隔 100ms
                            }
                        }
                    } else {
                        appendOutput(OutputLine("❌ gopls 初始化失败", OutputLine.Type.ERROR))
                        service.shutdown()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "LSP 启动异常", e)
                appendOutput(OutputLine("❌ LSP 异常: ${e.message}", OutputLine.Type.ERROR))
            }
        }
    }

    fun stopLsp() {
        try { lspService?.shutdown() } catch (_: Exception) {}
        lspService = null
        _lspRunning.value = false
        _diagnostics.value = emptyMap()
        _completions.value = emptyList()
        _showCompletions.value = false
        completionJob?.cancel()
    }

    private fun openFileInLsp(fileName: String, content: String) {
        val service = lspService ?: return
        val filePath = "$runDir/$fileName"
        val uri = "file://$filePath"

        // 确保目录和文件存在，gopls 需要磁盘上的文件才能创建 view
        try {
            val dir = java.io.File(runDir)
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(filePath)
            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
            file.writeText(content)
        } catch (e: Exception) {
            Log.e(TAG, "写入文件失败: $filePath", e)
        }

        // 如果文件已经在 gopls 中打开，用 didChange 全量替换，避免 close/reopen 导致状态丢失
        if (openFiles.containsKey(fileName)) {
            service.didChange(uri, content)
        } else {
            service.didOpen(uri, content)
            openFiles[fileName] = uri
        }
    }

    fun requestCompletions(line: Int, column: Int) {
        completionJob?.cancel()
        completionJob = viewModelScope.launch {
            kotlinx.coroutines.delay(200) // 200ms 防抖
            val service = lspService ?: run { Log.w(TAG, "requestCompletions: lspService is null"); return@launch }
            val fileName = _currentFileName.value
            val uri = openFiles[fileName] ?: run { Log.w(TAG, "requestCompletions: no uri for $fileName"); return@launch }

            Log.i(TAG, "requestCompletions: $fileName line=$line col=$column uri=$uri")
            try {
                service.getCompletions(uri, line, column) { items ->
                    Log.i(TAG, "requestCompletions callback: ${items.size} items")
                    _completions.value = items
                    if (items.isNotEmpty()) {
                        _selectedCompletionIndex.value = 0
                        _showCompletions.value = true
                    } else {
                        _showCompletions.value = false
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "补全请求失败", e)
            }
        }
    }

    fun notifyContentChanged(fileName: String, content: String) {
        // 同步写入磁盘，gopls 需要文件在磁盘上
        try {
            val filePath = "$runDir/$fileName"
            val file = java.io.File(filePath)
            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
            file.writeText(content)
        } catch (_: Exception) {}

        val service = lspService ?: return
        val uri = openFiles[fileName] ?: return
        try { service.didChange(uri, content) } catch (e: Exception) { Log.w(TAG, "didChange 失败", e) }
    }

    fun hideCompletions() { _showCompletions.value = false; _completions.value = emptyList() }
    fun moveCompletionSelection(delta: Int) {
        val items = _completions.value
        if (items.isEmpty()) return
        _selectedCompletionIndex.value = (_selectedCompletionIndex.value + delta + items.size) % items.size
    }
    fun selectCompletion(item: CompletionItem): String? {
        _showCompletions.value = false
        // 函数类型（kind == 3 或 kind == 2 方法）自动加括号，光标放在括号内
        val baseText = item.insertText ?: item.label
        val finalText = if (item.kind == 3 || item.kind == 2) {
            "$baseText()"
        } else {
            baseText
        }
        _completions.value = emptyList()
        return finalText
    }
    fun getSelectedCompletionItem(): CompletionItem? = _completions.value.getOrNull(_selectedCompletionIndex.value)

    // ===== 文件操作 =====
    fun updateEditorCode(code: String, markUnsaved: Boolean = true) {
        val n = _currentFileName.value
        val u = _files.value.toMutableMap(); u[n] = code; _files.value = u
        if (markUnsaved) _unsavedFiles.value = _unsavedFiles.value + n
        notifyContentChanged(n, code)
    }
    fun getCurrentFileCode(): String = _files.value[_currentFileName.value] ?: ""
    fun switchFile(name: String) {
        if (_files.value.containsKey(name)) {
            _currentFileName.value = name
            _showCompletions.value = false
            if (lspService != null) openFileInLsp(name, _files.value[name] ?: "")
        }
    }
    fun createFile(name: String): Boolean {
        if (_files.value.containsKey(name)) return false
        val u = _files.value.toMutableMap(); u[name] = ""; _files.value = u; _currentFileName.value = name
        if (lspService != null) openFileInLsp(name, "")
        return true
    }
    fun deleteFile(name: String): Boolean {
        if (name == "main.go" || !_files.value.containsKey(name)) return false
        val u = _files.value.toMutableMap(); u.remove(name); _files.value = u
        if (_currentFileName.value == name) _currentFileName.value = "main.go"
        return true
    }

    /** 复制文件 */
    fun copyFile(oldName: String, newName: String): Boolean {
        if (!_files.value.containsKey(oldName)) return false
        if (_files.value.containsKey(newName)) return false
        if (newName.isBlank()) return false
        val content = _files.value[oldName] ?: return false
        val u = _files.value.toMutableMap()
        u[newName] = content
        _files.value = u
        if (lspService != null) openFileInLsp(newName, content)
        appendOutput(OutputLine("📋 已复制文件: $oldName → $newName", OutputLine.Type.INFO))
        return true
    }

    /** 重命名文件 */
    fun renameFile(oldName: String, newName: String): Boolean {
        if (!_files.value.containsKey(oldName)) return false
        if (newName.isBlank()) return false
        if (newName != oldName && _files.value.containsKey(newName)) return false
        val content = _files.value[oldName] ?: return false
        val u = _files.value.toMutableMap()
        u.remove(oldName)
        u[newName] = content
        _files.value = u
        if (_currentFileName.value == oldName) {
            _currentFileName.value = newName
            if (lspService != null) openFileInLsp(newName, content)
        }
        appendOutput(OutputLine("✏️ 已重命名文件: $oldName → $newName", OutputLine.Type.INFO))
        return true
    }

    /** 重命名项目（磁盘目录同步重命名） */
    fun renameProject(oldName: String, newName: String): Boolean {
        if (oldName.isBlank() || newName.isBlank()) return false
        val oldDir = File("$projectsDir/$oldName")
        val newDir = File("$projectsDir/$newName")
        if (!oldDir.exists()) return false
        if (newDir.exists()) return false
        if (!oldDir.renameTo(newDir)) return false

        // 更新当前项目名
        if (_projectName.value == oldName) {
            _projectName.value = newName
        }
        refreshProjects()
        appendOutput(OutputLine("✏️ 已重命名项目: $oldName → $newName", OutputLine.Type.INFO))
        return true
    }

    /**
     * 创建文件夹
     * 在项目目录下创建目录
     */
    fun createFolder(name: String) {
        viewModelScope.launch {
            try {
                val dir = java.io.File("$runDir/$name")
                if (!dir.exists()) dir.mkdirs()
                // 在文件夹下创建一个占位文件
                val placeholder = java.io.File(dir, "$name.go")
                if (!placeholder.exists()) {
                    placeholder.writeText("package $name\n")
                    val filePath = "$name/$name.go"
                    if (!_files.value.containsKey(filePath)) {
                        val u = _files.value.toMutableMap()
                        u[filePath] = placeholder.readText()
                        _files.value = u
                        // 通知 gopls 新文件
                        if (lspService != null) openFileInLsp(filePath, placeholder.readText())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "创建文件夹失败", e)
            }
        }
    }

    /**
     * 从剪贴板粘贴文本到光标位置
     * 需要在 UI 层传入当前光标位置，返回插入后的完整代码
     */
    fun pasteFromClipboard(code: String, cursorPosition: Int): Pair<String, Int> {
        val ctx = getApplication<Application>()
        val clipboard = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
        val clip = clipboard.primaryClip ?: return code to cursorPosition
        if (clip.itemCount <= 0) return code to cursorPosition
        val pasteText = clip.getItemAt(0).text?.toString() ?: return code to cursorPosition
        val newCode = code.substring(0, cursorPosition) + pasteText + code.substring(cursorPosition)
        return newCode to (cursorPosition + pasteText.length)
    }

    /**
     * 复制当前行到下一行
     */
    fun duplicateCurrentLine(code: String, cursorPosition: Int): Pair<String, Int> {
        val text = code
        if (text.isEmpty()) return code to cursorPosition
        // 找到当前行的起止位置
        var lineStart = text.lastIndexOf('\n', cursorPosition - 1).let { if (it == -1) 0 else it + 1 }
        var lineEnd = text.indexOf('\n', cursorPosition)
        if (lineEnd == -1) lineEnd = text.length
        val currentLine = text.substring(lineStart, lineEnd)
        // 在当前行末尾插入换行 + 复制的行
        val newCode = text.substring(0, lineEnd) + "\n" + currentLine + text.substring(lineEnd)
        val newCursor = lineEnd + 1 + currentLine.length
        return newCode to newCursor
    }

    /**
     * 删除光标所在行
     */
    fun deleteCurrentLine(code: String, cursorPosition: Int): Pair<String, Int> {
        val text = code
        if (text.isEmpty()) return code to cursorPosition
        var lineStart = text.lastIndexOf('\n', cursorPosition - 1).let { if (it == -1) 0 else it + 1 }
        var lineEnd = text.indexOf('\n', cursorPosition)
        val hasNewlineAfter = lineEnd >= 0
        if (lineEnd == -1) lineEnd = text.length
        // 删除整行（包括行尾换行符）
        val deleteEnd = if (hasNewlineAfter) lineEnd + 1 else lineEnd
        val newCode = text.substring(0, lineStart) + text.substring(deleteEnd)
        val newCursor = lineStart.coerceAtMost(newCode.length)
        return newCode to newCursor
    }
    fun loadExample(exampleFiles: Map<String, String>) {
        _files.value = linkedMapOf<String, String>().apply {
            if (!exampleFiles.containsKey("go.mod")) put("go.mod", DEFAULT_GO_MOD)
            putAll(exampleFiles)
        }
        _currentFileName.value = "main.go"; _showCompletions.value = false
    }

    // ===== 代码格式化 =====

    /**
     * 格式化代码（go fmt）
     */
    fun formatCode() {
        if (_isRunning.value) return
        viewModelScope.launch {
            appendOutput(OutputLine("── 代码格式化 ──", OutputLine.Type.INFO))

            // 先保存文件到项目目录
            for ((name, content) in _files.value) {
                val file = File("$runDir/$name")
                file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                file.writeText(content)
            }

            val (output, error) = shellExecutor.executeAndWait(
                "export GOPROXY=https://goproxy.cn,direct && cd '$runDir' && go fmt ./... 2>&1",
                timeoutMs = 30_000L
            )
            if (output.isNotBlank()) {
                appendOutput(OutputLine(output.trim(), OutputLine.Type.STDOUT))
            }
            if (error.isNotBlank()) {
                appendOutput(OutputLine(error, OutputLine.Type.STDERR))
            }

            // 从磁盘重新读取格式化后的文件（Tab → 4空格）
            for (fileName in _files.value.keys) {
                val file = File("$runDir/$fileName")
                if (file.exists()) {
                    val updated = _files.value.toMutableMap()
                    updated[fileName] = file.readText().replace("\t", "  ")
                    _files.value = updated
                }
            }

            appendOutput(OutputLine("✅ 格式化完成", OutputLine.Type.INFO))
        }
    }

    /**
     * 运行 Go 代码
     */
    fun runCode() {
        val allFiles = _files.value
        if (_isRunning.value || (allFiles["main.go"]?.isBlank() != false)) return
        _outputExpanded.value = true // 运行时自动打开日志

        viewModelScope.launch {
            _isRunning.value = true
            appendOutput(OutputLine("--- 运行中 ---", OutputLine.Type.INFO))
            try {
                // 运行前自动保存当前项目到项目目录
                saveCurrentProject()

                for ((name, content) in allFiles) {
                    val file = File("$runDir/$name")
                    file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                    file.writeText(content)
                }

                appendOutput(OutputLine("正在编译...", OutputLine.Type.INFO))
                val goCmd = "export GOPROXY=https://goproxy.cn,direct && cd '$runDir' && go run . 2>&1"

                currentProcess = shellExecutor.createStreamingProcess(
                    command = goCmd,
                    onOutput = { for (l in it.lines()) appendOutput(OutputLine(l, OutputLine.Type.STDOUT)) },
                    onComplete = { code ->
                        appendOutput(OutputLine(
                            if (code == 0) "--- 运行完成 (退出码: $code) ---" else "--- 运行失败 (退出码: $code) ---",
                            if (code == 0) OutputLine.Type.INFO else OutputLine.Type.ERROR
                        ))
                        _isRunning.value = false; currentProcess = null
                    }
                )
            } catch (e: Exception) {
                appendOutput(OutputLine("运行异常: ${e.message}", OutputLine.Type.ERROR))
                currentProcess = null; _isRunning.value = false
            }
        }
    }

    fun goModTidy() {
        if (_isRunning.value) return
        viewModelScope.launch {
            appendOutput(OutputLine("--- 执行 go mod tidy ---", OutputLine.Type.INFO))
            try {
                saveCurrentProject()
                for ((name, content) in _files.value) {
                    val file = File("$runDir/$name")
                    file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                    file.writeText(content)
                }
                val (output, error) = shellExecutor.executeAndWait(
                    "export GOPROXY=https://goproxy.cn,direct && cd '$runDir' && go mod tidy 2>&1",
                    timeoutMs = 60_000L
                )
                appendOutput(OutputLine(output, OutputLine.Type.STDOUT))
                if (error.isNotBlank()) appendOutput(OutputLine(error, OutputLine.Type.STDERR))
                appendOutput(OutputLine("--- go mod tidy 完成 ---", OutputLine.Type.INFO))

                val modFile = File("$runDir/go.mod")
                if (modFile.exists()) { val u = _files.value.toMutableMap(); u["go.mod"] = modFile.readText(); _files.value = u }
                val sumFile = File("$runDir/go.sum")
                if (sumFile.exists()) { val u = _files.value.toMutableMap(); u["go.sum"] = sumFile.readText(); _files.value = u }
            } catch (e: Exception) {
                appendOutput(OutputLine("go mod tidy 异常: ${e.message}", OutputLine.Type.ERROR))
            }
        }
    }

    fun stopRunning() {
        currentProcess?.let {
            try { it.stop(); appendOutput(OutputLine("--- 已停止 ---", OutputLine.Type.INFO)) }
            catch (e: Exception) { Log.e(TAG, "停止失败", e) }
            currentProcess = null
        }
        _isRunning.value = false
    }

    fun clearOutput() { _outputLines.value = emptyList() }
    fun toggleOutputPanel() { _outputExpanded.value = !_outputExpanded.value }

    fun testTermux() {
        viewModelScope.launch {
            appendOutput(OutputLine("── 测试 Termux 环境 ──", OutputLine.Type.INFO))
            val (output, error) = shellExecutor.executeAndWait("go version", timeoutMs = 10_000)
            if (error.isNotEmpty()) appendOutput(OutputLine("❌ $error", OutputLine.Type.ERROR))
            else if (output.isNotEmpty()) appendOutput(OutputLine("✅ $output", OutputLine.Type.INFO))
            else appendOutput(OutputLine("⚠️ Go 未安装，请在终端执行 pkg install golang", OutputLine.Type.ERROR))
        }
    }

    fun openTerminal() {
        // 启动 TermuxActivity
        val ctx = getApplication<Application>()
        ctx.startActivity(android.content.Intent(ctx, com.termux.app.TermuxActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun captureLogcat() {}
    fun clearLog() { _outputLines.value = emptyList() }

    override fun onCleared() { super.onCleared(); stopLsp(); stopRunning() }

    private fun appendOutput(line: OutputLine) { _outputLines.value = _outputLines.value + line }

    data class OutputLine(val text: String, val type: Type) {
        enum class Type { STDOUT, STDERR, ERROR, INFO }
    }
}
