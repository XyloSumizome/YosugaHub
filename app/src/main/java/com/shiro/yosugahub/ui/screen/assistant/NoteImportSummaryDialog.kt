package com.shiro.yosugahub.ui.screen.assistant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.data.repository.NoteImportSummary
import com.shiro.yosugahub.data.repository.ProjectImportOutcome

/**
 * ノート取り込みの結果(v5 Phase 3-c)。
 * **何がどこへ入ったか**と、**人が仕分けるべきもの(Inbox)**が分かることを優先する。
 */
@Composable
fun NoteImportSummaryDialog(
    summary: NoteImportSummary,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ノートの取り込み") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (summary.vaultNotConfigured) {
                    Text(
                        text = "Obsidian Vault が未設定です。設定 → Obsidian Vault で選んでください。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text = "取り込み ${summary.imported}件 / 取得済み ${summary.skipped}件",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (summary.toInbox > 0) {
                        Text(
                            // Inbox は「失敗」ではなく「人が仕分ける必要がある」もの。
                            text = "うち ${summary.toInbox}件は Inbox へ入れました" +
                                "(type が無い・不明など)。Obsidian で仕分けてください。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (summary.failed > 0) {
                        Text(
                            text = "${summary.failed}件は取り込めませんでした。次回もう一度試されます。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                summary.outcomes.forEach { outcome ->
                    ProjectOutcomeRow(outcome)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
    )
}

@Composable
private fun ProjectOutcomeRow(outcome: ProjectImportOutcome) {
    Column {
        Text(outcome.projectName, style = MaterialTheme.typography.titleSmall)
        Text(
            text = if (outcome.message.isNotEmpty()) {
                outcome.message
            } else {
                buildString {
                    append("取り込み ${outcome.imported}件")
                    if (outcome.toInbox > 0) append(" (Inbox ${outcome.toInbox}件)")
                    if (outcome.skipped > 0) append(" / 取得済み ${outcome.skipped}件")
                    if (outcome.failed.isNotEmpty()) append(" / 失敗 ${outcome.failed.size}件")
                }
            },
            style = MaterialTheme.typography.bodySmall,
        )
        outcome.failed.forEach { path ->
            Text(
                text = "・$path",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
