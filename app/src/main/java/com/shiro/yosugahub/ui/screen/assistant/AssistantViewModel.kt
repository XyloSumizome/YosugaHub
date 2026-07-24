package com.shiro.yosugahub.ui.screen.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.net.Uri
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.repository.ApproveResult
import com.shiro.yosugahub.data.repository.AssistantRepository
import com.shiro.yosugahub.data.repository.ExportRepository
import com.shiro.yosugahub.data.repository.ExportResult
import com.shiro.yosugahub.data.repository.ImportRepository
import com.shiro.yosugahub.data.repository.ImportResult
import com.shiro.yosugahub.data.repository.ConversationImportRepository
import com.shiro.yosugahub.data.repository.ConversationImportResult
import com.shiro.yosugahub.data.repository.NoteImportRepository
import com.shiro.yosugahub.data.repository.NoteImportSummary
import com.shiro.yosugahub.data.repository.ProposalRepository
import com.shiro.yosugahub.domain.model.PendingProposal
import com.shiro.yosugahub.domain.model.Recommendation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ヨスガ連携画面が監視するUI状態。 */
data class AssistantUiState(
    val proposals: List<ProposalCardUi> = emptyList(),
    val recommendations: List<Recommendation> = emptyList(),
)

class AssistantViewModel(
    private val assistantRepository: AssistantRepository,
    private val exportRepository: ExportRepository,
    private val importRepository: ImportRepository,
    private val proposalRepository: ProposalRepository,
    private val noteImportRepository: NoteImportRepository,
    private val conversationImportRepository: ConversationImportRepository,
) : ViewModel() {

    /** ヨスガのセッションまとめを Obsidian へ保存する(v5 Phase 3-d)。 */
    fun saveConversation(body: String, onResult: (ConversationImportResult) -> Unit) {
        viewModelScope.launch { onResult(conversationImportRepository.save(body)) }
    }

    private val _noteImporting = MutableStateFlow(false)
    val noteImporting: StateFlow<Boolean> = _noteImporting

    private val _noteImportSummary = MutableStateFlow<NoteImportSummary?>(null)
    val noteImportSummary: StateFlow<NoteImportSummary?> = _noteImportSummary

    /**
     * 各ゲームの `.yosuga/notes/` を取り込んで Obsidian Vault へ収める(v5 Phase 3-c)。
     * 取得済みのノートは飛ばすので、繰り返し押しても二重には入らない。
     */
    fun importNotes() {
        if (_noteImporting.value) return
        viewModelScope.launch {
            _noteImporting.value = true
            _noteImportSummary.value = null
            try {
                _noteImportSummary.value = noteImportRepository.importAll()
            } finally {
                _noteImporting.value = false
            }
        }
    }

    fun dismissNoteImportSummary() {
        _noteImportSummary.value = null
    }

    /** 提案を承認して本テーブルへ反映する。反映できない提案は棄却へ回る。 */
    fun approveProposal(proposal: PendingProposal, onResult: (ApproveResult) -> Unit) {
        viewModelScope.launch {
            onResult(proposalRepository.approve(proposal))
        }
    }

    /** 提案を棄却する(行は履歴として残る)。 */
    fun rejectProposal(proposal: PendingProposal) {
        viewModelScope.launch {
            proposalRepository.reject(proposal.id)
        }
    }

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

    /** 貼り付けられた回答JSONを取り込む(v4.3: ファイルを作らず直接貼る)。 */
    fun importResponseText(text: String, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            onResult(importRepository.importResponseText(text))
        }
    }

    val uiState: StateFlow<AssistantUiState> = combine(
        proposalRepository.pending(),
        assistantRepository.recommendations(),
    ) { pending, recommendations ->
        AssistantUiState(
            proposals = pending.map { it.toCardUi() },
            recommendations = recommendations,
        )
    }.stateIn(
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
                    proposalRepository = app.container.proposalRepository,
                    noteImportRepository = app.container.noteImportRepository,
                    conversationImportRepository = app.container.conversationImportRepository,
                )
            }
        }
    }
}
