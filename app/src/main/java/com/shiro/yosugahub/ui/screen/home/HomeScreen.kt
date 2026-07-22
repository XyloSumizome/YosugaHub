package com.shiro.yosugahub.ui.screen.home

import android.widget.Toast
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.data.model.DummyData
import com.shiro.yosugahub.ui.component.EventRow
import com.shiro.yosugahub.ui.component.SectionCard

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val notYet = { Toast.makeText(context, "Phase 2 で実装予定です", Toast.LENGTH_SHORT).show() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = DummyData.today,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        item {
            SectionCard(title = "今日の予定") {
                DummyData.todayEvents.forEach { EventRow(it) }
            }
        }
        item {
            SectionCard(title = "次の予定") {
                EventRow(DummyData.upcomingEvents.first())
            }
        }
        item {
            SectionCard(title = "ゲーム進捗") {
                DummyData.projects.forEach { project ->
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
                Text(text = DummyData.priorityTask, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            Column {
                Button(onClick = notYet, modifier = Modifier.fillMaxWidth()) {
                    Text("ChatGPT用JSONを作成")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = notYet, modifier = Modifier.fillMaxWidth()) {
                    Text("ChatGPT回答JSONを取り込む")
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "最終同期: ${DummyData.lastSyncedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
