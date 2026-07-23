package com.shiro.yosugahub

import com.shiro.yosugahub.data.local.db.SampleSeed
import com.shiro.yosugahub.data.local.db.dao.ProjectDao
import com.shiro.yosugahub.data.local.db.entity.ProjectEntity
import com.shiro.yosugahub.data.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** ProjectDao をフェイクに差し替え、Repository の変換と書き込みロジックを検証する。 */
class ProjectRepositoryTest {

    private class FakeProjectDao(initial: List<ProjectEntity> = emptyList()) : ProjectDao {
        val stored = initial.toMutableList()

        override fun observeAll(): Flow<List<ProjectEntity>> = flowOf(stored.toList())
        override suspend fun count(): Int = stored.size
        override suspend fun countById(id: String): Int = stored.count { it.id == id }
        override suspend fun insertAll(projects: List<ProjectEntity>) {
            stored += projects
        }

        override suspend fun upsert(project: ProjectEntity) {
            stored.removeAll { it.id == project.id }
            stored += project
        }

        override suspend fun updateHealth(id: String, health: String, lastUpdated: String): Int {
            val target = stored.firstOrNull { it.id == id } ?: return 0
            stored[stored.indexOf(target)] = target.copy(health = health, lastUpdated = lastUpdated)
            return 1
        }
    }

    private val fixedNow = "2026-07-23 12:00"

    private val repository = ProjectRepository(FakeProjectDao(SampleSeed.projects))

    @Test
    fun projects_flow_maps_entities_to_domain_in_order() = runBlocking {
        val projects = repository.projects().first()
        assertEquals(3, projects.size)
        assertEquals(listOf("anri", "paper-armor-frog", "gengenkyo"), projects.map { it.id })
        assertEquals("ANRI", projects.first().name)
    }

    @Test
    fun upsert_stamps_lastUpdated_and_saves_edited_fields() = runBlocking {
        val dao = FakeProjectDao(SampleSeed.projects)
        val repo = ProjectRepository(dao, now = { fixedNow })
        val original = repo.projects().first().first { it.id == "anri" }

        repo.upsert(original.copy(name = "ANRI(改)", currentGoal = "体験版の完成", health = "attention"))

        val saved = dao.stored.single { it.id == "anri" }
        assertEquals("ANRI(改)", saved.name)
        assertEquals("体験版の完成", saved.currentGoal)
        assertEquals("attention", saved.health)
        assertEquals(fixedNow, saved.lastUpdated)
        // 編集対象外のフィールドは保持される
        assertEquals(original.inProgress, saved.inProgress)
        assertEquals(original.nextTask, saved.nextTask)
        assertEquals(3, dao.stored.size)
    }
}
