package com.shiro.yosugahub.ui.screen.assistant

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shiro.yosugahub.ui.theme.TermAmber
import com.shiro.yosugahub.ui.theme.TermConsole
import com.shiro.yosugahub.ui.theme.TermCyan
import com.shiro.yosugahub.ui.theme.TermGreen
import com.shiro.yosugahub.ui.theme.TermLine
import com.shiro.yosugahub.ui.theme.TermRed
import com.shiro.yosugahub.ui.theme.TermText

/**
 * 取り込み中に本物のログを1行ずつ流す端末パネル(v5 UI: ハッキング演出)。
 * 色は種別だけで決まり、最新行へ自動スクロールし、末尾にカーソルを点滅させる。
 */
@Composable
fun ImportTerminal(
    lines: List<LogLine>,
    running: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // 行が増えるたび最下部へ追従する(最新の処理が常に見える)。
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TermLine, androidx.compose.ui.graphics.RectangleShape)
            .background(TermConsole)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "IMPORT SEQUENCE",
                color = TermCyan,
                style = MaterialTheme.typography.labelLarge,
            )
            BlinkingCursor(active = running, color = TermCyan)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 260.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(lines) { line ->
                Text(
                    text = line.text,
                    color = line.tone.color(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BlinkingCursor(active: Boolean, color: Color) {
    if (!active) {
        Text("▮", color = color.copy(alpha = 0.3f), fontFamily = FontFamily.Monospace)
        return
    }
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "cursor-alpha",
    )
    Text("▮", color = color, fontFamily = FontFamily.Monospace, modifier = Modifier.alpha(alpha))
}

private fun LogTone.color(): Color = when (this) {
    LogTone.INFO -> TermText
    LogTone.OK -> TermGreen
    LogTone.WARN -> TermAmber
    LogTone.ERROR -> TermRed
    LogTone.ACCENT -> TermAmber
}
