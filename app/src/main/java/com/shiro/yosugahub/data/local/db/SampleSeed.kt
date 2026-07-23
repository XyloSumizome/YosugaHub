package com.shiro.yosugahub.data.local.db

import com.shiro.yosugahub.data.local.db.entity.CalendarEventEntity
import com.shiro.yosugahub.data.local.db.entity.ProjectEntity
import com.shiro.yosugahub.data.local.db.entity.RecommendationEntity
import com.shiro.yosugahub.data.local.db.entity.TaskEntity

/**
 * DB を初回起動時にシードするための仮データ(Phase 1 の DummyData 相当)。
 * Phase 3(GitHub)/ Phase 4(Google Calendar)/ Phase 2(JSON取り込み)で
 * 実データに置き換わり、このシードは不要になる。
 */
object SampleSeed {

    val projects: List<ProjectEntity> = listOf(
        ProjectEntity(
            id = "anri",
            name = "ANRI",
            currentGoal = "プロトタイプの完成",
            inProgress = "メインシナリオ第2章の執筆",
            nextTask = "戦闘バランスの調整",
            lastUpdated = "2026-07-22 18:00",
            health = "on_track",
        ),
        ProjectEntity(
            id = "paper-armor-frog",
            name = "紙装甲主人公と不死身のカエル",
            currentGoal = "体験版の公開",
            inProgress = "カエルのアニメーション実装",
            nextTask = "ステージ2のレベルデザイン",
            lastUpdated = "2026-07-21 22:00",
            health = "attention",
        ),
        ProjectEntity(
            id = "gengenkyo",
            name = "げんげきょう",
            currentGoal = "世界観設定の確定",
            inProgress = "キャラクター設定資料の作成",
            nextTask = "序盤マップの下書き",
            lastUpdated = "2026-07-19 21:00",
            health = "paused",
        ),
    )

    val events: List<CalendarEventEntity> = listOf(
        CalendarEventEntity(bucket = CalendarBucket.TODAY, title = "歯医者", start = "10:00", end = "11:00", calendarName = "個人"),
        CalendarEventEntity(bucket = CalendarBucket.TODAY, title = "ANRI 作業時間", start = "14:00", end = "16:00", calendarName = "制作"),
        CalendarEventEntity(bucket = CalendarBucket.TODAY, title = "買い物", start = "18:00", end = "19:00", calendarName = "個人"),
        CalendarEventEntity(bucket = CalendarBucket.UPCOMING, title = "げんげきょう ミーティング", start = "07-24 20:00", end = "07-24 21:00", calendarName = "制作"),
        CalendarEventEntity(bucket = CalendarBucket.UPCOMING, title = "Pixel 10a 受け取り", start = "07-26 15:00", end = "07-26 15:30", calendarName = "個人"),
        CalendarEventEntity(bucket = CalendarBucket.UPCOMING, title = "カエル 進捗確認", start = "07-28 21:00", end = "07-28 22:00", calendarName = "制作"),
        CalendarEventEntity(bucket = CalendarBucket.PAST, title = "ANRI プロット見直し", start = "07-20 14:00", end = "07-20 16:00", calendarName = "制作"),
        CalendarEventEntity(bucket = CalendarBucket.PAST, title = "素材整理", start = "07-18 20:00", end = "07-18 21:00", calendarName = "制作"),
    )

    /** シード時刻(仮データなので固定値)。 */
    private const val SEEDED_AT = "2026-07-23T09:00:00+09:00"

    /**
     * 仮タスク(v3-Step 1)。既存プロジェクトの nextTask / inProgress 相当をタスク化し、
     * プロジェクト外タスク(projectId = null)と完了済みタスクも1件ずつ含める。
     */
    val tasks: List<TaskEntity> = listOf(
        TaskEntity(
            id = "task-anri-001",
            projectId = "anri",
            title = "戦闘バランスの調整",
            detail = "敵HPと与ダメージの係数を見直す",
            status = "todo",
            priority = "high",
            dueDate = "2026-07-26",
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
            completedAt = null,
            source = "manual",
        ),
        TaskEntity(
            id = "task-frog-001",
            projectId = "paper-armor-frog",
            title = "カエルのアニメーション実装",
            detail = "ジャンプと着地のモーションを仮素材で組む",
            status = "doing",
            priority = "medium",
            dueDate = null,
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
            completedAt = null,
            source = "manual",
        ),
        TaskEntity(
            id = "task-frog-002",
            projectId = "paper-armor-frog",
            title = "カエルの歩行モーション",
            detail = "",
            status = "done",
            priority = "medium",
            dueDate = null,
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
            completedAt = SEEDED_AT,
            source = "manual",
        ),
        TaskEntity(
            id = "task-gengenkyo-001",
            projectId = "gengenkyo",
            title = "序盤マップの下書き",
            detail = "",
            status = "todo",
            priority = "low",
            dueDate = null,
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
            completedAt = null,
            source = "manual",
        ),
        TaskEntity(
            id = "task-general-001",
            projectId = null,
            title = "TGS出展資料の下調べ",
            detail = "出展要項と締切を確認する",
            status = "todo",
            priority = "medium",
            dueDate = "2026-08-01",
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
            completedAt = null,
            source = "manual",
        ),
    )

    val recommendations: List<RecommendationEntity> = listOf(
        RecommendationEntity(
            projectId = "anri",
            title = "第2章の山場を先に固める",
            detail = "執筆が停滞したら、先に山場のシーンを書いてから前後を埋める方法を試す。",
            priority = "high",
        ),
        RecommendationEntity(
            projectId = "paper-armor-frog",
            title = "アニメーションは仮素材で進める",
            detail = "完成素材を待たず、仮素材でステージ2の検証を進めると全体が止まらない。",
            priority = "medium",
        ),
    )
}
