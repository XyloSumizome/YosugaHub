package com.shiro.yosugahub.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** アプリ全体で共有する Preferences DataStore(1プロセス1インスタンス)。 */
private val Context.dataStore by preferencesDataStore(name = "user_prefs")

/**
 * 軽量設定を DataStore で永続化する Repository(設計書4.1)。
 * まずは最終同期時刻のみを扱う。GitHub/カレンダー設定などは Phase 3/4 で追加する。
 */
class UserPreferencesRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    /** 最終同期時刻。未設定なら空文字。 */
    val lastSyncedAt: Flow<String> = dataStore.data
        .catch { error ->
            // 読み込み失敗時(IO エラー)はクラッシュさせず空設定として扱う。
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[Keys.LAST_SYNCED_AT].orEmpty() }

    suspend fun setLastSyncedAt(value: String) {
        dataStore.edit { preferences -> preferences[Keys.LAST_SYNCED_AT] = value }
    }

    /** SAF で選択した Obsidian Vault のツリーURI。未設定なら空文字(v3-Step 3)。 */
    val obsidianVaultUri: Flow<String> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[Keys.OBSIDIAN_VAULT_URI].orEmpty() }

    suspend fun setObsidianVaultUri(value: String) {
        dataStore.edit { preferences -> preferences[Keys.OBSIDIAN_VAULT_URI] = value }
    }

    /**
     * GitHub アクセストークンの**暗号化済み**文字列(v3-Step 3)。
     * 平文は保存しない。復号は GitHubSettingsRepository が通信直前にのみ行う。
     */
    val gitHubTokenEncrypted: Flow<String> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[Keys.GITHUB_TOKEN_ENCRYPTED].orEmpty() }

    suspend fun setGitHubTokenEncrypted(value: String) {
        dataStore.edit { preferences -> preferences[Keys.GITHUB_TOKEN_ENCRYPTED] = value }
    }

    /** サーバー同期先のベースURL(例: https://example.com/yosuga)。秘密情報ではないため平文。 */
    val syncBaseUrl: Flow<String> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[Keys.SYNC_BASE_URL].orEmpty() }

    suspend fun setSyncBaseUrl(value: String) {
        dataStore.edit { preferences -> preferences[Keys.SYNC_BASE_URL] = value }
    }

    /** サーバー同期トークンの**暗号化済み**文字列(v4)。平文は保存しない。 */
    val syncTokenEncrypted: Flow<String> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[Keys.SYNC_TOKEN_ENCRYPTED].orEmpty() }

    suspend fun setSyncTokenEncrypted(value: String) {
        dataStore.edit { preferences -> preferences[Keys.SYNC_TOKEN_ENCRYPTED] = value }
    }

    private object Keys {
        val LAST_SYNCED_AT = stringPreferencesKey("last_synced_at")
        val OBSIDIAN_VAULT_URI = stringPreferencesKey("obsidian_vault_uri")
        val GITHUB_TOKEN_ENCRYPTED = stringPreferencesKey("github_token_encrypted")
        val SYNC_BASE_URL = stringPreferencesKey("sync_base_url")
        val SYNC_TOKEN_ENCRYPTED = stringPreferencesKey("sync_token_encrypted")
    }
}
