package com.shiro.yosugahub.ui.screen.projectdetail

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.ui.component.SectionCard
import com.shiro.yosugahub.ui.component.healthLabel

/** プロジェクト詳細(v3-Step 1-b: 読み取りのみ。編集は 1-c / 1-d で追加)。 */
@Composable
fun ProjectDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectDetailViewModel = viewModel(factory = ProjectDetailViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()

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
                        Text(text = "目標: ${project.currentGoal}", style = MaterialTheme.typography.bodyMedium)
                        AssistChip(onClick = {}, label = { Text(healthLabel(project.health)) })
                    }
                    Text(text = "作業中: ${project.inProgress}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "次: ${project.nextTask}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "最終更新: ${project.lastUpdated}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (uiState.tasks.isEmpty) {
            item {
                SectionCard(title = "タスク") {
                    Text(text = "タスクはまだありません", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            taskSection(title = "進行中", tasks = uiState.tasks.doing)
            taskSection(title = "未着手", tasks = uiState.tasks.todo)
            taskSection(title = "完了", tasks = uiState.tasks.done, done = true)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.taskSection(
    title: String,
    tasks: List<Task>,
    done: Boolean = false,
) {
    if (tasks.isEmpty()) return
    item {
        SectionCard(title = "$title(${tasks.size})") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tasks.forEach { task ->
                    TaskRow(task = task, done = done)
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, done: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
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

private fun priorityLabel(priority: String): String = when (priority) {
    "high" -> "高"
    "medium" -> "中"
    "low" -> "低"
    else -> priority
}
