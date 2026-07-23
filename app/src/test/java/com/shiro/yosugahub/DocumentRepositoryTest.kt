package com.shiro.yosugahub

import com.shiro.yosugahub.data.local.db.DocumentJsonColumns
import com.shiro.yosugahub.data.local.db.DocumentWithClassifications
import com.shiro.yosugahub.data.local.db.dao.DocumentDao
import com.shiro.yosugahub.data.local.db.entity.DocumentClassificationEntity
import com.shiro.yosugahub.data.local.db.entity.DocumentEntity
import com.shiro.yosugahub.data.local.db.toDomain
import com.shiro.yosugahub.data.repository.DocumentRepository
import com.shiro.yosugahub.domain.model.ClassificationOrigin
import com.shiro.yosugahub.domain.model.DocumentStatus
import com.shiro.yosugahub.domain.model.RelatedRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DocumentDao の抽象メソッドをインメモリ実装に差し替え、
 * default メソッド(saveClassificationAsCurrent / deleteDocumentWithClassifications)の実ロジックと
 * Repository の状態遷移・履歴の積み方を検証する(v4.1 AI分類ワークフロー)。
 */
class DocumentRepositoryTest {

    private class FakeDocumentDao : DocumentDao {
        val documents = mutableMapOf<String, DocumentEntity>()
        val classifications = mutableMapOf<String, DocumentClassificationEntity>()

        private fun withClassifications(doc: DocumentEntity) = DocumentWithClassifications(
            document = doc,
            classifications = classifications.values.filter { it.documentId == doc.id },
        )

        override fun observeDocuments(): Flow<List<DocumentWithClassifications>> =
            flowOf(
                documents.values.sortedByDescending { it.updatedAt }.map { withClassifications(it) }
            )

        override suspend fun getDocument(id: String): DocumentWithClassifications? =
            documents[id]?.let { withClassifications(it) }

        override suspend fun getDocumentsByStatus(status: String): List<DocumentEntity> =
            documents.values.filter { it.status == status }.sortedByDescending { it.updatedAt }

        override suspend fun countDocuments(): Int = documents.size

        override fun observeCountByStatus(status: String): Flow<Int> =
            flowOf(documents.values.count { it.status == status })

        override suspend fun upsertDocument(document: DocumentEntity) {
            documents[document.id] = document
        }

        override suspend fun updateStatus(id: String, status: String, updatedAt: String) {
            documents[id]?.let { documents[id] = it.copy(status = status, updatedAt = updatedAt) }
        }

        override suspend fun updateStatusForAll(from: String, to: String, updatedAt: String) {
            documents.values.filter { it.status == from }.forEach {
                documents[it.id] = it.copy(status = to, updatedAt = updatedAt)
            }
        }

        override suspend fun insertClassification(classification: DocumentClassificationEntity) {
            classifications[classification.id] = classification
        }

        override suspend fun clearCurrentClassification(documentId: String) {
            classifications.values.filter { it.documentId == documentId }.forEach {
                classifications[it.id] = it.copy(isCurrent = false)
            }
        }

        override suspend fun deleteClassificationsFor(documentId: String) {
            classifications.values.filter { it.documentId == documentId }.forEach {
                classifications.remove(it.id)
            }
        }

        override suspend fun deleteDocumentRow(id: String) {
            documents.remove(id)
        }
    }

    private var tick = 0
    private var idCounter = 0

    /** now は呼ぶたびに進む(遷移順の検証に使う)。 */
    private fun repository(dao: FakeDocumentDao) = DocumentRepository(
        dao,
        now = { "2026-07-23T15:00:%02d+09:00".format(tick++) },
        newId = { "gen-${idCounter++}" },
    )

    private suspend fun DocumentRepository.createSample() =
        createDocument(
            title = "グラップル検討メモ",
            body = "カエルの舌でグラップルするリズムアクションの検討。",
            source = "manual",
        )

