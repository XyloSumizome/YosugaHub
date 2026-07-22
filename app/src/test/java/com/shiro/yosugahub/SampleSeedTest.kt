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
}
