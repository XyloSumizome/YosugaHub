package com.shiro.yosugahub.data.github.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 「文字列のはずだが、書き手によってはオブジェクトや配列で来る」項目を
 * 落とさずに読むためのシリアライザ(設計書19.4 の「無理に読み込まない」より、
 * ここでは **1項目の型ゆれで status.json 全体が読めなくなる方が害が大きい** と判断した)。
 *
 * - 文字列 / 数値 / 真偽値 → そのまま文字列に
 * - オブジェクト → [TEXT_KEYS] の順に探し、最初に見つかった非空の文字列を使う。
 *   見つからなければ、値のうち最初の非空な文字列を使う
 * - 配列 → 各要素を同じ規則で文字列化し " / " で連結する
 * - null / 該当なし → 空文字(呼び出し側が空を落とす前提)
 *
 * 書き出しは常に素の文字列。
 */
object FlexibleTextSerializer : KSerializer<String> {

    /** オブジェクトで来たときに本文とみなすキー(前にあるものを優先)。 */
    private val TEXT_KEYS = listOf(
        "question", "text", "title", "summary", "content", "body", "detail", "message", "value",
    )

    /** 本文ではない管理用のキー。既知キーが無いときの拾い読みから除く。 */
    private val META_KEYS = setOf(
        "id", "key", "type", "kind", "category", "date", "createdAt", "updatedAt",
        "status", "state", "priority", "severity", "tag", "tags", "author", "source",
    )

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleText", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        return flatten(jsonDecoder.decodeJsonElement())
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    private fun flatten(element: kotlinx.serialization.json.JsonElement): String = when (element) {
        is JsonNull -> ""
        is JsonPrimitive -> element.content
        is JsonArray -> element.map { flatten(it) }.filter { it.isNotBlank() }.joinToString(" / ")
        is JsonObject -> fromObject(element)
    }

    private fun fromObject(obj: JsonObject): String {
        for (key in TEXT_KEYS) {
            val text = (obj[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
            if (!text.isNullOrBlank()) return text
        }
        return obj.entries
            .filterNot { it.key in META_KEYS }
            .mapNotNull { (it.value as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
            .firstOrNull { it.isNotBlank() }
            ?: ""
    }

    /** 1要素だけのときに配列で書き忘れる例があるため、配列でなくても読めるようにする。 */
    internal fun flattenToList(element: kotlinx.serialization.json.JsonElement): List<String> =
        when (element) {
            is JsonNull -> emptyList()
            is JsonArray -> element.map { flatten(it) }.filter { it.isNotBlank() }
            else -> listOf(flatten(element)).filter { it.isNotBlank() }
        }
}

/**
 * 文字列リスト用。配列でも、単体の文字列/オブジェクトでも読む。
 * 中身の型ゆれは [FlexibleTextSerializer] に委ねる。空文字は落とす。
 */
object FlexibleTextListSerializer : KSerializer<List<String>> {

    private val delegate = kotlinx.serialization.builtins.ListSerializer(FlexibleTextSerializer)

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        return FlexibleTextSerializer.flattenToList(jsonDecoder.decodeJsonElement())
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        delegate.serialize(encoder, value)
    }
}
