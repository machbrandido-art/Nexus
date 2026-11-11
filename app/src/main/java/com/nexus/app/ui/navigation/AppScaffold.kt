package com.nexus.app.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.nexus.app.features.chat.ChatViewModel
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

    val drawerContent: @Composable () -> Unit = {
        AppDrawerContent(
            state = drawerUiState,
            onNewChat = {
                chatViewModel.newChat()
                navController.navigate("chat") { launchSingleTop = true }
                if (layoutMode == DrawerLayoutMode.Modal) scope.launch { drawerState.close() }
            },
            onModeSelected = { modeId ->
                chatViewModel.switchMode(modeId)
                navController.navigate("chat") { launchSingleTop = true }
                if (layoutMode == DrawerLayoutMode.Modal) scope.launch { drawerState.close() }
            },
            onAddMode = {
                // Mode editor is a v1.2 item; no-op in v1.1
            },
            onConversationSelected = { id ->
                chatViewModel.openConversation(id)
                navController.navigate("chat") { launchSingleTop = true }
                if (layoutMode == DrawerLayoutMode.Modal) scope.launch { drawerState.close() }
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
                if (layoutMode == DrawerLayoutMode.Modal) scope.launch { drawerState.close() }
            },
            onActiveProviderTap = {
                navController.navigate("providers") { launchSingleTop = true }
                if (layoutMode == DrawerLayoutMode.Modal) scope.launch { drawerState.close() }
            }
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
                    drawerContent = { ModalDrawerSheet { drawerContent() } }
                ) {
                    content()
                }
            }

            DrawerLayoutMode.Permanent -> {
                PermanentNavigationDrawer(
                    drawerContent = { PermanentDrawerSheet { drawerContent() } }
                ) {
                    content()
                }
            }

            DrawerLayoutMode.Rail -> {
                if (drawerState.isOpen) {
                    BackHandler { scope.launch { drawerState.close() } }
                }
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = { ModalDrawerSheet { drawerContent() } }
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AppNavigationRail(
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
                            }
                        )
                        content()
                    }
                }
            }
        }
    }
}