package com.shiro.yosugahub.data.file

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** テキストを URI へ書き出す抽象。テストで差し替えられるようにしておく。 */
fun interface TextDocumentWriter {

    /** 書き込めたら true。失敗しても例外は投げず、呼び出し側でメッセージを出す。 */
    suspend fun write(uri: Uri, text: String): Boolean
}

/**
 * SAF で作成されたドキュメントへテキストを書き出す(設計書v5 Phase 1-c)。
 *
 * 保存先はユーザーがその場で選ぶため、FileProvider も追加のパーミッションも要らない。
 * UI から contentResolver を直接触らないよう、data 層に置いて ViewModel 経由で使う。
 */
class DocumentWriter(private val context: Context) : TextDocumentWriter {

    override suspend fun write(uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
                true
            } ?: false
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
