package com.shiro.yosugahub.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAll()

    /** 端末カレンダーの取得結果で丸ごと置き換える(同期は常に全件洗い替え)。 */
    @Transaction
    suspend fun replaceAll(events: List<CalendarEventEntity>) {
        deleteAll()
        insertAll(events)
    }
}
