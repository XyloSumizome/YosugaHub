package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.SampleDataSource
import com.shiro.yosugahub.domain.model.CalendarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * カレンダー予定の取得元を吸収する Repository。
 * 現状はインメモリの仮データ。Phase 4 で Google Calendar データソースへ差し替える。
 */
class CalendarRepository(private val source: SampleDataSource) {

    /** 現在日付(暫定)。後で端末時計から求める。 */
    val today: String get() = source.today

    /** 最終同期時刻(暫定)。後で DataStore から読む。 */
    val lastSyncedAt: String get() = source.lastSyncedAt

    fun todayEvents(): Flow<List<CalendarEvent>> = flowOf(source.todayEvents)

    fun upcomingEvents(): Flow<List<CalendarEvent>> = flowOf(source.upcomingEvents)

    fun pastEvents(): Flow<List<CalendarEvent>> = flowOf(source.pastEvents)
}
