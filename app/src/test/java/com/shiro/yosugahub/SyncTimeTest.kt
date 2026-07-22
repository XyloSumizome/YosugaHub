package com.shiro.yosugahub

import com.shiro.yosugahub.util.formatSyncTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class SyncTimeTest {

    @Test
    fun formats_datetime_as_yyyy_mm_dd_hh_mm() {
        val dateTime = LocalDateTime.of(2026, 7, 22, 20, 5)
        assertEquals("2026-07-22 20:05", formatSyncTime(dateTime))
    }
}
