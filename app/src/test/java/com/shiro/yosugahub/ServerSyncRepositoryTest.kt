package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.model.AiExportFile
import com.shiro.yosugahub.data.repository.ServerSyncRepository
import com.shiro.yosugahub.data.repository.SyncResult
import com.shiro.yosugahub.data.sync.SyncApi
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

/**
 * 同期の副作用(最終同期時刻の記録 / 文書を「分類待ち」へ進める)が
 * 成功時にだけ起きることを検証する(v4 Phase2 / v4.1)。
 */
class ServerSyncRepositoryTest {

    private val files = listOf(AiExportFile("documents.json", """{"schemaVersion":1}"""))

    private fun okApi() = SyncApi(engine = MockEngine {
        respond("""{"ok":true}""", HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
    })

    @Test
    fun `http は送る前に止める`() = runBlocking {
        var built = false
        val repository = ServerSyncRepository(
            buildFiles = { built = true; files },
            api = okApi(),
            urlProvider = { "http://example.com/yosuga" },
            tokenProvider = { "secret-token" },
        )

        val result = repository.sync()

        // 平文HTTPは Android が通信層で弾き「通信できませんでした」としか出ない。
        // 原因が分かる形で、送る前に止める(トークンが平文で流れるので通っても困る)。
        assertEquals(SyncResult.InsecureUrl, result)
        assertFalse(built)
    }

    @Test
    fun `未設定は http 判定より先に出す`() = runBlocking {
        val repository = ServerSyncRepository(
            buildFiles = { files },
            api = okApi(),
            urlProvider = { "  " },
            tokenProvider = { "secret-token" },
        )

        // 空欄の人に「https にしろ」と言っても意味が通らない。
        assertEquals(SyncResult.UrlNotConfigured, repository.sync())
    }

    @Test
    fun success_records_sync_time_and_marks_documents_uploaded() = runBlocking {
        var synced = false
        var documentsUploaded = false
        val repository = ServerSyncRepository(
            buildFiles = { files },
            api = okApi(),
            urlProvider = { "https://example.com/yosuga/" },
            tokenProvider = { "secret-token" },
            onSynced = { synced = true },
            onDocumentsUploaded = { documentsUploaded = true },
        )

        val result = repository.sync()

        assertEquals(SyncResult.Success(1), result)
        assertTrue(synced)
        assertTrue(documentsUploaded)
    }

    @Test
    fun http_error_leaves_documents_unclassified() = runBlocking {
        var synced = false
        var documentsUploaded = false
        val repository = ServerSyncRepository(
            buildFiles = { files },
            api = SyncApi(engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }),
            urlProvider = { "https://example.com/yosuga/" },
            tokenProvider = { "secret-token" },
            onSynced = { synced = true },
            onDocumentsUploaded = { documentsUploaded = true },
        )

        val result = repository.sync()

        assertEquals(SyncResult.HttpError(500), result)
        assertFalse(synced)
        // 届いていない文書を「分類待ち」にしない(次回の同期でもう一度送られる)。
        assertFalse(documentsUploaded)
    }

    @Test
    fun missing_url_skips_upload_entirely() = runBlocking {
        var built = false
        var documentsUploaded = false
        val repository = ServerSyncRepository(
            buildFiles = { built = true; files },
            api = okApi(),
            urlProvider = { "   " },
            tokenProvider = { "secret-token" },
            onDocumentsUploaded = { documentsUploaded = true },
        )

        assertEquals(SyncResult.UrlNotConfigured, repository.sync())
        assertFalse(built)
        assertFalse(documentsUploaded)
    }

    @Test
    fun missing_token_skips_upload_entirely() = runBlocking {
        var documentsUploaded = false
        val repository = ServerSyncRepository(
            buildFiles = { files },
            api = okApi(),
            urlProvider = { "https://example.com/yosuga/" },
            tokenProvider = { null },
            onDocumentsUploaded = { documentsUploaded = true },
        )

        assertEquals(SyncResult.TokenMissing, repository.sync())
        assertFalse(documentsUploaded)
    }
}
