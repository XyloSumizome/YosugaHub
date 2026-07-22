package com.shiro.yosugahub.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shiro.yosugahub.data.local.db.dao.CalendarEventDao
import com.shiro.yosugahub.data.local.db.dao.ProjectDao
import com.shiro.yosugahub.data.local.db.dao.RecommendationDao
import com.shiro.yosugahub.data.local.db.entity.CalendarEventEntity
import com.shiro.yosugahub.data.local.db.entity.ProjectEntity
import com.shiro.yosugahub.data.local.db.entity.RecommendationEntity

@Database(
    entities = [
        ProjectEntity::class,
        CalendarEventEntity::class,
        RecommendationEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class YosugaDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun recommendationDao(): RecommendationDao
}
