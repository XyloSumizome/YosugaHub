package com.shiro.yosugahub.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 武骨なターミナル配色(v5 UI: Brutalist / Industrial)。
 * 派手なネオンではなく、開発現場の計器風に低彩度で締める。グラデーションは使わない。
 * アクセントは Amber / Green / Cyan / Red の4色のみ。
 */

// アクセント。**フォスファーグリーンが主役**の端末配色。
val TermGreen = Color(0xFF33DD6B)   // 主役。見出し・コマンド・値・LED
val TermAmber = Color(0xFFE0A62C)   // 警告・処理中(BUSY)
val TermCyan = Color(0xFF3EB8C7)    // 補助情報(控えめに使う)
val TermRed = Color(0xFFD65C5C)     // エラー・警報・破壊的操作

// 地(かなり暗い。真っ黒にはしない)
val TermBackground = Color(0xFF0A0C0E) // 画面の地
val TermPanel = Color(0xFF0D1013)      // パネルの中(地よりごく僅かに明るい)
val TermPanelHeader = Color(0xFF14181C) // パネルの見出し帯・浮いた面
val TermConsole = Color(0xFF07090B)    // ログコンソール(地より一段暗く)

// 線と文字。**白/灰は使わず、すべてグリーンのトーンで統一する。**
val TermLine = Color(0xFF1C2A20)       // 細いパネル枠・区切り(緑がかった暗線)
val TermLineActive = Color(0xFF2E4A38) // 選択・強調された枠
val TermText = Color(0xFF8FD0A2)       // 本文(1トーン落ち着いたグリーン)
val TermTextDim = Color(0xFF5C9370)    // 補足・ラベル・説明(さらに沈めたグリーン)

// アクセントの上に載せる濃色(反転ブロックの文字)
val OnTermGreen = Color(0xFF03130A)
val OnTermAmber = Color(0xFF1A1200)
val OnTermRed = Color(0xFF200304)
