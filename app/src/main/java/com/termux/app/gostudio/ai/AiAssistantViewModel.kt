package com.termux.app.gostudio.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiMessage(
    val id: Long,
    val role: String,
    val content: String,
    val isLoading: Boolean = false
)

class AiAssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val apiKeyManager = ApiKeyManager(application)

    private val _messages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
    val messages: StateFlow<List<ChatUiMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    fun hasApiKey(): Boolean = apiKeyManager.getApiKey() != null

    fun getApiKey(): String? = apiKeyManager.getApiKey()

    fun saveApiKey(apiKey: String) {
        apiKeyManager.saveApiKey(apiKey)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun toggleSettings() {
        _showSettings.value = !_showSettings.value
    }

    fun hideSettings() {
        _showSettings.value = false
    }

    fun sendMessage(content: String, currentFileContent: String? = null, currentFileName: String? = null) {
        if (_isLoading.value) return

        val apiKey = apiKeyManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            _showSettings.value = true
            return
        }

        val userMessageId = System.currentTimeMillis()
        val userMessage = ChatUiMessage(
            id = userMessageId,
            role = "user",
            content = content
        )

        val loadingMessageId = userMessageId + 1
        val loadingMessage = ChatUiMessage(
            id = loadingMessageId,
            role = "assistant",
            content = "",
            isLoading = true
        )

        _messages.value = _messages.value + userMessage + loadingMessage
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val client = ClaudeApiClient(
                    apiKey = apiKey,
                    model = apiKeyManager.getModel()
                )

                val systemPrompt = buildString {
                    append("You are a helpful AI coding assistant specialized in Go programming. ")
                    append("You are working in a Go IDE called GoStudio. ")
                    append("When asked to write code, provide clear, well-formatted Go code. ")
                    append("When you generate code, wrap it in ```go ... ``` blocks. ")
                    append("Keep responses concise and focused on the task at hand.\n\n")

                    if (currentFileName != null && currentFileContent != null) {
                        append("Current file: $currentFileName\n")
                        append("Current file content:\n")
                        append("```go\n")
                        append(currentFileContent.take(2000)) // Limit context size
                        append("\n```\n")
                    }
                }

                val messagesList = mutableListOf<ChatMessage>()
                messagesList.add(ChatMessage(role = "user", content = systemPrompt))

                _messages.value.filterNot { it.isLoading }.forEach { msg ->
                    messagesList.add(ChatMessage(role = msg.role, content = msg.content))
                }

                val result = client.chat(messagesList)

                _messages.value = _messages.value.filterNot { it.id == loadingMessageId }

                if (result.isSuccess) {
                    val assistantMessage = ChatUiMessage(
                        id = System.currentTimeMillis(),
                        role = "assistant",
                        content = result.getOrNull() ?: ""
                    )
                    _messages.value = _messages.value + assistantMessage
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                _messages.value = _messages.value.filterNot { it.id == loadingMessageId }
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    fun extractCodeBlocks(content: String): List<String> {
        val codeBlocks = mutableListOf<String>()
        val regex = Regex("```(go)?\\n([\\s\\S]*?)\\n```")
        regex.findAll(content).forEach { match ->
            codeBlocks.add(match.groupValues[2])
        }
        return codeBlocks
    }
}
