package com.nexus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.features.chat.ChatViewModel
import com.nexus.app.features.settings.SettingsViewModel
import com.nexus.app.ui.navigation.NexusNavGraph
import com.nexus.app.ui.theme.NexusTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoinContext {
                val settingsVm: SettingsViewModel = koinViewModel()
                val chatVm: ChatViewModel = koinViewModel()
                val themeMode by settingsVm.themeMode.collectAsStateWithLifecycle()
                val dynamicColor by settingsVm.dynamicColor.collectAsStateWithLifecycle()
                val seedColor by settingsVm.seedColorArgb.collectAsStateWithLifecycle()

                NexusTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    seedColorArgb = seedColor
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NexusNavGraph(
                            chatViewModel = chatVm,
                            settingsViewModel = settingsVm
                        )
                    }
                }
            }
        }
    }
}