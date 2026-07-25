package com.shiro.yosugahub

import android.content.Intent

/**
 * 「共有 → Yosuga Hub」で届いた本文を取り出す。
 *
 * 受けるのは `ACTION_SEND` の text 系 MIME だけ。ランチャーからの起動や、
 * 本文の無い共有では null を返す。**中身の検査はここではしない**
 * (レコルの回答かどうかは取り込み時に `ResponseImporter` が判定する)。
 *
 * 判定は [from] の素の値を取る版に置いてある。`Intent` は単体テストで動かないため、
 * ロジックだけを切り離してテストできるようにしている。
 */
object SharedText {

    fun from(intent: Intent?): String? {
        if (intent == null) return null
        return from(intent.action, intent.type, intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    /** [from] の中身(純粋ロジック)。 */
    fun from(action: String?, type: String?, text: String?): String? {
        if (action != Intent.ACTION_SEND) return null
        if (type?.startsWith("text/") != true) return null
        return text?.takeIf { it.isNotBlank() }
    }
}
