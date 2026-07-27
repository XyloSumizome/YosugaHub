package com.shiro.yosugahub.ui.screen.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiro.yosugahub.ui.component.DialogAction
import com.shiro.yosugahub.ui.component.SectionCard
import com.shiro.yosugahub.ui.component.TacticalButton
import com.shiro.yosugahub.ui.component.TacticalOutlinedButton
import com.shiro.yosugahub.ui.component.TerminalCheckbox
import com.shiro.yosugahub.ui.component.TerminalDialog
import com.shiro.yosugahub.ui.component.TerminalField
import com.shiro.yosugahub.ui.share.syncResultMessage

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsState()
    val importHistory by viewModel.importHistory.collectAsState()
    val vaultChecking by viewModel.vaultChecking.collectAsState()
    val vaultCheck by viewModel.vaultCheck.collectAsState()
    // ファイル一覧は Flow ではないので、画面を開いたときに読み直す。
    LaunchedEffect(Unit) { viewModel.refreshImportHistory() }
    var openedHistory by remember { mutableStateOf<Pair<String, String>?>(null) }
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
            SectionCard(title = "Obsidian Vault") {
                Text(
                    text = if (uiState.obsidianVaultUri.isEmpty()) {
                        "未設定。知識の書き出しと、ヨスガへ渡すコンテキストの抽出に使います。"
                    } else {
                        "選択中: ${vaultDisplayName(uiState.obsidianVaultUri)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                TacticalOutlinedButton(
                    onClick = { vaultPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.obsidianVaultUri.isEmpty()) "Vaultフォルダを選択" else "Vaultフォルダを変更")
                }

                // 「選べた」と「読める」は別。提供元によっては選択できても中身を返さない。
                if (uiState.obsidianVaultUri.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TacticalOutlinedButton(
                        onClick = { viewModel.checkVault() },
                        enabled = !vaultChecking,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (vaultChecking) "確認中…" else "Vaultを読み取れるか確認")
                    }
                    vaultCheck?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (result.isError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        result.samplePaths.forEach { path ->
                            Text(
                                text = "・$path",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
        item {
            // 旧・設計書v2の Phase 4(OAuth で Google カレンダーAPIへ接続)は採らなかった。
            // CalendarContract で端末に同期済みのカレンダーを読む方式に差し替えてある。
            // ここに「Googleアカウント: 未接続(Phase 4 で実装予定)」と出していたため、
            // 予定が0件のときに「アカウント未接続が原因」と誤読させていた(2026-07-26 に削除)。
            SectionCard(title = "カレンダー") {
                Text(
                    text = "端末に同期済みのカレンダー(Googleカレンダーを含む)を読みます。" +
                        "READ_CALENDAR の許可だけで動くので、" +
                        "Hub 側で Google アカウントに接続する必要はありません。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("取得期間: 過去14日〜未来14日", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "予定が出ないときは、端末の 設定 → パスワードとアカウント → " +
                        "Google → アカウントの同期 で「カレンダー」が ON か確認してください。" +
                        "Googleカレンダーのアプリに表示されていても、" +
                        "端末のカレンダーへ同期されていなければ読めません。",
                    style = MaterialTheme.typography.bodySmall,
                )
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
                TerminalField(
                    value = syncUrlInput,
                    onValueChange = { syncUrlInput = it },
                    // http:// だと Android が通信ごと弾き、原因の分からない失敗になる。
                    label = "同期先URL(https:// で始めること)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                TerminalField(
                    value = syncTokenInput,
                    onValueChange = { syncTokenInput = it },
                    label = if (uiState.hasSyncToken) "トークン(設定済み・変更する場合のみ)" else "トークン",
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    TacticalOutlinedButton(
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
                    TacticalOutlinedButton(
                        onClick = {
                            val token = viewModel.generateSyncToken()
                            syncTokenInput = token
                            // 生成したトークンは**必ず外へ持ち出す**(サーバーの config.php に貼る)。
                            // 入力欄はマスク表示なので、Android の仕様で欄から選択してもコピーできない
                            // (貼り付けしか出ない)。ここで自動でクリップボードへ入れないと、
                            // 生成できるのに取り出せないという行き止まりになる。
                            clipboard.setText(AnnotatedString(token))
                            Toast.makeText(
                                context,
                                "トークンを生成してコピーしました。同じ値をサーバーの config.php にも貼ってください。",
                                Toast.LENGTH_LONG,
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("トークン生成")
                    }
                }
                // 生成したあとに別のものをコピーしてしまったとき用。欄が空なら出さない。
                if (syncTokenInput.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TacticalOutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(syncTokenInput))
                            Toast.makeText(context, "トークンをコピーしました", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("トークンをコピー")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TacticalButton(
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
                TerminalField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = "トークンを入力して保存",
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    TacticalOutlinedButton(
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
                        TacticalOutlinedButton(
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
            SectionCard(title = "取り込み履歴") {
                if (importHistory.isEmpty()) {
                    Text(
                        "まだありません。回答JSONを取り込むとここに残ります。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column {
                        importHistory.forEachIndexed { position, entry ->
                            if (position > 0) HorizontalDivider()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.readImportHistory(entry.fileName) { text ->
                                            openedHistory = entry.fileName to
                                                (text ?: "ファイルを読めませんでした。")
                                        }
                                    },
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = entry.savedAt.ifEmpty { entry.fileName },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = "${entry.sizeBytes} バイト",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = "診断情報") {
                Text("アプリバージョン: 0.1.0", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    openedHistory?.let { (fileName, content) ->
        TerminalDialog(
            title = fileName,
            onDismissRequest = { openedHistory = null },
            content = {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                DialogAction("閉じる", onClick = { openedHistory = null })
            },
        )
    }
}


/** ツリーURIから表示用のフォルダ名を取り出す(例: primary:Obsidian/Vault → Obsidian/Vault)。 */
private fun vaultDisplayName(uriString: String): String {
    val lastSegment = Uri.parse(uriString).lastPathSegment ?: return uriString
    return lastSegment.substringAfter(':').ifEmpty { lastSegment }
}
