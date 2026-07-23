package com.shiro.yosugahub.data.obsidian

/** Obsidian へ追記する Markdown の組み立て(純粋ロジック、ユニットテスト可能)。 */
object ObsidianMarkdown {

    /**
     * ノート名を正規化する。パス区切りを除去し(Vault直下のみ対象)、
     * `.md` 拡張子を保証する。空なら既定ノートにする。
     */
    fun normalizeNoteName(noteName: String): String {
        val base = noteName.trim().substringAfterLast('/').substringAfterLast('\\')
        if (base.isEmpty()) return DEFAULT_NOTE
        return if (base.endsWith(".md")) base else "$base.md"
    }

    /** 承認された知識アイテムをノート末尾へ追記する形の Markdown にする。 */
    fun buildNoteAppendix(
        title: String,
        body: String,
        tags: List<String>,
        date: String,
    ): String = buildString {
        append("\n## ").append(title).append("\n\n")
        if (body.isNotBlank()) {
            append(body).append("\n\n")
        }
        if (tags.isNotEmpty()) {
            append(tags.joinToString(" ") { "#${it.replace(' ', '_')}" }).append("\n")
        }
        append("*").append(date).append(" Yosuga Hub より*\n")
    }

    const val DEFAULT_NOTE = "YosugaHub.md"
}
