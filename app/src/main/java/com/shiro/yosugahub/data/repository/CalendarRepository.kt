package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.calendar.CalendarSyncResult
import com.shiro.yosugahub.data.calendar.DeviceCalendarDataSource
import com.shiro.yosugahub.data.calendar.EventFormatting
import com.shiro.yosugahub.data.local.db.CalendarBucket
import com.shiro.yosugahub.data.local.db.dao.CalendarEventDao
import com.shiro.yosugahub.data.local.db.toDomain
import com.shiro.yosugahub.domain.model.CalendarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * カレンダー予定の Repository。
 * 表示は Room のキャッシュから行い、`sync()` で端末カレンダー(CalendarContract)から洗い替える。
 * 取得に失敗しても既存のキャッシュは壊さない。
 */
class CalendarRepository(
    private val dao: CalendarEventDao,
    private val dataSource: DeviceCalendarDataSource? = null,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
) {

    /** 見出し用の今日の日付(端末時計から求める)。 */
    val today: String get() = EventFormatting.formatToday(todayProvider())

    fun todayEvents(): Flow<List<CalendarEvent>> = observeBucket(CalendarBucket.TODAY)

    fun upcomingEvents(): Flow<List<CalendarEvent>> = observeBucket(CalendarBucket.UPCOMING)

    fun pastEvents(): Flow<List<CalendarEvent>> = observeBucket(CalendarBucket.PAST)

    fun hasCalendarPermission(): Boolean = dataSource?.hasPermission() ?: false

    /**
     * 端末カレンダーから読み直してキャッシュを置き換える。
     * 権限がない・取得に失敗した場合はキャッシュを変更しない。
     */
    suspend fun sync(): CalendarSyncResult {
        val source = dataSource ?: return CalendarSyncResult.Failed("カレンダー取得元が未設定です")
        if (!source.hasPermission()) return CalendarSyncResult.PermissionDenied

        return source.loadEvents(today = todayProvider()).fold(
            onSuccess = { events ->
                dao.replaceAll(events)
                CalendarSyncResult.Success(events.size)
            },
            onFailure = { error ->
                CalendarSyncResult.Failed(error.message ?: "カレンダーを読み取れませんでした")
            },
        )
    }

    private fun observeBucket(bucket: String): Flow<List<CalendarEvent>> =
        dao.observeByBucket(bucket).map { events -> events.map { it.toDomain() } }
}
