package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.ChatGptLink
import com.shiro.yosugahub.data.prompt.YosugaPrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ヨスガの会話を「言うことを添えて」開く URL(2026-07-27)。
 * 2026-07-26 の `RecoruLinkTest` を、レコル廃止に伴って引き継いだもの。
 */
class ChatGptLinkTest {

    private val conversation = "https://chatgpt.com/c/abc123"

    @Test
    fun adds_prompt_as_query() {
        assertEquals(
            "$conversation?q=%E5%B7%A1%E5%9B%9E",
            ChatGptLink.withPrompt(conversation, "巡回"),
        )
    }

    @Test
    fun uses_ampersand_when_query_already_present() {
        val url = "https://chatgpt.com/c/abc123?model=gpt"
        assertTrue(ChatGptLink.withPrompt(url, "hi").startsWith("$url&q="))
    }

    /** q がフラグメントの一部として捨てられないよう、`#` の前に足す。 */
    @Test
    fun inserts_before_fragment() {
        assertEquals(
            "https://chatgpt.com/c/abc?q=hi#top",
            ChatGptLink.withPrompt("https://chatgpt.com/c/abc#top", "hi"),
        )
    }

    @Test
    fun blank_prompt_leaves_url_untouched() {
        assertEquals(conversation, ChatGptLink.withPrompt(conversation, "   "))
    }

    /** 未設定でも動く。新しい会話になるだけで、押して何も起きない口は作らない。 */
    @Test
    fun falls_back_to_default_url_when_unset() {
        assertTrue(ChatGptLink.withPrompt("", "hi").startsWith(ChatGptLink.DEFAULT_URL))
        assertTrue(ChatGptLink.withPrompt("javascript:alert(1)", "hi").startsWith(ChatGptLink.DEFAULT_URL))
    }

    /**
     * ⚠ **切れた指示文を渡さない。** URL の長さ上限を超えたときは
     * エラーではなく黙って切れるため、一見それらしい指示が届いて事故に気づけない。
     * 入り切らないと分かった時点で `q` を諦める。
     */
    @Test
    fun drops_query_when_too_long_instead_of_truncating() {
        val long = "あ".repeat(1000)
        val url = ChatGptLink.withPrompt(conversation, long)

        assertEquals(conversation, url)
        assertFalse(ChatGptLink.fitsInUrl(conversation, long))
    }

    @Test
    fun stays_within_the_length_cap_when_it_fits() {
        val url = ChatGptLink.withPrompt(conversation, "今日の観測日記")

        assertTrue(url.length <= ChatGptLink.MAX_URL_LENGTH)
        assertTrue(ChatGptLink.fitsInUrl(conversation, "今日の観測日記"))
    }

    /**
     * **実際の指示文は `?q=` に載らない**(2026-07-27 に実測)。
     * 日本語は URL エンコードで 1文字 9バイトになるため、数百字の指示文は数千字になる。
     * この事実が変わったら(指示文を大幅に削ったら)、コンソールの案内文と
     * `askYosuga` の Toast を見直すこと——「コピーしました」で止まっているので。
     */
    @Test
    fun real_prompts_do_not_fit_and_therefore_rely_on_the_clipboard() {
        assertFalse(ChatGptLink.fitsInUrl(conversation, YosugaPrompt.diary("2026-07-27")))
        assertFalse(ChatGptLink.fitsInUrl(conversation, YosugaPrompt.session("2026-07-27")))
        assertFalse(ChatGptLink.fitsInUrl(conversation, YosugaPrompt.MORNING_HEADER))
    }
}
