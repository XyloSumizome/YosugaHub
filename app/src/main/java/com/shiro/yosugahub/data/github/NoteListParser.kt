package com.shiro.yosugahub.data.github

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * リポジトリ内の知識ノート1件のメタ情報(v5 Phase 3-a)。
 * 本文はまだ持たない。一覧は軽く保ち、必要なものだけ後から取得する。
 */
data class RemoteNote(
    val name: String,
    /** リポジトリルートからのパス(例: `.yosuga/notes/2026-07-24-lighting.md`)。 */
    val path: String,
    /** Git のブロブSHA。**内容が変わらない限り不変**なので、取得済み判定に使う。 */
    val sha: String,
    val size: Long,
)

/**
 * GitHub Contents API のディレクトリ応答(JSON配列)を解釈する純粋パーサ。
 *
 * 応答は項目が多いため、必要な4つだけを取り出す。
 * サブディレクトリと `.md` 以外は無視する(ノート置き場に何が置かれても落ちない)。
 */
object NoteListParser {

    sealed interface Result {
        data class Success(val notes: List<RemoteNote>) : Result
        data class InvalidJson(val message: String) : Result
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): Result {
        val root = try {
            json.parseToJsonElement(body)
        } catch (e: SerializationException) {
            return Result.InvalidJson(e.message.orEmpty())
        }
        if (root !is JsonArray) {
            // ディレクトリではなくファイルを指した場合などはオブジェクトが返る。
            return Result.InvalidJson("ディレクトリの一覧ではありません")
        }

        val notes = root.mapNotNull { element ->
            val entry = element as? JsonObject ?: return@mapNotNull null
            val type = entry["type"]?.jsonPrimitive?.contentOrNullSafe()
            if (type != "file") return@mapNotNull null

            val name = entry["name"]?.jsonPrimitive?.contentOrNullSafe().orEmpty()
            if (!name.endsWith(NOTE_EXTENSION, ignoreCase = true)) return@mapNotNull null

            val path = entry["path"]?.jsonPrimitive?.contentOrNullSafe().orEmpty()
            val sha = entry["sha"]?.jsonPrimitive?.contentOrNullSafe().orEmpty()
            if (path.isEmpty() || sha.isEmpty()) return@mapNotNull null

            RemoteNote(
                name = name,
                path = path,
                sha = sha,
                size = runCatching { entry["size"]?.jsonPrimitive?.long ?: 0L }.getOrDefault(0L),
            )
        }
        // 名前が日付始まりなので、名前順が概ね時系列順になる。
        return Result.Success(notes.sortedBy { it.name })
    }

    private const val NOTE_EXTENSION = ".md"
}

/** 数値・真偽値が来ても落とさずに文字列として扱う。 */
private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
    if (isString) content else content.takeIf { it != "null" }
