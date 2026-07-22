package com.shiro.yosugahub.data.model

/**
 * Phase 1 用の仮データ。
 * Phase 2 以降で Room / DataStore / 外部API に置き換える。
 */
data class DummyEvent(
    val title: String,
    val start: String,
    val end: String,
    val calendarName: String,
    val description: String = "",
)

data class DummyProject(
    val id: String,
    val name: String,
    val currentGoal: String,
    val inProgress: String,
    val nextTask: String,
    val lastUpdated: String,
    val health: String,
)

data class DummyRecommendation(
    val projectId: String,
    val title: String,
    val detail: String,
    val priority: String,
)

object DummyData {

    const val today = "2026-07-22 (水)"
    const val lastSyncedAt = "2026-07-22 20:00"

    val todayEvents = listOf(
        DummyEvent("歯医者", "10:00", "11:00", "個人"),
        DummyEvent("ANRI 作業時間", "14:00", "16:00", "制作"),
        DummyEvent("買い物", "18:00", "19:00", "個人"),
    )

    val upcomingEvents = listOf(
        DummyEvent("げんげきょう ミーティング", "07-24 20:00", "07-24 21:00", "制作"),
        DummyEvent("Pixel 10a 受け取り", "07-26 15:00", "07-26 15:30", "個人"),
        DummyEvent("カエル 進捗確認", "07-28 21:00", "07-28 22:00", "制作"),
    )

    val pastEvents = listOf(
        DummyEvent("ANRI プロット見直し", "07-20 14:00", "07-20 16:00", "制作"),
        DummyEvent("素材整理", "07-18 20:00", "07-18 21:00", "制作"),
    )

    val projects = listOf(
        DummyProject(
            id = "anri",
            name = "ANRI",
            currentGoal = "プロトタイプの完成",
            inProgress = "メインシナリオ第2章の執筆",
            nextTask = "戦闘バランスの調整",
            lastUpdated = "2026-07-22 18:00",
            health = "on_track",
        ),
        DummyProject(
            id = "paper-armor-frog",
            name = "紙装甲主人公と不死身のカエル",
            currentGoal = "体験版の公開",
            inProgress = "カエルのアニメーション実装",
            nextTask = "ステージ2のレベルデザイン",
            lastUpdated = "2026-07-21 22:00",
            health = "attention",
        ),
        DummyProject(
            id = "gengenkyo",
            name = "げんげきょう",
            currentGoal = "世界観設定の確定",
            inProgress = "キャラクター設定資料の作成",
            nextTask = "序盤マップの下書き",
            lastUpdated = "2026-07-19 21:00",
            health = "paused",
        ),
    )

    val recommendations = listOf(
        DummyRecommendation(
            projectId = "anri",
            title = "第2章の山場を先に固める",
            detail = "執筆が停滞したら、先に山場のシーンを書いてから前後を埋める方法を試す。",
            priority = "high",
        ),
        DummyRecommendation(
            projectId = "paper-armor-frog",
            title = "アニメーションは仮素材で進める",
            detail = "完成素材を待たず、仮素材でステージ2の検証を進めると全体が止まらない。",
            priority = "medium",
        ),
    )

    val priorityTask = "ANRI: メインシナリオ第2章の執筆を進める"
}
