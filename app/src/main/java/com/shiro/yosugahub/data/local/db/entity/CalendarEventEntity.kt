package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * calendar_events テーブル。
 * bucket で「今日 / 今後7日 / 過去7日」を区別する(Phase 4 で実日時からの導出に置き換える)。
 */
@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val bucket: String,
    val title: String,
    val start: String,
    val end: String,
    val calendarName: String,
    val description: String = "",
)
