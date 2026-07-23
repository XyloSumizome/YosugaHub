package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.ResponseImporter
import com.shiro.yosugahub.data.local.db.DocumentWithClassifications
import com.shiro.yosugahub.data.local.db.dao.DocumentDao
import com.shiro.yosugahub.data.local.db.entity.DocumentClassificationEntity
import com.shiro.yosugahub.data.local.db.entity.DocumentEntity
import com.shiro.yosugahub.data.repository.ClassificationApplier
import com.shiro.yosugahub.data.repository.DocumentRepository
import com.shiro.yosugahub.domain.model.ClassificationOrigin
import com.shiro.yosugahub.domain.model.DocumentStatus
import com.shiro.yosugahub.domain.model.RelatedRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 分類取り込みの経路を通しで検証する(v4.1)。
 * 回答JSONの**文字列**から ResponseImporter → ClassificationApplier → DocumentRepository まで、
 * 本番コードをそのまま通す(ファイル入出力だけが範囲外)。
 * 実機で最初に触る経路なので、ここで固めておく。
 */
class ClassificationImportTest {

    /** DocumentRepositoryTest と同じ方式のインメモリ DAO。 */
    private class FakeDocumentDao : DocumentDao {
        val documents = mutableMapOf<String, DocumentEntity>()
        val classifications = mutableMapOf<String, DocumentClassificationEntity>()

        private fun withClassifications(doc: DocumentEntity) = DocumentWithClassifications(
            document = doc,
            classifications = classifications.values.filter { it.documentId == doc.id },
        )

        override fun observeDocuments(): Flow<List<DocumentWithClassifications>> =
            flowOf(documents.values.map { withClassifications(it) })

        override suspend fun getDocument(id: String): DocumentWithClassifications? =
            documents[id]?.let { withClassifications(it) }

        override suspend fun getDocumentsByStatus(status: String): List<DocumentEntity> =
            documents.values.filter { it.status == status }

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
            classifications.values.filter { it.documentId == documentId }
                .forEach { classifications.remove(it.id) }
        }

