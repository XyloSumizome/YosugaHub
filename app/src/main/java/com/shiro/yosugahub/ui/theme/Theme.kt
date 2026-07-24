package com.shiro.yosugahub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * ターミナル/ブルータリストテーマ(v5 UI)。
 *
 * **ダーク固定。**システム設定・ダイナミックカラーに追従しない。
 * アクセントは Amber(主) / Cyan(情報) / Green(正常) / Red(警報) の4色のみ。
 */
private val TerminalColorScheme = darkColorScheme(
    primary = TermGreen,
    onPrimary = OnTermGreen,
    primaryContainer = TermPanelHeader,
    onPrimaryContainer = TermGreen,

    secondary = TermCyan,
    onSecondary = OnTermGreen,
    secondaryContainer = TermPanelHeader,
    onSecondaryContainer = TermCyan,

    tertiary = TermAmber,
    onTertiary = OnTermAmber,

    background = TermBackground,
    onBackground = TermText,

    surface = TermPanel,
    onSurface = TermText,
    surfaceVariant = TermPanelHeader,
    onSurfaceVariant = TermTextDim,
    surfaceContainerHighest = TermPanelHeader,
    surfaceContainerHigh = TermPanelHeader,
    surfaceContainer = TermPanel,
    surfaceContainerLow = TermPanel,
    surfaceContainerLowest = TermConsole,

    error = TermRed,
    onError = OnTermRed,

    outline = TermLine,
    outlineVariant = TermLine,
)

@Composable
fun YosugaHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        typography = Typography,
        shapes = TerminalShapes,
        content = content,
    )
}
