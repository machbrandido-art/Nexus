package com.nexus.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.data.datastore.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Warm neutral palette — Anthropic-leaning calm with one terracotta accent.
private val WarmCream = Color(0xFFF5EFE6)
private val WarmPaper = Color(0xFFFBF7F0)
private val WarmInk = Color(0xFF1F1B16)
private val WarmInkSoft = Color(0xFF3D362E)
private val WarmMuted = Color(0xFFB8AD9D)
private val Terracotta = Color(0xFFC96A48)
private val TerracottaDeep = Color(0xFF9E4E32)
private val SageGreen = Color(0xFF7A8B6A)
private val BorderSoft = Color(0xFFE6DECF)
private val ErrorRed = Color(0xFFB3261E)

private val LightColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2D9CC),
    onPrimaryContainer = TerracottaDeep,
    secondary = SageGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8DC),
    onSecondaryContainer = Color(0xFF3A4632),
    tertiary = WarmInkSoft,
    onTertiary = Color.White,
    background = WarmPaper,
    onBackground = WarmInk,
    surface = WarmCream,
    onSurface = WarmInk,
    surfaceVariant = Color(0xFFF1EBDF),
    onSurfaceVariant = WarmInkSoft,
    outline = BorderSoft,
    outlineVariant = WarmMuted,
    error = ErrorRed,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8A585),
    onPrimary = Color(0xFF2A1A12),
    primaryContainer = Color(0xFF5C3624),
    onPrimaryContainer = Color(0xFFF7D9C7),
    secondary = Color(0xFFA9BD98),
    onSecondary = Color(0xFF1C2614),
    secondaryContainer = Color(0xFF3A4730),
    onSecondaryContainer = Color(0xFFD3E3C2),
    tertiary = WarmCream,
    onTertiary = WarmInk,
    background = Color(0xFF15120E),
    onBackground = Color(0xFFECE4D6),
    surface = Color(0xFF1B1814),
    onSurface = Color(0xFFECE4D6),
    surfaceVariant = Color(0xFF2A251F),
    onSurfaceVariant = Color(0xFFC9BEAC),
    outline = Color(0xFF4A423A),
    outlineVariant = Color(0xFF3A332C),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

private val NexusTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, lineHeight = 52.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

@Composable
fun NexusTheme(
    themeModeFlow: Flow<ThemeMode>? = null,
    content: @Composable () -> Unit
) {
    val flow = themeModeFlow ?: flowOf(ThemeMode.SYSTEM)
    val mode by flow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = NexusTypography,
        content = content
    )
}