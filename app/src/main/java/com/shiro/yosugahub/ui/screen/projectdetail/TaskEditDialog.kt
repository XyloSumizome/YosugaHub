package com.shiro.yosugahub.ui.screen.projectdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus

/**
 * タスクの新規作成・編集ダイアログ(v3-Step 1-c)。
 * original が null なら新規。編集時のみ削除ボタンを表示する。
 */
@Composable
fun TaskEditDialog(
    original: Task?,
    onDismiss: () -> Unit,
    onSave: (title: String, detail: String, priority: String, dueDate: String?, status: TaskStatus) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var title by remember(original) { mutableStateOf(original?.title ?: "") }
    var detail by remember(original) { mutableStateOf(original?.detail ?: "") }
    var priority by remember(original) { mutableStateOf(original?.priority ?: "medium") }
    var status by remember(original) { mutableStateOf(original?.status ?: TaskStatus.TODO) }
    var dueDateInput by remember(original) { mutableStateOf(original?.dueDate ?: "") }

    val dueDateValid = isValidDueDateInput(dueDateInput)
    val canSave = title.isNotBlank() && dueDateValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (original == null) "タスクを追加" else "タスクを編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("タイトル") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("詳細(任意)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(text = "優先度", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityChip("高", "high", priority) { priority = it }
                    PriorityChip("中", "medium", priority) { priority = it }
                    PriorityChip("低", "low", priority) { priority = it }
                }
                Text(text = "状態", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip("未着手", TaskStatus.TODO, status) { status = it }
                    StatusChip("進行中", TaskStatus.DOING, status) { status = it }
                    StatusChip("完了", TaskStatus.DONE, status) { status = it }
                }
                OutlinedTextField(
                    value = dueDateInput,
                    onValueChange = { dueDateInput = it },
                    label = { Text("締切(任意・yyyy-MM-dd)") },
                    singleLine = true,
                    isError = !dueDateValid,
                    supportingText = if (!dueDateValid) {
                        { Text("例: 2026-07-31 の形式で入力") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("このタスクを削除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(title.trim(), detail.trim(), priority, dueDateForSave(dueDateInput), status)
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

@Composable
private fun PriorityChip(
    label: String,
    value: String,
    selected: String,
    onSelect: (String) -> Unit,
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
    )
}

@Composable
private fun StatusChip(
    label: String,
    value: TaskStatus,
    selected: TaskStatus,
    onSelect: (TaskStatus) -> Unit,
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
    )
}
