package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.file.ProposalPayloads
import com.shiro.yosugahub.data.local.db.dao.PendingProposalDao
import com.shiro.yosugahub.data.local.db.toDomainOrNull
import com.shiro.yosugahub.domain.model.EntityRef
import com.shiro.yosugahub.domain.model.EntityType
import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.PendingProposal
import com.shiro.yosugahub.domain.model.ProposalStatus
import com.shiro.yosugahub.domain.model.ProposalType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 承認の結果。反映できない提案(壊れたpayload・対象プロジェクト不明)は棄却へ回る。 */
sealed interface ApproveResult {
    object Applied : ApproveResult
    object NotApplicable : ApproveResult
}

/**
 * 承認待ち提案の Repository(v3-Step 2-c)。
 * 承認されたときだけ本テーブル(tasks / knowledge_items / diary_entries / projects)へ反映する。
 * 棄却・反映不能の行は rejected として残す(履歴)。
 */
class ProposalRepository(
    private val dao: PendingProposalDao,
    private val taskRepository: TaskRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val diaryRepository: DiaryRepository,
    private val projectRepository: ProjectRepository,
) {

    fun pending(): Flow<List<PendingProposal>> =
        dao.observeByStatus(ProposalStatus.PENDING.dbValue)
            .map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    fun pendingCount(): Flow<Int> =
        dao.observeCountByStatus(ProposalStatus.PENDING.dbValue)

    /** 承認: 種別ごとに本テーブルへ反映し、成功したら approved にする。 */
    suspend fun approve(proposal: PendingProposal): ApproveResult {
        val applied = when (proposal.type) {
            ProposalType.TASK -> applyTask(proposal)
            ProposalType.ITEM -> applyItem(proposal)
            ProposalType.DIARY -> applyDiary(proposal)
            ProposalType.HEALTH -> applyHealth(proposal)
        }
        return if (applied) {
            dao.updateStatus(proposal.id, ProposalStatus.APPROVED.dbValue)
            ApproveResult.Applied
        } else {
            // 反映できない提案は残しても永遠に適用できないため棄却として片付ける。
            dao.updateStatus(proposal.id, ProposalStatus.REJECTED.dbValue)
            ApproveResult.NotApplicable
        }
    }

    suspend fun reject(id: String) {
        dao.updateStatus(id, ProposalStatus.REJECTED.dbValue)
    }

    private suspend fun applyTask(proposal: PendingProposal): Boolean {
        val payload = ProposalPayloads.decodeTask(proposal.payloadJson) ?: return false
        if (payload.title.isBlank()) return false
        taskRepository.create(
            projectId = payload.projectId?.takeIf { it.isNotBlank() },
            title = payload.title,
            detail = payload.detail,
            priority = payload.priority,
            dueDate = payload.dueDate?.takeIf { it.isNotBlank() },
            source = "assistant",
        )
        return true
    }

    private suspend fun applyItem(proposal: PendingProposal): Boolean {
        val payload = ProposalPayloads.decodeItem(proposal.payloadJson) ?: return false
        if (payload.title.isBlank()) return false
        knowledgeRepository.createItem(
            kind = ItemKind.fromDb(payload.kind),
            title = payload.title,
            body = payload.body,
            tags = payload.tags,
            entities = payload.entities
                .filter { it.name.isNotBlank() }
                .map { EntityRef(name = it.name, type = EntityType.fromDb(it.type)) },
            source = "assistant",
        )
        return true
    }

    private suspend fun applyDiary(proposal: PendingProposal): Boolean {
        val payload = ProposalPayloads.decodeDiary(proposal.payloadJson) ?: return false
        if (payload.body.isBlank()) return false
        // 日付が欠けていたら受信日で補う("yyyy-MM-dd..." の先頭10文字)。
        val date = payload.date.ifBlank { proposal.receivedAt.take(10) }
        diaryRepository.add(date = date, body = payload.body)
        return true
    }

    private suspend fun applyHealth(proposal: PendingProposal): Boolean {
        val payload = ProposalPayloads.decodeHealth(proposal.payloadJson) ?: return false
        if (payload.projectId.isBlank() || payload.health.isBlank()) return false
        return projectRepository.updateHealth(payload.projectId, payload.health)
    }
}
