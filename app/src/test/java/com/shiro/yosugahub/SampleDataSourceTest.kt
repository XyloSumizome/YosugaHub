package com.shiro.yosugahub

import com.shiro.yosugahub.data.local.SampleDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleDataSourceTest {

    private val source = SampleDataSource()

    @Test
    fun projects_have_three_entries_with_unique_ids() {
        assertEquals(3, source.projects.size)
        assertEquals(3, source.projects.map { it.id }.toSet().size)
    }

    @Test
    fun project_ids_match_design_document() {
        val ids = source.projects.map { it.id }
        assertTrue(ids.containsAll(listOf("anri", "paper-armor-frog", "gengenkyo")))
    }
}
