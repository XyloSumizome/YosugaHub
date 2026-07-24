package com.shiro.yosugahub

import com.shiro.yosugahub.data.obsidian.ContextMarkdown
import com.shiro.yosugahub.data.obsidian.ContextScope
import com.shiro.yosugahub.data.obsidian.LoadedNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextMarkdownTest {

    private val lighting = LoadedNote(
        relativePath = "Games/ANRI/Design/Lighting.md",
        title = "Lighting Design",
        game = "ANRI",
        updatedAt = "2026-07-23T21:15:00+09:00",
        tags = listOf("ANRI", "design", "lighting"),
        body = "ライトの減衰をゆるやかにする。",
    )
    private val log = LoadedNote(
        relativePath = "Games/ANRI/Logs/2026-07-23.md",
        title = "Recent Development Log",
        game = "ANRI",
        updatedAt = "2026-07-23T23:40:00+09:00",
        tags = listOf("ANRI", "development-log"),
        body = "減衰カーブを差し替えた。",
    )

    @Test
    fun output_has_expected_frontmatter() {
        val markdown = ContextMarkdown.build(
            notes = listOf(lighting, log),
            vaultName = "YosugaVault",
            generatedAt = "2026-07-24T08:00:00+09:00",
        )

        assertTrue(markdown.startsWith("---\n"))
        assertTrue(markdown.contains("type: yosuga-context"))
        assertTrue(markdown.contains("source: obsidian"))
        assertTrue(markdown.contains("vault: YosugaVault"))
        assertTrue(markdown.contains("file_count: 2"))
        // ISO 日時は平文スカラーとして書けるため引用符を付けない
        assertTrue(markdown.contains("generated_at: 2026-07-24T08:00:00+09:00"))
    }

    @Test
    fun scope_is_derived_from_selected_notes() {
        val markdown = ContextMarkdown.build(
            notes = listOf(lighting, log),
            vaultName = "YosugaVault",
            generatedAt = "2026-07-24T08:00:00+09:00",
        )

        assertTrue(markdown.contains("selected_scope:"))
        assertTrue(markdown.contains("  games:\n    - ANRI"))
        assertTrue(markdown.contains("    - lighting"))
        assertTrue(markdown.contains("    - Games/ANRI/Design"))
    }

    @Test
    fun each_note_carries_source_updated_and_tags() {
        val markdown = ContextMarkdown.build(
            notes = listOf(lighting),
            vaultName = "YosugaVault",
            generatedAt = "2026-07-24T08:00:00+09:00",
        )

        assertTrue(markdown.contains("## ANRI / Lighting Design"))
        assertTrue(markdown.contains("- Source: `Games/ANRI/Design/Lighting.md`"))
        assertTrue(markdown.contains("- Updated: `2026-07-23T21:15:00+09:00`"))
        assertTrue(markdown.contains("- Tags: `#ANRI #design #lighting`"))
        assertTrue(markdown.contains("ライトの減衰をゆるやかにする。"))
    }

    @Test
    fun body_is_copied_verbatim_without_summarising() {
        val long = lighting.copy(body = (1..50).joinToString("\n") { "行$it" })
        val markdown = ContextMarkdown.build(
            notes = listOf(long),
            vaultName = "V",
            generatedAt = "2026-07-24T08:00:00+09:00",
        )

        // 原文の全行がそのまま含まれる(要約しない = 設計書v5 §10)
        (1..50).forEach { assertTrue(markdown.contains("行$it")) }
    }

    @Test
    fun heading_omits_game_when_absent() {
        val shared = lighting.copy(game = null, title = "共通メモ")
        val markdown = ContextMarkdown.build(
            notes = listOf(shared),
            vaultName = "V",
            generatedAt = "2026-07-24T08:00:00+09:00",
        )

        assertTrue(markdown.contains("## 共通メモ"))
        assertFalse(markdown.contains("## null /"))
    }

    @Test
    fun empty_selection_still_produces_valid_header() {
        val markdown = ContextMarkdown.build(
            notes = emptyList(),
            vaultName = "V",
            generatedAt = "2026-07-24T08:00:00+09:00",
            scope = ContextScope(),
        )

        assertTrue(markdown.contains("file_count: 0"))
        assertTrue(markdown.contains("selected_scope: {}"))
    }

    @Test
    fun vault_name_with_yaml_special_chars_is_quoted() {
        val markdown = ContextMarkdown.build(
            notes = emptyList(),
            vaultName = "My Vault: 2026",
            generatedAt = "2026-07-24T08:00:00+09:00",
        )

        assertTrue(markdown.contains("""vault: "My Vault: 2026""""))
    }

    @Test
    fun file_name_uses_the_date() {
        assertEquals("yosuga_context_2026-07-24.md", ContextMarkdown.fileName("2026-07-24"))
    }
}
