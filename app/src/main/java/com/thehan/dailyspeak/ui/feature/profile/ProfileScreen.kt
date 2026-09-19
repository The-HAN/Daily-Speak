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
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.PracticeTopics
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
    onReminderTimeChange: (String) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onTtsEnabledChange: (Boolean) -> Unit,
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
                supportingText = { Text("24 小时制，例如 20:30") },
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