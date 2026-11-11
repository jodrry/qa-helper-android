package com.example.qahelper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.qahelper.data.AppSettings
import com.example.qahelper.data.QaRecord
import com.example.qahelper.ui.components.AppTopBar
import com.example.qahelper.ui.components.EmptyState
import com.example.qahelper.ui.components.ModeCard
import com.example.qahelper.ui.components.RecordSummaryCard
import com.example.qahelper.ui.components.SectionHeader
import com.example.qahelper.ui.theme.BrandBlue
import com.example.qahelper.ui.theme.BrandPurple
import com.example.qahelper.ui.theme.BrandPurpleDeep
import com.example.qahelper.ui.theme.BrandViolet

@Composable
fun HomeScreen(
    history: List<QaRecord>,
    settings: AppSettings,
    onOpenPhoto: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecord: (QaRecord) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "答题助手",
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.List, contentDescription = "历史记录")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenHistory,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(Icons.Default.List, contentDescription = null)
                },
                text = { Text("历史记录") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 96.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            HeroBanner(isMock = settings.isMockMode)
            Spacer(Modifier.height(20.dp))

            Text(
                text = "选择模式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(10.dp))

            ModeCard(
                emoji = "📷",
                title = "拍照搜题模式",
                subtitle = "拍下题目或从相册选择，支持补充题干与多模态识图",
                colors = listOf(BrandPurpleDeep, BrandPurple, BrandViolet),
                onClick = onOpenPhoto
            )
            Spacer(Modifier.height(14.dp))
            ModeCard(
                emoji = "✨",
                title = "AI解答模式",
                subtitle = "直接输入题目，分步骤解答并模拟流式输出",
                colors = listOf(Color(0xFF2F6BE8), BrandBlue, Color(0xFF53A6FF)),
                onClick = onOpenAi
            )
            Spacer(Modifier.height(14.dp))
            ModeCard(
                emoji = "💬",
                title = "对话答疑模式",
                subtitle = "围绕一道题连续追问，带着上下文深入讲解",
                colors = listOf(Color(0xFF0E8F83), Color(0xFF18A99A), Color(0xFF31B5D8)),
                onClick = onOpenChat
            )

            Spacer(Modifier.height(26.dp))
            SectionHeader(
                title = "最近答案",
                actionText = if (history.isEmpty()) null else "全部",
                onAction = onOpenHistory
            )
            Spacer(Modifier.height(10.dp))

            if (history.isEmpty()) {
                EmptyState(
                    emoji = "💡",
                    title = "还没有保存的答案，去搜一道题吧",
                    description = "点击上方任一模式开始，答案会自动保存在本机"
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    history.take(3).forEach { record ->
                        RecordSummaryCard(
                            record = record,
                            onClick = { onOpenRecord(record) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(isMock: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF241575), BrandPurpleDeep, Color(0xFF6754E8))
                )
            )
            .padding(22.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎓", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isMock) "模拟模式 · 无需 API Key" else "已连接自定义 API",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "不会的题，\n一步步讲清楚",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "拍照识题、文字提问、分步解答，最近答案自动留在本机。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
