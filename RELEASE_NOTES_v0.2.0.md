# Daily-Speak v0.2.0

Release date: 2026-09-19

本版重点修复问句朗读无声问题，增加可配置的语音识别服务，并支持替换应用背景。应用仍以离线题库、录音、转写、粗略发音评分和反馈闭环为基础。

## 本版更新

### 问句与参考回答朗读

- 系统 TTS 改用媒体音频属性：`USAGE_MEDIA` + `CONTENT_TYPE_SPEECH`。
- 增加 TTS 初始化、开始、完成和错误回调状态。
- 英语语音包缺失、TTS 初始化失败或朗读启动失败时显示明确提示。
- TTS 关闭时不再静默失败，而是引导用户到“我的 → 提醒与朗读”开启。
- Android Manifest 增加 `TTS_SERVICE` 查询声明。

### 可配置语音识别服务

- 支持“系统默认”“设备端识别”和“指定已安装服务”三种模式。
- “我的”页通过 `PackageManager` 列出声明 `android.speech.RecognitionService` 的服务。
- Android 12+ 支持选择设备端识别；Android 13+ 继续使用 `EXTRA_AUDIO_SOURCE` 转写已有录音文件。
- 所选服务不可用时显示可恢复错误，并允许切换服务后重试转写与评分。

### 界面与背景

- 增加默认、雾白、薄荷、暖杏、天空五套低饱和渐变背景。
- 支持通过系统文件选择器选择本地图片作为自定义背景。
- 使用 `content://` URI 和持久化读取权限，重启后继续读取。
- 自定义图片使用浅色或深色遮罩，保证主要文字可读性。
- 背景仅在本机读取，不上传。

## APK

文件：`Daily-Speak-v0.2.0.apk`

包名：`com.thehan.dailyspeak`

版本：`versionCode 2` / `versionName 0.2.0`

支持：Android 8.0+（minSdk 26），目标 SDK 36

SHA-256：

```text
0DCC0268EE722576F98C9163AA7D1D21100DDFDD41EDAAC8BCAF1116C62DD1FE
```

APK 使用仓库外保存的 Release 密钥签名，仓库和 GitHub 不包含签名密钥或密码。

## 安装

1. 下载 `Daily-Speak-v0.2.0.apk`。
2. 在手机“设置”中允许当前浏览器或文件管理器安装未知来源应用。
3. 打开 APK 并按提示安装。
4. 首次录音时允许麦克风权限；Android 13+ 首次开启提醒时允许通知权限。
5. 在“我的 → 界面与背景”选择预设背景或本地图片。
6. 在“我的 → 语音识别服务”选择系统默认、设备端识别或已安装的指定服务。

## 已知限制

- 当前没有在红米 K60 真机上完成最终安装、TTS 音量和厂商识别服务验收。
- Android `SpeechRecognizer` 对已有音频文件的转写能力仍依赖系统实现；部分厂商服务可能拒绝 `EXTRA_AUDIO_SOURCE`。
- 默认发音评分是本地粗略估算，不是音素级专业评测。
- 自定义背景会按最长边降采样到 2048 像素以控制内存，超大图片不会保留原始分辨率。
- DeepSeek 模型名、接口路径和请求格式仍需使用真实账号按官方文档完成联调。
- DeepSeek API Key 当前保存在本机 DataStore；正式生产版计划迁移到 Android Keystore。
- Room 当前使用 destructive migration fallback，正式发布前需要完成数据库迁移策略。

## 隐私说明

- 录音默认只保存在应用私有目录，不上传。
- 自定义背景图片仅在本机读取，不上传。
- DeepSeek 仅接收用户确认后的文本内容，不接收录音文件。
- API Key、录音、转写和反馈不得提交到 Git 或写入普通日志。
