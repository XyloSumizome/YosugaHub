package com.shiro.yosugahub.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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

    /**
     * ヨスガとの**その日の会話**のURL(`https://chatgpt.com/c/…`)。
     *
     * 2026-07-25 に持っていたレコル(カスタムGPT)のURLを、レコル廃止に伴って
     * 置き換えたもの(2026-07-27)。**役目が違う**ので値は引き継がない。
     *
     * ⚠ **なぜ会話単位のURLが要るのか。** 観察日誌とセッション記録の材料は
     * 「その日の会話そのもの」なので、新しい会話を開いてしまうと材料がゼロになる。
     * 共有インテント(`ACTION_SEND`)では行き先の会話を指定できないため、
     * URL で名指しする。未設定なら [ChatGptLink.DEFAULT_URL](新しい会話)へ退く。
     *
     * 秘密情報ではないため平文。開く前に `ExternalLink` で http/https のみに絞る。
     */
    val yosugaConversationUrl: Flow<String> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[Keys.YOSUGA_CONVERSATION_URL].orEmpty() }

    suspend fun setYosugaConversationUrl(value: String) {
        dataStore.edit { preferences -> preferences[Keys.YOSUGA_CONVERSATION_URL] = value }
    }

    private object Keys {
        val YOSUGA_CONVERSATION_URL = stringPreferencesKey("yosuga_conversation_url")
        val LAST_SYNCED_AT = stringPreferencesKey("last_synced_at")
        val OBSIDIAN_VAULT_URI = stringPreferencesKey("obsidian_vault_uri")
        val GITHUB_TOKEN_ENCRYPTED = stringPreferencesKey("github_token_encrypted")
        val SYNC_BASE_URL = stringPreferencesKey("sync_base_url")
        val SYNC_TOKEN_ENCRYPTED = stringPreferencesKey("sync_token_encrypted")
    }
}
