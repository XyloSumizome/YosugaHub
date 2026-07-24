package com.shiro.yosugahub.ui.screen.projectdetail

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiro.yosugahub.ui.component.TacticalOutlinedButton
import com.shiro.yosugahub.domain.model.ProjectStatusSnapshot
import com.shiro.yosugahub.domain.model.StatusLine
import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus
import com.shiro.yosugahub.ui.component.SectionCard
import com.shiro.yosugahub.ui.component.StatusTag
import com.shiro.yosugahub.ui.component.healthLabel
import com.shiro.yosugahub.ui.share.statusFetchMessage

/** プロジェクト詳細(1-b: 表示 / 1-c: タスクの追加・編集・完了・削除)。 */
@Composable
fun ProjectDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectDetailViewModel = viewModel(factory = ProjectDetailViewModel.Factory),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showNewTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showProjectEditDialog by remember { mutableStateOf(false) }
    var showProjectDeleteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                }
                Text(
                    text = uiState.project?.name ?: "プロジェクト",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        val project = uiState.project
        if (project == null) {
            if (!uiState.isLoading) {
                item {
                    Text(
                        text = "プロジェクトが見つかりません",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            return@LazyColumn
        }

        item {
            SectionCard(title = "プロジェクト情報") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 目標が空なら見出しだけ出さない(A運用では進捗はGitHub側にある)。
                        Text(
                            text = if (project.currentGoal.isNotBlank()) {
                                "目標: ${project.currentGoal}"
                            } else {
                                "目標: 未設定"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (project.currentGoal.isNotBlank()) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        StatusTag(healthLabel(project.health))
                    }
                    // 作業中 / 次 はタスクから導出(案C)。無ければ行ごと隠す。
                    if (project.inProgress.isNotBlank()) {
                        Text(
                            text = "作業中: ${project.inProgress}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (project.nextTask.isNotBlank()) {
                        Text(
                            text = "次: ${project.nextTask}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (project.inProgress.isBlank() && project.nextTask.isBlank()) {
                        Text(
                            text = "残タスク・進捗は下の「GitHub 進捗」に表示されます。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "最終更新: ${project.lastUpdated}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showProjectEditDialog = true }) {
                            Text("プロジェクトを編集")
                        }
                        TextButton(onClick = { showProjectDeleteDialog = true }) {
                            Text("削除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        item {
            GitHubStatusCard(
                project = project,
                status = uiState.status,
                isRefreshing = uiState.isRefreshing,
                onRefresh = {
                    viewModel.refreshStatus { result ->
                        Toast.makeText(context, statusFetchMessage(result), Toast.LENGTH_LONG).show()
                    }
                },
            )
        }

        item {
            TacticalOutlinedButton(
                onClick = { showNewTaskDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("タスクを追加")
            }
        }

        if (uiState.tasks.isEmpty) {
            item {
                SectionCard(title = "タスク") {
                    Text(text = "タスクはまだありません", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            taskSection(
                title = "進行中",
                tasks = uiState.tasks.doing,
                onToggleDone = viewModel::setTaskDone,
                onClickTask = { editingTask = it },
            )
            taskSection(
                title = "未着手",
                tasks = uiState.tasks.todo,
                onToggleDone = viewModel::setTaskDone,
                onClickTask = { editingTask = it },
            )
            taskSection(
                title = "完了",
                tasks = uiState.tasks.done,
                onToggleDone = viewModel::setTaskDone,
                onClickTask = { editingTask = it },
            )
        }
    }

    if (showProjectDeleteDialog) {
        uiState.project?.let { project ->
            AlertDialog(
                onDismissRequest = { showProjectDeleteDialog = false },
                title = { Text("プロジェクトを削除") },
                text = {
                    Text(
                        "「${project.name}」と、このプロジェクトのタスク${uiState.tasks.size}件、" +
                            "GitHubから取得した進捗を削除します。元に戻せません。",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showProjectDeleteDialog = false
                            // 消えた画面に留まらせず、一覧へ戻る。
                            viewModel.deleteProject(onDeleted = onBack)
                        },
                    ) {
                        Text("削除する", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProjectDeleteDialog = false }) { Text("やめる") }
                },
            )
        }
    }

    if (showProjectEditDialog) {
        uiState.project?.let { project ->
            ProjectEditDialog(
                original = project,
                onDismiss = { showProjectEditDialog = false },
                onSave = { name, currentGoal, health, repoOwner, repoName, repoBranch ->
                    viewModel.updateProject(
                        project.copy(
                            name = name,
                            currentGoal = currentGoal,
                            health = health,
                            repoOwner = repoOwner,
                            repoName = repoName,
                            repoBranch = repoBranch,
                        )
                    )
                    showProjectEditDialog = false
                },
            )
        }
    }

    if (showNewTaskDialog) {
        TaskEditDialog(
            original = null,
            onDismiss = { showNewTaskDialog = false },
            onSave = { title, detail, priority, dueDate, status ->
                viewModel.addTask(title, detail, priority, dueDate, status)
                showNewTaskDialog = false
            },
        )
    }

    editingTask?.let { task ->
        TaskEditDialog(
            original = task,
            onDismiss = { editingTask = null },
            onSave = { title, detail, priority, dueDate, status ->
                viewModel.updateTask(
                    task.copy(
                        title = title,
                        detail = detail,
                        priority = priority,
                        dueDate = dueDate,
                        status = status,
                    )
                )
                editingTask = null
            },
            onDelete = {
                viewModel.deleteTask(task.id)
                editingTask = null
            },
        )
    }
}

/**
 * GitHub 由来の進捗(.yosuga/status.json)。
 * projects テーブルは上書きせず、独立したセクションとして表示する。
 */
@Composable
private fun GitHubStatusCard(
    project: com.shiro.yosugahub.domain.model.Project,
    status: ProjectStatusSnapshot?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(title = "GitHub 進捗", modifier = modifier) {
        Column {
            when {
                !project.hasRepository -> Text(
                    text = "リポジトリ未設定。「プロジェクトを編集」で owner とリポジトリ名を入力すると、" +
                        ".yosuga/status.json を取得できます。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                status == null -> Text(
                    text = "まだ取得していません。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> {
                    if (status.summary.isNotBlank()) {
                        Text(text = status.summary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (status.goalTitle.isNotBlank()) {
                        Text(
                            text = "目標: ${status.goalTitle}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    StatusLineGroup("作業中", status.inProgress)
                    StatusLineGroup("次のタスク", status.nextTasks)
                    StatusLineGroup("ブロッカー", status.blockers)
                    StatusLineGroup("決定事項", status.decisions)
                    if (status.questionsForYosuga.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "ヨスガへの質問", style = MaterialTheme.typography.titleSmall)
                        status.questionsForYosuga.forEach { question ->
                            Text(text = "・$question", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buildList {
                            add("取得: ${status.fetchedAt.take(16).replace('T', ' ')}")
                            if (status.generatedAt.isNotBlank()) {
                                add("生成: ${status.generatedAt.take(16).replace('T', ' ')}")
                            }
                            if (status.health.isNotBlank()) add("状態: ${status.health}")
                        }.joinToString(" / "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (project.hasRepository) {
                Spacer(modifier = Modifier.height(8.dp))
                TacticalOutlinedButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isRefreshing) "取得中..." else "GitHubから更新")
                }
            }
        }
    }
}

@Composable
private fun StatusLineGroup(title: String, lines: List<StatusLine>) {
    if (lines.isEmpty()) return
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = title, style = MaterialTheme.typography.titleSmall)
    lines.forEach { line ->
        Text(text = "・${line.title}", style = MaterialTheme.typography.bodyMedium)
        if (line.detail.isNotBlank()) {
            Text(
                text = "　${line.detail}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.taskSection(
    title: String,
    tasks: List<Task>,
    onToggleDone: (id: String, done: Boolean) -> Unit,
    onClickTask: (Task) -> Unit,
) {
    if (tasks.isEmpty()) return
    item {
        SectionCard(title = "$title(${tasks.size})") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tasks.forEach { task ->
                    TaskRow(
                        task = task,
                        onToggleDone = { checked -> onToggleDone(task.id, checked) },
                        onClick = { onClickTask(task) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onToggleDone: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val done = task.status == TaskStatus.DONE
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = done, onCheckedChange = onToggleDone)
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
            )
            val meta = buildList {
                add("優先度: ${priorityLabel(task.priority)}")
                task.dueDate?.let { add("締切: $it") }
                if (task.detail.isNotEmpty()) add(task.detail)
            }.joinToString(" / ")
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun priorityLabel(priority: String): String = when (priority) {
    "high" -> "高"
    "medium" -> "中"
    "low" -> "低"
    else -> priority
}
