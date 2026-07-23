package com.shiro.yosugahub.ui.screen.records

import com.shiro.yosugahub.domain.model.EntityType
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.domain.model.TrackedEntity

/**
 * 実体とその関連アイテムの索引(記録タブ「関連」セクション)。
 * 実体は AI が関連付けるもので、Hub は集計して見せるだけ。
 */
data class EntityIndex(
    val entity: TrackedEntity,
    val items: List<KnowledgeItem>,
)

/**
 * 実体ごとに関連アイテムを集める(純粋関数)。
 * 実体は名前 + 種別で一意(DBの unique index と同じ規則)。
 * 並びは関連の多い順 → 名前順。関連ゼロの実体も残す
 * (アイテムを消しても実体本体は残る仕様のため、その存在を隠さない)。
 */
fun buildEntityIndex(
    entities: List<TrackedEntity>,
    items: List<KnowledgeItem>,
): List<EntityIndex> {
    val itemsByKey: Map<Pair<String, EntityType>, List<KnowledgeItem>> = items
        .flatMap { item -> item.entities.map { ref -> (ref.name to ref.type) to item } }
        .groupBy({ it.first }, { it.second })

    return entities
        .map { entity -> EntityIndex(entity, itemsByKey[entity.name to entity.type].orEmpty()) }
        .sortedWith(compareByDescending<EntityIndex> { it.items.size }.thenBy { it.entity.name })
}

/** 種別で絞り込む。null は「すべて」。 */
fun filterEntitiesByType(index: List<EntityIndex>, type: EntityType?): List<EntityIndex> =
    if (type == null) index else index.filter { it.entity.type == type }

/** 実体に実際に使われている種別だけを、既定の並び(enum 順)で返す。 */
fun entityTypesPresent(index: List<EntityIndex>): List<EntityType> =
    EntityType.entries.filter { type -> index.any { it.entity.type == type } }
