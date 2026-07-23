package com.shiro.yosugahub.data.security

import java.util.Base64

/**
 * 暗号文の入れ物(IV + 暗号文)の詰め替え。純粋ロジックなのでユニットテスト可能。
 * 保存形式: Base64(IV) + ":" + Base64(暗号文)
 *
 * ここでは平文を一切扱わない(平文はメモリ上のみ。ログにも出さない)。
 */
object SecretEnvelope {

    private const val SEPARATOR = ":"

    fun pack(iv: ByteArray, cipherText: ByteArray): String {
        val encoder = Base64.getEncoder()
        return encoder.encodeToString(iv) + SEPARATOR + encoder.encodeToString(cipherText)
    }

    /** 保存文字列を (IV, 暗号文) へ戻す。壊れていたら null(復号は諦めて未設定として扱う)。 */
    fun unpack(stored: String): Pair<ByteArray, ByteArray>? {
        val parts = stored.split(SEPARATOR)
        if (parts.size != 2) return null
        return try {
            val decoder = Base64.getDecoder()
            val iv = decoder.decode(parts[0])
            val cipherText = decoder.decode(parts[1])
            if (iv.isEmpty() || cipherText.isEmpty()) null else iv to cipherText
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
