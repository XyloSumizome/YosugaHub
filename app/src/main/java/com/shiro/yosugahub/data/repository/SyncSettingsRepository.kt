package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.datastore.UserPreferencesRepository
import com.shiro.yosugahub.data.security.TokenCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.SecureRandom

/**
 * サーバー同期の設定(URL + トークン)の保管(v4 Phase2)。
 * トークンは GitHub トークンと同じ仕組み(Keystore 暗号化)で保管し、UI へは有無だけを流す。
 */
class SyncSettingsRepository(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val tokenCrypto: TokenCrypto,
) {

    val baseUrl: Flow<String> = userPreferencesRepository.syncBaseUrl

    val hasToken: Flow<Boolean> =
        userPreferencesRepository.syncTokenEncrypted.map { it.isNotBlank() }

    suspend fun saveBaseUrl(url: String) {
        userPreferencesRepository.setSyncBaseUrl(url.trim().trimEnd('/'))
    }

    suspend fun saveToken(plainToken: String): Boolean {
        val trimmed = plainToken.trim()
        if (trimmed.isEmpty()) return false
        val encrypted = tokenCrypto.encrypt(trimmed) ?: return false
        userPreferencesRepository.setSyncTokenEncrypted(encrypted)
        return true
    }

    suspend fun clearToken() {
        userPreferencesRepository.setSyncTokenEncrypted("")
    }

    /** 復号したトークン(通信直前にのみ呼ぶ)。未設定・復号失敗は null。 */
    suspend fun currentToken(): String? {
        val stored = userPreferencesRepository.syncTokenEncrypted.first()
        if (stored.isBlank()) return null
        return tokenCrypto.decrypt(stored)?.takeIf { it.isNotBlank() }
    }

    companion object {
        /** サーバー側 config.php に貼るトークンの生成(64桁hex)。 */
        fun generateToken(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
