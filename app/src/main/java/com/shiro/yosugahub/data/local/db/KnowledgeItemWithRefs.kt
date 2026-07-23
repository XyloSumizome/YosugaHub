package com.shiro.yosugahub.data.local.db

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.shiro.yosugahub.data.local.db.entity.ItemEntityCrossRef
import com.shiro.yosugahub.data.local.db.entity.ItemTagCrossRef
import com.shiro.yosugahub.data.local.db.entity.KnowledgeItemEntity
import com.shiro.yosugahub.data.local.db.entity.TagEntity
import com.shiro.yosugahub.data.local.db.entity.TrackedEntityEntity

/** 情報アイテム + タグ + 実体をまとめて読むための Relation POJO。 */
data class KnowledgeItemWithRefs(
    @Embedded val item: KnowledgeItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ItemTagCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ItemEntityCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "entityId",
        ),
    )
    val entities: List<TrackedEntityEntity>,
)
