package com.viciousscan.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Vicious Scan palette — dark cyber / terminal aesthetic
val ViciousRed       = Color(0xFFE53935)   // primary accent
val ViciousRedDim    = Color(0xFF7F0000)
val ViciousOrange    = Color(0xFFFF6D00)   // REQUIRED severity
val ViciousYellow    = Color(0xFFFFD600)   // RECOMMENDED severity
val ViciousGreen     = Color(0xFF00E676)   // OPTIONAL / success
val ViciousSurface   = Color(0xFF0D0D0D)   // near-black background
val ViciousCard      = Color(0xFF1A1A1A)
val ViciousBorder    = Color(0xFF2C2C2C)
val ViciousOnSurface = Color(0xFFE0E0E0)
val ViciousMuted     = Color(0xFF757575)

private val DarkColors = darkColorScheme(
    primary          = ViciousRed,
    onPrimary        = Color.White,
    primaryContainer = ViciousRedDim,
    secondary        = ViciousOrange,
    onSecondary      = Color.Black,
    tertiary         = ViciousGreen,
    background       = ViciousSurface,
    surface          = ViciousCard,
    onBackground     = ViciousOnSurface,
    onSurface        = ViciousOnSurface,
    outline          = ViciousBorder,
    error            = ViciousRed
)

@Composable
fun ViciousScanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
