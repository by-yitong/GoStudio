package com.jmwl.gostudio.ui.dialogs.main

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.utils.uri_utils
import com.jmwl.gostudio.ui.theme.*

data class template_item(
    val id: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun new_project_dialog(
    on_dismiss: () -> Unit,
    on_create: (
        project_name: String,
        template_id: String,
        app_name: String,
        app_package: String
    ) -> Unit
) {
    val scope = rememberCoroutineScope()
    val colors = app_theme_provider.colors
    val sheet_state = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var current_step by remember { mutableIntStateOf(0) }
    var selected_template by remember { mutableStateOf("hello") }
    var app_name by remember { mutableStateOf("") }
    var app_package by remember { mutableStateOf("") }
    val is_app_template = selected_template == "app-ui"

    val templates = listOf(
        template_item("hello", "Hello World", Icons.Default.PlayArrow, "最简 main.go，打印 Hello"),
        template_item("cli", "CLI 工具", Icons.Default.Terminal, "os.Args 命令行工具"),
        template_item("database", "数据库", Icons.Default.Storage, "database/sql 与 SQLite 示例"),
        template_item("gin", "Gin", Icons.Default.Rocket, "Gin 路由与 JSON API"),
        template_item("gorm", "GORM", Icons.Default.TableChart, "GORM 模型与 CRUD"),
        template_item("app-ui", "App 界面", Icons.Default.Smartphone, "AndLua 式布局 + Go 逻辑，直接在宿主内运行")
    )

    var project_name by remember { mutableStateOf("") }
    var name_error by remember { mutableStateOf(false) }

    fun check_project_name(name: String): Boolean {
        project_name = name
        if (name.isBlank()) {
            name_error = false
            return false
        }
        val pattern = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
        val is_valid = pattern.matches(name)
        name_error = name.isNotBlank() && !is_valid
        return is_valid
    }

    val is_name_valid = project_name.isNotBlank() && !name_error
    val is_create_enabled = is_name_valid

    val text_field_colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.dialog_input_border,
        unfocusedBorderColor = colors.dialog_input_hint.copy(alpha = 0.5f),
        focusedTextColor = colors.dialog_input_text,
        unfocusedTextColor = colors.dialog_input_text,
        cursorColor = colors.dialog_input_border,
        focusedLeadingIconColor = colors.dialog_input_icon,
        unfocusedLeadingIconColor = colors.dialog_input_icon_hint,
        focusedLabelColor = colors.dialog_input_border,
        unfocusedLabelColor = colors.dialog_input_hint,
        focusedContainerColor = colors.dialog_input_bg,
        unfocusedContainerColor = colors.dialog_input_bg
    )
    
    ModalBottomSheet(
        onDismissRequest = on_dismiss,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        sheetState = sheet_state,
        containerColor = colors.dialog_bg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null,
        sheetGesturesEnabled = false,
        properties = ModalBottomSheetProperties(
            shouldDismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (current_step == 1) {
                    IconButton(onClick = { current_step = 0 }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = colors.dialog_text)
                    }
                } else {
                    Spacer(modifier = Modifier.width(36.dp))
                }
                
                Text(
                    text = if (current_step == 0) "新建项目" else "项目配置",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.dialog_text
                )
                
                IconButton(
                    onClick = {
                        scope.launch {
                            sheet_state.hide()
                            on_dismiss()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = colors.dialog_hint)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            HorizontalDivider(color = colors.dialog_hint.copy(alpha = 0.2f), thickness = 0.5.dp)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            AnimatedContent(
                targetState = current_step,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.85f, animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.85f, animationSpec = tween(200))
                },
                label = "step"
            ) { step ->
                when (step) {
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                           LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(templates) { template ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.95f)
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable {
                                                selected_template = template.id
                                                current_step = 1
                                            },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = colors.dialog_card_bg
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(colors.dialog_icon.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    template.icon,
                                                    contentDescription = template.title,
                                                    tint = colors.dialog_icon.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = template.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.dialog_text,
                                                maxLines = 1
                                            )
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            Text(
                                                text = template.description,
                                                fontSize = 10.sp,
                                                color = colors.dialog_hint,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = colors.dialog_icon.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.dialog_icon,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "已选择模板",
                                            fontSize = 12.sp,
                                            color = colors.dialog_hint
                                        )
                                        Text(
                                            text = templates.find { it.id == selected_template }?.title ?: "未知",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.dialog_icon
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            OutlinedTextField(
                                value = project_name,
                                onValueChange = { check_project_name(it) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = if (project_name.isNotBlank() && !name_error) colors.dialog_input_icon else colors.dialog_input_icon_hint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = { Text("项目名称", color = colors.dialog_input_hint) },
                                placeholder = { Text("my_project", color = colors.dialog_input_hint) },
                                isError = name_error,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = text_field_colors
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (is_app_template) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "App 名称",
                                    fontSize = 13.sp,
                                    color = colors.dialog_hint,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = app_name,
                                    onValueChange = { app_name = it },
                                    placeholder = { Text(project_name.ifBlank { "MyApp" }) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = text_field_colors
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "包名",
                                    fontSize = 13.sp,
                                    color = colors.dialog_hint,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = app_package,
                                    onValueChange = { app_package = it },
                                    placeholder = { Text("com.gs.$project_name".filter { it.isLetterOrDigit() || it == '.' }.lowercase()) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = text_field_colors
                                )
                            }

                            Text(
                                text = "项目保存在应用内部存储（proot 可直接访问，构建/补全更稳定）",
                                fontSize = 11.sp,
                                color = colors.dialog_hint,
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    on_create(
                                        project_name,
                                        selected_template,
                                        app_name,
                                        app_package
                                    )
                                    on_dismiss()
                                },
                                enabled = is_create_enabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.dialog_clone_bg,
                                    contentColor = colors.dialog_clone_text,
                                    disabledContainerColor = colors.dialog_hint.copy(alpha = 0.3f),
                                    disabledContentColor = colors.dialog_hint
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("创建项目", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
