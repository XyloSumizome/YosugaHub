package com.shiro.yosugahub.data.file

import com.shiro.yosugahub.data.file.model.DiaryProposal
import com.shiro.yosugahub.data.file.model.DirectiveProposal
import com.shiro.yosugahub.data.file.model.HealthProposal
import com.shiro.yosugahub.data.file.model.ItemProposal
import com.shiro.yosugahub.data.file.model.TaskProposal
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * pending_proposals の payloadJson を復号する純粋ロジック。
 * 壊れた payload は null を返し、呼び出し側(承認処理・表示)が安全に扱う。
 */
object ProposalPayloads {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun decodeTask(payloadJson: String): TaskProposal? = decode(payloadJson)
    fun decodeItem(payloadJson: String): ItemProposal? = decode(payloadJson)
    fun decodeDiary(payloadJson: String): DiaryProposal? = decode(payloadJson)
    fun decodeHealth(payloadJson: String): HealthProposal? = decode(payloadJson)
    fun decodeDirective(payloadJson: String): DirectiveProposal? = decode(payloadJson)

    private inline fun <reified T> decode(payloadJson: String): T? = try {
        json.decodeFromString<T>(payloadJson)
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}
