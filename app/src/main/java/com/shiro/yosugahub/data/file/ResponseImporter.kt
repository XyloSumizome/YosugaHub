package com.shiro.yosugahub.data.file

import com.shiro.yosugahub.data.file.model.AssistantResponse
import com.shiro.yosugahub.data.file.model.SchemaProbe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * 回答JSON文字列の解析・検証を担う純粋ロジック(設計書15章)。
 * - 未知の項目は無視する(ignoreUnknownKeys）
 * - schemaVersion を検証する
 * - 不正なJSONでクラッシュさせず、結果を型で返す
 * I/O を持たないためユニットテスト可能。
 */
object ResponseImporter {

    const val SUPPORTED_SCHEMA_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    sealed interface ParseResult {
        data class Success(val response: AssistantResponse) : ParseResult
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
        if (version != SUPPORTED_SCHEMA_VERSION) {
            return ParseResult.UnsupportedSchema(version)
        }

        return try {
            ParseResult.Success(json.decodeFromString<AssistantResponse>(text))
        } catch (e: SerializationException) {
            ParseResult.InvalidJson(e.message ?: "JSONの形式が正しくありません")
        } catch (e: IllegalArgumentException) {
            ParseResult.InvalidJson(e.message ?: "JSONの形式が正しくありません")
        }
    }
}
