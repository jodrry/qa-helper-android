# 答题助手（qa-helper-android）

一个纯原生的 Android 答题助手示例：拍照搜题、AI 文字解答、多轮对话答疑、模拟流式输出、Markdown/LaTeX 富文本答案、本地历史记录和可配置的 OpenAI 兼容接口。项目默认开启模拟模式，没有 API Key 也能完整演示。

- 包名：`com.example.qahelper`
- 应用名：答题助手
- 技术栈：Kotlin + Jetpack Compose + Material3 + OkHttp + org.json
- minSdk 24 / targetSdk 34 / compileSdk 34
- JDK 17 / Gradle 8.7 / AGP 8.5.2 / Kotlin 1.9.24
- Compose BOM 2024.06.00
- Markwon 4.6.2（Markdown + LaTeX 渲染）
- 不包含 Firebase、付费 OCR SDK、本地大模型或 WebView

## 功能

### 首页

- 上方大卡片：拍照搜题模式
- 下方大卡片：AI 解答模式
- 第三个模式：对话答疑模式（支持连续追问）
- 最近答案区域展示最近 3 条记录，点击进入详情
- 顶部提供设置入口，悬浮按钮和顶部按钮均可进入历史记录

### 拍照搜题

- 系统相机拍照：`ActivityResultContracts.TakePicture` + `FileProvider`
- 相册选择：`ActivityResultContracts.GetContent`，`image/*`
- 图片预览；压缩最长边至 1024、JPEG 质量 70，再转 Base64
- 可手动补充题干
- 配置 API Key 且开启多模态时，以 OpenAI 兼容格式发送 `text + image_url`
- 未配置 Key 或开启模拟模式时，使用本地模拟回答，并标注「模拟模式」
- loading 期间按钮禁用，自动保存答案，失败可重试

### AI 解答

- 多行题目输入
- 「提交解答」「清空」按钮
- 硬件键盘 Ctrl+Enter 提交
- 答案使用打字机效果逐字展示
- 自动保存历史；失败可重试

### 对话答疑

- 围绕同一道题连续追问，自动携带最近 12 条 user/assistant 消息作为上下文
- 用户/助手气泡区分，自动滚动到最新消息
- 支持 Ctrl+Enter 发送、失败重试、清空当前对话二次确认
- 每轮助手回答自动保存到历史记录（type 仍为 `ai`）
- 对话上下文保存在 `SharedPreferences`，key：`qa_app_chat_v1`
- 模拟模式会显示已携带的上下文轮次，便于演示多轮交互

### 答案富文本渲染

- 详情页、拍照搜题和 AI 解答的答案支持 Markdown：加粗、斜体、标题、列表、引用、行内代码、代码块、链接
- 支持 LaTeX 行内公式 `$...$`、`\(...\)`，以及块级公式 `$$...$$`、`\[...\]`
- 渲染颜色、代码块背景、字号跟随 Material3 深色/浅色主题
- Markdown 在原生 TextView 中渲染，不使用 WebView
- LaTeX 依赖 Markwon `ext-latex`，其传递依赖 `jlatexmath-android` 为 GPL-2.0；如需闭源分发请先评估许可证或替换渲染器

### 历史记录

- 保存到 `SharedPreferences`，key：`qa_app_history_v1`
- 最多保留 200 条，超出自动删除最旧记录
- 按时间倒序；全部 / 收藏筛选
- 详情、复制、收藏/取消收藏、删除单条、清空全部
- 删除和清空都有二次确认
- 图片只保存小尺寸缩略图；容量过大时自动只保留文字并提示
- 空状态：`还没有保存的答案，去搜一道题吧`

### 设置

- 保存到 `SharedPreferences`，key：`qa_app_settings_v1`
- API Endpoint（默认 `https://api.deepseek.com/chat/completions`）
- API Key（密码输入，仅保存在本机）
- 模型名称（默认 `deepseek-chat`）
- 模拟模式，默认开启
- 多模态开关，默认开启

## 安装 APK

1. 打开 GitHub 仓库的 **Actions** 页面，进入最近一次成功的 **Build Debug APK**。
2. 在页面底部下载 artifact：`qa-helper-debug-apk`。
3. 解压后得到 `app-debug.apk`，传到 Android 手机安装。
4. 首次安装需要在系统设置中允许「安装未知来源应用」。
5. 如果手机提示“应用未安装”，先卸载旧版本或确认系统版本不低于 Android 7.0（API 24）。

APK 在构建产物中的路径固定为：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 配置真实 API

