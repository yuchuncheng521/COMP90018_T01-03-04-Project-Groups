package com.knot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.knot.app.navigation.KnotNavHost
import com.knot.app.ui.theme.KnotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KnotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KnotNavHost()
                }
            }
        }
    }
}
