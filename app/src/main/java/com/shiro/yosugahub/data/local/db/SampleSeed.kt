package com.shiro.yosugahub.data.local.db

import com.shiro.yosugahub.data.local.db.entity.CalendarEventEntity
import com.shiro.yosugahub.data.local.db.entity.DiaryEntryEntity
import com.shiro.yosugahub.data.local.db.entity.ItemEntityCrossRef
import com.shiro.yosugahub.data.local.db.entity.ItemTagCrossRef
import com.shiro.yosugahub.data.local.db.entity.KnowledgeItemEntity
import com.shiro.yosugahub.data.local.db.entity.ProjectEntity
import com.shiro.yosugahub.data.local.db.entity.RecommendationEntity
import com.shiro.yosugahub.data.local.db.entity.TagEntity
import com.shiro.yosugahub.data.local.db.entity.TaskEntity
import com.shiro.yosugahub.data.local.db.entity.TrackedEntityEntity

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

    /** 仮の情報アイテム(v3-Step 2)。承認フロー実装後は AI 提案由来の実データに置き換わる。 */
    val knowledgeItems: List<KnowledgeItemEntity> = listOf(
        KnowledgeItemEntity(
            id = "item-001",
            kind = "shopping",
            title = "USB-Cハブを購入",
            body = "展示会デモ用。HDMI出力付きのものを選ぶ。",
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
            source = "manual",
        ),
        KnowledgeItemEntity(
            id = "item-002",
            kind = "decision",
            title = "設計をv3(AIファースト)へ転換",
            body = "AI=頭脳 / Hub=記憶・表示装置の役割分担にする。",
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
            source = "manual",
        ),
        KnowledgeItemEntity(
            id = "item-003",
            kind = "idea",
            title = "ビート表示のアイデア",
            body = "リズムに合わせて進捗が脈打つ表示。今は採用しないが面白い。",
            createdAt = SEEDED_AT,
            updatedAt = SEEDED_AT,
            source = "manual",
        ),
    )

    val tags: List<TagEntity> = listOf(
        TagEntity(id = "tag-001", name = "買い物"),
        TagEntity(id = "tag-002", name = "展示会準備"),
        TagEntity(id = "tag-003", name = "Yosuga Hub"),
    )

    val itemTags: List<ItemTagCrossRef> = listOf(
        ItemTagCrossRef(itemId = "item-001", tagId = "tag-001"),
        ItemTagCrossRef(itemId = "item-001", tagId = "tag-002"),
        ItemTagCrossRef(itemId = "item-002", tagId = "tag-003"),
        ItemTagCrossRef(itemId = "item-003", tagId = "tag-003"),
    )

    val entities: List<TrackedEntityEntity> = listOf(
        TrackedEntityEntity(id = "entity-001", name = "東京ゲームショウ", type = "event"),
        TrackedEntityEntity(id = "entity-002", name = "Yosuga Hub", type = "project"),
    )

    val itemEntities: List<ItemEntityCrossRef> = listOf(
        ItemEntityCrossRef(itemId = "item-001", entityId = "entity-001"),
        ItemEntityCrossRef(itemId = "item-002", entityId = "entity-002"),
    )

    /** 仮の観察日記(Yosuga視点の文例)。 */
    val diaryEntries: List<DiaryEntryEntity> = listOf(
        DiaryEntryEntity(
            id = "diary-001",
            date = "2026-07-23",
            body = "今日はシロさんがYosuga Hubの設計を大きく前進させた。" +
                "タグの考え方が整理されて嬉しそうだった。" +
                "小さな出来事も、このプロジェクトの一部になっているように感じた。",
            createdAt = SEEDED_AT,
        ),
    )

    // --- 後片付け用の ID 一覧(SampleDataRepository が使う) ---
    // 「テーブルを空にする」のではなく ID を指定して消すため、実データを巻き込まない。

    val projectIds: List<String> get() = projects.map { it.id }
    val taskIds: List<String> get() = tasks.map { it.id }
    val itemIds: List<String> get() = knowledgeItems.map { it.id }
    val diaryIds: List<String> get() = diaryEntries.map { it.id }

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
