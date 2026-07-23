package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index

/** item_tags 中間テーブル(knowledge_items × tags の多対多)。 */
@Entity(
    tableName = "item_tags",
    primaryKeys = ["itemId", "tagId"],
    indices = [Index("tagId")],
)
data class ItemTagCrossRef(
    val itemId: String,
    val tagId: String,
)
