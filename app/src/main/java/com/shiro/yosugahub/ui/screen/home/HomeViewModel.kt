package com.shiro.yosugahub.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.net.Uri
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.local.datastore.UserPreferencesRepository
import com.shiro.yosugahub.data.repository.CalendarRepository
import com.shiro.yosugahub.data.repository.DocumentRepository
import com.shiro.yosugahub.data.repository.ExportRepository
import com.shiro.yosugahub.data.repository.ExportResult
import com.shiro.yosugahub.data.repository.ImportRepository
import com.shiro.yosugahub.data.repository.ImportResult
import com.shiro.yosugahub.data.repository.KnowledgeRepository
import com.shiro.yosugahub.data.repository.ProjectRepository
import com.shiro.yosugahub.data.repository.ProposalRepository
import com.shiro.yosugahub.data.repository.TaskRepository
import com.shiro.yosugahub.domain.model.CalendarEvent
import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.Task
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** ホーム画面が監視するUI状態(v3 ホーム再編: AI秘書視点)。 */
data class HomeUiState(
    val today: String = "",
    val lastSyncedAt: String = "",
    val todayTasks: List<Task> = emptyList(),
    val pendingProposalCount: Int = 0,
    /** ヨスガの分類が届き、ユーザーの確認を待っている文書の件数(v4.1)。 */
    val documentsNeedingReviewCount: Int = 0,
    val recentDecisions: List<KnowledgeItem> = emptyList(),
    val todayEvents: List<CalendarEvent> = emptyList(),
    val nextEvent: CalendarEvent? = null,
    val projects: List<Project> = emptyList(),
)

/** AI秘書系のフロー(タスク・承認待ち・決定事項)をまとめる中間データ。 */
private data class SecretaryData(
    val todayTasks: List<Task>,
    val pendingProposalCount: Int,
    val documentsNeedingReviewCount: Int,
    val recentDecisions: List<KnowledgeItem>,
)

class HomeViewModel(
    private val calendarRepository: CalendarRepository,
    private val projectRepository: ProjectRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exportRepository: ExportRepository,
    private val importRepository: ImportRepository,
    taskRepository: TaskRepository,
    proposalRepository: ProposalRepository,
    knowledgeRepository: KnowledgeRepository,
    documentRepository: DocumentRepository,
    private val todayDate: () -> String = { LocalDate.now().toString() },
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

    /** 貼り付けられた回答JSONを取り込む(v4.3: ファイルを作らず直接貼る)。 */
    fun importResponseText(text: String, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            onResult(importRepository.importResponseText(text))
        }
    }

    /** タスク・承認待ち件数・確認待ち文書・最近の決定を1本にまとめる(combine の5フロー制限対策)。 */
    private val secretaryFlow = combine(
        taskRepository.tasks(),
        proposalRepository.pendingCount(),
        knowledgeRepository.items(),
        documentRepository.needsReviewCount(),
    ) { tasks, pendingCount, items, documentsNeedingReview ->
        SecretaryData(
            todayTasks = todayFocus(tasks, today = todayDate()),
            pendingProposalCount = pendingCount,
            documentsNeedingReviewCount = documentsNeedingReview,
            recentDecisions = items.filter { it.kind == ItemKind.DECISION }.take(RECENT_DECISIONS),
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        calendarRepository.todayEvents(),
        calendarRepository.upcomingEvents(),
        projectRepository.projects(),
        userPreferencesRepository.lastSyncedAt,
        secretaryFlow,
    ) { todayEvents, upcomingEvents, projects, lastSyncedAt, secretary ->
        HomeUiState(
            today = calendarRepository.today,
            lastSyncedAt = lastSyncedAt.ifEmpty { "未同期" },
            todayTasks = secretary.todayTasks,
            pendingProposalCount = secretary.pendingProposalCount,
            documentsNeedingReviewCount = secretary.documentsNeedingReviewCount,
            recentDecisions = secretary.recentDecisions,
            todayEvents = todayEvents,
            nextEvent = upcomingEvents.firstOrNull(),
            projects = projects,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    companion object {
        private const val RECENT_DECISIONS = 3

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YosugaHubApplication
                HomeViewModel(
                    calendarRepository = app.container.calendarRepository,
                    projectRepository = app.container.projectRepository,
                    userPreferencesRepository = app.container.userPreferencesRepository,
                    exportRepository = app.container.exportRepository,
                    importRepository = app.container.importRepository,
                    taskRepository = app.container.taskRepository,
                    proposalRepository = app.container.proposalRepository,
                    knowledgeRepository = app.container.knowledgeRepository,
                    documentRepository = app.container.documentRepository,
                )
            }
        }
    }
}
