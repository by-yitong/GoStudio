package com.xc.code

import android.app.Application
import android.content.Intent
import android.os.Build
import com.xc.code.activity.crash.crash_activity
import com.xc.code.core.logging.logger_manager
import com.xc.code.editor.theme.editor_theme_manager
import com.xc.code.service.keep_alive_service
import com.xc.code.toolchain.toolchain_runtime_provider
import com.xc.code.ui.theme.theme_manager
import com.xc.code.utils.app_lifecycle_observer
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.internal.oniguruma.Oniguruma
import java.io.File
class xc_application : Application() {

    companion object {
        lateinit var instance: xc_application
    }

    private var textmate_initialized = false

    var keep_alive_service_: keep_alive_service? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        theme_manager.init(this)
        logger_manager.init(this)
        logger_manager.is_debug = false
        logger_manager.enable_file_log = true
        logger_manager.errors_only = true

        val create_dirs = listOf(
            File(android.os.Environment.getExternalStorageDirectory(), "XCodeProjects"),
            File(filesDir, "home"),
            File(filesDir, "home/xcode/proot-tmps"),
            File(filesDir, "home/xcode/ubuntu-base")
        )
        for (dir in create_dirs) {
            if (!dir.exists()) dir.mkdirs()
        }

        toolchain_runtime_provider.init(
            context = this,
            xcode_dir = File(filesDir, "home/xcode"),
            home_dir = File(filesDir, "home"),
            ubuntu_base_dir = File(filesDir, "home/xcode/ubuntu-base"),
            proot_tmp_dir = File(filesDir, "home/xcode/proot-tmps")
        )

        init_textmate()
        start_keep_alive_service()
        app_lifecycle_observer.init(this)
        setup_uncaught_exception_handler()
    }

    private fun init_textmate() {
        if (textmate_initialized) return

        try {
            FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(assets))
            configure_textmate_regex_engine()

            editor_theme_manager.init(this)

            ThemeRegistry.getInstance().setTheme("xcode_user")
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")

            textmate_initialized = true
        } catch (e: Exception) {
            logger_manager.e("xc_application", "Failed to init TextMate: ${e.message}", e)
        }
    }

    private fun configure_textmate_regex_engine() {
        runCatching {
            Oniguruma().setUseNativeOniguruma(true)
        }.onFailure { error ->
            logger_manager.e("xc_application", "TextMate native Oniguruma unavailable: ${error.message}", error)
        }
    }

    fun set_textmate_theme(is_dark: Boolean) {
        if (!textmate_initialized) return

        try {
            editor_theme_manager.set_current_theme(this)
        } catch (e: Exception) {
            logger_manager.e("xc_application", "Failed to set TextMate theme: ${e.message}")
        }
    }

    fun get_language_scope_name(file_name: String): String {
        return when {
            file_name.endsWith(".c", ignoreCase = true) -> "source.c"
            file_name.endsWith(".cpp", ignoreCase = true) -> "source.cpp"
            file_name.endsWith(".cc", ignoreCase = true) -> "source.cpp"
            file_name.endsWith(".cxx", ignoreCase = true) -> "source.cpp"
            file_name.endsWith(".h", ignoreCase = true) -> "source.cpp"
            file_name.endsWith(".hpp", ignoreCase = true) -> "source.cpp"
            file_name.endsWith(".json", ignoreCase = true) -> "source.json"
            file_name.endsWith(".cmake", ignoreCase = true) -> "source.cmake"
            file_name.equals("CMakeLists.txt", ignoreCase = true) -> "source.cmake"
            else -> "source.cpp"
        }
    }

    fun create_textmate_language(file_name: String): TextMateLanguage? {
        if (!textmate_initialized) return null

        val scope_name = get_language_scope_name(file_name)
        return try {
            TextMateLanguage.create(scope_name, false)
        } catch (e: Exception) {
            logger_manager.e("xc_application", "Failed to create TextMate language $scope_name: ${e.message}")
            null
        }
    }

    private fun setup_uncaught_exception_handler() {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            handle_crash(throwable)
        }
    }

    private fun handle_crash(throwable: Throwable) {
        try {
            val log = throwable.javaClass.simpleName + ": " + (throwable.message ?: "Unknown error")
            val stack = get_stack_trace_string(throwable)
            val intent = Intent(this, crash_activity::class.java)
            intent.putExtra("crash_log", log)
            intent.putExtra("crash_stack", stack)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            logger_manager.e("xc_application", "Failed to show crash activity: ${e.message}")
        }
    }

    private fun get_stack_trace_string(throwable: Throwable): String {
        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }

    private fun start_keep_alive_service() {
        try {
            val intent = Intent(this, keep_alive_service::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            logger_manager.e("xc_application", "Failed to start service: ${e.message}")
        }
    }
}
