package com.shiro.yosugahub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.ui.theme.TermLine
import com.shiro.yosugahub.ui.theme.TermLineActive

/**
 * 端末風のダイアログ(v5 UI)。中身は [AlertDialog] のまま
 * (高さ・スクロール・インセットの面倒を Material に任せる)、
 * **見た目だけ**パネルへ寄せる: 細枠 + `[LED] TITLE ────` の見出し帯 + 角丸ゼロ。
 *
 * 操作は [DialogAction] を使う。Material の TextButton は丸い波紋が出て浮くため使わない。
 */
@Composable
fun TerminalDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    ledColor: Color = MaterialTheme.colorScheme.primary,
    confirmButton: @Composable () -> Unit = {},
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        // 枠は選択済みの線色。パネルより一段はっきりさせて「前に出ている」ことを示す。
        modifier = modifier.border(1.dp, TermLineActive, RectangleShape),
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = { DialogHeader(title = title, ledColor = ledColor) },
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}

/** ダイアログの見出し帯。SectionCard と同じ作りにして、画面とダイアログの語彙を揃える。 */
@Composable
private fun DialogHeader(title: String, ledColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusLed(color = ledColor)
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = ledColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = TermLine)
    }
}

/**
 * ダイアログの操作。`[ ラベル ]` の角ばった押し所(StatusTag と同じ語彙)。
 * 破壊的操作は [danger] を立てて赤にする。
 */
@Composable
fun DialogAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val base = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val alpha = if (enabled) 1f else 0.4f
    Text(
        text = "[ $label ]",
        style = MaterialTheme.typography.labelLarge,
        color = base.copy(alpha = alpha),
        maxLines = 1,
        modifier = modifier
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .border(1.dp, if (enabled) TermLine else Color.Transparent, RectangleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
