package com.thehan.dailyspeak.domain.coach

import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.Question
import com.thehan.dailyspeak.domain.model.ReferenceAnswers
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalContentFeedbackGeneratorTest {
    private val generator = LocalContentFeedbackGenerator()

    @Test
    fun blankTranscriptReturnsRecoverableGuidance() {
        val result = generator.generate(question = question(), transcript = "  ")

        assertTrue(result.overallComment.contains("未识别"))
        assertTrue(result.vocabularySuggestions.any { it.contains("重新回答") })
    }

    @Test
    fun shortAnswerReturnsStructureAndVocabularySuggestions() {
        val result = generator.generate(
            question = question(),
            transcript = "I like reading because it helps me learn.",
        )

        assertTrue(result.logicSuggestions.any { it.contains("观点") })
        assertTrue(result.vocabularySuggestions.any { it.contains("具体名词") })
    }

    @Test
    fun missingKeyPhraseIsReportedFromTranscript() {
        val result = generator.generate(
            question = question(keyPhrases = listOf("public transport", "city life")),
            transcript = "I usually walk to work because it keeps me healthy.",
        )

        assertTrue(result.vocabularySuggestions.any { it.contains("public transport") })
        assertTrue(result.vocabularySuggestions.any { it.contains("city life") })
    }

    private fun question(
        keyPhrases: List<String> = listOf("daily habit"),
    ): Question = Question(
        id = "test-question",
        englishQuestion = "What is one habit that improves your daily life?",
        chineseMeaning = "哪一个习惯能改善你的日常生活？",
        topic = "日常生活",
        difficulty = PracticeLevel.DAILY,
        source = "test",
        keyPhrases = keyPhrases,
        referenceAnswers = ReferenceAnswers(
            daily = "I go for a walk every day.",
            advanced = "I go for a walk every day because it clears my mind.",
            postgraduate = "Taking a daily walk helps me stay healthy and focused.",
        ),
        createdAtEpochMillis = 1L,
    )
}