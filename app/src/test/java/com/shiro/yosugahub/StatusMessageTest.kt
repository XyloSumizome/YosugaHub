package com.shiro.yosugahub

import com.shiro.yosugahub.data.github.model.ProjectStatus
import com.shiro.yosugahub.data.repository.StatusFetchResult
import com.shiro.yosugahub.ui.share.statusFetchMessage
import com.shiro.yosugahub.ui.share.statusRefreshSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusMessageTest {

    private fun success(id: String = "anri") = StatusFetchResult.Success(
        id,
        ProjectStatus(schemaVersion = 1, projectId = id),
        "{}",
    )

    @Test
    fun every_result_has_a_non_empty_message() {
        val results = listOf(
            success(),
            StatusFetchResult.NotConfigured("anri"),
            StatusFetchResult.TokenMissing("anri"),
            StatusFetchResult.AuthFailed("anri"),
            StatusFetchResult.FileNotFound("anri"),
            StatusFetchResult.NetworkError("anri"),
            StatusFetchResult.HttpError("anri", 500),
            StatusFetchResult.InvalidJson("anri", "boom"),
            StatusFetchResult.UnsupportedSchema("anri", 9),
            StatusFetchResult.ProjectIdMismatch("anri", "other"),
        )
        results.forEach { assertTrue(statusFetchMessage(it).isNotBlank()) }
    }

    @Test
    fun messages_include_actionable_details() {
        assertTrue(statusFetchMessage(StatusFetchResult.HttpError("anri", 503)).contains("503"))
        assertTrue(statusFetchMessage(StatusFetchResult.UnsupportedSchema("anri", 9)).contains("9"))
        assertTrue(statusFetchMessage(StatusFetchResult.ProjectIdMismatch("anri", "other")).contains("other"))
        assertTrue(statusFetchMessage(StatusFetchResult.TokenMissing("anri")).contains("設定画面"))
    }

    @Test
    fun summary_counts_success_and_failure() {
        assertEquals("更新対象のリポジトリがありません", statusRefreshSummary(emptyList()))
        assertEquals("2 件の進捗を取得しました", statusRefreshSummary(listOf(success(), success("b"))))
        assertEquals(
            "1 件成功 / 1 件失敗しました",
            statusRefreshSummary(listOf(success(), StatusFetchResult.NetworkError("b"))),
        )
    }
}
