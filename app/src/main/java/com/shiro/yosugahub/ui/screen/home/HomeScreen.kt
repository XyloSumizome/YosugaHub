package com.shiro.yosugahub.ui.screen.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
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
import com.shiro.yosugahub.ui.component.EventRow
import com.shiro.yosugahub.ui.component.SectionCard
import com.shiro.yosugahub.ui.share.importResultMessage
import com.shiro.yosugahub.ui.share.shareJsonText

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val createExport = {
        viewModel.createExport { result ->
            result
                .onSuccess { export ->
                    Toast.makeText(context, "保存しました: ${export.fileName}", Toast.LENGTH_SHORT).show()
                    shareJsonText(context, export.json)
                }
                .onFailure {
                    Toast.makeText(context, "JSONの作成に失敗しました", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importResponse(uri) { result ->
                Toast.makeText(context, importResultMessage(result), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importResponse = { importLauncher.launch(arrayOf("application/json", "text/plain")) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = uiState.today,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        item {
            SectionCard(title = "今日の予定") {
                uiState.todayEvents.forEach { EventRow(it) }
            }
        }
        item {
            SectionCard(title = "次の予定") {
                uiState.nextEvent?.let { EventRow(it) }
            }
        }
        item {
            SectionCard(title = "ゲーム進捗") {
                uiState.projects.forEach { project ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(text = project.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "作業中: ${project.inProgress}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        item {
            SectionCard(title = "優先タスク") {
                Text(text = uiState.priorityTask, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            Column {
                Button(onClick = createExport, modifier = Modifier.fillMaxWidth()) {
                    Text("ChatGPT用JSONを作成")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = importResponse, modifier = Modifier.fillMaxWidth()) {
                    Text("ChatGPT回答JSONを取り込む")
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "最終同期: ${uiState.lastSyncedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
