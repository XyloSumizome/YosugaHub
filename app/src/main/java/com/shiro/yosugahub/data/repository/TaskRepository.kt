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
 * now / newId はテスト容易性のため注入可能(既定は ISO 8601 の現在時刻 / UUID)。
 */
class TaskRepository(
    private val dao: TaskDao,
    private val now: () -> String = { OffsetDateTime.now().toString() },
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
) {

    fun tasks(): Flow<List<Task>> =
        dao.observeAll().map { tasks -> tasks.map { it.toDomain() } }

    fun tasksForProject(projectId: String): Flow<List<Task>> =
        dao.observeByProject(projectId).map { tasks -> tasks.map { it.toDomain() } }

    /** 新規タスクを作成して保存する(1-c)。id・時刻はここで採番する。提案承認時は source=assistant。 */
    suspend fun create(
        projectId: String?,
        title: String,
        detail: String,
        priority: String,
        dueDate: String?,
        status: TaskStatus = TaskStatus.TODO,
        source: String = "manual",
    ): Task {
        val timestamp = now()
        val task = Task(
            id = newId(),
            projectId = projectId,
            title = title,
            detail = detail,
            status = status,
            priority = priority,
            dueDate = dueDate,
            createdAt = timestamp,
            updatedAt = timestamp,
            completedAt = if (status == TaskStatus.DONE) timestamp else null,
            source = source,
        )
        dao.upsert(task.toEntity())
        return task
    }

    /**
     * 追加・編集の保存。updatedAt はここで刻み、completedAt は status と整合させる
     * (編集で DONE にしたら刻む・DONE から戻したらクリア。既に DONE だった場合は元の時刻を保持)。
     */
    suspend fun upsert(task: Task) {
        val timestamp = now()
        dao.upsert(
            task.copy(
                updatedAt = timestamp,
                completedAt = when {
                    task.status == TaskStatus.DONE -> task.completedAt ?: timestamp
                    else -> null
                },
            ).toEntity()
        )
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

    /** プロジェクト配下のタスクをまとめて削除する(プロジェクト削除の後始末)。 */
    suspend fun deleteByProject(projectId: String) {
        dao.deleteByProject(projectId)
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}
