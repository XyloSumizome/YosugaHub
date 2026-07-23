package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index

/** item_entities 中間テーブル(knowledge_items × entities の多対多)。 */
@Entity(
    tableName = "item_entities",
    primaryKeys = ["itemId", "entityId"],
    indices = [Index("entityId")],
)
data class ItemEntityCrossRef(
    val itemId: String,
    val entityId: String,
)
