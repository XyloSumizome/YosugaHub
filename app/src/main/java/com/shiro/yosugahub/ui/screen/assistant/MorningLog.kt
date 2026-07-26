package com.shiro.yosugahub.ui.screen.assistant

import com.shiro.yosugahub.data.calendar.CalendarSyncResult
import com.shiro.yosugahub.data.repository.MorningStep
import com.shiro.yosugahub.data.repository.StatusFetchResult
import com.shiro.yosugahub.data.repository.SyncResult
import com.shiro.yosugahub.ui.component.LogLine
import com.shiro.yosugahub.ui.component.LogTone

/**
 * 朝の準備の各段を端末ログ行へ整形する純粋ロジック(2026-07-26)。
 * [ImportLog] と同じ方針で、色や表示は持たず文字列と種別だけを決める。
 *
 * **転んだ段は WARN 以上で出す。** 朝の準備は途中で止めずに最後まで走るので、
 * 何が転んだかがログに残らないと「全部うまくいった」と誤読される。
 */
object MorningLog {

    fun format(step: MorningStep): LogLine = when (step) {
        is MorningStep.Calendar -> calendar(step.result)
        is MorningStep.Status -> status(step.result.fetches)
        is MorningStep.Notes -> notes(step)
        is MorningStep.Sync -> sync(step.result)
    }

    private fun calendar(result: CalendarSyncResult): LogLine = when (result) {
        is CalendarSyncResult.Success ->
            LogLine("  CALENDAR ${result.eventCount} event(s) … OK", LogTone.OK)
        CalendarSyncResult.PermissionDenied ->
            LogLine("  CALENDAR 権限なし(CALENDAR 画面で許可)", LogTone.WARN)
        is CalendarSyncResult.Failed ->
            LogLine("  CALENDAR FAIL ${result.message}", LogTone.ERROR)
    }

    private fun status(fetches: List<StatusFetchResult>): LogLine {
        val ok = fetches.count { it is StatusFetchResult.Success }
        // 「未設定」は失敗ではない(取得対象外)。転んだ扱いにすると毎朝 WARN が出る。
        val skipped = fetches.count { it is StatusFetchResult.NotConfigured }
        val failed = fetches.size - ok - skipped
        val text = "  STATUS $ok/${fetches.size} project(s)" +
            if (failed > 0) " … $failed failed" else " … OK"
        return LogLine(text, if (failed > 0) LogTone.WARN else LogTone.OK)
    }

    private fun notes(step: MorningStep.Notes): LogLine {
        val s = step.summary
        if (s.vaultNotConfigured) {
            return LogLine("  NOTES Vault 未設定(書き込みをスキップ)", LogTone.WARN)
        }
        val text = "  NOTES 新規 ${s.imported} / 更新 ${s.updated} / 取得済み ${s.skipped}"
        return LogLine(text, if (s.failed > 0) LogTone.WARN else LogTone.OK)
    }

    private fun sync(result: SyncResult): LogLine = when (result) {
        is SyncResult.Success ->
            LogLine("  SYNC ${result.fileCount} file(s) → server … OK", LogTone.OK)
        SyncResult.UrlNotConfigured ->
            LogLine("  SYNC 同期先URLが未設定", LogTone.WARN)
        SyncResult.TokenMissing ->
            LogLine("  SYNC トークンが未設定", LogTone.WARN)
        SyncResult.Unauthorized ->
            LogLine("  SYNC 認証に失敗(config.php と不一致)", LogTone.ERROR)
        is SyncResult.HttpError ->
            LogLine("  SYNC FAIL HTTP ${result.statusCode}", LogTone.ERROR)
        SyncResult.NetworkError ->
            LogLine("  SYNC FAIL 通信できず", LogTone.ERROR)
    }
}
