package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.github.FetchResult
import com.shiro.yosugahub.data.github.GitHubApi
import com.shiro.yosugahub.data.github.StatusParser
import com.shiro.yosugahub.data.github.model.ProjectStatus
import com.shiro.yosugahub.domain.model.Project

/** 1プロジェクト分の status.json 取得結果(取得〜検証まで)。 */
sealed interface StatusFetchResult {
    data class Success(val projectId: String, val status: ProjectStatus) : StatusFetchResult

    /** リポジトリ未設定(取得対象外)。 */
    data class NotConfigured(val projectId: String) : StatusFetchResult

    /** トークン未設定。非公開リポジトリでは必須。 */
    data class TokenMissing(val projectId: String) : StatusFetchResult

    data class AuthFailed(val projectId: String) : StatusFetchResult
    data class FileNotFound(val projectId: String) : StatusFetchResult
    data class NetworkError(val projectId: String) : StatusFetchResult
    data class HttpError(val projectId: String, val statusCode: Int) : StatusFetchResult
    data class InvalidJson(val projectId: String, val message: String) : StatusFetchResult
    data class UnsupportedSchema(val projectId: String, val version: Int) : StatusFetchResult
    data class ProjectIdMismatch(val projectId: String, val actual: String) : StatusFetchResult
}

/**
 * GitHub から `.yosuga/status.json` を取得する Repository。
 * トークンは通信直前に `tokenProvider` から取り出し、結果や例外に載せない。
 * 供給関数として受けることで、テストで Keystore / DataStore に依存せずに差し替えられる。
 */
class GitHubStatusRepository(
    private val api: GitHubApi,
    private val tokenProvider: suspend () -> String?,
) {

    suspend fun fetchStatus(project: Project): StatusFetchResult {
        if (!project.hasRepository) return StatusFetchResult.NotConfigured(project.id)

        val token = tokenProvider() ?: return StatusFetchResult.TokenMissing(project.id)

        val fetched = api.fetchFile(
            owner = project.repoOwner.orEmpty(),
            repo = project.repoName.orEmpty(),
            path = GitHubApi.STATUS_PATH,
            branch = project.repoBranch,
            token = token,
        )

        return when (fetched) {
            is FetchResult.Unauthorized -> StatusFetchResult.AuthFailed(project.id)
            FetchResult.NotFound -> StatusFetchResult.FileNotFound(project.id)
            FetchResult.NetworkError -> StatusFetchResult.NetworkError(project.id)
            is FetchResult.HttpError -> StatusFetchResult.HttpError(project.id, fetched.statusCode)
            is FetchResult.Success -> toStatusResult(project.id, fetched.content)
        }
    }

    private fun toStatusResult(projectId: String, content: String): StatusFetchResult =
        when (val parsed = StatusParser.parse(content, expectedProjectId = projectId)) {
            is StatusParser.Result.Success ->
                StatusFetchResult.Success(projectId, parsed.status)
            is StatusParser.Result.InvalidJson ->
                StatusFetchResult.InvalidJson(projectId, parsed.message)
            is StatusParser.Result.UnsupportedSchema ->
                StatusFetchResult.UnsupportedSchema(projectId, parsed.version)
            is StatusParser.Result.ProjectIdMismatch ->
                StatusFetchResult.ProjectIdMismatch(projectId, parsed.actual)
        }
}
