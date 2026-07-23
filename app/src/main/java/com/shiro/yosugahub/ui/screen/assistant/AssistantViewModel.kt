package com.shiro.yosugahub.ui.screen.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.net.Uri
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.repository.AssistantRepository
import com.shiro.yosugahub.data.repository.ExportRepository
import com.shiro.yosugahub.data.repository.ExportResult
import com.shiro.yosugahub.data.repository.ImportRepository
import com.shiro.yosugahub.data.repository.ImportResult
import com.shiro.yosugahub.domain.model.Recommendation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ヨスガ連携画面が監視するUI状態。 */
data class AssistantUiState(
    val recommendations: List<Recommendation> = emptyList(),
)

class AssistantViewModel(
    private val assistantRepository: AssistantRepository,
    private val exportRepository: ExportRepository,
    private val importRepository: ImportRepository,
) : ViewModel() {

    /** 状況JSNを生成・保存し、結果(成功/失敗)を UI へ返す。共有と表示は UI 側で行う。 */
    fun createExport(onResult: (Result<ExportResult>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { exportRepository.createContextExport() })
        }
    }

    /** 選択された回答JSNを取り込み、結果を UI へ返す。 */
    fun importResponse(uri: Uri, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            onResult(importRepository.importResponse(uri))
        }
    }

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
                AssistantViewModel(
                    assistantRepository = app.container.assistantRepository,
                    exportRepository = app.container.exportRepository,
                    importRepository = app.container.importRepository,
                )
            }
        }
    }
}
