package com.shiro.yosugahub.ui.screen.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.ui.component.StatusTag
import com.shiro.yosugahub.domain.model.Document
import com.shiro.yosugahub.domain.model.DocumentClassification
import com.shiro.yosugahub.domain.model.DocumentStatus
import com.shiro.yosugahub.ui.component.classificationOriginLabel
import com.shiro.yosugahub.ui.component.documentStatusLabel

/**
 * 文書の詳細 + 分類レビュー(v4.1)。原文・現行分類・分類履歴を確認して操作する。
 * 操作は 承認 / 修正 / 保留 / 再分類 / 元文表示 の5つ(設計書v4.1「UI」)。
 * 「保留」は状態を動かさず閉じるだけ = needs_review のまま据え置き。
 */
@Composable
fun DocumentDetailDialog(
    document: Document,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onApprove: () -> Unit,
    onEdit: () -> Unit,
    onReclassify: () -> Unit,
    onArchive: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(document.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatusTag(documentStatusLabel(document.status))
                Text(
                    text = "保存 ${document.createdAt.take(10)} / 更新 ${document.updatedAt.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val current = document.currentClassification
                if (current == null) {
                    Text(
                        text = when (document.status) {
                            DocumentStatus.UNCLASSIFIED ->
                                "まだ分類されていません。次回の同期でヨスガへ渡されます。"
                            DocumentStatus.CLASSIFICATION_PENDING ->
                                "ヨスガの分類待ちです。分類結果を取り込むと確認できます。"
                            else -> "分類結果がありません。"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    ClassificationSummary(current)
                }

                ReviewActions(
                    document = document,
                    onApprove = onApprove,
                    onEdit = onEdit,
                    onReclassify = onReclassify,
                    onArchive = onArchive,
                )

                HorizontalDivider()
                Text(text = "原文", style = MaterialTheme.typography.labelMedium)
                Text(text = document.body, style = MaterialTheme.typography.bodyMedium)

                // 現行以外の分類は履歴として畳んで並べる(ヨスガの元の判断を追えるように)。
                val past = document.classificationHistory.filterNot { it.isCurrent }
                if (past.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "分類履歴(${past.size}件)",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    past.forEach { record ->
                        Text(
                            text = historyLineOf(record),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                TextButton(onClick = onDelete) {
                    Text("この文書を削除", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                // needs_review の間は閉じる = 保留(状態を動かさない)。
                Text(if (document.status == DocumentStatus.NEEDS_REVIEW) "保留して閉じる" else "閉じる")
            }
        },
    )
}

/**
 * 状態に応じたレビュー操作。
 * 承認・修正は現行分類があるときだけ(空の分類を確定させない)。
 * 再分類は「一度分類された文書をやり直す」操作なので、
 * まだ分類されていない文書(未整理・分類待ち)には出さない
 * — それらは次の同期で自動的にヨスガへ渡るため、押す意味がない。
 */
@Composable
private fun ReviewActions(
    document: Document,
    onApprove: () -> Unit,
    onEdit: () -> Unit,
    onReclassify: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasClassification = document.currentClassification != null
    Column(modifier = modifier.fillMaxWidth()) {
        if (hasClassification && document.status == DocumentStatus.NEEDS_REVIEW) {
            TextButton(onClick = onApprove) { Text("この分類で承認") }
            TextButton(onClick = onEdit) { Text("修正して承認") }
        }
        if (hasClassification && document.status == DocumentStatus.CLASSIFIED) {
            TextButton(onClick = onEdit) { Text("分類を修正") }
        }
        if (hasClassification && document.status != DocumentStatus.ARCHIVED) {
            TextButton(onClick = onReclassify) { Text("再分類をヨスガに依頼") }
        }
        if (document.status != DocumentStatus.ARCHIVED) {
            TextButton(onClick = onArchive) { Text("アーカイブ") }
        }
    }
}

/** 履歴1件の1行表示。 */
fun historyLineOf(record: DocumentClassification): String {
    val confidence = record.confidence?.let { " (${formatConfidence(it)})" }.orEmpty()
    val summary = record.summary.ifBlank { "(要約なし)" }
    return "${record.classifiedAt.take(10)} ${classificationOriginLabel(record.origin)}$confidence: $summary"
}

/** 現行分類の内容表示。値が無い項目は行ごと省く。 */
@Composable
private fun ClassificationSummary(
    classification: DocumentClassification,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = classificationOriginLabel(classification.origin),
            style = MaterialTheme.typography.labelMedium,
        )
        if (classification.summary.isNotBlank()) {
            Text(text = classification.summary, style = MaterialTheme.typography.bodyMedium)
        }
        val details = buildList {
            if (classification.documentType.isNotBlank()) add("種別: ${classification.documentType}")
            if (classification.projectIds.isNotEmpty()) {
                add("プロジェクト: ${classification.projectIds.joinToString(", ")}")
            }
            if (classification.categories.isNotEmpty()) {
                add("カテゴリ: ${classification.categories.joinToString(", ")}")
            }
            if (classification.tags.isNotEmpty()) {
                add(classification.tags.joinToString(" ") { "#$it" })
            }
            if (classification.relatedEntities.isNotEmpty()) {
                add(
                    classification.relatedEntities
                        .joinToString(" / ", prefix = "関連: ") { "${it.type}:${it.id}" }
                )
            }
            classification.confidence?.let { add("信頼度: ${formatConfidence(it)}") }
            add("分類日時: ${classification.classifiedAt.take(10)}")
        }
        Text(
            text = details.joinToString("\n"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 信頼度 0.91 → "91%"(表示用)。 */
fun formatConfidence(confidence: Double): String =
    "${Math.round(confidence.coerceIn(0.0, 1.0) * 100)}%"
