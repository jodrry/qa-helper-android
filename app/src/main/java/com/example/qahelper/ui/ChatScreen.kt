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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.qahelper.data.CHAT_ROLE_ASSISTANT
import com.example.qahelper.data.CHAT_ROLE_USER
import com.example.qahelper.data.ChatMessage
import com.example.qahelper.data.QaRecord
import com.example.qahelper.data.TYPE_AI
import com.example.qahelper.network.ChatTurn
import com.example.qahelper.network.QaApi
import com.example.qahelper.ui.components.AppTopBar
import com.example.qahelper.ui.components.ErrorCard
import com.example.qahelper.util.showToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ChatScreen(
    settings: AppSettings,
    messages: List<ChatMessage>,
    onMessagesChange: (List<ChatMessage>) -> Unit,
    onSaveRecord: (QaRecord) -> Boolean,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var input by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingQuestion by remember { mutableStateOf<String?>(null) }
    var pendingContext by remember { mutableStateOf<List<ChatTurn>>(emptyList()) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val isMock = settings.isMockMode

    fun requestAnswer(
        question: String,
        contextTurns: List<ChatTurn>,
        baseMessages: List<ChatMessage>
    ) {
        sending = true
        error = null
        scope.launch {
            try {
                val result = QaApi.chat(
                    settings = settings,
                    history = contextTurns,
                    question = question
                )
                val assistantMessage = ChatMessage(
                    role = CHAT_ROLE_ASSISTANT,
                    content = result,
                    createdAt = System.currentTimeMillis()
                )
                onMessagesChange(baseMessages + assistantMessage)
                onSaveRecord(
                    QaRecord(
                        id = UUID.randomUUID().toString(),
                        type = TYPE_AI,
                        question = if (contextTurns.isEmpty()) question else "追问：$question",
                        answer = result,
                        imageThumb = null,
                        favorite = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
                pendingQuestion = null
                pendingContext = emptyList()
            } catch (exception: Exception) {
                error = exception.message ?: "对话请求失败，请重试"
            } finally {
                sending = false
            }
        }
    }

    fun send() {
        if (sending) return
        val question = input.trim()
        if (question.isBlank()) {
            context.showToast("请输入要问的问题")
            return
        }

        val contextTurns = messages
            .takeLast(12)
            .map { ChatTurn(role = it.role, content = it.content) }
        val userMessage = ChatMessage(
            role = CHAT_ROLE_USER,
            content = question,
            createdAt = System.currentTimeMillis()
        )
        val updatedMessages = messages + userMessage

        onMessagesChange(updatedMessages)
        input = ""
        pendingQuestion = question
        pendingContext = contextTurns
        requestAnswer(question, contextTurns, updatedMessages)
    }

    fun retry() {
        val question = pendingQuestion ?: return
        if (sending) return
        requestAnswer(question, pendingContext, messages)
    }

    LaunchedEffect(messages.size, sending, error) {
        delay(80)
        val extraItems = (if (sending) 1 else 0) + (if (error != null) 1 else 0)
        val targetIndex = messages.size + extraItems
        listState.animateScrollToItem(targetIndex.coerceAtLeast(0))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "对话答疑",
                onBack = onBack,
                actions = {
                    TextButton(onClick = onOpenHistory) {
                        Text("历史")
                    }
                    if (messages.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearConfirm = true },
                            enabled = !sending
                        ) {
                            Text("清空")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Text(
                text = "会记住最近 6 轮对话 · 每轮答案自动保存",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 4.dp,
                    bottom = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    ChatBubble(
                        message = ChatMessage(
                            role = CHAT_ROLE_ASSISTANT,
                            content = "你好，我是答题助手。可以围绕一道题连续追问，我会结合最近 6 轮对话理解上下文。默认模拟模式也能体验完整流程。",
                            createdAt = 0L
                        ),
                        isMock = false
                    )
                }

                items(items = messages) { message ->
                    ChatBubble(message = message, isMock = isMock)
                }

                if (sending) {
                    item {
                        ChatTypingBubble()
                    }
                }

                if (error != null) {
                    item {
                        ErrorCard(
                            message = error.orEmpty(),
                            onRetry = { retry() }
                        )
                    }
                }
            }

            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier
                            .weight(1f)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    event.key == Key.Enter &&
                                    event.isCtrlPressed
                                ) {
                                    send()
                                    true
                                } else {
                                    false
                                }
                            },
                        placeholder = { Text("继续追问，例如：第二步为什么要这样变形？") },
                        minLines = 1,
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        enabled = !sending
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { send() },
                        modifier = Modifier.height(52.dp),
                        enabled = !sending && input.isNotBlank()
                    ) {
                        if (sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("发送中")
                        } else {
                            Text("发送")
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空当前对话？") },
            text = { Text("只清空当前多轮上下文，已保存到历史记录的答案不会删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMessagesChange(emptyList())
                        input = ""
                        error = null
                        pendingQuestion = null
                        pendingContext = emptyList()
                        showClearConfirm = false
                    }
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, isMock: Boolean) {
    val isUser = message.role == CHAT_ROLE_USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = shape,
            color = bubbleColor,
            tonalElevation = if (isUser) 0.dp else 1.dp,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "你" else "答题助手",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    if (!isUser && isMock) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "模拟模式",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                SelectionContainer {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatTypingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "正在结合上下文思考...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
