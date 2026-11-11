package com.nexus.app.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

enum class DrawerLayoutMode { Modal, Rail, Permanent }

@Composable
fun rememberDrawerLayoutMode(windowSizeClass: WindowSizeClass): DrawerLayoutMode =
    remember(windowSizeClass) {
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Expanded -> DrawerLayoutMode.Permanent
            WindowWidthSizeClass.Medium -> DrawerLayoutMode.Rail
            else -> DrawerLayoutMode.Modal
        }
    }