package com.example.qahelper.data

/**
 * 保存在本机的接口设置。
 * 默认开启模拟模式，因此没有 API Key 也能完整演示全部流程。
 */
data class AppSettings(
    val endpoint: String = DEFAULT_ENDPOINT,
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
    val simulationMode: Boolean = true,
    val multimodalEnabled: Boolean = true
) {
    val isMockMode: Boolean
        get() = simulationMode || apiKey.isBlank()

    companion object {
        const val DEFAULT_ENDPOINT = "https://api.deepseek.com/chat/completions"
        const val DEFAULT_MODEL = "deepseek-chat"
    }
}
