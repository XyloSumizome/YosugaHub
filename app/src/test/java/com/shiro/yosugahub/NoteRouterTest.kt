package com.shiro.yosugahub

import com.shiro.yosugahub.data.obsidian.Frontmatter
import com.shiro.yosugahub.data.obsidian.NoteRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteRouterTest {

    private val gameFolders = mapOf(
        "anri" to "ANRI",
        "paper-armor-frog" to "紙装甲主人公と不死身のカエル",
    )

    private fun route(
        frontmatter: String,
        fileName: String = "2026-07-24-lighting.md",
        repoProjectId: String = "anri",
    ) = NoteRouter.route(
        parsed = Frontmatter.parse(frontmatter),
        sourceFileName = fileName,
        repoProjectId = repoProjectId,
        gameFolders = gameFolders,
    )

    private fun frontmatter(vararg lines: String) =
        "---\n" + lines.joinToString("\n") + "\n---\n\n本文"

    @Test
    fun each_type_maps_to_its_folder() {
        assertEquals(
            "Games/ANRI/Design",
            route(frontmatter("type: design", "project_id: anri")).directory,
        )
        assertEquals(
            "Games/ANRI/Development Logs",
            route(frontmatter("type: development-log", "project_id: anri")).directory,
        )
        assertEquals(
            "Games/ANRI/Decisions",
            route(frontmatter("type: decision", "project_id: anri")).directory,
        )
        assertEquals(
            "Games/ANRI/Overview",
            route(frontmatter("type: reference", "project_id: anri")).directory,
        )
    }

    @Test
    fun the_repository_project_is_used_when_frontmatter_omits_project_id() {
        // どのリポジトリから取ってきたかは分かっているので Inbox 送りにしない
        val destination = route(frontmatter("type: design"), repoProjectId = "paper-armor-frog")

        assertEquals("Games/紙装甲主人公と不死身のカエル/Design", destination.directory)
        assertFalse(destination.isInbox)
    }

    @Test
    fun a_project_id_that_contradicts_the_repository_goes_to_inbox() {
        // 取り違えの可能性があるので人に見せる
        val destination = route(
            frontmatter("type: design", "project_id: gengenkyo"),
            repoProjectId = "anri",
        )

        assertTrue(destination.isInbox)
        assertEquals(NoteRouter.INBOX, destination.directory)
        assertTrue(destination.reason.contains("gengenkyo"))
    }

    @Test
    fun a_missing_type_goes_to_inbox() {
        val destination = route(frontmatter("project_id: anri"))

        assertTrue(destination.isInbox)
        assertTrue(destination.reason.contains("type"))
    }

    @Test
    fun an_unknown_type_goes_to_inbox() {
        val destination = route(frontmatter("type: 落書き", "project_id: anri"))

        assertTrue(destination.isInbox)
        assertEquals("落書き", destination.noteType)
    }

    @Test
    fun a_note_without_any_frontmatter_goes_to_inbox() {
        val destination = NoteRouter.route(
            parsed = Frontmatter.parse("# ただのメモ\n\n本文"),
            sourceFileName = "memo.md",
            repoProjectId = "anri",
            gameFolders = gameFolders,
        )

        assertTrue(destination.isInbox)
        assertEquals("memo.md", destination.fileName)
    }

    @Test
    fun an_unknown_project_goes_to_inbox() {
        val destination = route(frontmatter("type: design"), repoProjectId = "deleted-game")

        assertTrue(destination.isInbox)
        assertTrue(destination.reason.contains("deleted-game"))
    }

    @Test
    fun file_names_cannot_escape_the_directory() {
        // Frontmatter やリポジトリ側の名前を信用してディレクトリ外へ書かせない
        assertEquals("passwd.md", NoteRouter.sanitizeFileName("../../etc/passwd"))
        assertEquals("a_b.md", NoteRouter.sanitizeFileName("a:b.md"))
        assertEquals("note.md", NoteRouter.sanitizeFileName("  note.md  "))
        assertEquals("untitled.md", NoteRouter.sanitizeFileName("   "))
    }

    @Test
    fun the_extension_is_added_when_missing() {
        assertEquals("2026-07-24-lighting.md", NoteRouter.sanitizeFileName("2026-07-24-lighting"))
    }

    @Test
    fun the_full_path_joins_directory_and_name() {
        val destination = route(frontmatter("type: decision", "project_id: anri"))
        assertEquals("Games/ANRI/Decisions/2026-07-24-lighting.md", destination.path)
    }
}
