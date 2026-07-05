package com.xc.code.ui.dialogs.main

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
import com.xc.code.utils.uri_utils
import com.xc.code.ui.theme.*

data class template_item(
    val id: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun new_project_dialog(
    ndk_versions: List<String>,
    cmake_versions: List<String>,
    on_dismiss: () -> Unit,
    on_create: (
        project_name: String,
        project_path: String,
        template_id: String,
        ndk_version: String,
        cmake_version: String,
        android_platform: String,
        cpp_standard: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = app_theme_provider.colors
    val sheet_state = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var current_step by remember { mutableIntStateOf(0) }
    var selected_template by remember { mutableStateOf("executable") }
    
    val templates = listOf(
        template_item("executable", "可执行", Icons.Default.PlayArrow, "生成独立的可执行程序"),
        template_item("dynamic_lib", "动态库", Icons.Default.DynamicForm, "生成 .so 动态链接库"),
        template_item("static_lib", "静态库", Icons.AutoMirrored.Filled.LibraryBooks, "生成 .a 静态库文件")
    )
    
    var project_name by remember { mutableStateOf("") }
    var project_path by remember { mutableStateOf("") }
    var name_error by remember { mutableStateOf(false) }
    var expanded_ndk by remember { mutableStateOf(false) }
    var expanded_cmake by remember { mutableStateOf(false) }
    var expanded_platform by remember { mutableStateOf(false) }
    var expanded_cpp_standard by remember { mutableStateOf(false) }
    var selected_ndk by remember(ndk_versions) { mutableStateOf(ndk_versions.firstOrNull().orEmpty()) }
    var selected_cmake by remember(cmake_versions) { mutableStateOf(cmake_versions.firstOrNull().orEmpty()) }
    var android_platform by remember { mutableStateOf("android-24") }
    var cpp_standard by remember { mutableStateOf("20") }
    val android_platform_options = listOf("android-21", "android-23", "android-24", "android-26", "android-28", "android-30", "android-33", "android-35")
    val cpp_standard_options = listOf("11", "14", "17", "20", "23", "26")
    
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
    val is_path_valid = project_path.isNotBlank()
    val is_platform_valid = android_platform in android_platform_options
    val is_cpp_standard_valid = cpp_standard in cpp_standard_options
    val has_toolchains = selected_ndk.isNotBlank() && selected_cmake.isNotBlank()
    val is_create_enabled = is_name_valid && is_path_valid && has_toolchains && is_platform_valid && is_cpp_standard_valid
    
    val folder_launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val path = uri_utils.get_path_from_uri(context, it)
                project_path = path
            }
        }
    }
    
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
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            OutlinedTextField(
                                value = project_path,
                                onValueChange = { project_path = it },
                                readOnly = true,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (project_path.isNotBlank()) colors.dialog_input_icon else colors.dialog_input_icon_hint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = { Text("项目位置", color = colors.dialog_input_hint) },
                                placeholder = { Text("点击文件夹图标以选择", color = colors.dialog_input_hint) },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                        folder_launcher.launch(intent)
                                    }) {
                                        Icon(
                                            Icons.Default.FolderOpen,
                                            contentDescription = "选择文件夹",
                                            tint = colors.dialog_input_icon_hint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = text_field_colors
                            )
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ExposedDropdownMenuBox(
                                    expanded = expanded_ndk,
                                    onExpandedChange = { expanded_ndk = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                OutlinedTextField(
                                    value = selected_ndk,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("NDK 版本", color = colors.dialog_input_hint) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded_ndk) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = text_field_colors
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded_ndk,
                                    onDismissRequest = { expanded_ndk = false },
                                    containerColor = colors.dialog_bg
                                ) {
                                    ndk_versions.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    option,
                                                    color = if (option == selected_ndk) colors.dialog_icon else colors.dialog_text
                                                )
                                            },
                                            onClick = {
                                                selected_ndk = option
                                                expanded_ndk = false
                                            },
                                            leadingIcon = {
                                                if (option == selected_ndk) {
                                                    Icon(Icons.Default.Check, null, tint = colors.dialog_icon, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                                ExposedDropdownMenuBox(
                                    expanded = expanded_cmake,
                                    onExpandedChange = { expanded_cmake = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                OutlinedTextField(
                                    value = selected_cmake,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("CMake 版本", color = colors.dialog_input_hint) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded_cmake) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = text_field_colors
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded_cmake,
                                    onDismissRequest = { expanded_cmake = false },
                                    containerColor = colors.dialog_bg
                                ) {
                                    cmake_versions.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    option,
                                                    color = if (option == selected_cmake) colors.dialog_icon else colors.dialog_text
                                                )
                                            },
                                            onClick = {
                                                selected_cmake = option
                                                expanded_cmake = false
                                            },
                                            leadingIcon = {
                                                if (option == selected_cmake) {
                                                    Icon(Icons.Default.Check, null, tint = colors.dialog_icon, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ExposedDropdownMenuBox(
                                    expanded = expanded_cpp_standard,
                                    onExpandedChange = { expanded_cpp_standard = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = "C++$cpp_standard",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("C++标准", color = colors.dialog_input_hint) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded_cpp_standard) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = text_field_colors
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expanded_cpp_standard,
                                        onDismissRequest = { expanded_cpp_standard = false },
                                        containerColor = colors.dialog_bg
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .heightIn(max = 240.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            cpp_standard_options.forEach { option ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            "C++$option",
                                                            color = if (option == cpp_standard) colors.dialog_icon else colors.dialog_text
                                                        )
                                                    },
                                                    onClick = {
                                                        cpp_standard = option
                                                        expanded_cpp_standard = false
                                                    },
                                                    leadingIcon = {
                                                        if (option == cpp_standard) {
                                                            Icon(Icons.Default.Check, null, tint = colors.dialog_icon, modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                ExposedDropdownMenuBox(
                                    expanded = expanded_platform,
                                    onExpandedChange = { expanded_platform = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = android_platform,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Android Platform", color = colors.dialog_input_hint) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded_platform) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = text_field_colors
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expanded_platform,
                                        onDismissRequest = { expanded_platform = false },
                                        containerColor = colors.dialog_bg
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .heightIn(max = 240.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            android_platform_options.forEach { option ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            option,
                                                            color = if (option == android_platform) colors.dialog_icon else colors.dialog_text
                                                        )
                                                    },
                                                    onClick = {
                                                        android_platform = option
                                                        expanded_platform = false
                                                    },
                                                    leadingIcon = {
                                                        if (option == android_platform) {
                                                            Icon(Icons.Default.Check, null, tint = colors.dialog_icon, modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            
                            if (!has_toolchains) {
                                Text(
                                    text = "需要先安装 NDK 和 CMake 才能创建项目",
                                    fontSize = 12.sp,
                                    color = colors.dialog_hint
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            } else {
                                Spacer(modifier = Modifier.height(28.dp))
                            }
                            
                            Button(
                                onClick = {
                                    on_create(
                                        project_name,
                                        project_path,
                                        selected_template,
                                        selected_ndk,
                                        selected_cmake,
                                        android_platform.trim(),
                                        cpp_standard
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