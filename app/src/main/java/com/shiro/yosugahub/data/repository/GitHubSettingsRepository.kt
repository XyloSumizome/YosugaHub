package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.datastore.UserPreferencesRepository
import com.shiro.yosugahub.data.security.TokenCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * GitHub アクセストークンの保管(設計書9章)。
 * 暗号化した文字列だけを DataStore に置き、平文は返す瞬間だけメモリに乗せる。
 * UI へはトークン本体を公開せず「設定済みか」だけを流す。
 */
class GitHubSettingsRepository(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val tokenCrypto: TokenCrypto,
) {

    /** トークンが設定されているか(UI 表示用。トークン本体は流さない)。 */
    val hasToken: Flow<Boolean> =
        userPreferencesRepository.gitHubTokenEncrypted.map { it.isNotBlank() }

    /** 保存。暗号化に失敗したら false を返し、何も保存しない。 */
    suspend fun saveToken(plainToken: String): Boolean {
        val trimmed = plainToken.trim()
        if (trimmed.isEmpty()) return false
        val encrypted = tokenCrypto.encrypt(trimmed) ?: return false
        userPreferencesRepository.setGitHubTokenEncrypted(encrypted)
        return true
    }

    suspend fun clearToken() {
        userPreferencesRepository.setGitHubTokenEncrypted("")
    }

    /**
     * 復号したトークンを返す(通信直前にのみ呼ぶ)。
     * 未設定・復号失敗時は null。ログへ出力しないこと。
     */
    suspend fun currentToken(): String? {
        val stored = userPreferencesRepository.gitHubTokenEncrypted.first()
        if (stored.isBlank()) return null
        return tokenCrypto.decrypt(stored)?.takeIf { it.isNotBlank() }
    }
}
