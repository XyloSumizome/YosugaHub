package com.shiro.yosugahub.ui.screen.records

import com.shiro.yosugahub.domain.model.Document
import com.shiro.yosugahub.domain.model.DocumentStatus

/** 記録タブ「文書」セクションの絞り込みロジック(純粋関数、ユニットテスト可能)。 */

/** 状態で絞り込む。null は「すべて」。 */
fun filterDocumentsByStatus(documents: List<Document>, status: DocumentStatus?): List<Document> =
    if (status == null) documents else documents.filter { it.status == status }

/**
 * キーワード検索。空なら全件。
 * タイトル・原文に加え、現行分類の要約・タグ・カテゴリも対象にする(分類後に探しやすくする)。
 */
fun searchDocuments(documents: List<Document>, query: String): List<Document> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return documents
    return documents.filter { document ->
        val classification = document.currentClassification
        document.title.contains(trimmed, ignoreCase = true) ||
            document.body.contains(trimmed, ignoreCase = true) ||
            classification?.summary?.contains(trimmed, ignoreCase = true) == true ||
            classification?.tags?.any { it.contains(trimmed, ignoreCase = true) } == true ||
            classification?.categories?.any { it.contains(trimmed, ignoreCase = true) } == true
    }
}

/** 一覧カードに出す1行要約。分類前は原文の冒頭で代用する。 */
fun documentPreviewOf(document: Document, maxLength: Int = 80): String {
    val source = document.currentClassification?.summary?.takeIf { it.isNotBlank() }
        ?: document.body
    val singleLine = source.replace('\n', ' ').trim()
    return if (singleLine.length <= maxLength) singleLine else singleLine.take(maxLength) + "…"
}
