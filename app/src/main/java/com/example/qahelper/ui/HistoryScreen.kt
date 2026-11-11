package com.example.qahelper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.qahelper.data.QaRecord
import com.example.qahelper.ui.components.AppTopBar
import com.example.qahelper.ui.components.EmptyState
import com.example.qahelper.ui.components.TypePill
import com.example.qahelper.ui.components.formatRecordTime
import com.example.qahelper.util.copyPlainText

@Composable
fun HistoryScreen(
    history: List<QaRecord>,
    onBack: () -> Unit,
    onOpenRecord: (QaRecord) -> Unit,
    onToggleFavorite: (QaRecord) -> Unit,
    onDelete: (QaRecord) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf("all") }
    var pendingDelete by remember { mutableStateOf<QaRecord?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val filtered = if (filter == "favorite") {
        history.filter { it.favorite }
    } else {
        history
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "历史记录",
                onBack = onBack,
                actions = {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirm = true }) {
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "所有答案都保存在本机",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "共 ${history.size} 条，最多保留 200 条",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter == "all",
                    onClick = { filter = "all" },
                    label = { Text("全部 ${history.size}") }
                )
                FilterChip(
                    selected = filter == "favorite",
                    onClick = { filter = "favorite" },
                    label = { Text("收藏 ${history.count { it.favorite }}") }
                )
            }

            Spacer(Modifier.height(10.dp))
            if (filtered.isEmpty()) {
                EmptyState(
                    emoji = if (filter == "favorite") "⭐" else "📭",
                    title = if (filter == "favorite") {
                        "还没有收藏的答案"
                    } else {
                        "还没有保存的答案，去搜一道题吧"
                    },
                    description = if (filter == "favorite") {
                        "在答案详情里点收藏，就会出现在这里"
                    } else {
                        "拍照搜题或 AI 解答后，答案会自动保存"
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                ) {
                    items(items = filtered, key = { it.id }) { record ->
                        HistoryRecordCard(
                            record = record,
                            onOpen = { onOpenRecord(record) },
                            onCopy = {
                                context.copyPlainText("答题助手答案", record.answer)
                            },
                            onFavorite = { onToggleFavorite(record) },
                            onDelete = { pendingDelete = record }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条记录？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(record)
                        pendingDelete = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空全部历史记录？") },
            text = { Text("共 ${history.size} 条记录，清空后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
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
private fun HistoryRecordCard(
    record: QaRecord,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (record.isPhoto) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (record.isPhoto) "📷" else "✨",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TypePill(record)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatRecordTime(record.createdAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (record.favorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "已收藏",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(9.dp))
            Text(
                text = record.question.ifBlank { "图片题目" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = record.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCopy) {
                    Text("复制答案")
                }
                TextButton(onClick = onOpen) {
                    Text("详情")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = if (record.favorite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = if (record.favorite) "取消收藏" else "收藏",
                        tint = if (record.favorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
