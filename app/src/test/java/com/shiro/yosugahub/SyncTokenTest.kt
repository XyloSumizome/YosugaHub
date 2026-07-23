package com.shiro.yosugahub

import com.shiro.yosugahub.data.repository.SyncSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * サーバー同期トークンの生成(v4 Phase2)。
 * このトークンがロリポップ上の受け口・配信の唯一の保護なので、性質を固定しておく。
 */
class SyncTokenTest {

    @Test
    fun generates_64_hex_characters() {
        val token = SyncSettingsRepository.generateToken()
        assertEquals(64, token.length)  // 32バイト = 64桁hex
        assertTrue("英小文字と数字のみ: $token", token.matches(Regex("^[0-9a-f]{64}$")))
    }

    /** 予測可能な値を返していないこと(SecureRandom を使う理由そのもの)。 */
    @Test
    fun generates_a_different_token_every_time() {
        val tokens = List(50) { SyncSettingsRepository.generateToken() }
        assertEquals(50, tokens.toSet().size)
    }

    /** URL・ヘッダー・PHPの設定ファイルにそのまま貼れる文字だけであること。 */
    @Test
    fun contains_no_characters_needing_escape() {
        val token = SyncSettingsRepository.generateToken()
        assertEquals(token, token.trim())
        assertTrue(token.none { it.isWhitespace() || it in "'\"\\/&?=#<>" })
    }
}
