package com.shiro.yosugahub.ui.screen.assistant

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.local.datastore.UserPreferencesRepository
import com.shiro.yosugahub.data.repository.ApproveResult
import com.shiro.yosugahub.data.repository.AssistantRepository
import com.shiro.yosugahub.data.repository.ConversationImportRepository
import com.shiro.yosugahub.data.repository.ConversationImportResult
import com.shiro.yosugahub.data.repository.ExportRepository
import com.shiro.yosugahub.data.repository.ExportResult
import com.shiro.yosugahub.data.repository.ImportRepository
import com.shiro.yosugahub.data.repository.ImportResult
import com.shiro.yosugahub.data.repository.NoteImportRepository
import com.shiro.yosugahub.data.repository.NoteImportSummary
import com.shiro.yosugahub.data.repository.ProjectRepository
import com.shiro.yosugahub.data.repository.ProposalRepository
import com.shiro.yosugahub.domain.model.PendingProposal
import com.shiro.yosugahub.domain.model.Recommendation
import com.shiro.yosugahub.ui.component.LogLine
import com.shiro.yosugahub.ui.component.LogTone
import com.shiro.yosugahub.ui.component.OpLogState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ヨスガ連携画面 / コンソールが監視するUI状態。 */
data class AssistantUiState(
    val proposals: List<ProposalCardUi> = emptyList(),
    val recommendations: List<Recommendation> = emptyList(),
    /** コンソール上部の状態表示用。 */
    val projectCount: Int = 0,
    val lastSync: String = "",
) {
    val pendingCount: Int get() = proposals.size
}

class AssistantViewModel(
    private val assistantRepository: AssistantRepository,
    private val exportRepository: ExportRepository,
    private val importRepository: ImportRepository,
    private val proposalRepository: ProposalRepository,
    private val noteImportRepository: NoteImportRepository,
    private val conversationImportRepository: ConversationImportRepository,
    private val projectRepository: ProjectRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val projectCount = projectRepository.projects()
        .map { it.size }
    private val lastSync = userPreferencesRepository.lastSyncedAt

    /** 端末ログ(v5 UI)。取り込み・保存・生成などの操作すべてで共用する。 */
    val opLog = OpLogState()

    private val _noteImportSummary = MutableStateFlow<NoteImportSummary?>(null)
    val noteImportSummary: StateFlow<NoteImportSummary?> = _noteImportSummary

    /**
     * 各ゲームの `.yosuga/notes/` を取り込んで Obsidian Vault へ収める(v5 Phase 3-c)。
     * 進捗イベントを端末ログとして1行ずつ流す。**実処理は本物**。
     */
    fun importNotes() {
        viewModelScope.launch {
            val summary = opLog.run { emit ->
                noteImportRepository.importAll { event -> emit.emit(ImportLog.format(event)) }
            } ?: return@launch
            delay(SUMMARY_DELAY_MS)
            _noteImportSummary.value = summary
        }
    }

    fun dismissNoteImportSummary() {
        _noteImportSummary.value = null
        opLog.clear()
    }

    /** ヨスガのセッションまとめを Obsidian へ保存する(v5 Phase 3-d)。演出付き。 */
    fun saveConversation(body: String, onResult: (ConversationImportResult) -> Unit) {
        viewModelScope.launch {
            val result = opLog.run { emit ->
                emit.emit(LogLine("> SAVE SESSION", LogTone.ACCENT))
                emit.emit(LogLine("  PARSE session …", LogTone.INFO))
                val r = conversationImportRepository.save(body)
                when (r) {
                    is ConversationImportResult.Saved ->
                        emit.emit(LogLine("  WRITE ${r.path} … OK", LogTone.OK))
                    ConversationImportResult.Empty ->
                        emit.emit(LogLine("  ABORT 内容が空です", LogTone.ERROR))
                    ConversationImportResult.VaultNotConfigured ->
                        emit.emit(LogLine("  ABORT Vault 未設定", LogTone.ERROR))
                    is ConversationImportResult.Failed ->
                        emit.emit(LogLine("  FAIL ${r.reason}", LogTone.ERROR))
                }
                emit.emit(LogLine("> DONE", LogTone.ACCENT))
                r
            } ?: return@launch
            onResult(result)
        }
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

    /** 状況JSNを生成・保存し、結果を UI へ返す。演出付き。 */
    fun createExport(onResult: (Result<ExportResult>) -> Unit) {
        viewModelScope.launch {
            val result = opLog.run { emit ->
                emit.emit(LogLine("> EXPORT STATUS", LogTone.ACCENT))
                emit.emit(LogLine("  COLLECT projects / tasks / events / decisions", LogTone.INFO))
                emit.emit(LogLine("  BUILD context json …", LogTone.INFO))
                val r = runCatching { exportRepository.createContextExport() }
                r.onSuccess {
                    emit.emit(LogLine("  WRITE ${it.fileName} … OK", LogTone.OK))
                    emit.emit(LogLine("  SIZE ${it.json.length} bytes", LogTone.INFO))
                }.onFailure {
                    emit.emit(LogLine("  FAIL 生成に失敗しました", LogTone.ERROR))
                }
                emit.emit(LogLine("> DONE", LogTone.ACCENT))
                r
            } ?: return@launch
            onResult(result)
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
        projectCount,
        lastSync,
    ) { pending, recommendations, projects, sync ->
        AssistantUiState(
            proposals = pending.map { it.toCardUi() },
            recommendations = recommendations,
            projectCount = projects,
            lastSync = sync,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AssistantUiState(),
    )

    companion object {
        private const val SUMMARY_DELAY_MS = 550L

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
                    projectRepository = app.container.projectRepository,
                    userPreferencesRepository = app.container.userPreferencesRepository,
                )
            }
        }
    }
}
