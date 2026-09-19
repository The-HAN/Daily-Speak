package com.thehan.dailyspeak.ui.feature.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thehan.dailyspeak.domain.model.Attempt
import com.thehan.dailyspeak.domain.model.Feedback
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.Question
import com.thehan.dailyspeak.domain.model.ReferenceAnswers
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FeedbackScreen(
    uiState: FeedbackUiState,
    onBack: () -> Unit,
    onRetryRecording: () -> Unit,
    onNextQuestion: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayReferenceAnswer: (String) -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        FeedbackHeader(
            isFavorite = uiState.isFavorite,
            favoriteEnabled = uiState.question != null && !uiState.isUpdatingFavorite,
            onBack = onBack,
            onToggleFavorite = onToggleFavorite,
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading -> FeedbackLoading()
            uiState.errorMessage != null -> FeedbackError(
                message = uiState.errorMessage,
                onRetry = onRetryLoad,
            )
            else -> {
                val attempt = uiState.attempt
                val question = uiState.question
                val feedback = uiState.feedback
                if (attempt == null || question == null || feedback == null) {
                    FeedbackError(
                        message = "反馈数据不完整，请返回后重试。",
                        onRetry = onRetryLoad,
                    )
                } else {
                    FeedbackContent(
                        question = question,
                        attempt = attempt,
                        feedback = feedback,
                        onPlayReferenceAnswer = onPlayReferenceAnswer,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        FeedbackActions(
            onRetryRecording = onRetryRecording,
            onNextQuestion = onNextQuestion,
        )
        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun FeedbackHeader(
    isFavorite: Boolean,
    favoriteEnabled: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
            )
        }
        Text(
            text = "练习反馈",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        IconButton(
            onClick = onToggleFavorite,
            enabled = favoriteEnabled,
        ) {
            Icon(
                imageVector = if (isFavorite) {
                    Icons.Default.Favorite
                } else {
                    Icons.Default.FavoriteBorder
                },
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                tint = if (isFavorite) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun FeedbackContent(
    question: Question,
    attempt: Attempt,
    feedback: Feedback,
    onPlayReferenceAnswer: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        QuestionHeroCard(question)

        ExpandableSection(title = "原句意思", subtitle = "中文理解") {
            Text(
                text = question.chineseMeaning,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        ExpandableSection(title = "参考回答", subtitle = "日常 / 进阶 / 考研") {
            ReferenceAnswerItem(
                label = "日常版",
                text = question.referenceAnswers.daily,
                onPlay = onPlayReferenceAnswer,
            )
            ReferenceAnswerItem(
                label = "进阶版",
                text = question.referenceAnswers.advanced,
                onPlay = onPlayReferenceAnswer,
            )
            ReferenceAnswerItem(
                label = "考研版",
                text = question.referenceAnswers.postgraduate,
                onPlay = onPlayReferenceAnswer,
            )
        }

        ExpandableSection(title = "你的回答", subtitle = "语音转写") {
            Text(
                text = attempt.transcript.ifBlank { "未识别到有效内容。" },
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "录音时长：${formatDuration(attempt.durationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ExpandableSection(
            title = "发音评分",
            subtitle = "总分 ${feedback.overall.roundToInt()}",
        ) {
            OverallScore(score = feedback.overall)
            Spacer(modifier = Modifier.height(18.dp))
            ScoreRow(label = "准确度", score = feedback.accuracy)
            ScoreRow(label = "流利度", score = feedback.fluency)
            ScoreRow(label = "完整度", score = feedback.completeness)
            ScoreRow(label = "语调 / 韵律", score = feedback.prosody)

            if (feedback.pronunciationIssues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "发音问题",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SuggestionList(feedback.pronunciationIssues)
            }
        }

        ExpandableSection(title = "改善建议", subtitle = "语法 / 词汇 / 逻辑 / 地道表达") {
            SuggestionBlock("语法", feedback.grammarSuggestions)
            SuggestionBlock("词汇", feedback.vocabularySuggestions)
            SuggestionBlock("逻辑", feedback.logicSuggestions)

            if (feedback.betterAnswer.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "更好的回答",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = feedback.betterAnswer,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (
                feedback.pronunciationIssues.isEmpty() &&
                feedback.grammarSuggestions.isEmpty() &&
                feedback.vocabularySuggestions.isEmpty() &&
                feedback.logicSuggestions.isEmpty() &&
                feedback.betterAnswer.isBlank()
            ) {
                Text(
                    text = "本次为本地粗略评分，暂未生成内容改进建议。接入 DeepSeek 后会补充语法、词汇、逻辑和地道表达建议。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuestionHeroCard(question: Question) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(
                text = "${question.topic} · ${question.difficulty.displayName()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = question.englishQuestion,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (question.keyPhrases.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "关键词：${question.keyPhrases.joinToString(" · ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ReferenceAnswerItem(
    label: String,
    text: String,
    onPlay: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(onClick = { onPlay(text) }) {
                Text("示范朗读")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun OverallScore(score: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(92.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = score.roundToInt().toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "综合得分",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "基于系统识别置信度和录音音频特征估算，不代表音素级评测。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = score.roundToInt().toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((score / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun SuggestionBlock(title: String, suggestions: List<String>) {
    if (suggestions.isEmpty()) return
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    SuggestionList(suggestions)
    Spacer(modifier = Modifier.height(18.dp))
}

@Composable
private fun SuggestionList(suggestions: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.forEach { suggestion ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "•",
                    modifier = Modifier.width(18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = suggestion,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    subtitle: String? = null,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title, initiallyExpanded) {
        mutableStateOf(initiallyExpanded)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = if (expanded) "收起" else "展开",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun FeedbackActions(
    onRetryRecording: () -> Unit,
    onNextQuestion: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onRetryRecording,
            modifier = Modifier.weight(1f),
        ) {
            Text("重录")
        }
        Button(
            onClick = onNextQuestion,
            modifier = Modifier.weight(1f),
        ) {
            Text("下一题")
        }
    }
}

@Composable
private fun FeedbackLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "正在整理反馈…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeedbackError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "反馈加载失败",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    return "%02d:%02d".format(Locale.US, totalSeconds / 60L, totalSeconds % 60L)
}

private fun PracticeLevel.displayName(): String = when (this) {
    PracticeLevel.DAILY -> "日常"
    PracticeLevel.POSTGRADUATE -> "考研"
    PracticeLevel.MIXED -> "混合"
}