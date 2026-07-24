package com.shiro.yosugahub.ui.navigation

/**
 * 画面ルート(v5 UI: 下部ナビを廃止)。
 * ルートは Console。他はコンソールから開くサブ画面で、アイコンやラベルは持たない。
 */
enum class YosugaDestination(val route: String) {
    Console("console"),
    Projects("projects"),
    Calendar("calendar"),
    Records("records"),
    Settings("settings"),
    /** 承認待ちの提案レビュー(旧ヨスガ画面)。 */
    Review("review"),
}
