package com.shiro.yosugahub.data.repository

import android.content.Context
import android.net.Uri
import com.shiro.yosugahub.data.file.ImportHistoryNames
import com.shiro.yosugahub.data.file.PastedJson
import com.shiro.yosugahub.data.file.ProposalMapper
import com.shiro.yosugahub.data.file.ResponseImporter
import com.shiro.yosugahub.data.local.db.dao.PendingProposalDao
import com.shiro.yosugahub.data.local.db.dao.RecommendationDao
import com.shiro.yosugahub.data.local.db.entity.RecommendationEntity
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
    data class Success(
        val recommendationCount: Int,
        val fileName: String,
        /** 取り込み後の自動同期の結果。未配線なら null。 */
        val sync: SyncResult? = null,
    ) : ImportResult

    /**
     * v2: 提案を承認待ちに入れた(反映はユーザー承認後)。
     * 分類結果は文書へ適用済み(状態は「確認待ち」で、確定はユーザーの承認後)。
     * skippedClassificationCount は適用できず読み飛ばした件数
     * (宛先の文書が見つからない / ユーザーが決着させた文書 = 確定済み・アーカイブ済み)。
     */
    data class SuccessProposals(
        val proposalCount: Int,
        val fileName: String,
        val classificationCount: Int = 0,
        val skippedClassificationCount: Int = 0,
        /** 取り込み後の自動同期の結果。未配線なら null。 */
        val sync: SyncResult? = null,
    ) : ImportResult

    data class InvalidJson(val message: String) : ImportResult
    data class UnsupportedSchema(val version: Int) : ImportResult
    object ReadError : ImportResult
}

/** 取り込み履歴の1件(設定画面の一覧用)。 */
data class ImportHistoryEntry(
    val fileName: String,
    /** 表示用の保存時刻。名前から解釈できなければ空文字。 */
    val savedAt: String,
    val sizeBytes: Long,
)

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
    /**
     * 取り込みが成立した直後にサーバーへ反映する(v4.1 運用)。
     * ヨスガが読む側のデータを古いまま放置しないため。未配線なら null を返す。
     */
    private val syncAfterImport: suspend () -> SyncResult? = { null },
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

        importResponseText(text)
    }

    /**
     * 貼り付けられたテキストの取り込み(v4.3 運用: ファイルを作らず直接貼る)。
     * コードブロックの囲いは外し、以降はファイル取り込みと完全に同じ経路を通る
     * (履歴保存・承認待ちへの積み込み・分類適用・自動同期)。
     */
    suspend fun importResponseText(raw: String): ImportResult = withContext(Dispatchers.IO) {
        val text = PastedJson.normalize(raw)
        if (text.isBlank()) {
            return@withContext ImportResult.InvalidJson("内容が空です。回答JSONを貼り付けてください。")
        }

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

                ImportResult.Success(
                    recommendationCount = entities.size,
                    fileName = fileName,
                    sync = syncAfterImport(),
                )
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

                val classified =
                    classificationApplier.apply(result.response.proposals.classifications)

                ImportResult.SuccessProposals(
                    proposalCount = rows.size,
                    fileName = fileName,
                    classificationCount = classified.applied,
                    skippedClassificationCount = classified.skipped,
                    // Room への反映が済んでから同期する(送る内容に取り込み結果を含めるため)。
                    sync = syncAfterImport(),
                )
            }
        }
    }

    /** 分類の適用は ClassificationApplier(ファイル入出力を持たない)へ委譲する。 */
    private val classificationApplier = ClassificationApplier(documentRepository)

    /** 元ファイルは上書きせず履歴として保存する(設計書15章)。 */
    private fun saveHistory(text: String): String {
        val fileName = "response_${LocalDateTime.now().format(FILE_TIMESTAMP)}.json"
        val dir = File(context.filesDir, IMPORTS_DIR).apply { mkdirs() }
        File(dir, fileName).writeText(text)
        return fileName
    }

    /**
     * 取り込み履歴の一覧(新しい順)。
     * ファイル名が時刻そのものなので、名前の降順が時系列の降順になる。
     */
    suspend fun history(limit: Int = HISTORY_LIMIT): List<ImportHistoryEntry> =
        withContext(Dispatchers.IO) {
            File(context.filesDir, IMPORTS_DIR).listFiles()
                ?.filter { it.isFile && it.name.endsWith(".json") }
                ?.sortedByDescending { it.name }
                ?.take(limit)
                ?.map { file ->
                    ImportHistoryEntry(
                        fileName = file.name,
                        savedAt = ImportHistoryNames.formatSavedAt(file.name),
                        sizeBytes = file.length(),
                    )
                }
                .orEmpty()
        }

    /**
     * 履歴の中身を読む(何を取り込んだか確認するため)。
     * 想定外のファイル名は受け付けない(imports/ の外を読ませない)。
     */
    suspend fun readHistory(fileName: String): String? = withContext(Dispatchers.IO) {
        if (!ImportHistoryNames.isValidHistoryName(fileName)) return@withContext null
        val file = File(File(context.filesDir, IMPORTS_DIR), fileName)
        try {
            if (file.isFile) file.readText() else null
        } catch (e: IOException) {
            null
        }
    }

    private companion object {
        const val IMPORTS_DIR = "imports"
        const val HISTORY_LIMIT = 20
        val FILE_TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
    }
}
