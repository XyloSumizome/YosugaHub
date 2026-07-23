package com.shiro.yosugahub.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.shiro.yosugahub.data.local.db.DocumentWithClassifications
import com.shiro.yosugahub.data.local.db.entity.DocumentClassificationEntity
import com.shiro.yosugahub.data.local.db.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * 文書 + 分類履歴の DAO(v4.1)。
 * 原文(documents.body)を更新する SQL は置かない — 分類は必ず別テーブルへ積む。
 */
@Dao
interface DocumentDao {

    // --- 読み取り ---

    @Transaction
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun observeDocuments(): Flow<List<DocumentWithClassifications>>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocument(id: String): DocumentWithClassifications?

    @Query("SELECT * FROM documents WHERE status = :status ORDER BY updatedAt DESC")
    suspend fun getDocumentsByStatus(status: String): List<DocumentEntity>

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun countDocuments(): Int

    /** ホームの「確認待ちの文書」用。件数だけ数える(分類行まで読まない)。 */
    @Query("SELECT COUNT(*) FROM documents WHERE status = :status")
    fun observeCountByStatus(status: String): Flow<Int>

    // --- 書き込み(部品) ---

    @Upsert
    suspend fun upsertDocument(document: DocumentEntity)

    @Query("UPDATE documents SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: String)

    @Query("UPDATE documents SET status = :to, updatedAt = :updatedAt WHERE status = :from")
    suspend fun updateStatusForAll(from: String, to: String, updatedAt: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassification(classification: DocumentClassificationEntity)

    @Query("UPDATE document_classifications SET isCurrent = 0 WHERE documentId = :documentId")
    suspend fun clearCurrentClassification(documentId: String)

    @Query("DELETE FROM document_classifications WHERE documentId = :documentId")
    suspend fun deleteClassificationsFor(documentId: String)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentRow(id: String)

    // --- 書き込み(まとまり) ---

    /**
     * 分類レコードを現行として積む(旧現行は isCurrent = false に落とし履歴として残す)。
     * 文書の状態更新は呼び出し側(Repository)の責務。
     */
    @Transaction
    suspend fun saveClassificationAsCurrent(classification: DocumentClassificationEntity) {
        clearCurrentClassification(classification.documentId)
        insertClassification(classification.copy(isCurrent = true))
    }

    /** 文書を分類履歴ごと削除する。 */
    @Transaction
    suspend fun deleteDocumentWithClassifications(id: String) {
        deleteClassificationsFor(id)
        deleteDocumentRow(id)
    }
}
