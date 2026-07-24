package com.shiro.yosugahub.ui.screen.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

/**
 * 未整理文書の追加ダイアログ(v4.1)。
 * 分類・タグ付けはヨスガの仕事なので、ここでは原文とタイトルだけを入力する。
 * 保存後に原文を編集する手段は用意しない(原文は不変)。
 */
@Composable
fun DocumentAddDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, body: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    TerminalDialog(
        title = "文書を追加",
        onDismissRequest = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "分類・タグ付け・要約はヨスガが行います。ここでは原文をそのまま保存します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("タイトル") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("本文(原文)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                )
            }
        },
        confirmButton = {
            DialogAction(
                "保存",
                onClick = { onSave(title.trim(), body.trim()) },
                enabled = title.isNotBlank() && body.isNotBlank(),
            )
        },
        dismissButton = {
            DialogAction("キャンセル", onClick = onDismiss)
        },
    )
}
