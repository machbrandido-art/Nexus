package com.nexus.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.app.data.datastore.ThemeMode

data class NexusColors(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val lime: Color,
    val limeDeep: Color,
    val limeSoft: Color,
    val ink: Color,
    val onInk: Color,
    val text: Color,
    val textMuted: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val isDark: Boolean
) {
    companion object {
        val Light = NexusColors(
            bg = Color(0xFFEAF7D5),
            surface = Color(0xFFFFFFFF),
            surfaceAlt = Color(0xFFF4FBE8),
            lime = Color(0xFFA8D13F),
            limeDeep = Color(0xFF7A9A2A),
            limeSoft = Color(0xFFD6EB9A),
            ink = Color(0xFF1A1A1A),
            onInk = Color(0xFFFFFFFF),
            text = Color(0xFF1A1A1A),
            textMuted = Color(0xFF5E6B4A),
            success = Color(0xFF3F8F5B),
            warning = Color(0xFFC58A1B),
            error = Color(0xFFC2403A),
            isDark = false
        )
        val Dark = NexusColors(
            bg = Color(0xFF121410),
            surface = Color(0xFF1A1A1A),
            surfaceAlt = Color(0xFF242720),
            lime = Color(0xFFA8D13F),
            limeDeep = Color(0xFF7A9A2A),
            limeSoft = Color(0xFF3A4720),
            ink = Color(0xFFEAF7D5),
            onInk = Color(0xFF1A1A1A),
            text = Color(0xFFF2F7E8),
            textMuted = Color(0xFFA5B08F),
            success = Color(0xFF5CB077),
            warning = Color(0xFFDBA53B),
            error = Color(0xFFD95A54),
            isDark = true
        )
    }
}

data class NexusTypography(
    val display: TextStyle,
    val title: TextStyle,
    val subtitle: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val mono: TextStyle
) {
    companion object {
        val Default = NexusTypography(
            display = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 43.sp
            ),
            title = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 30.sp
            ),
            subtitle = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            body = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp
            ),
            caption = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp
            ),
            mono = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        )
    }
}

data class NexusSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val screen: Dp = 16.dp,
    val touchTarget: Dp = 44.dp
)

val LocalNexusColors = staticCompositionLocalOf { NexusColors.Light }
val LocalNexusTypography = staticCompositionLocalOf { NexusTypography.Default }
val LocalNexusSpacing = staticCompositionLocalOf { NexusSpacing() }

@Composable
fun NexusTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    seedColorArgb: Long = 0L,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (dark) NexusColors.Dark else NexusColors.Light
    val typography = NexusTypography.Default
    val spacing = NexusSpacing()

    CompositionLocalProvider(
        LocalNexusColors provides colors,
        LocalNexusTypography provides typography,
        LocalNexusSpacing provides spacing
    ) {
        // TRANSITIONAL SHIM — removed once all screens use LocalNexusColors.
        // Its only job is to keep residual MaterialTheme.colorScheme references
        // resolving during the migration.
        MaterialTheme(
            colorScheme = if (dark) {
                darkColorScheme(
                    primary = colors.lime,
                    onPrimary = colors.onInk,
                    background = colors.bg,
                    onBackground = colors.text,
                    surface = colors.surface,
                    onSurface = colors.text,
                    surfaceVariant = colors.surfaceAlt,
                    onSurfaceVariant = colors.textMuted,
                    outline = colors.textMuted,
                    error = colors.error,
                    onError = Color.White
                )
            } else {
                lightColorScheme(
                    primary = colors.lime,
                    onPrimary = colors.onInk,
                    background = colors.bg,
                    onBackground = colors.text,
                    surface = colors.surface,
                    onSurface = colors.text,
                    surfaceVariant = colors.surfaceAlt,
                    onSurfaceVariant = colors.textMuted,
                    outline = colors.textMuted,
                    error = colors.error,
                    onError = Color.White
                )
            },
            typography = Typography(
                displayLarge = typography.display,
                headlineMedium = typography.title,
                titleMedium = typography.subtitle,
                bodyLarge = typography.body,
                labelSmall = typography.caption
            ),
            content = content
        )
    }
}