package com.loansolver.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  Typography
//
//  Defines the text styles used across the app.
//  Material3's Typography system has a set of named styles (displayLarge,
//  headlineMedium, bodySmall, etc.). You override only what you need.
//
//  Using the typography system (instead of hardcoding fontSize in every
//  Composable) means:
//    • One change here resizes text everywhere
//    • Your app automatically respects the user's system font size setting
// ─────────────────────────────────────────────────────────────────────────────

val AppTypography = Typography(

    // Used for the screen title "EMI Calculator"
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 26.sp,
        lineHeight = 32.sp
    ),

    // Used for input field values (the numbers the user types)
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 18.sp,
        lineHeight = 24.sp
    ),

    // Used for row labels ("Amount", "Interest %", etc.)
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 22.sp
    ),

    // Used for summary card labels and small text
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp
    ),

    // Used for summary card values
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp,
        lineHeight = 22.sp
    )
)
