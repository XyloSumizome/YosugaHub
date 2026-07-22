package com.shiro.yosugahub.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.ui.component.SectionCard

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = "設定", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            SectionCard(title = "Googleアカウント") {
                Text("未接続(Phase 4 で実装予定)", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            SectionCard(title = "カレンダー") {
                Text("取得期間: 過去7日〜未来7日", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            SectionCard(title = "GitHub") {
                Text("未設定(Phase 3 で実装予定)", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            SectionCard(title = "JSON保存先") {
                Text("未設定(Phase 2 で実装予定)", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            SectionCard(title = "診断情報") {
                Text("アプリバージョン: 0.1.0", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
