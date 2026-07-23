package com.shiro.yosugahub.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** 下部ナビゲーションの6画面(v3-Step 2-d で「記録」を追加) */
enum class YosugaDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "ホーム", Icons.Filled.Home),
    Calendar("calendar", "カレンダー", Icons.Filled.DateRange),
    Projects("projects", "プロジェクト", Icons.AutoMirrored.Filled.List),
    Records("records", "記録", Icons.Filled.Create),
    Assistant("assistant", "ヨスガ", Icons.AutoMirrored.Filled.Send),
    Settings("settings", "設定", Icons.Filled.Settings),
}
