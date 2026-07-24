package com.shiro.yosugahub

import com.shiro.yosugahub.data.github.NoteListParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteListParserTest {

    private fun entry(
        name: String,
        type: String = "file",
        sha: String = "sha-$name",
        size: Long = 100,
    ) = """
        {"name":"$name","path":".yosuga/notes/$name","sha":"$sha","size":$size,"type":"$type"}
    """.trimIndent()

    private fun array(vararg entries: String) = entries.joinToString(",", "[", "]")

    @Test
    fun parses_markdown_files() {
        val result = NoteListParser.parse(
            array(entry("2026-07-24-lighting.md"), entry("2026-07-23-audio.md")),
        )

        val notes = (result as NoteListParser.Result.Success).notes
        assertEquals(2, notes.size)
        // 名前が日付始まりなので名前順＝概ね時系列順
        assertEquals("2026-07-23-audio.md", notes[0].name)
        assertEquals(".yosuga/notes/2026-07-24-lighting.md", notes[1].path)
        assertEquals("sha-2026-07-24-lighting.md", notes[1].sha)
        assertEquals(100L, notes[1].size)
    }

    @Test
    fun ignores_subdirectories_and_non_markdown() {
        val result = NoteListParser.parse(
            array(
                entry("2026-07-24-ok.md"),
                entry("archive", type = "dir"),
                entry("README.txt"),
                entry("image.png"),
            ),
        )

        val notes = (result as NoteListParser.Result.Success).notes
        assertEquals(listOf("2026-07-24-ok.md"), notes.map { it.name })
    }

    @Test
    fun skips_entries_missing_path_or_sha() {
        // sha が無いと取得済み判定ができないので採用しない
        val broken = """{"name":"x.md","path":".yosuga/notes/x.md","type":"file"}"""
        val result = NoteListParser.parse(array(broken, entry("y.md")))

        val notes = (result as NoteListParser.Result.Success).notes
        assertEquals(listOf("y.md"), notes.map { it.name })
    }

    @Test
    fun unknown_fields_do_not_break_parsing() {
        val withExtras = """
            {"name":"a.md","path":".yosuga/notes/a.md","sha":"s1","size":10,"type":"file",
             "url":"https://api.github.com/...","html_url":"https://github.com/...",
             "_links":{"self":"..."},"download_url":null}
        """.trimIndent()

        val result = NoteListParser.parse(array(withExtras))
        assertEquals(1, (result as NoteListParser.Result.Success).notes.size)
    }

    @Test
    fun empty_directory_yields_no_notes() {
        val result = NoteListParser.parse("[]")
        assertTrue((result as NoteListParser.Result.Success).notes.isEmpty())
    }

    @Test
    fun an_object_response_is_reported_as_invalid() {
        // ディレクトリではなくファイルを指すとオブジェクトが返る
        val result = NoteListParser.parse("""{"name":"status.json","type":"file"}""")
        assertTrue(result is NoteListParser.Result.InvalidJson)
    }

    @Test
    fun broken_json_is_reported_not_thrown() {
        val result = NoteListParser.parse("[{")
        assertTrue(result is NoteListParser.Result.InvalidJson)
    }

    @Test
    fun missing_size_defaults_to_zero() {
        val noSize = """{"name":"a.md","path":".yosuga/notes/a.md","sha":"s1","type":"file"}"""
        val notes = (NoteListParser.parse(array(noSize)) as NoteListParser.Result.Success).notes
        assertEquals(0L, notes.single().size)
    }
}
