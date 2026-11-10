package com.nexus.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexus.app.R
import com.nexus.app.features.chat.ChatScreen
import com.nexus.app.features.kanban.KanbanScreen
import com.nexus.app.features.providers.ProvidersScreen
import com.nexus.app.features.settings.SettingsScreen

private sealed class Destination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    data object Chat : Destination("chat", R.string.nav_chat, Icons.AutoMirrored.Filled.Chat)
    data object Kanban : Destination("kanban", R.string.nav_kanban, Icons.Filled.ViewKanban)
    data object Providers : Destination("providers", R.string.nav_providers, Icons.Filled.Cloud)
    data object Settings : Destination("settings", R.string.nav_settings, Icons.Filled.Settings)
}

private val BottomItems = listOf(Destination.Chat, Destination.Kanban, Destination.Providers, Destination.Settings)

@Composable
fun NexusNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                        label = { Text(stringResource(item.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Chat.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Chat.route) {
                ChatScreen(
                    onOpenProviders = {
                        navController.navigate(Destination.Providers.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Destination.Kanban.route) { KanbanScreen() }
            composable(Destination.Providers.route) { ProvidersScreen() }
            composable(Destination.Settings.route) { SettingsScreen() }
        }
    }
}