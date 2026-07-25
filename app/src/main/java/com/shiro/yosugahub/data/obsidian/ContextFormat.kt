package com.shiro.yosugahub.data.obsidian

/**
 * コンテキストの出力形式(設計書v5 §3「初期対応形式はMarkdown。必要に応じてJSONも」)。
 *
 * 正本は Markdown(人間が読む形)。JSON は機械が読む用途のための追加であって、
 * 置き換えではない(§6「Markdownを人間が読む正本とする」)。
 */
enum class ContextFormat(
    val label: String,
    val extension: String,
    val mimeType: String,
) {
    MARKDOWN("Markdown", ".md", "text/markdown"),
    JSON("JSON", ".json", "application/json"),
}

/** 出力ファイル名。日付ごとに分け、既存ファイルと衝突させない(設計書v5 §11)。 */
object ContextFileNames {
    private const val PREFIX = "yosuga_context_"

    fun of(date: String, format: ContextFormat): String = "$PREFIX$date${format.extension}"
}

/**
 * 本文を読み終えた状態のコンテキスト。**形式に依存しない**。
 * ここから Markdown にも JSON にもできるので、形式を切り替えても
 * ファイルを読み直す必要がない。
 */
data class ContextData(
    val notes: List<LoadedNote>,
    val vaultName: String,
    /** ISO 表記の生成時刻。 */
    val generatedAt: String,
    /** ファイル名に使う日付(`2026-07-24`)。 */
    val date: String,
    val scope: ContextScope,
    /** 本文を読めなかったノートの相対パス。 */
    val skipped: List<String>,
    /**
     * 一緒に渡す現況(状況JSONの本文 / 2026-07-25)。空なら過去ログだけ。
     * **形式に依存しない中間表現の一部**として持つので、
     * Markdown ⇄ JSON を切り替えてもファイルを読み直さずに済む。
     */
    val status: String = "",
)
