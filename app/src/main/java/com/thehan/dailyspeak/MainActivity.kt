package com.thehan.dailyspeak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.thehan.dailyspeak.ui.DailySpeakApp
import com.thehan.dailyspeak.ui.theme.DailySpeakTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailySpeakTheme {
                DailySpeakApp()
            }
        }
    }
}