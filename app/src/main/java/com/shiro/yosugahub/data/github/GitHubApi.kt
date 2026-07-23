package com.shiro.yosugahub.data.github

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import java.io.IOException

/** GitHub からのファイル取得結果。UI 向けメッセージは呼び出し側で組み立てる。 */
sealed interface FetchResult {
    data class Success(val content: String) : FetchResult

    /** 401 / 403: トークン未設定・失効・権限不足。 */
    data class Unauthorized(val statusCode: Int) : FetchResult

    /** 404: リポジトリ名違い、またはファイル未作成。 */
    object NotFound : FetchResult

    /** その他のHTTPエラー。 */
    data class HttpError(val statusCode: Int) : FetchResult

    /** 通信できない(オフライン・タイムアウトなど)。 */
    object NetworkError : FetchResult
}

/**
 * GitHub Contents API から `.yosuga/` のファイルを取得する(設計書19章)。
 * 非公開リポジトリ前提のためトークンを Authorization ヘッダーへ付ける。
 * トークンはここでは保持せず、呼び出しごとに引数で受け取る(ログにも出さない)。
 *
 * engine を差し替えられるようにしてテストで MockEngine を使う。
 */
class GitHubApi(
    engine: HttpClientEngine = OkHttp.create(),
    private val baseUrl: String = "https://api.github.com",
) {

    private val client = HttpClient(engine) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
        }
    }

    /**
     * 指定パスのファイル内容を取得する。
     * `Accept: application/vnd.github.raw` で本文をそのまま受け取る(Base64 デコード不要)。
     */
    suspend fun fetchFile(
        owner: String,
        repo: String,
        path: String,
        branch: String?,
        token: String?,
    ): FetchResult {
        val url = buildString {
            append(baseUrl)
            append("/repos/").append(owner.encodeURLPathPart())
            append("/").append(repo.encodeURLPathPart())
            append("/contents/").append(path)
            if (!branch.isNullOrBlank()) append("?ref=").append(branch.encodeURLPathPart())
        }

        return try {
            val response = client.get(url) {
                header("Accept", "application/vnd.github.raw")
                header("X-GitHub-Api-Version", API_VERSION)
                if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
            }
            when (val code = response.status.value) {
                HttpStatusCode.OK.value -> FetchResult.Success(response.bodyAsText())
                HttpStatusCode.Unauthorized.value,
                HttpStatusCode.Forbidden.value,
                -> FetchResult.Unauthorized(code)
                HttpStatusCode.NotFound.value -> FetchResult.NotFound
                else -> FetchResult.HttpError(code)
            }
        } catch (e: IOException) {
            FetchResult.NetworkError
        } catch (e: Exception) {
            // タイムアウト等。例外メッセージにトークンが載らないよう内容は伝播させない。
            FetchResult.NetworkError
        }
    }

    companion object {
        const val STATUS_PATH = ".yosuga/status.json"

        private const val API_VERSION = "2022-11-28"
        private const val REQUEST_TIMEOUT_MS = 15_000L
        private const val CONNECT_TIMEOUT_MS = 10_000L
    }
}
