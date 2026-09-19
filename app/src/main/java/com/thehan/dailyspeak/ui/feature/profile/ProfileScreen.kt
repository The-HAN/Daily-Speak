package com.thehan.dailyspeak.ui.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.thehan.dailyspeak.domain.model.AccentPreference
import com.thehan.dailyspeak.domain.model.BackgroundPreset
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.PracticeTopics
import com.thehan.dailyspeak.domain.model.SpeechRecognizerMode
import com.thehan.dailyspeak.domain.model.UserSettings
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    hasBuildConfigApiKey: Boolean,
    onDailyCountChange: (Int) -> Unit,
    onLevelChange: (PracticeLevel) -> Unit,
    onAccentChange: (AccentPreference) -> Unit,
    onTopicToggle: (String) -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (String) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onTtsEnabledChange: (Boolean) -> Unit,
    onSpeechRecognizerModeChange: (SpeechRecognizerMode) -> Unit,
    onSpeechRecognizerServiceChange: (String) -> Unit,
    onBackgroundPresetChange: (BackgroundPreset) -> Unit,
    onPickBackgroundImage: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = uiState.settings

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "我的",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "调整练习节奏与内容偏好",
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SettingsCard(title = "每日练习") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("每日题量", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${settings.dailyCount} 题",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Slider(
                value = settings.dailyCount.toFloat(),
                onValueChange = { onDailyCountChange(it.roundToInt().coerceIn(1, 20)) },
                valueRange = 1f..20f,
                steps = 18,
            )
            Text(
                text = "可选 1–20 题，修改后今日题目会自动重新加载。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        SettingsCard(title = "难度") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LevelChip(
                    label = "日常",
                    selected = settings.level == PracticeLevel.DAILY,
                    onClick = { onLevelChange(PracticeLevel.DAILY) },
                    modifier = Modifier.weight(1f),
                )
                LevelChip(
                    label = "考研",
                    selected = settings.level == PracticeLevel.POSTGRADUATE,
                    onClick = { onLevelChange(PracticeLevel.POSTGRADUATE) },
                    modifier = Modifier.weight(1f),
                )
                LevelChip(
                    label = "混合",
                    selected = settings.level == PracticeLevel.MIXED,
                    onClick = { onLevelChange(PracticeLevel.MIXED) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SettingsCard(title = "口音偏好") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LevelChip(
                    label = "美音",
                    selected = settings.accent == AccentPreference.AMERICAN,
                    onClick = { onAccentChange(AccentPreference.AMERICAN) },
                    modifier = Modifier.weight(1f),
                )
                LevelChip(
                    label = "英音",
                    selected = settings.accent == AccentPreference.BRITISH,
                    onClick = { onAccentChange(AccentPreference.BRITISH) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SettingsCard(title = "界面与背景") {
            Text(
                text = "选择低饱和预设，或用本地图片替换应用背景。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            BackgroundPreset.entries.chunked(3).forEach { presets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEach { preset ->
                        LevelChip(
                            label = preset.displayName(),
                            selected = settings.backgroundImageUri.isBlank() &&
                                settings.backgroundPreset == preset,
                            onClick = { onBackgroundPresetChange(preset) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - presets.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onPickBackgroundImage,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("选择图片")
                }
                OutlinedButton(
                    onClick = onClearBackgroundImage,
                    enabled = settings.backgroundImageUri.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("移除图片")
                }
            }
            Text(
                text = if (settings.backgroundImageUri.isBlank()) {
                    "当前使用预设背景；图片不会上传。"
                } else {
                    "已启用自定义背景；图片仅由本机读取，不会上传。"
                },
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        SettingsCard(title = "语音识别服务") {
            Text(
                text = "选择用于把录音转成文字的系统服务。若某种服务不支持已有录音文件，请切换其他服务后重试。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LevelChip(
                    label = "系统默认",
                    selected = settings.speechRecognizerMode == SpeechRecognizerMode.SYSTEM_DEFAULT,
                    onClick = { onSpeechRecognizerModeChange(SpeechRecognizerMode.SYSTEM_DEFAULT) },
                    modifier = Modifier.weight(1f),
                )
                LevelChip(
                    label = "设备端",
                    selected = settings.speechRecognizerMode == SpeechRecognizerMode.ON_DEVICE,
                    onClick = { onSpeechRecognizerModeChange(SpeechRecognizerMode.ON_DEVICE) },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = if (uiState.isOnDeviceRecognitionAvailable) {
                    "设备端识别可用；Android 12 及以上才支持。"
                } else {
                    "设备端识别不可用或系统版本低于 Android 12。"
                },
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.speechRecognizerServices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "已安装服务（${uiState.speechRecognizerServices.size}）",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                uiState.speechRecognizerServices.forEach { service ->
                    FilterChip(
                        selected = settings.speechRecognizerMode == SpeechRecognizerMode.SELECTED_SERVICE &&
                            settings.speechRecognizerComponent == service.componentName,
                        onClick = { onSpeechRecognizerServiceChange(service.componentName) },
                        label = {
                            Column {
                                Text(service.label)
                                Text(
                                    text = service.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "未检测到已安装的语音识别服务，请安装并启用 Google 语音服务或厂商语音服务。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SettingsCard(title = "话题偏好") {
            Text(
                text = "不选择时使用全部话题。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            PracticeTopics.all.chunked(3).forEach { topics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    topics.forEach { topic ->
                        FilterChip(
                            selected = topic in settings.topics,
                            onClick = { onTopicToggle(topic) },
                            label = { Text(topic, maxLines = 1) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - topics.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SettingsCard(title = "提醒与朗读") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("每日提醒", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (settings.reminderEnabled) {
                            "已开启，每天 ${settings.reminderTime} 提醒"
                        } else {
                            "关闭"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.reminderEnabled,
                    onCheckedChange = onReminderEnabledChange,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            var reminderDraft by rememberSaveable(settings.reminderTime) {
                mutableStateOf(settings.reminderTime)
            }
            OutlinedTextField(
                value = reminderDraft,
                onValueChange = { value ->
                    reminderDraft = value
                    onReminderTimeChange(value)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("每日提醒时间") },
                supportingText = { Text("24 小时制，例如 20:30；修改后会自动重新调度。") },
                singleLine = true,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("启用参考回答朗读", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "开启后按口音偏好朗读问题和参考回答。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.ttsEnabled,
                    onCheckedChange = onTtsEnabledChange,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SettingsCard(title = "DeepSeek API") {
            var apiKeyDraft by rememberSaveable { mutableStateOf("") }
            Text(
                text = if (hasBuildConfigApiKey) {
                    "已从 local.properties 读取 API Key；下方可保存仅本机使用的覆盖值。"
                } else {
                    "未检测到 BuildConfig API Key。可在 local.properties 或下方配置。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = apiKeyDraft,
                onValueChange = { apiKeyDraft = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key 覆盖值") },
                placeholder = { Text("sk-...") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            Button(
                onClick = { onSaveApiKey(apiKeyDraft) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                Text(if (apiKeyDraft.isBlank()) "清除覆盖值" else "保存到本机")
            }
            Text(
                text = "MVP 使用 DataStore 保存；正式发布前应迁移到 Android Keystore 加密存储。",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun LevelChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
    )
}

private fun BackgroundPreset.displayName(): String = when (this) {
    BackgroundPreset.DEFAULT -> "默认"
    BackgroundPreset.MIST -> "雾白"
    BackgroundPreset.MINT -> "薄荷"
    BackgroundPreset.PEACH -> "暖杏"
    BackgroundPreset.SKY -> "天空"
}
