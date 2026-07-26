package com.shiro.yosugahub

import com.shiro.yosugahub.data.obsidian.ConversationNoteBuilder
import com.shiro.yosugahub.data.obsidian.VaultWriteResult
import com.shiro.yosugahub.data.obsidian.VaultWriter
import com.shiro.yosugahub.data.repository.ConversationImportRepository
import com.shiro.yosugahub.data.repository.ConversationImportResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ConversationNoteTest {

    private val now = OffsetDateTime.of(2026, 7, 24, 22, 0, 0, 0, ZoneOffset.ofHours(9))

    private fun build(body: String) = ConversationNoteBuilder.build(
        body = body,
        date = "2026-07-24",
        generatedAt = "2026-07-24T22:00:00+09:00",
    )

    @Test
    fun frontmatter_is_added_when_missing() {
        val note = build("# 今日のまとめ\n\n光の設計を詰めた。")

        assertTrue(note.content.startsWith("---\n"))
        assertTrue(note.content.contains("type: conversation"))
        assertTrue(note.content.contains("source: yosuga"))
        assertTrue(note.content.contains("created_at: 2026-07-24T22:00:00+09:00"))
        assertTrue(note.content.contains("title: 今日のまとめ"))
        // 本文は原文のまま
        assertTrue(note.content.contains("光の設計を詰めた。"))
    }

    @Test
    fun existing_frontmatter_is_not_wrapped_twice() {
        val original = "---\ntype: conversation\ntitle: 既にある\n---\n\n本文"
        val note = build(original)

        assertEquals(original, note.content)
        assertEquals(1, Regex("^type: conversation$", RegexOption.MULTILINE)
            .findAll(note.content).count())
    }

    @Test
    fun the_file_name_uses_the_first_heading() {
        assertEquals(
            "2026-07-24-今日のまとめ.md",
            build("# 今日のまとめ\n\n本文").fileName,
        )
    }

    @Test
    fun a_note_without_a_heading_falls_back_to_session() {
        assertEquals("2026-07-24-session.md", build("見出しのない本文").fileName)
    }

    @Test
    fun characters_that_break_paths_are_removed_from_the_name() {
        val name = ConversationNoteBuilder.fileName("2026-07-24", "光/影: 第2案")
        assertFalse(name.contains('/'))
        assertFalse(name.contains(':'))
        assertTrue(name.endsWith(".md"))
    }

    @Test
    fun long_headings_are_shortened() {
        val long = "あ".repeat(200)
        val name = ConversationNoteBuilder.fileName("2026-07-24", long)

        // 日付 + スラッグ上限 + 拡張子 に収まる
        assertTrue(name.length < 60)
    }

    @Test
    fun a_title_with_a_colon_is_quoted_in_yaml() {
        val note = build("# 光の設計: 第2案\n\n本文")
        assertTrue(note.content.contains("""title: "光の設計: 第2案""""))
    }

    @Test
    fun conversations_go_to_their_own_folder() {
        assertEquals("Conversations/Yosuga", build("本文").directory)
    }

    @Test
    fun saving_reports_the_written_path() = runBlocking {
        val writer = object : VaultWriter {
            override suspend fun write(directory: String, fileName: String, content: String) =
                VaultWriteResult.Written("$directory/$fileName")

            override suspend fun overwrite(vaultPath: String, content: String) =
                unusedOverwrite()
        }
        val repository = ConversationImportRepository(writer) { now }

        val result = repository.save("# まとめ\n\n本文")

        assertEquals(
            ConversationImportResult.Saved("Conversations/Yosuga/2026-07-24-まとめ.md"),
            result,
        )
    }

    @Test
    fun an_empty_paste_is_rejected_before_touching_the_vault() = runBlocking {
        var called = false
        val writer = object : VaultWriter {
            override suspend fun write(directory: String, fileName: String, content: String):
                VaultWriteResult {
                called = true
                return VaultWriteResult.Written("x")
            }

            override suspend fun overwrite(vaultPath: String, content: String) =
                unusedOverwrite()
        }
        val repository = ConversationImportRepository(writer) { now }

        assertEquals(ConversationImportResult.Empty, repository.save("   \n  "))
        assertFalse(called)
    }

    @Test
    fun an_unconfigured_vault_is_surfaced() = runBlocking {
        val writer = object : VaultWriter {
            override suspend fun write(directory: String, fileName: String, content: String) =
                VaultWriteResult.NotConfigured

            override suspend fun overwrite(vaultPath: String, content: String) =
                unusedOverwrite()
        }
        val repository = ConversationImportRepository(writer) { now }

        assertEquals(ConversationImportResult.VaultNotConfigured, repository.save("本文"))
    }

    /**
     * 会話ログの保存は必ず新規作成で、[VaultWriter.overwrite] を通らない
     * (同じ日に2回まとめても、別ファイルとして積む)。
     * 呼ばれたら仕様が変わったということなので、黙って通さず落とす。
     */
    private fun unusedOverwrite(): VaultWriteResult =
        throw AssertionError("会話ログの保存で overwrite は呼ばれないはず")
}
