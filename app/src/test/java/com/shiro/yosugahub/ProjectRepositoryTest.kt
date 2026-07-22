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

/** ProjectDao をフェイクに差し替え、Repository のエンティティ→ドメイン変換を検証する。 */
class ProjectRepositoryTest {

    private class FakeProjectDao(private val stored: List<ProjectEntity>) : ProjectDao {
        override fun observeAll(): Flow<List<ProjectEntity>> = flowOf(stored)
        override suspend fun count(): Int = stored.size
        override suspend fun insertAll(projects: List<ProjectEntity>) = Unit
    }

    private val repository = ProjectRepository(FakeProjectDao(SampleSeed.projects))

    @Test
    fun projects_flow_maps_entities_to_domain_in_order() = runBlocking {
        val projects = repository.projects().first()
        assertEquals(3, projects.size)
        assertEquals(listOf("anri", "paper-armor-frog", "gengenkyo"), projects.map { it.id })
        assertEquals("ANRI", projects.first().name)
    }
}
