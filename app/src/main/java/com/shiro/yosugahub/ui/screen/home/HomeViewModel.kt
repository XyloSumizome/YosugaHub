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
import com.shiro.yosugahub.data.repository.ExportRepository
import com.shiro.yosugahub.data.repository.ExportResult
import com.shiro.yosugahub.data.repository.ImportRepository
import com.shiro.yosugahub.data.repository.ImportResult
import com.shiro.yosugahub.data.repository.ProjectRepository
import com.shiro.yosugahub.domain.model.CalendarEvent
import com.shiro.yosugahub.domain.model.Project
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ホーム画面が監視するUI状態。 */
data class HomeUiState(
    val today: String = "",
    val lastSyncedAt: String = "",
    val priorityTask: String = "",
    val todayEvents: List<CalendarEvent> = emptyList(),
    val nextEvent: CalendarEvent? = null,
    val projects: List<Project> = emptyList(),
)

class HomeViewModel(
    private val calendarRepository: CalendarRepository,
    private val projectRepository: ProjectRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
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

    val uiState: StateFlow<HomeUiState> = combine(
        calendarRepository.todayEvents(),
        calendarRepository.upcomingEvents(),
        projectRepository.projects(),
        userPreferencesRepository.lastSyncedAt,
    ) { todayEvents, upcomingEvents, projects, lastSyncedAt ->
        HomeUiState(
            today = calendarRepository.today,
            lastSyncedAt = lastSyncedAt.ifEmpty { "未同期" },
            priorityTask = projectRepository.priorityTask,
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
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YosugaHubApplication
                HomeViewModel(
                    calendarRepository = app.container.calendarRepository,
                    projectRepository = app.container.projectRepository,
                    userPreferencesRepository = app.container.userPreferencesRepository,
                    exportRepository = app.container.exportRepository,
                    importRepository = app.container.importRepository,
                )
            }
        }
    }
}
