package com.thehan.dailyspeak.ui.feature.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thehan.dailyspeak.domain.model.ReviewItem

@Composable
fun ReviewRoute(
    onOpenFeedback: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReviewScreen(
        uiState = uiState,
        onSelectSection = viewModel::selectSection,
        onSelectDifficulty = viewModel::selectDifficulty,
        onSelectTopic = viewModel::selectTopic,
        onSelectDateFilter = viewModel::selectDateFilter,
        onToggleFavorite = viewModel::toggleFavorite,
        onOpenFeedback = { item -> openFeedback(item, onOpenFeedback) },
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

private fun openFeedback(item: ReviewItem, onOpenFeedback: (Long) -> Unit) {
    val attemptId = item.latestAttempt?.id ?: return
    onOpenFeedback(attemptId)
}