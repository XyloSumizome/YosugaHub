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

    /**
     * シード由来の文言を空にする(選択A)。プロジェクト名は残したいが中身は架空、という状態を消す。
     * **シード時の値と一致するときだけ**更新するので、ユーザーが書き換えた内容は巻き込まない。
     */
    @Query(
        """
        UPDATE projects SET currentGoal = '', inProgress = '', nextTask = ''
        WHERE id = :id AND currentGoal = :currentGoal
          AND inProgress = :inProgress AND nextTask = :nextTask
        """
    )
    suspend fun clearSeededText(
        id: String,
        currentGoal: String,
        inProgress: String,
        nextTask: String,
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM projects
        WHERE id = :id AND currentGoal = :currentGoal
          AND inProgress = :inProgress AND nextTask = :nextTask
        """
    )
    suspend fun countWithSeededText(
        id: String,
        currentGoal: String,
        inProgress: String,
        nextTask: String,
    ): Int
}
