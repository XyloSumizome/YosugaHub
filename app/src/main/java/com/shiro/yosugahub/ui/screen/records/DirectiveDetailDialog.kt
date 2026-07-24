package com.shiro.yosugahub.ui.screen.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.domain.model.Directive
import com.shiro.yosugahub.domain.model.DirectiveStatus
import com.shiro.yosugahub.ui.component.DialogAction
import com.shiro.yosugahub.ui.component.StatusTag
import com.shiro.yosugahub.ui.component.TerminalDialog
import com.shiro.yosugahub.ui.component.directiveStatusLabel

/**
 * 指示書の詳細(v4.2)。本文は Claude Code へそのまま届く Markdown。
 * ここでは編集させない — 指示を書くのはヨスガの仕事で、Hub は承認と配信管理を担う。
 */
@Composable
fun DirectiveDetailDialog(
    directive: Directive,
    targetName: String,
    onDismiss: () -> Unit,
    onMarkDone: () -> Unit,
    onReopen: () -> Unit,
    onDelete: () -> Unit,
) {
    TerminalDialog(
        title = directive.title.ifBlank { "$targetName への指示" },
        onDismissRequest = onDismiss,
        content = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatusTag(directiveStatusLabel(directive.status))
                Text(
                    text = "宛先: $targetName / 優先度: ${directivePriorityLabel(directive.priority)}" +
                        "\n承認 ${directive.createdAt.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                Text(text = directive.body, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider()

                if (directive.status == DirectiveStatus.OPEN) {
                    Text(
                        text = "対応済みにすると、次の同期から配信されなくなります。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DialogAction("対応済みにする", onClick = onMarkDone)
                } else {
                    DialogAction("配信中に戻す", onClick = onReopen)
                }
                DialogAction("この指示書を削除", onClick = onDelete, danger = true)
            }
        },
        confirmButton = {
            DialogAction("閉じる", onClick = onDismiss)
        },
    )
}

/** 指示書の優先度ラベル。未知の値はそのまま出す(ヨスガが自由記述しても壊さない)。 */
fun directivePriorityLabel(priority: String): String = when (priority) {
    "high" -> "高"
    "medium" -> "中"
    "low" -> "低"
    else -> priority
}
