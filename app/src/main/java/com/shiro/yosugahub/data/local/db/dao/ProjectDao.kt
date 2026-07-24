package com.shiro.yosugahub.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.shiro.yosugahub.data.local.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun count(): Int

    /** 宛先の妥当性確認用(指示書の承認など)。 */
    @Query("SELECT COUNT(*) FROM projects WHERE id = :id")
    suspend fun countById(id: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectEntity>)

    @Upsert
    suspend fun upsert(project: ProjectEntity)

    /** 健康状態のみ更新(提案承認用)。戻り値は更新行数。 */
    @Query("UPDATE projects SET health = :health, lastUpdated = :lastUpdated WHERE id = :id")
    suspend fun updateHealth(id: String, health: String, lastUpdated: String): Int

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT COUNT(*) FROM projects WHERE id IN (:ids)")
    suspend fun countByIds(ids: List<String>): Int
}
