package com.shiro.yosugahub.ui.component

/** プロジェクト状態(health)の表示ラベル。一覧と詳細で共用する。 */
fun healthLabel(health: String): String = when (health) {
    "on_track" -> "順調"
    "attention" -> "要確認"
    "blocked" -> "停滞"
    "paused" -> "休止中"
    else -> health
}
