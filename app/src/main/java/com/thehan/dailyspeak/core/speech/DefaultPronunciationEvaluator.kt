package com.thehan.dailyspeak.core.speech

import com.thehan.dailyspeak.domain.model.PronunciationScore
import com.thehan.dailyspeak.domain.service.TranscriptAwarePronunciationEvaluator
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local coarse evaluator.
 *
 * It combines recognizer confidence with objective signal metrics:
 * duration, voiced ratio, silence ratio, loudness and short-term dynamics.
 * It does not claim phoneme-level accuracy; that requires a future adapter.
 */
@Singleton
class DefaultPronunciationEvaluator @Inject constructor(
    private val pcmAudioDecoder: PcmAudioDecoder,
) : TranscriptAwarePronunciationEvaluator {
    override suspend fun evaluate(
        audioFile: File,
        expectedText: String,
    ): PronunciationScore = evaluate(
        audioFile = audioFile,
        expectedText = expectedText,
        transcript = "",
        recognitionConfidence = null,
    )

    override suspend fun evaluate(
        audioFile: File,
        expectedText: String,
        transcript: String,
        recognitionConfidence: Float?,
    ): PronunciationScore = withContext(Dispatchers.IO) {
        val decoded = pcmAudioDecoder.decode(audioFile)
        try {
            val features = analyzePcm(decoded)
            score(
                expectedText = expectedText,
                transcript = transcript,
                confidence = recognitionConfidence,
                features = features,
            )
        } finally {
            decoded.file.delete()
        }
    }

    private fun score(
        expectedText: String,
        transcript: String,
        confidence: Float?,
        features: AudioFeatures,
    ): PronunciationScore {
        val wordCount = ENGLISH_WORD_REGEX.findAll(transcript).count()
        val durationSeconds = max(features.durationMs / 1000f, 0.5f)
        val wordsPerMinute = wordCount * 60f / durationSeconds
        val expectedWordCount = max(12, ENGLISH_WORD_REGEX.findAll(expectedText).count() / 2)
        val signalQuality = features.rmsDbfs
            ?.let { (((it + 50f) / 35f) * 100f).coerceIn(0f, 100f) }
            ?: 50f
        val confidenceScore = confidence?.times(100f) ?: signalQuality

        val targetRateScore = when {
            wordsPerMinute < MIN_WORDS_PER_MINUTE -> {
                (wordsPerMinute / MIN_WORDS_PER_MINUTE * 100f).coerceIn(0f, 100f)
            }

            wordsPerMinute > MAX_WORDS_PER_MINUTE -> {
                (100f - (wordsPerMinute - MAX_WORDS_PER_MINUTE) * 1.2f).coerceIn(0f, 100f)
            }

            else -> 100f
        }
        val silenceRatio = features.silenceRatio ?: 0.25f
        val fluency = (
            targetRateScore * 0.55f +
                (100f - silenceRatio * 150f).coerceIn(0f, 100f) * 0.45f
            ).coerceIn(0f, 100f)

        val completeness = if (wordCount == 0) {
            0f
        } else {
            (wordCount.toFloat() / expectedWordCount * 100f).coerceIn(0f, 100f)
        }

        val dynamicRange = features.dynamicRangeDb ?: 6f
        val prosody = (
            (dynamicRange / TARGET_DYNAMIC_RANGE_DB * 100f).coerceIn(0f, 100f) * 0.65f +
                (features.voicedRatio ?: 0.5f).times(100f) * 0.35f
            ).coerceIn(0f, 100f)

        val accuracy = (
            confidenceScore * 0.75f +
                signalQuality * 0.25f
            ).coerceIn(0f, 100f)

        val issues = buildList {
            if (wordCount == 0) add("未获得有效转写，无法判断具体单词发音。")
            if (confidence != null && confidence < 0.55f) add("系统识别置信度偏低，可能存在咬字不清或环境噪声。")
            if (wordsPerMinute in 0.1f..<MIN_WORDS_PER_MINUTE) add("语速偏慢，建议减少犹豫并保持短语连贯。")
            if (wordsPerMinute > MAX_WORDS_PER_MINUTE) add("语速偏快，建议适当停顿以提升清晰度。")
            if (silenceRatio > 0.32f) add("停顿占比较高，建议先组织意群再开口。")
            if ((features.rmsDbfs ?: -24f) < -34f) add("整体音量偏低，建议靠近麦克风并保持稳定音量。")
            if (dynamicRange < 4f) add("语调起伏较少，可加强关键词重音和句末语调。")
            if (this.isEmpty()) add("未发现明显的音量、停顿或语速异常。")
        }

        val overall = (
            accuracy * 0.35f +
                fluency * 0.25f +
                completeness * 0.20f +
                prosody * 0.20f
            ).coerceIn(0f, 100f)

        return PronunciationScore(
            accuracy = accuracy.roundToTenth(),
            fluency = fluency.roundToTenth(),
            completeness = completeness.roundToTenth(),
            prosody = prosody.roundToTenth(),
            overall = overall.roundToTenth(),
            recognitionConfidence = confidence,
            issues = issues,
            note = "由系统识别置信度和音频特征粗略估算，非音素级评测。",
        )
    }

    private fun analyzePcm(decoded: DecodedPcmAudio): AudioFeatures {
        val bytes = decoded.file.readBytes()
        if (bytes.size < 2 || decoded.channelCount <= 0) {
            return AudioFeatures(durationMs = 0L)
        }

        val samples = ByteBuffer.wrap(bytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        val frameCount = samples.remaining() / decoded.channelCount
        if (frameCount <= 0) return AudioFeatures(durationMs = 0L)

        val samplesPerAnalysisFrame = max(1, decoded.sampleRateHz / 50)
        var sumSquares = 0.0
        var sampleCount = 0L
        var silentSamples = 0L
        var peak = 0
        var frameSquares = 0.0
        var frameSamples = 0
        val frameLevels = ArrayList<Float>(max(8, frameCount / samplesPerAnalysisFrame + 1))

        while (samples.hasRemaining()) {
            var mixed = 0
            repeat(decoded.channelCount) {
                if (samples.hasRemaining()) mixed += samples.get().toInt()
            }
            val sample = mixed / decoded.channelCount
            val normalized = sample / 32768f
            val absolute = abs(sample)
            peak = max(peak, absolute)
            sumSquares += normalized * normalized
            frameSquares += normalized * normalized
            sampleCount++
            frameSamples++
            if (absolute < SILENCE_AMPLITUDE) silentSamples++

            if (frameSamples >= samplesPerAnalysisFrame) {
                val frameRms = sqrt(frameSquares / frameSamples).toFloat()
                frameLevels += amplitudeToDb(frameRms)
                frameSquares = 0.0
                frameSamples = 0
            }
        }

        if (frameSamples > 0) {
            val frameRms = sqrt(frameSquares / frameSamples).toFloat()
            frameLevels += amplitudeToDb(frameRms)
        }

        val sortedLevels = frameLevels.sorted()
        val p10 = percentile(sortedLevels, 0.10f)
        val p90 = percentile(sortedLevels, 0.90f)
        val durationMs = frameCount * 1000L / decoded.sampleRateHz
        val rms = sqrt(sumSquares / sampleCount.coerceAtLeast(1L))
        val voicedFrames = frameLevels.count { it > VOICED_THRESHOLD_DB }

        return AudioFeatures(
            durationMs = durationMs,
            rmsDbfs = amplitudeToDb(rms.toFloat()),
            peakDbfs = amplitudeToDb(peak / 32768f),
            silenceRatio = silentSamples.toFloat() / sampleCount.coerceAtLeast(1L),
            voicedRatio = voicedFrames.toFloat() / frameLevels.size.coerceAtLeast(1),
            dynamicRangeDb = (p90 - p10).coerceAtLeast(0f),
        )
    }

    private fun percentile(sorted: List<Float>, percentile: Float): Float {
        if (sorted.isEmpty()) return 0f
        val index = ((sorted.size - 1) * percentile).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }

    private fun amplitudeToDb(amplitude: Float): Float =
        if (amplitude <= MIN_AMPLITUDE) MIN_DB else 20f * log10(amplitude)

    private fun Float.roundToTenth(): Float = (this * 10f).toInt().coerceIn(0, 1000) / 10f

    private data class AudioFeatures(
        val durationMs: Long,
        val rmsDbfs: Float? = null,
        val peakDbfs: Float? = null,
        val silenceRatio: Float? = null,
        val voicedRatio: Float? = null,
        val dynamicRangeDb: Float? = null,
    )

    private companion object {
        val ENGLISH_WORD_REGEX = Regex("[A-Za-z]+(?:'[A-Za-z]+)?")
        const val MIN_WORDS_PER_MINUTE = 80f
        const val MAX_WORDS_PER_MINUTE = 190f
        const val TARGET_DYNAMIC_RANGE_DB = 12f
        const val SILENCE_AMPLITUDE = 328
        const val VOICED_THRESHOLD_DB = -45f
        const val MIN_AMPLITUDE = 0.00001f
        const val MIN_DB = -100f
    }
}
