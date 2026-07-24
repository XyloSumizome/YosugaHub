package com.shiro.yosugahub.ui.screen.records

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.ui.component.DialogAction
import com.shiro.yosugahub.ui.component.TerminalChip
import com.shiro.yosugahub.ui.component.TerminalDialog
import com.shiro.yosugahub.ui.component.TerminalField
import com.shiro.yosugahub.ui.component.itemKindLabel

/**
 * 情報アイテムの手動追加・編集ダイアログ(v3-Step 2-d 磨き込み)。
 * original が null なら新規。編集時のみ削除ボタンを表示する。
 * タグは通常 AI が管理するが、手動でもカンマ区切りで付けられる。
 */
@Composable
fun ItemEditDialog(
    original: KnowledgeItem?,
    onDismiss: () -> Unit,
    onSave: (kind: ItemKind, title: String, body: String, tags: List<String>) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var kind by remember(original) { mutableStateOf(original?.kind ?: ItemKind.MEMO) }
    var title by remember(original) { mutableStateOf(original?.title ?: "") }
    var body by remember(original) { mutableStateOf(original?.body ?: "") }
    var tagsInput by remember(original) { mutableStateOf(original?.tags?.joinToString(", ") ?: "") }

    TerminalDialog(
        title = if (original == null) "アイテムを追加" else "アイテムを編集",
        onDismissRequest = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "種類", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ItemKind.entries.forEach { candidate ->
                        TerminalChip(
                            selected = kind == candidate,
                            onClick = { kind = candidate },
                            label = itemKindLabel(candidate),
                        )
                    }
                }
                TerminalField(
                    value = title,
                    onValueChange = { title = it },
                    label = "タイトル",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TerminalField(
                    value = body,
                    onValueChange = { body = it },
                    label = "本文(任意)",
                    modifier = Modifier.fillMaxWidth(),
                )
                TerminalField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = "タグ(任意・カンマ区切り)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (onDelete != null) {
                    DialogAction("このアイテムを削除", onClick = onDelete, danger = true)
                }
            }
        },
        confirmButton = {
            DialogAction(
                "保存",
                onClick = {
                    onSave(kind, title.trim(), body.trim(), parseTagsInput(tagsInput))
                },
                enabled = title.isNotBlank(),
            )
        },
        dismissButton = {
            DialogAction("キャンセル", onClick = onDismiss)
        },
    )
}
