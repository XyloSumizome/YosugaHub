package com.shiro.yosugahub.data.file

/**
 * 外から開くリンクの検証(純粋ロジック / 2026-07-25)。
 *
 * レコル(カスタムGPT)へ一発で飛ぶために URL を保存するが、
 * **保存された文字列をそのまま `ACTION_VIEW` へ渡さない**。
 * 打ち間違いや貼り付け事故で `file:` や `intent:` のような
 * 別のスキームが入ると、意図しないものを開いてしまうため。
 * 通すのは **http / https だけ**。
 */
object ExternalLink {

    private val ALLOWED = listOf("https://", "http://")

    /** 開いてよい URL なら整形して返す。そうでなければ null。 */
    fun sanitize(raw: String): String? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        // 空白を含むものは URL ではない(貼り付け時に説明文が混ざった等)。
        if (text.any { it.isWhitespace() }) return null
        val scheme = ALLOWED.firstOrNull { text.startsWith(it, ignoreCase = true) } ?: return null
        // スキームだけで中身が無いものは開かない。
        if (text.length <= scheme.length) return null
        return text
    }

    fun isValid(raw: String): Boolean = sanitize(raw) != null
}