    @Test
    fun createDocument_starts_unclassified_with_original_body() = runBlocking {
        val dao = FakeDocumentDao()
        val doc = repository(dao).createSample()
        assertEquals(DocumentStatus.UNCLASSIFIED, doc.status)
        assertNull(doc.currentClassification)
        assertEquals("カエルの舌でグラップルするリズムアクションの検討。", dao.documents[doc.id]?.body)
    }

    @Test
    fun applyAiClassification_moves_to_needs_review_and_keeps_body() = runBlocking {
        val dao = FakeDocumentDao()
        val repo = repository(dao)
        val doc = repo.createSample()
        val originalBody = doc.body

        val classified = repo.applyAiClassification(
            documentId = doc.id,
            summary = "グラップル仕様に関する検討",
            documentType = "design-discussion",
            confidence = 0.91,
            projectIds = listOf("fragile-hero"),
            categories = listOf("game-design", "player-action"),
            tags = listOf("grapple", "frog"),
            relatedEntities = listOf(RelatedRef(type = "feature", id = "grapple")),
        )

        assertEquals(DocumentStatus.NEEDS_REVIEW, classified?.status)
        assertEquals(originalBody, dao.documents[doc.id]?.body)  // 原文は不変
        val current = classified?.currentClassification
        assertEquals(ClassificationOrigin.AI, current?.origin)
        assertEquals(0.91, current?.confidence)
        assertEquals(listOf("fragile-hero"), current?.projectIds)
        assertEquals(RelatedRef("feature", "grapple"), current?.relatedEntities?.single())
    }

    @Test
    fun applyAiClassification_for_missing_document_returns_null() = runBlocking {
        val dao = FakeDocumentDao()
        val result = repository(dao).applyAiClassification(
            documentId = "missing",
            summary = "", documentType = "", confidence = null,
            projectIds = emptyList(), categories = emptyList(), tags = emptyList(),
            relatedEntities = emptyList(),
        )
        assertNull(result)
        assertTrue(dao.classifications.isEmpty())
    }

    @Test
    fun approve_confirms_current_classification() = runBlocking {
        val dao = FakeDocumentDao()
        val repo = repository(dao)
        val doc = repo.createSample()
        repo.applyAiClassification(
            doc.id, "要約", "design-discussion", 0.9,
            emptyList(), emptyList(), emptyList(), emptyList(),
        )
        assertTrue(repo.approve(doc.id))
        assertEquals(DocumentStatus.CLASSIFIED.dbValue, dao.documents[doc.id]?.status)
    }

    @Test
    fun approve_without_classification_is_rejected() = runBlocking {
        val dao = FakeDocumentDao()
        val repo = repository(dao)
        val doc = repo.createSample()
        assertFalse(repo.approve(doc.id))
        assertEquals(DocumentStatus.UNCLASSIFIED.dbValue, dao.documents[doc.id]?.status)
    }

    @Test
    fun approveWithEdits_stacks_user_record_and_keeps_ai_history() = runBlocking {
        val dao = FakeDocumentDao()
        val repo = repository(dao)
        val doc = repo.createSample()
        repo.applyAiClassification(
            doc.id, "AIの要約", "design-discussion", 0.9,
            listOf("fragile-hero"), emptyList(), listOf("grapple"), emptyList(),
        )
        val edited = repo.approveWithEdits(
            documentId = doc.id,
            summary = "ユーザーが直した要約",
            documentType = "design-discussion",
            projectIds = listOf("fragile-hero"),
            categories = listOf("game-design"),
            tags = listOf("grapple", "rhythm-action"),
            relatedEntities = emptyList(),
        )

        assertEquals(DocumentStatus.CLASSIFIED, edited?.status)
        // 現行はユーザー修正(confidence なし)
        val current = edited?.currentClassification
        assertEquals(ClassificationOrigin.USER, current?.origin)
        assertNull(current?.confidence)
        assertEquals("ユーザーが直した要約", current?.summary)
        // AI結果は履歴として残る(isCurrent = false)
        val history = repo.classificationHistory(doc.id)
        assertEquals(2, history.size)
        val aiRecord = history.single { it.origin == ClassificationOrigin.AI }
        assertFalse(aiRecord.isCurrent)
        assertEquals("AIの要約", aiRecord.summary)
    }

