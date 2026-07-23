package com.shiro.yosugahub.data.file

/**
 * 取り込み履歴のファイル名を扱う純粋ロジック。
 * 保存名は `response_yyyy-MM-dd_HHmmss.json`(ImportRepository.saveHistory)。
 */
object ImportHistoryNames {

    /** 履歴として扱ってよい名前か。ディレクトリ外への参照を防ぐ番人も兼ねる。 */
    private val PATTERN = Regex("""^response_(\d{4}-\d{2}-\d{2})_(\d{2})(\d{2})(\d{2})\.json$""")

    fun isValidHistoryName(fileName: String): Boolean = PATTERN.matches(fileName)

    /**
     * 表示用の保存時刻("2026-07-23 15:04")。
     * 想定外の名前(手で置いたファイルなど)は空文字を返し、一覧では名前だけ見せる。
     */
    fun formatSavedAt(fileName: String): String {
        val match = PATTERN.matchEntire(fileName) ?: return ""
        val (date, hour, minute, _) = match.destructured
        return "$date $hour:$minute"
    }
}
