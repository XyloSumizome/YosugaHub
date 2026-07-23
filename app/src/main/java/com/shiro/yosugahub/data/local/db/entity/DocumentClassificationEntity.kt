package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * document_classifications テーブル(v4.1)。
 * AI結果とユーザー修正を別レコードとして積む = そのまま分類履歴になる。
 * リスト項目は履歴スナップショットのため JSON 文字列で保持する
 * (後からタグを改名しても過去の分類記録は変わらない)。
 */
@Entity(
    tableName = "document_classifications",
    indices = [Index("documentId")],
)
data class DocumentClassificationEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val summary: String,
    val documentType: String,
    val confidence: Double?,          // ユーザー修正レコードは null
    val projectIdsJson: String,       // JSON配列: ["fragile-hero"]
    val categoriesJson: String,       // JSON配列: ["game-design"]
    val tagsJson: String,             // JSON配列: ["grapple"]
    val relatedEntitiesJson: String,  // JSON配列: [{"type":"feature","id":"grapple"}]
    val classifiedAt: String,         // ISO 8601
    val appliedBy: String,            // ai / user
    val isCurrent: Boolean,           // 現行の1件だけ true
)