    @Test
    fun requestReclassification_returns_to_pending_and_keeps_history() = runBlocking {
        val dao = FakeDocumentDao()
        val repo = repository(dao)
        val doc = repo.createSample()
        repo.applyAiClassification(
            doc.id, "1回目", "memo", 0.5, emptyList(), emptyList(), emptyList(), emptyList(),
        )
        repo.requestReclassification(doc.id)
        assertEquals(DocumentStatus.CLASSIFICATION_PENDING.dbValue, dao.documents[doc.id]?.status)
        assertEquals(1, repo.classificationHistory(doc.id).size)

        // 2回目の分類で現行が置き換わり、履歴は2件になる
        repo.applyAiClassification(
            doc.id, "2回目", "design-discussion", 0.8,
            emptyList(), emptyList(), emptyList(), emptyList(),
        )
        val history = repo.classificationHistory(doc.id)
        assertEquals(2, history.size)
        assertEquals("2回目", history.single { it.isCurrent }.summary)
    }

    @Test
    fun markUnclassifiedAsPending_moves_only_unclassified() = runBlocking {
        val dao = FakeDocumentDao()
        val repo = repository(dao)
        val target = repo.createSample()
        val classified = repo.createSample()
        repo.applyAiClassification(
            classified.id, "済", "memo", 0.9, emptyList(), emptyList(), emptyList(), emptyList(),
        )
        repo.approve(classified.id)

        val moved = repo.markUnclassifiedAsPending()

        assertEquals(listOf(target.id), moved.map { it.id })
        assertEquals(DocumentStatus.CLASSIFICATION_PENDING.dbValue, dao.documents[target.id]?.status)
        assertEquals(DocumentStatus.CLASSIFIED.dbValue, dao.documents[classified.id]?.status)
    }

    /** 確定させた文書は取り込みで揺り戻さない(やり直しは明示的な再分類だけ)。 */
    @Test
    fun applyAiClassification_skips_classified_document() = runBlocking {
        val dao = FakeDocumentDao()
        val repo = repository(dao)
        val doc = repo.createSample()
        repo.applyAiClassification(
            doc.id, "承認した分類", "memo", 0.9, emptyList(), emptyList(), emptyList(), emptyList(),
        )
        repo.approve(doc.id)

        val result = repo.applyAiClassification(
            doc.id, "あとから届いた分類", "memo", 0.9,
            emptyList(), emptyList(), emptyList(), emptyList(),
        )

        assertNull(result)
        assertEquals(DocumentStatus.CLASSIFIED.dbValue, dao.documents[doc.id]?.status)
        assertEquals("承認した分類", repo.document(doc.id)?.currentClassification?.summary)
        assertEquals(1, repo.classificationHistory(doc.id).size)
    }

    /** 古い回答JSONを取り込んでも、片付けた文書を勝手に復活させない。 */
    @Test
    fun applyAiClassification_skips_archived_document() = runBlocking {
        val dao = FakeDocumentDao()
        val repo = repository(dao)
        val doc = repo.createSample()
        repo.applyAiClassification(
            doc.id, "最初の分類", "memo", 0.9, emptyList(), emptyList(), emptyList(), emptyList(),
        )
        repo.archive(doc.id)

        val result = repo.applyAiClassification(
            doc.id, "古い回答からの分類", "memo", 0.9,
            emptyList(), emptyList(), emptyList(), emptyList(),
        )

        assertNull(result)
        assertEquals(DocumentStatus.ARCHIVED.dbValue, dao.documents[doc.id]?.status)
        // 分類履歴も増えない。
        assertEquals(1, repo.classificationHistory(doc.id).size)
    }

