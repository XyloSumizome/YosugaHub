package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.file.model.AiExportFile
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

    /**
     * 同期先が `http://`(2026-07-26)。
     *
     * targetSdk 28 以降、平文HTTPは Android が既定でブロックする。
     * そのまま送ると通信層で弾かれ「通信できませんでした」としか出ず、
     * サーバーやトークンを疑って回り道することになる(実際にした)。
     * **トークンが平文で流れるので、通っては困る**。理由を言って止める。
     */
    object InsecureUrl : SyncResult
}

/**
 * AI向けJSONを生成してロリポップへ同期する(v4 Phase2 / v4.1 で文書の状態遷移を追加)。
 * URL・トークンは供給関数で受け、通信直前にのみ取り出す。
 * 成功時は onSynced(最終同期時刻の記録など)と onDocumentsUploaded を呼ぶ。
 */
class ServerSyncRepository(
    /** AI向けJSONの生成(既定は AiExportRepository::buildAndSave)。 */
    private val buildFiles: suspend () -> List<AiExportFile>,
    private val api: SyncApi,
    private val urlProvider: suspend () -> String,
    private val tokenProvider: suspend () -> String?,
    private val onSynced: suspend () -> Unit = {},
    /** アップロード成功時に未整理文書を「分類待ち」へ進める(v4.1)。 */
    private val onDocumentsUploaded: suspend () -> Unit = {},
) {

    suspend fun sync(): SyncResult {
        val url = urlProvider().trim()
        if (url.isEmpty()) return SyncResult.UrlNotConfigured
        // 送る前に止める。通信層まで行かせると原因が分からないエラーになる。
        if (!url.startsWith("https://", ignoreCase = true)) return SyncResult.InsecureUrl
        val token = tokenProvider() ?: return SyncResult.TokenMissing

        // 生成に失敗した場合は例外を伝播させず NetworkError 相当にしない(生成はローカル処理)。
        val files = buildFiles()

        return when (val result = api.upload(url, token, files)) {
            UploadResult.Ok -> {
                onSynced()
                // 送信後に追加された文書もここで pending になるが、pending も毎回送るため
                // 次回の同期で必ずヨスガの手元に届く(取りこぼしにはならない)。
                onDocumentsUploaded()
                SyncResult.Success(files.size)
            }
            UploadResult.Unauthorized -> SyncResult.Unauthorized
            is UploadResult.HttpError -> SyncResult.HttpError(result.statusCode)
            UploadResult.NetworkError -> SyncResult.NetworkError
        }
    }
}
