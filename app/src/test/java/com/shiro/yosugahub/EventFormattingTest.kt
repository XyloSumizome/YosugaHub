package com.shiro.yosugahub

import com.shiro.yosugahub.data.calendar.EventFormatting
import com.shiro.yosugahub.data.local.db.CalendarBucket
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

class EventFormattingTest {

    private val today = LocalDate.of(2026, 7, 23)

    @Test
    fun formats_today_with_day_of_week() {
        assertEquals("2026-07-23 (木)", EventFormatting.formatToday(today, Locale.JAPANESE))
    }

    @Test
    fun today_events_show_time_only() {
        val start = LocalDateTime.of(2026, 7, 23, 14, 30)
        assertEquals("14:30", EventFormatting.formatEventTime(start, today, allDay = false))
    }

    @Test
    fun other_days_show_date_and_time() {
        val start = LocalDateTime.of(2026, 7, 26, 9, 5)
        assertEquals("07-26 09:05", EventFormatting.formatEventTime(start, today, allDay = false))
    }

    @Test
    fun all_day_events_show_date_only() {
        val start = LocalDateTime.of(2026, 7, 23, 0, 0)
        assertEquals("07-23", EventFormatting.formatEventTime(start, today, allDay = true))
    }

    @Test
    fun buckets_split_past_today_and_upcoming() {
        assertEquals(CalendarBucket.TODAY, EventFormatting.bucketOf(today, today))
        assertEquals(CalendarBucket.UPCOMING, EventFormatting.bucketOf(today.plusDays(1), today))
        assertEquals(CalendarBucket.PAST, EventFormatting.bucketOf(today.minusDays(1), today))
    }
}
