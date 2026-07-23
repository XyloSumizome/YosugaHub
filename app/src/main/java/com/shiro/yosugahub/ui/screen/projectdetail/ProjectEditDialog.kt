package com.shiro.yosugahub.ui.screen.projectdetail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.ui.component.healthLabel

/**
 * プロジェクト編集ダイアログ(1-d + GitHub連携)。
 * 編集対象は name / currentGoal / health と GitHub リポジトリ情報。
 * inProgress / nextTask はタスクからの導出へ置き換える予定のため編集させない。
 */
@Composable
fun ProjectEditDialog(
    original: Project,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        currentGoal: String,
        health: String,
        repoOwner: String?,
        repoName: String?,
        repoBranch: String?,
    ) -> Unit,
) {
    var name by remember(original) { mutableStateOf(original.name) }
    var currentGoal by remember(original) { mutableStateOf(original.currentGoal) }
    var health by remember(original) { mutableStateOf(original.health) }
    var repoOwner by remember(original) { mutableStateOf(original.repoOwner.orEmpty()) }
    var repoName by remember(original) { mutableStateOf(original.repoName.orEmpty()) }
    var repoBranch by remember(original) { mutableStateOf(original.repoBranch.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("プロジェクトを編集") },
        text = {
            // 項目が増えたためダイアログ内をスクロール可能にする
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("プロジェクト名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = currentGoal,
                    onValueChange = { currentGoal = it },
                    label = { Text("目標") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(text = "状態", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HEALTH_OPTIONS.forEach { option ->
                        FilterChip(
                            selected = health == option,
                            onClick = { health = option },
                            label = { Text(healthLabel(option)) },
                        )
                    }
                }
                Text(
                    text = "GitHub(.yosuga/status.json の取得先。空欄なら取得しない)",
                    style = MaterialTheme.typography.labelMedium,
                )
                OutlinedTextField(
                    value = repoOwner,
                    onValueChange = { repoOwner = it },
                    label = { Text("owner") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = repoName,
                    onValueChange = { repoName = it },
                    label = { Text("リポジトリ名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = repoBranch,
                    onValueChange = { repoBranch = it },
                    label = { Text("ブランチ(空欄なら main)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        name.trim(),
                        currentGoal.trim(),
                        health,
                        repoOwner.trim().ifEmpty { null },
                        repoName.trim().ifEmpty { null },
                        repoBranch.trim().ifEmpty { null },
                    )
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

/** 手動編集で選べる状態(設計書v2の語彙)。AI分析由来の自由な値は表示のみで受け入れる。 */
private val HEALTH_OPTIONS = listOf("on_track", "attention", "blocked", "paused")
