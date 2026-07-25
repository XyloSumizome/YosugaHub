package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.ExternalLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 外部リンクの検証(2026-07-25 / レコルへのショートカット)。 */
class ExternalLinkTest {

    @Test
    fun accepts_https() {
        val url = "https://chatgpt.com/g/g-xxxx-recoru"
        assertEquals(url, ExternalLink.sanitize(url))
        assertTrue(ExternalLink.isValid(url))
    }

    @Test
    fun accepts_http() {
        assertEquals("http://example.com", ExternalLink.sanitize("http://example.com"))
    }

    @Test
    fun trims_surrounding_whitespace() {
        assertEquals("https://example.com", ExternalLink.sanitize("  https://example.com \n"))
    }

    /** file: や intent: を開かせない(貼り付け事故で意図しないものを開かないため)。 */
    @Test
    fun rejects_other_schemes() {
        assertNull(ExternalLink.sanitize("file:///sdcard/secret.txt"))
        assertNull(ExternalLink.sanitize("intent://evil#Intent;end"))
        assertNull(ExternalLink.sanitize("javascript:alert(1)"))
        assertNull(ExternalLink.sanitize("chatgpt.com/g/xxx"))
    }

    /** 説明文ごと貼り付けたものを URL として扱わない。 */
    @Test
    fun rejects_text_containing_spaces() {
        assertNull(ExternalLink.sanitize("レコルはこちら https://example.com"))
        assertNull(ExternalLink.sanitize("https://example.com を開く"))
    }

    @Test
    fun rejects_scheme_only_and_blank() {
        assertNull(ExternalLink.sanitize("https://"))
        assertNull(ExternalLink.sanitize(""))
        assertNull(ExternalLink.sanitize("   "))
        assertFalse(ExternalLink.isValid(" "))
    }
}
