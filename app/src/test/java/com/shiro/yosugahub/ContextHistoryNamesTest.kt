package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.ContextHistoryNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextHistoryNamesTest {

    @Test
    fun accepts_both_formats() {
        assertTrue(ContextHistoryNames.isValidHistoryName("context_2026-07-24_150430.md"))
        assertTrue(ContextHistoryNames.isValidHistoryName("context_2026-07-24_150430.json"))
    }

    @Test
    fun rejects_names_that_could_escape_the_directory() {
        // ディレクトリ外を読ませないための番人
        assertFalse(ContextHistoryNames.isValidHistoryName("../secrets.md"))
        assertFalse(ContextHistoryNames.isValidHistoryName("context_2026-07-24_150430.md/../x"))
        assertFalse(ContextHistoryNames.isValidHistoryName("/etc/passwd"))
    }

    @Test
    fun rejects_other_shapes() {
        assertFalse(ContextHistoryNames.isValidHistoryName("context_2026-07-24.md"))
        assertFalse(ContextHistoryNames.isValidHistoryName("response_2026-07-24_150430.json"))
        assertFalse(ContextHistoryNames.isValidHistoryName("context_2026-07-24_150430.txt"))
        assertFalse(ContextHistoryNames.isValidHistoryName(""))
    }

    @Test
    fun formats_saved_at_for_display() {
        assertEquals("2026-07-24 15:04", ContextHistoryNames.formatSavedAt("context_2026-07-24_150430.md"))
        // 解釈できない名前は空文字(一覧では名前だけ見せる)
        assertEquals("", ContextHistoryNames.formatSavedAt("手で置いたファイル.md"))
    }

    @Test
    fun formats_the_format_label() {
        assertEquals("Markdown", ContextHistoryNames.formatLabel("context_2026-07-24_150430.md"))
        assertEquals("JSON", ContextHistoryNames.formatLabel("context_2026-07-24_150430.json"))
        assertEquals("", ContextHistoryNames.formatLabel("なにか.md"))
    }
}
