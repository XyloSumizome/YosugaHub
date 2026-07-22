package com.shiro.yosugahub.ui.screen.assistant

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.shiro.yosugahub.ui.component.SectionCard
import com.shiro.yosugahub.ui.share.importResultMessage
import com.shiro.yosugahub.ui.share.shareJsonText

@Composable
fun AssistantScreen(
    modifier: Modifier = Modifier,
    viewModel: AssistantViewModel = viewModel(factory = AssistantViewModel.Factory),
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = "よすが連携", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            SectionCard(title = "状況JSON") {
                Text(
                    text = "カレンダーとゲーム進捗をまとめたJSONを作成し、" +
                        "共有メニューまたはファイルとしてChatGPTへ渡します。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = createExport, modifier = Modifier.fillMaxWidth()) {
                    Text("状況JSONを作成")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = importResponse, modifier = Modifier.fillMaxWidth()) {
                    Text("回答JSONを取り込む")
                }
            }
        }
        item {
            Text(text = "受け取った提案", style = MaterialTheme.typography.titleMedium)
        }
        items(uiState.recommendations) { recommendation ->
            SectionCard(title = recommendation.title) {
                Text(
                    text = recommendation.detail,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "対象: ${recommendation.projectId} / 優先度: ${recommendation.priority}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
