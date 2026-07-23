package com.shiro.yosugahub

import com.shiro.yosugahub.data.local.db.SampleSeed
import com.shiro.yosugahub.data.local.db.dao.TaskDao
import com.shiro.yosugahub.data.local.db.entity.TaskEntity
import com.shiro.yosugahub.data.repository.TaskRepository
import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** TaskDao をフェイクに差し替え、Repository の変換と書き込みロジックを検証する。 */
class TaskRepositoryTest {

    private class FakeTaskDao(initial: List<TaskEntity> = emptyList()) : TaskDao {
        val stored = initial.toMutableList()
        var lastStatusUpdate: Triple<String, String?, String>? = null // status, completedAt, updatedAt

        override fun observeAll(): Flow<List<TaskEntity>> = flowOf(stored.toList())
        override fun observeByProject(projectId: String): Flow<List<TaskEntity>> =
            flowOf(stored.filter { it.projectId == projectId })

        override suspend fun count(): Int = stored.size
        override suspend fun insertAll(tasks: List<TaskEntity>) {
            stored += tasks
        }

        override suspend fun upsert(task: TaskEntity) {
            stored.removeAll { it.id == task.id }
            stored += task
        }

        override suspend fun updateStatus(id: String, status: String, completedAt: String?, updatedAt: String) {
            lastStatusUpdate = Triple(status, completedAt, updatedAt)
        }

        override suspend fun deleteById(id: String) {
            stored.removeAll { it.id == id }
        }
    }

    private val fixedNow = "2026-07-23T12:00:00+09:00"
    private val fixedId = "task-fixed-id"

    private fun repository(dao: FakeTaskDao) =
        TaskRepository(dao, now = { fixedNow }, newId = { fixedId })

    @Test
    fun tasks_flow_maps_entities_to_domain_with_status_enum() = runBlocking {
        val repo = repository(FakeTaskDao(SampleSeed.tasks))
        val tasks = repo.tasks().first()
        assertEquals(SampleSeed.tasks.size, tasks.size)
        assertEquals(TaskStatus.DOING, tasks.first { it.id == "task-frog-001" }.status)
        assertEquals(TaskStatus.DONE, tasks.first { it.id == "task-frog-002" }.status)
        assertNull(tasks.first { it.id == "task-general-001" }.projectId)
    }

    @Test
    fun tasksForProject_filters_by_project() = runBlocking {
        val repo = repository(FakeTaskDao(SampleSeed.tasks))
        val tasks = repo.tasksForProject("paper-armor-frog").first()
        assertEquals(2, tasks.size)
        assertTrue(tasks.all { it.projectId == "paper-armor-frog" })
    }

    @Test
    fun upsert_stamps_updatedAt_and_converts_to_entity() = runBlocking {
        val dao = FakeTaskDao()
        val task = Task(
            id = "task-new-001",
            projectId = null,
            title = "新しいタスク",
            detail = "",
            status = TaskStatus.TODO,
            priority = "medium",
            dueDate = null,
            createdAt = "2026-07-23T09:00:00+09:00",
            updatedAt = "2026-07-23T09:00:00+09:00",
            completedAt = null,
            source = "manual",
        )
        repository(dao).upsert(task)
        val saved = dao.stored.single()
        assertEquals("task-new-001", saved.id)
        assertEquals("todo", saved.status)
        assertEquals(fixedNow, saved.updatedAt)
        assertEquals("2026-07-23T09:00:00+09:00", saved.createdAt) // createdAt は変えない
    }

    @Test
    fun create_assigns_id_and_stamps_times() = runBlocking {
        val dao = FakeTaskDao()
        val created = repository(dao).create(
            projectId = "anri",
            title = "新規タスク",
            detail = "",
            priority = "high",
            dueDate = "2026-07-30",
        )
        assertEquals(fixedId, created.id)
        assertEquals(TaskStatus.TODO, created.status)
        assertEquals("manual", created.source)
        val saved = dao.stored.single()
        assertEquals(fixedNow, saved.createdAt)
        assertEquals(fixedNow, saved.updatedAt)
        assertNull(saved.completedAt)
    }

    @Test
    fun create_with_done_status_stamps_completedAt() = runBlocking {
        val dao = FakeTaskDao()
        repository(dao).create(
            projectId = null,
            title = "もう終わったタスク",
            detail = "",
            priority = "low",
            dueDate = null,
            status = TaskStatus.DONE,
        )
        assertEquals(fixedNow, dao.stored.single().completedAt)
    }

    @Test
    fun upsert_keeps_original_completedAt_when_still_done() = runBlocking {
        val dao = FakeTaskDao()
        val originalCompletedAt = "2026-07-20T10:00:00+09:00"
        val task = Task(
            id = "task-done-001",
            projectId = null,
            title = "完了済み",
            detail = "",
            status = TaskStatus.DONE,
            priority = "medium",
            dueDate = null,
            createdAt = "2026-07-19T09:00:00+09:00",
            updatedAt = "2026-07-20T10:00:00+09:00",
            completedAt = originalCompletedAt,
            source = "manual",
        )
        repository(dao).upsert(task)
        assertEquals(originalCompletedAt, dao.stored.single().completedAt)
    }

    @Test
    fun upsert_clears_completedAt_when_status_reverted() = runBlocking {
        val dao = FakeTaskDao()
        val task = Task(
            id = "task-done-001",
            projectId = null,
            title = "やっぱり未完了",
            detail = "",
            status = TaskStatus.TODO,
            priority = "medium",
            dueDate = null,
            createdAt = "2026-07-19T09:00:00+09:00",
            updatedAt = "2026-07-20T10:00:00+09:00",
            completedAt = "2026-07-20T10:00:00+09:00",
            source = "manual",
        )
        repository(dao).upsert(task)
        assertNull(dao.stored.single().completedAt)
    }

    @Test
    fun setStatus_done_stamps_completedAt() = runBlocking {
        val dao = FakeTaskDao(SampleSeed.tasks)
        repository(dao).setStatus("task-anri-001", TaskStatus.DONE)
        assertEquals(Triple("done", fixedNow, fixedNow), dao.lastStatusUpdate)
    }

    @Test
    fun setStatus_back_to_todo_clears_completedAt() = runBlocking {
        val dao = FakeTaskDao(SampleSeed.tasks)
        repository(dao).setStatus("task-frog-002", TaskStatus.TODO)
        assertEquals(Triple("todo", null, fixedNow), dao.lastStatusUpdate)
    }

    @Test
    fun unknown_status_string_falls_back_to_todo() {
        assertEquals(TaskStatus.TODO, TaskStatus.fromDb("unknown_value"))
    }
}
