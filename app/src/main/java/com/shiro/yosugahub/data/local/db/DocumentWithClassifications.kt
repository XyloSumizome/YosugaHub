package com.shiro.yosugahub.data.local.db

import androidx.room.Embedded
import androidx.room.Relation
import com.shiro.yosugahub.data.local.db.entity.DocumentClassificationEntity
import com.shiro.yosugahub.data.local.db.entity.DocumentEntity

/** 文書 + 分類履歴(v4.1)。現行分類は isCurrent = true の1件。 */
data class DocumentWithClassifications(
    @Embedded val document: DocumentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "documentId",
    )
    val classifications: List<DocumentClassificationEntity>,
)
