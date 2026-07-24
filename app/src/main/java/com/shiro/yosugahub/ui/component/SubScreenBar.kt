package com.shiro.yosugahub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.ui.theme.OnTermGreen
import com.shiro.yosugahub.ui.theme.TermGreen
import com.shiro.yosugahub.ui.theme.TermLine

/**
 * サブ画面の上辺バー(v5 UI)。下部ナビを廃したので、戻り口はここに置く。
 * `[ < BACK ]  TITLE  …操作` の1行 + 下に細い区切り線。武骨に。
 *
 * **全サブ画面がこれを使う。** 画面ごとに戻り方が違うと、戻り口を探す羽目になる。
 */
@Composable
fun SubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** 右端に置く画面固有の操作(履歴・再読込など)。無ければ空。 */
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 反転ブロックの戻りボタン(参考画像の <BACK)。
            Text(
                text = "<BACK",
                style = MaterialTheme.typography.labelLarge,
                color = OnTermGreen,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .background(TermGreen)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = TermGreen,
                maxLines = 1,
                // 長いタイトル(プロジェクト名など)で操作を押し出さない。
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
        HorizontalDivider(thickness = 1.dp, color = TermLine)
        content()
    }
}

/** 上辺バーの右端に並べる操作。`[ ラベル ]` の角ばった押し所にする。 */
@Composable
fun SubScreenAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.4f
    Text(
        text = "[ ${label.uppercase()} ]",
        style = MaterialTheme.typography.labelMedium,
        color = TermGreen.copy(alpha = alpha),
        maxLines = 1,
        modifier = modifier
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
    )
}
