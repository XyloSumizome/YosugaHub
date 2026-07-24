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
import com.shiro.yosugahub.ui.component.StatusTag
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = if (uiState.proposals.isEmpty()) {
                    "承認待ちの提案はありません"
                } else {
                    "承認待ちの提案 (${uiState.proposals.size})"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (uiState.proposals.isNotEmpty()) {
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
                StatusTag(card.typeLabel)
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
