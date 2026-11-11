package com.example.qahelper.data

/**
 * 一条本地保存的问答记录。
 *
 * imageThumb 只在拍照搜题时保存小尺寸缩略图，避免把整张大图塞进 SharedPreferences。
 */
data class QaRecord(
    val id: String,
    val type: String,
    val question: String,
    val answer: String,
    val imageThumb: String?,
    val favorite: Boolean,
    val createdAt: Long
) {
    val isPhoto: Boolean
        get() = type == TYPE_PHOTO
}

const val TYPE_PHOTO = "photo"
const val TYPE_AI = "ai"
