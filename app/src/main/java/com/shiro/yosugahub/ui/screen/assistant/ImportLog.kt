package com.shiro.yosugahub.ui.screen.assistant

import com.shiro.yosugahub.data.repository.ImportEvent

/** 端末ログ1行の種別。UI が色分けに使う。 */
enum class LogTone { INFO, OK, WARN, ERROR, ACCENT }

/** 端末風ログの1行。 */
data class LogLine(val text: String, val tone: LogTone)

/**
 * 取り込みイベントを端末ログ行へ整形する純粋ロジック(v5 UI: ハッキング演出)。
 * 色や表示は持たず、文字列と種別だけを決める(ユニットテスト可能)。
 */
object ImportLog {

    fun format(event: ImportEvent): LogLine = when (event) {
        ImportEvent.Connect ->
            LogLine("> CONNECT github.com … OK", LogTone.ACCENT)

        is ImportEvent.Target ->
            LogLine("> TARGET ${event.projectName}", LogTone.ACCENT)

        is ImportEvent.Scan ->
            LogLine("  SCAN .yosuga/notes/ … ${event.total} file(s)", LogTone.INFO)

        is ImportEvent.Note ->
            LogLine("  ${event.message}", LogTone.INFO)

        is ImportEvent.Fetch ->
            LogLine("  FETCH ${event.fileName} … OK", LogTone.OK)

        is ImportEvent.Route -> if (event.isInbox) {
            LogLine("  ROUTE ${event.fileName} → ${event.destination} (INBOX)", LogTone.WARN)
        } else {
            LogLine("  ROUTE ${event.fileName} → ${event.destination}", LogTone.INFO)
        }

        is ImportEvent.Written ->
            LogLine("  WRITE ${event.path} … OK", LogTone.OK)

        is ImportEvent.Skip ->
            LogLine("  SKIP ${event.count} (already imported)", LogTone.INFO)

        is ImportEvent.Fail ->
            LogLine("  FAIL ${event.path}", LogTone.ERROR)

        is ImportEvent.Done -> LogLine(
            "> DONE. ${event.imported} imported / ${event.toInbox} inbox / " +
                "${event.skipped} skipped / ${event.failed} failed",
            if (event.failed > 0) LogTone.ERROR else LogTone.ACCENT,
        )
    }
}
