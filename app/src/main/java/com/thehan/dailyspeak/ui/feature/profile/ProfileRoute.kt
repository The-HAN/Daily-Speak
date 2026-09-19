package com.thehan.dailyspeak.ui.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thehan.dailyspeak.BuildConfig

@Composable
fun ProfileRoute(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileScreen(
        uiState = uiState,
        hasBuildConfigApiKey = BuildConfig.DEEPSEEK_API_KEY.isNotBlank(),
        onDailyCountChange = viewModel::setDailyCount,
        onLevelChange = viewModel::setLevel,
        onAccentChange = viewModel::setAccent,
        onTopicToggle = viewModel::toggleTopic,
        onReminderTimeChange = viewModel::setReminderTime,
        onSaveApiKey = viewModel::saveDeepSeekApiKey,
        onTtsEnabledChange = viewModel::setTtsEnabled,
        modifier = modifier,
    )
}