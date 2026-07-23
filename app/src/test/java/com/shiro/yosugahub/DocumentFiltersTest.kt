package com.shiro.yosugahub

import com.shiro.yosugahub.domain.model.ClassificationOrigin
import com.shiro.yosugahub.domain.model.Document
import com.shiro.yosugahub.domain.model.DocumentClassification
import com.shiro.yosugahub.domain.model.DocumentStatus
import com.shiro.yosugahub.ui.screen.records.documentPreviewOf
import com.shiro.yosugahub.ui.screen.records.filterDocumentsByStatus
import com.shiro.yosugahub.ui.screen.records.formatConfidence
import com.shiro.yosugahub.ui.screen.records.historyLineOf
import com.shiro.yosugahub.ui.screen.records.searchDocuments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 記録タブ「文書」セクションの絞り込み・表示ロジック(純粋関数)の検証。 */
class DocumentFiltersTest {

    private fun classification(
        summary: String = "グラップル仕様に関する検討",
        tags: List<String> = listOf("grapple", "frog"),
        categories: List<String> = listOf("game-design"),
    ) = DocumentClassification(
        id = "c1",
        documentId = "d1",
        summary = summary,
        documentType = "design-discussion",
        confidence = 0.91,
        projectIds = listOf("fragile-hero"),
        categories = categories,
        tags = tags,
        relatedEntities = emptyList(),
        classifiedAt = "2026-07-23T15:00:00+09:00",
        origin = ClassificationOrigin.AI,
        isCurrent = true,
    )

    private fun document(
        id: String = "d1",
        title: String = "グラップル検討メモ",
        body: String = "カエルの舌でグラップルする案。",
        status: DocumentStatus = DocumentStatus.UNCLASSIFIED,
        currentClassification: DocumentClassification? = null,
    ) = Document(
        id = id,
        title = title,
        body = body,
        status = status,
        createdAt = "2026-07-23T15:00:00+09:00",
        updatedAt = "2026-07-23T15:00:00+09:00",
        source = "manual",
        currentClassification = currentClassification,
    )

    @Test
    fun filterDocumentsByStatus_null_returns_all() {
        val documents = listOf(
            document(id = "a", status = DocumentStatus.UNCLASSIFIED),
            document(id = "b", status = DocumentStatus.CLASSIFIED),
        )
        assertEquals(2, filterDocumentsByStatus(documents, null).size)
        assertEquals(
            listOf("b"),
            filterDocumentsByStatus(documents, DocumentStatus.CLASSIFIED).map { it.id },
        )
    }

    @Test
    fun searchDocuments_matches_title_and_body() {
        val documents = listOf(
            document(id = "a", title = "グラップル検討メモ", body = "無関係な本文"),
            document(id = "b", title = "別のメモ", body = "カエルの舌の話"),
            document(id = "c", title = "サウンド設計", body = "CRIWAREの調査"),
        )
        assertEquals(listOf("a"), searchDocuments(documents, "グラップル").map { it.id })
        assertEquals(listOf("b"), searchDocuments(documents, "カエル").map { it.id })
        assertEquals(3, searchDocuments(documents, "  ").size)
    }

    @Test
    fun searchDocuments_matches_classification_summary_tags_and_categories() {
        val documents = listOf(
            document(id = "a", title = "無題", body = "本文", currentClassification = classification()),
            document(id = "b", title = "別の文書", body = "別の本文"),
        )
        assertEquals(listOf("a"), searchDocuments(documents, "grapple").map { it.id })
        assertEquals(listOf("a"), searchDocuments(documents, "game-design").map { it.id })
        assertEquals(listOf("a"), searchDocuments(documents, "仕様に関する").map { it.id })
        assertTrue(searchDocuments(documents, "存在しない語").isEmpty())
    }

    @Test
    fun searchDocuments_ignores_case() {
        val documents = listOf(document(currentClassification = classification(tags = listOf("Grapple"))))
        assertEquals(1, searchDocuments(documents, "GRAPPLE").size)
    }

    @Test
    fun documentPreview_uses_summary_when_classified() {
        val classified = document(
            body = "とても長い原文…",
            currentClassification = classification(summary = "グラップル仕様の検討"),
        )
        assertEquals("グラップル仕様の検討", documentPreviewOf(classified))
    }

    @Test
    fun documentPreview_falls_back_to_body_and_flattens_newlines() {
        val unclassified = document(body = "1行目\n2行目")
        assertEquals("1行目 2行目", documentPreviewOf(unclassified))
    }

    @Test
    fun documentPreview_truncates_long_text() {
        val long = document(body = "あ".repeat(200))
        val preview = documentPreviewOf(long, maxLength = 10)
        assertEquals("あ".repeat(10) + "…", preview)
    }

    @Test
    fun documentPreview_uses_body_when_summary_is_blank() {
        val blankSummary = document(
            body = "原文はこちら",
            currentClassification = classification(summary = "   "),
        )
        assertEquals("原文はこちら", documentPreviewOf(blankSummary))
    }

    @Test
    fun formatConfidence_renders_percent_and_clamps() {
        assertEquals("91%", formatConfidence(0.91))
        assertEquals("100%", formatConfidence(1.5))
        assertEquals("0%", formatConfidence(-0.2))
    }

    @Test
    fun historyLine_shows_origin_confidence_and_summary() {
        val aiRecord = classification(summary = "グラップル仕様の検討")
        assertEquals(
            "2026-07-23 ヨスガの分類 (91%): グラップル仕様の検討",
            historyLineOf(aiRecord),
        )
    }

    @Test
    fun historyLine_omits_confidence_for_user_edits_and_handles_blank_summary() {
        val userRecord = classification(summary = "  ").copy(
            confidence = null,
            origin = ClassificationOrigin.USER,
        )
        assertEquals("2026-07-23 ユーザーの修正: (要約なし)", historyLineOf(userRecord))
    }
}
