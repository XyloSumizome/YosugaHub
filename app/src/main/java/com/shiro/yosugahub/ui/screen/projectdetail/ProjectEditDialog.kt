package com.shiro.yosugahub.ui.screen.projectdetail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.ui.component.DialogAction
import com.shiro.yosugahub.ui.component.TerminalChip
import com.shiro.yosugahub.ui.component.TerminalDialog
import com.shiro.yosugahub.ui.component.TerminalField
import com.shiro.yosugahub.ui.component.healthLabel

/**
 * プロジェクトの新規作成・編集ダイアログ(1-d + GitHub連携 / 2026-07-25 に新規作成へ対応)。
 * 編集対象は name / currentGoal / health と GitHub リポジトリ情報。
 * inProgress / nextTask はタスクからの導出へ置き換える予定のため編集させない。
 *
 * [original] が null なら**新規作成**。このときだけ **ID を入力させる**。
 * ID はサーバー・各ゲームの Claude Code・分類履歴が参照する正本なので、
 * 作成後は変えられない(編集時は表示のみ)。
 */
@Composable
fun ProjectEditDialog(
    original: Project?,
    onDismiss: () -> Unit,
    onSave: (
        id: String,
        name: String,
        currentGoal: String,
        health: String,
        repoOwner: String?,
        repoName: String?,
        repoBranch: String?,
    ) -> Unit,
    /** 既に使われている ID。新規作成で衝突させないために渡す。 */
    existingIds: Set<String> = emptySet(),
) {
    val creating = original == null
    var id by remember(original) { mutableStateOf(original?.id.orEmpty()) }
    var name by remember(original) { mutableStateOf(original?.name.orEmpty()) }
    var currentGoal by remember(original) { mutableStateOf(original?.currentGoal.orEmpty()) }
    var health by remember(original) { mutableStateOf(original?.health ?: HEALTH_OPTIONS.first()) }
    var repoOwner by remember(original) { mutableStateOf(original?.repoOwner.orEmpty()) }
    var repoName by remember(original) { mutableStateOf(original?.repoName.orEmpty()) }
    var repoBranch by remember(original) { mutableStateOf(original?.repoBranch.orEmpty()) }

    val trimmedId = id.trim()
    val idTaken = creating && trimmedId in existingIds
    val idValid = trimmedId.isNotEmpty() && trimmedId.matches(ID_PATTERN) && !idTaken

    TerminalDialog(
        title = if (creating) "プロジェクトを追加" else "プロジェクトを編集",
        onDismissRequest = onDismiss,
        content = {
            // 項目が増えたためダイアログ内をスクロール可能にする
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                if (creating) {
                    TerminalField(
                        value = id,
                        onValueChange = { id = it },
                        label = "ID(英小文字・数字・ハイフン。あとから変更できない)",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = when {
                            idTaken -> "その ID は既に使われています。"
                            trimmedId.isNotEmpty() && !trimmedId.matches(ID_PATTERN) ->
                                "使えるのは英小文字・数字・ハイフンだけです。"
                            else -> "サーバー・各ゲームの Claude Code・分類履歴が" +
                                "この ID を参照します。" +
                                "作り直すときは以前と同じ ID にしてください(例: anri)。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        text = "ID: ${original?.id.orEmpty()}(変更できません)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TerminalField(
                    value = name,
                    onValueChange = { name = it },
                    label = "プロジェクト名",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TerminalField(
                    value = currentGoal,
                    onValueChange = { currentGoal = it },
                    label = "目標",
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(text = "状態", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HEALTH_OPTIONS.forEach { option ->
                        TerminalChip(
                            selected = health == option,
                            onClick = { health = option },
                            label = healthLabel(option),
                        )
                    }
                }
                Text(
                    text = "GitHub(.yosuga/status.json の取得先。空欄なら取得しない)",
                    style = MaterialTheme.typography.labelMedium,
                )
                TerminalField(
                    value = repoOwner,
                    onValueChange = { repoOwner = it },
                    label = "owner",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TerminalField(
                    value = repoName,
                    onValueChange = { repoName = it },
                    label = "リポジトリ名",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TerminalField(
                    value = repoBranch,
                    onValueChange = { repoBranch = it },
                    label = "ブランチ(空欄なら main)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            DialogAction(
                if (creating) "作成" else "保存",
                onClick = {
                    onSave(
                        if (creating) trimmedId else original?.id.orEmpty(),
                        name.trim(),
                        currentGoal.trim(),
                        health,
                        repoOwner.trim().ifEmpty { null },
                        repoName.trim().ifEmpty { null },
                        repoBranch.trim().ifEmpty { null },
                    )
                },
                enabled = name.isNotBlank() && (!creating || idValid),
            )
        },
        dismissButton = {
            DialogAction("キャンセル", onClick = onDismiss)
        },
    )
}

/** 手動編集で選べる状態(設計書v2の語彙)。AI分析由来の自由な値は表示のみで受け入れる。 */
private val HEALTH_OPTIONS = listOf("on_track", "attention", "blocked", "paused")

/**
 * ID に使える文字。サーバーの `api.php` がファイル名検証に使う語彙へ寄せ、
 * URL やファイル名で困らない範囲に絞る(ハイフンは既存の paper-armor-frog のため許す)。
 */
private val ID_PATTERN = Regex("^[a-z0-9-]+$")
