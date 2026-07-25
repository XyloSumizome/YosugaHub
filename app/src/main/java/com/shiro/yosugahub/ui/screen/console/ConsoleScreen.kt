package com.shiro.yosugahub.ui.screen.console

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiro.yosugahub.data.file.ExternalLink
import com.shiro.yosugahub.data.repository.ConversationImportResult
import com.shiro.yosugahub.ui.component.AsciiDivider
import com.shiro.yosugahub.ui.component.DialogAction
import com.shiro.yosugahub.ui.component.OpTerminal
import com.shiro.yosugahub.ui.component.PasteImportDialog
import com.shiro.yosugahub.ui.component.TerminalDialog
import com.shiro.yosugahub.ui.screen.assistant.AssistantViewModel
import com.shiro.yosugahub.ui.screen.assistant.NoteImportSummaryDialog
import com.shiro.yosugahub.ui.share.importResultMessage
import com.shiro.yosugahub.ui.share.openExternalLink
import com.shiro.yosugahub.ui.share.shareJsonText
import com.shiro.yosugahub.ui.theme.TermAmber
import com.shiro.yosugahub.ui.theme.TermGreen
import com.shiro.yosugahub.ui.theme.TermTextDim

/**
 * オペレーションコンソール(v5 UI)。アプリの「ホーム」ではなく**コマンドランチャー**。
 *
 * 画面を移動して回るのではなく、ここから**コマンドを実行する / 画面を開く**。
 * 下部ナビは無い。カードも使わず、区切り線で整理する。
 */
