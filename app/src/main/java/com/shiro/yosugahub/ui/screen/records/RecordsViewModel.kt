package com.shiro.yosugahub.ui.screen.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.repository.DiaryRepository
import com.shiro.yosugahub.data.repository.KnowledgeRepository
import com.shiro.yosugahub.domain.model.DiaryEntry
import com.shiro.yosugahub.domain.model.KnowledgeItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** 記録タブが監視するUI状態。絞り込みは画面側(純粋関数)で行う。 */
data class RecordsUiState(
    val items: List<KnowledgeItem> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val diaryEntries: List<DiaryEntry> = emptyList(),
)

class RecordsViewModel(
    knowledgeRepository: KnowledgeRepository,
    diaryRepository: DiaryRepository,
) : ViewModel() {

    val uiState: StateFlow<RecordsUiState> = combine(
        knowledgeRepository.items(),
        knowledgeRepository.tagNames(),
        diaryRepository.entries(),
    ) { items, tagNames, diaryEntries ->
        RecordsUiState(
            items = items,
            tagNames = tagNames,
            diaryEntries = diaryEntries,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordsUiState(),
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YosugaHubApplication
                RecordsViewModel(
                    knowledgeRepository = app.container.knowledgeRepository,
                    diaryRepository = app.container.diaryRepository,
                )
            }
        }
    }
}
