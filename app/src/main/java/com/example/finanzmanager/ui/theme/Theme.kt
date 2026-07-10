package com.example.finanzmanager.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---- Brand Colors ----
val PrimaryBlue      = Color(0xFF3B82F6)
val PrimaryBlueDark  = Color(0xFF1D4ED8)
val AccentGreen      = Color(0xFF10B981)
val AccentRed        = Color(0xFFEF4444)
val AccentPurple     = Color(0xFF8B5CF6)
val AccentAmber      = Color(0xFFF59E0B)

// Hero card gradient stops
val HeroGradientStart = Color(0xFF1D4ED8)
val HeroGradientEnd   = Color(0xFF4F46E5)

// Dark scheme
private val DarkColorScheme = darkColorScheme(
    primary             = PrimaryBlue,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFF1E3A5F),
    onPrimaryContainer  = Color(0xFFBFDBFE),
    secondary           = AccentGreen,
    onSecondary         = Color.White,
    background          = Color(0xFF0F172A),
    onBackground        = Color(0xFFF1F5F9),
    surface             = Color(0xFF1E293B),
    onSurface           = Color(0xFFF1F5F9),
    surfaceVariant      = Color(0xFF334155),
    onSurfaceVariant    = Color(0xFF94A3B8),
    outline             = Color(0xFF334155),
    error               = AccentRed,
    onError             = Color.White
)

// Light scheme
private val LightColorScheme = lightColorScheme(
    primary             = PrimaryBlue,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFDBEAFE),
    onPrimaryContainer  = Color(0xFF1D4ED8),
    secondary           = AccentGreen,
    onSecondary         = Color.White,
    background          = Color(0xFFF8FAFC),
    onBackground        = Color(0xFF0F172A),
    surface             = Color.White,
    onSurface           = Color(0xFF0F172A),
    surfaceVariant      = Color(0xFFF1F5F9),
    onSurfaceVariant    = Color(0xFF64748B),
    outline             = Color(0xFFE2E8F0),
    error               = AccentRed,
    onError             = Color.White
)

private val AppTypography = Typography(
    titleLarge  = TextStyle(fontWeight = FontWeight.Black,  fontSize = 22.sp, letterSpacing = (-0.5).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 16.sp),
    bodyMedium  = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall   = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall  = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
)

@Composable
fun FinanzManagerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}