@Composable
fun ConsoleScreen(
    onOpenProjects: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenContext: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
    /** 「共有 → Yosuga Hub」で届いた本文。届いていれば確認ダイアログを出す。 */
    sharedText: String? = null,
    onSharedTextHandled: () -> Unit = {},
    viewModel: AssistantViewModel = viewModel(factory = AssistantViewModel.Factory),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val importing by viewModel.opLog.running.collectAsState()
    val opLines by viewModel.opLog.lines.collectAsState()
    val summary by viewModel.noteImportSummary.collectAsState()

    var showSaveSession by remember { mutableStateOf(false) }
    var showImportResponse by remember { mutableStateOf(false) }

    summary?.let {
        NoteImportSummaryDialog(summary = it, onDismiss = viewModel::dismissNoteImportSummary)
    }
    // 共有で届いた本文。**中身の検査はせず**に確認だけ取り、判定は取り込み側へ任せる
    // (レコルの回答でなければ「JSONの形式が正しくありません」として弾かれる)。
    sharedText?.let { text ->
        SharedImportDialog(
            text = text,
            onDismiss = onSharedTextHandled,
            onImport = {
                onSharedTextHandled()
                viewModel.importResponseText(text) { result ->
                    Toast.makeText(context, importResultMessage(result), Toast.LENGTH_LONG).show()
                }
            },
        )
    }
    if (showSaveSession) {
        PasteImportDialog(
            onDismiss = { showSaveSession = false },
            onImport = { text ->
                showSaveSession = false
                viewModel.saveConversation(text) { result ->
                    val msg = when (result) {
                        is ConversationImportResult.Saved -> "SAVED: ${result.path}"
                        ConversationImportResult.Empty -> "内容が空です"
                        ConversationImportResult.VaultNotConfigured -> "Vault が未設定です"
                        is ConversationImportResult.Failed -> result.reason
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            title = "SAVE SESSION",
            description = "ヨスガのセッションまとめを貼り付け。Conversations/Yosuga/ へ原文保存。",
            label = "session markdown",
            confirmLabel = "SAVE",
        )
    }
    if (showImportResponse) {
        PasteImportDialog(
            onDismiss = { showImportResponse = false },
            onImport = { text ->
                showImportResponse = false
                viewModel.importResponseText(text) { result ->
                    Toast.makeText(context, importResultMessage(result), Toast.LENGTH_SHORT).show()
                }
            },
            title = "IMPORT RESPONSE",
            description = "回答JSONを貼り付けて取り込む。" +
                "レコルの整理でも、ヨスガの観測日記でも同じ口から入る。",
            label = "response json",
            confirmLabel = "IMPORT",
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Yosuga", style = MaterialTheme.typography.headlineSmall, color = TermGreen)
        Text(
            "> watcher toolkit // operations console",
            style = MaterialTheme.typography.labelMedium,
            color = TermTextDim,
        )
        Spacer(Modifier.height(12.dp))
        AsciiDivider()
        Spacer(Modifier.height(10.dp))

        // ── 状態表示 ──
        Readout("PROJECT", uiState.projectCount.toString())
        Readout("PENDING", uiState.pendingCount.toString())
        Readout("SYNC", uiState.lastSync.ifBlank { "--" })
        Readout("STATUS", if (importing) "[ BUSY ]" else "[ READY ]", if (importing) TermAmber else TermGreen)

        Spacer(Modifier.height(10.dp))
        AsciiDivider()

        // 実行中・直後は共通の端末ログをここに流す(取り込み/保存/生成すべて)。
        if (importing || opLines.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            OpTerminal(title = "CONSOLE", lines = opLines, running = importing)
        }

        // ── オペレーション ──
        Command(
            "IMPORT NOTES",
            "GitHubからノートを取得しObsidianへ保存",
            enabled = !importing,
            onClick = viewModel::importNotes,
        )
        AsciiDivider()
        Command("SAVE SESSION", "ヨスガとの会話をObsidianへ保存") { showSaveSession = true }
        AsciiDivider()
        Command("BUILD CONTEXT", "Obsidianから情報をまとめヨスガへ貼るMarkdownを生成", onClick = onOpenContext)
        AsciiDivider()
        Command("EXPORT STATUS", "状況JSONを生成して共有") {
            viewModel.createExport { result ->
                result.onSuccess {
                    Toast.makeText(context, "EXPORT: ${it.fileName}", Toast.LENGTH_SHORT).show()
                    shareJsonText(context, it.json)
                }.onFailure {
                    Toast.makeText(context, "生成に失敗しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
        AsciiDivider()
        // レコル(整理)とヨスガ(観測日記)の両方が同じ口から入る。
        Command("IMPORT RESPONSE", "AIの回答JSONを取り込む(レコル / ヨスガ)") { showImportResponse = true }
        AsciiDivider()

        // URL 未設定なら出さない(押せて何も起きない口を作らない)。
        ExternalLink.sanitize(uiState.recoruUrl)?.let { url ->
            Command("OPEN RECORU", "レコル(カスタムGPT)を開く") {
                val opened = openExternalLink(context, url)
                if (!opened) {
                    Toast.makeText(context, "開けるアプリがありません。", Toast.LENGTH_SHORT).show()
                }
            }
            AsciiDivider()
        }

        val reviewSuffix = if (uiState.pendingCount > 0) " (${uiState.pendingCount})" else ""
        Command("REVIEW$reviewSuffix", "承認待ちの提案を確認する", token = ">", onClick = onOpenReview)
        AsciiDivider()

        Spacer(Modifier.height(10.dp))
        Text("DATA", style = MaterialTheme.typography.labelMedium, color = TermTextDim)
        Spacer(Modifier.height(6.dp))
        AsciiDivider()
        Command("PROJECTS (${uiState.projectCount})", "ゲームの進捗・タスク", onClick = onOpenProjects)
        AsciiDivider()
        Command("RECORDS", "知識・決定・観測・文書・指示", onClick = onOpenRecords)
        AsciiDivider()
        Command("CALENDAR", "端末カレンダーの予定", onClick = onOpenCalendar)
        AsciiDivider()
        Command("SETTINGS", "Vault / GitHub / 同期 / サンプルデータ", onClick = onOpenSettings)
        AsciiDivider()
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * 「共有 → Yosuga Hub」で届いた本文の確認(v5 / 2026-07-25)。
 *
 * 共有を選んだ時点で意思表示はあるが、**取り込みは後戻りしにくい**ので
 * 一度だけ内容を見せる。中身の判定はここでせず、取り込み側の結果メッセージに任せる。
 */
@Composable
private fun SharedImportDialog(
    text: String,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
) {
    val preview = if (text.length > PREVIEW_LIMIT) text.take(PREVIEW_LIMIT) + "\n…" else text

    TerminalDialog(
        title = "SHARED",
        onDismissRequest = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "共有された内容を回答JSONとして取り込みます。" +
                        "説明文が前後に付いていても、JSONの部分だけを取り出します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TermTextDim,
                )
                Text(
                    text = "${text.length} 文字",
                    style = MaterialTheme.typography.labelMedium,
                    color = TermTextDim,
                )
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = TermGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = { DialogAction("IMPORT", onClick = onImport) },
        dismissButton = { DialogAction("CANCEL", onClick = onDismiss) },
    )
}

/** 確認ダイアログで見せる本文の長さ。全文は要らず、頭が見えれば取り違えに気づける。 */
private const val PREVIEW_LIMIT = 600

/** `KEY  : VALUE` の状態表示行。 */
@Composable
private fun Readout(key: String, value: String, valueColor: androidx.compose.ui.graphics.Color = TermGreen) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = key.padEnd(8),
            style = MaterialTheme.typography.bodyMedium,
            color = TermTextDim,
        )
        Text(": ", style = MaterialTheme.typography.bodyMedium, color = TermTextDim)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

/** `> COMMAND` + 説明 の実行行。丸みも塗りも無い。 */
@Composable
private fun Command(
    name: String,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    token: String = ">",
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(token, color = TermGreen.copy(alpha = alpha), style = MaterialTheme.typography.titleSmall)
            Text(
                name,
                color = TermGreen.copy(alpha = alpha),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Text(
            description,
            color = TermTextDim.copy(alpha = alpha),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 18.dp),
        )
    }
}
