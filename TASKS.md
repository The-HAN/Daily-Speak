# 任务拆分

状态说明：`[x]` 已完成，`[~]` 部分完成或需要真机/外部服务验证，`[ ]` 待完成。

## 阶段 0：工程与工具链

- [x] Gradle Kotlin DSL 工程和 Version Catalog。
- [x] Compose、Material 3、Hilt、Room、Retrofit、DataStore 基础配置。
- [x] 将 Android SDK、JDK、Android 用户目录和 Gradle 缓存放在 Git 仓库父目录 `.tooling`。
- [x] `local.properties` 指向仓库外 Android SDK，并保持 Git 忽略。
- [x] 验证 `:app:assembleDebug` 和 `:app:testDebugUnitTest`。
- [x] Room schema 导出配置。
- [x] 第一批 Repository 和复习筛选 JVM 单元测试。
- [x] DeepSeek 生成题目缓存和 Room 持久化的 JVM 单元测试。

## 阶段 1：应用骨架

- [x] 今日 / 复习 / 我的底部导航。
- [x] 深色模式、边到边和主题骨架。
- [x] 今日题本地原创题库和稳定选题。
- [x] Room 题目缓存、Attempt、Feedback 和 Favorite 表。
- [x] DataStore 设置持久化。
- [x] 我的页设置控件。
- [x] MediaRecorder 录音和本地 Attempt 保存。
- [x] 反馈页基础导航和本地反馈展示。
- [ ] Compose UI 自动化测试。
- [ ] 红米 K60 真机截图、布局和性能验收。

## 阶段 2：语音与反馈闭环

- [x] `SpeechRecognizerService` 契约。
- [x] Android 13+ `EXTRA_AUDIO_SOURCE` 文件转写适配器。
- [x] 录音转写失败时的可恢复错误和重试入口。
- [~] 红米 K60 真机验证系统识别服务和音频文件来源兼容性。
- [x] `PronunciationEvaluator` 契约。
- [x] 基于识别置信度和音频特征的粗略本地评分。
- [ ] 音素级准确度、重音、连读和韵律评测适配器。
- [x] `DeepSeekService` 契约和 Retrofit 封装。
- [x] 把 DeepSeek 接入今日动态出题和反馈页按需生成建议流程，调用前由用户确认。
- [ ] 用真实 DeepSeek 官方文档和测试账号确认模型名、接口路径和 JSON 格式。
- [x] 反馈页折叠卡片、评分、建议和参考回答展示。
- [x] Android TTS 示范朗读和口音偏好。
- [x] TTS 初始化/播放状态监听、媒体音频通道、英语语音包缺失和播放失败提示。
- [x] 可配置系统默认、设备端和指定语音识别服务。
- [x] 可配置预设背景和本地自定义背景图片。
- [x] 云端文本上传前的明确授权对话框和隐私说明；录音文件不上传。

## 阶段 3：复习与习惯

- [x] Favorite 表和收藏切换。
- [x] 低分题筛选与复习入口。
- [x] 历史记录列表。
- [x] 日期、话题、难度筛选。
- [x] 每日提醒通知、Android 13 通知权限和重启后恢复调度。
- [ ] 连续打卡计算和今日完成状态。
- [ ] 复习计划/间隔重复策略。

## 阶段 4：生产化

- [~] Room schema 已导出；正式 Migration 尚未实现，当前使用 destructive fallback。
- [ ] Android Keystore 保存 API Key。
- [ ] 数据导出和彻底清除。
- [ ] 后台任务、网络重试、超时和离线队列。
- [ ] 隐私政策和第三方 SDK 合规清单。
- [ ] 性能基线、崩溃监控和发布构建。
- [x] 应用图标、启动页、版本号和 Release 签名配置。
- [x] 构建并校验 v0.1.0 可安装 Release APK。
- [x] 构建并校验 v0.2.0 可安装 Release APK。
