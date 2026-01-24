package com.oneaccess.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.oneaccess.app.ui.OneAccessApp
import com.oneaccess.app.ui.theme.OneAccessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OneAccessTheme {
                OneAccessApp()
            }
        }
    }
}

