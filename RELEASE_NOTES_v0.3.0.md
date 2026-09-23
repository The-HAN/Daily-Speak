# Daily-Speak v0.3.0

Release date: 2026-09-23

本版增加 App 内 DeepSeek 模型配置，改善每日题目随机轮换，并提高预设背景之间的辨识度。

## 本版更新

### App 内配置 API 模型

- 在“我的 → DeepSeek API”新增“模型名称”输入框。
- App 内保存的模型名称优先于 `local.properties` 中的 `DEEPSEEK_MODEL`。
- 清空 App 内模型配置后自动回退到构建配置。
- 不硬编码具体模型 ID；模型名称必须以 DeepSeek 官方文档和实际账号可用结果为准。

### 每日题目随机轮换

- 本地题库先使用稳定哈希打乱，再按日期以完整题组轮换，不再只是每天向后移动一道题。
- 同一天、同一难度和话题条件下重复打开 App 会保持相同题组，方便继续练习。
- 日期变化后默认题组会变化；候选题库足够大时，相邻日期不会长期只替换一道题。
- DeepSeek 出题请求增加随机生成标识和原创变化要求，降低重复生成相同表达的概率。
- 增加同日稳定和跨日变化的 JVM 单元测试。

### 背景主题

- 默认、雾白、薄荷、暖杏、天空五套背景增大色相、饱和度和明度差异。
- 仍保留低至中饱和风格，并继续支持浅色、深色模式和本地图片背景。

## APK

文件：`Daily-Speak-v0.3.0.apk`

包名：`com.thehan.dailyspeak`

版本：`versionCode 3` / `versionName 0.3.0`

支持：Android 8.0+（minSdk 26），目标 SDK 36

SHA-256：

```text
4DFE22D8C494C0429AD951DBB74313E21BF1F917011FBE833337E01F7F6FC1A2
```

APK 使用仓库外保存的 Release 密钥签名，仓库和 GitHub 不包含签名密钥或密码。

## 安装

1. 下载 `Daily-Speak-v0.3.0.apk`。
2. 在手机“设置”中允许当前浏览器或文件管理器安装未知来源应用。
3. 打开 APK 并按提示安装。
4. 在“我的 → DeepSeek API”填写 API Key 和 DeepSeek 官方模型 ID。
5. 在“我的 → 界面与背景”切换预设或本地图片背景。

## 验证结果

- `:app:lintDebug` 通过。
- `:app:testDebugUnitTest` 通过，共 16 个 JVM 单元测试。
- `:app:assembleDebug` 和 `:app:assembleRelease` 通过。
- Release APK 已通过 APK Signature Scheme v2 签名校验。

## 已知限制

- 尚未在红米 K60 真机上完成最终安装、TTS 音量和厂商语音识别服务验收。
- DeepSeek 模型 ID、接口路径和请求格式仍需使用真实账号按官方文档完成联调。
- Android `SpeechRecognizer` 对已有音频文件的转写能力仍依赖系统实现。
- 默认发音评分是本地粗略估算，不是音素级专业评测。
- API Key 当前保存在本机 DataStore；正式生产版计划迁移到 Android Keystore。

## 隐私说明

- 录音默认只保存在应用私有目录，不上传。
- 自定义背景图片仅在本机读取，不上传。
- DeepSeek 仅接收用户确认后的文本内容，不接收录音文件。
- API Key、录音、转写和反馈不得提交到 Git 或写入普通日志。
