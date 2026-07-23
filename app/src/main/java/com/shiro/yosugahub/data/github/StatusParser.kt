package com.shiro.yosugahub.data.github

import com.shiro.yosugahub.data.github.model.ProjectStatus
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * `.yosuga/status.json` の解析・検証(設計書19.2 / 20章)。
 * 純粋ロジックなのでユニットテスト可能。不正なJSONでもクラッシュさせない。
 */
object StatusParser {

    const val SUPPORTED_SCHEMA_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    sealed interface Result {
        data class Success(val status: ProjectStatus) : Result

        /** JSONとして壊れている / schemaVersion が無い。 */
        data class InvalidJson(val message: String) : Result

        /** 対応していないスキーマ版(設計書19.4: 無理に読み込まず警告する)。 */
        data class UnsupportedSchema(val version: Int) : Result

        /** projectId が期待と違う(設計書20章の必須条件)。 */
        data class ProjectIdMismatch(val expected: String, val actual: String) : Result
    }

    /**
     * @param expectedProjectId 突き合わせる projectId。空なら照合しない。
     */
    fun parse(text: String, expectedProjectId: String = ""): Result {
        val status = try {
            json.decodeFromString<ProjectStatus>(text)
        } catch (e: SerializationException) {
            return Result.InvalidJson(e.message ?: "status.json の形式が正しくありません")
        } catch (e: IllegalArgumentException) {
            return Result.InvalidJson(e.message ?: "status.json の形式が正しくありません")
        }

        if (status.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            return Result.UnsupportedSchema(status.schemaVersion)
        }
        if (expectedProjectId.isNotBlank() &&
            status.projectId.isNotBlank() &&
            status.projectId != expectedProjectId
        ) {
            return Result.ProjectIdMismatch(expected = expectedProjectId, actual = status.projectId)
        }
        return Result.Success(status)
    }
}
