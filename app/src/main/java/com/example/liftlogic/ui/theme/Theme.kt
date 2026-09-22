package com.example.liftlogic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LiftLogicDarkScheme = darkColorScheme(
    primary = LiftGreen,
    onPrimary = Charcoal,
    secondary = AmberBadge,
    background = Charcoal,
    surface = CharcoalCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = CharcoalBorder
)

@Composable
fun LiftLogicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LiftLogicDarkScheme,
        typography = LiftLogicTypography,
        content = content
    )
}