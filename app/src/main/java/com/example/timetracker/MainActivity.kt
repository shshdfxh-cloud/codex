package com.example.timetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = appColors) {
                Surface(modifier = Modifier.fillMaxSize(), color = appColors.background) {
                    TimeTrackerApp()
                }
            }
        }
    }
}

private val appColors = lightColorScheme(
    primary = Color(0xFF242424),
    secondary = Color(0xFF5B8CFF),
    background = Color(0xFFF7F4EF),
    surface = Color.White
)
