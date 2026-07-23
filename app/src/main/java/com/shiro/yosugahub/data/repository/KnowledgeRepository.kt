package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.db.dao.KnowledgeDao
import com.shiro.yosugahub.data.local.db.entity.KnowledgeItemEntity
import com.shiro.yosugahub.data.local.db.entity.TagEntity
import com.shiro.yosugahub.data.local.db.entity.TrackedEntityEntity
import com.shiro.yosugahub.data.local.db.toDomain
import com.shiro.yosugahub.domain.model.EntityRef
import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.domain.model.TrackedEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.OffsetDateTime

/**
 * 情報アイテム・タグ・実体の Repository(v3-Step 2)。
 * タグの生成・統合の判断は AI の仕事で、Hub 側は承認済みの結果を保存するだけ。
 * now / newId はテスト容易性のため注入可能。
 */
class KnowledgeRepository(
    private val dao: KnowledgeDao,
    private val now: () -> String = { OffsetDateTime.now().toString() },
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
) {

    fun items(): Flow<List<KnowledgeItem>> =
        dao.observeItemsWithRefs().map { items -> items.map { it.toDomain() } }

    fun tagNames(): Flow<List<String>> =
        dao.observeTags().map { tags -> tags.map { it.name } }

    fun entities(): Flow<List<TrackedEntity>> =
        dao.observeEntities().map { entities -> entities.map { it.toDomain() } }

    /**
     * 新規アイテムの保存(承認時・手動作成の両方から使う)。
     * タグ・実体は名前で解決し、未登録なら作成する。
     */
    suspend fun createItem(
        kind: ItemKind,
        title: String,
        body: String,
        tags: List<String>,
        entities: List<EntityRef>,
        source: String,
    ): KnowledgeItem {
        val timestamp = now()
        val id = newId()
        saveWithRefs(
            entity = KnowledgeItemEntity(
                id = id,
                kind = kind.dbValue,
                title = title,
                body = body,
                createdAt = timestamp,
                updatedAt = timestamp,
                source = source,
            ),
            tags = tags,
            entities = entities,
        )
        return KnowledgeItem(
            id = id,
            kind = kind,
            title = title,
            body = body,
            tags = tags,
            entities = entities,
            createdAt = timestamp,
            updatedAt = timestamp,
            source = source,
        )
    }

    /** 既存アイテムの編集保存。updatedAt はここで刻む(createdAt は不変)。 */
    suspend fun updateItem(item: KnowledgeItem) {
        saveWithRefs(
            entity = KnowledgeItemEntity(
                id = item.id,
                kind = item.kind.dbValue,
                title = item.title,
                body = item.body,
                createdAt = item.createdAt,
                updatedAt = now(),
                source = item.source,
            ),
            tags = item.tags,
            entities = item.entities,
        )
    }

    suspend fun deleteItem(id: String) {
        dao.deleteItemWithRefs(id)
    }

    private suspend fun saveWithRefs(
        entity: KnowledgeItemEntity,
        tags: List<String>,
        entities: List<EntityRef>,
    ) {
        dao.saveItemWithRefs(
            item = entity,
            tagCandidates = tags
                .map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                .map { TagEntity(id = newId(), name = it) },
            entityCandidates = entities
                .distinctBy { it.name to it.type }
                .map { TrackedEntityEntity(id = newId(), name = it.name, type = it.type.dbValue) },
        )
    }
}
