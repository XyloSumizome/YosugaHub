package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** recommendations テーブル。Phase 2 で回答JSONの取り込み結果を格納する。 */
@Entity(tableName = "recommendations")
data class RecommendationEntity(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val projectId: String,
    val title: String,
    val detail: String,
    val priority: String,
)
