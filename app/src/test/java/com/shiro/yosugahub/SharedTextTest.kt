package com.shiro.yosugahub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 「共有 → Yosuga Hub」で受ける条件(v5 / 2026-07-25)。 */
class SharedTextTest {

    private val send = "android.intent.action.SEND"

    @Test
    fun accepts_plain_text_share() {
        assertEquals("""{"schemaVersion": 2}""", SharedText.from(send, "text/plain", """{"schemaVersion": 2}"""))
    }

    /** ChatGPT が text/plain 以外の text 系 MIME で共有してきても受ける。 */
    @Test
    fun accepts_other_text_types() {
        assertEquals("hi", SharedText.from(send, "text/markdown", "hi"))
    }

    @Test
    fun ignores_non_text_share() {
        assertNull(SharedText.from(send, "image/png", "hi"))
        assertNull(SharedText.from(send, null, "hi"))
    }

    /** ランチャーからの通常起動で取り込みダイアログが出てはいけない。 */
    @Test
    fun ignores_launcher_start() {
        assertNull(SharedText.from("android.intent.action.MAIN", null, null))
        assertNull(SharedText.from(null, "text/plain", "hi"))
    }

    /** 複数共有(SEND_MULTIPLE)は受けない。回答JSONは1件で来る。 */
    @Test
    fun ignores_send_multiple() {
        assertNull(SharedText.from("android.intent.action.SEND_MULTIPLE", "text/plain", "hi"))
    }

    @Test
    fun ignores_empty_body() {
        assertNull(SharedText.from(send, "text/plain", null))
        assertNull(SharedText.from(send, "text/plain", "   \n "))
    }
}
