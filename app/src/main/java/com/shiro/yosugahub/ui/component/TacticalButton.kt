package com.shiro.yosugahub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.ui.theme.TermLine

/**
 * コマンド実行行(v5 UI: ターミナルとGUIの中間)。
 *
 * 塗りの巨大 CTA をやめ、**`▶ コマンド名` の実行行**にする。
 * 「ボタンを押す」ではなく「コマンドを走らせる」印象にするのが狙い。
 * 既存の Button と引数互換(onClick / modifier / enabled / content)。
 * content 内の Text は色・書体を指定しなければ行のアクセント色・等幅を継ぐ。
 */
@Composable
fun TacticalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
    content: @Composable RowScope.() -> Unit,
) {
    CommandRow(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        token = "▶",
        accent = MaterialTheme.colorScheme.primary,
        // 主コマンドは左に細いアクセント帯 + うっすら地色。
        accentBar = true,
        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        contentPadding = contentPadding,
        content = content,
    )
}

/** 副次コマンド行。アクセント帯・地色なしの、細枠だけの実行行。 */
@Composable
fun TacticalOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
    content: @Composable RowScope.() -> Unit,
) {
    CommandRow(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        token = ">",
        accent = MaterialTheme.colorScheme.onSurfaceVariant,
        accentBar = false,
        background = Color.Transparent,
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
private fun CommandRow(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    token: String,
    accent: Color,
    accentBar: Boolean,
    background: Color,
    contentPadding: PaddingValues,
    content: @Composable RowScope.() -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    val borderColor = if (accentBar) accent.copy(alpha = 0.5f * alpha) else TermLine

    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .background(background)
            .border(1.dp, borderColor, RectangleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 主コマンドの左端に細いアクセント帯(配線が刺さっている感じ)。
        if (accentBar) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(accent.copy(alpha = alpha)),
            )
        }
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = token, color = accent.copy(alpha = alpha))
            CompositionLocalProvider(
                LocalContentColor provides labelColor,
                LocalTextStyle provides MaterialTheme.typography.labelLarge,
            ) {
                content()
            }
        }
    }
}
