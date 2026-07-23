package com.shiro.yosugahub.ui.screen.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiro.yosugahub.ui.component.SectionCard
import com.shiro.yosugahub.ui.share.syncResultMessage

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    // 入力中のトークンは画面ローカルにのみ保持し、保存後は即クリアする。
    var tokenInput by remember { mutableStateOf("") }
    var syncTokenInput by remember { mutableStateOf("") }
    // 保存済みURLを初期値にする(uiState 反映後に一度だけ取り込む)。
    var syncUrlInput by remember(uiState.syncBaseUrl) { mutableStateOf(uiState.syncBaseUrl) }

    // Vault フォルダ選択。選択されたら読み書き権限を永続化して URI を保存する。
    val vaultPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.saveVaultUri(uri.toString())
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = "設定", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            SectionCard(title = "Obsidian Vault") {
                Text(
                    text = if (uiState.obsidianVaultUri.isEmpty()) {
                        "未設定。承認した知識のObsidian書き出し(targetNote)に使います。"
                    } else {
                        "選択中: ${vaultDisplayName(uiState.obsidianVaultUri)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vaultPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.obsidianVaultUri.isEmpty()) "Vaultフォルダを選択" else "Vaultフォルダを変更")
                }
            }
        }
        item {
            SectionCard(title = "Googleアカウント") {
                Text("未接続(Phase 4 で実装予定)", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            SectionCard(title = "カレンダー") {
                Text("取得期間: 過去7日〜未来7日", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            SectionCard(title = "サーバー同期(AI用JSON)") {
                Text(
                    text = "プロジェクト・タスク・知識・カレンダー・履歴の5つのJSONを生成し、" +
                        "ロリポップ上の受け口(server/ ディレクトリ参照)へ送信します。" +
                        "ChatGPTは api.php 経由でこれを読みます。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = syncUrlInput,
                    onValueChange = { syncUrlInput = it },
                    label = { Text("同期先URL(例: https://example.com/yosuga)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = syncTokenInput,
                    onValueChange = { syncTokenInput = it },
                    label = {
                        Text(if (uiState.hasSyncToken) "トークン(設定済み・変更する場合のみ)" else "トークン")
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedButton(
                        onClick = {
                            viewModel.saveSyncUrl(syncUrlInput)
                            if (syncTokenInput.isNotBlank()) {
                                viewModel.saveSyncToken(syncTokenInput) { saved ->
                                    Toast.makeText(
                                        context,
                                        if (saved) "同期設定を保存しました" else "トークンの保存に失敗しました",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    if (saved) syncTokenInput = ""
                                }
                            } else {
                                Toast.makeText(context, "URLを保存しました", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = syncUrlInput.isNotBlank() || syncTokenInput.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("設定を保存")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            syncTokenInput = viewModel.generateSyncToken()
                            Toast.makeText(
                                context,
                                "トークンを生成しました。同じ値をサーバーの config.php にも設定してください。",
                                Toast.LENGTH_LONG,
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("トークン生成")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.syncNow { result ->
                            Toast.makeText(context, syncResultMessage(result), Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !uiState.isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.isSyncing) "同期中..." else "今すぐ同期")
                }
            }
        }
        item {
            SectionCard(title = "GitHub アクセストークン") {
                Text(
                    text = if (uiState.hasGitHubToken) {
                        "設定済み(端末のKeystoreで暗号化して保存)"
                    } else {
                        "未設定。非公開リポジトリの .yosuga/status.json 取得に必要です。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("トークンを入力して保存") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedButton(
                        onClick = {
                            viewModel.saveGitHubToken(tokenInput) { saved ->
                                Toast.makeText(
                                    context,
                                    if (saved) "トークンを保存しました" else "保存に失敗しました",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                if (saved) tokenInput = ""
                            }
                        },
                        enabled = tokenInput.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("保存")
                    }
                    if (uiState.hasGitHubToken) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.clearGitHubToken()
                                tokenInput = ""
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("削除")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "GitHub の Fine-grained personal access token を、対象リポジトリの " +
                        "Contents: Read-only で作成してください。取得先の owner / リポジトリ名は " +
                        "各プロジェクトの編集画面で設定します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard(title = "JSON保存先") {
                Text(
                    "アプリ内の exports / imports に保存(保存先の選択は今後対応)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            SectionCard(title = "診断情報") {
                Text("アプリバージョン: 0.1.0", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** ツリーURIから表示用のフォルダ名を取り出す(例: primary:Obsidian/Vault → Obsidian/Vault)。 */
private fun vaultDisplayName(uriString: String): String {
    val lastSegment = Uri.parse(uriString).lastPathSegment ?: return uriString
    return lastSegment.substringAfter(':').ifEmpty { lastSegment }
}
