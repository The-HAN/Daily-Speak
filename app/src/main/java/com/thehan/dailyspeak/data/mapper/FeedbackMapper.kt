package com.thehan.dailyspeak.data.mapper

import com.thehan.dailyspeak.data.local.entity.FeedbackEntity
import com.thehan.dailyspeak.domain.model.Feedback

fun FeedbackEntity.toDomain(): Feedback = Feedback(
    attemptId = attemptId,
    accuracy = accuracy,
    fluency = fluency,
    completeness = completeness,
    prosody = prosody,
    overall = overall,
    pronunciationIssues = pronunciationIssues,
    grammarSuggestions = grammarSuggestions,
    vocabularySuggestions = vocabularySuggestions,
    logicSuggestions = logicSuggestions,
    naturalExpressionSuggestions = naturalExpressionSuggestions,
    betterAnswer = betterAnswer,
    overallComment = overallComment,
    source = source,
    createdAtEpochMillis = createdAtEpochMillis,
)

fun Feedback.toEntity(): FeedbackEntity = FeedbackEntity(
    attemptId = attemptId,
    accuracy = accuracy,
    fluency = fluency,
    completeness = completeness,
    prosody = prosody,
    overall = overall,
    pronunciationIssues = pronunciationIssues,
    grammarSuggestions = grammarSuggestions,
    vocabularySuggestions = vocabularySuggestions,
    logicSuggestions = logicSuggestions,
    naturalExpressionSuggestions = naturalExpressionSuggestions,
    betterAnswer = betterAnswer,
    overallComment = overallComment,
    source = source,
    createdAtEpochMillis = createdAtEpochMillis,
)
