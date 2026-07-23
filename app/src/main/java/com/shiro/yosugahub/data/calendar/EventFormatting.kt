package com.shiro.yosugahub.data.calendar

import com.shiro.yosugahub.data.local.db.CalendarBucket
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * カレンダー表示の整形と区分判定(純粋ロジック、ユニットテスト可能)。
 * 端末のタイムゾーンで解決済みの LocalDateTime を受け取る。
 */
object EventFormatting {

    private val TIME_ONLY: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    private val DATE_ONLY: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd")

    /** ホーム見出し用の日付(例: 2026-07-23 (木))。 */
    fun formatToday(date: LocalDate, locale: Locale = Locale.JAPANESE): String {
        val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        return "$date ($dayOfWeek)"
    }

    /**
     * 予定の表示時刻。今日の予定は時刻のみ、それ以外は日付+時刻にして一覧で区別しやすくする。
     * 終日予定は時刻を出さない。
     */
    fun formatEventTime(dateTime: LocalDateTime, today: LocalDate, allDay: Boolean): String = when {
        allDay -> dateTime.toLocalDate().format(DATE_ONLY)
        dateTime.toLocalDate() == today -> dateTime.format(TIME_ONLY)
        else -> dateTime.format(DATE_TIME)
    }

    /** 開始日から今日・今後・過去のどれに入れるかを決める。 */
    fun bucketOf(startDate: LocalDate, today: LocalDate): String = when {
        startDate.isEqual(today) -> CalendarBucket.TODAY
        startDate.isAfter(today) -> CalendarBucket.UPCOMING
        else -> CalendarBucket.PAST
    }
}
