package com.shiro.yosugahub.data.obsidian

/** Vault へ保存する会話ログ。 */
data class ConversationNote(
    val directory: String,
    val fileName: String,
    val content: String,
)

/**
 * ヨスガとの会話まとめを Obsidian 用ノートへ整える純粋ロジック(設計書v5 §7 / Phase 3-d)。
 *
 * v5 §7 のとおり **ChatGPT から自動取得はしない**。
 * ヨスガがまとめた Markdown を人が貼り、Hub は置き場所を決めて保存するだけ。
 * 将来 API 連携へ差し替えられるよう、他の取り込み経路とは独立させている。
 */
object ConversationNoteBuilder {

    const val DIRECTORY = "Conversations/Yosuga"

    private const val TYPE = "conversation"
    private const val SOURCE = "yosuga"
    private const val DELIMITER = "---"

    /** 見出しから作るファイル名スラッグの最大長。長い見出しでも扱える名前にする。 */
    private const val MAX_SLUG_LENGTH = 40

    /**
     * @param body 貼り付けられた本文
     * @param date ファイル名に使う日付(`2026-07-24`)
     * @param generatedAt Frontmatter に入れる ISO 日時
     */
    fun build(body: String, date: String, generatedAt: String): ConversationNote {
        val trimmed = body.trim()
        val title = firstHeading(trimmed)

        return ConversationNote(
            directory = DIRECTORY,
            fileName = fileName(date, title),
            // 既に Frontmatter があるなら二重に付けない(ヨスガが付けてくる場合がある)。
            content = if (hasFrontmatter(trimmed)) trimmed else wrap(trimmed, title, generatedAt),
        )
    }

    fun hasFrontmatter(text: String): Boolean =
        text.lineSequence().firstOrNull()?.trim() == DELIMITER

    /** 最初の見出し(`# タイトル`)。無ければ空文字。 */
    fun firstHeading(text: String): String =
        text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("#") }
            ?.trimStart('#')
            ?.trim()
            .orEmpty()

    /**
     * `2026-07-24-session.md` の形。見出しがあればスラッグに使う。
     * 日本語の見出しはそのまま残す(Obsidian で読めるほうが優先)。
     */
    fun fileName(date: String, title: String): String {
        val slug = title
            .replace(Regex("""[\\/:*?"<>|#\[\]]"""), "")
            .replace(Regex("""\s+"""), "-")
            .trim('-')
            .take(MAX_SLUG_LENGTH)
            .ifBlank { DEFAULT_SLUG }
        return "$date-$slug.md"
    }

    private fun wrap(body: String, title: String, generatedAt: String): String = buildString {
        appendLine(DELIMITER)
        appendLine("type: $TYPE")
        appendLine("source: $SOURCE")
        appendLine("created_at: $generatedAt")
        if (title.isNotBlank()) appendLine("title: ${quoteIfNeeded(title)}")
        appendLine("tags:")
        appendLine("  - conversation")
        appendLine("  - yosuga")
        appendLine(DELIMITER)
        appendLine()
        append(body)
    }

    /** YAML の平文スカラーとして書けない場合だけ引用符で囲む。 */
    private fun quoteIfNeeded(value: String): String =
        if (value.contains(": ") || value.first() in "-?:,[]{}#&*!|>'\"%@`") {
            "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        } else {
            value
        }

    private const val DEFAULT_SLUG = "session"
}
