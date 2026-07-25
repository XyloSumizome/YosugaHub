package com.shiro.yosugahub

import com.shiro.yosugahub.data.repository.ImportEvent
import com.shiro.yosugahub.ui.screen.assistant.ImportLog
import com.shiro.yosugahub.ui.component.LogTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportLogTest {

    @Test
    fun connect_is_an_accent_line() {
        val line = ImportLog.format(ImportEvent.Connect)
        assertTrue(line.text.contains("CONNECT"))
        assertEquals(LogTone.ACCENT, line.tone)
    }

    @Test
    fun a_fetch_shows_the_real_file_name_and_reads_as_ok() {
        val line = ImportLog.format(ImportEvent.Fetch("2026-07-24-boss.md"))
        assertTrue(line.text.contains("2026-07-24-boss.md"))
        assertEquals(LogTone.OK, line.tone)
    }

    @Test
    fun a_normal_route_is_info_but_an_inbox_route_is_a_warning() {
        val normal = ImportLog.format(
            ImportEvent.Route("a.md", "Games/ANRI/Design", isInbox = false),
        )
        assertEquals(LogTone.INFO, normal.tone)
        assertTrue(normal.text.contains("Games/ANRI/Design"))

        val inbox = ImportLog.format(ImportEvent.Route("b.md", "Inbox", isInbox = true))
        // Inbox 行きは人が仕分ける必要があるので目立たせる
        assertEquals(LogTone.WARN, inbox.tone)
        assertTrue(inbox.text.contains("INBOX"))
    }

    @Test
    fun a_failure_is_an_error_line() {
        val line = ImportLog.format(ImportEvent.Fail(".yosuga/notes/x.md"))
        assertEquals(LogTone.ERROR, line.tone)
        assertTrue(line.text.contains("FAIL"))
    }

    @Test
    fun done_summarises_the_counts() {
        val line = ImportLog.format(
            ImportEvent.Done(imported = 3, toInbox = 1, updated = 0, skipped = 2, failed = 0, missing = 0),
        )
        assertTrue(line.text.contains("3 imported"))
        assertTrue(line.text.contains("1 inbox"))
        assertTrue(line.text.contains("2 skipped"))
        // 失敗ゼロなら締めはアクセント色
        assertEquals(LogTone.ACCENT, line.tone)
    }

    @Test
    fun done_turns_red_when_something_failed() {
        val line = ImportLog.format(
            ImportEvent.Done(imported = 0, toInbox = 0, updated = 0, skipped = 0, failed = 2, missing = 0),
        )
        assertEquals(LogTone.ERROR, line.tone)
    }
}
