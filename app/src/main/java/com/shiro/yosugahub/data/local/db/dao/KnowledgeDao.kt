package com.shiro.yosugahub.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.shiro.yosugahub.data.local.db.KnowledgeItemWithRefs
import com.shiro.yosugahub.data.local.db.entity.ItemEntityCrossRef
import com.shiro.yosugahub.data.local.db.entity.ItemTagCrossRef
import com.shiro.yosugahub.data.local.db.entity.KnowledgeItemEntity
import com.shiro.yosugahub.data.local.db.entity.TagEntity
import com.shiro.yosugahub.data.local.db.entity.TrackedEntityEntity
import kotlinx.coroutines.flow.Flow

/**
 * 情報アイテム + タグ + 実体をまとめて扱う DAO(v3-Step 2)。
 * 3者は常に一緒に読み書きするため1つの DAO に集約する。
 */
@Dao
interface KnowledgeDao {

    // --- 読み取り ---

    @Transaction
    @Query("SELECT * FROM knowledge_items ORDER BY createdAt DESC")
    fun observeItemsWithRefs(): Flow<List<KnowledgeItemWithRefs>>

    @Query("SELECT * FROM tags ORDER BY name")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM entities ORDER BY type, name")
    fun observeEntities(): Flow<List<TrackedEntityEntity>>

    @Query("SELECT COUNT(*) FROM knowledge_items")
    suspend fun countItems(): Int

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Query("SELECT * FROM entities WHERE name = :name AND type = :type LIMIT 1")
    suspend fun getEntityByNameAndType(name: String, type: String): TrackedEntityEntity?

    // --- 書き込み(部品) ---

    @Upsert
    suspend fun upsertItem(item: KnowledgeItemEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntity(entity: TrackedEntityEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemTag(ref: ItemTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemEntity(ref: ItemEntityCrossRef)

    @Query("DELETE FROM item_tags WHERE itemId = :itemId")
    suspend fun clearItemTags(itemId: String)

    @Query("DELETE FROM item_entities WHERE itemId = :itemId")
    suspend fun clearItemEntities(itemId: String)

    @Query("DELETE FROM knowledge_items WHERE id = :itemId")
    suspend fun deleteItemRow(itemId: String)

    @Query("SELECT COUNT(*) FROM knowledge_items WHERE id IN (:ids)")
    suspend fun countItemsByIds(ids: List<String>): Int

    /** どのアイテムからも参照されていないタグだけを消す(実データのタグを巻き込まない)。 */
    @Query("DELETE FROM tags WHERE id = :id AND id NOT IN (SELECT tagId FROM item_tags)")
    suspend fun deleteTagIfUnused(id: String): Int

    /** どのアイテムからも参照されていないエンティティだけを消す。 */
    @Query("DELETE FROM entities WHERE id = :id AND id NOT IN (SELECT entityId FROM item_entities)")
    suspend fun deleteEntityIfUnused(id: String): Int

    // --- 書き込み(まとまり) ---

    /**
     * アイテムをタグ・実体ごと保存する。タグ・実体は名前で解決し、
     * 未登録なら候補(呼び出し側で採番済み)をそのまま登録する。
     * 中間テーブルは張り直し(編集で外れたタグを残さない)。
     */
    @Transaction
    suspend fun saveItemWithRefs(
        item: KnowledgeItemEntity,
        tagCandidates: List<TagEntity>,
        entityCandidates: List<TrackedEntityEntity>,
    ) {
        upsertItem(item)
        clearItemTags(item.id)
        clearItemEntities(item.id)
        for (candidate in tagCandidates) {
            val tagId = getTagByName(candidate.name)?.id ?: run {
                insertTag(candidate)
                candidate.id
            }
            insertItemTag(ItemTagCrossRef(itemId = item.id, tagId = tagId))
        }
        for (candidate in entityCandidates) {
            val entityId = getEntityByNameAndType(candidate.name, candidate.type)?.id ?: run {
                insertEntity(candidate)
                candidate.id
            }
            insertItemEntity(ItemEntityCrossRef(itemId = item.id, entityId = entityId))
        }
    }

    /** アイテムを中間テーブルごと削除する(タグ・実体本体は残す)。 */
    @Transaction
    suspend fun deleteItemWithRefs(itemId: String) {
        clearItemTags(itemId)
        clearItemEntities(itemId)
        deleteItemRow(itemId)
    }
}
