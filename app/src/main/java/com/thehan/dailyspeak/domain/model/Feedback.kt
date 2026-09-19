package com.thehan.dailyspeak.domain.model

data class Feedback(
    val attemptId: Long,
    val accuracy: Float,
    val fluency: Float,
    val completeness: Float,
    val prosody: Float,
    val overall: Float,
    val pronunciationIssues: List<String> = emptyList(),
    val grammarSuggestions: List<String> = emptyList(),
    val vocabularySuggestions: List<String> = emptyList(),
    val logicSuggestions: List<String> = emptyList(),
    val betterAnswer: String = "",
    val overallComment: String = "",
    val source: String = FeedbackSource.LOCAL,
    val createdAtEpochMillis: Long,
) {
    fun toPronunciationScore(recognitionConfidence: Float? = null): PronunciationScore =
        PronunciationScore(
            accuracy = accuracy,
            fluency = fluency,
            completeness = completeness,
            prosody = prosody,
            overall = overall,
            recognitionConfidence = recognitionConfidence,
            issues = pronunciationIssues,
            note = overallComment,
        )
}

object FeedbackSource {
    const val LOCAL = "local"
    const val DEEPSEEK = "deepseek"
}
