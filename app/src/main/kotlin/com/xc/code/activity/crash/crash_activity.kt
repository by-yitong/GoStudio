package com.xc.code.activity.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.xc.code.ui.toast.app_toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.ui.theme.app_theme_provider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class crash_activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val crash_log = intent.getStringExtra("crash_log") ?: return
        val crash_stack = intent.getStringExtra("crash_stack") ?: return
        
        setContent {
            app_theme_provider {
                crash_screen(
                    crash_log = crash_log,
                    crash_stack = crash_stack,
                    on_restart = {
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                        finish()
                    },
                    on_exit = { finishAffinity() },
                    on_copy = { log, stack ->
                        copy_to_clipboard(log, stack)
                    },
                    on_save = { log, stack ->
                        save_crash_log_to_file(log, stack)
                    }
                )
            }
        }
    }

    private fun copy_to_clipboard(log: String, stack: String) {
        val text = buildString {
            append("XCode Crash Report\n")
            append("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("\nCrash Log\n")
            append(log)
            append("\n\nStack Trace\n")
            append(stack)
        }
        
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Crash Report", text)
        clipboard.setPrimaryClip(clip)
        app_toast.show(this, "已复制到剪贴板", app_toast.LENGTH_SHORT)
    }

    private fun save_crash_log_to_file(log: String, stack: String) {
        try {
            val crash_dir = File(cacheDir, "crash_logs")
            if (!crash_dir.exists()) {
                crash_dir.mkdirs()
            }
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val crash_file = File(crash_dir, "crash_$timestamp.txt")
            
            FileWriter(crash_file).use { writer ->
                writer.write("XCode Crash Report\n")
                writer.write("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                writer.write("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
                writer.write("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                writer.write("App Version: ${packageManager.getPackageInfo(packageName, 0).versionName}\n")
                writer.write("\nCrash Log\n")
                writer.write(log + "\n")
                writer.write("\nStack Trace\n")
                writer.write(stack + "\n")
            }
            
            app_toast.show(this, "已保存到: ${crash_file.absolutePath}", app_toast.LENGTH_LONG)
        } catch (e: Exception) {
            app_toast.show(this, "保存失败: ${e.message}", app_toast.LENGTH_SHORT)
        }
    }
}

@Composable
fun crash_screen(
    crash_log: String,
    crash_stack: String,
    on_restart: () -> Unit,
    on_exit: () -> Unit,
    on_copy: (String, String) -> Unit,
    on_save: (String, String) -> Unit
) {
    val log_lines = remember(crash_log, crash_stack) {
        val lines = mutableListOf<String>()
        lines.add("Crash Log")
        lines.addAll(crash_log.split("\n"))
        lines.add("")
        lines.add("Stack Trace")
        lines.addAll(crash_stack.split("\n"))
        lines
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.2f))
        
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFE74C3C)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "程序出现异常",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = "很抱歉，XCode 遇到了一个意外错误",
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = "错误信息",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE74C3C)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = crash_log.take(300),
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
        ) {
            LazyColumn(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(log_lines) { line ->
                    val color = when {
                        line.contains("Caused by") -> Color(0xFFE74C3C)
                        line.contains("Exception") || line.contains("Error") -> Color(0xFFE74C3C)
                        line.startsWith("\t") -> Color.Gray
                        line == "Crash Log" || line == "Stack Trace" -> Color(0xFFFF9800)
                        else -> Color.DarkGray
                    }
                    Text(
                        text = line,
                        fontSize = 9.sp,
                        color = color,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = 12.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(0.5f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { on_copy(crash_log, crash_stack) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("复制", fontSize = 13.sp)
            }
            
            OutlinedButton(
                onClick = { on_save(crash_log, crash_stack) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("保存", fontSize = 13.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = on_restart,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("重启应用", fontSize = 13.sp, color = Color.White)
            }
            
            Button(
                onClick = on_exit,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("退出", fontSize = 13.sp, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}