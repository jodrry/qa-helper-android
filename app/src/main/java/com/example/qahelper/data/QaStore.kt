package com.example.qahelper.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AddResult(
    val records: List<QaRecord>,
    val thumbnailDropped: Boolean
)

/**
 * SharedPreferences + JSON 字符串的轻量本地存储。
 * 历史记录 key 固定为 qa_app_history_v1，设置 key 固定为 qa_app_settings_v1。
 */
class QaStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadHistory(): List<QaRecord> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").trim()
                    if (id.isEmpty()) continue

                    val type = when (item.optString("type")) {
                        TYPE_PHOTO -> TYPE_PHOTO
                        else -> TYPE_AI
                    }
                    val thumb = if (item.isNull("imageThumb")) {
                        null
                    } else {
                        item.optString("imageThumb").takeIf { it.isNotBlank() }
                    }

                    add(
                        QaRecord(
                            id = id,
                            type = type,
                            question = item.optString("question"),
                            answer = item.optString("answer"),
                            imageThumb = thumb,
                            favorite = item.optBoolean("favorite", false),
                            createdAt = item.optLong("createdAt", 0L)
                        )
                    )
                }
            }
                .sortedByDescending { it.createdAt }
                .take(MAX_RECORDS)
        }.getOrDefault(emptyList())
    }

    fun addRecord(record: QaRecord): AddResult {
        var safeRecord = record
        var thumbnailDropped = false

        val initialThumb = safeRecord.imageThumb
        if (initialThumb != null && initialThumb.length > MAX_THUMB_CHARS) {
            safeRecord = safeRecord.copy(imageThumb = null)
            thumbnailDropped = true
        }

        val records = loadHistory().toMutableList()
        records.removeAll { it.id == safeRecord.id }
        records.add(0, safeRecord)
        trimToMaxCount(records)

        var encoded = encode(records)
        if (encoded.toByteArray(Charsets.UTF_8).size > MAX_STORE_BYTES) {
            // 优先从最旧的记录开始丢弃缩略图，保留文字答案。
            for (index in records.indices.reversed()) {
                if (records[index].imageThumb != null) {
                    records[index] = records[index].copy(imageThumb = null)
                }
                encoded = encode(records)
                if (encoded.toByteArray(Charsets.UTF_8).size <= MAX_STORE_BYTES) break
            }
        }

        // 极端情况下文字本身仍然过大，就从最旧记录开始裁掉，避免 SharedPreferences 无限膨胀。
        while (encoded.toByteArray(Charsets.UTF_8).size > MAX_STORE_BYTES && records.size > 1) {
            records.removeAt(records.lastIndex)
            encoded = encode(records)
        }

        if (safeRecord.imageThumb != null &&
            records.none { it.id == safeRecord.id && it.imageThumb != null }
        ) {
            thumbnailDropped = true
        }

        prefs.edit().putString(KEY_HISTORY, encoded).apply()
        return AddResult(records.toList(), thumbnailDropped)
    }

    fun toggleFavorite(id: String): List<QaRecord> {
        val records = loadHistory().toMutableList()
        val index = records.indexOfFirst { it.id == id }
        if (index >= 0) {
            records[index] = records[index].copy(favorite = !records[index].favorite)
            saveHistory(records)
        }
        return records
    }

    fun deleteRecord(id: String): List<QaRecord> {
        val records = loadHistory().toMutableList()
        records.removeAll { it.id == id }
        saveHistory(records)
        return records
    }

    fun clearHistory(): List<QaRecord> {
        prefs.edit().remove(KEY_HISTORY).apply()
        return emptyList()
    }

    fun loadChat(): List<ChatMessage> {
        val raw = prefs.getString(KEY_CHAT, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val role = item.optString("role")
                    if (role != CHAT_ROLE_USER && role != CHAT_ROLE_ASSISTANT) continue
                    val content = item.optString("content")
                    if (content.isBlank()) continue
                    add(
                        ChatMessage(
                            role = role,
                            content = content,
                            createdAt = item.optLong("createdAt", 0L)
                        )
                    )
                }
            }
                .sortedBy { it.createdAt }
                .takeLast(MAX_CHAT_MESSAGES)
        }.getOrDefault(emptyList())
    }

    fun saveChat(messages: List<ChatMessage>) {
        val array = JSONArray()
        messages.takeLast(MAX_CHAT_MESSAGES).forEach { message ->
            array.put(
                JSONObject()
                    .put("role", message.role)
                    .put("content", message.content)
                    .put("createdAt", message.createdAt)
            )
        }
        prefs.edit().putString(KEY_CHAT, array.toString()).apply()
    }

    fun clearChat() {
        prefs.edit().remove(KEY_CHAT).apply()
    }

    fun saveHistory(records: List<QaRecord>) {
        val limited = records.sortedByDescending { it.createdAt }.toMutableList()
        trimToMaxCount(limited)
        prefs.edit().putString(KEY_HISTORY, encode(limited)).apply()
    }

    fun loadSettings(): AppSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return AppSettings()
        return runCatching {
            val json = JSONObject(raw)
            AppSettings(
                endpoint = json.optString("endpoint")
                    .takeIf { it.isNotBlank() }
                    ?: AppSettings.DEFAULT_ENDPOINT,
                apiKey = json.optString("apiKey"),
                model = json.optString("model")
                    .takeIf { it.isNotBlank() }
                    ?: AppSettings.DEFAULT_MODEL,
                simulationMode = if (json.has("simulationMode")) {
                    json.optBoolean("simulationMode", true)
                } else {
                    true
                },
                multimodalEnabled = if (json.has("multimodalEnabled")) {
                    json.optBoolean("multimodalEnabled", true)
                } else {
                    true
                }
            )
        }.getOrDefault(AppSettings())
    }

    fun saveSettings(settings: AppSettings) {
        val json = JSONObject()
            .put("endpoint", settings.endpoint.trim())
            .put("apiKey", settings.apiKey.trim())
            .put("model", settings.model.trim())
            .put("simulationMode", settings.simulationMode)
            .put("multimodalEnabled", settings.multimodalEnabled)
        prefs.edit().putString(KEY_SETTINGS, json.toString()).apply()
    }

    fun isFirstLaunch(): Boolean = !prefs.getBoolean(KEY_FIRST_LAUNCH_DONE, false)

    fun markFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_DONE, true).apply()
    }

    private fun trimToMaxCount(records: MutableList<QaRecord>) {
        while (records.size > MAX_RECORDS) {
            records.removeAt(records.lastIndex)
        }
    }

    private fun encode(records: List<QaRecord>): String {
        val array = JSONArray()
        records.forEach { record ->
            val json = JSONObject()
                .put("id", record.id)
                .put("type", record.type)
                .put("question", record.question)
                .put("answer", record.answer)
                .put("favorite", record.favorite)
                .put("createdAt", record.createdAt)
            if (record.imageThumb.isNullOrBlank()) {
                json.put("imageThumb", JSONObject.NULL)
            } else {
                json.put("imageThumb", record.imageThumb)
            }
            array.put(json)
        }
        return array.toString()
    }

    companion object {
        const val KEY_HISTORY = "qa_app_history_v1"
        const val KEY_SETTINGS = "qa_app_settings_v1"
        const val KEY_CHAT = "qa_app_chat_v1"
        private const val PREFS_NAME = "qa_app_prefs_v1"
        private const val KEY_FIRST_LAUNCH_DONE = "qa_app_first_launch_done_v1"
        private const val MAX_RECORDS = 200
        private const val MAX_CHAT_MESSAGES = 100
        private const val MAX_THUMB_CHARS = 20_000
        private const val MAX_STORE_BYTES = 1_200_000
    }
}
