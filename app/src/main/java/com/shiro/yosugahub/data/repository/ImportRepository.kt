package com.shiro.yosugahub.data.repository

import android.content.Context
import android.net.Uri
import com.shiro.yosugahub.data.file.ProposalMapper
import com.shiro.yosugahub.data.file.ResponseImporter
import com.shiro.yosugahub.data.file.model.ClassificationProposal
import com.shiro.yosugahub.data.local.db.dao.PendingProposalDao
import com.shiro.yosugahub.data.local.db.dao.RecommendationDao
import com.shiro.yosugahub.data.local.db.entity.RecommendationEntity
import com.shiro.yosugahub.domain.model.RelatedRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** 回答JSON取り込みの結果。UI はこれを見てメッセージを出す。 */
sealed interface ImportResult {
    /** v1: recommendations を直接反映した。 */
    data class Success(val recommendationCount: Int, val fileName: String) : ImportResult

    /**
     * v2: 提案を承認待ちに入れた(反映はユーザー承認後)。
     * 分類結果は文書へ適用済み(状態は「確認待ち」で、確定はユーザーの承認後)。
     * unknownDocumentCount は宛先の文書が見つからず読み飛ばした件数。
     */
    data class SuccessProposals(
        val proposalCount: Int,
        val fileName: String,
        val classificationCount: Int = 0,
        val unknownDocumentCount: Int = 0,
    ) : ImportResult

    data class InvalidJson(val message: String) : ImportResult
    data class UnsupportedSchema(val version: Int) : ImportResult
    object ReadError : ImportResult
}

/**
 * ChatGPT回答JSONの取り込みを担う Repository(設計書2.3 / 4.2 / 15章)。
 * 選択されたファイルを読み、検証し、成功時のみ履歴保存 + Room 反映する。
 * v2 は pending_proposals へ入れて承認を待つ(v3 の提案→承認→保存フロー)。
 * 不正JSONでもクラッシュせず、結果を型で返す。
 */
class ImportRepository(
    private val context: Context,
    private val recommendationDao: RecommendationDao,
    private val pendingProposalDao: PendingProposalDao,
    private val documentRepository: DocumentRepository,
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
) {

    suspend fun importResponse(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: IOException) {
            null
        } ?: return@withContext ImportResult.ReadError

        when (val result = ResponseImporter.parse(text)) {
            is ResponseImporter.ParseResult.InvalidJson ->
                ImportResult.InvalidJson(result.message)

            is ResponseImporter.ParseResult.UnsupportedSchema ->
                ImportResult.UnsupportedSchema(result.version)

            is ResponseImporter.ParseResult.Success -> {
                val fileName = saveHistory(text)
                val entities = result.response.recommendations.map { rec ->
                    RecommendationEntity(
                        projectId = rec.projectId,
                        title = rec.title,
                        detail = rec.detail,
                        priority = rec.priority,
                    )
                }
                // 表示中の提案を取り込み結果で置き換える。
                recommendationDao.deleteAll()
                recommendationDao.insertAll(entities)

                ImportResult.Success(recommendationCount = entities.size, fileName = fileName)
            }

            is ResponseImporter.ParseResult.SuccessV2 -> {
                val fileName = saveHistory(text)
                val rows = ProposalMapper.toPendingEntities(
                    response = result.response,
                    receivedAt = OffsetDateTime.now().toString(),
                    newId = newId,
                )
                // 直接反映せず承認待ちに積む(v3: 提案→承認→保存)。
                pendingProposalDao.insertAll(rows)

                val classified = applyClassifications(result.response.proposals.classifications)

                ImportResult.SuccessProposals(
                    proposalCount = rows.size,
                    fileName = fileName,
                    classificationCount = classified.applied,
                    unknownDocumentCount = classified.unknownDocument,
                )
            }
        }
    }

    private data class ClassificationOutcome(val applied: Int, val unknownDocument: Int)

    /**
     * 分類結果を文書へ適用する(v4.1)。
     * 他の提案と違い pending_proposals には積まない — 文書は「確認待ち」になり、
     * 承認・修正は文書の詳細画面で行う(承認を二重に求めない)。
     * document_id が空、または宛先の文書が無いものは読み飛ばす。
     */
    private suspend fun applyClassifications(
        classifications: List<ClassificationProposal>,
    ): ClassificationOutcome {
        var applied = 0
        var unknown = 0
        classifications
            .filter { it.documentId.isNotBlank() }
            .forEach { classification ->
                val result = documentRepository.applyAiClassification(
                    documentId = classification.documentId,
                    summary = classification.summary,
                    documentType = classification.documentType,
                    confidence = classification.confidence,
                    projectIds = classification.projectIds,
                    categories = classification.categories,
                    tags = classification.tags,
                    relatedEntities = classification.relatedEntities
                        .filter { it.type.isNotBlank() && it.id.isNotBlank() }
                        .map { RelatedRef(type = it.type, id = it.id) },
                )
                if (result != null) applied++ else unknown++
            }
        return ClassificationOutcome(applied = applied, unknownDocument = unknown)
    }

    /** 元ファイルは上書きせず履歴として保存する(設計書15章)。 */
    private fun saveHistory(text: String): String {
        val fileName = "response_${LocalDateTime.now().format(FILE_TIMESTAMP)}.json"
        val dir = File(context.filesDir, "imports").apply { mkdirs() }
        File(dir, fileName).writeText(text)
        return fileName
    }

    private companion object {
        val FILE_TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
    }
}
