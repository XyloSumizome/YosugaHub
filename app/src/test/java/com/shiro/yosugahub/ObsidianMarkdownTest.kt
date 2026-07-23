package com.shiro.yosugahub

import com.shiro.yosugahub.data.obsidian.ObsidianMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObsidianMarkdownTest {

    @Test
    fun normalize_appends_md_extension_and_strips_paths() {
        assertEquals("GameDesign.md", ObsidianMarkdown.normalizeNoteName("GameDesign"))
        assertEquals("GameDesign.md", ObsidianMarkdown.normalizeNoteName("GameDesign.md"))
        assertEquals("Music.md", ObsidianMarkdown.normalizeNoteName("notes/Music.md"))
        assertEquals("TGS.md", ObsidianMarkdown.normalizeNoteName("..\\TGS"))
        assertEquals(ObsidianMarkdown.DEFAULT_NOTE, ObsidianMarkdown.normalizeNoteName("  "))
    }

    @Test
    fun appendix_contains_title_body_tags_and_date() {
        val markdown = ObsidianMarkdown.buildNoteAppendix(
            title = "ビート表示を採用",
            body = "リズムに合わせた表示にする。",
            tags = listOf("Yosuga Hub", "UI"),
            date = "2026-07-23",
        )
        assertTrue(markdown.contains("## ビート表示を採用"))
        assertTrue(markdown.contains("リズムに合わせた表示にする。"))
        assertTrue(markdown.contains("#Yosuga_Hub #UI"))  // タグ中の空白は _ に置換
        assertTrue(markdown.contains("2026-07-23"))
    }

    @Test
    fun appendix_omits_empty_body_and_tags() {
        val markdown = ObsidianMarkdown.buildNoteAppendix(
            title = "タイトルのみ",
            body = "",
            tags = emptyList(),
            date = "2026-07-23",
        )
        assertTrue(markdown.contains("## タイトルのみ"))
        // 見出しの「##」以外にハッシュタグ行が無いこと
        assertFalse(markdown.lines().any { it.startsWith("#") && !it.startsWith("##") })
    }
}
