package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.db.CalendarBucket
import com.shiro.yosugahub.data.local.db.dao.CalendarEventDao
import com.shiro.yosugahub.data.local.db.toDomain
import com.shiro.yosugahub.domain.model.CalendarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * カレンダー予定の取得元を吸収する Repository。
 * 現状は Room(仮データでシード)。Phase 4 で Google Calendar データソースへ差し替える。
 */
class CalendarRepository(private val dao: CalendarEventDao) {

    /** 現在日付(暫定)。後で端末時計から求める。 */
    val today: String get() = PLACEHOLDER_TODAY

    /** 最終同期時刻(暫定)。後で DataStore から読む。 */
    val lastSyncedAt: String get() = PLACEHOLDER_LAST_SYNCED_AT

    fun todayEvents(): Flow<List<CalendarEvent>> = observeBucket(CalendarBucket.TODAY)

    fun upcomingEvents(): Flow<List<CalendarEvent>> = observeBucket(CalendarBucket.UPCOMING)

    fun pastEvents(): Flow<List<CalendarEvent>> = observeBucket(CalendarBucket.PAST)

    private fun observeBucket(bucket: String): Flow<List<CalendarEvent>> =
        dao.observeByBucket(bucket).map { events -> events.map { it.toDomain() } }

    private companion object {
        const val PLACEHOLDER_TODAY = "2026-07-22 (水)"
        const val PLACEHOLDER_LAST_SYNCED_AT = "2026-07-22 20:00"
    }
}
