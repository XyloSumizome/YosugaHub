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

    /**
     * セッション記録(回答JSON の `session[]`)からノートを組む(2026-07-27)。
     *
     * [build] との違いは **Frontmatter を Hub が組み立てる**こと。
     * 貼り付け経路ではヨスガが書いた YAML をそのまま信じるしかなかったが、
     * JSON で受け取れるなら札は Hub が付けられる。引用符やコロンで
     * 壊れた YAML が Vault に入ると、Obsidian 側で**検索から消える**
     * (パースに失敗した Frontmatter は無いものとして扱われる)。
     *
     * `games` / `category` / `tags` は空なら**その行ごと省く**。
     * `tags: []` のような空の札は、あとで見たとき「付け忘れ」と
     * 「該当なし」の区別がつかない。
     */
    fun buildSession(
        body: String,
        date: String,
        generatedAt: String,
        games: List<String> = emptyList(),
        category: String = "",
        tags: List<String> = emptyList(),
    ): ConversationNote {
        val trimmed = body.trim()
        return ConversationNote(
            directory = DIRECTORY,
            fileName = fileName(date, SESSION_SLUG),
            content = buildString {
                appendLine(DELIMITER)
                appendLine("type: $TYPE")
                appendLine("source: $SOURCE")
                appendLine("date: $date")
                appendLine("created_at: $generatedAt")
                flowSequence("games", games)?.let { appendLine(it) }
                if (category.isNotBlank()) appendLine("category: ${quoted(category)}")
                flowSequence("tags", tags)?.let { appendLine(it) }
                appendLine(DELIMITER)
                appendLine()
                appendLine("# セッション記録($date)")
                appendLine()
                append(trimmed)
            },
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

    /**
     * `tags: ["a", "b"]` の1行。空要素は捨て、残りが無ければ null(行ごと省く)。
     *
     * **要素は必ず引用符で囲む。** フロー表記の中では `,` `]` `:` が区切りとして働き、
     * 日本語のタグでも「グラップル, 暑さ」のような値が混ざると壊れる。
     * 条件付きで囲むより、常に囲むほうが読みやすさの損より安全が勝つ。
     */
    private fun flowSequence(key: String, values: List<String>): String? {
        val cleaned = values.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return null
        return "$key: [${cleaned.joinToString(", ") { quoted(it) }}]"
    }

    /** YAML の二重引用符スカラー。バックスラッシュと引用符だけを逃がす。 */
    private fun quoted(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    /** YAML の平文スカラーとして書けない場合だけ引用符で囲む。 */
    private fun quoteIfNeeded(value: String): String =
        if (value.contains(": ") || value.first() in "-?:,[]{}#&*!|>'\"%@`") {
            "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        } else {
            value
        }

    private const val DEFAULT_SLUG = "session"

    /**
     * セッション記録のファイル名スラッグ。**日付ごとに1つ**にする。
     * 同じ日に2回頼んだら [VaultWriter] の枝番が付く(上書きしない)。
     */
    private const val SESSION_SLUG = "session"
}
