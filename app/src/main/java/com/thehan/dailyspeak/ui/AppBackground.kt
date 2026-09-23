package com.thehan.dailyspeak.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.thehan.dailyspeak.domain.model.BackgroundPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppBackground(
    preset: BackgroundPreset,
    imageUri: String,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = imageUri,
    ) {
        value = if (imageUri.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                decodeBackgroundImage(context, imageUri)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (imageBitmap != null) {
            Image(
                bitmap = requireNotNull(imageBitmap),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (darkTheme) {
                            Color.Black.copy(alpha = 0.46f)
                        } else {
                            Color.White.copy(alpha = 0.58f)
                        },
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(preset.gradient(darkTheme)),
            )
        }
    }
}

private fun BackgroundPreset.gradient(darkTheme: Boolean): Brush {
    val colors = if (darkTheme) {
        when (this) {
            BackgroundPreset.DEFAULT -> listOf(Color(0xFF211D16), Color(0xFF171510))
            BackgroundPreset.MIST -> listOf(Color(0xFF1B2028), Color(0xFF11161D))
            BackgroundPreset.MINT -> listOf(Color(0xFF0E2A1A), Color(0xFF091B11))
            BackgroundPreset.PEACH -> listOf(Color(0xFF381B13), Color(0xFF24110C))
            BackgroundPreset.SKY -> listOf(Color(0xFF0C2440), Color(0xFF09182B))
        }
    } else {
        when (this) {
            BackgroundPreset.DEFAULT -> listOf(Color(0xFFFAF4E8), Color(0xFFF3EBDD))
            BackgroundPreset.MIST -> listOf(Color(0xFFE8EDF3), Color(0xFFDDE6F0))
            BackgroundPreset.MINT -> listOf(Color(0xFFDDF3E3), Color(0xFFCDEAD8))
            BackgroundPreset.PEACH -> listOf(Color(0xFFFFE2D2), Color(0xFFF7D3C0))
            BackgroundPreset.SKY -> listOf(Color(0xFFD8E8FA), Color(0xFFC6DDF4))
        }
    }
    return Brush.verticalGradient(colors)
}

private fun decodeBackgroundImage(
    context: Context,
    imageUri: String,
): ImageBitmap? = runCatching {
    val uri = Uri.parse(imageUri)
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > MAX_BACKGROUND_DIMENSION ||
        bounds.outHeight / sampleSize > MAX_BACKGROUND_DIMENSION
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
    }
    resolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
    }
}.getOrNull()

private const val MAX_BACKGROUND_DIMENSION = 2_048
