package com.shiro.yosugahub.ui.screen.assistant

import com.shiro.yosugahub.data.repository.ImportEvent
import com.shiro.yosugahub.ui.component.LogLine
import com.shiro.yosugahub.ui.component.LogTone

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

        is ImportEvent.Update ->
            LogLine("  UPDATE ${event.fileName} → ${event.vaultPath}", LogTone.INFO)

        is ImportEvent.Missing ->
            LogLine("  GONE ${event.vaultPath} (元が消えた。Vault側は残す)", LogTone.WARN)

        is ImportEvent.Written ->
            LogLine("  WRITE ${event.path} … OK", LogTone.OK)

        is ImportEvent.Skip ->
            LogLine("  SKIP ${event.count} (already imported)", LogTone.INFO)

        is ImportEvent.Fail ->
            LogLine("  FAIL ${event.path}", LogTone.ERROR)

        is ImportEvent.Done -> LogLine(
            buildString {
                append("> DONE. ${event.imported} imported / ${event.updated} updated / ")
                append("${event.skipped} skipped / ${event.failed} failed")
                if (event.toInbox > 0) append(" / ${event.toInbox} inbox")
                if (event.missing > 0) append(" / ${event.missing} gone")
            },
            if (event.failed > 0) LogTone.ERROR else LogTone.ACCENT,
        )
    }
}
