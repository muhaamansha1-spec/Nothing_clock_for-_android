package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.FontFamily

val LocalCustomFont = compositionLocalOf<FontFamily> { DotMatrixFontFamily }

private val NothingDarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = AmoledBlack,
    secondary = MutedGrey,
    onSecondary = PureWhite,
    tertiary = NothingRed,
    onTertiary = PureWhite,
    background = AmoledBlack,
    onBackground = PureWhite,
    surface = AmoledBlack,
    onSurface = PureWhite,
    surfaceVariant = SurfaceGrey,
    onSurfaceVariant = MutedGrey,
    outline = BorderGrey
)

@Composable
fun NothingClockTheme(
    customFontFamily: FontFamily? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val systemAccent = if (dynamicColor) {
        try {
            dynamicDarkColorScheme(context).primary
        } catch (e: Exception) {
            NothingRed
        }
    } else {
        NothingRed
    }

    val customColorScheme = NothingDarkColorScheme.copy(
        tertiary = systemAccent
    )

    val typography = if (customFontFamily != null) {
        androidx.compose.material3.Typography(
            displayLarge = Typography.displayLarge.copy(fontFamily = customFontFamily),
            displayMedium = Typography.displayMedium.copy(fontFamily = customFontFamily),
            headlineMedium = Typography.headlineMedium.copy(fontFamily = customFontFamily),
            titleLarge = Typography.titleLarge.copy(fontFamily = customFontFamily),
            titleMedium = Typography.titleMedium.copy(fontFamily = customFontFamily),
            bodyLarge = Typography.bodyLarge.copy(fontFamily = customFontFamily),
            bodyMedium = Typography.bodyMedium.copy(fontFamily = customFontFamily),
            labelMedium = Typography.labelMedium.copy(fontFamily = customFontFamily),
            labelSmall = Typography.labelSmall.copy(fontFamily = customFontFamily)
        )
    } else {
        Typography
    }

    CompositionLocalProvider(LocalCustomFont provides (customFontFamily ?: DotMatrixFontFamily)) {
        MaterialTheme(
            colorScheme = customColorScheme,
            typography = typography,
            content = content
        )
    }
}
