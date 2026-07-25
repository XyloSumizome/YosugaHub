package com.shiro.yosugahub.data.file

/**
 * クリップボードの中身を貼り付け欄の初期値にしてよいか判断する(純粋ロジック / 2026-07-25)。
 *
 * **何でも貼らない。** 無関係な文字列を入れると、消す手間のほうが増える。
 * 通すのは [PastedJson] で正規化した結果が JSON オブジェクトに見えるものだけ
 * (説明文が前後に付いた回答まるごとでも、JSON が取り出せれば通る)。
 *
 * ⚠ クリップボードを読むこと自体は Android 12 以降でシステムのトーストが出る。
 * これは抑制できないので、**読むのはユーザーが取り込みを始めたときだけ**にする。
 */
object ClipboardPrefill {

    /** 貼ってよければその文字列、そうでなければ空文字。 */
    fun of(clipboard: String?): String {
        val raw = clipboard ?: return ""
        if (raw.isBlank()) return ""
        val normalized = PastedJson.normalize(raw)
        // 正規化しても JSON にならないもの(ただの会話文など)は貼らない。
        if (!normalized.startsWith("{")) return ""
        return raw
    }
}
