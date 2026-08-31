package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Inter - sans-serif minimal, fallback to system default
private val InterFamily = FontFamily.SansSerif
private val MonoFamily = FontFamily.Monospace

val Typography = Typography(
    // Display 56px 700 - hero
    displayLarge = TextStyle(
        fontFamily = InterFamily,
        fontSize = 56.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 58.sp, // 1.05 * 56
        letterSpacing = (-1.68).sp // -0.03em
    ),
    displayMedium = TextStyle(
        fontFamily = InterFamily,
        fontSize = 45.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 47.sp,
        letterSpacing = (-0.02).sp
    ),
    // Heading 32px 600
    headlineLarge = TextStyle(
        fontFamily = InterFamily,
        fontSize = 32.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 37.sp, // 1.15
        letterSpacing = (-0.64).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
        letterSpacing = (-0.02).sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp
    ),
    // Body 15px 400 line 1.65
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 25.sp,
        letterSpacing = (-0.15).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
        letterSpacing = (-0.12).sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        color = TextMuted
    ),
    labelLarge = TextStyle(
        fontFamily = InterFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    ),
    // Mono 13px
    displaySmall = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp
    )
)

// Convenience aliases per YAML spec
val DisplayTypography = Typography.displayLarge
val HeadingTypography = Typography.headlineLarge
val BodyTypography = Typography.bodyLarge
val MonoTypography = Typography.displaySmall
