package com.shiro.yosugahub.data.local.db

import com.shiro.yosugahub.domain.model.RelatedRef
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * document_classifications の JSON 列の符号化・復号(純粋ロジック)。
 * 壊れた JSON は空リストへフォールバックしクラッシュさせない(ProposalPayloads と同方針)。
 */
object DocumentJsonColumns {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** related_entities の保存形。回答JSONの {"type": "...", "id": "..."} と同じ形。 */
    @Serializable
    private data class RelatedRefJson(val type: String, val id: String)

    fun encodeStrings(values: List<String>): String = json.encodeToString(values)

    fun decodeStrings(encoded: String): List<String> = try {
        json.decodeFromString<List<String>>(encoded)
    } catch (e: SerializationException) {
        emptyList()
    } catch (e: IllegalArgumentException) {
        emptyList()
    }

    fun encodeRelatedRefs(values: List<RelatedRef>): String =
        json.encodeToString(values.map { RelatedRefJson(type = it.type, id = it.id) })

    fun decodeRelatedRefs(encoded: String): List<RelatedRef> = try {
        json.decodeFromString<List<RelatedRefJson>>(encoded)
            .map { RelatedRef(type = it.type, id = it.id) }
    } catch (e: SerializationException) {
        emptyList()
    } catch (e: IllegalArgumentException) {
        emptyList()
    }
}
