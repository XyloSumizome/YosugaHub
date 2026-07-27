package com.shiro.yosugahub.data.file

import java.net.URLEncoder

/**
 * ChatGPT を「言うことを添えて」開くための URL 組み立て(純粋ロジック)。
 *
 * 2026-07-26 に `RecoruLink` として作ったものを、レコル廃止(2026-07-27)に伴って
 * **ヨスガ用に転用**した。仕組み自体は正しく、捨てる理由が無い。
 *
 * ChatGPT は `?q=` に載せた文字列を**入力欄へ入れてくれる**。
 * ただし Android アプリでは**送信まではされない**(実機で確認)。
 * 完全自動にする手は無い——ChatGPT にアプリから送信させる公式な口が無いため。
 *
 * ⚠ **長い文字列は載せられない**([MAX_URL_LENGTH])。URL の長さ上限は経路ごとに違い、
 * 超えたときに**エラーではなく黙って切れる**。切れた指示文は一見それらしく見えるので、
 * 事故に気づけない。そこで**入り切らないと分かった時点で `?q=` を諦める**
 * (呼び出し側はクリップボードへ入れてある本文を貼ればよい)。
 * 日本語は URL エンコードで 1文字 9バイトになるため、入るのは実質 200 文字前後。
 */
object ChatGptLink {

    /** 会話URLが未設定のときに開く先。新しい会話になる。 */
    const val DEFAULT_URL = "https://chatgpt.com"

    /**
     * 安全側に倒した URL の長さ上限。
     *
     * 実際の上限は Android の Intent・ChatGPT アプリ・ブラウザで異なり、
     * どれが効くかは開いてみるまで分からない。**広く安全とされる 2000 文字**に揃える。
     */
    const val MAX_URL_LENGTH = 2000

    /**
     * [rawUrl] に [prompt] を `q` として足す。
     *
     * @return 組み立てた URL。[prompt] が長すぎて入らない場合は **`q` を付けない素の URL**
     *   を返す(切れた指示文を渡すより、何も渡さないほうが安全)。
     *   [rawUrl] が開けない文字列なら [DEFAULT_URL] に対して同じことをする。
     */
    fun withPrompt(rawUrl: String, prompt: String): String {
        val url = ExternalLink.sanitize(rawUrl) ?: DEFAULT_URL
        val text = prompt.trim()
        if (text.isEmpty()) return url

        // 断片(#…)より前に足さないと、q がフラグメントの一部として捨てられる。
        val fragmentAt = url.indexOf('#')
        val base = if (fragmentAt >= 0) url.substring(0, fragmentAt) else url
        val fragment = if (fragmentAt >= 0) url.substring(fragmentAt) else ""

        val separator = if (base.contains('?')) "&" else "?"
        val encoded = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
        val candidate = "$base${separator}q=$encoded$fragment"

        return if (candidate.length <= MAX_URL_LENGTH) candidate else url
    }

    /** [withPrompt] が `q` を載せられるか(呼び出し側の案内文を変えるため)。 */
    fun fitsInUrl(rawUrl: String, prompt: String): Boolean {
        val url = ExternalLink.sanitize(rawUrl) ?: DEFAULT_URL
        return withPrompt(url, prompt) != url
    }
}
