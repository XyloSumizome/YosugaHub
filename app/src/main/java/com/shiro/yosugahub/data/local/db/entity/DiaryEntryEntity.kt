package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** diary_entries テーブル(Yosuga視点の観察日誌)。date で時系列参照する。 */
@Entity(
    tableName = "diary_entries",
    indices = [Index("date")],
)
data class DiaryEntryEntity(
    @PrimaryKey val id: String,
    val date: String,       // "yyyy-MM-dd"
    val body: String,
    val createdAt: String,  // ISO 8601
)
