package com.shiro.yosugahub.ui.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/** ChatGPT アプリのパッケージ名。共有先を名指しするために使う。 */
const val CHATGPT_PACKAGE = "com.openai.chatgpt"

/**
 * ChatGPT アプリへ**直接**テキストを渡す(2026-07-26)。
 *
 * 朝の準備は渡す相手も中身も決まっている(ヨスガへ現況)。
 * 選択画面を出す意味が無いので、`setPackage` で名指しして1タップ減らす。
 *
 * ⚠ **URL の `?q=` は使えない。** 現況は 30KB を超えることがあり、
 * URL に載せると**黙って切り捨てられる**。テキストは `EXTRA_TEXT` で渡すこと。
 * こちらは Binder の上限(約1MB)まで通る。
 *
 * @return 渡せたら true。ChatGPT が入っていない・共有を受け付けない場合は false
 *   (呼び出し側で選択画面へ逃がす)。
 */
fun sendToChatGpt(context: Context, text: String, subject: String): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage(CHATGPT_PACKAGE)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return try {
        context.startActivity(send)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
