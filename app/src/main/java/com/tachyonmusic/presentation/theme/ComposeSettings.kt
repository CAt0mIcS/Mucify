package com.tachyonmusic.presentation.theme

import androidx.compose.runtime.compositionLocalOf
import com.tachyonmusic.core.ColorScheme
import com.tachyonmusic.util.Duration
import com.tachyonmusic.util.ms

data class ComposeSettings(
    val animateText: Boolean = true,
    val dynamicColors: Boolean = true,
    val audioUpdateInterval: Duration = 100.ms,
    val colorScheme: ColorScheme = ColorScheme.System
)

val LocalSettings = compositionLocalOf { ComposeSettings() }