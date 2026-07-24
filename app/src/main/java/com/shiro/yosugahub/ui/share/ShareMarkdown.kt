package com.shiro.yosugahub.ui.share

import android.content.Context
import android.content.Intent

/**
 * コンテキスト Markdown を Android の共有メニューへ流す(設計書v5 Phase 1-c)。
 * ChatGPT アプリなど、テキストを受け取れる相手へ直接渡すための経路。
 */
fun shareMarkdownText(
    context: Context,
    markdown: String,
    subject: String,
    title: String = "コンテキストを共有",
) {
    val send = Intent(Intent.ACTION_SEND).apply {
        // text/markdown を受け付けないアプリが多いため text/plain で送る。
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, markdown)
    }
    context.startActivity(Intent.createChooser(send, title))
}
