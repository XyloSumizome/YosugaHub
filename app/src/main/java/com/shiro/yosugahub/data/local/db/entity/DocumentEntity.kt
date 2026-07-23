package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * documents テーブル(v4.1 AI分類ワークフロー)。
 * body は原文で、分類処理では一切書き換えない。分類結果は document_classifications に別で積む。
 */
@Entity(
    tableName = "documents",
    indices = [Index("status")],
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,       // 原文(不変)
    val status: String,     // unclassified / classification_pending / needs_review / classified / archived
    val createdAt: String,  // ISO 8601
    val updatedAt: String,  // ISO 8601
    val source: String,     // manual / share
)
