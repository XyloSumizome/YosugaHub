package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.ResponseImporter
import com.shiro.yosugahub.data.obsidian.ConversationNoteBuilder
import com.shiro.yosugahub.data.obsidian.VaultWriteResult
import com.shiro.yosugahub.data.obsidian.VaultWriter
import com.shiro.yosugahub.data.repository.ConversationImportRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * 1日のセッション記録(2026-07-27)。
 *
 * 回答JSONの**文字列**から ResponseImporter → ConversationImportRepository →
 * VaultWriter まで、本番コードをそのまま通す(SAF の I/O だけが範囲外)。
 * `ClassificationImportTest` と同じ方針で、経路の継ぎ目に嘘が無いことを見る。
 */
class SessionRecordTest {

    private val now = OffsetDateTime.of(2026, 7, 27, 23, 30, 0, 0, ZoneOffset.ofHours(9))

    /** 書き込みを記録するだけの Vault。 */
    private class RecordingWriter(
        private val result: (String) -> VaultWriteResult = { VaultWriteResult.Written(it) },
    ) : VaultWriter {
        val written = mutableListOf<Triple<String, String, String>>()

        override suspend fun write(directory: String, fileName: String, content: String):
            VaultWriteResult {
            written += Triple(directory, fileName, content)
            return result("$directory/$fileName")
        }

        override suspend fun overwrite(vaultPath: String, content: String) =
            throw UnsupportedOperationException("セッション記録は上書きしない")
    }

    private fun repository(writer: VaultWriter) = ConversationImportRepository(writer) { now }

    // ── ノートの組み立て ──

