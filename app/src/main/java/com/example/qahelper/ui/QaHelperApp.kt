package com.example.qahelper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.qahelper.data.AppSettings
import com.example.qahelper.data.QaRecord
import com.example.qahelper.data.QaStore
import com.example.qahelper.ui.components.formatRecordTime

private sealed interface Screen {
    data object Home : Screen
    data object Photo : Screen
    data object Ai : Screen
    data object History : Screen
    data object Settings : Screen
    data class Detail(val id: String) : Screen
}

@Composable
fun QaHelperApp() {
    val context = LocalContext.current
    val store = remember { QaStore(context.applicationContext) }

    var settings by remember { mutableStateOf(store.loadSettings()) }
    var history by remember { mutableStateOf(store.loadHistory()) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val backStack = remember { mutableStateListOf<Screen>() }
    var showFirstLaunchTip by remember { mutableStateOf(store.isFirstLaunch()) }

    fun navigate(next: Screen) {
        if (screen == next) return
        backStack.add(screen)
        screen = next
    }

    fun goBack() {
        screen = if (backStack.isNotEmpty()) {
            backStack.removeAt(backStack.lastIndex)
        } else {
            Screen.Home
        }
    }

    fun saveRecord(record: QaRecord): Boolean {
        val result = store.addRecord(record)
        history = result.records
        return result.thumbnailDropped
    }

    BackHandler(enabled = screen != Screen.Home) {
        goBack()
    }

    when (val current = screen) {
        Screen.Home -> HomeScreen(
            history = history,
            settings = settings,
            onOpenPhoto = { navigate(Screen.Photo) },
            onOpenAi = { navigate(Screen.Ai) },
            onOpenHistory = { navigate(Screen.History) },
            onOpenSettings = { navigate(Screen.Settings) },
            onOpenRecord = { navigate(Screen.Detail(it.id)) }
        )

        Screen.Photo -> PhotoScreen(
            settings = settings,
            onBack = { goBack() },
            onSaveRecord = { record -> saveRecord(record) },
            onOpenHistory = { navigate(Screen.History) }
        )

        Screen.Ai -> AiScreen(
            settings = settings,
            onBack = { goBack() },
            onSaveRecord = { record -> saveRecord(record) },
            onOpenHistory = { navigate(Screen.History) }
        )

        Screen.History -> HistoryScreen(
            history = history,
            onBack = { goBack() },
            onOpenRecord = { record -> navigate(Screen.Detail(record.id)) },
            onToggleFavorite = { record ->
                history = store.toggleFavorite(record.id)
            },
            onDelete = { record ->
                history = store.deleteRecord(record.id)
            },
            onClearAll = {
                history = store.clearHistory()
            }
        )

        Screen.Settings -> SettingsScreen(
            settings = settings,
            onBack = { goBack() },
            onSave = { updated ->
                store.saveSettings(updated)
                settings = updated
            }
        )

        is Screen.Detail -> {
            val record = history.firstOrNull { it.id == current.id }
            if (record == null) {
                LaunchedEffect(current.id) {
                    goBack()
                }
            } else {
                DetailScreen(
                    record = record,
                    onBack = { goBack() },
                    onToggleFavorite = { item ->
                        history = store.toggleFavorite(item.id)
                    },
                    onDelete = { item ->
                        history = store.deleteRecord(item.id)
                    }
                )
            }
        }
    }

    if (showFirstLaunchTip) {
        FirstLaunchDialog(
            onDismiss = {
                store.markFirstLaunchDone()
                showFirstLaunchTip = false
            }
        )
    }
}

@Composable
private fun FirstLaunchDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("欢迎使用答题助手") },
        text = {
            Column {
                Text("1. 拍照或从相册选择题目，也可以直接输入文字。")
                Spacer(Modifier.height(8.dp))
                Text("2. 答案会自动保存到本机历史记录，可收藏、复制或删除。")
                Spacer(Modifier.height(8.dp))
                Text("3. 默认开启模拟模式，无需 API Key 也能完整演示。")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "需要真实解答时，到设置里填写自己的 API Key 即可。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("开始使用")
            }
        }
    )
}
