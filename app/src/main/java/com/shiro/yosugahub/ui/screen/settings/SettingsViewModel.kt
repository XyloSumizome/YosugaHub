package com.shiro.yosugahub.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.local.datastore.UserPreferencesRepository
import com.shiro.yosugahub.data.repository.GitHubSettingsRepository
import com.shiro.yosugahub.data.repository.ServerSyncRepository
import com.shiro.yosugahub.data.repository.SyncResult
import com.shiro.yosugahub.data.repository.SyncSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 設定画面が監視するUI状態(Obsidian Vault / GitHub トークン / サーバー同期)。 */
data class SettingsUiState(
    val obsidianVaultUri: String = "",
    val hasGitHubToken: Boolean = false,
    val syncBaseUrl: String = "",
    val hasSyncToken: Boolean = false,
    val isSyncing: Boolean = false,
)

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val gitHubSettingsRepository: GitHubSettingsRepository,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val serverSyncRepository: ServerSyncRepository,
) : ViewModel() {

    private val syncing = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.obsidianVaultUri,
        gitHubSettingsRepository.hasToken,
        syncSettingsRepository.baseUrl,
        syncSettingsRepository.hasToken,
        syncing,
    ) { vaultUri, hasGitHubToken, syncUrl, hasSyncToken, isSyncing ->
        SettingsUiState(
            obsidianVaultUri = vaultUri,
            hasGitHubToken = hasGitHubToken,
            syncBaseUrl = syncUrl,
            hasSyncToken = hasSyncToken,
            isSyncing = isSyncing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    /** サーバー同期のURL保存(末尾スラッシュは正規化)。 */
    fun saveSyncUrl(url: String) {
        viewModelScope.launch { syncSettingsRepository.saveBaseUrl(url) }
    }

    /** サーバー同期トークンを暗号化して保存。結果を UI へ返す。 */
    fun saveSyncToken(token: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(syncSettingsRepository.saveToken(token)) }
    }

    /** config.php に貼るトークンを生成する(保存はしない。表示して控えてもらう)。 */
    fun generateSyncToken(): String = SyncSettingsRepository.generateToken()

    /** AI向けJSONを生成してサーバーへ同期する。 */
    fun syncNow(onResult: (SyncResult) -> Unit) {
        viewModelScope.launch {
            syncing.value = true
            try {
                onResult(serverSyncRepository.sync())
            } finally {
                syncing.value = false
            }
        }
    }

    /** SAF で選択された Vault のツリーURIを保存する(権限の永続化は画面側で実施済み)。 */
    fun saveVaultUri(uri: String) {
        viewModelScope.launch {
            userPreferencesRepository.setObsidianVaultUri(uri)
        }
    }

    /** GitHub トークンを暗号化して保存する。結果(成功/失敗)を UI へ返す。 */
    fun saveGitHubToken(token: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(gitHubSettingsRepository.saveToken(token))
        }
    }

    fun clearGitHubToken() {
        viewModelScope.launch { gitHubSettingsRepository.clearToken() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YosugaHubApplication
                SettingsViewModel(
                    userPreferencesRepository = app.container.userPreferencesRepository,
                    gitHubSettingsRepository = app.container.gitHubSettingsRepository,
                    syncSettingsRepository = app.container.syncSettingsRepository,
                    serverSyncRepository = app.container.serverSyncRepository,
                )
            }
        }
    }
}
