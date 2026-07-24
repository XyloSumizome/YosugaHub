package com.shiro.yosugahub

import com.shiro.yosugahub.data.obsidian.Frontmatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrontmatterTest {

    private val claudeCodeNote = """
        ---
        type: development-log
        game: ANRI
        category: lighting
        created_at: 2026-07-24T08:00:00+09:00
        updated_at: 2026-07-24T09:30:00+09:00
        source: claude-code
        tags:
          - ANRI
          - development-log
          - lighting
        related_files:
          - Assets/Scripts/LightingController.cs
          - Assets/Scenes/Main.unity
        commit:
          hash: abc1234
          branch: main
        ---

        # 変更概要

        ライトの減衰を変更した。
    """.trimIndent()

    @Test
    fun parses_claude_code_frontmatter() {
        val parsed = Frontmatter.parse(claudeCodeNote)

        assertEquals("development-log", parsed.type)
        assertEquals("ANRI", parsed.game)
        assertEquals("2026-07-24T09:30:00+09:00", parsed.updatedAt)
        assertEquals(listOf("ANRI", "development-log", "lighting"), parsed.tags)
        assertEquals(
            listOf("Assets/Scripts/LightingController.cs", "Assets/Scenes/Main.unity"),
            parsed.fields["related_files"],
        )
    }

    @Test
    fun nested_keys_are_flattened_with_dots() {
        val parsed = Frontmatter.parse(claudeCodeNote)
        assertEquals("abc1234", parsed.first("commit.hash"))
        assertEquals("main", parsed.first("commit.branch"))
    }

    @Test
    fun body_excludes_frontmatter() {
        val parsed = Frontmatter.parse(claudeCodeNote)
        assertTrue(parsed.body.startsWith("# 変更概要"))
        assertTrue(parsed.body.contains("ライトの減衰"))
        // Frontmatter の中身が本文へ混ざらないこと
        assertTrue(!parsed.body.contains("development-log"))
    }

    @Test
    fun note_without_frontmatter_keeps_whole_text_as_body() {
        val raw = "# 手書きのノート\n\n本文だけ。"
        val parsed = Frontmatter.parse(raw)

        assertEquals(raw, parsed.body)
        assertTrue(parsed.fields.isEmpty())
    }

    @Test
    fun inline_tags_are_picked_up_and_headings_are_not() {
        val raw = "# 変更概要\n\n## 実装内容\n\n#ANRI と #lighting のメモ。#設計 も。"
        val parsed = Frontmatter.parse(raw)

        assertEquals(listOf("ANRI", "lighting", "設計"), parsed.tags)
    }

    @Test
    fun frontmatter_tags_and_inline_tags_are_merged_without_duplicates() {
        val raw = """
            ---
            tags:
              - ANRI
              - design
            ---

            #ANRI の続き。#audio も関係する。
        """.trimIndent()

        assertEquals(listOf("ANRI", "design", "audio"), Frontmatter.parse(raw).tags)
    }

    @Test
    fun inline_list_and_quotes_are_supported() {
        val raw = """
            ---
            title: "光の設計: 第2案"
            tags: [ANRI, 'design']
            ---

            本文。
        """.trimIndent()
        val parsed = Frontmatter.parse(raw)

        assertEquals("光の設計: 第2案", parsed.title)
        assertEquals(listOf("ANRI", "design"), parsed.tags)
    }

    @Test
    fun unterminated_frontmatter_is_treated_as_body() {
        val raw = "---\ntype: broken\n\n本文が続く。"
        val parsed = Frontmatter.parse(raw)

        assertTrue(parsed.fields.isEmpty())
        assertTrue(parsed.body.contains("本文が続く。"))
    }
}
