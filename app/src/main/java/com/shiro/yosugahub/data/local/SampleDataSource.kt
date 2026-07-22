package com.shiro.yosugahub.data.local

import com.shiro.yosugahub.domain.model.CalendarEvent
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.Recommendation

/**
 * Phase 1〜2 の仮データを保持するインメモリのデータソース。
 * Phase 2 以降で Room / DataStore / 外部API のデータソースへ差し替える。
 * Repository はこのクラスを内部で参照し、UI からは直接触れない。
 */
class SampleDataSource {

    /** 現在日付の暫定値。後で端末時計 (LocalDate.now) から求める。 */
    val today: String = "2026-07-22 (水)"

    /** 最終同期時刻の暫定値。後で DataStore から読む。 */
    val lastSyncedAt: String = "2026-07-22 20:00"

    /** 優先タスクの暫定値。後で提案・進捗から導出する。 */
    val priorityTask: String = "ANRI: メインシナリオ第2章の執筆を進める"

    val todayEvents: List<CalendarEvent> = listOf(
        CalendarEvent("歯医者", "10:00", "11:00", "個人"),
        CalendarEvent("ANRI 作業時間", "14:00", "16:00", "制作"),
        CalendarEvent("買い物", "18:00", "19:00", "個人"),
    )

    val upcomingEvents: List<CalendarEvent> = listOf(
        CalendarEvent("げんげきょう ミーティング", "07-24 20:00", "07-24 21:00", "制作"),
        CalendarEvent("Pixel 10a 受け取り", "07-26 15:00", "07-26 15:30", "個人"),
        CalendarEvent("カエル 進捗確認", "07-28 21:00", "07-28 22:00", "制作"),
    )

    val pastEvents: List<CalendarEvent> = listOf(
        CalendarEvent("ANRI プロット見直し", "07-20 14:00", "07-20 16:00", "制作"),
        CalendarEvent("素材整理", "07-18 20:00", "07-18 21:00", "制作"),
    )

    val projects: List<Project> = listOf(
        Project(
            id = "anri",
            name = "ANRI",
            currentGoal = "プロトタイプの完成",
            inProgress = "メインシナリオ第2章の執筆",
            nextTask = "戦闘バランスの調整",
            lastUpdated = "2026-07-22 18:00",
            health = "on_track",
        ),
        Project(
            id = "paper-armor-frog",
            name = "紙装甲主人公と不死身のカエル",
            currentGoal = "体験版の公開",
            inProgress = "カエルのアニメーション実装",
            nextTask = "ステージ2のレベルデザイン",
            lastUpdated = "2026-07-21 22:00",
            health = "attention",
        ),
        Project(
            id = "gengenkyo",
            name = "げんげきょう",
            currentGoal = "世界観設定の確定",
            inProgress = "キャラクター設定資料の作成",
            nextTask = "序盤マップの下書き",
            lastUpdated = "2026-07-19 21:00",
            health = "paused",
        ),
    )

    val recommendations: List<Recommendation> = listOf(
        Recommendation(
            projectId = "anri",
            title = "第2章の山場を先に固める",
            detail = "執筆が停滞したら、先に山場のシーンを書いてから前後を埋める方法を試す。",
            priority = "high",
        ),
        Recommendation(
            projectId = "paper-armor-frog",
            title = "アニメーションは仮素材で進める",
            detail = "完成素材を待たず、仮素材でステージ2の検証を進めると全体が止まらない。",
            priority = "medium",
        ),
    )
}
