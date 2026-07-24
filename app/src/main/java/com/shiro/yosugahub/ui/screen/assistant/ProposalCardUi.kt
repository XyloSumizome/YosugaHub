package com.shiro.yosugahub.ui.screen.assistant

import com.shiro.yosugahub.data.file.ProposalPayloads
import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.PendingProposal
import com.shiro.yosugahub.domain.model.ProposalType
import com.shiro.yosugahub.ui.component.itemKindLabel

/**
 * 承認待ち提案カードの表示モデル(純粋関数で生成、ユニットテスト可能)。
 * proposal を保持し、承認/棄却の操作にそのまま渡す。
 */
data class ProposalCardUi(
    val proposal: PendingProposal,
    val typeLabel: String,
    val title: String,
    val body: String,
    val readable: Boolean,
)

/** payload を表示用に要約する。壊れた payload でもクラッシュさせない。 */
fun PendingProposal.toCardUi(): ProposalCardUi = when (type) {
    ProposalType.TASK -> {
        val p = ProposalPayloads.decodeTask(payloadJson)
        if (p == null) unreadable("タスク") else ProposalCardUi(
            proposal = this,
            typeLabel = "タスク",
            title = p.title,
            body = listOfNotNull(
                p.detail.takeIf { it.isNotBlank() },
                "優先度: ${priorityLabel(p.priority)}",
                p.dueDate?.takeIf { it.isNotBlank() }?.let { "締切: $it" },
                p.projectId?.takeIf { it.isNotBlank() }?.let { "プロジェクト: $it" },
            ).joinToString(" / "),
            readable = true,
        )
    }

    ProposalType.ITEM -> {
        val p = ProposalPayloads.decodeItem(payloadJson)
        if (p == null) unreadable("情報") else ProposalCardUi(
            proposal = this,
            typeLabel = itemKindLabel(ItemKind.fromDb(p.kind)),
            title = p.title,
            body = listOfNotNull(
                p.body.takeIf { it.isNotBlank() },
                p.tags.takeIf { it.isNotEmpty() }?.joinToString(" ") { "#$it" },
                p.entities.filter { it.name.isNotBlank() }
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(" / ", prefix = "関連: ") { it.name },
                p.targetNote.takeIf { it.isNotBlank() }?.let { "Obsidian: $it" },
            ).joinToString("\n"),
            readable = true,
        )
    }

    ProposalType.DIARY -> {
        val p = ProposalPayloads.decodeDiary(payloadJson)
        if (p == null) unreadable("観測") else ProposalCardUi(
            proposal = this,
            typeLabel = "観測",
            title = p.date.ifBlank { receivedAt.take(10) },
            body = p.body,
            readable = true,
        )
    }

    ProposalType.DIRECTIVE -> {
        val p = ProposalPayloads.decodeDirective(payloadJson)
        if (p == null) unreadable("指示書") else ProposalCardUi(
            proposal = this,
            typeLabel = "指示書",
            title = p.title.ifBlank { "${p.projectId} への指示" },
            body = listOfNotNull(
                "宛先: ${p.projectId} / 優先度: ${priorityLabel(p.priority)}",
                p.body.takeIf { it.isNotBlank() },
            ).joinToString("\n"),
            readable = true,
        )
    }

    ProposalType.HEALTH -> {
        val p = ProposalPayloads.decodeHealth(payloadJson)
        if (p == null) unreadable("状態更新") else ProposalCardUi(
            proposal = this,
            typeLabel = "状態更新",
            title = "${p.projectId} を「${p.health}」へ",
            body = p.reason,
            readable = true,
        )
    }
}

private fun PendingProposal.unreadable(typeLabel: String) = ProposalCardUi(
    proposal = this,
    typeLabel = typeLabel,
    title = "(読み取れない提案)",
    body = "内容を解釈できませんでした。棄却してください。",
    readable = false,
)

private fun priorityLabel(priority: String): String = when (priority) {
    "high" -> "高"
    "medium" -> "中"
    "low" -> "低"
    else -> priority
}
