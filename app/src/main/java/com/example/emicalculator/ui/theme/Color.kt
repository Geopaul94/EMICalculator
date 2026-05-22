package com.example.emicalculator.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
//  Color Palette
//
//  All colors for the app defined in ONE place.
//  WHY? If you ever want to change the accent color, you change it here —
//  not in 20 different files. This is the "single source of truth" principle.
//
//  Naming convention: describe the role, not the hex value.
//  ✓ AccentBlue      (good — describes purpose)
//  ✗ Color4A9EFF     (bad  — meaningless if you change the shade)
// ─────────────────────────────────────────────────────────────────────────────

// Backgrounds
val BackgroundDark  = Color(0xFF1A1A1C)   // Main screen background
val SurfaceDark     = Color(0xFF2A2A2E)   // Input field background
val SurfaceResult   = Color(0xFF232326)   // Result field background (slightly darker)
val DividerColor    = Color(0xFF3A3A3C)   // Separator lines and borders

// Text
val TextPrimary     = Color(0xFFE5E5E7)   // Main text (near-white)
val TextSecondary   = Color(0xFF8E8E93)   // Placeholder / label text (gray)

// Accents
val AccentBlue      = Color(0xFF4A9EFF)   // Radio buttons, toggles, highlights
val AccentAmber     = Color(0xFFFF9F0A)   // Total interest (warm warning color)
val AccentGreen     = Color(0xFF30D158)   // Total amount paid (positive/success)
