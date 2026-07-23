package com.shiro.yosugahub

import com.shiro.yosugahub.data.local.db.CalendarBucket
import com.shiro.yosugahub.data.local.db.SampleSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleSeedTest {

    @Test
    fun projects_have_three_entries_with_unique_ids() {
        assertEquals(3, SampleSeed.projects.size)
        assertEquals(3, SampleSeed.projects.map { it.id }.toSet().size)
    }

    @Test
    fun project_ids_match_design_document() {
        val ids = SampleSeed.projects.map { it.id }
        assertTrue(ids.containsAll(listOf("anri", "paper-armor-frog", "gengenkyo")))
    }

    @Test
    fun events_use_only_known_buckets() {
        val known = setOf(CalendarBucket.TODAY, CalendarBucket.UPCOMING, CalendarBucket.PAST)
        assertTrue(SampleSeed.events.all { it.bucket in known })
    }

    @Test
    fun tasks_have_unique_ids() {
        assertEquals(SampleSeed.tasks.size, SampleSeed.tasks.map { it.id }.toSet().size)
    }

    @Test
    fun task_project_ids_reference_existing_projects_or_null() {
        val projectIds = SampleSeed.projects.map { it.id }.toSet()
        assertTrue(SampleSeed.tasks.all { it.projectId == null || it.projectId in projectIds })
    }

    @Test
    fun tasks_include_a_projectless_task() {
        assertTrue(SampleSeed.tasks.any { it.projectId == null })
    }

    @Test
    fun tasks_use_only_known_statuses_and_priorities() {
        assertTrue(SampleSeed.tasks.all { it.status in setOf("todo", "doing", "done") })
        assertTrue(SampleSeed.tasks.all { it.priority in setOf("high", "medium", "low") })
    }

    @Test
    fun completedAt_is_set_only_for_done_tasks() {
        assertTrue(SampleSeed.tasks.all { (it.status == "done") == (it.completedAt != null) })
    }
}
