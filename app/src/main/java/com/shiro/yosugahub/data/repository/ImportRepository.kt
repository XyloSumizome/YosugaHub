package com.shiro.yosugahub.data.repository

import android.content.Context
import android.net.Uri
import com.shiro.yosugahub.data.file.ResponseImporter
import com.shiro.yosugahub.data.local.db.dao.RecommendationDao
import com.shiro.yosugahub.data.local.db.entity.RecommendationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 回答JSON取り込みの結果。UI はこれを見てメッセージを出す。 */
sealed interface ImportResult {
    data class Success(val recommendationCount: Int, val fileName: String) : ImportResult
    data class InvalidJson(val message: String) : ImportResult
    data class UnsupportedSchema(val version: Int) : ImportResult
    object ReadError : ImportResult
}

/**
 * ChatGPT回答JSONの取り込みを担う Repository(設計書2.3 / 4.2 / 15章)。
 * 選択されたファイルを読み、検証し、成功時のみ履歴保存 + Room 反映する。
 * 不正JSONでもクラッシュせず、結果を型で返す。
 */
class ImportRepository(
    private val context: Context,
    private val recommendationDao: RecommendationDao,
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
                // 元ファイルは上書きせず履歴として保存する(設計書15章)。
                val now = LocalDateTime.now()
                val fileName = "response_${now.format(FILE_TIMESTAMP)}.json"
                val dir = File(context.filesDir, "imports").apply { mkdirs() }
                File(dir, fileName).writeText(text)

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
        }
    }

    private companion object {
        val FILE_TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
    }
}
