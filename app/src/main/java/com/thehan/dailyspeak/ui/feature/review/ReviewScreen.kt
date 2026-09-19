package com.thehan.dailyspeak.ui.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.ReviewItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ReviewScreen(
    uiState: ReviewUiState,
    onSelectSection: (ReviewSection) -> Unit,
    onSelectDifficulty: (PracticeLevel?) -> Unit,
    onSelectTopic: (String?) -> Unit,
    onSelectDateFilter: (ReviewDateFilter) -> Unit,
    onToggleFavorite: (ReviewItem) -> Unit,
    onOpenFeedback: (ReviewItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 36.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "复习",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "低分题、收藏题和练习历史都在这里。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReviewSection.entries) { section ->
                    FilterChip(
                        selected = uiState.selectedSection == section,
                        onClick = { onSelectSection(section) },
                        label = { Text(section.label) },
                    )
                }
            }
        }

        item {
            FilterGroup(title = "日期") {
                ReviewDateFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.selectedDateFilter == filter,
                        onClick = { onSelectDateFilter(filter) },
                        label = { Text(filter.label) },
                    )
                }
            }
        }

        item {
            FilterGroup(title = "难度") {
                FilterChip(
                    selected = uiState.selectedDifficulty == null,
                    onClick = { onSelectDifficulty(null) },
                    label = { Text("全部") },
                )
                FilterChip(
                    selected = uiState.selectedDifficulty == PracticeLevel.DAILY,
                    onClick = { onSelectDifficulty(PracticeLevel.DAILY) },
                    label = { Text("日常") },
                )
                FilterChip(
                    selected = uiState.selectedDifficulty == PracticeLevel.POSTGRADUATE,
                    onClick = { onSelectDifficulty(PracticeLevel.POSTGRADUATE) },
                    label = { Text("考研") },
                )
            }
        }

        if (uiState.availableTopics.isNotEmpty()) {
            item {
                FilterGroup(title = "话题") {
                    FilterChip(
                        selected = uiState.selectedTopic == null,
                        onClick = { onSelectTopic(null) },
                        label = { Text("全部") },
                    )
                    uiState.availableTopics.forEach { topic ->
                        FilterChip(
                            selected = uiState.selectedTopic == topic,
                            onClick = { onSelectTopic(topic) },
                            label = { Text(topic) },
                        )
                    }
                }
            }
        }

        when {
            uiState.isLoading -> item {
                ReviewLoading()
            }

            uiState.errorMessage != null -> item {
                ReviewError(
                    message = uiState.errorMessage,
                    onRetry = onRetry,
                )
            }

            uiState.filteredItems.isEmpty() -> item {
                ReviewEmpty(section = uiState.selectedSection)
            }

            else -> items(
                items = uiState.filteredItems,
                key = { it.question.id },
            ) { item ->
                ReviewItemCard(
                    item = item,
                    onToggleFavorite = { onToggleFavorite(item) },
                    onOpenFeedback = { onOpenFeedback(item) },
                )
            }
        }
    }
}

@Composable
private fun FilterGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ReviewItemCard(
    item: ReviewItem,
    onToggleFavorite: () -> Unit,
    onOpenFeedback: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${item.question.topic} · ${item.question.difficulty.displayName()}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (item.isFavorite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = if (item.isFavorite) "取消收藏" else "收藏",
                        tint = if (item.isFavorite) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Text(
                text = item.question.englishQuestion,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.question.chineseMeaning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            item.feedback?.let { feedback ->
                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "发音 ${feedback.overall.roundToInt()} 分",
                        style = MaterialTheme.typography.labelLarge,
                        color = scoreColor(feedback.overall),
                    )
                    item.latestAttempt?.let { attempt ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatDate(attempt.createdAtEpochMillis),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item.latestAttempt?.transcript?.takeIf(String::isNotBlank)?.let { transcript ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = transcript,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (item.latestAttempt != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onOpenFeedback) {
                    Text("查看反馈")
                }
            }
        }
    }
}

@Composable
private fun ReviewLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "正在整理复习内容…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReviewError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "复习数据加载失败",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun ReviewEmpty(section: ReviewSection) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (section) {
                    ReviewSection.LOW_SCORE -> "暂无低分题"
                    ReviewSection.FAVORITES -> "暂无收藏题"
                    ReviewSection.HISTORY -> "暂无历史记录"
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "完成练习后，这里会自动整理可复习内容。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun scoreColor(score: Float) = if (score >= 80f) {
    MaterialTheme.colorScheme.primary
} else if (score >= 60f) {
    MaterialTheme.colorScheme.secondary
} else {
    MaterialTheme.colorScheme.error
}

private fun PracticeLevel.displayName(): String = when (this) {
    PracticeLevel.DAILY -> "日常"
    PracticeLevel.POSTGRADUATE -> "考研"
    PracticeLevel.MIXED -> "混合"
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA))