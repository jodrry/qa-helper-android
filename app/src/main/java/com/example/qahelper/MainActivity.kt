package com.example.qahelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.qahelper.ui.QaHelperApp
import com.example.qahelper.ui.theme.QAHelperTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QAHelperTheme {
                QaHelperApp()
            }
        }
    }
}
