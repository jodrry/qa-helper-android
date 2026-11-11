package com.example.qahelper.data

const val CHAT_ROLE_USER = "user"
const val CHAT_ROLE_ASSISTANT = "assistant"

/**
 * 对话答疑中的一条消息。
 * 只保存用于展示和继续追问的文本，不保存图片。
 */
data class ChatMessage(
    val role: String,
    val content: String,
    val createdAt: Long
)
