package com.shiro.yosugahub.ui.screen.projects

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
import androidx.compose.foundation.lazy.items
import android.widget.Toast
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.ui.component.healthLabel
import com.shiro.yosugahub.ui.component.inProgressLine
import com.shiro.yosugahub.ui.share.statusRefreshSummary

@Composable
fun ProjectsScreen(
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectsViewModel = viewModel(factory = ProjectsViewModel.Factory),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = "プロジェクト", style = MaterialTheme.typography.headlineSmall)
        }
        if (uiState.hasAnyRepository) {
            item {
                OutlinedButton(
                    onClick = {
                        viewModel.refreshAll { results ->
                            Toast.makeText(
                                context,
                                statusRefreshSummary(results),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                    enabled = !uiState.isRefreshing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.isRefreshing) "取得中..." else "GitHubからすべて更新")
                }
            }
        }
        items(uiState.projects) { project ->
            ProjectCard(project = project, onClick = { onProjectClick(project.id) })
        }
    }
}

@Composable
private fun ProjectCard(project: Project, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = project.name, style = MaterialTheme.typography.titleMedium)
                AssistChip(
                    onClick = {},
                    label = { Text(healthLabel(project.health)) },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (project.currentGoal.isNotBlank()) {
                Text(
                    text = "目標: ${project.currentGoal}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(text = inProgressLine(project.inProgress), style = MaterialTheme.typography.bodyMedium)
            if (project.nextTask.isNotBlank()) {
                Text(text = "次: ${project.nextTask}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "最終更新: ${project.lastUpdated}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
