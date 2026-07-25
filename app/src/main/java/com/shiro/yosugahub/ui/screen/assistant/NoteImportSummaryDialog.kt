package com.shiro.yosugahub.ui.screen.assistant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.data.repository.NoteImportSummary
import com.shiro.yosugahub.data.repository.ProjectImportOutcome
import com.shiro.yosugahub.ui.component.DialogAction
import com.shiro.yosugahub.ui.component.TerminalDialog

/**
 * ノート取り込みの結果(v5 Phase 3-c)。
 * **何がどこへ入ったか**と、**人が仕分けるべきもの(Inbox)**が分かることを優先する。
 */
@Composable
fun NoteImportSummaryDialog(
    summary: NoteImportSummary,
    onDismiss: () -> Unit,
) {
    TerminalDialog(
        title = "ノートの取り込み",
        onDismissRequest = onDismiss,
        content = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (summary.vaultNotConfigured) {
                    Text(
                        text = "Obsidian Vault が未設定です。設定 → Obsidian Vault で選んでください。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text = "取り込み ${summary.imported}件 / 更新 ${summary.updated}件 / 取得済み ${summary.skipped}件",
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
        confirmButton = { DialogAction("閉じる", onClick = onDismiss) },
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
                    if (outcome.updated > 0) append(" / 更新 ${outcome.updated}件")
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
        // 元が消えたノート。Vault 側は消さないので、消すかどうかは人が Obsidian で決める。
        if (outcome.missing.isNotEmpty()) {
            Text(
                text = "リポジトリから消えたノート(Vault側は残しています):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            outcome.missing.forEach { path ->
                Text(
                    text = "・$path",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
