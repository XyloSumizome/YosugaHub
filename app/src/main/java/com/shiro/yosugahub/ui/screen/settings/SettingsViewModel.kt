package com.shiro.yosugahub.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.local.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 設定画面が監視するUI状態(v3-Step 3: Obsidian Vault 設定)。 */
data class SettingsUiState(
    val obsidianVaultUri: String = "",
)

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = userPreferencesRepository.obsidianVaultUri
        .map { SettingsUiState(obsidianVaultUri = it) }
        .stateIn(
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YosugaHubApplication
                SettingsViewModel(app.container.userPreferencesRepository)
            }
        }
    }
}
