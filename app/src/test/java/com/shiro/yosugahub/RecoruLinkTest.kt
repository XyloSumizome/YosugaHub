package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.RecoruLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `?q=` は ChatGPT の入力欄を埋めるための口。
 * 組み立てを誤ると「開くが何も入っていない」という**気づきにくい壊れ方**をする。
 */
class RecoruLinkTest {

    private val gpt = "https://chatgpt.com/g/g-abc-rekoru"

    @Test
    fun `クエリの無いURLには疑問符で足す`() {
        assertEquals(
            "$gpt?q=patrol%20now",
            RecoruLink.withPrompt(gpt, "patrol now"),
        )
    }

    @Test
    fun `既にクエリがあるURLにはアンパサンドで足す`() {
        assertEquals(
            "$gpt?model=x&q=patrol%20now",
            RecoruLink.withPrompt("$gpt?model=x", "patrol now"),
        )
    }

    @Test
    fun `断片より前に足す`() {
        // # の後ろに付けると q がフラグメント扱いになり、静かに無視される。
        assertEquals(
            "$gpt?q=patrol%20now#top",
            RecoruLink.withPrompt("$gpt#top", "patrol now"),
        )
    }

    @Test
    fun `日本語のプロンプトをエンコードする`() {
        val url = RecoruLink.withPrompt(gpt, RecoruLink.PATROL)

        assertTrue(url!!.startsWith("$gpt?q=%"))
        assertTrue(url.none { it.code > 127 })
    }

    @Test
    fun `空のプロンプトなら素のURLを返す`() {
        assertEquals(gpt, RecoruLink.withPrompt(gpt, "   "))
    }

    @Test
    fun `開けないURLは null`() {
        // ExternalLink と同じ判定を通す(file: や空文字で開かない)。
        assertNull(RecoruLink.withPrompt("file:///etc/passwd", "patrol now"))
        assertNull(RecoruLink.withPrompt("", "patrol now"))
    }

    @Test
    fun `プラス記号ではなくパーセントで空白を表す`() {
        // URLEncoder の既定は + だが、URL のパスやクエリでは %20 のほうが安全に解釈される。
        val url = RecoruLink.withPrompt(gpt, "patrol now")

        assertTrue(url!!.contains("%20"))
        assertTrue(!url.contains("+"))
    }
}
