package com.shiro.yosugahub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/**
 * 計器パネル(v5 UI: Brutalist)。丸みも影も無い。
 * 細い枠 + 見出し帯(LED + ラベル + 区切り線)で「配線されたパネル」感を出す。
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    ledColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TermLine, RectangleShape)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // 見出し帯: [LED] TITLE ───────────
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
                text = title.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = ledColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = TermLine,
            )
        }
        Column(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

/** 状態表示LED。小さな四角(丸くしない)。 */
@Composable
fun StatusLed(
    color: Color,
    modifier: Modifier = Modifier,
    size: Int = 8,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(color),
    )
}

/** ASCII 風の細い区切り線。密度の高いパネル内の仕切りに。 */
@Composable
fun AsciiDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = TermLine,
    )
}
