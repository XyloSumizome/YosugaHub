package com.shiro.yosugahub

import com.shiro.yosugahub.data.prompt.YosugaPrompt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ヨスガへ同梱する指示文(2026-07-27)。
 *
 * ここで守りたいのは**文面の good/bad ではなく、壊れると気づけない性質**:
 * - 日付が本当に埋まっているか(埋まっていないと毎回「今日は何日?」から始まる)
 * - 受け取り側が弾かない鍵(`schemaVersion` / `proposals`)を書いているか
 * - 日記とセッション記録が**混ざっていない**か(混ぜると両方の精度が落ちる)
 */
class YosugaPromptTest {

    @Test
    fun morning_header_is_prepended_not_appended() {
        val json = """{"generatedAt":"2026-07-27T08:00:00+09:00"}"""
        val payload = YosugaPrompt.withMorningHeader(json)

        assertTrue(payload.startsWith(YosugaPrompt.MORNING_HEADER))
        assertTrue(payload.endsWith(json))
    }

    /** 現況の読み方を書いていないと、全部読み上げるか頭だけ見て終わるかになる。 */
    @Test
    fun morning_header_names_the_fields_it_expects() {
        val header = YosugaPrompt.MORNING_HEADER
        listOf("tasks", "recentChanges", "blockers", "calendar", "recentDiary", "vocabulary")
            .forEach { field ->
                assertTrue("現況の $field に触れていない", header.contains(field))
            }
    }

    @Test
    fun diary_embeds_the_requested_date() {
        val prompt = YosugaPrompt.diary("2026-07-27")

        assertTrue(prompt.contains("\"date\": \"2026-07-27\""))
        assertTrue(prompt.contains("\"schemaVersion\": 2"))
        assertTrue(prompt.contains("proposals"))
    }

    /**
     * 日記のJSONに他の鍵を混ぜさせない。混ざると承認画面に無関係な提案が並び、
     * 「日記を頼んだのに何を承認しているのか」になる。
     */
    @Test
    fun diary_forbids_other_proposal_keys() {
        val prompt = YosugaPrompt.diary("2026-07-27")

        assertTrue(prompt.contains("diary"))
        assertTrue(prompt.contains("別のJSONで出す"))
        // 出力例そのものに tasks/items を混ぜない。
        assertFalse(prompt.contains("\"tasks\""))
        assertFalse(prompt.contains("\"items\""))
    }

    /**
     * 文体はヨスガのメモリが正(`YosugaPrompt.diary` の KDoc)。
     * [YosugaPrompt.DIARY_STYLE] が空のあいだは、そう明示していること。
     */
    @Test
    fun diary_defers_style_to_memory_while_style_is_empty() {
        val prompt = YosugaPrompt.diary("2026-07-27")

        if (YosugaPrompt.DIARY_STYLE.isBlank()) {
            assertTrue(prompt.contains("観察日記 作成ルール"))
        } else {
            assertTrue(prompt.contains(YosugaPrompt.DIARY_STYLE))
        }
    }

    @Test
    fun session_embeds_the_requested_date() {
        val prompt = YosugaPrompt.session("2026-07-27")

        assertTrue(prompt.contains("\"date\": \"2026-07-27\""))
        assertTrue(prompt.contains("\"schemaVersion\": 2"))
        assertTrue(prompt.contains("session"))
    }

    /** シロさんが求めた4種(実装 / 情報収集 / 相談 / 会話ログ由来のメモ)を落とさない。 */
    @Test
    fun session_asks_for_every_requested_section() {
        val prompt = YosugaPrompt.session("2026-07-27")

        listOf("実装・作業", "調べたこと", "相談・検討", "決定事項", "新しいタスク", "気づき・メモ")
            .forEach { heading ->
                assertTrue("$heading の節が無い", prompt.contains(heading))
            }
    }

    /**
     * Frontmatter は Hub が組む(`ConversationNoteBuilder.buildSession`)。
     * ヨスガに YAML を書かせると、引用符やコロンで壊れて Obsidian の検索から消える。
     */
    @Test
    fun session_forbids_frontmatter() {
        assertTrue(YosugaPrompt.session("2026-07-27").contains("Frontmatter"))
        assertTrue(YosugaPrompt.session("2026-07-27").contains("書かない"))
    }

    /** 日記とセッション記録は別の口。片方がもう片方を頼まない。 */
    @Test
    fun diary_and_session_do_not_request_each_other() {
        assertFalse(YosugaPrompt.diary("2026-07-27").contains("\"session\""))
        assertFalse(YosugaPrompt.session("2026-07-27").contains("\"diary\""))
    }
}