        override suspend fun deleteDocumentRow(id: String) {
            documents.remove(id)
        }
    }

    private var tick = 0
    private var idCounter = 0

    private fun repository(dao: FakeDocumentDao) = DocumentRepository(
        dao,
        now = { "2026-07-23T16:00:%02d+09:00".format(tick++) },
        newId = { "gen-${idCounter++}" },
    )

    /** 設計書v4.1 の例に沿った、実際にヨスガが返す形の回答JSON。 */
    private val responseJson = """
        {
          "schemaVersion": 2,
          "generatedAt": "2026-07-23T21:00:00+09:00",
          "summary": "文書を分類しました",
          "proposals": {
            "items": [
              { "kind": "memo", "title": "ついでのメモ" }
            ],
            "classifications": [
              {
                "document_id": "doc-grapple",
                "project_ids": ["paper-armor-frog"],
                "categories": ["game-design", "player-action"],
                "tags": ["グラップル", "リズムアクション"],
                "document_type": "design-discussion",
                "summary": "グラップル仕様に関する検討",
                "related_entities": [
                  { "type": "feature", "id": "grapple" },
                  { "type": "", "id": "壊れた関連" }
                ],
                "confidence": 0.91,
                "requires_user_confirmation": true
              },
              {
                "document_id": "存在しない文書",
                "summary": "宛先が無い分類"
              },
              {
                "document_id": "",
                "summary": "IDが空の分類"
              }
            ]
          }
        }
    """.trimIndent()

    private fun classificationsOf(json: String) =
        (ResponseImporter.parse(json) as ResponseImporter.ParseResult.SuccessV2)
            .response.proposals.classifications

    @Test
    fun response_json_flows_through_to_document_as_needs_review() = runBlocking {
        val dao = FakeDocumentDao()
        val repository = repository(dao)
        val document = repository.createDocument(
            title = "グラップル検討メモ",
            body = "カエルの舌でグラップルする案。",
            source = "manual",
        )
        // 回答JSONは実在するIDを指す(実運用では documents.json の documentId をヨスガが使う)。
        val json = responseJson.replace("doc-grapple", document.id)

        val outcome = ClassificationApplier(repository).apply(classificationsOf(json))

        // 適用1件 / 読み飛ばし2件(宛先なし・ID空)
        assertEquals(ClassificationApplier.Outcome(applied = 1, skipped = 2), outcome)

        val classified = repository.document(document.id)!!
        assertEquals(DocumentStatus.NEEDS_REVIEW, classified.status)
        // 原文は取り込みで一切変わらない(最重要原則)
        assertEquals("カエルの舌でグラップルする案。", classified.body)

        val current = classified.currentClassification!!
        assertEquals(ClassificationOrigin.AI, current.origin)
        assertEquals("グラップル仕様に関する検討", current.summary)
        assertEquals("design-discussion", current.documentType)
        assertEquals(0.91, current.confidence!!, 0.0001)
        assertEquals(listOf("paper-armor-frog"), current.projectIds)
        assertEquals(listOf("game-design", "player-action"), current.categories)
        assertEquals(listOf("グラップル", "リズムアクション"), current.tags)
        // type が空の関連は落ちる
        assertEquals(listOf(RelatedRef("feature", "grapple")), current.relatedEntities)
    }

    @Test
    fun classification_for_unknown_document_changes_nothing() = runBlocking {
        val dao = FakeDocumentDao()
        val repository = repository(dao)

        val outcome = ClassificationApplier(repository).apply(classificationsOf(responseJson))

        assertEquals(0, outcome.applied)
        assertEquals(3, outcome.skipped)
        assertTrue(dao.documents.isEmpty())
        assertTrue(dao.classifications.isEmpty())
    }

    /** 同じ回答JSONを二度取り込んでも、履歴が積まれるだけで原文と状態は壊れない。 */
    @Test
    fun importing_the_same_response_twice_stacks_history_only() = runBlocking {
        val dao = FakeDocumentDao()
        val repository = repository(dao)
        val document = repository.createDocument("メモ", "原文", "manual")
        val json = responseJson.replace("doc-grapple", document.id)
        val applier = ClassificationApplier(repository)

        applier.apply(classificationsOf(json))
        applier.apply(classificationsOf(json))

        val reloaded = repository.document(document.id)!!
        assertEquals(DocumentStatus.NEEDS_REVIEW, reloaded.status)
        assertEquals("原文", reloaded.body)
        assertEquals(2, reloaded.classificationHistory.size)
        // 現行はちょうど1件
        assertEquals(1, reloaded.classificationHistory.count { it.isCurrent })
    }

    /**
     * 承認して確定させた文書は、あとから分類が届いても揺り戻さない。
     * やり直すときはユーザーが明示的に「再分類」を押す。
     */
    @Test
    fun classification_does_not_disturb_an_approved_document() = runBlocking {
        val dao = FakeDocumentDao()
        val repository = repository(dao)
        val document = repository.createDocument("メモ", "原文", "manual")
        val json = responseJson.replace("doc-grapple", document.id)
        val applier = ClassificationApplier(repository)

        applier.apply(classificationsOf(json))
        repository.approve(document.id)
        assertEquals(DocumentStatus.CLASSIFIED, repository.document(document.id)!!.status)

        val outcome = applier.apply(classificationsOf(json))

        assertEquals(0, outcome.applied)
        assertEquals(DocumentStatus.CLASSIFIED, repository.document(document.id)!!.status)
        // 確定済みの分類はそのまま(履歴も増えない)
        assertEquals(1, repository.classificationHistory(document.id).size)
    }

    /** 「再分類」を押した後なら、新しい分類を受け取れる(やり直しの経路が塞がっていないこと)。 */
    @Test
    fun reclassification_reopens_an_approved_document_for_new_results() = runBlocking {
        val dao = FakeDocumentDao()
        val repository = repository(dao)
        val document = repository.createDocument("メモ", "原文", "manual")
        val json = responseJson.replace("doc-grapple", document.id)
        val applier = ClassificationApplier(repository)

        applier.apply(classificationsOf(json))
        repository.approve(document.id)
        repository.requestReclassification(document.id)
        assertEquals(
            DocumentStatus.CLASSIFICATION_PENDING,
            repository.document(document.id)!!.status,
        )

        val outcome = applier.apply(classificationsOf(json))

        assertEquals(1, outcome.applied)
        assertEquals(DocumentStatus.NEEDS_REVIEW, repository.document(document.id)!!.status)
        assertEquals(2, repository.classificationHistory(document.id).size)
    }

    /** アーカイブ済みは復活しない(自己レビューで直した挙動を経路ごと固定する)。 */
    @Test
    fun archived_document_is_not_revived_by_a_stale_response() = runBlocking {
        val dao = FakeDocumentDao()
        val repository = repository(dao)
        val document = repository.createDocument("メモ", "原文", "manual")
        val json = responseJson.replace("doc-grapple", document.id)
        val applier = ClassificationApplier(repository)

        applier.apply(classificationsOf(json))
        repository.archive(document.id)

        val outcome = applier.apply(classificationsOf(json))

        assertEquals(0, outcome.applied)
        assertEquals(DocumentStatus.ARCHIVED, repository.document(document.id)!!.status)
        assertEquals(1, repository.classificationHistory(document.id).size)
    }

    /** 分類を含まない回答JSON(従来どおりの提案だけ)でも何も起きない。 */
    @Test
    fun response_without_classifications_is_a_no_op() = runBlocking {
        val dao = FakeDocumentDao()
        val repository = repository(dao)
        repository.createDocument("メモ", "原文", "manual")

        val json = """{"schemaVersion": 2, "proposals": {"tasks": [{"title": "何か"}]}}"""
        val outcome = ClassificationApplier(repository).apply(classificationsOf(json))

        assertEquals(ClassificationApplier.Outcome(applied = 0, skipped = 0), outcome)
        assertTrue(dao.classifications.isEmpty())
        assertNull(repository.document(dao.documents.keys.single())!!.currentClassification)
    }
}
