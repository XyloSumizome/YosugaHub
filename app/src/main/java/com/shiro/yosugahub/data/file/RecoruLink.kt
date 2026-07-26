package com.shiro.yosugahub.data.file

import java.net.URLEncoder

/**
 * レコルを「言うことを添えて」開くための URL 組み立て(純粋ロジック / 2026-07-26)。
 *
 * ChatGPT は `?q=` に載せた文字列を**入力欄へ入れてくれる**。
 * ただし Android アプリでは**送信まではされない**(実機で確認)。
 * つまり朝は「朝の準備 → 送信ボタン」の2タップになる。打つ手間はゼロにできる。
 *
 * 完全自動にする手はない。ChatGPT にアプリから送信させる公式な口が無いため、
 * ここで止まるのは仕様上の限界であって、作り込みの不足ではない。
 */
object RecoruLink {

    /**
     * [rawUrl] に [prompt] を `q` として足す。開けない URL なら null。
     *
     * 既にクエリが付いた URL を保存している場合もあるので、`?` と `&` を選び分ける。
     * [prompt] が空なら素の URL を返す(余計な `?q=` を付けない)。
     */
    fun withPrompt(rawUrl: String, prompt: String): String? {
        val url = ExternalLink.sanitize(rawUrl) ?: return null
        val text = prompt.trim()
        if (text.isEmpty()) return url

        // 断片(#…)より前に足さないと、q がフラグメントの一部として捨てられる。
        val fragmentAt = url.indexOf('#')
        val base = if (fragmentAt >= 0) url.substring(0, fragmentAt) else url
        val fragment = if (fragmentAt >= 0) url.substring(fragmentAt) else ""

        val separator = if (base.contains('?')) "&" else "?"
        val encoded = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
        return "$base${separator}q=$encoded$fragment"
    }

    /**
     * レコルへ渡す言葉。
     *
     * **朝の近況はレコルを通さない**(2026-07-26)。現況JSONが Morning Brief の
     * 材料をすべて持っており、間に要約を挟む必要がないと分かったため。
     * レコルに残るのは、記録タブに手打ちした未整理メモの仕分けだけ。
     */
    const val PATROL = "巡回"
}
