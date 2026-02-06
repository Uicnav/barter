package com.barter.core.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Brand palette ──────────────────────────────────────────
val BarterTeal = Color(0xFF0E8C6E)
val BarterTealLight = Color(0xFF4ECBA7)
val BarterTealDark = Color(0xFF065C48)
val BarterAmber = Color(0xFFE8944A)
val BarterAmberLight = Color(0xFFFFBE76)
val BarterCoral = Color(0xFFE85454)
val BarterGreen = Color(0xFF27AE60)
val BarterCream = Color(0xFFFAF9F6)
val BarterDark = Color(0xFF2C3E50)
val BarterPurple = Color(0xFF7C5CFC)
val BarterBlue = Color(0xFF4FACFE)
val BarterPink = Color(0xFFF093FB)

// ── Neon / futuristic accents ────────────────────────────
val NeonCyan = Color(0xFF00E5FF)
val NeonTeal = Color(0xFF00FFB2)
val NeonPurple = Color(0xFFBB86FC)
val FutureDark = Color(0xFF0A0E1A)
val FutureSurface = Color(0xFF141B2D)
val FutureCard = Color(0xFF1A2240)
val FutureBorder = Color(0xFF2A3456)
val GlassWhite = Color(0x1AFFFFFF) // 10% white for glass effects

// ── Gradient lists ────────────────────────────────────────
val BarterGradientPrimary = listOf(BarterTeal, BarterTealLight)
val BarterGradientWarm = listOf(BarterAmber, BarterCoral)
val BarterGradientAccent = listOf(BarterPurple, BarterBlue)
val BarterGradientNeon = listOf(NeonCyan, NeonTeal)

// ── Light scheme ───────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary = BarterTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8E8D8),
    onPrimaryContainer = BarterTealDark,
    secondary = BarterAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0C0),
    onSecondaryContainer = Color(0xFF6B3A0A),
    tertiary = Color(0xFF7C5CFC),
    onTertiary = Color.White,
    background = BarterCream,
    onBackground = BarterDark,
    surface = Color.White,
    onSurface = BarterDark,
    surfaceVariant = Color(0xFFF0EDE8),
    onSurfaceVariant = Color(0xFF5A5A5A),
    error = BarterCoral,
    onError = Color.White,
    outline = Color(0xFFD0CCC4),
    outlineVariant = Color(0xFFE8E4DC),
)

// ── Futuristic Dark scheme ───────────────────────────────
private val DarkColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = FutureDark,
    primaryContainer = Color(0xFF003544),
    onPrimaryContainer = NeonCyan,
    secondary = BarterAmberLight,
    onSecondary = Color(0xFF3E2400),
    secondaryContainer = Color(0xFF6B3A0A),
    onSecondaryContainer = BarterAmberLight,
    tertiary = NeonPurple,
    onTertiary = Color(0xFF1A0044),
    background = FutureDark,
    onBackground = Color(0xFFE2E8F0),
    surface = FutureSurface,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = FutureCard,
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3E0000),
    outline = FutureBorder,
    outlineVariant = Color(0xFF1E2844),
)

// ── Typography ─────────────────────────────────────────────
val BarterTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

// ── Shapes ─────────────────────────────────────────────────
val BarterShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// ── Theme entry point (dark by default for futuristic look) ──
@Composable
fun BarterTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BarterTypography,
        shapes = BarterShapes,
    ) {
        if (darkTheme) {
            BarterBackground(content = content)
        } else {
            content()
        }
    }
}
