package com.shiro.yosugahub.data.file

/**
 * 出力履歴のファイル名を扱う純粋ロジック(設計書v5 Phase 2)。
 * 保存名は `context_yyyy-MM-dd_HHmmss.md` / `.json`。
 *
 * 取り込み履歴([ImportHistoryNames])と同じ考え方で、
 * **ディレクトリ外への参照を防ぐ番人**も兼ねる。
 */
object ContextHistoryNames {

    private val PATTERN =
        Regex("""^context_(\d{4}-\d{2}-\d{2})_(\d{2})(\d{2})(\d{2})\.(md|json)$""")

    fun isValidHistoryName(fileName: String): Boolean = PATTERN.matches(fileName)

    /** 表示用の保存時刻("2026-07-24 15:04")。解釈できない名前は空文字。 */
    fun formatSavedAt(fileName: String): String {
        val match = PATTERN.matchEntire(fileName) ?: return ""
        val (date, hour, minute, _, _) = match.destructured
        return "$date $hour:$minute"
    }

    /** 表示用の形式("Markdown" / "JSON")。解釈できない名前は空文字。 */
    fun formatLabel(fileName: String): String =
        when (PATTERN.matchEntire(fileName)?.groupValues?.get(5)) {
            "md" -> "Markdown"
            "json" -> "JSON"
            else -> ""
        }
}
