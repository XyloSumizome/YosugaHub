package com.shiro.yosugahub.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shiro.yosugahub.data.local.db.entity.PendingProposalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingProposalDao {

    @Query("SELECT * FROM pending_proposals WHERE status = :status ORDER BY receivedAt DESC")
    fun observeByStatus(status: String): Flow<List<PendingProposalEntity>>

    @Query("SELECT COUNT(*) FROM pending_proposals WHERE status = :status")
    fun observeCountByStatus(status: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(proposals: List<PendingProposalEntity>)

    @Query("UPDATE pending_proposals SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}
