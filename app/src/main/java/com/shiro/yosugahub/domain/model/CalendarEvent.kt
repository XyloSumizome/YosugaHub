package com.shiro.yosugahub.domain.model

/** カレンダー予定1件。Phase 4 で Google Calendar 由来のデータに置き換える。 */
data class CalendarEvent(
    val title: String,
    val start: String,
    val end: String,
    val calendarName: String,
    val description: String = "",
)
