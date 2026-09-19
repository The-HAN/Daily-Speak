package com.thehan.dailyspeak.data.remote.deepseek

import com.thehan.dailyspeak.domain.model.AccentPreference
import com.thehan.dailyspeak.domain.model.Feedback
import com.thehan.dailyspeak.domain.model.FeedbackSource
import com.thehan.dailyspeak.domain.model.ImprovementSuggestion
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.PronunciationScore
import com.thehan.dailyspeak.domain.model.Question
import com.thehan.dailyspeak.domain.model.ReferenceAnswers
import com.thehan.dailyspeak.domain.service.DeepSeekService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class DeepSeekServiceImpl @Inject constructor(
    private val remoteDataSource: DeepSeekRemoteDataSource,
    private val json: Json,
) : DeepSeekService {
    override suspend fun generateDailyQuestions(
        date: String,
        count: Int,
        level: PracticeLevel,
        topics: List<String>,
    ): List<Question> {
        val requestedCount = count.coerceIn(1, 20)
        val raw = remoteDataSource.complete(
            systemPrompt = QUESTION_SYSTEM_PROMPT,
            userPrompt = buildString {
                appendLine("日期：$date")
                appendLine("题量：$requestedCount")
                appendLine("难度：${level.toPromptValue()}")
                appendLine("话题偏好：${topics.ifEmpty { listOf("不限") }.joinToString("、")}")
                appendLine()
                appendLine("只返回 JSON，不要解释，不要 Markdown：")
                appendLine(
                    """{"questions":[{"englishQuestion":"...","chineseMeaning":"...","topic":"...","difficulty":"DAILY 或 POSTGRADUATE","keyPhrases":["..."],"referenceAnswers":{"daily":"...","advanced":"...","postgraduate":"..."}}]}""",
                )
            },
            temperature = 0.85,
        )
        val payload = decode(
            raw = raw,
            serializer = QuestionsEnvelope.serializer(),
        )
        return payload.questions
            .take(requestedCount)
            .mapIndexed { index, item ->
                val difficulty = item.difficulty.toPracticeLevel()
                Question(
                    id = "deepseek-$date-${index}-${item.englishQuestion.hashCode().toString().replace('-', 'n')}",
                    englishQuestion = item.englishQuestion.trim(),
                    chineseMeaning = item.chineseMeaning.trim(),
                    topic = item.topic.trim().ifBlank { "综合" },
                    difficulty = difficulty,
                    source = FeedbackSource.DEEPSEEK,
                    keyPhrases = item.keyPhrases.map(String::trim).filter(String::isNotBlank).distinct(),
                    referenceAnswers = ReferenceAnswers(
                        daily = item.referenceAnswers.daily.trim(),
                        advanced = item.referenceAnswers.advanced.trim(),
                        postgraduate = item.referenceAnswers.postgraduate.trim(),
                    ),
                    createdAtEpochMillis = System.currentTimeMillis() + index,
                )
            }
            .filter { it.englishQuestion.isNotBlank() }
    }

    override suspend fun translateQuestion(question: Question): String {
        val raw = remoteDataSource.complete(
            systemPrompt = TRANSLATION_SYSTEM_PROMPT,
            userPrompt = buildString {
                appendLine("英文问句：${question.englishQuestion}")
                appendLine("只返回 JSON：{\"chineseMeaning\":\"准确、自然的中文意思\"}")
            },
            temperature = 0.2,
        )
        return decode(raw, ChineseMeaningEnvelope.serializer()).chineseMeaning.trim()
    }

    override suspend fun generateReferenceAnswers(
        question: Question,
        level: PracticeLevel,
    ): ReferenceAnswers {
        val raw = remoteDataSource.complete(
            systemPrompt = REFERENCE_SYSTEM_PROMPT,
            userPrompt = buildString {
                appendLine("英文问句：${question.englishQuestion}")
                appendLine("用户目标难度：${level.toPromptValue()}")
                appendLine("只返回 JSON：")
                appendLine(
                    """{"daily":"2-3 句自然日常回答","advanced":"4-5 句进阶回答","postgraduate":"6-8 句考研口语表达"}""",
                )
            },
            temperature = 0.6,
        )
        val payload = decode(raw, ReferenceAnswersPayload.serializer())
        return ReferenceAnswers(
            daily = payload.daily.trim(),
            advanced = payload.advanced.trim(),
            postgraduate = payload.postgraduate.trim(),
        )
    }

    override suspend fun evaluateAnswer(
        question: Question,
        transcript: String,
        pronunciationScore: PronunciationScore,
    ): Feedback {
        val raw = remoteDataSource.complete(
            systemPrompt = FEEDBACK_SYSTEM_PROMPT,
            userPrompt = buildString {
                appendLine("英文问句：${question.englishQuestion}")
                appendLine("参考回答（进阶版）：${question.referenceAnswers.advanced}")
                appendLine("用户转写：$transcript")
                appendLine("发音粗略评分：${pronunciationScore.overall}/100")
                appendLine("只返回 JSON：")
                appendLine(
                    """{"pronunciationIssues":["..."],"grammarSuggestions":["..."],"vocabularySuggestions":["..."],"logicSuggestions":["..."],"naturalExpressionSuggestions":["..."],"betterAnswer":"...","overallComment":"..."}""",
                )
            },
            temperature = 0.25,
        )
        val payload = decode(raw, FeedbackPayload.serializer())
        return Feedback(
            attemptId = 0L,
            accuracy = pronunciationScore.accuracy,
            fluency = pronunciationScore.fluency,
            completeness = pronunciationScore.completeness,
            prosody = pronunciationScore.prosody,
            overall = pronunciationScore.overall,
            pronunciationIssues = payload.pronunciationIssues.clean(),
            grammarSuggestions = payload.grammarSuggestions.clean(),
            vocabularySuggestions = payload.vocabularySuggestions.clean(),
            logicSuggestions = payload.logicSuggestions.clean(),
            naturalExpressionSuggestions = payload.naturalExpressionSuggestions.clean(),
            betterAnswer = payload.betterAnswer.trim(),
            overallComment = payload.overallComment.trim(),
            source = FeedbackSource.DEEPSEEK,
            createdAtEpochMillis = System.currentTimeMillis(),
        )
    }

    override suspend fun improveAnswer(
        question: Question,
        transcript: String,
    ): ImprovementSuggestion {
        val raw = remoteDataSource.complete(
            systemPrompt = IMPROVEMENT_SYSTEM_PROMPT,
            userPrompt = buildString {
                appendLine("英文问句：${question.englishQuestion}")
                appendLine("用户回答：$transcript")
                appendLine("只返回 JSON：")
                appendLine(
                    """{"grammarSuggestions":["..."],"vocabularySuggestions":["..."],"logicSuggestions":["..."],"naturalExpressionSuggestions":["..."],"betterAnswer":"...","overallComment":"..."}""",
                )
            },
            temperature = 0.35,
        )
        val payload = decode(raw, ImprovementPayload.serializer())
        return ImprovementSuggestion(
            grammarSuggestions = payload.grammarSuggestions.clean(),
            vocabularySuggestions = payload.vocabularySuggestions.clean(),
            logicSuggestions = payload.logicSuggestions.clean(),
            naturalExpressionSuggestions = payload.naturalExpressionSuggestions.clean(),
            betterAnswer = payload.betterAnswer.trim(),
            overallComment = payload.overallComment.trim(),
        )
    }

    private fun <T> decode(raw: String, serializer: kotlinx.serialization.KSerializer<T>): T {
        val jsonText = extractJson(raw)
        return runCatching { json.decodeFromString(serializer, jsonText) }
            .getOrElse { error ->
                throw IllegalStateException("DeepSeek 返回格式无法解析，请稍后重试。", error)
            }
    }

    private fun extractJson(raw: String): String {
        val cleaned = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        check(start >= 0 && end > start) { "DeepSeek 未返回 JSON 对象。" }
        return cleaned.substring(start, end + 1)
    }

    private fun List<String>.clean(): List<String> =
        map(String::trim).filter(String::isNotBlank).distinct()

    private fun PracticeLevel.toPromptValue(): String = when (this) {
        PracticeLevel.DAILY -> "日常"
        PracticeLevel.POSTGRADUATE -> "考研"
        PracticeLevel.MIXED -> "日常与考研混合"
    }

    private fun String.toPracticeLevel(): PracticeLevel =
        if (contains("POST", ignoreCase = true) || contains("考研")) {
            PracticeLevel.POSTGRADUATE
        } else {
            PracticeLevel.DAILY
        }

    @Serializable
    private data class QuestionsEnvelope(
        val questions: List<QuestionPayload> = emptyList(),
    )

    @Serializable
    private data class QuestionPayload(
        val englishQuestion: String = "",
        val chineseMeaning: String = "",
        val topic: String = "",
        val difficulty: String = "",
        val keyPhrases: List<String> = emptyList(),
        val referenceAnswers: ReferenceAnswersPayload = ReferenceAnswersPayload(),
    )

    @Serializable
    private data class ReferenceAnswersPayload(
        val daily: String = "",
        val advanced: String = "",
        val postgraduate: String = "",
    )

    @Serializable
    private data class ChineseMeaningEnvelope(
        val chineseMeaning: String = "",
    )

    @Serializable
    private data class FeedbackPayload(
        val pronunciationIssues: List<String> = emptyList(),
        val grammarSuggestions: List<String> = emptyList(),
        val vocabularySuggestions: List<String> = emptyList(),
        val logicSuggestions: List<String> = emptyList(),
        val naturalExpressionSuggestions: List<String> = emptyList(),
        val betterAnswer: String = "",
        val overallComment: String = "",
    )

    @Serializable
    private data class ImprovementPayload(
        val grammarSuggestions: List<String> = emptyList(),
        val vocabularySuggestions: List<String> = emptyList(),
        val logicSuggestions: List<String> = emptyList(),
        val naturalExpressionSuggestions: List<String> = emptyList(),
        val betterAnswer: String = "",
        val overallComment: String = "",
    )

    private companion object {
        const val QUESTION_SYSTEM_PROMPT =
            "你是考研英语口语教练。题目必须原创、可口语作答、避免敏感或侵权内容。严格输出用户要求的 JSON。"
        const val TRANSLATION_SYSTEM_PROMPT =
            "你是英语教师。将英文问句翻译成准确自然的中文，严格输出 JSON。"
        const val REFERENCE_SYSTEM_PROMPT =
            "你是考研英语口语教练。给出符合不同水平的原创参考回答，结构清晰、表达自然，严格输出 JSON。"
        const val FEEDBACK_SYSTEM_PROMPT =
            "你是严谨的英语口语教练。只根据用户真实转写给出语法、词汇、逻辑、地道表达建议，不编造用户没有说过的内容。严格输出 JSON。"
        const val IMPROVEMENT_SYSTEM_PROMPT =
            "你是考研英语口语教练。针对用户回答给出具体可执行的改善建议和更好的原创回答，严格输出 JSON。"
    }
}
