package com.shiro.yosugahub

import com.shiro.yosugahub.data.obsidian.ContextFileNames
import com.shiro.yosugahub.data.obsidian.ContextFormat
import com.shiro.yosugahub.data.obsidian.ContextJson
import com.shiro.yosugahub.data.obsidian.ContextJsonFile
import com.shiro.yosugahub.data.obsidian.ContextMarkdown
import com.shiro.yosugahub.data.obsidian.LoadedNote
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextJsonTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val lighting = LoadedNote(
        relativePath = "Games/ANRI/Design/Lighting.md",
        title = "Lighting Design",
        game = "ANRI",
        updatedAt = "2026-07-23T21:15:00+09:00",
        tags = listOf("ANRI", "design"),
        body = "ライトの減衰をゆるやかにする。",
    )
    private val shared = LoadedNote(
        relativePath = "Shared/Ideas/思いつき.md",
        title = "思いつき",
        game = null,
        updatedAt = "2026-07-24T01:00:00+09:00",
        tags = emptyList(),
        body = "雨の日だけ音が変わる。",
    )

    private fun build(notes: List<LoadedNote>) = ContextJson.build(
        notes = notes,
        vaultName = "YosugaVault",
        generatedAt = "2026-07-24T08:00:00+09:00",
    )

    @Test
    fun output_parses_back_into_the_same_data() {
        val decoded = json.decodeFromString(
            ContextJsonFile.serializer(),
            build(listOf(lighting, shared)),
        )

        assertEquals("yosuga-context", decoded.type)
        assertEquals("obsidian", decoded.source)
        assertEquals("YosugaVault", decoded.vault)
        assertEquals("2026-07-24T08:00:00+09:00", decoded.generatedAt)
        assertEquals(2, decoded.fileCount)
        assertEquals(2, decoded.notes.size)
    }

    @Test
    fun notes_carry_the_same_fields_as_the_markdown_sections() {
        val decoded = json.decodeFromString(ContextJsonFile.serializer(), build(listOf(lighting)))
        val note = decoded.notes.single()

        assertEquals("Games/ANRI/Design/Lighting.md", note.path)
        assertEquals("Lighting Design", note.title)
        assertEquals("ANRI", note.game)
        assertEquals("2026-07-23T21:15:00+09:00", note.updatedAt)
        assertEquals(listOf("ANRI", "design"), note.tags)
        assertEquals("ライトの減衰をゆるやかにする。", note.body)
    }

    @Test
    fun scope_is_derived_the_same_way_as_markdown() {
        val decoded = json.decodeFromString(
            ContextJsonFile.serializer(),
            build(listOf(lighting, shared)),
        )

        assertEquals(listOf("ANRI"), decoded.selectedScope.games)
        assertEquals(listOf("ANRI", "design"), decoded.selectedScope.tags)
        assertEquals(
            listOf("Games/ANRI/Design", "Shared/Ideas"),
            decoded.selectedScope.folders,
        )
    }

    @Test
    fun a_note_without_a_game_stays_null() {
        val decoded = json.decodeFromString(ContextJsonFile.serializer(), build(listOf(shared)))
        assertNull(decoded.notes.single().game)
    }

    @Test
    fun body_is_copied_verbatim_without_summarising() {
        val long = lighting.copy(body = (1..50).joinToString("\n") { "行$it" })
        val decoded = json.decodeFromString(ContextJsonFile.serializer(), build(listOf(long)))

        assertEquals(long.body, decoded.notes.single().body)
    }

    @Test
    fun empty_selection_is_still_valid_json() {
        val decoded = json.decodeFromString(ContextJsonFile.serializer(), build(emptyList()))

        assertEquals(0, decoded.fileCount)
        assertTrue(decoded.notes.isEmpty())
    }

    @Test
    fun file_names_differ_by_extension_only() {
        assertEquals(
            "yosuga_context_2026-07-24.md",
            ContextFileNames.of("2026-07-24", ContextFormat.MARKDOWN),
        )
        assertEquals(
            "yosuga_context_2026-07-24.json",
            ContextFileNames.of("2026-07-24", ContextFormat.JSON),
        )
        // Markdown 側の既存の入口も同じ名前を返す
        assertEquals(
            ContextFileNames.of("2026-07-24", ContextFormat.MARKDOWN),
            ContextMarkdown.fileName("2026-07-24"),
        )
    }
}
