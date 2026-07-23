package com.shiro.yosugahub.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shiro.yosugahub.data.local.db.dao.CalendarEventDao
import com.shiro.yosugahub.data.local.db.dao.ProjectDao
import com.shiro.yosugahub.data.local.db.dao.RecommendationDao
import com.shiro.yosugahub.data.local.db.dao.TaskDao
import com.shiro.yosugahub.data.local.db.entity.CalendarEventEntity
import com.shiro.yosugahub.data.local.db.entity.ProjectEntity
import com.shiro.yosugahub.data.local.db.entity.RecommendationEntity
import com.shiro.yosugahub.data.local.db.entity.TaskEntity

/**
 * v2: tasks テーブルを追加(v3-Step 1)。マイグレーションは Migrations.kt。
 * exportSchema=true: スキーマJSONを app/schemas/ へ出力し、以後の変更を検証可能にする。
 */
@Database(
    entities = [
        ProjectEntity::class,
        CalendarEventEntity::class,
        RecommendationEntity::class,
        TaskEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class YosugaDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun taskDao(): TaskDao
}
