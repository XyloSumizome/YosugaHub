package com.shiro.yosugahub.ui.screen.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.domain.model.DocumentClassification
import com.shiro.yosugahub.domain.model.RelatedRef
import com.shiro.yosugahub.ui.component.DialogAction
import com.shiro.yosugahub.ui.component.TerminalDialog
import com.shiro.yosugahub.ui.component.TerminalField

/** 修正ダイアログの編集結果(承認時に Repository へ渡す)。 */
data class ClassificationEdits(
    val summary: String,
    val documentType: String,
    val projectIds: List<String>,
    val categories: List<String>,
    val tags: List<String>,
    val relatedEntities: List<RelatedRef>,
)

/**
 * 分類結果の修正ダイアログ(v4.1)。
 * ヨスガの分類を下敷きにユーザーが直し、承認するとユーザー修正レコードとして積まれる
 * (AIの結果は履歴に残り、原文には触れない)。
 */
@Composable
fun ClassificationEditDialog(
    original: DocumentClassification?,
    onDismiss: () -> Unit,
    onApprove: (ClassificationEdits) -> Unit,
) {
    var summary by remember(original) { mutableStateOf(original?.summary ?: "") }
    var documentType by remember(original) { mutableStateOf(original?.documentType ?: "") }
    var projectIds by remember(original) {
        mutableStateOf(original?.projectIds?.joinToString(", ") ?: "")
    }
    var categories by remember(original) {
        mutableStateOf(original?.categories?.joinToString(", ") ?: "")
    }
    var tags by remember(original) { mutableStateOf(original?.tags?.joinToString(", ") ?: "") }

    TerminalDialog(
        title = "分類を修正して承認",
        onDismissRequest = onDismiss,
        content = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "原文は変更されません。修正内容は履歴に残ります。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TerminalField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = "要約",
                    modifier = Modifier.fillMaxWidth(),
                )
                TerminalField(
                    value = documentType,
                    onValueChange = { documentType = it },
                    label = "文書種別",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TerminalField(
                    value = projectIds,
                    onValueChange = { projectIds = it },
                    label = "プロジェクト(カンマ区切り)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TerminalField(
                    value = categories,
                    onValueChange = { categories = it },
                    label = "カテゴリ(カンマ区切り)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TerminalField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = "タグ(カンマ区切り)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            DialogAction(
                // 要約が空の分類を確定させない(既存の ItemEditDialog と同じ方針)。
                "承認",
                onClick = {
                    onApprove(
                        ClassificationEdits(
                            summary = summary.trim(),
                            documentType = documentType.trim(),
                            projectIds = parseTagsInput(projectIds),
                            categories = parseTagsInput(categories),
                            tags = parseTagsInput(tags),
                            // 関連実体はヨスガが付けるもので手入力させない(元の値を維持)。
                            relatedEntities = original?.relatedEntities.orEmpty(),
                        )
                    )
                },
                enabled = summary.isNotBlank(),
            )
        },
        dismissButton = {
            DialogAction("キャンセル", onClick = onDismiss)
        },
    )
}
