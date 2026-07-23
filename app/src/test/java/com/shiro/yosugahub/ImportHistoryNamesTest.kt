package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.ImportHistoryNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 取り込み履歴のファイル名の解釈と検証。 */
class ImportHistoryNamesTest {

    @Test
    fun formats_saved_time_from_file_name() {
        assertEquals(
            "2026-07-23 15:04",
            ImportHistoryNames.formatSavedAt("response_2026-07-23_150432.json"),
        )
    }

    @Test
    fun unknown_name_yields_empty_time_instead_of_crashing() {
        assertEquals("", ImportHistoryNames.formatSavedAt("手で置いたファイル.json"))
        assertEquals("", ImportHistoryNames.formatSavedAt("response_2026-07-23.json"))
        assertEquals("", ImportHistoryNames.formatSavedAt(""))
    }

    @Test
    fun accepts_only_the_expected_history_name() {
        assertTrue(ImportHistoryNames.isValidHistoryName("response_2026-07-23_150432.json"))
        assertFalse(ImportHistoryNames.isValidHistoryName("response_2026-07-23_1504.json"))
        assertFalse(ImportHistoryNames.isValidHistoryName("other.json"))
    }

    /** imports/ の外を読ませないための番人として働くこと。 */
    @Test
    fun rejects_path_traversal_attempts() {
        assertFalse(ImportHistoryNames.isValidHistoryName("../secrets.json"))
        assertFalse(ImportHistoryNames.isValidHistoryName("../../response_2026-07-23_150432.json"))
        assertFalse(ImportHistoryNames.isValidHistoryName("imports/response_2026-07-23_150432.json"))
        // 改行を挟んで正規表現を回避しようとするパターン(Regex.matches は複数行でも全体一致)
        assertFalse(ImportHistoryNames.isValidHistoryName("x\nresponse_2026-07-23_150432.json"))
    }

    /** 名前の降順 = 時系列の降順(一覧の並びがこの性質に依存している)。 */
    @Test
    fun descending_name_order_matches_reverse_chronological_order() {
        val names = listOf(
            "response_2026-07-23_090000.json",
            "response_2026-07-23_150432.json",
            "response_2026-07-22_235959.json",
        )
        assertEquals(
            listOf(
                "response_2026-07-23_150432.json",
                "response_2026-07-23_090000.json",
                "response_2026-07-22_235959.json",
            ),
            names.sortedDescending(),
        )
    }
}
