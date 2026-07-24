package com.shiro.yosugahub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.ui.theme.OnTermGreen
import com.shiro.yosugahub.ui.theme.TermGreen
import com.shiro.yosugahub.ui.theme.TermLine
import com.shiro.yosugahub.ui.theme.TermTextDim

/**
 * サブ画面の上辺バー(v5 UI)。下部ナビを廃したので、戻り口はここに置く。
 * `[ < BACK ]  TITLE` の1行 + 下に細い区切り線。武骨に。
 */
@Composable
fun SubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
            )
        }
        HorizontalDivider(thickness = 1.dp, color = TermLine)
        content()
    }
}
