package com.shiro.yosugahub

import com.shiro.yosugahub.data.local.db.KnowledgeItemWithRefs
import com.shiro.yosugahub.data.local.db.dao.KnowledgeDao
import com.shiro.yosugahub.data.local.db.entity.ItemEntityCrossRef
import com.shiro.yosugahub.data.local.db.entity.ItemTagCrossRef
import com.shiro.yosugahub.data.local.db.entity.KnowledgeItemEntity
import com.shiro.yosugahub.data.local.db.entity.TagEntity
import com.shiro.yosugahub.data.local.db.entity.TrackedEntityEntity
import com.shiro.yosugahub.data.repository.KnowledgeRepository
import com.shiro.yosugahub.domain.model.EntityRef
import com.shiro.yosugahub.domain.model.EntityType
import com.shiro.yosugahub.domain.model.ItemKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KnowledgeDao の抽象メソッドをインメモリ実装に差し替え、
 * default メソッド(saveItemWithRefs / deleteItemWithRefs)の実ロジックと
 * Repository の変換・タグ解決を検証する。
 */
class KnowledgeRepositoryTest {

    private class FakeKnowledgeDao : KnowledgeDao {
        val items = mutableMapOf<String, KnowledgeItemEntity>()
        val tags = mutableMapOf<String, TagEntity>()          // id -> tag
        val entities = mutableMapOf<String, TrackedEntityEntity>()
        val itemTags = mutableSetOf<ItemTagCrossRef>()
        val itemEntities = mutableSetOf<ItemEntityCrossRef>()

        override fun observeItemsWithRefs(): Flow<List<KnowledgeItemWithRefs>> =
            flowOf(
                items.values.sortedByDescending { it.createdAt }.map { item ->
                    KnowledgeItemWithRefs(
                        item = item,
                        tags = itemTags.filter { it.itemId == item.id }
                            .mapNotNull { ref -> tags[ref.tagId] },
                        entities = itemEntities.filter { it.itemId == item.id }
                            .mapNotNull { ref -> entities[ref.entityId] },
                    )
                }
            )

        override fun observeTags(): Flow<List<TagEntity>> =
            flowOf(tags.values.sortedBy { it.name })

        override fun observeEntities(): Flow<List<TrackedEntityEntity>> =
            flowOf(entities.values.sortedWith(compareBy({ it.type }, { it.name })))

        override suspend fun countItems(): Int = items.size
        override suspend fun getTagByName(name: String): TagEntity? =
            tags.values.firstOrNull { it.name == name }

        override suspend fun getEntityByNameAndType(name: String, type: String): TrackedEntityEntity? =
            entities.values.firstOrNull { it.name == name && it.type == type }

        override suspend fun upsertItem(item: KnowledgeItemEntity) {
            items[item.id] = item
        }

        override suspend fun insertTag(tag: TagEntity) {
            if (tags.values.none { it.name == tag.name }) tags[tag.id] = tag
        }

        override suspend fun insertEntity(entity: TrackedEntityEntity) {
            if (entities.values.none { it.name == entity.name && it.type == entity.type }) {
                entities[entity.id] = entity
            }
        }

        override suspend fun insertItemTag(ref: ItemTagCrossRef) {
            itemTags += ref
        }

        override suspend fun insertItemEntity(ref: ItemEntityCrossRef) {
            itemEntities += ref
        }

        override suspend fun clearItemTags(itemId: String) {
            itemTags.removeAll { it.itemId == itemId }
        }

        override suspend fun clearItemEntities(itemId: String) {
            itemEntities.removeAll { it.itemId == itemId }
        }

        override suspend fun deleteItemRow(itemId: String) {
            items.remove(itemId)
        }
    }

    private val fixedNow = "2026-07-23T15:00:00+09:00"
    private var idCounter = 0

    private fun repository(dao: FakeKnowledgeDao) =
        KnowledgeRepository(dao, now = { fixedNow }, newId = { "gen-${idCounter++}" })

