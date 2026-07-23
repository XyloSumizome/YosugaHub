package com.shiro.yosugahub

import com.shiro.yosugahub.data.security.SecretEnvelope
import com.shiro.yosugahub.data.security.TokenCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * SecretEnvelope(純粋ロジック)と TokenCrypto 実装の契約を検証する。
 * Keystore を使う KeystoreTokenCrypto 自体は Android 実機/エミュレーターが必要なため
 * ユニットテスト対象外(instrumentation で確認する)。
 */
class GitHubSettingsRepositoryTest {

    @Test
    fun envelope_round_trips_iv_and_cipher_text() {
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val cipherText = byteArrayOf(-1, 0, 42, 99)
        val packed = SecretEnvelope.pack(iv, cipherText)
        val (unpackedIv, unpackedCipher) = SecretEnvelope.unpack(packed)!!
        assertArrayEquals(iv, unpackedIv)
        assertArrayEquals(cipherText, unpackedCipher)
    }

    @Test
    fun envelope_rejects_malformed_input() {
        assertNull(SecretEnvelope.unpack(""))
        assertNull(SecretEnvelope.unpack("no-separator"))
        assertNull(SecretEnvelope.unpack("a:b:c"))
        assertNull(SecretEnvelope.unpack("!!!:!!!"))
        assertNull(SecretEnvelope.unpack(":"))
    }

    @Test
    fun envelope_output_does_not_contain_plain_text() {
        // 暗号文はバイト列なので、包んだ結果に平文が現れないことを確認する
        val packed = SecretEnvelope.pack(byteArrayOf(1, 2, 3), byteArrayOf(9, 9, 9))
        assertEquals(false, packed.contains("ghp_"))
    }

    /** 暗号化をフェイクにして、Repository が扱う保存形式の往復を確認する。 */
    private class FakeTokenCrypto : TokenCrypto {
        var failEncrypt = false
        override fun encrypt(plainText: String): String? =
            if (failEncrypt) null else "enc(" + plainText + ")"

        override fun decrypt(stored: String): String? =
            if (stored.startsWith("enc(") && stored.endsWith(")")) {
                stored.removePrefix("enc(").removeSuffix(")")
            } else null
    }

    @Test
    fun crypto_contract_round_trips_and_reports_failure() {
        val crypto = FakeTokenCrypto()
        val encrypted = crypto.encrypt("ghp_example")!!
        assertEquals("ghp_example", crypto.decrypt(encrypted))
        assertNull(crypto.decrypt("broken-data"))

        crypto.failEncrypt = true
        assertNull(crypto.encrypt("ghp_example"))
    }
}
