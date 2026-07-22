package com.shiro.yosugahub.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.shiro.yosugahub.data.local.db.entity.RecommendationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecommendationDao {

    @Query("SELECT * FROM recommendations ORDER BY uid")
    fun observeAll(): Flow<List<RecommendationEntity>>

    @Query("SELECT COUNT(*) FROM recommendations")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(recommendations: List<RecommendationEntity>)
}
