package com.shiro.yosugahub

import com.shiro.yosugahub.data.local.db.dao.TaskDao
import com.shiro.yosugahub.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * タスクを持たない TaskDao。
 * `ProjectRepository` がタスクから「作業中 / 次」を導出するようになったため、
 * その導出に関心のないテストで空のタスク一覧を渡すのに使う。
 */
class EmptyTaskDao : TaskDao {
    override fun observeAll(): Flow<List<TaskEntity>> = flowOf(emptyList())
    override fun observeByProject(projectId: String): Flow<List<TaskEntity>> = flowOf(emptyList())
    override suspend fun count(): Int = 0
    override suspend fun insertAll(tasks: List<TaskEntity>) = Unit
    override suspend fun upsert(task: TaskEntity) = Unit
    override suspend fun updateStatus(
        id: String,
        status: String,
        completedAt: String?,
        updatedAt: String,
    ) = Unit

    override suspend fun deleteById(id: String) = Unit
    override suspend fun deleteByProject(projectId: String) = Unit
    override suspend fun countByIds(ids: List<String>): Int = 0
}
