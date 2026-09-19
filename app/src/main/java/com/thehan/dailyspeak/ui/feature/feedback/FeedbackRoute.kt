package com.thehan.dailyspeak.ui.feature.feedback

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FeedbackRoute(
    onBack: () -> Unit,
    onRetryRecording: () -> Unit,
    onNextQuestion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedbackViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeepSeekConsent by rememberSaveable { mutableStateOf(false) }

    FeedbackScreen(
        uiState = uiState,
        onBack = onBack,
        onRetryRecording = onRetryRecording,
        onNextQuestion = onNextQuestion,
        onToggleFavorite = viewModel::toggleFavorite,
        onPlayReferenceAnswer = viewModel::playReferenceAnswer,
        onGenerateAiFeedback = { showDeepSeekConsent = true },
        onRetryLoad = viewModel::load,
        modifier = modifier,
    )

    if (showDeepSeekConsent) {
        AlertDialog(
            onDismissRequest = { showDeepSeekConsent = false },
            title = { Text("发送文本给 DeepSeek？") },
            text = {
                Text(
                    "将发送当前英文问题、你的语音转写文本和本地发音分数，" +
                        "用于生成语法、词汇、逻辑与地道表达建议。" +
                        "不会上传录音文件或其他本地记录。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeepSeekConsent = false
                        viewModel.generateAiFeedback()
                    },
                ) {
                    Text("同意并生成")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeepSeekConsent = false }) {
                    Text("取消")
                }
            },
        )
    }
}