进入应用右上角「设置」：

- **API Endpoint**：你的 OpenAI 兼容地址，例如 `https://api.deepseek.com/chat/completions`
- **API Key**：你自己的 Key，仅写入本机 SharedPreferences，不会提交到仓库
- **模型名称**：例如 `deepseek-chat`，也可以换成服务商支持的模型
- **模拟模式**：关闭后才会请求真实接口；没有 Key 时仍会自动回退到模拟回答
- **启用多模态**：拍照搜题时把压缩后的图片作为 `image_url` 发送

请求格式：

```json
{
  "model": "deepseek-chat",
  "messages": [
    {
      "role": "system",
      "content": "你是一个严谨的答题助手，请给出清晰、分步骤的解答。"
    },
    {
      "role": "user",
      "content": "题目文字"
    }
  ],
  "temperature": 0.3
}
```

请求头：

```text
Authorization: Bearer <你的 API Key>
Content-Type: application/json
```

> 注意：本项目不会内置任何真实 API Key。请勿把自己的 Key 提交到 GitHub、截图或公开日志中。若 API 服务商要求不同的鉴权方式，修改 `QaApi.kt` 中的请求封装即可。

## 使用 GitHub Actions 构建

仓库已包含 `.github/workflows/build-apk.yml`：

- push 到 `main` 或手动 `workflow_dispatch` 触发
- `ubuntu-latest`
- `actions/checkout@v4`
- `actions/setup-java@v4`，Temurin JDK 17
- `gradle/actions/setup-gradle@v4`，Gradle 8.7
- 执行 `gradle assembleDebug`
- 上传 artifact：`qa-helper-debug-apk`
- 产物路径：`app/build/outputs/apk/debug/app-debug.apk`

下载 artifact 需要登录 GitHub；artifact 默认保留 30 天。可以直接在 Actions 运行页面的 Artifacts 区域下载 zip。

## 本地构建

用 Android Studio 打开仓库根目录，等待 Gradle Sync 后运行 `app` 配置即可。

命令行构建需要 JDK 17 + Android SDK Platform 34：

```bash
gradle assembleDebug
```

本项目不需要 Gradle Wrapper；CI 中由 `gradle/actions/setup-gradle@v4` 安装 Gradle 8.7。若你希望在本地固定 Wrapper，可在 Android Studio 中执行 **File → Sync Project with Gradle Files** 后使用 `./gradlew assembleDebug`。

## 项目结构

```text
.
├── .github/workflows/build-apk.yml
├── app
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main
│       ├── AndroidManifest.xml
│       ├── java/com/example/qahelper
│       │   ├── MainActivity.kt
│       │   ├── data
│       │   │   ├── AppSettings.kt
│       │   │   ├── ChatMessage.kt
│       │   │   ├── QaRecord.kt
│       │   │   └── QaStore.kt
│       │   ├── network
│       │   │   ├── MockAnswer.kt
│       │   │   └── QaApi.kt
│       │   ├── ui
│       │   │   ├── AiScreen.kt
│       │   │   ├── ChatScreen.kt
│       │   │   ├── DetailScreen.kt
│       │   │   ├── HistoryScreen.kt
│       │   │   ├── HomeScreen.kt
│       │   │   ├── PhotoScreen.kt
│       │   │   ├── QaHelperApp.kt
│       │   │   ├── SettingsScreen.kt
│       │   │   ├── components/QaComponents.kt
│       │   │   ├── components/RichMarkdownText.kt
│       │   │   └── theme/{Color,Theme,Type}.kt
│       │   └── util
│       │       ├── AndroidUi.kt
│       │       └── ImageUtils.kt
│       └── res
│           ├── drawable/ic_launcher.xml
│           ├── values/{strings,themes}.xml
│           ├── values-night/themes.xml
│           └── xml/file_paths.xml
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── README.md
```

## 权限说明

- 只声明 `INTERNET`
- 相机使用系统相机 Intent，不申请 `CAMERA` 权限
- 相册使用系统选择器，不申请存储权限
- `FileProvider` 只暴露应用缓存目录中的拍照临时文件

## 已知限制

- 模拟模式不会真实理解题目，只用于演示交互和本地存储流程。
- 图片会先压缩；如果接口本身不支持视觉输入，请关闭多模态并补充题干文字。
- 历史记录和 API Key 都存放在本机；卸载应用会一并删除。
- 本项目是示例工程，正式发布前请自行补充隐私政策、签名配置和更严格的日志脱敏。
