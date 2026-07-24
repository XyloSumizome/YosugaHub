package com.shiro.yosugahub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.ui.theme.OnTermGreen
import com.shiro.yosugahub.ui.theme.TermGreen
import com.shiro.yosugahub.ui.theme.TermLine
import com.shiro.yosugahub.ui.theme.TermTextDim

/**
 * 選択チップ(v5 UI)。Material の FilterChip(角丸・チェックアイコン)の代わりに、
 * **選択で緑に反転する角ばったタグ**にする。StatusTag と同じ `[ ]` の語彙。
 *
 * 選択中を「色が濃い」だけで示さず**反転**させるのは、等幅・単色の画面では
 * 濃淡の差が読み取りにくいため。
 */
@Composable
fun TerminalChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.4f
    Text(
        text = if (selected) "[ $label ]" else "  $label  ",
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) OnTermGreen.copy(alpha = alpha) else TermTextDim.copy(alpha = alpha),
        maxLines = 1,
        modifier = modifier
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .background(if (selected) TermGreen.copy(alpha = alpha) else Color.Transparent)
            .border(1.dp, if (selected) TermGreen.copy(alpha = alpha) else TermLine, RectangleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/**
 * チェックボックス(v5 UI)。Material の丸みのある箱をやめ、`[x]` / `[ ]` の文字で示す。
 * ラベルも含めて押せる(小さな箱だけを狙わせない)。
 * ラベルを隣の要素が持っている場合は [label] を空にすると箱だけになる。
 */
@Composable
fun TerminalCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = modifier
            .then(if (enabled) Modifier.clickable { onCheckedChange(!checked) } else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (checked) "[x]" else "[ ]",
            style = MaterialTheme.typography.bodyMedium,
            color = TermGreen.copy(alpha = alpha),
        )
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
        }
    }
}
