package com.shiro.yosugahub.ui.navigation

/**
 * 「Obsidianから文脈を作る」画面のルート(設計書v5 Phase 1-b)。
 *
 * 下部ナビは6個で埋まっているため7個目にはせず、
 * 出力先がヨスガである以上ヨスガ画面からのネストルートにする。
 */
object ObsidianContextRoute {
    const val PATTERN = "obsidian_context"
}
