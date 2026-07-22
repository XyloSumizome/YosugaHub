package com.shiro.yosugahub.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.shiro.yosugahub.data.local.db.entity.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {

    @Query("SELECT * FROM calendar_events WHERE bucket = :bucket ORDER BY uid")
    fun observeByBucket(bucket: String): Flow<List<CalendarEventEntity>>

    @Query("SELECT COUNT(*) FROM calendar_events")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(events: List<CalendarEventEntity>)
}
