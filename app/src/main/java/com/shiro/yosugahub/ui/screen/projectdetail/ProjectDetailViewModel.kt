package com.shiro.yosugahub.ui.screen.projectdetail

import androidx.lifecycle.ViewModel
import com.shiro.yosugahub.ui.component.OpLogState
import com.shiro.yosugahub.ui.component.LogTone
import com.shiro.yosugahub.ui.component.LogLine
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.repository.ProjectRepository
import com.shiro.yosugahub.data.repository.ProjectStatusRepository
import com.shiro.yosugahub.data.repository.StatusFetchResult
import com.shiro.yosugahub.data.repository.StatusRefreshResult
import com.shiro.yosugahub.data.repository.SyncResult
import com.shiro.yosugahub.data.repository.TaskRepository
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.ProjectStatusSnapshot
import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus
import com.shiro.yosugahub.ui.navigation.ProjectDetailRoute
import kotlinx.coroutines.flow.MutableStateFlow
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
    val status: ProjectStatusSnapshot? = null,
    val isRefreshing: Boolean = false,
)

class ProjectDetailViewModel(
    private val projectId: String,
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val projectStatusRepository: ProjectStatusRepository,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)

    /** GitHub 取得の端末ログ(v5 UI)。 */
    val opLog = OpLogState()

    /** GitHub から status.json を取得しキャッシュを更新する(成功なら自動同期)。演出付き。 */
    fun refreshStatus(onResult: (StatusRefreshResult) -> Unit) {
        val project = uiState.value.project ?: return
        viewModelScope.launch {
            refreshing.value = true
            try {
                val result = opLog.run { emit ->
                    emit.emit(LogLine("> CONNECT github.com", LogTone.ACCENT))
                    emit.emit(
                        LogLine(
                            "  TARGET ${project.repoOwner.orEmpty()}/${project.repoName.orEmpty()}",
                            LogTone.INFO,
                        )
                    )
                    emit.emit(LogLine("  FETCH .yosuga/status.json …", LogTone.INFO))
                    val r = projectStatusRepository.refresh(project)
                    statusLines(r.fetch).forEach { emit.emit(it) }
                    syncLines(r.sync).forEach { emit.emit(it) }
                    emit.emit(LogLine("> DONE", LogTone.ACCENT))
                    r
                } ?: return@launch
                onResult(result)
            } finally {
                refreshing.value = false
            }
        }
    }

    private fun statusLines(result: StatusFetchResult): List<LogLine> = when (result) {
        is StatusFetchResult.Success -> listOf(
            LogLine("  200 OK", LogTone.OK),
            LogLine("  PARSE schemaVersion=1 … OK", LogTone.OK),
        )
        is StatusFetchResult.AuthFailed ->
            listOf(LogLine("  401/403 認証に失敗", LogTone.ERROR))
        is StatusFetchResult.FileNotFound ->
            listOf(LogLine("  404 status.json が見つかりません", LogTone.ERROR))
        is StatusFetchResult.NetworkError ->
            listOf(LogLine("  ERR 通信できません", LogTone.ERROR))
        is StatusFetchResult.HttpError ->
            listOf(LogLine("  HTTP ${result.statusCode}", LogTone.ERROR))
        is StatusFetchResult.InvalidJson ->
            listOf(LogLine("  PARSE 失敗: 型が合いません", LogTone.ERROR))
        is StatusFetchResult.UnsupportedSchema ->
            listOf(LogLine("  未対応の schemaVersion=${result.version}", LogTone.WARN))
        is StatusFetchResult.ProjectIdMismatch ->
            listOf(LogLine("  projectId 不一致: ${result.actual}", LogTone.WARN))
        is StatusFetchResult.NotConfigured ->
            listOf(LogLine("  リポジトリ未設定", LogTone.WARN))
        is StatusFetchResult.TokenMissing ->
            listOf(LogLine("  GitHub トークン未設定", LogTone.WARN))
    }

    /**
     * 取得後の自動同期の行。同期を使っていない場合は**何も出さない**
     * (未設定は失敗ではないので、毎回エラーめいた行を流さない)。
     */
    private fun syncLines(sync: SyncResult?): List<LogLine> = when (sync) {
        null, SyncResult.UrlNotConfigured, SyncResult.TokenMissing -> emptyList()
        is SyncResult.Success -> listOf(
            LogLine("> SYNC lolipop", LogTone.ACCENT),
            LogLine("  PUSH ${sync.fileCount} files … OK", LogTone.OK),
        )
        else -> listOf(
            LogLine("> SYNC lolipop", LogTone.ACCENT),
            LogLine("  PUSH 失敗(設定の「今すぐ同期」でやり直せます)", LogTone.ERROR),
        )
    }

    /** プロジェクト編集の保存(1-d)。lastUpdated は Repository が刻む。 */
    fun updateProject(project: Project) {
        viewModelScope.launch { projectRepository.upsert(project) }
    }

    /**
     * プロジェクトを削除する(v5 / 選択A の残り)。
     *
     * **タスクと進捗キャッシュを先に消してから本体を消す。**
     * 逆順だと、途中で失敗したときに親のいないタスクが残る。
     * 完了後に [onDeleted] で画面を閉じてもらう(消えた画面に留まらせない)。
     */
    fun deleteProject(onDeleted: () -> Unit) {
        val project = uiState.value.project ?: return
        viewModelScope.launch {
            taskRepository.deleteByProject(project.id)
            projectStatusRepository.deleteCache(project.id)
            projectRepository.delete(project.id)
            onDeleted()
        }
    }

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
        projectStatusRepository.statuses(),
        refreshing,
    ) { projects, tasks, statuses, isRefreshing ->
        ProjectDetailUiState(
            isLoading = false,
            project = projects.firstOrNull { it.id == projectId },
            tasks = groupTasks(tasks),
            status = statuses[projectId],
            isRefreshing = isRefreshing,
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
                    projectStatusRepository = app.container.projectStatusRepository,
                )
            }
        }
    }
}
