package com.shiro.yosugahub.data.obsidian

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** コンテキストJSONの1ノート分。Markdown 版の各セクションと同じ情報を持つ。 */
@Serializable
data class ContextNoteJson(
    val path: String,
    val title: String,
    val game: String? = null,
    val updatedAt: String,
    val tags: List<String> = emptyList(),
    val body: String,
)

@Serializable
data class ContextScopeJson(
    val games: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val folders: List<String> = emptyList(),
)

@Serializable
data class ContextJsonFile(
    val type: String = TYPE,
    val generatedAt: String,
    val source: String = SOURCE,
    val vault: String,
    val selectedScope: ContextScopeJson,
    val fileCount: Int,
    /**
     * 現況(状況JSON)を丸ごと入れる(2026-07-25)。含めないときは null。
     * 文字列ではなく**オブジェクトのまま**入れるので、読み手が二重に解析せずに済む。
     */
    val status: JsonElement? = null,
    val notes: List<ContextNoteJson> = emptyList(),
) {
    companion object {
        const val TYPE = "yosuga-context"
        const val SOURCE = "obsidian"
    }
}

/**
 * 選択されたノートを JSON へ書き出す純粋関数(設計書v5 §3)。
 *
 * Markdown 版([ContextMarkdown])と**同じ情報**を持つ。要約はしない。
 * 用途は機械処理側で、人間が読む正本はあくまで Markdown。
 */
object ContextJson {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun build(
        notes: List<LoadedNote>,
        vaultName: String,
        generatedAt: String,
        scope: ContextScope = ContextScope.of(notes),
        /** 現況(状況JSONの本文)。空なら入れない。 */
        status: String = "",
    ): String {
        val file = ContextJsonFile(
            generatedAt = generatedAt,
            vault = vaultName,
            selectedScope = ContextScopeJson(
                games = scope.games,
                tags = scope.tags,
                folders = scope.folders,
            ),
            fileCount = notes.size,
            // 解析できないものを黙って落とさず、その場合だけ入れないでおく。
            status = status.takeIf { it.isNotBlank() }
                ?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() },
            notes = notes.map { note ->
                ContextNoteJson(
                    path = note.relativePath,
                    title = note.title,
                    game = note.game,
                    updatedAt = note.updatedAt,
                    tags = note.tags,
                    body = note.body,
                )
            },
        )
        return json.encodeToString(ContextJsonFile.serializer(), file)
    }
}
