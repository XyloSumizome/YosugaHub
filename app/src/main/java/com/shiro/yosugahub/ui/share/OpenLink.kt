package com.shiro.yosugahub.ui.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * 外部リンクを開く(2026-07-25 / コンソールの OPEN RECORU)。
 *
 * 渡す URL は必ず `ExternalLink.sanitize` を通したものにする。
 * 開けるアプリが無い場合に落ちないよう false を返し、呼び出し側で伝える。
 */
fun openExternalLink(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (e: ActivityNotFoundException) {
    false
}