    @Test
    fun frontmatter_is_built_by_the_hub_not_by_yosuga() {
        val note = ConversationNoteBuilder.buildSession(
            body = "## 実装・作業\nグラップルの慣性を直した。",
            date = "2026-07-27",
            generatedAt = "2026-07-27T23:30:00+09:00",
            games = listOf("Kamieru"),
            category = "game-dev",
            tags = listOf("グラップル", "暑さ"),
        )

        assertTrue(note.content.startsWith("---\n"))
        assertTrue(note.content.contains("type: conversation"))
        assertTrue(note.content.contains("source: yosuga"))
        assertTrue(note.content.contains("date: 2026-07-27"))
        assertTrue(note.content.contains("""games: ["Kamieru"]"""))
        assertTrue(note.content.contains("""category: "game-dev""""))
        assertTrue(note.content.contains("""tags: ["グラップル", "暑さ"]"""))
        assertTrue(note.content.contains("グラップルの慣性を直した。"))
        assertEquals("Conversations/Yosuga", note.directory)
        assertEquals("2026-07-27-session.md", note.fileName)
    }

    /**
     * 空の札を残さない。`tags: []` は「付け忘れ」と「該当なし」の区別がつかず、
     * あとから見たときにどちらとも読める。
     */
    @Test
    fun empty_labels_are_omitted_entirely() {
        val note = ConversationNoteBuilder.buildSession(
            body = "本文",
            date = "2026-07-27",
            generatedAt = "2026-07-27T23:30:00+09:00",
        )

        assertFalse(note.content.contains("games:"))
        assertFalse(note.content.contains("category:"))
        assertFalse(note.content.contains("tags:"))
        // 型と日付は常に付ける(これが無いと Obsidian 側で拾えない)。
        assertTrue(note.content.contains("date: 2026-07-27"))
    }

    /**
     * フロー表記の中では `,` `]` `:` が区切りとして働く。囲わないと壊れた YAML になり、
     * **Obsidian はパースに失敗した Frontmatter を無いものとして扱う**
     * ——つまり検索から静かに消える。
     */
    @Test
    fun values_containing_separators_stay_valid_yaml() {
        val note = ConversationNoteBuilder.buildSession(
            body = "本文",
            date = "2026-07-27",
            generatedAt = "2026-07-27T23:30:00+09:00",
            tags = listOf("光, 影", "設計: 第2案"),
        )

        assertTrue(note.content.contains("""tags: ["光, 影", "設計: 第2案"]"""))
    }

    @Test
    fun blank_labels_are_dropped_from_the_list() {
        val note = ConversationNoteBuilder.buildSession(
            body = "本文",
            date = "2026-07-27",
            generatedAt = "2026-07-27T23:30:00+09:00",
            tags = listOf("グラップル", "  ", ""),
        )

        assertTrue(note.content.contains("""tags: ["グラップル"]"""))
    }

    // ── 取り込み経路 ──

    @Test
    fun a_session_in_the_response_json_reaches_the_vault() = runBlocking {
        val json = """
            {"schemaVersion": 2,
             "proposals": {"session": [{
               "date": "2026-07-27",
               "games": ["Kamieru"],
               "category": "game-dev",
               "tags": ["グラップル"],
               "body": "## 実装・作業\n慣性を直した。"
             }]}}
        """.trimIndent()

        val parsed = ResponseImporter.parse(json)
        assertTrue(parsed is ResponseImporter.ParseResult.SuccessV2)
        val sessions = (parsed as ResponseImporter.ParseResult.SuccessV2)
            .response.proposals.session

        val writer = RecordingWriter()
        val outcome = repository(writer).saveSessions(sessions)

        assertEquals(1, outcome.saved)
        assertTrue(outcome.failures.isEmpty())
        assertEquals("Conversations/Yosuga", writer.written.single().first)
        assertEquals("2026-07-27-session.md", writer.written.single().second)
        assertTrue(writer.written.single().third.contains("慣性を直した。"))
    }

    /** 日付が空でも捨てない。取り込んだ日に寄せれば記録は残る。 */
    @Test
    fun a_missing_date_falls_back_to_today() = runBlocking {
        val json = """{"schemaVersion":2,"proposals":{"session":[{"body":"本文"}]}}"""
        val sessions = (ResponseImporter.parse(json) as ResponseImporter.ParseResult.SuccessV2)
            .response.proposals.session

        val writer = RecordingWriter()
        repository(writer).saveSessions(sessions)

        assertEquals("2026-07-27-session.md", writer.written.single().second)
    }

    @Test
    fun an_empty_body_never_reaches_the_vault() = runBlocking {
        val json = """{"schemaVersion":2,"proposals":{"session":[{"date":"2026-07-27","body":"  "}]}}"""
        val sessions = (ResponseImporter.parse(json) as ResponseImporter.ParseResult.SuccessV2)
            .response.proposals.session

        val writer = RecordingWriter()
        val outcome = repository(writer).saveSessions(sessions)

        assertEquals(0, outcome.saved)
        assertTrue(writer.written.isEmpty())
    }

    /** 1件転んでも残りは書く。1日分の記録を「一部が転んだから」で丸ごと失わない。 */
    @Test
    fun one_failure_does_not_stop_the_rest() = runBlocking {
        var call = 0
        val writer = RecordingWriter { path ->
            call++
            if (call == 1) VaultWriteResult.Failed("書けません") else VaultWriteResult.Written(path)
        }
        val json = """
            {"schemaVersion":2,"proposals":{"session":[
              {"date":"2026-07-26","body":"昨日"},
              {"date":"2026-07-27","body":"今日"}
            ]}}
        """.trimIndent()
        val sessions = (ResponseImporter.parse(json) as ResponseImporter.ParseResult.SuccessV2)
            .response.proposals.session

        val outcome = repository(writer).saveSessions(sessions)

        assertEquals(1, outcome.saved)
        assertEquals(listOf("書けません"), outcome.failures)
        assertEquals(2, writer.written.size)
    }

    /**
     * セッション記録は**承認待ちに積まない**。これは Hub のデータを変える提案ではなく
     * 記録そのもので、正本は Obsidian にある(`SessionProposal` の KDoc)。
     */
    @Test
    fun sessions_are_not_queued_for_approval() {
        val json = """{"schemaVersion":2,"proposals":{"session":[{"date":"2026-07-27","body":"本文"}]}}"""
        val response = (ResponseImporter.parse(json) as ResponseImporter.ParseResult.SuccessV2).response

        val rows = com.shiro.yosugahub.data.file.ProposalMapper.toPendingEntities(
            response = response,
            receivedAt = "2026-07-27T23:30:00+09:00",
            newId = { "id" },
        )

        assertTrue(rows.isEmpty())
    }
}
