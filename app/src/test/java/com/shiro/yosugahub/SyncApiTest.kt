package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.model.AiExportFile
import com.shiro.yosugahub.data.sync.SyncApi
import com.shiro.yosugahub.data.sync.UploadResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncApiTest {

    private val files = listOf(
        AiExportFile("tasks.json", """{"schemaVersion":1}"""),
        AiExportFile("projects.json", """{"schemaVersion":1}"""),
    )

    @Test
    fun posts_to_upload_php_with_token_header_and_files_body() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        val api = SyncApi(engine = MockEngine { request ->
            requests += request
            respond("""{"ok":true,"saved":2}""", HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        })

        val result = api.upload("https://example.com/yosuga/", "secret-token", files)

        assertEquals(UploadResult.Ok, result)
        val request = requests.single()
        assertEquals("https://example.com/yosuga/upload.php", request.url.toString())
        assertEquals("secret-token", request.headers[SyncApi.TOKEN_HEADER])
        val body = (request.body as TextContent).text
        assertTrue(body.contains("tasks.json"))
        assertTrue(body.contains("projects.json"))
    }

    @Test
    fun maps_403_to_unauthorized() = runBlocking {
        val api = SyncApi(engine = MockEngine { respondError(HttpStatusCode.Forbidden) })
        assertEquals(UploadResult.Unauthorized, api.upload("https://e.com/y", "t", files))
    }

    @Test
    fun maps_500_to_http_error() = runBlocking {
        val api = SyncApi(engine = MockEngine { respondError(HttpStatusCode.InternalServerError) })
        val result = api.upload("https://e.com/y", "t", files)
        assertTrue(result is UploadResult.HttpError)
        assertEquals(500, (result as UploadResult.HttpError).statusCode)
    }

    @Test
    fun maps_io_failure_to_network_error() = runBlocking {
        val api = SyncApi(engine = MockEngine { throw java.io.IOException("offline") })
        assertEquals(UploadResult.NetworkError, api.upload("https://e.com/y", "t", files))
    }
}
