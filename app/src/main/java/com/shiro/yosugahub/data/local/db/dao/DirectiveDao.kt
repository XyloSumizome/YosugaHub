package com.shiro.yosugahub.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.shiro.yosugahub.data.local.db.entity.DirectiveEntity
import kotlinx.coroutines.flow.Flow

/** 承認済み指示書の DAO(v4.2)。 */
@Dao
interface DirectiveDao {

    @Query("SELECT * FROM directives ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DirectiveEntity>>

    /** 配信対象(未完了)。並びは優先度ではなく作成順 — 出した順に読ませる。 */
    @Query("SELECT * FROM directives WHERE status = :status ORDER BY createdAt")
    suspend fun getByStatus(status: String): List<DirectiveEntity>

    @Query("SELECT COUNT(*) FROM directives WHERE status = :status")
    fun observeCountByStatus(status: String): Flow<Int>

    @Upsert
    suspend fun upsert(directive: DirectiveEntity)

    @Query("UPDATE directives SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: String)

    @Query("DELETE FROM directives WHERE id = :id")
    suspend fun delete(id: String)
}
