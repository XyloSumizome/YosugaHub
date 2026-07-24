package com.shiro.yosugahub

import com.shiro.yosugahub.data.obsidian.NoteFilter
import com.shiro.yosugahub.data.obsidian.VaultNote
import com.shiro.yosugahub.data.obsidian.VaultNoteFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultNoteFiltersTest {

    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun note(path: String, ageDays: Long = 0L) = VaultNote(
        relativePath = path,
        name = path.substringAfterLast('/'),
        documentUri = "uri://$path",
        lastModified = now - ageDays * day,
        size = 0L,
    )

    private val lighting = note("Games/ANRI/Design/Lighting.md", ageDays = 0)
    private val log = note("Games/ANRI/Logs/2026-07-23.md", ageDays = 3)
    private val idea = note("Shared/Ideas/思いつき.md", ageDays = 40)
    private val all = listOf(lighting, log, idea)

    @Test
    fun inactive_filter_returns_everything_untouched() {
        assertEquals(all, VaultNoteFilters.apply(all, NoteFilter(), now))
        assertFalse(NoteFilter().isActive)
    }

    @Test
    fun query_matches_anywhere_in_the_path() {
        val byFolder = VaultNoteFilters.apply(all, NoteFilter(query = "Logs"), now)
        assertEquals(listOf(log), byFolder)

        val byName = VaultNoteFilters.apply(all, NoteFilter(query = "思いつき"), now)
        assertEquals(listOf(idea), byName)
    }

    @Test
    fun query_ignores_case_and_surrounding_spaces() {
        val result = VaultNoteFilters.apply(all, NoteFilter(query = "  lighting  "), now)
        assertEquals(listOf(lighting), result)
    }

    @Test
    fun recent_days_keeps_only_notes_updated_within_the_window() {
        assertEquals(listOf(lighting), VaultNoteFilters.apply(all, NoteFilter(recentDays = 1), now))
        assertEquals(
            listOf(lighting, log),
            VaultNoteFilters.apply(all, NoteFilter(recentDays = 7), now),
        )
        assertEquals(all, VaultNoteFilters.apply(all, NoteFilter(recentDays = 60), now))
    }

    @Test
    fun notes_without_a_timestamp_are_excluded_when_filtering_by_recency() {
        val unknown = lighting.copy(relativePath = "Inbox/謎.md", lastModified = 0L)
        val notes = listOf(lighting, unknown)

        // 更新時刻が分からないものを「最近更新された」に混ぜない
        assertEquals(listOf(lighting), VaultNoteFilters.apply(notes, NoteFilter(recentDays = 7), now))
        // 期間で絞らないときは残る
        assertEquals(notes, VaultNoteFilters.apply(notes, NoteFilter(query = ""), now))
    }

    @Test
    fun query_and_recency_are_combined_with_and() {
        val filter = NoteFilter(query = "Games", recentDays = 7)
        assertEquals(listOf(lighting, log), VaultNoteFilters.apply(all, filter, now))

        val narrower = NoteFilter(query = "Design", recentDays = 7)
        assertEquals(listOf(lighting), VaultNoteFilters.apply(all, narrower, now))
    }

    @Test
    fun original_order_is_preserved() {
        val reversed = all.reversed()
        val result = VaultNoteFilters.apply(reversed, NoteFilter(query = ".md"), now)
        assertEquals(reversed, result)
    }

    @Test
    fun is_active_reflects_either_condition() {
        assertTrue(NoteFilter(query = "a").isActive)
        assertTrue(NoteFilter(recentDays = 7).isActive)
        assertFalse(NoteFilter(query = "   ").isActive)
    }
}
