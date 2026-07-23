package com.shiro.yosugahub

import com.shiro.yosugahub.ui.screen.projectdetail.dueDateForSave
import com.shiro.yosugahub.ui.screen.projectdetail.isValidDueDateInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskFormTest {

    @Test
    fun blank_due_date_is_valid_as_no_deadline() {
        assertTrue(isValidDueDateInput(""))
        assertTrue(isValidDueDateInput("   "))
    }

    @Test
    fun iso_date_is_valid_including_surrounding_spaces() {
        assertTrue(isValidDueDateInput("2026-07-31"))
        assertTrue(isValidDueDateInput(" 2026-07-31 "))
    }

    @Test
    fun malformed_or_impossible_dates_are_invalid() {
        assertFalse(isValidDueDateInput("2026/07/31"))
        assertFalse(isValidDueDateInput("7月31日"))
        assertFalse(isValidDueDateInput("2026-13-01"))
        assertFalse(isValidDueDateInput("2026-02-30"))
    }

    @Test
    fun due_date_for_save_converts_blank_to_null_and_trims() {
        assertNull(dueDateForSave(""))
        assertNull(dueDateForSave("  "))
        assertEquals("2026-07-31", dueDateForSave(" 2026-07-31 "))
    }
}
