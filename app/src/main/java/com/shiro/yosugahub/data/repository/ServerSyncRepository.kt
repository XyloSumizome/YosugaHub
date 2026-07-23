package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.sync.SyncApi
import com.shiro.yosugahub.data.sync.UploadResult

/** サーバー同期の結果。UI 向けメッセージは ui/share 側で組み立てる。 */
sealed interface SyncResult {
    data class Success(val fileCount: Int) : SyncResult
    object UrlNotConfigured : SyncResult
    object TokenMissing : SyncResult
    object Unauthorized : SyncResult
    data class HttpError(val statusCode: Int) : SyncResult
    object NetworkError : SyncResult
}

/**
 * AI向けJSONを生成してロリポップへ同期する(v4 Phase2)。
 * URL・トークンは供給関数で受け、通信直前にのみ取り出す。
 * 成功時は onSynced(最終同期時刻の記録など)を呼ぶ。
 */
class ServerSyncRepository(
    private val aiExportRepository: AiExportRepository,
    private val api: SyncApi,
    private val urlProvider: suspend () -> String,
    private val tokenProvider: suspend () -> String?,
    private val onSynced: suspend () -> Unit = {},
) {

    suspend fun sync(): SyncResult {
        val url = urlProvider().trim()
        if (url.isEmpty()) return SyncResult.UrlNotConfigured
        val token = tokenProvider() ?: return SyncResult.TokenMissing

        // 生成に失敗した場合は例外を伝播させず NetworkError 相当にしない(生成はローカル処理)。
        val files = aiExportRepository.buildAndSave()

        return when (val result = api.upload(url, token, files)) {
            UploadResult.Ok -> {
                onSynced()
                SyncResult.Success(files.size)
            }
            UploadResult.Unauthorized -> SyncResult.Unauthorized
            is UploadResult.HttpError -> SyncResult.HttpError(result.statusCode)
            UploadResult.NetworkError -> SyncResult.NetworkError
        }
    }
}
