package com.thehan.dailyspeak.ui.feature.today

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TodayRoute(
    onOpenFeedback: (Long) -> Unit,
    nextQuestionRequested: Boolean,
    onNextQuestionRequestConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startRecording()
        }
    }

    LaunchedEffect(uiState.recorder.processedAttemptId) {
        val attemptId = uiState.recorder.processedAttemptId ?: return@LaunchedEffect
        viewModel.consumeProcessedAttempt()
        onOpenFeedback(attemptId)
    }

    LaunchedEffect(nextQuestionRequested) {
        if (nextQuestionRequested) {
            viewModel.moveToNextQuestion()
            onNextQuestionRequestConsumed()
        }
    }

    TodayScreen(
        uiState = uiState,
        onRetry = viewModel::loadTodayQuestions,
        onNextQuestion = viewModel::moveToNextQuestion,
        onRecordClick = {
            when {
                uiState.recorder.isRecording -> viewModel.stopRecording()
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED -> viewModel.startRecording()
                else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onPlayQuestion = viewModel::playCurrentQuestion,
        onRetryAnalysis = viewModel::retryAnalysis,
        modifier = modifier,
    )
}