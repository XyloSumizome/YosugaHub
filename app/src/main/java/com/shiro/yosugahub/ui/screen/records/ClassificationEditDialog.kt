package com.shiro.yosugahub.ui.screen.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.shiro.yosugahub.domain.model.DocumentClassification
import com.shiro.yosugahub.domain.model.RelatedRef

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分類を修正して承認") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "原文は変更されません。修正内容は履歴に残ります。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("要約") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = documentType,
                    onValueChange = { documentType = it },
                    label = { Text("文書種別") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = projectIds,
                    onValueChange = { projectIds = it },
                    label = { Text("プロジェクト(カンマ区切り)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = categories,
                    onValueChange = { categories = it },
                    label = { Text("カテゴリ(カンマ区切り)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("タグ(カンマ区切り)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
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
            ) {
                Text("承認")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}
