package com.termux.app.gostudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termux.app.gostudio.ai.AiAssistantViewModel
import com.termux.app.gostudio.ai.ChatUiMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantPanel(
    viewModel: AiAssistantViewModel,
    currentFileContent: String? = null,
    currentFileName: String? = null,
    onInsertCode: (String) -> Unit = {}
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showSettings) {
        ApiKeySettingsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideSettings() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = Color(0xFF4FC1FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Assistant",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF2D2D30)
        ),
        actions = {
            IconButton(onClick = { viewModel.clearMessages() }) {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = "Clear",
                    tint = Color(0xFFAAAAAA)
                )
            }
            IconButton(onClick = { viewModel.toggleSettings() }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFFAAAAAA)
                )
            }
        }
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (messages.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = Color(0xFF4FC1FF),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "How can I help you with Go?",
                            color = Color(0xFF888888),
                            fontSize = 14.sp
                        )
                        }
                    }
                }
            }

        items(messages) { message ->
            ChatMessageItem(
                message = message,
                onInsertCode = { code -> onInsertCode(code) }
            )
        }
    }

    errorMessage?.let { error ->
        Card(
            modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF442222))
        ) {
            Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFFF6B6B)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = error,
                    color = Color(0xFFFF6B6B),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.clearError() }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFFF6B6B)
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    "Ask anything...",
                    color = Color(0xFF888888)
                )
            },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF2D2D30),
                unfocusedContainerColor = Color(0xFF2D2D30),
                cursorColor = Color(0xFF4FC1FF),
                focusedBorderColor = Color(0xFF4FC1FF),
                unfocusedBorderColor = Color(0xFF3D3D40)
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send),
            enabled = !isLoading,
            singleLine = false,
            maxLines = 4
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(
                        content = inputText,
                        currentFileContent = currentFileContent,
                        currentFileName = currentFileName
                    )
                    inputText = ""
                }
            },
            enabled = !isLoading && inputText.isNotBlank()
        ) {
            Icon(
                if (isLoading) Icons.Default.Refresh else Icons.Default.Send,
                contentDescription = "Send",
                tint = if (isLoading || inputText.isBlank()) Color(0xFF888888) else Color(0xFF4FC1FF)
            )
        }
    }
}
}

@Composable
fun ChatMessageItem(
    message: ChatUiMessage,
    onInsertCode: (String) -> Unit
) {
    val isUser = message.role == "user"
    val bgColor = if (isUser) Color(0xFF2D2D30) else Color(0xFF1A1A2E)
    val textColor = if (isUser) Color.White else Color(0xFFE0E0E0)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isUser) Icons.Default.Person else Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = if (isUser) Color(0xFF4FC1FF) else Color(0xFF9B59B6),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isUser) "You" else "Claude",
                        color = Color(0xFF888888),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (message.isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF4FC1FF),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thinking...",
                            color = Color(0xFF888888),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    MessageContent(
                        content = message.content,
                        onInsertCode = onInsertCode
                    )
                }
            }
        }
    }
}

@Composable
fun MessageContent(
    content: String,
    onInsertCode: (String) -> Unit
) {
    val codeBlocks = mutableListOf<String>()
    val regex = Regex("```(go)?\\n([\\s\\S]*?)\\n```")
    var lastIndex = 0
    
    regex.findAll(content).forEach { match ->
        if (match.range.first > lastIndex) {
            Text(
                text = content.substring(lastIndex, match.range.first),
                color = Color(0xFFE0E0E0),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        
        val code = match.groupValues[2]
        codeBlocks.add(code)
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117))
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "go",
                        color = Color(0xFF888888),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                    TextButton(
                        onClick = { onInsertCode(code) },
                        modifier = Modifier.padding(end = 4.dp, top = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Insert",
                            tint = Color(0xFF4FC1FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Insert",
                            color = Color(0xFF4FC1FF),
                            fontSize = 11.sp
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFF30363D))
                Text(
                    text = code,
                    color = Color(0xFFC9D1D9),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        
        lastIndex = match.range.last + 1
    }
    
    if (lastIndex < content.length) {
        Text(
            text = content.substring(lastIndex),
            color = Color(0xFFE0E0E0),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeySettingsDialog(
    viewModel: AiAssistantViewModel,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf(viewModel.getApiKey() ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API Settings") },
        text = {
            Column {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Anthropic API Key") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Default.Visibility
                        else Icons.Default.VisibilityOff
                        
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide key" else "Show key")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Get your API key from https://console.anthropic.com/",
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.saveApiKey(apiKey)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
