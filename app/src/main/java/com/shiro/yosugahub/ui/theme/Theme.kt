package com.shiro.yosugahub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * サイバーパンクテーマ(v5 UI刷新)。
 *
 * **ダーク固定。**システム設定にもダイナミックカラーにも追従しない
 * (端末の壁紙由来の色が混ざると世界観が壊れるため、意図的に無効化)。
 */
private val CyberColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = OnNeonCyan,
    primaryContainer = CyanContainer,
    onPrimaryContainer = NeonCyan,

    secondary = NeonMagenta,
    onSecondary = OnNeonMagenta,
    secondaryContainer = MagentaContainer,
    onSecondaryContainer = NeonMagenta,

    tertiary = NeonGreen,
    onTertiary = OnNeonGreen,

    background = CyberBackground,
    onBackground = CyberOnBackground,

    surface = CyberSurface,
    onSurface = CyberOnBackground,
    surfaceVariant = CyberSurfaceHigh,
    onSurfaceVariant = CyberOnSurfaceVariant,
    surfaceContainerHighest = CyberSurfaceHigh,
    surfaceContainerHigh = CyberSurfaceHigh,
    surfaceContainer = CyberSurface,
    surfaceContainerLow = CyberSurface,
    surfaceContainerLowest = CyberTerminal,

    error = NeonRed,
    onError = OnNeonRed,

    outline = CyberOutline,
    outlineVariant = CyberOutline,
)

@Composable
fun YosugaHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content,
    )
}
