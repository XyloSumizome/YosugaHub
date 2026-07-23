package com.shiro.yosugahub.data.sync

import com.shiro.yosugahub.data.file.model.AiExportFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

/** アップロード結果。 */
sealed interface UploadResult {
    object Ok : UploadResult
    object Unauthorized : UploadResult
    data class HttpError(val statusCode: Int) : UploadResult
    object NetworkError : UploadResult
}

/** upload.php へ送るリクエスト本文。 */
@Serializable
data class UploadRequest(
    val files: List<AiExportFile>,
)

/**
 * ロリポップ上の受け口(server/upload.php)へ JSON 群を POST する(v4 Phase2)。
 * トークンはヘッダーで送り、URL やログに載せない。
 */
class SyncApi(
    engine: HttpClientEngine = OkHttp.create(),
) {

    private val client = HttpClient(engine) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
        }
    }

    private val json = Json { encodeDefaults = true }

    suspend fun upload(baseUrl: String, token: String, files: List<AiExportFile>): UploadResult {
        val url = baseUrl.trimEnd('/') + "/upload.php"
        return try {
            val response = client.post(url) {
                header(TOKEN_HEADER, token)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(UploadRequest(files)))
            }
            when (val code = response.status.value) {
                HttpStatusCode.OK.value -> UploadResult.Ok
                HttpStatusCode.Unauthorized.value,
                HttpStatusCode.Forbidden.value,
                -> UploadResult.Unauthorized
                else -> UploadResult.HttpError(code)
            }
        } catch (e: IOException) {
            UploadResult.NetworkError
        } catch (e: Exception) {
            // タイムアウト等。例外メッセージにトークンが載らないよう内容は伝播させない。
            UploadResult.NetworkError
        }
    }

    companion object {
        const val TOKEN_HEADER = "X-Yosuga-Token"

        private const val REQUEST_TIMEOUT_MS = 30_000L
        private const val CONNECT_TIMEOUT_MS = 10_000L
    }
}
