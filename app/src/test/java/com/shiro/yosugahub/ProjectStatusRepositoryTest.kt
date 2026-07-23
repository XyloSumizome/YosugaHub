package com.shiro.yosugahub

import com.shiro.yosugahub.data.github.model.ProjectStatus
import com.shiro.yosugahub.data.github.model.StatusBlocker
import com.shiro.yosugahub.data.github.model.StatusGoal
import com.shiro.yosugahub.data.github.model.StatusTask
import com.shiro.yosugahub.data.github.toSnapshot
import com.shiro.yosugahub.data.local.db.dao.ProjectStatusDao
import com.shiro.yosugahub.data.local.db.entity.ProjectStatusCacheEntity
import com.shiro.yosugahub.data.repository.ProjectStatusRepository
import com.shiro.yosugahub.data.repository.StatusFetchResult
import com.shiro.yosugahub.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectStatusRepositoryTest {

    private class FakeStatusDao(initial: List<ProjectStatusCacheEntity> = emptyList()) : ProjectStatusDao {
        val stored = initial.toMutableList()
        override fun observeAll(): Flow<List<ProjectStatusCacheEntity>> = flowOf(stored.toList())
        override suspend fun upsert(entity: ProjectStatusCacheEntity) {
            stored.removeAll { it.projectId == entity.projectId }
            stored += entity
        }
        override suspend fun deleteByProject(projectId: String) {
            stored.removeAll { it.projectId == projectId }
        }
    }

    private val fixedNow = "2026-07-23T22:00:00+09:00"

    private fun project(id: String = "anri", withRepo: Boolean = true) = Project(
        id = id,
        name = id,
        currentGoal = "",
        inProgress = "",
        nextTask = "",
        lastUpdated = "",
        health = "on_track",
        repoOwner = if (withRepo) "shiro" else null,
        repoName = if (withRepo) id else null,
    )

    private val validJson = """{"schemaVersion":1,"projectId":"anri","summary":"進行中"}"""

    private fun repository(
        dao: FakeStatusDao,
        fetch: suspend (Project) -> StatusFetchResult,
    ) = ProjectStatusRepository(dao = dao, fetch = fetch, now = { fixedNow })

    // --- 変換 ---

    @Test
    fun snapshot_maps_fields_and_drops_blank_titles() {
        val status = ProjectStatus(
            schemaVersion = 1,
            projectId = "anri",
            summary = "要約",
            health = "on_track",
            generatedAt = "2026-07-23T21:00:00+09:00",
            currentGoal = StatusGoal(title = "目標", detail = "詳細"),
            inProgress = listOf(
                StatusTask(title = "作業中", detail = "説明", progressPercent = 50),
                StatusTask(title = ""),  // 空タイトルは落とす
            ),
            nextTasks = listOf(StatusTask(title = "次", priority = "high")),
            blockers = listOf(StatusBlocker(title = "詰まり", severity = "high")),
            questionsForYosuga = listOf("質問", "  "),
        )
        val snapshot = status.toSnapshot(projectId = "anri", fetchedAt = fixedNow)

        assertEquals("要約", snapshot.summary)
        assertEquals("目標", snapshot.goalTitle)
        assertEquals(1, snapshot.inProgress.size)
        assertTrue(snapshot.inProgress.first().detail.contains("50%"))
        assertTrue(snapshot.nextTasks.first().detail.contains("優先度: high"))
        assertTrue(snapshot.blockers.first().detail.contains("深刻度: high"))
        assertEquals(listOf("質問"), snapshot.questionsForYosuga)
        assertEquals(fixedNow, snapshot.fetchedAt)
    }

    // --- キャッシュ ---

    @Test
    fun refresh_success_stores_raw_json_and_fetched_at() = runBlocking {
        val dao = FakeStatusDao()
        val repo = repository(dao) {
            StatusFetchResult.Success("anri", ProjectStatus(schemaVersion = 1, projectId = "anri"), validJson)
        }
        val result = repo.refresh(project())
        assertTrue(result is StatusFetchResult.Success)
        val cached = dao.stored.single()
        assertEquals("anri", cached.projectId)
        assertEquals(validJson, cached.statusJson)
        assertEquals(fixedNow, cached.fetchedAt)
    }

    @Test
    fun refresh_failure_keeps_previous_cache() = runBlocking {
        val existing = ProjectStatusCacheEntity("anri", validJson, "2026-07-01T10:00:00+09:00")
        val dao = FakeStatusDao(listOf(existing))
        val repo = repository(dao) { StatusFetchResult.NetworkError("anri") }

        val result = repo.refresh(project())
        assertTrue(result is StatusFetchResult.NetworkError)
        assertEquals(existing, dao.stored.single())  // 直近の表示を壊さない
    }

    @Test
    fun statuses_flow_parses_cache_into_snapshots() = runBlocking {
        val dao = FakeStatusDao(listOf(ProjectStatusCacheEntity("anri", validJson, fixedNow)))
        val statuses = repository(dao) { StatusFetchResult.NetworkError("anri") }.statuses().first()
        assertEquals(1, statuses.size)
        assertEquals("進行中", statuses["anri"]?.summary)
    }

    @Test
    fun statuses_flow_skips_broken_cache_rows() = runBlocking {
        val dao = FakeStatusDao(
            listOf(
                ProjectStatusCacheEntity("anri", "{ broken", fixedNow),
                ProjectStatusCacheEntity("gengenkyo", """{"schemaVersion":1,"projectId":"gengenkyo"}""", fixedNow),
            )
        )
        val statuses = repository(dao) { StatusFetchResult.NetworkError("x") }.statuses().first()
        assertEquals(1, statuses.size)
        assertNull(statuses["anri"])
        assertTrue(statuses.containsKey("gengenkyo"))
    }

    @Test
    fun refresh_all_skips_projects_without_repository() = runBlocking {
        val dao = FakeStatusDao()
        val fetched = mutableListOf<String>()
        val repo = repository(dao) { project ->
            fetched += project.id
            StatusFetchResult.Success(
                project.id,
                ProjectStatus(schemaVersion = 1, projectId = project.id),
                """{"schemaVersion":1,"projectId":"${project.id}"}""",
            )
        }
        val results = repo.refreshAll(
            listOf(project("anri"), project("gengenkyo", withRepo = false))
        )
        assertEquals(listOf("anri"), fetched)
        assertEquals(1, results.size)
    }
}
