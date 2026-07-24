package com.shiro.yosugahub.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 武骨なターミナル配色(v5 UI: Brutalist / Industrial)。
 * 派手なネオンではなく、開発現場の計器風に低彩度で締める。グラデーションは使わない。
 * アクセントは Amber / Green / Cyan / Red の4色のみ。
 */

// アクセント(計器の指示色。彩度は抑えめ)
val TermAmber = Color(0xFFE0A62C)   // 主役。見出し・主要ボタン
val TermCyan = Color(0xFF3EB8C7)    // 値・リンク・情報
val TermGreen = Color(0xFF4CB86A)   // 正常・OK・LED点灯
val TermRed = Color(0xFFD65C5C)     // エラー・警報・破壊的操作

// 地(かなり暗い。真っ黒にはしない)
val TermBackground = Color(0xFF0A0C0E) // 画面の地
val TermPanel = Color(0xFF0D1013)      // パネルの中(地よりごく僅かに明るい)
val TermPanelHeader = Color(0xFF14181C) // パネルの見出し帯・浮いた面
val TermConsole = Color(0xFF07090B)    // ログコンソール(地より一段暗く)

// 線と文字
val TermLine = Color(0xFF23292F)       // 細いパネル枠・区切り
val TermLineActive = Color(0xFF35414A) // 選択・強調された枠
val TermText = Color(0xFFBFC7CE)       // 本文(白すぎない灰)
val TermTextDim = Color(0xFF6C7783)    // 補足・ラベルの薄い文字

// アクセントの上に載せる濃色(塗りボタンの文字)
val OnTermAmber = Color(0xFF1A1200)
val OnTermRed = Color(0xFF200304)
