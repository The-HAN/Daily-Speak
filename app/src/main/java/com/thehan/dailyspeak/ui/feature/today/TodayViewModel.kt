package com.thehan.dailyspeak.ui.feature.today

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehan.dailyspeak.core.audio.AudioRecorder
import com.thehan.dailyspeak.core.audio.RecordingFileStore
import com.thehan.dailyspeak.domain.model.Attempt
import com.thehan.dailyspeak.domain.model.Feedback
import com.thehan.dailyspeak.domain.model.FeedbackSource
import com.thehan.dailyspeak.domain.model.Question
import com.thehan.dailyspeak.domain.model.UserSettings
import com.thehan.dailyspeak.domain.repository.AttemptRepository
import com.thehan.dailyspeak.domain.repository.FeedbackRepository
import com.thehan.dailyspeak.domain.repository.QuestionRepository
import com.thehan.dailyspeak.domain.repository.SettingsRepository
import com.thehan.dailyspeak.domain.service.DeepSeekService
import com.thehan.dailyspeak.domain.service.PronunciationEvaluator
import com.thehan.dailyspeak.domain.service.SpeechRecognizerService
import com.thehan.dailyspeak.domain.service.TranscriptAwarePronunciationEvaluator
import com.thehan.dailyspeak.domain.service.TtsPlaybackState
import com.thehan.dailyspeak.domain.service.TtsService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecorderUiState(
    val isRecording: Boolean = false,
    val elapsedMs: Long = 0L,
    val isProcessing: Boolean = false,
    val savedAttemptId: Long? = null,
    val processedAttemptId: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

data class TodayUiState(
    val isLoading: Boolean = true,
    val isGeneratingQuestions: Boolean = false,
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val errorMessage: String? = null,
    val generationMessage: String? = null,
    val generationError: String? = null,
    val ttsMessage: String? = null,
    val recorder: RecorderUiState = RecorderUiState(),
) {
    val currentQuestion: Question?
        get() = questions.getOrNull(currentIndex)

    val progressCurrent: Int
        get() = if (questions.isEmpty()) 0 else currentIndex + 1

    val progressTotal: Int
        get() = questions.size

    val canMoveNext: Boolean
        get() = currentIndex < questions.lastIndex
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val settingsRepository: SettingsRepository,
    private val deepSeekService: DeepSeekService,
    private val attemptRepository: AttemptRepository,
    private val feedbackRepository: FeedbackRepository,
    private val audioRecorder: AudioRecorder,
    private val recordingFileStore: RecordingFileStore,
    private val speechRecognizerService: SpeechRecognizerService,
    private val pronunciationEvaluator: PronunciationEvaluator,
    private val ttsService: TtsService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private var currentSettings = UserSettings()
    private var timerJob: Job? = null
    private var pendingAnalysis: PendingAnalysis? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings
                .distinctUntilChanged()
                .collectLatest { settings ->
                    currentSettings = settings
                    loadQuestions(settings)
                }
        }
        viewModelScope.launch {
            ttsService.playbackState.collect { playbackState ->
                if (playbackState is TtsPlaybackState.Error) {
                    _uiState.update { it.copy(ttsMessage = playbackState.message) }
                }
            }
        }
    }

    fun loadTodayQuestions() {
        viewModelScope.launch { loadQuestions(currentSettings) }
    }

    /**
     * Generates questions only after the UI has collected explicit user consent.
     * Only settings and question metadata are sent; recordings never leave the device.
     */
    fun generateDailyQuestionsWithDeepSeek() {
        val state = _uiState.value
        if (state.isGeneratingQuestions) return

        val settings = currentSettings
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingQuestions = true,
                    generationMessage = "正在通过 DeepSeek 生成题目…",
                    generationError = null,
                    errorMessage = null,
                )
            }

            val result = runCatching {
                deepSeekService.generateDailyQuestions(
                    date = LocalDate.now().toString(),
                    count = settings.dailyCount,
                    level = settings.level,
                    topics = settings.topics.toList(),
                ).also { questions ->
                    check(questions.isNotEmpty()) {
                        "DeepSeek 没有返回可用题目，请稍后重试。"
                    }
                }
            }

            result.onSuccess { questions ->
                val cacheResult = runCatching {
                    questionRepository.cacheQuestions(questions)
                }
                _uiState.value = TodayUiState(
                    isLoading = false,
                    questions = questions,
                    generationMessage = if (cacheResult.isSuccess) {
                        "已生成 ${questions.size} 道题，并缓存到本机。"
                    } else {
                        "题目已生成，但本地缓存失败；当前仍可继续练习。"
                    },
                )
                pendingAnalysis = null
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isGeneratingQuestions = false,
                        generationMessage = null,
                        generationError = error.message
                            ?: "AI 出题失败，已保留当前题目。",
                    )
                }
            }
        }
    }

    fun moveToNextQuestion() {
        _uiState.update { state ->
            if (!state.canMoveNext) {
                state
            } else {
                state.copy(
                    currentIndex = state.currentIndex + 1,
                    recorder = RecorderUiState(),
                )
            }
        }
        pendingAnalysis = null
    }

    fun playCurrentQuestion() {
        if (!currentSettings.ttsEnabled) {
            _uiState.update {
                it.copy(ttsMessage = "朗读已关闭，请到“我的 → 提醒与朗读”开启。")
            }
            return
        }
        val question = _uiState.value.currentQuestion ?: return
        _uiState.update { it.copy(ttsMessage = null) }
        ttsService.speak(question.englishQuestion, currentSettings.accent)
    }

    fun consumeTtsMessage() {
        _uiState.update { it.copy(ttsMessage = null) }
    }

    fun consumeProcessedAttempt() {
        _uiState.update { state ->
            state.copy(recorder = state.recorder.copy(processedAttemptId = null))
        }
    }

    fun startRecording() {
        if (_uiState.value.recorder.isRecording) return

        var outputFile: File? = null
        runCatching {
            outputFile = recordingFileStore.createRecordingFile()
            audioRecorder.start(requireNotNull(outputFile))
        }.onSuccess {
            _uiState.update { state ->
                state.copy(
                    recorder = RecorderUiState(
                        isRecording = true,
                        elapsedMs = 0L,
                        statusMessage = "正在录音，再次点击结束。",
                    ),
                )
            }
            startTimer()
        }.onFailure { error ->
            outputFile?.delete()
            _uiState.update { state ->
                state.copy(
                    recorder = RecorderUiState(
                        errorMessage = error.message ?: "无法开始录音，请检查麦克风权限。",
                    ),
                )
            }
        }
    }

    fun stopRecording() {
        val question = _uiState.value.currentQuestion ?: return
        if (!_uiState.value.recorder.isRecording) return

        timerJob?.cancel()
        timerJob = null
        _uiState.update { state ->
            state.copy(
                recorder = state.recorder.copy(
                    isRecording = false,
                    isProcessing = true,
                    statusMessage = "正在保存录音并识别语音…",
                    errorMessage = null,
                ),
            )
        }

        viewModelScope.launch {
            val recordingResult = withContext(Dispatchers.IO) {
                runCatching { audioRecorder.stop() }
            }

            recordingResult.onSuccess { recording ->
                val attemptId = saveAttempt(question, recording.file, recording.durationMs)
                if (attemptId == null) {
                    return@onSuccess
                }

                pendingAnalysis = PendingAnalysis(
                    attemptId = attemptId,
                    question = question,
                    audioFile = recording.file,
                    durationMs = recording.durationMs,
                )
                analyzePendingAttempt()
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        recorder = RecorderUiState(
                            errorMessage = error.message ?: "录音结束失败。",
                        ),
                    )
                }
            }
        }
    }

    fun retryAnalysis() {
        if (pendingAnalysis == null || _uiState.value.recorder.isProcessing) return
        _uiState.update { state ->
            state.copy(
                recorder = state.recorder.copy(
                    isProcessing = true,
                    statusMessage = "正在重新识别并评分…",
                    errorMessage = null,
                ),
            )
        }
        viewModelScope.launch { analyzePendingAttempt() }
    }

    private suspend fun analyzePendingAttempt() {
        val pending = pendingAnalysis ?: return
        val recognition = runCatching {
            speechRecognizerService.recognizeDetailed(pending.audioFile)
        }

        if (recognition.isFailure) {
            _uiState.update { state ->
                state.copy(
                    recorder = state.recorder.copy(
                        isProcessing = false,
                        savedAttemptId = pending.attemptId,
                        statusMessage = "录音已保存，但语音识别未完成。",
                        errorMessage = recognition.exceptionOrNull()?.message
                            ?: "语音识别失败，请重试或重新录音。",
                    ),
                )
            }
            return
        }

        val result = recognition.getOrThrow()
        val scoreResult = runCatching {
            when (val evaluator = pronunciationEvaluator) {
                is TranscriptAwarePronunciationEvaluator -> evaluator.evaluate(
                    audioFile = pending.audioFile,
                    expectedText = pending.question.referenceAnswers.advanced,
                    transcript = result.transcript,
                    recognitionConfidence = result.confidence,
                )

                else -> evaluator.evaluate(
                    audioFile = pending.audioFile,
                    expectedText = pending.question.referenceAnswers.advanced,
                )
            }
        }

        if (scoreResult.isFailure) {
            _uiState.update { state ->
                state.copy(
                    recorder = state.recorder.copy(
                        isProcessing = false,
                        savedAttemptId = pending.attemptId,
                        statusMessage = "转写已完成，但发音评分失败。",
                        errorMessage = scoreResult.exceptionOrNull()?.message
                            ?: "发音评分失败，请重试。",
                    ),
                )
            }
            return
        }

        runCatching {
            attemptRepository.updateTranscript(pending.attemptId, result.transcript)
            val score = scoreResult.getOrThrow()
            feedbackRepository.save(
                Feedback(
                    attemptId = pending.attemptId,
                    accuracy = score.accuracy,
                    fluency = score.fluency,
                    completeness = score.completeness,
                    prosody = score.prosody,
                    overall = score.overall,
                    pronunciationIssues = score.issues,
                    betterAnswer = pending.question.referenceAnswers.advanced,
                    overallComment = score.note,
                    source = FeedbackSource.LOCAL,
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }.onSuccess {
            pendingAnalysis = null
            _uiState.update { state ->
                state.copy(
                    recorder = state.recorder.copy(
                        isProcessing = false,
                        savedAttemptId = pending.attemptId,
                        processedAttemptId = pending.attemptId,
                        statusMessage = "分析完成，正在打开反馈。",
                        errorMessage = null,
                    ),
                )
            }
        }.onFailure { error ->
            _uiState.update { state ->
                state.copy(
                    recorder = state.recorder.copy(
                        isProcessing = false,
                        savedAttemptId = pending.attemptId,
                        statusMessage = "本地分析结果保存失败。",
                        errorMessage = error.message ?: "反馈保存失败。",
                    ),
                )
            }
        }
    }

    private suspend fun saveAttempt(
        question: Question,
        audioFile: File,
        durationMs: Long,
    ): Long? = runCatching {
        attemptRepository.save(
            Attempt(
                questionId = question.id,
                audioPath = audioFile.absolutePath,
                transcript = "",
                durationMs = durationMs,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }.onFailure { error ->
        _uiState.update { state ->
            state.copy(
                recorder = RecorderUiState(
                    errorMessage = error.message ?: "录音保存失败。",
                ),
            )
        }
    }.getOrNull()

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            while (isActive) {
                val elapsedMs = SystemClock.elapsedRealtime() - startedAt
                _uiState.update { state ->
                    if (state.recorder.isRecording) {
                        state.copy(recorder = state.recorder.copy(elapsedMs = elapsedMs))
                    } else {
                        state
                    }
                }
                delay(200L)
            }
        }
    }

    private suspend fun loadQuestions(settings: UserSettings) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            questionRepository.getDailyQuestions(
                date = LocalDate.now().toString(),
                count = settings.dailyCount,
                level = settings.level,
                topics = settings.topics.toList(),
            )
        }.onSuccess { questions ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    questions = questions,
                    currentIndex = 0,
                    errorMessage = null,
                    recorder = RecorderUiState(),
                )
            }
            pendingAnalysis = null
        }.onFailure { error ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "今日题目加载失败",
                )
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        audioRecorder.cancel()
        ttsService.stop()
        super.onCleared()
    }

    private data class PendingAnalysis(
        val attemptId: Long,
        val question: Question,
        val audioFile: File,
        val durationMs: Long,
    )
}
