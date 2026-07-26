package com.shiro.yosugahub

import com.shiro.yosugahub.data.calendar.CalendarSyncResult
import com.shiro.yosugahub.data.repository.MorningStep
import com.shiro.yosugahub.data.repository.NoteImportSummary
import com.shiro.yosugahub.data.repository.ProjectImportOutcome
import com.shiro.yosugahub.data.repository.StatusFetchResult
import com.shiro.yosugahub.data.repository.StatusRefreshAllResult
import com.shiro.yosugahub.data.repository.SyncResult
import com.shiro.yosugahub.data.github.model.ProjectStatus
import com.shiro.yosugahub.ui.component.LogTone
import com.shiro.yosugahub.ui.screen.assistant.MorningLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 朝の準備は途中で止まらないので、**転んだ段がログで分かる**ことが要。
 * 全部 OK の色で流れると「うまくいった」と誤読される。
 */
class MorningLogTest {

    @Test
    fun `カレンダーの権限なしは警告として出す`() {
        val line = MorningLog.format(MorningStep.Calendar(CalendarSyncResult.PermissionDenied))

        assertEquals(LogTone.WARN, line.tone)
        assertTrue(line.text.contains("権限"))
    }

    @Test
    fun `カレンダーの件数を出す`() {
        val line = MorningLog.format(MorningStep.Calendar(CalendarSyncResult.Success(7)))

        assertEquals(LogTone.OK, line.tone)
        assertTrue(line.text.contains("7"))
    }

    @Test
    fun `リポジトリ未設定は失敗に数えない`() {
        val line = MorningLog.format(
            MorningStep.Status(
                StatusRefreshAllResult(
                    fetches = listOf(
                        StatusFetchResult.Success("anri", ProjectStatus(schemaVersion = 1, projectId = "anri"), "{}"),
                        StatusFetchResult.NotConfigured("gengenkyo"),
                    ),
                ),
            ),
        )

        // 未設定は取得対象外。失敗扱いにすると毎朝 WARN が出て、本物の失敗が埋もれる。
        assertEquals(LogTone.OK, line.tone)
    }

    @Test
    fun `取得に失敗したプロジェクトがあれば警告にする`() {
        val line = MorningLog.format(
            MorningStep.Status(
                StatusRefreshAllResult(
                    fetches = listOf(
                        StatusFetchResult.Success("anri", ProjectStatus(schemaVersion = 1, projectId = "anri"), "{}"),
                        StatusFetchResult.AuthFailed("gengenkyo"),
                    ),
                ),
            ),
        )

        assertEquals(LogTone.WARN, line.tone)
    }

    @Test
    fun `Vault 未設定は警告として出す`() {
        val line = MorningLog.format(
            MorningStep.Notes(
                NoteImportSummary(outcomes = emptyList(), vaultNotConfigured = true),
            ),
        )

        assertEquals(LogTone.WARN, line.tone)
        assertTrue(line.text.contains("Vault"))
    }

    @Test
    fun `ノートの新規と更新の件数を出す`() {
        val line = MorningLog.format(
            MorningStep.Notes(
                NoteImportSummary(
                    outcomes = listOf(
                        ProjectImportOutcome(
                            projectId = "anri",
                            projectName = "ANRI",
                            imported = 2,
                            updated = 1,
                            skipped = 8,
                        ),
                    ),
                ),
            ),
        )

        assertTrue(line.text.contains("2"))
        assertTrue(line.text.contains("1"))
    }

    @Test
    fun `同期の認証失敗はエラーとして出す`() {
        val line = MorningLog.format(MorningStep.Sync(SyncResult.Unauthorized))

        assertEquals(LogTone.ERROR, line.tone)
    }

    @Test
    fun `同期の成功はファイル数を出す`() {
        val line = MorningLog.format(MorningStep.Sync(SyncResult.Success(7)))

        assertEquals(LogTone.OK, line.tone)
        assertTrue(line.text.contains("7"))
    }
}
