package com.thehan.dailyspeak.ui.feature.feedback

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehan.dailyspeak.domain.model.AccentPreference
import com.thehan.dailyspeak.domain.model.Attempt
import com.thehan.dailyspeak.domain.model.Feedback
import com.thehan.dailyspeak.domain.model.Question
import com.thehan.dailyspeak.domain.repository.AttemptRepository
import com.thehan.dailyspeak.domain.repository.FavoriteRepository
import com.thehan.dailyspeak.domain.repository.FeedbackRepository
import com.thehan.dailyspeak.domain.repository.QuestionRepository
import com.thehan.dailyspeak.domain.repository.SettingsRepository
import com.thehan.dailyspeak.domain.service.DeepSeekService
import com.thehan.dailyspeak.domain.service.TtsPlaybackState
import com.thehan.dailyspeak.domain.service.TtsService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val isLoading: Boolean = true,
    val attempt: Attempt? = null,
    val question: Question? = null,
    val feedback: Feedback? = null,
    val isFavorite: Boolean = false,
    val isUpdatingFavorite: Boolean = false,
    val accent: AccentPreference = AccentPreference.AMERICAN,
    val ttsEnabled: Boolean = true,
    val isGeneratingAiFeedback: Boolean = false,
    val aiFeedbackMessage: String? = null,
    val aiFeedbackError: String? = null,
    val ttsMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val attemptRepository: AttemptRepository,
    private val questionRepository: QuestionRepository,
    private val feedbackRepository: FeedbackRepository,
    private val favoriteRepository: FavoriteRepository,
    private val settingsRepository: SettingsRepository,
    private val deepSeekService: DeepSeekService,
    private val ttsService: TtsService,
) : ViewModel() {
    private val attemptId: Long = checkNotNull(
        savedStateHandle.get<Long>(ATTEMPT_ID_ARG),
    ) { "Missing attemptId navigation argument." }

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        accent = settings.accent,
                        ttsEnabled = settings.ttsEnabled,
                    )
                }
            }
        }
        viewModelScope.launch {
            ttsService.playbackState.collect { playbackState ->
                if (playbackState is TtsPlaybackState.Error) {
                    _uiState.update { it.copy(ttsMessage = playbackState.message) }
                }
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val attempt = attemptRepository.getById(attemptId)
                    ?: error("没有找到这次录音记录。")
                val question = questionRepository.getQuestion(attempt.questionId)
                    ?: error("没有找到对应题目。")
                val feedback = feedbackRepository.getByAttemptId(attemptId)
                    ?: error("这次回答还没有生成反馈。")
                Triple(attempt, question, feedback)
            }.onSuccess { (attempt, question, feedback) ->
                val favorite = runCatching {
                    favoriteRepository.isFavorite(question.id)
                }.getOrDefault(false)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        attempt = attempt,
                        question = question,
                        feedback = feedback,
                        isFavorite = favorite,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "反馈加载失败。",
                    )
                }
            }
        }
    }

    fun toggleFavorite() {
        val state = _uiState.value
        val questionId = state.question?.id ?: return
        if (state.isUpdatingFavorite) return

        val target = !state.isFavorite
        _uiState.update { it.copy(isUpdatingFavorite = true) }
        viewModelScope.launch {
            runCatching {
                favoriteRepository.setFavorite(questionId, target)
                target
            }.onSuccess { favorite ->
                _uiState.update {
                    it.copy(
                        isFavorite = favorite,
                        isUpdatingFavorite = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isUpdatingFavorite = false,
                        errorMessage = error.message ?: "收藏操作失败。",
                    )
                }
            }
        }
    }

    fun playReferenceAnswer(text: String) {
        val state = _uiState.value
        if (!state.ttsEnabled) {
            _uiState.update {
                it.copy(ttsMessage = "朗读已关闭，请到“我的 → 提醒与朗读”开启。")
            }
            return
        }
        if (text.isBlank()) return
        _uiState.update { it.copy(ttsMessage = null) }
        ttsService.speak(text, state.accent)
    }

    fun consumeTtsMessage() {
        _uiState.update { it.copy(ttsMessage = null) }
    }

    /**
     * Requests richer content coaching after the UI has collected explicit consent.
     * The audio file is never passed to this method or uploaded by it.
     */
    fun generateAiFeedback() {
        val state = _uiState.value
        val attempt = state.attempt ?: return
        val question = state.question ?: return
        val localFeedback = state.feedback ?: return
        if (state.isGeneratingAiFeedback) return
        if (attempt.transcript.isBlank()) {
            _uiState.update {
                it.copy(aiFeedbackError = "没有可分析的转写文本，请先重新录音。")
            }
            return
        }

        _uiState.update {
            it.copy(
                isGeneratingAiFeedback = true,
                aiFeedbackMessage = "正在请求 DeepSeek 生成改善建议…",
                aiFeedbackError = null,
            )
        }
        viewModelScope.launch {
            runCatching {
                deepSeekService.evaluateAnswer(
                    question = question,
                    transcript = attempt.transcript,
                    pronunciationScore = localFeedback.toPronunciationScore(),
                ).copy(attemptId = attemptId)
            }.onSuccess { aiFeedback ->
                runCatching {
                    feedbackRepository.save(aiFeedback)
                }.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                feedback = aiFeedback,
                                isGeneratingAiFeedback = false,
                                aiFeedbackMessage = "AI 建议已生成并缓存到本机。",
                                aiFeedbackError = null,
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                feedback = aiFeedback,
                                isGeneratingAiFeedback = false,
                                aiFeedbackMessage = "AI 建议已生成，但本次未能保存到本机。",
                                aiFeedbackError = error.message,
                            )
                        }
                    },
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isGeneratingAiFeedback = false,
                        aiFeedbackMessage = null,
                        aiFeedbackError = error.message
                            ?: "AI 建议生成失败，已保留本地反馈。",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        ttsService.stop()
        super.onCleared()
    }

    private companion object {
        const val ATTEMPT_ID_ARG = "attemptId"
    }
}
