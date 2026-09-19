package com.thehan.dailyspeak.ui.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.ReviewItem
import com.thehan.dailyspeak.domain.repository.FavoriteRepository
import com.thehan.dailyspeak.domain.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReviewSection(val label: String) {
    LOW_SCORE("低分题"),
    FAVORITES("收藏题"),
    HISTORY("历史记录"),
}

enum class ReviewDateFilter(val label: String) {
    ALL("全部日期"),
    TODAY("今天"),
    LAST_7_DAYS("近 7 天"),
    LAST_30_DAYS("近 30 天"),
}

data class ReviewUiState(
    val isLoading: Boolean = true,
    val items: List<ReviewItem> = emptyList(),
    val selectedSection: ReviewSection = ReviewSection.LOW_SCORE,
    val selectedDifficulty: PracticeLevel? = null,
    val selectedTopic: String? = null,
    val selectedDateFilter: ReviewDateFilter = ReviewDateFilter.ALL,
    val errorMessage: String? = null,
) {
    val availableTopics: List<String>
        get() = items.map { it.question.topic }.distinct().sorted()

    val filteredItems: List<ReviewItem>
        get() {
            val today = LocalDate.now()
            val cutoff = when (selectedDateFilter) {
                ReviewDateFilter.ALL -> null
                ReviewDateFilter.TODAY -> today
                ReviewDateFilter.LAST_7_DAYS -> today.minusDays(6)
                ReviewDateFilter.LAST_30_DAYS -> today.minusDays(29)
            }

            return items.filter { item ->
                val sectionMatches = when (selectedSection) {
                    ReviewSection.LOW_SCORE -> item.feedback?.overall?.let { it < LOW_SCORE_THRESHOLD } == true
                    ReviewSection.FAVORITES -> item.isFavorite
                    ReviewSection.HISTORY -> item.latestAttempt != null
                }
                val difficultyMatches = selectedDifficulty == null ||
                    item.question.difficulty == selectedDifficulty
                val topicMatches = selectedTopic == null || item.question.topic == selectedTopic
                val dateMatches = cutoff == null || item.latestAttempt?.createdAtEpochMillis?.let { timestamp ->
                    val date = Instant.ofEpochMilli(timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    !date.isBefore(cutoff)
                } == true

                sectionMatches && difficultyMatches && topicMatches && dateMatches
            }
        }

    private companion object {
        const val LOW_SCORE_THRESHOLD = 70f
    }
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val favoriteRepository: FavoriteRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { reviewRepository.getReviewItems() }
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = items,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "复习数据加载失败。",
                        )
                    }
                }
        }
    }

    fun selectSection(section: ReviewSection) {
        _uiState.update { it.copy(selectedSection = section) }
    }

    fun selectDifficulty(level: PracticeLevel?) {
        _uiState.update { it.copy(selectedDifficulty = level) }
    }

    fun selectTopic(topic: String?) {
        _uiState.update {
            it.copy(selectedTopic = topic.takeUnless { selected -> selected == it.selectedTopic })
        }
    }

    fun selectDateFilter(filter: ReviewDateFilter) {
        _uiState.update { it.copy(selectedDateFilter = filter) }
    }

    fun toggleFavorite(item: ReviewItem) {
        val target = !item.isFavorite
        viewModelScope.launch {
            runCatching {
                favoriteRepository.setFavorite(item.question.id, target)
                target
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        items = state.items.map { current ->
                            if (current.question.id == item.question.id) {
                                current.copy(isFavorite = target)
                            } else {
                                current
                            }
                        },
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "收藏操作失败。")
                }
            }
        }
    }
}