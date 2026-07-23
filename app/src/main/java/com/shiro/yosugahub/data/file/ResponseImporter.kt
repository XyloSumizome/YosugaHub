package com.shiro.yosugahub.data.file

import com.shiro.yosugahub.data.file.model.AssistantResponse
import com.shiro.yosugahub.data.file.model.AssistantResponseV2
import com.shiro.yosugahub.data.file.model.SchemaProbe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * 回答JSON文字列の解析・検証を担う純粋ロジック(設計書15章)。
 * - 未知の項目は無視する(ignoreUnknownKeys）
 * - schemaVersion を検証する(v1 / v2 対応。v1 互換は維持)
 * - 不正なJSONでクラッシュさせず、結果を型で返す
 * I/O を持たないためユニットテスト可能。
 */
object ResponseImporter {

    const val SCHEMA_VERSION_V1 = 1
    const val SCHEMA_VERSION_V2 = 2

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    sealed interface ParseResult {
        /** v1(recommendations)。従来どおり recommendations テーブルへ。 */
        data class Success(val response: AssistantResponse) : ParseResult

        /** v2(proposals)。pending_proposals へ入れて承認を待つ。 */
        data class SuccessV2(val response: AssistantResponseV2) : ParseResult

        data class InvalidJson(val message: String) : ParseResult
        data class UnsupportedSchema(val version: Int) : ParseResult
    }

    fun parse(text: String): ParseResult {
        val version = try {
            json.decodeFromString<SchemaProbe>(text).schemaVersion
        } catch (e: SerializationException) {
            return ParseResult.InvalidJson(e.message ?: "JSONの形式が正しくありません")
        } catch (e: IllegalArgumentException) {
            return ParseResult.InvalidJson(e.message ?: "JSONの形式が正しくありません")
        }

        if (version == null) {
            return ParseResult.InvalidJson("schemaVersion がありません")
        }

        return try {
            when (version) {
                SCHEMA_VERSION_V1 ->
                    ParseResult.Success(json.decodeFromString<AssistantResponse>(text))

                SCHEMA_VERSION_V2 ->
                    ParseResult.SuccessV2(json.decodeFromString<AssistantResponseV2>(text))

                else -> ParseResult.UnsupportedSchema(version)
            }
        } catch (e: SerializationException) {
            ParseResult.InvalidJson(e.message ?: "JSONの形式が正しくありません")
        } catch (e: IllegalArgumentException) {
            ParseResult.InvalidJson(e.message ?: "JSONの形式が正しくありません")
        }
    }
}
