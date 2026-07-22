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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.data.model.DummyData
import com.shiro.yosugahub.data.model.DummyProject

@Composable
fun ProjectsScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = "プロジェクト", style = MaterialTheme.typography.headlineSmall)
        }
        items(DummyData.projects) { project ->
            ProjectCard(project)
        }
    }
}

@Composable
private fun ProjectCard(project: DummyProject, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
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
            Text(text = "目標: ${project.currentGoal}", style = MaterialTheme.typography.bodyMedium)
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

private fun healthLabel(health: String): String = when (health) {
    "on_track" -> "順調"
    "attention" -> "要確認"
    "blocked" -> "停滞"
    "paused" -> "休止中"
    else -> health
}
