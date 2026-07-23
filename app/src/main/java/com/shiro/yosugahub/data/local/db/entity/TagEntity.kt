package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** tags テーブル(v3-Step 2)。name は unique(タグの生成・統合は AI の仕事)。 */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
)
