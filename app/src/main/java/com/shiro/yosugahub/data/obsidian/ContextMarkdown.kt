package com.shiro.yosugahub.data.obsidian

/** 出力の Frontmatter に載せる「何を選んだか」。空の項目は出力しない。 */
data class ContextScope(
    val games: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val folders: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = games.isEmpty() && tags.isEmpty() && folders.isEmpty()

    companion object {
        /** 選択されたノートから、載っているゲーム名・タグ・フォルダを集める。 */
        fun of(notes: List<LoadedNote>): ContextScope = ContextScope(
            games = notes.mapNotNull { it.game }.filter { it.isNotBlank() }.distinct().sorted(),
            tags = notes.flatMap { it.tags }.distinct().sorted(),
            folders = notes.map { it.relativePath.substringBeforeLast('/', "") }
                .filter { it.isNotBlank() }.distinct().sorted(),
        )
    }
}

/**
 * 選択されたノートを 1 本のコンテキスト Markdown へ連結する純粋関数(設計書v5 §4)。
 *
 * **要約はしない。** 渡された本文を整形して連ねるだけ。
 * 要約が必要になっても、この関数ではなく `NoteTransformer` を差し替える。
 */
object ContextMarkdown {

    /** これを超えたら画面で警告を出す目安。制限はしない(設計書v5 §11)。 */
    const val WARN_CHAR_COUNT = 60_000

    private const val SEPARATOR = "---"

    fun build(
        notes: List<LoadedNote>,
        vaultName: String,
        generatedAt: String,
        scope: ContextScope = ContextScope.of(notes),
    ): String = buildString {
        appendLine(SEPARATOR)
        appendLine("type: yosuga-context")
        appendLine("generated_at: ${yamlScalar(generatedAt)}")
        appendLine("source: obsidian")
        appendLine("vault: ${yamlScalar(vaultName)}")
        appendScope(scope)
        appendLine("file_count: ${notes.size}")
        appendLine(SEPARATOR)
        appendLine()
        appendLine("# Yosuga Context")
        appendLine()
        appendLine("以下はObsidianから抽出した、今回の会話に関連する情報です。")
        appendLine("内容を前提として、相談または設計の続きを行ってください。")

        notes.forEach { note ->
            appendLine()
            appendLine(SEPARATOR)
            appendLine()
            appendLine("## ${note.heading}")
            appendLine()
            appendLine("- Source: `${note.relativePath}`")
            appendLine("- Updated: `${note.updatedAt}`")
            if (note.tags.isNotEmpty()) {
                appendLine("- Tags: `${note.tags.joinToString(" ") { "#$it" }}`")
            }
            appendLine()
            appendLine(note.body.trim())
        }
    }

    /** Markdown の出力ファイル名。 */
    fun fileName(date: String): String = ContextFileNames.of(date, ContextFormat.MARKDOWN)

    private fun StringBuilder.appendScope(scope: ContextScope) {
        if (scope.isEmpty) {
            appendLine("selected_scope: {}")
            return
        }
        appendLine("selected_scope:")
        appendList("games", scope.games)
        appendList("tags", scope.tags)
        appendList("folders", scope.folders)
    }

    private fun StringBuilder.appendList(key: String, values: List<String>) {
        if (values.isEmpty()) return
        appendLine("  $key:")
        values.forEach { appendLine("    - ${yamlScalar(it)}") }
    }

    /**
     * YAML の平文スカラーとして書けない場合だけ引用符で囲む。
     * ISO 日時(`2026-07-24T08:00:00+09:00`)は `: ` を含まないためそのまま出る。
     */
    private fun yamlScalar(value: String): String {
        val needsQuote = value.isEmpty() ||
            value.first() in YAML_INDICATORS ||
            value.contains(": ") ||
            value.endsWith(":") ||
            value.contains(" #") ||
            value.first().isWhitespace() ||
            value.last().isWhitespace()
        if (!needsQuote) return value
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }

    private val YAML_INDICATORS = "-?:,[]{}#&*!|>'\"%@`".toSet()
}
