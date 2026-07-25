package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.ClipboardPrefill
import org.junit.Assert.assertEquals
import org.junit.Test

/** クリップボードを貼り付け欄の初期値にしてよいかの判断(2026-07-25)。 */
class ClipboardPrefillTest {

    @Test
    fun fills_plain_json() {
        val json = """{"schemaVersion":2,"proposals":{}}"""
        assertEquals(json, ClipboardPrefill.of(json))
    }

    /** ヨスガが返す封筒なしの日記も対象(取り込み側が受け取れるため)。 */
    @Test
    fun fills_bare_diary() {
        val json = """{"diary":[{"date":"2026-07-20","body":"本文"}]}"""
        assertEquals(json, ClipboardPrefill.of(json))
    }

    /** 説明文が前後に付いた回答まるごとでも、JSON が取り出せるなら貼る。 */
    @Test
    fun fills_answer_with_surrounding_prose() {
        val shared = "巡回しました。\n```json\n{\"schemaVersion\":2}\n```\n- 分類: 0件"
        assertEquals(shared, ClipboardPrefill.of(shared))
    }

    /** 関係ない文字列は貼らない(消す手間を増やさない)。 */
    @Test
    fun does_not_fill_unrelated_text() {
        assertEquals("", ClipboardPrefill.of("明日の買い物リスト"))
        assertEquals("", ClipboardPrefill.of("https://example.com"))
    }

    @Test
    fun does_not_fill_empty_or_null() {
        assertEquals("", ClipboardPrefill.of(null))
        assertEquals("", ClipboardPrefill.of(""))
        assertEquals("", ClipboardPrefill.of("   \n "))
    }

    /** 壊れた JSON は貼る(直して使えるので、黙って捨てない)。 */
    @Test
    fun fills_broken_json_so_the_user_can_fix_it() {
        val broken = """{"schemaVersion":2,"proposals":"""
        assertEquals(broken, ClipboardPrefill.of(broken))
    }
}
