package com.termux.app.gostudio.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 4096,
    val temperature: Double = 0.7
)

@Serializable
data class ChatResponse(
    val id: String,
    val content: List<ContentBlock>
) {
    @Serializable
    data class ContentBlock(
        val type: String,
        val text: String? = null
    )
}

class ClaudeApiClient(
    private val apiKey: String,
    private val model: String = "claude-3-5-sonnet-20241022"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun chat(messages: List<ChatMessage>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = ChatRequest(
                model = model,
                messages = messages
            )

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Result.failure(Exception("API error: ${response.code} - $errorBody"))
            } else {
                val responseBody = response.body?.string() ?: throw Exception("Empty response")
                val chatResponse = json.decodeFromString<ChatResponse>(responseBody)

                val assistantMessage = chatResponse.content
                    .filter { it.type == "text" && it.text != null }
                    .joinToString("") { it.text ?: "" }

                Result.success(assistantMessage)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
