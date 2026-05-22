package com.example.emicalculator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ─────────────────────────────────────────────────────────────────────────────
//  Theme
//
//  MaterialTheme is a Composable that wraps your entire app and provides:
//    • ColorScheme  — accessible via MaterialTheme.colorScheme
//    • Typography   — accessible via MaterialTheme.typography
//    • Shapes       — accessible via MaterialTheme.shapes
//
//  Any Composable inside EMICalculatorTheme can access these values.
//  This is how Material components (Button, Card, etc.) automatically
//  pick up your brand colors without being told explicitly.
// ─────────────────────────────────────────────────────────────────────────────

// Define the color scheme for dark mode using our custom colors
private val DarkColorScheme = darkColorScheme(
    primary         = AccentBlue,        // Buttons, selected states, FABs
    onPrimary       = BackgroundDark,    // Text/icon on top of primary color
    background      = BackgroundDark,    // Screen background
    onBackground    = TextPrimary,       // Text drawn on the background
    surface         = SurfaceDark,       // Cards, sheets, menus
    onSurface       = TextPrimary,       // Text drawn on surfaces
    surfaceVariant  = SurfaceResult,     // Alternative surface (result field)
    onSurfaceVariant = TextSecondary,    // Text on surfaceVariant
    outline         = DividerColor       // Borders, dividers
)

/**
 * The root theme Composable for the entire app.
 *
 * Usage: wrap your top-level screen in this to apply colors and typography.
 *
 *   EMICalculatorTheme {
 *       EMIScreen()
 *   }
 */
@Composable
fun EMICalculatorTheme(
    content: @Composable () -> Unit   // the UI that gets styled
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
