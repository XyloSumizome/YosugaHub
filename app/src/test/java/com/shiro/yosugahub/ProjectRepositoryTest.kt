package com.shiro.yosugahub

import com.shiro.yosugahub.data.local.SampleDataSource
import com.shiro.yosugahub.data.repository.ProjectRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectRepositoryTest {

    private val repository = ProjectRepository(SampleDataSource())

    @Test
    fun projects_flow_emits_all_seed_projects() = runBlocking {
        val projects = repository.projects().first()
        assertEquals(3, projects.size)
        assertEquals(listOf("anri", "paper-armor-frog", "gengenkyo"), projects.map { it.id })
    }
}
