package com.shiro.yosugahub.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.ui.component.DialogAction
import com.shiro.yosugahub.ui.component.TerminalDialog
import com.shiro.yosugahub.ui.component.TerminalField

/**
 * テキストを貼り付けて取り込む共用ダイアログ。
 * 既定は回答JSON(観察日誌でも、セッション記録でも、タスクの提案でも同じ口から入る。
 * コードブロックの囲いは取り込み側で外れる)。
 * 文言を差し替えれば会話ログの保存にも使える(v5 Phase 3-d)。
 */
@Composable
fun PasteImportDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    title: String = "回答JSONを貼り付け",
    description: String = "ヨスガの回答JSONをコピーして、ここに貼り付けてください。" +
        "コードブロックの囲い(```)ごとでも構いません。",
    label: String = "回答JSON",
    confirmLabel: String = "取り込む",
    /**
     * 開いたときに入れておく文字列(2026-07-25)。
     * クリップボードを読むかどうかは**呼び出し側の判断**にして、
     * このダイアログは端末の状態に触れない(テストしやすさを保つ)。
     */
    prefill: String = "",
) {
    var text by remember(prefill) { mutableStateOf(prefill) }

    TerminalDialog(
        title = title,
        onDismissRequest = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TerminalField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 280.dp),
                )
            }
        },
        confirmButton = {
            DialogAction(confirmLabel, onClick = { onImport(text) }, enabled = text.isNotBlank())
        },
        dismissButton = {
            DialogAction("キャンセル", onClick = onDismiss)
        },
    )
}
