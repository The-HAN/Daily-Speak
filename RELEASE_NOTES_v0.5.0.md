# Daily-Speak v0.5.0

Release date: 2026-09-29

本版重点修复 AI 出题超时问题，并改进 DeepSeek API 配置体验：应用会从官方模型列表接口读取可用模型供用户选择，API Key 与模型选择会持久保存。应用图标也已重新设计。

## 本版更新

### DeepSeek 出题与超时修复

- 延长 OkHttp 连接、读取、写入和总调用超时，减少生成较长回答时过早中断。
- 每日题目改为分批生成，每批最多 5 题、最多 4 批，兼顾最多 20 题的设置和单次请求稳定性。
- 每批请求加入随机生成标识并携带已生成问句，降低跨批重复和模板化表达。
- 对超时、网络异常和 HTTP 错误提供更明确的中文提示。
- Base URL 为空或末尾缺少斜杠时自动回退或补全，避免 Retrofit 路径拼接异常。

### 自动读取并选择模型

- 新增 DeepSeek 官方 `GET /models` 接口调用。
- 在“我的 → DeepSeek API”中点击“保存并读取可用模型”，自动展示账号可用的模型列表。
- 模型名称不再要求用户手工输入；点击列表中的模型即可选择并保存。
- API Key 和模型选择写入 DataStore，重新进入应用后继续使用上次配置。
- 若 `local.properties` 提供了构建期 API Key，首次进入配置页时也会尝试读取模型列表。
- 不在代码中硬编码具体模型 ID，最终以 DeepSeek 官方接口实时返回为准。

### 应用图标

- 重新设计每日口语练习主题图标，使用深绿色背景、白色语音气泡/声波和黄色语音点。
- 支持 Android 自适应图标、圆形图标和 Android 13+ 单色主题图标。
- 同步更新应用内通知图标风格。

### 测试与文档

- 补充 README、架构文档、项目结构和任务记录。
- 更新版本号为 `versionCode 5` / `versionName 0.5.0`。

## APK

文件：`Daily-Speak-v0.5.0.apk`

包名：`com.thehan.dailyspeak`

版本：`versionCode 5` / `versionName 0.5.0`

支持：Android 8.0+（minSdk 26），目标 SDK 36

SHA-256：

```text
E360BA71EDE758A9D088869775CE737DD7DDCAF90E59D750C9B5C29D746D7C10
```

APK 使用仓库外保存的 Release 密钥签名；仓库和 GitHub 不包含签名密钥、密码、API Key 或 APK。

## 安装

1. 下载 `Daily-Speak-v0.5.0.apk`。
2. 在手机“设置”中允许当前浏览器或文件管理器安装未知来源应用。
3. 打开 APK 并按提示安装。
4. 首次录音时允许麦克风权限。
5. 在“我的 → DeepSeek API”填写 API Key，点击“保存并读取可用模型”，选择模型。
6. 确保 API Key 和模型选择已保存，再开始 AI 出题。

## 验证结果

- `:app:testDebugUnitTest` 通过，共 19 个 JVM 单元测试。
- `:app:lintDebug` 通过。
- `:app:assembleDebug` 和 `:app:assembleRelease` 通过。
- Release APK 已通过 APK Signature Scheme v2 签名校验和 zipalign 校验。
- APK 元数据确认包名为 `com.thehan.dailyspeak`，版本为 `5 / 0.5.0`，minSdk 26，targetSdk 36。

## 已知限制与验证说明

- `/models` 和实际出题效果依赖有效 DeepSeek API Key、账号权限、可用模型及网络环境；发布构建未内置 API Key，因此需在应用内使用真实账号完成最终联调。
- 模型 ID 不硬编码，以 DeepSeek 官方 `/models` 返回结果为准。
- DeepSeek 服务端生成延迟会随模型、题量和网络状态变化；应用已拆分请求并延长超时，但无法消除服务端排队或限流。
- Android `SpeechRecognizer` 对已有音频文件的转写能力依赖系统实现；部分识别服务可能拒绝音频文件来源。
- 默认发音评分是本地粗略估算，不是音素级专业评测。
- API Key 当前保存在本机 DataStore；正式生产版计划迁移到 Android Keystore。

## 隐私说明

- 录音默认只保存在应用私有目录，不上传。
- 自定义背景图片仅在本机读取，不上传。
- 本地文本建议完全离线生成。
- DeepSeek 仅接收用户确认后的文本内容，不接收录音文件。
- API Key、录音、转写和反馈不得提交到 Git 或写入普通日志。