package com.shiro.yosugahub.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * テキストを貼り付けて取り込む共用ダイアログ。
 * 既定は回答JSON(v4.3 運用: レコルの回答をコピーして貼る。コードブロックの囲いは
 * 取り込み側で外れる)。文言を差し替えれば会話ログの保存にも使える(v5 Phase 3-d)。
 */
@Composable
fun PasteImportDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    title: String = "回答JSONを貼り付け",
    description: String = "レコルの回答をコピーして、ここに貼り付けてください。" +
        "コードブロックの囲い(```)ごとでも構いません。",
    label: String = "回答JSON",
    confirmLabel: String = "取り込む",
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 280.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onImport(text) },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}
