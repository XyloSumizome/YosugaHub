package com.shiro.yosugahub.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * トークンの暗号化・復号(設計書9章: 秘密情報は Android Keystore で保持)。
 * テスト差し替えのため interface にする。
 */
interface TokenCrypto {
    /** 平文 → 保存用文字列。失敗したら null。 */
    fun encrypt(plainText: String): String?

    /** 保存用文字列 → 平文。鍵の失効・データ破損時は null。 */
    fun decrypt(stored: String): String?
}

/**
 * Android Keystore の AES-256-GCM 鍵で暗号化する実装。
 * 鍵は Keystore 内から取り出せない形で生成され、平文トークンは永続化しない。
 * 端末のバックアップ復元などで鍵が失われた場合は復号に失敗し、null を返す
 * (呼び出し側は「未設定」として扱い、ユーザーに再入力を促す)。
 */
class KeystoreTokenCrypto : TokenCrypto {

    override fun encrypt(plainText: String): String? = try {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        SecretEnvelope.pack(cipher.iv, cipherText)
    } catch (e: Exception) {
        // 例外メッセージにも平文を含めないため、内容は伝播させない。
        null
    }

    override fun decrypt(stored: String): String? = try {
        val (iv, cipherText) = SecretEnvelope.unpack(stored) ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        }
        String(cipher.doFinal(cipherText), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "yosuga_github_token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
        const val KEY_SIZE_BITS = 256
    }
}
