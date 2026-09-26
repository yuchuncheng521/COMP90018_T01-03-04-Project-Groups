package com.knot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knot.app.navigation.KnotNavHost
import com.knot.app.ui.settings.AccountSettingsViewModel
import com.knot.app.ui.theme.KnotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: AccountSettingsViewModel = viewModel()
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                settingsViewModel.loadPreferences(context)
            }

            val appTheme = settingsViewModel.uiState.appTheme
            val textSizeMultiplier = settingsViewModel.uiState.textSizeMultiplier

            KnotTheme(
                appTheme = appTheme,
                textSizeMultiplier = textSizeMultiplier
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KnotNavHost(settingsViewModel = settingsViewModel)
                }
            }
        }
    }
}
