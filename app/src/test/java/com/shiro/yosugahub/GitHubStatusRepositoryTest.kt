package com.shiro.yosugahub

import com.shiro.yosugahub.data.github.GitHubApi
import com.shiro.yosugahub.data.repository.GitHubStatusRepository
import com.shiro.yosugahub.data.repository.StatusFetchResult
import com.shiro.yosugahub.domain.model.Project
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MockEngine で GitHub 応答を差し替え、取得〜検証〜エラー分類を確認する。
 * トークンは供給関数を差し替えるだけでよく、Keystore / DataStore に依存しない。
 */
class GitHubStatusRepositoryTest {

    private val project = Project(
        id = "anri",
        name = "ANRI",
        currentGoal = "",
        inProgress = "",
        nextTask = "",
        lastUpdated = "",
        health = "on_track",
        repoOwner = "shiro",
        repoName = "anri",
        repoBranch = null,
    )

    private val validStatus = """{"schemaVersion":1,"projectId":"anri","summary":"進行中"}"""

    private fun repository(
        token: String? = "ghp_test",
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): Pair<GitHubStatusRepository, MutableList<HttpRequestData>> {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        return GitHubStatusRepository(
            api = GitHubApi(engine = engine),
            tokenProvider = { token },
        ) to requests
    }

    @Test
    fun fetches_status_and_sends_auth_header_and_correct_path() = runBlocking {
        val (repo, requests) = repository {
            respond(
                content = validStatus,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }
        val result = repo.fetchStatus(project)
        assertTrue(result is StatusFetchResult.Success)
        assertEquals("進行中", (result as StatusFetchResult.Success).status.summary)

        val request = requests.single()
        assertTrue(request.url.toString().contains("/repos/shiro/anri/contents/.yosuga/status.json"))
        assertEquals("Bearer ghp_test", request.headers["Authorization"])
        assertEquals("application/vnd.github.raw", request.headers["Accept"])
    }

    @Test
    fun adds_ref_parameter_when_branch_is_set() = runBlocking {
        val (repo, requests) = repository {
            respond(validStatus, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        repo.fetchStatus(project.copy(repoBranch = "develop"))
        assertTrue(requests.single().url.toString().contains("ref=develop"))
    }

    @Test
    fun returns_not_configured_when_repository_missing() = runBlocking {
        val (repo, requests) = repository { respond("", HttpStatusCode.OK) }
        val result = repo.fetchStatus(project.copy(repoOwner = null, repoName = null))
        assertTrue(result is StatusFetchResult.NotConfigured)
        assertTrue(requests.isEmpty())  // 通信していない
    }

    @Test
    fun returns_token_missing_without_calling_api() = runBlocking {
        val (repo, requests) = repository(token = null) { respond("", HttpStatusCode.OK) }
        val result = repo.fetchStatus(project)
        assertTrue(result is StatusFetchResult.TokenMissing)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun maps_401_and_403_to_auth_failed() = runBlocking {
        val (repo401, _) = repository { respondError(HttpStatusCode.Unauthorized) }
        assertTrue(repo401.fetchStatus(project) is StatusFetchResult.AuthFailed)

        val (repo403, _) = repository { respondError(HttpStatusCode.Forbidden) }
        assertTrue(repo403.fetchStatus(project) is StatusFetchResult.AuthFailed)
    }

    @Test
    fun maps_404_to_file_not_found() = runBlocking {
        val (repo, _) = repository { respondError(HttpStatusCode.NotFound) }
        assertTrue(repo.fetchStatus(project) is StatusFetchResult.FileNotFound)
    }

    @Test
    fun maps_other_http_errors() = runBlocking {
        val (repo, _) = repository { respondError(HttpStatusCode.InternalServerError) }
        val result = repo.fetchStatus(project)
        assertTrue(result is StatusFetchResult.HttpError)
        assertEquals(500, (result as StatusFetchResult.HttpError).statusCode)
    }

    @Test
    fun maps_network_failure() = runBlocking {
        val (repo, _) = repository { throw java.io.IOException("offline") }
        assertTrue(repo.fetchStatus(project) is StatusFetchResult.NetworkError)
    }

    @Test
    fun maps_broken_json_and_schema_and_id_mismatch() = runBlocking {
        val (broken, _) = repository { respond("{ oops", HttpStatusCode.OK) }
        assertTrue(broken.fetchStatus(project) is StatusFetchResult.InvalidJson)

        val (unsupported, _) = repository {
            respond("""{"schemaVersion":9,"projectId":"anri"}""", HttpStatusCode.OK)
        }
        assertTrue(unsupported.fetchStatus(project) is StatusFetchResult.UnsupportedSchema)

        val (mismatch, _) = repository {
            respond("""{"schemaVersion":1,"projectId":"other"}""", HttpStatusCode.OK)
        }
        val result = mismatch.fetchStatus(project)
        assertTrue(result is StatusFetchResult.ProjectIdMismatch)
        assertEquals("other", (result as StatusFetchResult.ProjectIdMismatch).actual)
    }
}
