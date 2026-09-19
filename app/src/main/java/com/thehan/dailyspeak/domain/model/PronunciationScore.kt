package com.thehan.dailyspeak.domain.model

/**
 * Coarse pronunciation score. Values are in the 0..100 range.
 *
 * The default implementation intentionally uses recognizer confidence and
 * objective audio features only. A phoneme-level cloud evaluator can replace
 * it without changing the UI model.
 */
data class PronunciationScore(
    val accuracy: Float,
    val fluency: Float,
    val completeness: Float,
    val prosody: Float,
    val overall: Float,
    val recognitionConfidence: Float? = null,
    val issues: List<String> = emptyList(),
    val note: String = "",
) {
    init {
        require(accuracy in 0f..100f) { "accuracy must be in 0..100" }
        require(fluency in 0f..100f) { "fluency must be in 0..100" }
        require(completeness in 0f..100f) { "completeness must be in 0..100" }
        require(prosody in 0f..100f) { "prosody must be in 0..100" }
        require(overall in 0f..100f) { "overall must be in 0..100" }
        require(recognitionConfidence == null || recognitionConfidence in 0f..1f) {
            "recognitionConfidence must be in 0..1"
        }
    }
}
