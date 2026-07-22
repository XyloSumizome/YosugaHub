package com.shiro.yosugahub.ui.screen.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.repository.AssistantRepository
import com.shiro.yosugahub.domain.model.Recommendation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** よすが連携画面が監視するUI状態。 */
data class AssistantUiState(
    val recommendations: List<Recommendation> = emptyList(),
)

class AssistantViewModel(
    private val assistantRepository: AssistantRepository,
) : ViewModel() {

    val uiState: StateFlow<AssistantUiState> = assistantRepository.recommendations()
        .map { AssistantUiState(recommendations = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AssistantUiState(),
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YosugaHubApplication
                AssistantViewModel(app.container.assistantRepository)
            }
        }
    }
}
