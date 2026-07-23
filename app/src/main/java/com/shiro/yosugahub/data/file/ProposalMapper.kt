package com.shiro.yosugahub.data.file

import com.shiro.yosugahub.data.file.model.AssistantResponseV2
import com.shiro.yosugahub.data.local.db.entity.PendingProposalEntity
import com.shiro.yosugahub.domain.model.ProposalStatus
import com.shiro.yosugahub.domain.model.ProposalType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 回答JSON v2 の proposals を pending_proposals の行へ変換する純粋ロジック。
 * - 空タイトル・空本文など意味を持たない提案は読み飛ばす(クラッシュさせない)
 * - payloadJson には提案1件分のJSONを保存し、承認時(2-c)に解釈する
 */
object ProposalMapper {

    private val json = Json { encodeDefaults = true }

    fun toPendingEntities(
        response: AssistantResponseV2,
        receivedAt: String,
        newId: () -> String,
    ): List<PendingProposalEntity> {
        val rows = mutableListOf<PendingProposalEntity>()

        response.proposals.tasks
            .filter { it.title.isNotBlank() }
            .forEach { proposal ->
                rows += row(ProposalType.TASK, json.encodeToString(proposal), receivedAt, newId)
            }
        response.proposals.items
            .filter { it.title.isNotBlank() }
            .forEach { proposal ->
                rows += row(ProposalType.ITEM, json.encodeToString(proposal), receivedAt, newId)
            }
        response.proposals.diary
            .filter { it.body.isNotBlank() }
            .forEach { proposal ->
                rows += row(ProposalType.DIARY, json.encodeToString(proposal), receivedAt, newId)
            }
        response.proposals.projectHealth
            .filter { it.projectId.isNotBlank() && it.health.isNotBlank() }
            .forEach { proposal ->
                rows += row(ProposalType.HEALTH, json.encodeToString(proposal), receivedAt, newId)
            }

        return rows
    }

    private fun row(
        type: ProposalType,
        payloadJson: String,
        receivedAt: String,
        newId: () -> String,
    ) = PendingProposalEntity(
        id = newId(),
        type = type.dbValue,
        payloadJson = payloadJson,
        status = ProposalStatus.PENDING.dbValue,
        receivedAt = receivedAt,
    )
}
