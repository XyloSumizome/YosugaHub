package com.shiro.yosugahub.data.obsidian

/**
 * ノートを解析した結果。値はすべて文字列リストとして持ち、
 * ネストしたキーはドット区切り(例: `commit.hash`)で平坦化する。
 */
data class ParsedNote(
    val fields: Map<String, List<String>>,
    val tags: List<String>,
    val body: String,
) {
    fun first(key: String): String? = fields[key]?.firstOrNull()

    val title: String? get() = first("title")
    val game: String? get() = first("game")
    val type: String? get() = first("type")

    /** 更新日時。`updated_at` を優先し、無ければ `created_at`。 */
    val updatedAt: String? get() = first("updated_at") ?: first("created_at")
}

/**
 * Obsidian ノートの Frontmatter と本文を分離する純粋パーサ(設計書v5 §11)。
 *
 * 完全な YAML 実装ではない。Claude Code が出力する範囲(スカラー / 文字列リスト /
 * 1 段のネスト)だけを扱い、**解釈できないものは無視して落とさない**。
 * Frontmatter が無い既存ノートは全文を本文として扱う。
 */
object Frontmatter {

    private const val DELIMITER = "---"

    private val KEY = Regex("""^([A-Za-z0-9_][A-Za-z0-9_-]*):\s*(.*)$""")

    /**
     * 本文中のインラインタグ。`#tag` に一致し、見出し(`# 変更概要`)には一致しない。
     * `#` の直後に語構成文字が続くことを要求するため、`#` + 空白 の見出しは除外される。
     */
    private val INLINE_TAG = Regex("""(?<![\w#])#([\p{L}\p{N}_/-]+)""")

    fun parse(raw: String): ParsedNote {
        val text = raw.removePrefix("\uFEFF")
        val lines = text.lines()

        // 1 行目が `---` でなければ Frontmatter 無しとみなす。
        if (lines.firstOrNull()?.trim() != DELIMITER) {
            return ParsedNote(emptyMap(), inlineTags(text), text.trim())
        }
        // 閉じ `---` が無い壊れたノートも本文として扱う(エラーにしない)。
        val end = (1 until lines.size).firstOrNull { lines[it].trim() == DELIMITER }
            ?: return ParsedNote(emptyMap(), inlineTags(text), text.trim())

        val fields = parseBlock(lines.subList(1, end))
        val body = lines.subList(end + 1, lines.size).joinToString("\n").trim()

        // Obsidian では Frontmatter と本文の両方のタグが有効。順序を保ったまま統合する。
        val tags = LinkedHashSet<String>()
        fields["tags"].orEmpty().forEach { tags.add(it.removePrefix("#")) }
        inlineTags(body).forEach { tags.add(it) }

        return ParsedNote(fields, tags.toList(), body)
    }

    /** 本文中の `#tag` を出現順に重複なく拾う。 */
    fun inlineTags(body: String): List<String> =
        INLINE_TAG.findAll(body)
            .map { it.groupValues[1] }
            .filter { it.any(Char::isLetter) } // `#2026` のような数字だけの並びは除外
            .distinct()
            .toList()

    private fun parseBlock(lines: List<String>): Map<String, List<String>> {
        val result = LinkedHashMap<String, MutableList<String>>()
        // (インデント, キー名) の入れ子。`commit:` → `hash:` を `commit.hash` にする。
        val stack = ArrayDeque<Pair<Int, String>>()
        var lastKey: String? = null

        for (line in lines) {
            if (line.isBlank()) continue
            val indent = line.indexOfFirst { !it.isWhitespace() }
            val trimmed = line.trim()

            if (trimmed.startsWith("- ") || trimmed == "-") {
                val key = lastKey ?: continue
                val value = unquote(trimmed.removePrefix("-").trim())
                if (value.isNotEmpty()) result.getOrPut(key) { mutableListOf() }.add(value)
                continue
            }

            val match = KEY.matchEntire(trimmed) ?: continue
            val name = match.groupValues[1]
            val value = match.groupValues[2].trim()

            while (stack.isNotEmpty() && stack.last().first >= indent) stack.removeLast()
            val path = (stack.map { it.second } + name).joinToString(".")
            stack.addLast(indent to name)
            lastKey = path

            if (value.isEmpty()) continue
            val values = if (value.startsWith("[") && value.endsWith("]")) {
                value.removeSurrounding("[", "]").split(',').map { unquote(it.trim()) }
            } else {
                listOf(unquote(value))
            }
            values.filter { it.isNotEmpty() }
                .forEach { result.getOrPut(path) { mutableListOf() }.add(it) }
        }
        return result
    }

    private fun unquote(value: String): String {
        if (value.length >= 2) {
            val head = value.first()
            if ((head == '"' || head == '\'') && value.last() == head) {
                return value.substring(1, value.length - 1)
            }
        }
        return value
    }
}
