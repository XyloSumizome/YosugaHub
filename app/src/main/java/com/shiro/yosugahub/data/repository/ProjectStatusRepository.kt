package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.github.StatusParser
import com.shiro.yosugahub.data.github.toSnapshot
import com.shiro.yosugahub.data.local.db.dao.ProjectStatusDao
import com.shiro.yosugahub.data.local.db.entity.ProjectStatusCacheEntity
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.ProjectStatusSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.OffsetDateTime

/** 取得と、その直後の自動同期をまとめた結果。同期を試みなかったときは sync が null。 */
data class StatusRefreshResult(
    val fetch: StatusFetchResult,
    val sync: SyncResult? = null,
)

/** 一括更新の結果。同期は最後に1回だけなので sync も1つ。 */
data class StatusRefreshAllResult(
    val fetches: List<StatusFetchResult>,
    val sync: SyncResult? = null,
)

/**
 * GitHub 由来の進捗(status.json)のキャッシュと更新を担う Repository(3-c)。
 *
 * - 取得成功時のみキャッシュを更新する(失敗しても直近の表示を壊さない = オフラインでも見える)
 * - projects テーブルは**上書きしない**。手動編集(1-d)と衝突させないため、
 *   GitHub 由来の情報は独立したセクションとして表示する
 * - fetch は関数として受け、テストで差し替え可能にする
 */
class ProjectStatusRepository(
    private val dao: ProjectStatusDao,
    private val fetch: suspend (Project) -> StatusFetchResult,
    private val now: () -> String = { OffsetDateTime.now().toString() },
    /**
     * 取得が成立した直後にサーバーへ反映する(取り込み時の自動同期と同じ考え方)。
     * これが無いと「GitHubから更新」しても、レコルが読む projects.json は古いままになる。
     * 未配線なら null を返す。
     */
    private val syncAfterFetch: suspend () -> SyncResult? = { null },
) {

    /** キャッシュ済みの進捗(projectId → スナップショット)。壊れた行は読み飛ばす。 */
    fun statuses(): Flow<Map<String, ProjectStatusSnapshot>> =
        dao.observeAll().map { rows ->
            rows.mapNotNull { row -> row.toSnapshotOrNull() }
                .associateBy { it.projectId }
        }

    /** 1プロジェクトを更新する。成功時のみキャッシュへ保存し、そのままサーバーへ反映する。 */
    suspend fun refresh(project: Project): StatusRefreshResult {
        val result = fetchAndCache(project)
        // 取得できなかったときは送る中身が変わらないので、通信を無駄に増やさない。
        val sync = if (result is StatusFetchResult.Success) syncAfterFetch() else null
        return StatusRefreshResult(fetch = result, sync = sync)
    }

    private suspend fun fetchAndCache(project: Project): StatusFetchResult {
        val result = fetch(project)
        if (result is StatusFetchResult.Success) {
            dao.upsert(
                ProjectStatusCacheEntity(
                    projectId = result.projectId,
                    statusJson = result.rawJson,
                    fetchedAt = now(),
                )
            )
        }
        return result
    }

    /** 進捗キャッシュを捨てる(プロジェクト削除の後始末)。 */
    suspend fun deleteCache(projectId: String) {
        dao.deleteByProject(projectId)
    }

    /**
     * リポジトリが設定されているプロジェクトをまとめて更新する。
     * 未設定のプロジェクトは通信せず結果からも除く。
     * サーバーへ送るのは毎回スナップショット全体なので、**同期は最後に1回だけ**行う。
     */
    suspend fun refreshAll(projects: List<Project>): StatusRefreshAllResult {
        val results = projects.filter { it.hasRepository }.map { fetchAndCache(it) }
        val sync = if (results.any { it is StatusFetchResult.Success }) syncAfterFetch() else null
        return StatusRefreshAllResult(fetches = results, sync = sync)
    }

    private fun ProjectStatusCacheEntity.toSnapshotOrNull(): ProjectStatusSnapshot? =
        when (val parsed = StatusParser.parse(statusJson, expectedProjectId = projectId)) {
            is StatusParser.Result.Success ->
                parsed.status.toSnapshot(projectId = projectId, fetchedAt = fetchedAt)
            else -> null
        }
}
