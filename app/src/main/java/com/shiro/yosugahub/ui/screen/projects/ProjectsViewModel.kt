package com.shiro.yosugahub.ui.screen.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.repository.ProjectRepository
import com.shiro.yosugahub.data.repository.ProjectStatusRepository
import com.shiro.yosugahub.data.repository.StatusFetchResult
import com.shiro.yosugahub.domain.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** プロジェクト一覧画面が監視するUI状態。 */
data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val isRefreshing: Boolean = false,
    /** GitHub 取得先が設定されているプロジェクトがあるか(更新ボタンの表示判定)。 */
    val hasAnyRepository: Boolean = false,
)

class ProjectsViewModel(
    private val projectRepository: ProjectRepository,
    private val projectStatusRepository: ProjectStatusRepository,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)

    /** リポジトリ設定済みの全プロジェクトを GitHub から更新する。 */
    fun refreshAll(onResult: (List<StatusFetchResult>) -> Unit) {
        val projects = uiState.value.projects
        viewModelScope.launch {
            refreshing.value = true
            try {
                onResult(projectStatusRepository.refreshAll(projects))
            } finally {
                refreshing.value = false
            }
        }
    }

    val uiState: StateFlow<ProjectsUiState> = combine(
        projectRepository.projects(),
        refreshing,
    ) { projects, isRefreshing ->
        ProjectsUiState(
            projects = projects,
            isRefreshing = isRefreshing,
            hasAnyRepository = projects.any { it.hasRepository },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProjectsUiState(),
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YosugaHubApplication
                ProjectsViewModel(
                    projectRepository = app.container.projectRepository,
                    projectStatusRepository = app.container.projectStatusRepository,
                )
            }
        }
    }
}
