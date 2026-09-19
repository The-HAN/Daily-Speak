package com.thehan.dailyspeak.domain.model

data class ImprovementSuggestion(
    val grammarSuggestions: List<String> = emptyList(),
    val vocabularySuggestions: List<String> = emptyList(),
    val logicSuggestions: List<String> = emptyList(),
    val naturalExpressionSuggestions: List<String> = emptyList(),
    val betterAnswer: String = "",
    val overallComment: String = "",
)
