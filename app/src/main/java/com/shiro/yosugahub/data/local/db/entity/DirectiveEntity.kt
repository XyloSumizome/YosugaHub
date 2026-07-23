package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * directives テーブル(v4.2)。承認済みの指示書だけが入る。
 * status=open のものが directives.json として配信される。
 */
@Entity(
    tableName = "directives",
    indices = [Index("status"), Index("projectId")],
)
data class DirectiveEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val body: String,
    val priority: String,
    val status: String,     // open / done
    val createdAt: String,  // ISO 8601
    val updatedAt: String,  // ISO 8601
)
