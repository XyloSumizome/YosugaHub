package com.shiro.yosugahub.ui.screen.obsidiancontext

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiro.yosugahub.data.obsidian.ContextMarkdown
import com.shiro.yosugahub.data.obsidian.NoteFilter
import com.shiro.yosugahub.data.obsidian.VaultNote
import com.shiro.yosugahub.data.obsidian.VaultNoteFilters
import com.shiro.yosugahub.data.repository.ContextBuildResult
import com.shiro.yosugahub.ui.share.shareMarkdownText

/**
 * Obsidian から必要な範囲だけ選んで、ヨスガへ貼るコンテキストを作る画面(設計書v5 Phase 1-b)。
 * 要約はしない。**選ぶ・見る**までがこの画面の責任で、コピー/保存/共有は 1-c で足す。
 */
@Composable
fun ObsidianContextScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ObsidianContextViewModel = viewModel(factory = ObsidianContextViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // 保存先はその場でユーザーが選ぶ(FileProvider も追加権限も不要)。
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri ->
        if (uri != null) {
            viewModel.saveTo(uri) { saved ->
                val message = if (saved) "保存しました" else "保存に失敗しました"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("戻る") }
            Text(
                text = "Obsidianから文脈を作る",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            TextButton(
                onClick = viewModel::refresh,
                enabled = uiState.loadState != VaultLoadState.LOADING,
            ) { Text("再読込") }
        }
        HorizontalDivider()

        when (uiState.loadState) {
            VaultLoadState.LOADING, VaultLoadState.IDLE -> CenterMessage {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Vaultを読み込んでいます…")
            }

            VaultLoadState.NOT_CONFIGURED -> CenterMessage {
                Text("Vaultフォルダが未選択です。", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "設定 → Obsidian Vault からフォルダを選んでください。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            VaultLoadState.FAILED -> CenterMessage {
                Text(
                    text = uiState.errorMessage.ifBlank { "Vaultの読み取りに失敗しました。" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = viewModel::refresh) { Text("もう一度試す") }
            }

            VaultLoadState.LOADED -> if (uiState.notes.isEmpty()) {
                CenterMessage {
                    Text("Markdownが1件も見つかりませんでした。")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "選んだフォルダが Vault のルートか確認してください。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                FilterBar(
                    filter = uiState.filter,
                    onQueryChange = viewModel::setQuery,
                    onRecentDays = viewModel::setRecentDays,
                    onClear = viewModel::clearFilter,
                )
                if (uiState.visible.isEmpty()) {
                    CenterMessage(modifier = Modifier.weight(1f)) {
                        Text("条件に合うノートがありません。")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = viewModel::clearFilter) { Text("絞り込みを解除") }
                    }
                } else {
                    NoteList(
                        uiState = uiState,
                        onToggle = viewModel::toggle,
                        onToggleFolder = viewModel::toggleFolder,
                        modifier = Modifier.weight(1f),
                    )
                }
                SelectionBar(
                    uiState = uiState,
                    onClear = viewModel::clearSelection,
                    onBuild = viewModel::buildPreview,
                )
            }
        }
    }

    uiState.preview?.let { preview ->
        PreviewDialog(
            preview = preview,
            onDismiss = viewModel::dismissPreview,
            onCopy = {
                clipboard.setText(AnnotatedString(preview.markdown))
                Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
            },
            onSave = { saveLauncher.launch(preview.fileName) },
            onShare = {
                shareMarkdownText(context, preview.markdown, subject = preview.fileName)
            },
        )
    }
}

/** 絞り込み。ファイルを開かずに判定できる条件だけを置く(設計書v5 Phase 2)。 */
@Composable
private fun FilterBar(
    filter: NoteFilter,
    onQueryChange: (String) -> Unit,
    onRecentDays: (Int?) -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = filter.query,
            onValueChange = onQueryChange,
            label = { Text("パス・ファイル名で絞り込む") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VaultNoteFilters.RECENT_DAY_OPTIONS.forEach { days ->
                FilterChip(
                    selected = filter.recentDays == days,
                    onClick = { onRecentDays(days) },
                    label = { Text("${days}日以内") },
                )
            }
            if (filter.isActive) {
                TextButton(onClick = onClear) { Text("解除") }
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun NoteList(
    uiState: ObsidianContextUiState,
    onToggle: (VaultNote) -> Unit,
    onToggleFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        uiState.grouped.forEach { (folder, notes) ->
            item(key = "folder:$folder") {
                val allSelected = notes.all { it.relativePath in uiState.selected }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onToggleFolder(folder) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = folder.ifBlank { "(Vault直下)" },
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (allSelected) "解除" else "すべて",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            items(notes, key = { it.relativePath }) { note ->
                val checked = note.relativePath in uiState.selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(note) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = { onToggle(note) })
                    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${note.size} B",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(
    uiState: ObsidianContextUiState,
    onClear: () -> Unit,
    onBuild: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${uiState.selectedCount} / ${uiState.notes.size} 件",
                    style = MaterialTheme.typography.bodyMedium,
                )
                // 絞り込みで見えていない選択があると件数が合わなく見えるため明示する。
                if (uiState.hiddenSelectedCount > 0) {
                    Text(
                        text = "うち${uiState.hiddenSelectedCount}件は絞り込みで非表示",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            TextButton(onClick = onClear, enabled = uiState.selectedCount > 0) {
                Text("選択解除")
            }
            Button(onClick = onBuild, enabled = uiState.canBuild) {
                Text(if (uiState.isBuilding) "生成中…" else "まとめる")
            }
        }
    }
}

@Composable
private fun PreviewDialog(
    preview: ContextBuildResult,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(preview.fileName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${preview.noteCount}件 / ${preview.charCount}文字",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (preview.isLarge) {
                    Text(
                        text = "${ContextMarkdown.WARN_CHAR_COUNT}文字を超えています。" +
                            "貼り付け先で切れる場合は選択を減らしてください。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (preview.skipped.isNotEmpty()) {
                    Text(
                        text = "読めなかったノート: ${preview.skipped.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Text(
                    text = preview.markdown,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = onCopy, modifier = Modifier.weight(1f)) { Text("コピー") }
                    OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Text("保存")
                    }
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                        Text("共有")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("閉じる") }
                }
            }
        }
    }
}

@Composable
private fun CenterMessage(
    modifier: Modifier = Modifier.fillMaxSize(),
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}
