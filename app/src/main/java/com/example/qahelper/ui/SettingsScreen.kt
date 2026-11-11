package com.example.qahelper.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.qahelper.data.AppSettings
import com.example.qahelper.ui.components.AppTopBar
import com.example.qahelper.ui.components.SectionHeader
import com.example.qahelper.util.showToast

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onSave: (AppSettings) -> Unit
) {
    val context = LocalContext.current

    var endpoint by rememberSaveable(settings.endpoint) { mutableStateOf(settings.endpoint) }
    var apiKey by rememberSaveable(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var model by rememberSaveable(settings.model) { mutableStateOf(settings.model) }
    var simulationMode by rememberSaveable(settings.simulationMode) {
        mutableStateOf(settings.simulationMode)
    }
    var multimodalEnabled by rememberSaveable(settings.multimodalEnabled) {
        mutableStateOf(settings.multimodalEnabled)
    }
    var showApiKey by remember { mutableStateOf(false) }

    fun save() {
        val finalEndpoint = endpoint.trim().ifBlank { AppSettings.DEFAULT_ENDPOINT }
        if (!finalEndpoint.startsWith("https://") && !finalEndpoint.startsWith("http://")) {
            context.showToast("接口地址应以 https:// 或 http:// 开头")
            return
        }
        val finalModel = model.trim().ifBlank { AppSettings.DEFAULT_MODEL }
        onSave(
            AppSettings(
                endpoint = finalEndpoint,
                apiKey = apiKey.trim(),
                model = finalModel,
                simulationMode = simulationMode,
                multimodalEnabled = multimodalEnabled
            )
        )
        context.showToast("设置已保存")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(title = "设置", onBack = onBack)
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
                text = "连接你的答题模型",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "兼容 OpenAI /chat/completions 协议。不填 API Key 时会继续使用模拟模式。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(title = "接口配置")
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Endpoint") },
                        placeholder = { Text(AppSettings.DEFAULT_ENDPOINT) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Key") },
                        placeholder = { Text("sk-...（仅保存在本机）") },
                        singleLine = true,
                        visualTransformation = if (showApiKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(onClick = { showApiKey = !showApiKey }) {
                                Text(if (showApiKey) "隐藏" else "显示")
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("模型名称") },
                        placeholder = { Text(AppSettings.DEFAULT_MODEL) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Text(
                        text = "API Key 只保存在本机 SharedPreferences，不写死在代码里，也不会提交到 GitHub。",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(title = "运行模式")
                    Spacer(Modifier.height(6.dp))
                    SettingSwitchRow(
                        title = "模拟模式",
                        description = "开启后不调用真实接口，使用本地示例回答，默认开启。",
                        checked = simulationMode,
                        onCheckedChange = { simulationMode = it }
                    )
                    SettingSwitchRow(
                        title = "启用多模态",
                        description = "拍照搜题时把压缩图片作为 image_url 发送；关闭后只发送文字。",
                        checked = multimodalEnabled,
                        onCheckedChange = { multimodalEnabled = it }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("保存设置", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "安全提示：如在公共设备使用，请及时清除 API Key。应用不会申请存储权限，也不会修改系统相机设置。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
