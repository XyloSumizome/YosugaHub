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
) {

    /** キャッシュ済みの進捗(projectId → スナップショット)。壊れた行は読み飛ばす。 */
    fun statuses(): Flow<Map<String, ProjectStatusSnapshot>> =
        dao.observeAll().map { rows ->
            rows.mapNotNull { row -> row.toSnapshotOrNull() }
                .associateBy { it.projectId }
        }

    /** 1プロジェクトを更新する。成功時のみキャッシュへ保存。 */
    suspend fun refresh(project: Project): StatusFetchResult {
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

    /**
     * リポジトリが設定されているプロジェクトをまとめて更新する。
     * 未設定のプロジェクトは通信せず結果からも除く。
     */
    suspend fun refreshAll(projects: List<Project>): List<StatusFetchResult> =
        projects.filter { it.hasRepository }.map { refresh(it) }

    private fun ProjectStatusCacheEntity.toSnapshotOrNull(): ProjectStatusSnapshot? =
        when (val parsed = StatusParser.parse(statusJson, expectedProjectId = projectId)) {
            is StatusParser.Result.Success ->
                parsed.status.toSnapshot(projectId = projectId, fetchedAt = fetchedAt)
            else -> null
        }
}
