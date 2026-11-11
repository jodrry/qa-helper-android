package com.example.qahelper.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.qahelper.data.AppSettings
import com.example.qahelper.data.QaRecord
import com.example.qahelper.data.TYPE_PHOTO
import com.example.qahelper.network.QaApi
import com.example.qahelper.ui.components.AnswerCard
import com.example.qahelper.ui.components.AppTopBar
import com.example.qahelper.ui.components.ErrorCard
import com.example.qahelper.ui.components.SectionHeader
import com.example.qahelper.util.ImageUtils
import com.example.qahelper.util.copyPlainText
import com.example.qahelper.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun PhotoScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onSaveRecord: (QaRecord) -> Boolean,
    onOpenHistory: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var question by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var savedThumbnailNotice by remember { mutableStateOf(false) }

    val isMock = settings.isMockMode

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            imageUri = uri
            answer = null
            error = null
            savedThumbnailNotice = false
        } else {
            context.showToast("拍照已取消或失败")
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            answer = null
            error = null
            savedThumbnailNotice = false
        } else {
            context.showToast("未选择图片")
        }
    }

    LaunchedEffect(imageUri) {
        preview = null
        val uri = imageUri ?: return@LaunchedEffect
        val bitmap = withContext(Dispatchers.IO) {
            ImageUtils.decodePreview(context, uri, 1024)
        }
        if (bitmap == null) {
            context.showToast("图片预览失败，请重新选择")
        } else {
            preview = bitmap.asImageBitmap()
        }
    }

    fun launchCamera() {
        if (loading) return
        try {
            val uri = ImageUtils.createCameraUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (exception: Exception) {
            context.showToast("无法打开相机：${exception.message ?: "请稍后重试"}")
        }
    }

    fun launchGallery() {
        if (loading) return
        try {
            galleryLauncher.launch("image/*")
        } catch (exception: Exception) {
            context.showToast("无法打开相册：${exception.message ?: "请稍后重试"}")
        }
    }

    fun submit() {
        if (loading) return
        val currentUri = imageUri
        val questionText = question.trim()
        if (currentUri == null && questionText.isBlank()) {
            context.showToast("请先拍照、选择图片或补充题目文字")
            return
        }

        loading = true
        error = null
        answer = null
        savedThumbnailNotice = false

        scope.launch {
            try {
                var uploadBase64: String? = null
                var thumbnailBase64: String? = null

                if (currentUri != null) {
                    val prepared = withContext(Dispatchers.IO) {
                        ImageUtils.prepareForUpload(context, currentUri)
                    }
                    thumbnailBase64 = prepared.thumbnailBase64
                    if (!isMock && settings.multimodalEnabled) {
                        uploadBase64 = prepared.uploadBase64
                    }
                }

                val result = QaApi.answerQuestion(
                    settings = settings,
                    question = questionText,
                    imageBase64 = uploadBase64,
                    type = TYPE_PHOTO
                )

                answer = result
                val record = QaRecord(
                    id = UUID.randomUUID().toString(),
                    type = TYPE_PHOTO,
                    question = questionText.ifBlank { "（图片题目）" },
                    answer = result,
                    imageThumb = thumbnailBase64,
                    favorite = false,
                    createdAt = System.currentTimeMillis()
                )
                val thumbnailDropped = onSaveRecord(record)
                savedThumbnailNotice = thumbnailDropped
                if (thumbnailDropped) {
                    context.showToast("图片缩略图较大，已只保存文字答案")
                }
            } catch (exception: Exception) {
                error = exception.message ?: "识别并解答失败，请重试"
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "拍照搜题",
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
                text = "拍下题目，或从相册选一张图",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isMock) {
                    "当前是模拟模式，会生成本地示例答案，不会上传图片。"
                } else if (settings.multimodalEnabled) {
                    "将压缩图片并作为多模态内容发送到你配置的接口。"
                } else {
                    "多模态已关闭，本次只发送补充文字。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val image = preview
                        if (image != null) {
                            Image(
                                bitmap = image,
                                contentDescription = "题目图片预览",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text("🖼️", style = MaterialTheme.typography.headlineLarge)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "拍照或从相册选择题目图片",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "图片会自动压缩，最长边不超过 1024",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { launchCamera() },
                            modifier = Modifier.weight(1f),
                            enabled = !loading
                        ) {
                            Text("拍照")
                        }
                        OutlinedButton(
                            onClick = { launchGallery() },
                            modifier = Modifier.weight(1f),
                            enabled = !loading
                        ) {
                            Text("从相册选择")
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("补充题目文字") },
                placeholder = { Text("如果识图接口不可用，可手动补充题干") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(18.dp),
                enabled = !loading
            )

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !loading && (imageUri != null || question.isNotBlank())
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("正在识别并解答...")
                } else {
                    Text("识别并解答", fontWeight = FontWeight.SemiBold)
                }
            }

            if (isMock) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "提示：在设置中关闭模拟模式并填写 API Key，即可调用真实接口。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                SectionHeader(title = "本次解答")
                Spacer(Modifier.height(10.dp))
                AnswerCard(
                    answer = answer.orEmpty(),
                    isMock = isMock,
                    typing = false
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (savedThumbnailNotice) {
                            "已自动保存（仅文字）"
                        } else {
                            "已自动保存到历史记录"
                        },
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