    @Test
    fun archive_and_delete() = runBlocking {
        val dao = FakeDocumentDao()
        val repo = repository(dao)
        val doc = repo.createSample()
        repo.applyAiClassification(
            doc.id, "要約", "memo", 0.9, emptyList(), emptyList(), emptyList(), emptyList(),
        )
        repo.archive(doc.id)
        assertEquals(DocumentStatus.ARCHIVED.dbValue, dao.documents[doc.id]?.status)

        repo.deleteDocument(doc.id)
        assertTrue(dao.documents.isEmpty())
        assertTrue(dao.classifications.isEmpty())  // 履歴も一緒に消える
    }

    @Test
    fun broken_json_columns_fall_back_to_empty_lists() {
        val entity = DocumentClassificationEntity(
            id = "c1", documentId = "d1",
            summary = "壊れたレコード", documentType = "memo", confidence = null,
            projectIdsJson = "{not json",
            categoriesJson = "42",
            tagsJson = "",
            relatedEntitiesJson = "[{\"type\":\"feature\"}]",  // id 欠落
            classifiedAt = "2026-07-23T15:00:00+09:00",
            appliedBy = "ai", isCurrent = true,
        )
        val domain = entity.toDomain()
        assertTrue(domain.projectIds.isEmpty())
        assertTrue(domain.categories.isEmpty())
        assertTrue(domain.tags.isEmpty())
        assertTrue(domain.relatedEntities.isEmpty())
    }

    @Test
    fun json_columns_roundtrip() {
        val refs = listOf(RelatedRef("feature", "grapple"), RelatedRef("spec", "sound-design"))
        assertEquals(refs, DocumentJsonColumns.decodeRelatedRefs(DocumentJsonColumns.encodeRelatedRefs(refs)))
        val tags = listOf("grapple", "frog", "rhythm-action")
        assertEquals(tags, DocumentJsonColumns.decodeStrings(DocumentJsonColumns.encodeStrings(tags)))
    }

    @Test
    fun unknown_status_and_origin_fall_back_safely() {
        assertEquals(DocumentStatus.UNCLASSIFIED, DocumentStatus.fromDb("hologram"))
        assertEquals(ClassificationOrigin.AI, ClassificationOrigin.fromDb("robot"))
    }

    @Test
    fun documents_flow_maps_to_domain() = runBlocking {
        val dao = FakeDocumentDao()
        val repo = repository(dao)
        repo.createSample()
        val docs = repo.documents().first()
        assertEquals(1, docs.size)
        assertEquals(DocumentStatus.UNCLASSIFIED, docs.single().status)
    }

    /** ホームの「確認待ちの文書」は needs_review だけを数える。 */
    @Test
    fun needsReviewCount_counts_only_documents_awaiting_confirmation() = runBlocking {
        val dao = FakeDocumentDao()
        val repo = repository(dao)
        assertEquals(0, repo.needsReviewCount().first())

        val awaiting = repo.createSample()
        val approved = repo.createSample()
        repo.createSample()  // 未整理のまま
        repo.applyAiClassification(
            awaiting.id, "確認待ち", "memo", 0.9, emptyList(), emptyList(), emptyList(), emptyList(),
        )
        repo.applyAiClassification(
            approved.id, "承認済み", "memo", 0.9, emptyList(), emptyList(), emptyList(), emptyList(),
        )
        assertEquals(2, repo.needsReviewCount().first())

        repo.approve(approved.id)
        assertEquals(1, repo.needsReviewCount().first())
    }

    @Test
    fun document_carries_history_newest_first_with_current_flagged() = runBlocking {
        val dao = FakeDocumentDao()
        val repo = repository(dao)
        val doc = repo.createSample()
        repo.applyAiClassification(
            doc.id, "1回目", "memo", 0.5, emptyList(), emptyList(), emptyList(), emptyList(),
        )
        repo.approveWithEdits(
            doc.id, "2回目(修正)", "design-discussion",
            emptyList(), emptyList(), emptyList(), emptyList(),
        )

        val loaded = repo.document(doc.id)!!
        assertEquals(listOf("2回目(修正)", "1回目"), loaded.classificationHistory.map { it.summary })
        assertEquals("2回目(修正)", loaded.currentClassification?.summary)
        assertEquals(1, loaded.classificationHistory.count { it.isCurrent })
    }
}
