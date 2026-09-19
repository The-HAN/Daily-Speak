package com.thehan.dailyspeak.core.speech

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DecodedPcmAudio(
    val file: File,
    val sampleRateHz: Int,
    val channelCount: Int,
    val pcmEncoding: Int,
)

/**
 * Decodes an m4a/AAC recording into a temporary PCM file.
 *
 * Android SpeechRecognizer's file-source API expects raw PCM, while
 * MediaRecorder produces a container file. Keeping this conversion in one
 * component avoids pretending that an m4a file can be passed directly.
 */
@Singleton
class PcmAudioDecoder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun decode(inputFile: File): DecodedPcmAudio = withContext(Dispatchers.IO) {
        require(inputFile.exists()) { "音频文件不存在。" }

        val outputFile = File.createTempFile("speech_", ".pcm", context.cacheDir)
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(inputFile.absolutePath)
            val trackIndex = firstAudioTrack(extractor)
            require(trackIndex >= 0) { "录音中没有可识别的音轨。" }

            extractor.selectTrack(trackIndex)
            val sourceFormat = extractor.getTrackFormat(trackIndex)
            val mime = sourceFormat.getString(MediaFormat.KEY_MIME)
                ?: error("无法读取录音编码格式。")
            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(sourceFormat, null, null, 0)
            decoder.start()

            var outputFormat: MediaFormat = sourceFormat
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            BufferedOutputStream(outputFile.outputStream()).use { output ->
                while (!outputDone) {
                    if (!inputDone) {
                        val inputIndex = decoder.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                        if (inputIndex >= 0) {
                            val inputBuffer = decoder.getInputBuffer(inputIndex)
                                ?: error("无法申请音频解码缓冲区。")
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime,
                                    0,
                                )
                                extractor.advance()
                            }
                        }
                    }

                    when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_US)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            outputFormat = decoder.outputFormat
                        }

                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit

                        else -> if (outputIndex >= 0) {
                            val outputBuffer = decoder.getOutputBuffer(outputIndex)
                            if (outputBuffer != null && bufferInfo.size > 0) {
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                writeBuffer(output, outputBuffer)
                            }
                            decoder.releaseOutputBuffer(outputIndex, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputDone = true
                            }
                        }
                    }
                }
            }

            val sampleRate = outputFormat.getIntegerOrDefault(
                MediaFormat.KEY_SAMPLE_RATE,
                DEFAULT_SAMPLE_RATE,
            )
            val channelCount = outputFormat.getIntegerOrDefault(
                MediaFormat.KEY_CHANNEL_COUNT,
                1,
            )

            DecodedPcmAudio(
                file = outputFile,
                sampleRateHz = sampleRate,
                channelCount = channelCount,
                pcmEncoding = outputFormat.getIntegerOrDefault(
                    MediaFormat.KEY_PCM_ENCODING,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                ),
            )
        } catch (error: Throwable) {
            outputFile.delete()
            throw IllegalStateException("录音解码失败：${error.message ?: "未知错误"}", error)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun firstAudioTrack(extractor: MediaExtractor): Int {
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) return index
        }
        return -1
    }

    private fun writeBuffer(
        output: BufferedOutputStream,
        buffer: ByteBuffer,
    ) {
        if (buffer.hasArray()) {
            output.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining())
        } else {
            val copy = ByteBuffer.allocate(buffer.remaining()).order(ByteOrder.LITTLE_ENDIAN)
            copy.put(buffer)
            output.write(copy.array())
        }
    }

    private fun MediaFormat.getIntegerOrDefault(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback

    private companion object {
        const val DEQUEUE_TIMEOUT_US = 10_000L
        const val DEFAULT_SAMPLE_RATE = 16_000
    }
}
