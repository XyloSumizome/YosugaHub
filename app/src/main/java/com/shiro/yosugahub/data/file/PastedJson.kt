package com.shiro.yosugahub.data.file

/**
 * 貼り付け・共有された回答JSONの正規化(純粋ロジック)。
 *
 * 想定する入力は3通り:
 * 1. JSON そのもの(コードブロックの「コピー」ボタンで取った形)
 * 2. コードブロックの囲い付き(```json 〜 ```)
 * 3. **前後に説明文が付いた回答まるごと**(共有シートから来る形)
 *
 * 3 は AI が JSON の**後ろに実行報告や補足**を書くため、しばしば起きる。
 * そこで囲いの外し方を「先頭が ``` のときだけ」から
 * **文中から最初の JSON を抜き出す**方式へ広げた。JSONそのものには手を加えない。
 */
object PastedJson {

    private const val FENCE = "```"

    fun normalize(raw: String): String {
        val text = raw.trim()
        if (text.isEmpty()) return ""

        // 先頭が `{` なら、それ自体が JSON。**囲いを探しに行かない**。
        // 本文に ``` を含む JSON(指示書の body など)を壊さないための順序。
        if (text.startsWith("{")) return extractJsonObject(text) ?: text

        extractFencedBlock(text)?.let { return it.trim() }
        return extractJsonObject(text) ?: text
    }

    /**
     * 最初のコードブロックの中身を返す。閉じ忘れのコピーでも中身は残す。
     * 複数ある場合は **`{` で始まるものを優先**する(説明用のブロックを拾わないため)。
     * 囲いが1つも無ければ null。
     */
    private fun extractFencedBlock(text: String): String? {
        var first: String? = null
        var cursor = 0
        while (true) {
            val open = text.indexOf(FENCE, cursor)
            if (open < 0) return first
            // 開始行(``` または ```json 等の言語タグ)を読み飛ばす
            val bodyStart = text.indexOf('\n', open)
            if (bodyStart < 0) return first ?: ""
            val close = text.indexOf(FENCE, bodyStart)
            val body =
                if (close < 0) text.substring(bodyStart + 1)
                else text.substring(bodyStart + 1, close)
            if (body.trimStart().startsWith("{")) return body
            if (first == null) first = body
            if (close < 0) return first
            cursor = close + FENCE.length
        }
    }

    /**
     * 最初の `{` から**対応する** `}` までを返す。文字列リテラルの中の波括弧は数えない。
     * 閉じていなければ null(呼び出し側が原文のまま解析へ回し、エラーとして報告する)。
     */
    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null

        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }
}
