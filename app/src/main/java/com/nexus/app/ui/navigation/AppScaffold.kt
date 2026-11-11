package com.nexus.app.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.nexus.app.features.chat.ChatViewModel
import com.nexus.app.ui.theme.LocalNexusColors
import kotlinx.coroutines.launch

@Stable
class DrawerController(
    private val open: () -> Unit,
    private val close: () -> Unit
) {
    fun openDrawer() = open()
    fun closeDrawer() = close()
}

val LocalDrawerController = staticCompositionLocalOf<DrawerController> {
    error("LocalDrawerController not provided; wrap content in AppScaffold")
}

val LocalDrawerLayoutMode = staticCompositionLocalOf<DrawerLayoutMode> {
    DrawerLayoutMode.Modal
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AppScaffold(
    navController: NavHostController,
    chatViewModel: ChatViewModel,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val windowSizeClass = activity?.let { calculateWindowSizeClass(it) }
    val layoutMode = windowSizeClass?.let { rememberDrawerLayoutMode(it) } ?: DrawerLayoutMode.Modal

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val c = LocalNexusColors.current

    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val currentRoute = navController.currentBackStackEntry?.destination?.route

    val drawerUiState = DrawerUiState(
        recentConversations = chatState.recentConversations,
        activeModeId = chatState.modeId,
        activeProvider = chatState.providers.firstOrNull { it.id == chatState.selectedProviderId },
        activeModel = chatState.selectedModel,
        activeConversationId = chatState.conversationId,
        currentRoute = currentRoute
    )

    val drawerController = remember(drawerState, scope) {
        DrawerController(
            open = { scope.launch { drawerState.open() } },
            close = { scope.launch { drawerState.close() } }
        )
    }

    val drawerBody: @Composable () -> Unit = {
        AppDrawerContent(
            state = drawerUiState,
            onNewChat = {
                chatViewModel.newChat()
                navController.navigate("chat") { launchSingleTop = true }
                if (layoutMode != DrawerLayoutMode.Permanent) scope.launch { drawerState.close() }
            },
            onModeSelected = { modeId ->
                chatViewModel.switchMode(modeId)
                navController.navigate("chat") { launchSingleTop = true }
                if (layoutMode != DrawerLayoutMode.Permanent) scope.launch { drawerState.close() }
            },
            onAddMode = {
                // Mode editor is a later item; button is present per spec but inert.
            },
            onConversationSelected = { id ->
                chatViewModel.openConversation(id)
                navController.navigate("chat") { launchSingleTop = true }
                if (layoutMode != DrawerLayoutMode.Permanent) scope.launch { drawerState.close() }
            },
            onRenameConversation = { id, title -> chatViewModel.renameConversation(id, title) },
            onPinConversation = { id, pinned -> chatViewModel.setPinned(id, pinned) },
            onArchiveConversation = { id -> chatViewModel.setArchived(id, true) },
            onDeleteConversation = { id -> chatViewModel.deleteConversation(id) },
            onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                if (layoutMode != DrawerLayoutMode.Permanent) scope.launch { drawerState.close() }
            },
            onActiveProviderTap = {
                navController.navigate("providers") { launchSingleTop = true }
                if (layoutMode != DrawerLayoutMode.Permanent) scope.launch { drawerState.close() }
            },
            modifier = Modifier
                .fillMaxHeight()
                .width(320.dp)
        )
    }

    CompositionLocalProvider(
        LocalDrawerController provides drawerController,
        LocalDrawerLayoutMode provides layoutMode
    ) {
        when (layoutMode) {
            DrawerLayoutMode.Modal -> {
                if (drawerState.isOpen) {
                    BackHandler { scope.launch { drawerState.close() } }
                }
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = drawerBody
                ) {
                    content()
                }
            }

            DrawerLayoutMode.Permanent -> {
                PermanentNavigationDrawer(drawerContent = drawerBody) {
                    content()
                }
            }

            DrawerLayoutMode.Rail -> {
                if (drawerState.isOpen) {
                    BackHandler { scope.launch { drawerState.close() } }
                }
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = drawerBody
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        CustomRail(
                            state = drawerUiState,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNewChat = {
                                chatViewModel.newChat()
                                navController.navigate("chat") { launchSingleTop = true }
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(88.dp)
                                .background(c.surface)
                        )
                        Box(modifier = Modifier.weight(1f)) { content() }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomRail(
    state: DrawerUiState,
    onNavigate: (String) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RailItem(
            icon = Icons.Filled.Add,
            label = "New",
            selected = false,
            onClick = onNewChat
        )
        RailItem(
            icon = Icons.AutoMirrored.Filled.Chat,
            label = "Chat",
            selected = state.currentRoute == "chat",
            onClick = { onNavigate("chat") }
        )
        RailItem(
            icon = Icons.Filled.ViewKanban,
            label = "Kanban",
            selected = state.currentRoute == "kanban",
            onClick = { onNavigate("kanban") }
        )
        RailItem(
            icon = Icons.Filled.Cloud,
            label = "Providers",
            selected = state.currentRoute == "providers",
            onClick = { onNavigate("providers") }
        )
        RailItem(
            icon = Icons.Filled.Settings,
            label = "Settings",
            selected = state.currentRoute == "settings",
            onClick = { onNavigate("settings") }
        )
    }
}

@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val c = LocalNexusColors.current
    Box(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .size(56.dp)
            .clip(CircleShape)
            .background(if (selected) c.lime else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) c.ink else c.textMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}