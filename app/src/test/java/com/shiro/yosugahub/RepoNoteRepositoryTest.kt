package com.shiro.yosugahub

import com.shiro.yosugahub.data.github.GitHubApi
import com.shiro.yosugahub.data.repository.NoteFetchResult
import com.shiro.yosugahub.data.repository.RepoNoteRepository
import com.shiro.yosugahub.domain.model.Project
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** MockEngine で GitHub 応答を差し替え、一覧→本文取得→取得済みスキップを確認する。 */
class RepoNoteRepositoryTest {

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
        repoBranch = "main",
    )

    private fun listingJson(vararg names: String) = names.joinToString(",", "[", "]") { name ->
        """{"name":"$name","path":".yosuga/notes/$name","sha":"sha-$name","size":10,"type":"file"}"""
    }

    /** 一覧は listing、本文はパス末尾を返す簡易サーバー。 */
    private fun repository(
        listing: String,
        failingPaths: Set<String> = emptySet(),
        onRequest: (String) -> Unit = {},
    ): RepoNoteRepository {
        val engine = MockEngine { request ->
            val url = request.url.toString()
            onRequest(url)
            when {
                failingPaths.any { url.contains(it) } ->
                    respondError(HttpStatusCode.InternalServerError)

                url.contains("/contents/.yosuga/notes?") || url.endsWith("/contents/.yosuga/notes") ->
                    respond(listing, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))

                else -> respond(
                    "本文: ${url.substringAfterLast('/').substringBefore('?')}",
                    HttpStatusCode.OK,
                    headersOf("Content-Type", "text/plain"),
                )
            }
        }
        return RepoNoteRepository(
            api = GitHubApi(engine = engine),
            tokenProvider = { "token" },
        )
    }

    @Test
    fun fetches_every_note_when_nothing_is_known() = runBlocking {
        val repository = repository(listingJson("2026-07-24-a.md", "2026-07-24-b.md"))

        val result = repository.fetchNewNotes(project, knownShas = emptySet())

        result as NoteFetchResult.Success
        assertEquals(2, result.fetched.size)
        assertEquals(0, result.skipped)
        assertTrue(result.failed.isEmpty())
        assertTrue(result.fetched.all { it.projectId == "anri" })
        assertTrue(result.fetched[0].content.startsWith("本文:"))
    }

    @Test
    fun already_fetched_notes_are_skipped_by_sha() = runBlocking {
        var requests = 0
        val repository = repository(
            listingJson("2026-07-24-a.md", "2026-07-24-b.md"),
            onRequest = { requests++ },
        )

        val result = repository.fetchNewNotes(
            project,
            knownShas = setOf("sha-2026-07-24-a.md"),
        )

        result as NoteFetchResult.Success
        assertEquals(listOf("2026-07-24-b.md"), result.fetched.map { it.note.name })
        assertEquals(1, result.skipped)
        // 一覧1回 + 本文1回。取得済みの本文は取りに行かない
        assertEquals(2, requests)
    }

    @Test
    fun a_single_failed_body_does_not_stop_the_rest() = runBlocking {
        val repository = repository(
            listingJson("2026-07-24-a.md", "2026-07-24-b.md"),
            failingPaths = setOf("2026-07-24-a.md"),
        )

        val result = repository.fetchNewNotes(project, knownShas = emptySet())

        result as NoteFetchResult.Success
        assertEquals(listOf("2026-07-24-b.md"), result.fetched.map { it.note.name })
        assertEquals(listOf(".yosuga/notes/2026-07-24-a.md"), result.failed)
    }

    @Test
    fun a_missing_notes_directory_is_not_an_error() = runBlocking {
        val engine = MockEngine { respondError(HttpStatusCode.NotFound) }
        val repository = RepoNoteRepository(GitHubApi(engine = engine)) { "token" }

        val result = repository.fetchNewNotes(project, knownShas = emptySet())

        // まだノートを書いていないゲームでは普通に起こる
        assertTrue(result is NoteFetchResult.NoNotesDirectory)
    }

    @Test
    fun unauthorized_is_reported_separately() = runBlocking {
        val engine = MockEngine { respondError(HttpStatusCode.Unauthorized) }
        val repository = RepoNoteRepository(GitHubApi(engine = engine)) { "token" }

        assertTrue(repository.fetchNewNotes(project, emptySet()) is NoteFetchResult.AuthFailed)
    }

    @Test
    fun a_project_without_a_repository_is_skipped() = runBlocking {
        val repository = repository(listingJson())
        val local = project.copy(repoOwner = null, repoName = null)

        assertTrue(
            repository.fetchNewNotes(local, emptySet()) is NoteFetchResult.NotConfigured,
        )
    }

    @Test
    fun a_missing_token_is_reported_before_any_request() = runBlocking {
        var requests = 0
        val engine = MockEngine {
            requests++
            respond("[]", HttpStatusCode.OK)
        }
        val repository = RepoNoteRepository(GitHubApi(engine = engine)) { null }

        assertTrue(repository.fetchNewNotes(project, emptySet()) is NoteFetchResult.TokenMissing)
        assertEquals(0, requests)
    }

    @Test
    fun a_broken_listing_is_reported_as_invalid() = runBlocking {
        val repository = repository("[{")

        assertTrue(
            repository.fetchNewNotes(project, emptySet()) is NoteFetchResult.InvalidListing,
        )
    }

    @Test
    fun non_ascii_note_names_produce_a_valid_encoded_url() = runBlocking {
        var bodyUrl = ""
        val engine = MockEngine { request ->
            val url = request.url.toString()
            if (url.contains("/contents/.yosuga/notes?") || url.endsWith("/contents/.yosuga/notes")) {
                respond(
                    listingJson("2026-07-24-光の設計.md"),
                    HttpStatusCode.OK,
                    headersOf("Content-Type", "application/json"),
                )
            } else {
                bodyUrl = url
                respond("本文", HttpStatusCode.OK)
            }
        }
        val repository = RepoNoteRepository(GitHubApi(engine = engine)) { "token" }

        repository.fetchNewNotes(project, knownShas = emptySet())

        // 日本語が生のまま URL に載らない(パーセントエンコードされる)
        assertFalse(bodyUrl.contains("光"))
        assertTrue(bodyUrl.contains("%"))
        // 区切りの / は保たれている
        assertTrue(bodyUrl.contains("/contents/.yosuga/notes/"))
    }
}
