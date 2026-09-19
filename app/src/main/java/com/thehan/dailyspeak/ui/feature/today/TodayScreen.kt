package com.thehan.dailyspeak.ui.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.Question
import com.thehan.dailyspeak.domain.model.ReferenceAnswers
import com.thehan.dailyspeak.ui.theme.DailySpeakTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onRetry: () -> Unit,
    onNextQuestion: () -> Unit,
    onRecordClick: () -> Unit,
    onPlayQuestion: () -> Unit,
    onRetryAnalysis: () -> Unit,
    onGenerateWithDeepSeek: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayLabel = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA),
        )
    }
    val currentQuestion = uiState.currentQuestion

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        when {
            uiState.isLoading -> LoadingContent(modifier = Modifier.fillMaxWidth())
            uiState.errorMessage != null -> ErrorContent(
                message = uiState.errorMessage,
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
            currentQuestion == null -> EmptyContent(modifier = Modifier.fillMaxWidth())
            else -> PracticeContent(
                dateLabel = todayLabel,
                question = currentQuestion,
                progressCurrent = uiState.progressCurrent,
                progressTotal = uiState.progressTotal,
                canMoveNext = uiState.canMoveNext,
                isGeneratingQuestions = uiState.isGeneratingQuestions,
                generationMessage = uiState.generationMessage,
                generationError = uiState.generationError,
                recorder = uiState.recorder,
                onNextQuestion = onNextQuestion,
                onRecordClick = onRecordClick,
                onPlayQuestion = onPlayQuestion,
                onRetryAnalysis = onRetryAnalysis,
                onGenerateWithDeepSeek = onGenerateWithDeepSeek,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun PracticeContent(
    dateLabel: String,
    question: Question,
    progressCurrent: Int,
    progressTotal: Int,
    canMoveNext: Boolean,
    isGeneratingQuestions: Boolean,
    generationMessage: String?,
    generationError: String?,
    recorder: RecorderUiState,
    onNextQuestion: () -> Unit,
    onRecordClick: () -> Unit,
    onPlayQuestion: () -> Unit,
    onRetryAnalysis: () -> Unit,
    onGenerateWithDeepSeek: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hintVisible by rememberSaveable(question.id) { mutableStateOf(false) }

    Column(modifier = modifier) {
        TodayHeader(
            dateLabel = dateLabel,
            progressCurrent = progressCurrent,
            progressTotal = progressTotal,
        )
        Spacer(modifier = Modifier.height(16.dp))
        DeepSeekQuestionCard(
            isGenerating = isGeneratingQuestions,
            message = generationMessage,
            errorMessage = generationError,
            onClick = onGenerateWithDeepSeek,
        )
        Spacer(modifier = Modifier.height(16.dp))
        QuestionCard(
            question = question,
            hintVisible = hintVisible,
            onPlayQuestion = onPlayQuestion,
            onToggleHint = { hintVisible = !hintVisible },
        )
        Spacer(modifier = Modifier.height(28.dp))
        RecordAction(
            recorder = recorder,
            onRecordClick = onRecordClick,
            onRetryAnalysis = onRetryAnalysis,
        )
        if (canMoveNext) {
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = onNextQuestion,
                enabled = !recorder.isRecording && !recorder.isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("下一题")
            }
        }
    }
}

@Composable
private fun DeepSeekQuestionCard(
    isGenerating: Boolean,
    message: String?,
    errorMessage: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI 动态出题",
                    style = MaterialTheme.typography.titleSmall,
                )
                val helper = errorMessage
                    ?: message
                    ?: "按当前难度和话题偏好生成，可离线缓存。"
                Text(
                    text = helper,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (errorMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            OutlinedButton(
                onClick = onClick,
                enabled = !isGenerating,
            ) {
                Text(if (isGenerating) "生成中…" else "生成")
            }
        }
    }
}

@Composable
private fun TodayHeader(
    dateLabel: String,
    progressCurrent: Int,
    progressTotal: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    text = "今日练习",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = dateLabel,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$progressCurrent / $progressTotal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "连续打卡 0 天",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: Question,
    hintVisible: Boolean,
    onPlayQuestion: () -> Unit,
    onToggleHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "DAILY QUESTION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = question.topic,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = question.englishQuestion,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (question.difficulty == PracticeLevel.POSTGRADUATE) "考研难度" else "日常难度",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.height(22.dp))

            if (hintVisible) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = question.chineseMeaning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "提示：${question.keyPhrases.joinToString(" · ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onPlayQuestion,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("播放问句")
                }
                OutlinedButton(
                    onClick = onToggleHint,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (hintVisible) "收起提示" else "查看提示")
                }
            }
        }
    }
}

@Composable
private fun RecordAction(
    recorder: RecorderUiState,
    onRecordClick: () -> Unit,
    onRetryAnalysis: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonColor = if (recorder.isRecording) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val buttonContentColor = if (recorder.isRecording) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    val buttonLabel = when {
        recorder.isProcessing -> "分析中…"
        recorder.isRecording -> "停止\n录音"
        else -> "开始\n回答"
    }
    val helperText = when {
        recorder.isProcessing -> recorder.statusMessage ?: "正在转写并计算发音评分…"
        recorder.isRecording -> "录音中 ${formatDuration(recorder.elapsedMs)}，再次点击结束"
        recorder.processedAttemptId != null -> "分析完成，正在打开反馈…"
        recorder.errorMessage != null -> recorder.errorMessage
        else -> "录音默认仅保存在本机，上传前会再次征求同意。"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onRecordClick,
            enabled = !recorder.isProcessing,
            modifier = Modifier.size(132.dp),
            shape = CircleShape,
            color = buttonColor,
            contentColor = buttonContentColor,
            shadowElevation = 4.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = helperText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (recorder.errorMessage != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
        )
        if (recorder.savedAttemptId != null && recorder.errorMessage != null) {
            OutlinedButton(
                onClick = onRetryAnalysis,
                enabled = !recorder.isProcessing,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text("重试转写与评分")
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    return "%02d:%02d".format(Locale.US, totalSeconds / 60L, totalSeconds % 60L)
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "正在准备今日题目…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "题目加载失败",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 20.dp),
        ) {
            Text("重试")
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "暂时没有可用题目",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "请稍后重试，或先在设置中配置题库来源。",
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun TodayScreenPreview() {
    DailySpeakTheme {
        TodayScreen(
            uiState = TodayUiState(
                isLoading = false,
                questions = listOf(
                    Question(
                        id = "preview",
                        englishQuestion = "What is one small habit that has improved your daily life?",
                        chineseMeaning = "哪一个小的日常习惯改善了你的生活？",
                        topic = "生活方式",
                        difficulty = PracticeLevel.DAILY,
                        source = "preview",
                        keyPhrases = listOf("small habit", "consistency"),
                        referenceAnswers = ReferenceAnswers(
                            daily = "I read before bed.",
                            advanced = "I read before bed to relax and learn.",
                            postgraduate = "A small bedtime reading habit helps me reflect and learn consistently.",
                        ),
                        createdAtEpochMillis = 0L,
                    ),
                ),
            ),
            onRetry = { },
            onNextQuestion = { },
            onRecordClick = { },
            onPlayQuestion = { },
            onRetryAnalysis = { },
            onGenerateWithDeepSeek = { },
        )
    }
}

