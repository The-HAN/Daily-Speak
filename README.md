# Daily-Speak：考研口语每日练

Daily-Speak 是一个以“每日一问 + 录音回答 + 反馈复习”为核心的 Android 英语口语练习应用。当前目标设备为 Android 手机，优先适配红米 K60；技术栈为 Kotlin、Jetpack Compose、Material 3、MVVM、Hilt、Room、DataStore、Retrofit/OkHttp 和 Kotlinx Serialization。

## 当前可运行能力

- 今日 / 复习 / 我的三页 Compose 底部导航，支持深色模式和边到边显示。
- 今日题目从本地原创题库加载，按日期、难度和话题偏好稳定选题。
- Room 持久化题目、录音记录、反馈和收藏。
- 我的页持久化每日题量、难度、口音、话题、提醒时间、TTS 开关和 DeepSeek API Key 覆盖值。
- 请求 `RECORD_AUDIO` 权限后使用 `MediaRecorder` 录音，并保存 `.m4a` 到应用私有目录。
- Android 12+ 文件来源 `SpeechRecognizer` 适配器；识别失败会返回明确错误，不会伪造 transcript。
- 默认 `PronunciationEvaluator` 使用识别置信度、音量、停顿、语速和动态范围做粗略评分。
- 录音完成后保存 Attempt，生成本地基础反馈，并打开反馈页。
- 反馈页支持折叠卡片、评分展示、参考回答 TTS、重录、下一题和收藏。
- 复习页支持低分题、收藏题、历史记录以及日期、话题、难度筛选。

## 尚未接入或仍需完善

- `DeepSeekService` 已完成 Retrofit 封装，但尚未接入“今日自动出题”和“回答后云端反馈”的 UI 编排。
- DeepSeek 模型名、接口路径和请求格式仍需以官方文档和真实账号联调结果为准。
- Android `SpeechRecognizer` 的音频文件来源能力依赖 Android 12+ 和设备上的识别服务，需要在红米 K60 真机验证。
- 默认发音评分是粗略估算，不宣称为音素级评测；云端评测接口仍待实现。
- 每日提醒通知、连续打卡计算、数据导出/清除尚未实现。
- API Key 当前使用 DataStore 保存本机覆盖值，正式发布前应迁移到 Android Keystore。
- 当前没有云音频上传；后续发送转写或音频前必须增加明确授权提示。

## 项目文档

- [项目结构与数据流](PROJECT_STRUCTURE.md)
- [产品需求](PRD.md)
- [架构设计](ARCHITECTURE.md)
- [任务拆分](TASKS.md)

## 依赖工具链位置

Git 仓库位于：

```text
D:\github\The-HAN\Daily-speak\Daily-Speak
```

Android SDK、JDK、Android 用户目录和 Gradle 缓存位于 Git 仓库外的父目录：

```text
D:\github\The-HAN\Daily-speak\.tooling\
├── android-sdk\
├── android-user-home\
├── gradle-user-home\
└── jdk-24\
```

这些目录不应提交到 Git。当前构建已验证可使用 JDK 24、Android SDK 36 和 Gradle Wrapper 8.14.3。

## 本地配置

复制 `local.properties.example` 为 `local.properties`。该文件已被 `.gitignore` 忽略，不能提交。

```properties
sdk.dir=D:/github/The-HAN/Daily-speak/.tooling/android-sdk

DEEPSEEK_API_KEY=
DEEPSEEK_BASE_URL=https://api.deepseek.com/
# 必须填写 DeepSeek 官方文档确认可用的模型名
DEEPSEEK_MODEL=
```

API Key 有两种来源：

1. `local.properties` 中的 `DEEPSEEK_API_KEY`，构建时注入 `BuildConfig`。
2. “我的”页保存的本机覆盖值，优先于 `BuildConfig`。

代码中不保存真实 API Key，不把 Authorization 写入日志。模型名不在源码中写死，必须由 `DEEPSEEK_MODEL` 提供。

## 构建与测试

在 PowerShell 中执行：

```powershell
Remove-Item Env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue
$env:JAVA_HOME = 'D:\github\The-HAN\Daily-speak\.tooling\jdk-24'
$env:ANDROID_HOME = 'D:\github\The-HAN\Daily-speak\.tooling\android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:ANDROID_USER_HOME = 'D:\github\The-HAN\Daily-speak\.tooling\android-user-home'
$env:GRADLE_USER_HOME = 'D:\github\The-HAN\Daily-speak\.tooling\gradle-user-home'

Set-Location 'D:\github\The-HAN\Daily-speak\Daily-Speak'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon
```

Debug APK 位于：

```text
app\build\outputs\apk\debug\app-debug.apk
```

如果 Gradle 显示 `C:\.android` 不可写，清理当前终端的 `ANDROID_PREFS_ROOT` 后重新执行即可；这不是应用编译错误。

## 真机运行

1. Redmi K60 开启开发者选项和 USB 调试。
2. 使用 `adb devices` 确认设备已授权。
3. 安装：`adb install -r app\build\outputs\apk\debug\app-debug.apk`。
4. 首次录音时允许麦克风权限。
5. 在“我的”页调整每日题量、难度和话题，返回“今日”页确认题目重新加载。
6. 检查浅色、深色、字体放大和边到边布局是否无遮挡。
7. 录音后查看是否进入反馈页，并确认"重录 / 下一题 / 收藏 / TTS"可用。

## 替换发音评测服务

默认实现位于：

```text
app/src/main/java/com/thehan/dailyspeak/core/speech/DefaultPronunciationEvaluator.kt
```

新增云端或本地音素级实现时，实现以下接口：

```kotlin
interface PronunciationEvaluator {
    suspend fun evaluate(audioFile: File, expectedText: String): PronunciationScore
}
```

如果服务还能返回识别转写和置信度，可同时实现 `TranscriptAwarePronunciationEvaluator`。完成后在 `SpeechModule` 中把 `@Binds` 指向新实现即可，UI 和领域层不需要改动。

云端评测必须：

- 在执行前明确告诉用户会发送哪些音频或文本数据。
- 取得用户同意后才上传。
- 不把音频、转写或 API 响应中的敏感内容写入普通日志。
- 在失败时回退到本地评分或显示可恢复错误。

## SpeechRecognizer 限制

Android `SpeechRecognizer` 的基础公开 API 主要面向实时麦克风识别，不保证所有设备都能接受任意音频文件。当前实现使用 Android 12+ 的 `EXTRA_AUDIO_SOURCE` 契约，并要求设备安装可用的识别服务；如果平台或厂商实现拒绝该能力，会显示错误并保留 Attempt，用户可重试或后续替换为本地/云端识别适配器。

不要在该接口中返回伪造转写。要支持旧设备，优先改造为“录音与实时识别同一会话”或接入真正的文件识别服务。

## 合规与隐私

- 录音默认存储在应用私有目录，不上传。
- 调用云端 API 前必须取得用户明确同意。
- 不抓取或存储受版权保护的完整题库；公开来源必须遵守 robots.txt 和授权条款。
- API Key、录音、转写和反馈属于敏感数据，不应写入日志或提交到 Git。