package com.shiro.yosugahub.ui.screen.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.repository.ProjectRepository
import com.shiro.yosugahub.domain.model.Project
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** プロジェクト一覧画面が監視するUI状態。 */
data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
)

class ProjectsViewModel(
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    val uiState: StateFlow<ProjectsUiState> = projectRepository.projects()
        .map { ProjectsUiState(projects = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProjectsUiState(),
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YosugaHubApplication
                ProjectsViewModel(app.container.projectRepository)
            }
        }
    }
}
