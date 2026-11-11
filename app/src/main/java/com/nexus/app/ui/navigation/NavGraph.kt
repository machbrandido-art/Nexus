package com.nexus.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexus.app.features.chat.ChatScreen
import com.nexus.app.features.chat.ChatViewModel
import com.nexus.app.features.kanban.KanbanScreen
import com.nexus.app.features.providers.ProvidersScreen
import com.nexus.app.features.settings.SettingsScreen
import com.nexus.app.features.settings.SettingsViewModel

@Composable
fun NexusNavGraph(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()

    AppScaffold(
        navController = navController,
        chatViewModel = chatViewModel
    ) {
        NavHost(
            navController = navController,
            startDestination = "chat"
        ) {
            composable("chat") {
                ChatScreen(
                    viewModel = chatViewModel,
                    onOpenProviders = {
                        navController.navigate("providers") { launchSingleTop = true }
                    }
                )
            }
            composable("kanban") { KanbanScreen() }
            composable("providers") { ProvidersScreen() }
            composable("settings") { SettingsScreen(viewModel = settingsViewModel) }
        }
    }
}