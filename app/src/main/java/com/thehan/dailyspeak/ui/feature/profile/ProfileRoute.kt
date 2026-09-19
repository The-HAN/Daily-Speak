package com.thehan.dailyspeak.ui.feature.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thehan.dailyspeak.BuildConfig
import com.thehan.dailyspeak.R

@Composable
fun ProfileRoute(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backgroundImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }.onFailure {
                Toast.makeText(
                    context,
                    "图片已选择，但系统未授予长期访问权限，重启应用后可能需要重新选择。",
                    Toast.LENGTH_LONG,
                ).show()
            }
            viewModel.setBackgroundImageUri(uri.toString())
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.setReminderEnabled(granted)
        if (!granted) {
            Toast.makeText(
                context,
                context.getString(R.string.reminder_permission_denied),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    val onReminderEnabledChange: (Boolean) -> Unit = { enabled ->
        when {
            !enabled -> viewModel.setReminderEnabled(false)
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED -> viewModel.setReminderEnabled(true)
            else -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    ProfileScreen(
        uiState = uiState,
        hasBuildConfigApiKey = BuildConfig.DEEPSEEK_API_KEY.isNotBlank(),
        onDailyCountChange = viewModel::setDailyCount,
        onLevelChange = viewModel::setLevel,
        onAccentChange = viewModel::setAccent,
        onTopicToggle = viewModel::toggleTopic,
        onReminderEnabledChange = onReminderEnabledChange,
        onReminderTimeChange = viewModel::setReminderTime,
        onSaveApiKey = viewModel::saveDeepSeekApiKey,
        onTtsEnabledChange = viewModel::setTtsEnabled,
        onSpeechRecognizerModeChange = viewModel::setSpeechRecognizerMode,
        onSpeechRecognizerServiceChange = viewModel::selectSpeechRecognizerService,
        onBackgroundPresetChange = viewModel::setBackgroundPreset,
        onPickBackgroundImage = { backgroundImageLauncher.launch(arrayOf("image/*")) },
        onClearBackgroundImage = viewModel::clearBackgroundImage,
        modifier = modifier,
    )
}
