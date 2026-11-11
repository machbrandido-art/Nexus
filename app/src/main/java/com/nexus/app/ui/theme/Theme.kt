package com.nexus.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nexus.app.data.datastore.DEFAULT_SEED_COLOR_ARGB
import com.nexus.app.data.datastore.ThemeMode

/**
 * Seed choices exposed in Settings. Each entry is an ARGB long. Picking one only
 * matters when dynamic color is off (or on API < 31); on API 31+ dynamic color
 * takes priority and ignores the seed.
 */
data class SeedOption(val name: String, val argb: Long)

val SeedPalette: List<SeedOption> = listOf(
    SeedOption("Terracotta", 0xFFC96A48),
    SeedOption("Sage", 0xFF7A8B6A),
    SeedOption("Lavender", 0xFF9D7FBF),
    SeedOption("Ocean", 0xFF4A7FAB),
    SeedOption("Rose", 0xFFC25B7F),
    SeedOption("Amber", 0xFFE0A23C)
)

// Hand-tuned warm neutral fallback for the default seed. Keeps the app's identity
// when dynamic color is off. All other seeds derive from `schemeFromSeed`.
private val LightColors = lightColorScheme(
    primary = Color(0xFFC96A48),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2D9CC),
    onPrimaryContainer = Color(0xFF9E4E32),
    secondary = Color(0xFF7A8B6A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8DC),
    onSecondaryContainer = Color(0xFF3A4632),
    tertiary = Color(0xFF3D362E),
    onTertiary = Color.White,
    background = Color(0xFFFBF7F0),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFF5EFE6),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFF1EBDF),
    onSurfaceVariant = Color(0xFF3D362E),
    outline = Color(0xFFE6DECF),
    outlineVariant = Color(0xFFB8AD9D),
    error = Color(0xFFB3261E),
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
    tertiary = Color(0xFFF5EFE6),
    onTertiary = Color(0xFF1F1B16),
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

// M3 type scale. System default fonts (Roboto / Noto); no serif override.
private val NexusTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)

/**
 * Theme entry point. Reads `themeMode`, `dynamicColor`, and `seedColorArgb`
 * from the caller. Dynamic color needs API 31+ and the wallpaper to be set;
 * otherwise we fall back to a seed-derived or hand-tuned scheme.
 */
@Composable
fun NexusTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    seedColorArgb: Long = DEFAULT_SEED_COLOR_ARGB,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val seed = Color(seedColorArgb)

    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        seedColorArgb == DEFAULT_SEED_COLOR_ARGB -> if (dark) DarkColors else LightColors
        else -> schemeFromSeed(seed, dark)
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = NexusTypography,
        content = content
    )
}

/**
 * Minimal seed-based scheme: keep the warm neutral surfaces and swap the
 * primary + primaryContainer slots to match the seed. This is a pragmatic
 * fallback — full M3 tonal palette derivation would need the full HCT
 * algorithm, which we don't ship. Dynamic color on API 31+ uses the real
 * wallpaper palette and bypasses this entirely.
 */
private fun schemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
    val base = if (dark) DarkColors else LightColors
    return if (dark) {
        base.copy(
            primary = seed.lighten(0.35f),
            onPrimary = Color(0xFF1A1A1A),
            primaryContainer = seed.darken(0.35f),
            onPrimaryContainer = seed.lighten(0.85f)
        )
    } else {
        base.copy(
            primary = seed.darken(0.05f),
            onPrimary = Color.White,
            primaryContainer = seed.lighten(0.72f),
            onPrimaryContainer = seed.darken(0.55f)
        )
    }
}

private fun Color.lighten(amount: Float): Color = Color(
    red = (red + (1f - red) * amount).coerceIn(0f, 1f),
    green = (green + (1f - green) * amount).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * amount).coerceIn(0f, 1f),
    alpha = alpha
)

private fun Color.darken(amount: Float): Color = Color(
    red = (red * (1f - amount)).coerceIn(0f, 1f),
    green = (green * (1f - amount)).coerceIn(0f, 1f),
    blue = (blue * (1f - amount)).coerceIn(0f, 1f),
    alpha = alpha
)