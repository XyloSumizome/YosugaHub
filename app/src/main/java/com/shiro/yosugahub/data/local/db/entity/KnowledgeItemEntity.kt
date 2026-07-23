package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** knowledge_items テーブル(v3-Step 2)。タグ・実体は中間テーブルで関連付ける。 */
@Entity(tableName = "knowledge_items")
data class KnowledgeItemEntity(
    @PrimaryKey val id: String,
    val kind: String,       // memo / idea / decision / shopping / tech / other
    val title: String,
    val body: String,
    val createdAt: String,  // ISO 8601
    val updatedAt: String,  // ISO 8601
    val source: String,     // assistant / manual
)
