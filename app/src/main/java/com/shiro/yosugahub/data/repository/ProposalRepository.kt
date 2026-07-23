package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.file.ProposalPayloads
import com.shiro.yosugahub.data.local.db.dao.PendingProposalDao
import com.shiro.yosugahub.data.local.db.toDomainOrNull
import com.shiro.yosugahub.data.obsidian.AppendOutcome
import com.shiro.yosugahub.data.obsidian.KnowledgeStore
import com.shiro.yosugahub.data.obsidian.ObsidianMarkdown
import com.shiro.yosugahub.domain.model.EntityRef
import com.shiro.yosugahub.domain.model.EntityType
import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.PendingProposal
import com.shiro.yosugahub.domain.model.ProposalStatus
import com.shiro.yosugahub.domain.model.ProposalType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 承認の結果。反映できない提案(壊れたpayload・対象プロジェクト不明)は棄却へ回る。
 * obsidian は targetNote 付きアイテムの書き出し結果(対象外の提案は null)。
 * Obsidian書き出しの成否に関わらず、Room への保存は成立している。
 */
sealed interface ApproveResult {
    data class Applied(val obsidian: AppendOutcome? = null) : ApproveResult
    object NotApplicable : ApproveResult
}

/**
 * 承認待ち提案の Repository(v3-Step 2-c / 2-3)。
 * 承認されたときだけ本テーブル(tasks / knowledge_items / diary_entries / projects)へ反映する。
 * 棄却・反映不能の行は rejected として残す(履歴)。
 */
class ProposalRepository(
    private val dao: PendingProposalDao,
    private val taskRepository: TaskRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val diaryRepository: DiaryRepository,
    private val projectRepository: ProjectRepository,
    private val knowledgeStore: KnowledgeStore,
) {

    fun pending(): Flow<List<PendingProposal>> =
        dao.observeByStatus(ProposalStatus.PENDING.dbValue)
            .map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    fun pendingCount(): Flow<Int> =
        dao.observeCountByStatus(ProposalStatus.PENDING.dbValue)

    /** 承認: 種別ごとに本テーブルへ反映し、成功したら approved にする。 */
    suspend fun approve(proposal: PendingProposal): ApproveResult {
        val result: ApproveResult.Applied? = when (proposal.type) {
            ProposalType.TASK -> if (applyTask(proposal)) ApproveResult.Applied() else null
            ProposalType.ITEM -> applyItem(proposal)
            ProposalType.DIARY -> if (applyDiary(proposal)) ApproveResult.Applied() else null
            ProposalType.HEALTH -> if (applyHealth(proposal)) ApproveResult.Applied() else null
        }
        return if (result != null) {
            dao.updateStatus(proposal.id, ProposalStatus.APPROVED.dbValue)
            result
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

    /**
     * アイテムの反映。Room へ保存し、targetNote があれば Obsidian へも追記する。
     * 反映できない場合は null(呼び出し側で棄却)。
     */
    private suspend fun applyItem(proposal: PendingProposal): ApproveResult.Applied? {
        val payload = ProposalPayloads.decodeItem(proposal.payloadJson) ?: return null
        if (payload.title.isBlank()) return null
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
        // Obsidian への追記は付加的な処理。失敗しても Room 保存は成立している。
        val obsidian = payload.targetNote.takeIf { it.isNotBlank() }?.let { note ->
            knowledgeStore.appendToNote(
                noteName = note,
                markdown = ObsidianMarkdown.buildNoteAppendix(
                    title = payload.title,
                    body = payload.body,
                    tags = payload.tags,
                    date = proposal.receivedAt.take(10),
                ),
            )
        }
        return ApproveResult.Applied(obsidian = obsidian)
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
