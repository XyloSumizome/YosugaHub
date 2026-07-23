package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.db.dao.TaskDao
import com.shiro.yosugahub.data.local.db.toDomain
import com.shiro.yosugahub.data.local.db.toEntity
import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.OffsetDateTime

/**
 * 制作タスクの Repository(v3-Step 1)。
 * now はテスト容易性のため注入可能(既定は ISO 8601 の現在時刻)。
 */
class TaskRepository(
    private val dao: TaskDao,
    private val now: () -> String = { OffsetDateTime.now().toString() },
) {

    fun tasks(): Flow<List<Task>> =
        dao.observeAll().map { tasks -> tasks.map { it.toDomain() } }

    fun tasksForProject(projectId: String): Flow<List<Task>> =
        dao.observeByProject(projectId).map { tasks -> tasks.map { it.toDomain() } }

    /** 追加・編集の保存。updatedAt はここで刻む。 */
    suspend fun upsert(task: Task) {
        dao.upsert(task.copy(updatedAt = now()).toEntity())
    }

    /** 状態変更。DONE にしたら completedAt を刻み、戻したらクリアする。 */
    suspend fun setStatus(id: String, status: TaskStatus) {
        val timestamp = now()
        dao.updateStatus(
            id = id,
            status = status.dbValue,
            completedAt = if (status == TaskStatus.DONE) timestamp else null,
            updatedAt = timestamp,
        )
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}
