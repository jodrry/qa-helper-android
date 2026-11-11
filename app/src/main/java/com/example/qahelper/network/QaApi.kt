package com.example.qahelper.network

import com.example.qahelper.data.AppSettings
import com.example.qahelper.data.TYPE_PHOTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class QaApiException(message: String) : Exception(message)

/**
 * OpenAI 兼容 /chat/completions 接口封装。
 * 后续如果要换成别的服务，只需要替换 answerQuestion 的内部实现。
 */
object QaApi {

    private const val SYSTEM_PROMPT =
        "你是一个严谨的答题助手，请给出清晰、分步骤的解答。"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .build()
    }

    suspend fun answerQuestion(
        settings: AppSettings,
        question: String,
        imageBase64: String? = null,
        type: String = TYPE_PHOTO
    ): String {
        val hasImage = !imageBase64.isNullOrBlank() && settings.multimodalEnabled

        if (settings.isMockMode) {
            return MockAnswer.build(question, type, hasImage)
        }

        val endpoint = settings.endpoint.trim()
        if (endpoint.isBlank()) {
            throw QaApiException("接口地址为空，请先到设置中填写 API Endpoint。")
        }
        if (!endpoint.startsWith("https://") && !endpoint.startsWith("http://")) {
            throw QaApiException("接口地址格式不正确，应以 https:// 或 http:// 开头。")
        }

        return withContext(Dispatchers.IO) {
            try {
                executeRequest(
                    endpoint = endpoint,
                    apiKey = settings.apiKey.trim(),
                    model = settings.model.trim(),
                    question = question,
                    imageBase64 = if (hasImage) imageBase64 else null
                )
            } catch (error: QaApiException) {
                throw error
            } catch (error: SocketTimeoutException) {
                throw QaApiException("请求超时了，请检查网络后重试。")
            } catch (error: UnknownHostException) {
                throw QaApiException("无法连接服务器，请检查网络或接口地址。")
            } catch (error: IOException) {
                throw QaApiException("网络请求失败：${error.message ?: "请稍后重试"}")
            } catch (error: IllegalArgumentException) {
                throw QaApiException("接口地址或请求参数不正确，请到设置中检查。")
            } catch (error: Exception) {
                throw QaApiException("解析回答失败：${error.message ?: "请重试"}")
            }
        }
    }

    private fun executeRequest(
        endpoint: String,
        apiKey: String,
        model: String,
        question: String,
        imageBase64: String?
    ): String {
        val payload = JSONObject()
            .put("model", model)
            .put("messages", buildMessages(question, imageBase64))
            .put("temperature", 0.3)

        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val apiMessage = runCatching {
                    JSONObject(bodyText)
                        .optJSONObject("error")
                        ?.optString("message")
                        .orEmpty()
                }.getOrDefault("")
                throw QaApiException(httpErrorMessage(response.code, apiMessage))
            }

            val content = runCatching {
                JSONObject(bodyText)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content")
            }.getOrNull()

            if (content.isNullOrBlank()) {
                throw QaApiException("接口返回了空回答，请稍后重试或检查模型名称。")
            }
            return content.trim()
        }
    }

    private fun buildMessages(question: String, imageBase64: String?): JSONArray {
        val messages = JSONArray()

        messages.put(
            JSONObject()
                .put("role", "system")
                .put("content", SYSTEM_PROMPT)
        )

        val userMessage = JSONObject().put("role", "user")
        if (imageBase64.isNullOrBlank()) {
            userMessage.put(
                "content",
                question.trim().ifBlank { "请解答图片中的题目。" }
            )
        } else {
            val text = if (question.isBlank()) {
                "请解答图片中的题目，并给出清晰、分步骤的解答。"
            } else {
                "$question\n\n请结合图片内容作答，并给出清晰、分步骤的解答。"
            }
            val content = JSONArray()
                .put(
                    JSONObject()
                        .put("type", "text")
                        .put("text", text)
                )
                .put(
                    JSONObject()
                        .put("type", "image_url")
                        .put(
                            "image_url",
                            JSONObject().put("url", "data:image/jpeg;base64,$imageBase64")
                        )
                )
            userMessage.put("content", content)
        }

        messages.put(userMessage)
        return messages
    }

    private fun httpErrorMessage(code: Int, apiMessage: String): String {
        val detail = apiMessage.trim().take(180)
        val prefix = when (code) {
            400 -> "请求参数有误"
            401 -> "API Key 无效或已过期"
            403 -> "当前 API Key 没有访问权限"
            404 -> "接口地址或模型不存在"
            429 -> "请求过于频繁或额度不足"
            in 500..599 -> "服务端暂时不可用"
            else -> "请求失败（HTTP $code）"
        }
        return if (detail.isBlank()) "$prefix，请检查设置后重试。" else "$prefix：$detail"
    }
}
