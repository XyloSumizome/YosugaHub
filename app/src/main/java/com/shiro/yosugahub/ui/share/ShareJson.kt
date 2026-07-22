package com.shiro.yosugahub.ui.share

import android.content.Context
import android.content.Intent

/**
 * 状況JSNを Android 共有メニューでテキストとして送る(設計書2.3 / 4.2 の「共有する」)。
 * ファイル添付での共有は FileProvider 導入時に対応する。
 */
fun shareJsonText(context: Context, json: String, title: String = "状況JSONを共有") {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_TEXT, json)
    }
    context.startActivity(Intent.createChooser(send, title))
}
