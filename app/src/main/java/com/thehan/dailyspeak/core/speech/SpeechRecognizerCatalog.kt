package com.thehan.dailyspeak.core.speech

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SpeechRecognizerServiceInfo(
    val label: String,
    val packageName: String,
    val componentName: String,
)

data class SpeechRecognizerCatalogSnapshot(
    val services: List<SpeechRecognizerServiceInfo> = emptyList(),
    val isOnDeviceRecognitionAvailable: Boolean = false,
)

@Singleton
class SpeechRecognizerCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun snapshot(): SpeechRecognizerCatalogSnapshot {
        val packageManager = context.packageManager
        val intent = Intent(RecognitionService.SERVICE_INTERFACE)
        val services = queryRecognitionServices(packageManager, intent)
            .mapNotNull { resolveInfo -> resolveInfo.toServiceInfo(packageManager) }
            .distinctBy(SpeechRecognizerServiceInfo::componentName)
            .sortedBy { it.label.lowercase() }

        val onDeviceAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            }.getOrDefault(false)
        } else {
            false
        }

        return SpeechRecognizerCatalogSnapshot(
            services = services,
            isOnDeviceRecognitionAvailable = onDeviceAvailable,
        )
    }

    @Suppress("DEPRECATION")
    private fun queryRecognitionServices(
        packageManager: PackageManager,
        intent: Intent,
    ): List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentServices(
            intent,
            PackageManager.ResolveInfoFlags.of(0L),
        )
    } else {
        packageManager.queryIntentServices(intent, 0)
    }

    private fun ResolveInfo.toServiceInfo(
        packageManager: PackageManager,
    ): SpeechRecognizerServiceInfo? {
        val serviceInfo = serviceInfo ?: return null
        val component = ComponentName(serviceInfo.packageName, serviceInfo.name)
        return SpeechRecognizerServiceInfo(
            label = loadLabel(packageManager).toString().ifBlank { serviceInfo.packageName },
            packageName = serviceInfo.packageName,
            componentName = component.flattenToString(),
        )
    }
}
