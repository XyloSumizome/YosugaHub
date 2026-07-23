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
import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.KnowledgeItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 記録タブが監視するUI状態。絞り込みは画面側(純粋関数)で行う。 */
data class RecordsUiState(
    val items: List<KnowledgeItem> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val diaryEntries: List<DiaryEntry> = emptyList(),
)

class RecordsViewModel(
    private val knowledgeRepository: KnowledgeRepository,
    diaryRepository: DiaryRepository,
) : ViewModel() {

    /** 手動でアイテムを追加する(source=manual、実体の関連付けは AI 経由のみ)。 */
    fun addItem(kind: ItemKind, title: String, body: String, tags: List<String>) {
        viewModelScope.launch {
            knowledgeRepository.createItem(
                kind = kind,
                title = title,
                body = body,
                tags = tags,
                entities = emptyList(),
                source = "manual",
            )
        }
    }

    /** 編集済みアイテムを保存する(updatedAt は Repository が刻む)。 */
    fun updateItem(item: KnowledgeItem) {
        viewModelScope.launch { knowledgeRepository.updateItem(item) }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch { knowledgeRepository.deleteItem(id) }
    }

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