    @Test
    fun createItem_saves_item_with_tags_and_entities() = runBlocking {
        val dao = FakeKnowledgeDao()
        repository(dao).createItem(
            kind = ItemKind.SHOPPING,
            title = "USB-Cハブを購入",
            body = "HDMI付き",
            tags = listOf("買い物", "展示会準備"),
            entities = listOf(EntityRef("東京ゲームショウ", EntityType.EVENT)),
            source = "assistant",
        )
        assertEquals(1, dao.items.size)
        assertEquals(2, dao.tags.size)
        assertEquals(1, dao.entities.size)
        assertEquals(2, dao.itemTags.size)
        assertEquals(1, dao.itemEntities.size)
    }

    @Test
    fun createItem_reuses_existing_tag_by_name() = runBlocking {
        val dao = FakeKnowledgeDao()
        val repo = repository(dao)
        repo.createItem(ItemKind.MEMO, "1つ目", "", listOf("買い物"), emptyList(), "manual")
        repo.createItem(ItemKind.MEMO, "2つ目", "", listOf("買い物", "新タグ"), emptyList(), "manual")
        // 「買い物」は1つだけ(名前で解決して再利用)
        assertEquals(2, dao.tags.size)
        assertEquals(1, dao.tags.values.count { it.name == "買い物" })
    }

    @Test
    fun createItem_normalizes_blank_and_duplicate_tags() = runBlocking {
        val dao = FakeKnowledgeDao()
        repository(dao).createItem(
            ItemKind.MEMO, "正規化テスト", "",
            tags = listOf(" 買い物 ", "買い物", "", "  "),
            entities = emptyList(),
            source = "manual",
        )
        assertEquals(1, dao.tags.size)
        assertEquals("買い物", dao.tags.values.single().name)
        assertEquals(1, dao.itemTags.size)
    }

    @Test
    fun updateItem_replaces_tag_links_and_keeps_createdAt() = runBlocking {
        val dao = FakeKnowledgeDao()
        val repo = repository(dao)
        val created = repo.createItem(
            ItemKind.IDEA, "アイデア", "", listOf("旧タグ"), emptyList(), "assistant",
        )
        repo.updateItem(created.copy(tags = listOf("新タグ")))

        val links = dao.itemTags.filter { it.itemId == created.id }
        assertEquals(1, links.size)
        assertEquals("新タグ", dao.tags[links.single().tagId]?.name)
        assertEquals(created.createdAt, dao.items[created.id]?.createdAt)
        // 旧タグ本体は残る(タグの統合・削除は AI の仕事)
        assertTrue(dao.tags.values.any { it.name == "旧タグ" })
    }

    @Test
    fun items_flow_maps_to_domain_with_sorted_tag_names() = runBlocking {
        val dao = FakeKnowledgeDao()
        val repo = repository(dao)
        repo.createItem(
            ItemKind.TECH, "CRIWARE調査", "",
            tags = listOf("音", "Unity連携"),
            entities = listOf(EntityRef("CRIWARE", EntityType.TECH)),
            source = "assistant",
        )
        val item = repo.items().first().single()
        assertEquals(ItemKind.TECH, item.kind)
        assertEquals(listOf("Unity連携", "音"), item.tags)
        assertEquals(EntityType.TECH, item.entities.single().type)
    }

    @Test
    fun deleteItem_removes_links_but_keeps_tags() = runBlocking {
        val dao = FakeKnowledgeDao()
        val repo = repository(dao)
        val created = repo.createItem(
            ItemKind.MEMO, "消すメモ", "", listOf("残るタグ"), emptyList(), "manual",
        )
        repo.deleteItem(created.id)
        assertTrue(dao.items.isEmpty())
        assertTrue(dao.itemTags.isEmpty())
        assertEquals(1, dao.tags.size)
    }

    @Test
    fun unknown_kind_and_type_fall_back_safely() {
        assertEquals(ItemKind.OTHER, ItemKind.fromDb("hologram"))
        assertEquals(EntityType.OTHER, EntityType.fromDb("spaceship"))
    }
}
