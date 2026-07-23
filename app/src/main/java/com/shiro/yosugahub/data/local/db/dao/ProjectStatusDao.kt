package com.shiro.yosugahub.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.shiro.yosugahub.data.local.db.entity.ProjectStatusCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectStatusDao {

    @Query("SELECT * FROM project_status_cache")
    fun observeAll(): Flow<List<ProjectStatusCacheEntity>>

    @Upsert
    suspend fun upsert(entity: ProjectStatusCacheEntity)

    @Query("DELETE FROM project_status_cache WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: String)
}
