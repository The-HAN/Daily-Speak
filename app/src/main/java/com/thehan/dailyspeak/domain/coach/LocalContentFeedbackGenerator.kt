package com.thehan.dailyspeak.domain.coach

import com.thehan.dailyspeak.domain.model.ImprovementSuggestion
import com.thehan.dailyspeak.domain.model.Question
import javax.inject.Inject

/**
 * Produces immediate, deterministic text feedback without network access.
 *
 * This is intentionally a lightweight screen, not a grammar engine: it only
 * flags observable patterns in the transcript and missing key phrases. The
 * DeepSeek-backed coach remains the source of richer language feedback.
 */
class LocalContentFeedbackGenerator @Inject constructor() {
    fun generate(
        question: Question,
        transcript: String,
    ): ImprovementSuggestion {
        val normalized = transcript.trim()
        if (normalized.isBlank()) {
            return ImprovementSuggestion(
                grammarSuggestions = listOf("没有获得可用转写，暂时无法分析句子结构。"),
                vocabularySuggestions = listOf("请靠近麦克风重新回答，或切换可用的语音识别服务。"),
                logicSuggestions = listOf("建议先给出观点，再补充原因或例子。"),
                naturalExpressionSuggestions = listOf("重录后会自动生成新的本地文本初筛。"),
                betterAnswer = question.referenceAnswers.advanced,
                overallComment = "录音已保存，但未识别到有效英文内容。",
            )
        }

        val words = ENGLISH_WORD_REGEX.findAll(normalized).map { it.value }.toList()
        val lowerCase = normalized.lowercase()
        val sentenceCount = normalized
            .split(Regex("[.!?]+"))
            .count { it.isNotBlank() }
        val missingKeyPhrases = question.keyPhrases.filterNot { phrase ->
            phrase.lowercase() in lowerCase
        }
        val hasConnector = CONNECTORS.any { it in lowerCase }
        val hasOpinionCue = OPINION_CUES.any { it in lowerCase }
        val hasSentencePunctuation = normalized.any { it in ".!?" }

        return ImprovementSuggestion(
            grammarSuggestions = buildList {
                if (Regex("\\bi\\b").containsMatchIn(normalized)) {
                    add("第一人称代词 I 需要大写；这是口语转写中常见的书写规范问题。")
                }
                if (!hasSentencePunctuation && words.size >= MIN_ANSWER_WORDS) {
                    add("转写文本没有句末标点；回听时可检查意群停顿，并尝试用连接词衔接较长的回答。")
                }
                if (words.size < MIN_ANSWER_WORDS) {
                    add("句子数量偏少，可检查时态、主谓一致和单复数是否完整。")
                }
                if (isEmpty()) {
                    add("未发现明显的转写级语法风险；正式表达前建议再核对时态和主谓一致。")
                }
            },
            vocabularySuggestions = buildList {
                if (missingKeyPhrases.isEmpty()) {
                    add("已覆盖题目关键词，可继续用具体细节替换笼统词。")
                } else {
                    missingKeyPhrases.take(MAX_KEY_PHRASE_HINTS).forEach { phrase ->
                        add("可以尝试自然加入关键词：$phrase。")
                    }
                }
                if (words.size < MIN_ANSWER_WORDS) {
                    add("补充一个具体名词或场景细节，能减少回答过于笼统的感觉。")
                }
            },
            logicSuggestions = buildList {
                if (words.size < MIN_ANSWER_WORDS) {
                    add("回答较短，建议按“观点 → 原因 → 例子/细节 → 总结”展开。")
                } else if (!hasConnector) {
                    add("可加入 because、for example、however 等连接词，让观点之间的关系更清楚。")
                } else {
                    add("已使用一定衔接表达，可再检查每个理由是否直接支持中心观点。")
                }
                if (sentenceCount <= 1 && words.size >= MIN_ANSWER_WORDS) {
                    add("整段只有一句话，建议在理由和例子之间增加停顿。")
                }
            },
            naturalExpressionSuggestions = buildList {
                if (!hasOpinionCue) {
                    add("可在开头使用 Personally speaking / I'd say / In my view 等自然引入观点。")
                } else {
                    add("观点引入较自然，可尝试加入一个口语化细节或简短例子。")
                }
                if (!CONTRACTIONS.any { it in lowerCase }) {
                    add("口语中适度使用 I'd、it's、I've 等缩合形式，会更接近自然对话。")
                }
            },
            betterAnswer = question.referenceAnswers.advanced,
            overallComment = "以上为基于转写文本的本地初筛建议；点击下方按钮可获得针对原句和上下文的 AI 分析。",
        )
    }

    private companion object {
        val ENGLISH_WORD_REGEX = Regex("[A-Za-z]+(?:'[A-Za-z]+)?")
        val CONNECTORS = listOf(
            " because ",
            " so ",
            " for example ",
            " however ",
            " although ",
            " therefore ",
            " first ",
            " finally ",
        )
        val OPINION_CUES = listOf(
            "i think",
            "i believe",
            "in my opinion",
            "i'd say",
            "personally",
        )
        val CONTRACTIONS = listOf("i'd", "it's", "i've", "don't", "can't", "that's")
        const val MIN_ANSWER_WORDS = 20
        const val MAX_KEY_PHRASE_HINTS = 3
    }
}
