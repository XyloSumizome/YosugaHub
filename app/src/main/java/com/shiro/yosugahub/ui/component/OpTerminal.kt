package com.shiro.yosugahub.ui.component

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shiro.yosugahub.ui.theme.TermAmber
import com.shiro.yosugahub.ui.theme.TermConsole
import com.shiro.yosugahub.ui.theme.TermGreen
import com.shiro.yosugahub.ui.theme.TermLine
import com.shiro.yosugahub.ui.theme.TermRed
import com.shiro.yosugahub.ui.theme.TermText

/** 端末ログ1行の種別。UI が色分けに使う。 */
enum class LogTone { INFO, OK, WARN, ERROR, ACCENT }

/** 端末風ログの1行。 */
data class LogLine(val text: String, val tone: LogTone = LogTone.INFO)

/**
 * 汎用の端末ログパネル(v5 UI: システムが動いている演出)。
 * 取り込み・保存・生成・同期など、あらゆる操作の進捗をこの端末に流す。
 * 種別で色分けし、最新行へ自動スクロールし、末尾にカーソルを点滅させる。
 */
@Composable
fun OpTerminal(
    title: String,
    lines: List<LogLine>,
    running: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    // 実行中は掃引ラインを上→下へ動かす(データ転送している感じ)。
    val sweep = rememberInfiniteTransition(label = "sweep")
    val sweepPos by sweep.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Restart),
        label = "sweep-pos",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TermLine, RectangleShape)
            .background(TermConsole)
            .scanlines(running = running, sweepFraction = sweepPos)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title, color = TermGreen, style = MaterialTheme.typography.labelLarge)
            BlinkingCursor(active = running, color = TermGreen)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp, max = 240.dp)
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
    LogTone.ACCENT -> TermGreen
}

/**
 * CRT 風の走査線を重ねる(v5 UI)。
 * 常時: 3px 間隔の薄い横線。実行中: 明るい掃引ラインが上→下へ動く。
 * どちらも低アルファで、文字の可読性は損なわない。
 */
private fun Modifier.scanlines(running: Boolean, sweepFraction: Float): Modifier =
    drawWithContent {
        drawContent()
        // 常時の横縞
        val gap = 3.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = TermGreen.copy(alpha = 0.035f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += gap
        }
        // 実行中の掃引ライン
        if (running) {
            val cy = size.height * sweepFraction
            val band = 18.dp.toPx()
            drawRect(
                color = TermGreen.copy(alpha = 0.06f),
                topLeft = Offset(0f, cy - band / 2f),
                size = Size(size.width, band),
            )
            drawLine(
                color = TermGreen.copy(alpha = 0.5f),
                start = Offset(0f, cy),
                end = Offset(size.width, cy),
                strokeWidth = 1.5f,
            )
        }
    }
