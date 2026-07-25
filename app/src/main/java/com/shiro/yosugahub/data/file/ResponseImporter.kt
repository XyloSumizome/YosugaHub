package com.shiro.yosugahub.data.file

import com.shiro.yosugahub.data.file.model.AssistantResponse
import com.shiro.yosugahub.data.file.model.AssistantResponseV2
import com.shiro.yosugahub.data.file.model.SchemaProbe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

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
            // ヨスガ(通常の ChatGPT)は封筒を落として proposals の中身だけを出しがち。
            // 提案のキーが1つでもあれば v2 とみなして受け取る(2026-07-25)。
            // 既に questionsForYosuga の型ゆれを吸収しているのと同じ判断
            // ——AIの出力揺れはアプリ側で吸収し、人に貼り直させない。
            return parseWithoutEnvelope(text)
                ?: ParseResult.InvalidJson("schemaVersion がありません")
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

    /**
     * `schemaVersion` が無い JSON を、**proposals の中身だけが来た**ものとして読む。
     *
     * 受けるのは次の2つだけ:
     * - `{"diary": [...]}` のように**提案のキーが直に並ぶ**形
     * - `{"proposals": {...}}` のように**封筒だけ欠けた**形
     *
     * 提案のキーが1つも無ければ null を返し、呼び出し側が従来どおりエラーにする。
     * **何でも受け取るわけではない**——中身が提案だと分かるときだけ助ける。
     */
    private fun parseWithoutEnvelope(text: String): ParseResult? {
        val root = try {
            json.parseToJsonElement(text) as? JsonObject ?: return null
        } catch (e: SerializationException) {
            return null
        } catch (e: IllegalArgumentException) {
            return null
        }

        val proposals = root["proposals"] as? JsonObject ?: root
        if (PROPOSAL_KEYS.none { it in proposals }) return null

        return try {
            ParseResult.SuccessV2(
                AssistantResponseV2(
                    schemaVersion = SCHEMA_VERSION_V2,
                    generatedAt = (root["generatedAt"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                    summary = (root["summary"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                    proposals = json.decodeFromJsonElement(proposals),
                )
            )
        } catch (e: SerializationException) {
            ParseResult.InvalidJson(e.message ?: "JSONの形式が正しくありません")
        } catch (e: IllegalArgumentException) {
            ParseResult.InvalidJson(e.message ?: "JSONの形式が正しくありません")
        }
    }

    /** proposals として認めるキー。ここに1つも無ければ提案の JSON ではないと判断する。 */
    private val PROPOSAL_KEYS = listOf(
        "tasks", "items", "diary", "projectHealth", "classifications", "directives",
    )
}
