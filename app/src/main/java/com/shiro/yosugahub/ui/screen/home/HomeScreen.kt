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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiro.yosugahub.ui.component.EventRow
import com.shiro.yosugahub.ui.component.PasteImportDialog
import com.shiro.yosugahub.ui.component.SectionCard
import com.shiro.yosugahub.ui.component.inProgressLine
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

    var showPasteDialog by remember { mutableStateOf(false) }
    if (showPasteDialog) {
        PasteImportDialog(
            onDismiss = { showPasteDialog = false },
            onImport = { text ->
                showPasteDialog = false
                viewModel.importResponseText(text) { result ->
                    Toast.makeText(context, importResultMessage(result), Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

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
        if (uiState.pendingProposalCount > 0) {
            item {
                SectionCard(title = "承認待ちの提案") {
                    Text(
                        text = "ヨスガからの提案が ${uiState.pendingProposalCount} 件あります。" +
                            "ヨスガ画面で確認してください。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (uiState.documentsNeedingReviewCount > 0) {
            item {
                SectionCard(title = "確認待ちの文書") {
                    Text(
                        text = "ヨスガが分類した文書が ${uiState.documentsNeedingReviewCount} 件あります。" +
                            "記録タブの「文書」で確認してください。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item {
            SectionCard(title = "今日やること") {
                if (uiState.todayTasks.isEmpty()) {
                    Text(
                        text = "未完了のタスクはありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val projectNames = uiState.projects.associate { it.id to it.name }
                    uiState.todayTasks.forEach { task ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
                            val meta = buildList {
                                task.projectId?.let { add(projectNames[it] ?: it) }
                                add(priorityLabel(task.priority))
                                task.dueDate?.let { add("締切: $it") }
                            }.joinToString(" / ")
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
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
                            text = inProgressLine(project.inProgress),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        if (uiState.recentDecisions.isNotEmpty()) {
            item {
                SectionCard(title = "最近の決定") {
                    uiState.recentDecisions.forEach { decision ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(text = decision.title, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = decision.createdAt.take(10),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item {
            Column {
                Button(onClick = createExport, modifier = Modifier.fillMaxWidth()) {
                    Text("ChatGPT用JSONを作成")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = importResponse, modifier = Modifier.fillMaxWidth()) {
                    Text("回答JSONを取り込む(ファイル)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showPasteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("回答JSONを貼り付けて取り込む")
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

private fun priorityLabel(priority: String): String = when (priority) {
    "high" -> "優先度: 高"
    "medium" -> "優先度: 中"
    "low" -> "優先度: 低"
    else -> "優先度: $priority"
}
