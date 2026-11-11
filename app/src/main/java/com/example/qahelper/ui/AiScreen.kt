package com.example.qahelper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.qahelper.data.AppSettings
import com.example.qahelper.data.QaRecord
import com.example.qahelper.data.TYPE_AI
import com.example.qahelper.network.QaApi
import com.example.qahelper.ui.components.AnswerCard
import com.example.qahelper.ui.components.AppTopBar
import com.example.qahelper.ui.components.ErrorCard
import com.example.qahelper.ui.components.SectionHeader
import com.example.qahelper.util.copyPlainText
import com.example.qahelper.util.showToast
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AiScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onSaveRecord: (QaRecord) -> Boolean,
    onOpenHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var question by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val isMock = settings.isMockMode

    fun submit() {
        if (loading) return
        val questionText = question.trim()
        if (questionText.isBlank()) {
            context.showToast("请先输入题目")
            return
        }

        loading = true
        error = null
        answer = null

        scope.launch {
            try {
                val result = QaApi.answerQuestion(
                    settings = settings,
                    question = questionText,
                    imageBase64 = null,
                    type = TYPE_AI
                )
                answer = result
                onSaveRecord(
                    QaRecord(
                        id = UUID.randomUUID().toString(),
                        type = TYPE_AI,
                        question = questionText,
                        answer = result,
                        imageThumb = null,
                        favorite = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } catch (exception: Exception) {
                error = exception.message ?: "解答失败，请重试"
            } finally {
                loading = false
            }
        }
    }

    fun clearAll() {
        if (loading) return
        question = ""
        answer = null
        error = null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "AI解答",
                onBack = onBack,
                actions = {
                    TextButton(onClick = onOpenHistory) {
                        Text("历史")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "输入题目，获得分步解答",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isMock) {
                    "模拟模式已开启：无需 API Key，也能体验完整流程。"
                } else {
                    "回答将以逐段打字机效果呈现，并自动保存到历史记录。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown &&
                            event.key == Key.Enter &&
                            event.isCtrlPressed
                        ) {
                            submit()
                            true
                        } else {
                            false
                        }
                    },
                label = { Text("题目") },
                placeholder = { Text("输入题目，例如：请解答这道数学题...") },
                minLines = 7,
                maxLines = 14,
                shape = RoundedCornerShape(20.dp),
                enabled = !loading
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "支持键盘 Ctrl+Enter 快速提交",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { clearAll() },
                    modifier = Modifier.weight(1f),
                    enabled = !loading
                ) {
                    Text("清空")
                }
                Button(
                    onClick = { submit() },
                    modifier = Modifier
                        .weight(1.8f)
                        .height(50.dp),
                    enabled = !loading && question.isNotBlank()
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("正在解答...")
                    } else {
                        Text("提交解答", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(16.dp))
                ErrorCard(
                    message = error.orEmpty(),
                    onRetry = { submit() }
                )
            }

            if (answer != null) {
                Spacer(Modifier.height(20.dp))
                SectionHeader(title = "解答结果")
                Spacer(Modifier.height(10.dp))
                AnswerCard(
                    answer = answer.orEmpty(),
                    isMock = isMock,
                    typing = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已自动保存到历史记录",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            context.copyPlainText("答题助手答案", answer.orEmpty())
                        }
                    ) {
                        Text("复制答案")
                    }
                }
            }
        }
    }
}
