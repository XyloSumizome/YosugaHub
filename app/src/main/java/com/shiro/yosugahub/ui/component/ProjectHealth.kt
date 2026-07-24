package com.shiro.yosugahub.ui.component

/** プロジェクト状態(health)の表示ラベル。一覧と詳細で共用する。 */
fun healthLabel(health: String): String = when (health) {
    "on_track" -> "順調"
    "attention" -> "要確認"
    "blocked" -> "停滞"
    "paused" -> "休止中"
    else -> health
}

/**
 * 「作業中」の1行表示。A運用ではローカルの作業中が空のことが多いので、
 * 空なら `作業中: -` にして「作業中: 」(後ろが空)という壊れた見た目を避ける。
 * 一覧・ホームで共用する。
 */
fun inProgressLine(inProgress: String): String =
    if (inProgress.isBlank()) "作業中: -" else "作業中: $inProgress"
