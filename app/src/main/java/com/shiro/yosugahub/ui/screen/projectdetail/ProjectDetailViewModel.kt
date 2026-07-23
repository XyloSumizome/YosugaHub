package com.shiro.yosugahub.ui.screen.projectdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.repository.ProjectRepository
import com.shiro.yosugahub.data.repository.TaskRepository
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus
import com.shiro.yosugahub.ui.navigation.ProjectDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** プロジェクト詳細画面が監視するUI状態。 */
data class ProjectDetailUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val tasks: GroupedTasks = GroupedTasks(),
)

class ProjectDetailViewModel(
    private val projectId: String,
    projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    /** 新規タスクを追加する(このプロジェクトに紐付け)。 */
    fun addTask(title: String, detail: String, priority: String, dueDate: String?, status: TaskStatus) {
        viewModelScope.launch {
            taskRepository.create(
                projectId = projectId,
                title = title,
                detail = detail,
                priority = priority,
                dueDate = dueDate,
                status = status,
            )
        }
    }

    /** 編集済みタスクを保存する(updatedAt / completedAt は Repository が整合させる)。 */
    fun updateTask(task: Task) {
        viewModelScope.launch { taskRepository.upsert(task) }
    }

    /** チェックボックスによる完了⇄未着手の切り替え。 */
    fun setTaskDone(id: String, done: Boolean) {
        viewModelScope.launch {
            taskRepository.setStatus(id, if (done) TaskStatus.DONE else TaskStatus.TODO)
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch { taskRepository.delete(id) }
    }

    val uiState: StateFlow<ProjectDetailUiState> = combine(
        projectRepository.projects(),
        taskRepository.tasksForProject(projectId),
    ) { projects, tasks ->
        ProjectDetailUiState(
            isLoading = false,
            project = projects.firstOrNull { it.id == projectId },
            tasks = groupTasks(tasks),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProjectDetailUiState(),
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YosugaHubApplication
                val savedStateHandle = createSavedStateHandle()
                ProjectDetailViewModel(
                    projectId = checkNotNull(savedStateHandle[ProjectDetailRoute.ARG_PROJECT_ID]),
                    projectRepository = app.container.projectRepository,
                    taskRepository = app.container.taskRepository,
                )
            }
        }
    }
}
