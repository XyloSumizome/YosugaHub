package com.shiro.yosugahub.ui.screen.assistant

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiro.yosugahub.ui.component.TacticalButton
import com.shiro.yosugahub.ui.component.TacticalOutlinedButton
import com.shiro.yosugahub.data.obsidian.AppendOutcome
import com.shiro.yosugahub.data.repository.ApproveResult
import com.shiro.yosugahub.data.repository.ConversationImportResult
import com.shiro.yosugahub.ui.component.PasteImportDialog
import com.shiro.yosugahub.ui.component.SectionCard
import com.shiro.yosugahub.ui.share.importResultMessage
import com.shiro.yosugahub.ui.share.shareJsonText

@Composable
fun AssistantScreen(
    onOpenObsidianContext: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AssistantViewModel = viewModel(factory = AssistantViewModel.Factory),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val noteImporting by viewModel.noteImporting.collectAsState()
    val noteImportSummary by viewModel.noteImportSummary.collectAsState()
    val importLog by viewModel.importLog.collectAsState()

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
    var showConversationDialog by remember { mutableStateOf(false) }
    noteImportSummary?.let { summary ->
        NoteImportSummaryDialog(
            summary = summary,
            onDismiss = viewModel::dismissNoteImportSummary,
        )
    }

    if (showConversationDialog) {
        PasteImportDialog(
            onDismiss = { showConversationDialog = false },
            onImport = { text ->
                showConversationDialog = false
                viewModel.saveConversation(text) { result ->
                    val message = when (result) {
                        is ConversationImportResult.Saved -> "保存しました: ${result.path}"
                        ConversationImportResult.Empty -> "内容が空です"
                        ConversationImportResult.VaultNotConfigured ->
                            "Obsidian Vault が未設定です。設定で選んでください。"
                        is ConversationImportResult.Failed -> result.reason
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            },
            title = "会話ログを保存",
            description = "ヨスガの「セッションまとめ」をコピーして貼り付けてください。" +
                "Obsidian の Conversations/Yosuga/ へそのまま保存します(要約しません)。",
            label = "会話まとめ(Markdown)",
            confirmLabel = "保存",
        )
    }

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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = "ヨスガ連携", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            SectionCard(title = "ゲームのノートを取り込む") {
                Text(
                    text = "各ゲームのリポジトリの .yosuga/notes/ を取得し、" +
                        "Frontmatter の type に従って Obsidian Vault へ振り分けます。" +
                        "取り込み済みのノートは飛ばすので、何度押しても二重には入りません。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TacticalButton(
                    onClick = viewModel::importNotes,
                    enabled = !noteImporting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (noteImporting) "取り込み中…" else "ノートを取り込む")
                }
                // 取り込み中・直後は本物のログを端末風に流す(ハッキング演出)。
                if (noteImporting || importLog.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ImportTerminal(lines = importLog, running = noteImporting)
                }
            }
        }
        item {
            SectionCard(title = "会話ログをObsidianへ") {
                Text(
                    text = "ヨスガの「セッションまとめ」を貼り付けると、" +
                        "Conversations/Yosuga/ へ保存します。原文のまま保存し、要約はしません。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TacticalButton(
                    onClick = { showConversationDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("会話ログを保存")
                }
            }
        }
        item {
            SectionCard(title = "Obsidianの文脈") {
                Text(
                    text = "Obsidianから必要な範囲だけを選び、ヨスガへ貼り付けるMarkdownにまとめます。" +
                        "要約はせず、原文をそのまま連結します。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TacticalButton(onClick = onOpenObsidianContext, modifier = Modifier.fillMaxWidth()) {
                    Text("Obsidianから文脈を作る")
                }
            }
        }
        item {
            SectionCard(title = "状況JSON") {
                Text(
                    text = "カレンダーとゲーム進捗をまとめたJSONを作成し、" +
                        "共有メニューまたはファイルとしてChatGPTへ渡します。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TacticalButton(onClick = createExport, modifier = Modifier.fillMaxWidth()) {
                    Text("状況JSONを作成")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TacticalOutlinedButton(onClick = importResponse, modifier = Modifier.fillMaxWidth()) {
                    Text("回答JSONを取り込む(ファイル)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TacticalOutlinedButton(
                    onClick = { showPasteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("回答JSONを貼り付けて取り込む")
                }
            }
        }
        if (uiState.proposals.isNotEmpty()) {
            item {
                Text(
                    text = "承認待ちの提案(${uiState.proposals.size})",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(uiState.proposals, key = { it.proposal.id }) { card ->
                ProposalCard(
                    card = card,
                    onApprove = {
                        viewModel.approveProposal(card.proposal) { result ->
                            val message = when (result) {
                                is ApproveResult.Applied -> when (result.obsidian) {
                                    null -> "反映しました"
                                    AppendOutcome.WRITTEN -> "反映しました(Obsidianへ追記済み)"
                                    AppendOutcome.NOT_CONFIGURED ->
                                        "反映しました(Vault未設定のためObsidian書き出しなし)"
                                    AppendOutcome.FAILED -> "反映しました(Obsidianへの書き出しに失敗)"
                                }
                                ApproveResult.NotApplicable -> "反映できない提案のため棄却しました"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onReject = { viewModel.rejectProposal(card.proposal) },
                )
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

/** 承認待ち提案のカード(種別チップ + 内容 + 棄却/承認)。 */
@Composable
private fun ProposalCard(
    card: ProposalCardUi,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                AssistChip(onClick = {}, label = { Text(card.typeLabel) })
            }
            if (card.body.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = card.body, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                TacticalOutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                    Text("棄却")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TacticalButton(
                    onClick = onApprove,
                    enabled = card.readable,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("承認")
                }
            }
        }
    }
}
