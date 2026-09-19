package com.thehan.dailyspeak.ui.feature.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    FeedbackScreen(
        uiState = uiState,
        onBack = onBack,
        onRetryRecording = onRetryRecording,
        onNextQuestion = onNextQuestion,
        onToggleFavorite = viewModel::toggleFavorite,
        onPlayReferenceAnswer = viewModel::playReferenceAnswer,
        onRetryLoad = viewModel::load,
        modifier = modifier,
    )
}