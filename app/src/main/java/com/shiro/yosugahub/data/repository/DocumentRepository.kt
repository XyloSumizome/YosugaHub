package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.db.DocumentJsonColumns
import com.shiro.yosugahub.data.local.db.dao.DocumentDao
import com.shiro.yosugahub.data.local.db.entity.DocumentClassificationEntity
import com.shiro.yosugahub.data.local.db.entity.DocumentEntity
import com.shiro.yosugahub.data.local.db.toDomain
import com.shiro.yosugahub.domain.model.ClassificationOrigin
import com.shiro.yosugahub.domain.model.Document
import com.shiro.yosugahub.domain.model.DocumentClassification
import com.shiro.yosugahub.domain.model.DocumentStatus
import com.shiro.yosugahub.domain.model.RelatedRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.OffsetDateTime

/**
 * 未整理文書 + 分類履歴の Repository(v4.1 AI分類ワークフロー)。
 * 状態遷移はすべてここを通す。原文(body)を書き換える操作は提供しない(最重要原則)。
 * AI結果とユーザー修正は別レコードとして積み、そのまま分類履歴になる。
 * now / newId はテスト容易性のため注入可能。
 */
class DocumentRepository(
    private val dao: DocumentDao,
    private val now: () -> String = { OffsetDateTime.now().toString() },
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
) {

    fun documents(): Flow<List<Document>> =
        dao.observeDocuments().map { docs -> docs.map { it.toDomain() } }

    suspend fun document(id: String): Document? = dao.getDocument(id)?.toDomain()

    /** ユーザーの確認を待っている文書の件数(ホーム表示用)。 */
    fun needsReviewCount(): Flow<Int> =
        dao.observeCountByStatus(DocumentStatus.NEEDS_REVIEW.dbValue)

    /** 分類履歴(新しい順)。現行・過去の両方を含む。 */
    suspend fun classificationHistory(id: String): List<DocumentClassification> =
        document(id)?.classificationHistory.orEmpty()

    /** 未整理文書の保存。必ず unclassified から始まる。 */
    suspend fun createDocument(title: String, body: String, source: String): Document {
        val timestamp = now()
        val entity = DocumentEntity(
            id = newId(),
            title = title,
            body = body,
            status = DocumentStatus.UNCLASSIFIED.dbValue,
            createdAt = timestamp,
            updatedAt = timestamp,
            source = source,
        )
        dao.upsertDocument(entity)
        return Document(
            id = entity.id,
            title = entity.title,
            body = entity.body,
            status = DocumentStatus.UNCLASSIFIED,
            createdAt = timestamp,
            updatedAt = timestamp,
            source = source,
            currentClassification = null,
        )
    }

    /**
     * アップロード対象(unclassified)を classification_pending へ進める。
     * サーバー同期(documents.json 送信)の成功時に呼ぶ想定。対象の文書を返す。
     */
    suspend fun markUnclassifiedAsPending(): List<Document> {
        val targets = dao.getDocumentsByStatus(DocumentStatus.UNCLASSIFIED.dbValue)
        if (targets.isNotEmpty()) {
            dao.updateStatusForAll(
                from = DocumentStatus.UNCLASSIFIED.dbValue,
                to = DocumentStatus.CLASSIFICATION_PENDING.dbValue,
                updatedAt = now(),
            )
        }
        return targets.mapNotNull { dao.getDocument(it.id)?.toDomain() }
    }

    /**
     * AI分類結果の取り込み。分類レコードを現行として積み、needs_review へ進める。
     * 原文には一切触れない。次の場合は適用せず null を返す(取込側で読み飛ばす):
     * - 文書が存在しない
     * - **アーカイブ済み**。古い回答JSONを取り込んだときに、片付けたはずの文書を
     *   勝手に復活させないため(再開したいときはユーザーが明示的に再分類する)
     */
    suspend fun applyAiClassification(
        documentId: String,
        summary: String,
        documentType: String,
        confidence: Double?,
        projectIds: List<String>,
        categories: List<String>,
        tags: List<String>,
        relatedEntities: List<RelatedRef>,
    ): Document? {
        val existing = dao.getDocument(documentId) ?: return null
        if (DocumentStatus.fromDb(existing.document.status) == DocumentStatus.ARCHIVED) return null
        saveClassification(
            documentId = documentId,
            summary = summary,
            documentType = documentType,
            confidence = confidence,
            projectIds = projectIds,
            categories = categories,
            tags = tags,
            relatedEntities = relatedEntities,
            origin = ClassificationOrigin.AI,
        )
        dao.updateStatus(documentId, DocumentStatus.NEEDS_REVIEW.dbValue, now())
        return dao.getDocument(documentId)?.toDomain()
    }

    /**
     * 現行分類をそのまま承認して classified へ確定する。
     * 現行分類が無い文書は承認できない(false)。
     */
    suspend fun approve(documentId: String): Boolean {
        dao.getDocument(documentId)?.toDomain()?.currentClassification ?: return false
        dao.updateStatus(documentId, DocumentStatus.CLASSIFIED.dbValue, now())
        return true
    }

    /**
     * ユーザーが修正して承認する。修正内容を USER レコードとして積み(AI結果は履歴に残る)、
     * classified へ確定する。confidence はユーザー修正には無いので null。
     */
    suspend fun approveWithEdits(
        documentId: String,
        summary: String,
        documentType: String,
        projectIds: List<String>,
        categories: List<String>,
        tags: List<String>,
        relatedEntities: List<RelatedRef>,
    ): Document? {
        dao.getDocument(documentId) ?: return null
        saveClassification(
            documentId = documentId,
            summary = summary,
            documentType = documentType,
            confidence = null,
            projectIds = projectIds,
            categories = categories,
            tags = tags,
            relatedEntities = relatedEntities,
            origin = ClassificationOrigin.USER,
        )
        dao.updateStatus(documentId, DocumentStatus.CLASSIFIED.dbValue, now())
        return dao.getDocument(documentId)?.toDomain()
    }

    /** 再分類を要求する。classification_pending へ戻すだけで、履歴は消さない。 */
    suspend fun requestReclassification(documentId: String) {
        dao.getDocument(documentId) ?: return
        dao.updateStatus(documentId, DocumentStatus.CLASSIFICATION_PENDING.dbValue, now())
    }

    suspend fun archive(documentId: String) {
        dao.getDocument(documentId) ?: return
        dao.updateStatus(documentId, DocumentStatus.ARCHIVED.dbValue, now())
    }

    /** 文書を分類履歴ごと削除する。 */
    suspend fun deleteDocument(documentId: String) {
        dao.deleteDocumentWithClassifications(documentId)
    }

    private suspend fun saveClassification(
        documentId: String,
        summary: String,
        documentType: String,
        confidence: Double?,
        projectIds: List<String>,
        categories: List<String>,
        tags: List<String>,
        relatedEntities: List<RelatedRef>,
        origin: ClassificationOrigin,
    ) {
        dao.saveClassificationAsCurrent(
            DocumentClassificationEntity(
                id = newId(),
                documentId = documentId,
                summary = summary,
                documentType = documentType,
                confidence = confidence,
                projectIdsJson = DocumentJsonColumns.encodeStrings(projectIds),
                categoriesJson = DocumentJsonColumns.encodeStrings(categories),
                tagsJson = DocumentJsonColumns.encodeStrings(tags),
                relatedEntitiesJson = DocumentJsonColumns.encodeRelatedRefs(relatedEntities),
                classifiedAt = now(),
                appliedBy = origin.dbValue,
                isCurrent = true,
            )
        )
    }
}
