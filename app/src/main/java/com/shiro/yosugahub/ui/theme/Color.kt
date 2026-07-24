package com.shiro.yosugahub.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * サイバーパンク配色(v5 UI刷新)。ネオン系: シアン主体 + マゼンタのアクセント。
 * ダーク固定なのでライト用の色は持たない。
 */

// ネオン(前景アクセント)
val NeonCyan = Color(0xFF00E5FF)      // 主役。ボタン・見出し・端末ログ
val NeonMagenta = Color(0xFFFF2E97)   // 対になるアクセント。強調・選択状態
val NeonGreen = Color(0xFF39FF88)     // 成功・OK 行
val NeonAmber = Color(0xFFFFB74D)     // 警告
val NeonRed = Color(0xFFFF5370)       // エラー・破壊的操作

// 地(背景側)。真っ黒ではなく青みを残すと「夜の街」感が出る
val CyberBackground = Color(0xFF0A0E14) // 画面の地
val CyberSurface = Color(0xFF111826)    // カード
val CyberSurfaceHigh = Color(0xFF182233) // ダイアログ・浮いた面
val CyberTerminal = Color(0xFF060A10)   // 端末ログパネル(地より一段暗く)

// 文字
val CyberOnBackground = Color(0xFFD8E2F0) // 本文(白すぎない)
val CyberOnSurfaceVariant = Color(0xFF8FA3BF) // 補足・薄い文字
val CyberOutline = Color(0xFF2C3A52)    // 枠線・区切り

// ネオンの上に載せる濃色(ボタン文字など)
val OnNeonCyan = Color(0xFF00252B)
val OnNeonMagenta = Color(0xFF3A0019)
val OnNeonGreen = Color(0xFF00391B)
val OnNeonRed = Color(0xFF3F000E)

// コンテナ(チップ・トナール面)
val CyanContainer = Color(0xFF003D46)
val MagentaContainer = Color(0xFF4A0E2C)
