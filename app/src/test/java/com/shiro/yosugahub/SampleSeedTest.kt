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

    @Test
    fun knowledge_items_and_tags_have_unique_ids_and_names() {
        assertEquals(SampleSeed.knowledgeItems.size, SampleSeed.knowledgeItems.map { it.id }.toSet().size)
        assertEquals(SampleSeed.tags.size, SampleSeed.tags.map { it.id }.toSet().size)
        assertEquals(SampleSeed.tags.size, SampleSeed.tags.map { it.name }.toSet().size)
    }

    @Test
    fun item_cross_refs_reference_seeded_rows() {
        val itemIds = SampleSeed.knowledgeItems.map { it.id }.toSet()
        val tagIds = SampleSeed.tags.map { it.id }.toSet()
        val entityIds = SampleSeed.entities.map { it.id }.toSet()
        assertTrue(SampleSeed.itemTags.all { it.itemId in itemIds && it.tagId in tagIds })
        assertTrue(SampleSeed.itemEntities.all { it.itemId in itemIds && it.entityId in entityIds })
    }

    @Test
    fun knowledge_items_use_only_known_kinds() {
        val known = setOf("memo", "idea", "decision", "shopping", "tech", "other")
        assertTrue(SampleSeed.knowledgeItems.all { it.kind in known })
    }

    @Test
    fun entities_use_only_known_types() {
        val known = setOf("project", "person", "tech", "gear", "event", "other")
        assertTrue(SampleSeed.entities.all { it.type in known })
    }

    @Test
    fun diary_entries_use_iso_date() {
        assertTrue(SampleSeed.diaryEntries.all { it.date.matches(Regex("""\d{4}-\d{2}-\d{2}""")) })
    }
}
