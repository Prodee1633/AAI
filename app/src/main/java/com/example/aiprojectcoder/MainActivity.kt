package com.example.aiprojectcoder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.aiprojectcoder.ui.AIProjectCoderTheme
import com.example.aiprojectcoder.ui.AppScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AIProjectCoderTheme {
                AppScreen()
            }
        }
    }
}
