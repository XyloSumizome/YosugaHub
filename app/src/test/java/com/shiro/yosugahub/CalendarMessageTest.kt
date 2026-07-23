package com.shiro.yosugahub

import com.shiro.yosugahub.data.calendar.CalendarSyncResult
import com.shiro.yosugahub.ui.share.calendarSyncMessage
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarMessageTest {

    @Test
    fun success_reports_count() {
        assertTrue(calendarSyncMessage(CalendarSyncResult.Success(5)).contains("5"))
    }

    @Test
    fun zero_events_is_distinguished_from_failure() {
        val message = calendarSyncMessage(CalendarSyncResult.Success(0))
        assertTrue(message.contains("見つかりませんでした"))
    }

    @Test
    fun permission_denied_tells_user_what_to_do() {
        val message = calendarSyncMessage(CalendarSyncResult.PermissionDenied)
        assertTrue(message.contains("設定"))
    }

    @Test
    fun failure_message_is_not_technical() {
        val message = calendarSyncMessage(CalendarSyncResult.Failed("SQLiteException: boom"))
        assertTrue(!message.contains("SQLiteException"))
    }
}
