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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 設定画面が監視するUI状態(Obsidian Vault / GitHub トークン)。 */
data class SettingsUiState(
    val obsidianVaultUri: String = "",
    val hasGitHubToken: Boolean = false,
)

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val gitHubSettingsRepository: GitHubSettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.obsidianVaultUri,
        gitHubSettingsRepository.hasToken,
    ) { vaultUri, hasToken ->
        SettingsUiState(obsidianVaultUri = vaultUri, hasGitHubToken = hasToken)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

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
                )
            }
        }
    }
}
