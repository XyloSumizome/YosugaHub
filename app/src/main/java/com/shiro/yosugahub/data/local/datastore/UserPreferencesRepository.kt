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

    private object Keys {
        val LAST_SYNCED_AT = stringPreferencesKey("last_synced_at")
    }
}
