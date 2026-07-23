package com.shiro.yosugahub

import com.shiro.yosugahub.domain.model.EntityRef
import com.shiro.yosugahub.domain.model.EntityType
import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.domain.model.TrackedEntity
import com.shiro.yosugahub.ui.screen.records.buildEntityIndex
import com.shiro.yosugahub.ui.screen.records.entityTypesPresent
import com.shiro.yosugahub.ui.screen.records.filterEntitiesByType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 記録タブ「関連」セクションの集計ロジック(純粋関数)。 */
class EntityIndexTest {

    private fun item(title: String, vararg entities: EntityRef) = KnowledgeItem(
        id = title,
        kind = ItemKind.MEMO,
        title = title,
        body = "",
        tags = emptyList(),
        entities = entities.toList(),
        createdAt = "2026-07-23T15:00:00+09:00",
        updatedAt = "2026-07-23T15:00:00+09:00",
        source = "assistant",
    )

    private val criware = TrackedEntity("e1", "CRIWARE", EntityType.TECH)
    private val tgs = TrackedEntity("e2", "東京ゲームショウ", EntityType.EVENT)
    private val anri = TrackedEntity("e3", "ANRI", EntityType.PROJECT)

    @Test
    fun groups_items_by_entity_name_and_type() {
        val index = buildEntityIndex(
            entities = listOf(criware, tgs),
            items = listOf(
                item("音の調査", EntityRef("CRIWARE", EntityType.TECH)),
                item("出展準備", EntityRef("東京ゲームショウ", EntityType.EVENT)),
                item("両方に関係", EntityRef("CRIWARE", EntityType.TECH), EntityRef("東京ゲームショウ", EntityType.EVENT)),
            ),
        )
        assertEquals(2, index.size)
        assertEquals(listOf("音の調査", "両方に関係"), index.first { it.entity == criware }.items.map { it.title })
        assertEquals(listOf("出展準備", "両方に関係"), index.first { it.entity == tgs }.items.map { it.title })
    }

    /** 名前が同じでも種別が違えば別の実体(DBの unique index と同じ規則)。 */
    @Test
    fun same_name_with_different_type_is_a_different_entity() {
        val projectAnri = TrackedEntity("e3", "ANRI", EntityType.PROJECT)
        val techAnri = TrackedEntity("e4", "ANRI", EntityType.TECH)
        val index = buildEntityIndex(
            entities = listOf(projectAnri, techAnri),
            items = listOf(item("進捗", EntityRef("ANRI", EntityType.PROJECT))),
        )
        assertEquals(1, index.first { it.entity == projectAnri }.items.size)
        assertTrue(index.first { it.entity == techAnri }.items.isEmpty())
    }

    @Test
    fun sorts_by_relation_count_then_name() {
        val index = buildEntityIndex(
            entities = listOf(anri, criware, tgs),
            items = listOf(
                item("a", EntityRef("CRIWARE", EntityType.TECH)),
                item("b", EntityRef("CRIWARE", EntityType.TECH)),
                item("c", EntityRef("東京ゲームショウ", EntityType.EVENT)),
            ),
        )
        // CRIWARE(2件)→ 東京ゲームショウ(1件)→ ANRI(0件)
        assertEquals(listOf("CRIWARE", "東京ゲームショウ", "ANRI"), index.map { it.entity.name })
    }

    /** アイテムを消しても実体本体は残る仕様なので、関連ゼロでも隠さない。 */
    @Test
    fun keeps_entities_without_related_items() {
        val index = buildEntityIndex(entities = listOf(anri), items = emptyList())
        assertEquals(1, index.size)
        assertTrue(index.single().items.isEmpty())
    }

    @Test
    fun filters_by_type() {
        val index = buildEntityIndex(listOf(criware, tgs, anri), emptyList())
        assertEquals(3, filterEntitiesByType(index, null).size)
        assertEquals(
            listOf("CRIWARE"),
            filterEntitiesByType(index, EntityType.TECH).map { it.entity.name },
        )
    }

    /** 種別チップは実際に使われている種別だけを enum 順で出す。 */
    @Test
    fun lists_only_present_types_in_enum_order() {
        val index = buildEntityIndex(listOf(criware, tgs, anri), emptyList())
        assertEquals(
            listOf(EntityType.PROJECT, EntityType.TECH, EntityType.EVENT),
            entityTypesPresent(index),
        )
        assertTrue(entityTypesPresent(emptyList()).isEmpty())
    }

    /** 実体テーブルに無い名前がアイテム側にあっても落ちない(索引は実体テーブルが基準)。 */
    @Test
    fun ignores_item_references_without_registered_entity() {
        val index = buildEntityIndex(
            entities = listOf(criware),
            items = listOf(item("未登録の関連", EntityRef("知らない実体", EntityType.OTHER))),
        )
        assertEquals(1, index.size)
        assertTrue(index.single().items.isEmpty())
    }
}
