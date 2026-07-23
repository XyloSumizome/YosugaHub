package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.db.dao.ProjectDao
import com.shiro.yosugahub.data.local.db.toDomain
import com.shiro.yosugahub.data.local.db.toEntity
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.util.formatSyncTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * ゲーム制作プロジェクトの進捗を吸収する Repository。
 * 現状は Room(仮データでシード)。将来 GitHub の status.json 取得へ差し替える。
 * now はテスト容易性のため注入可能(lastUpdated の表示形式に合わせる)。
 */
class ProjectRepository(
    private val dao: ProjectDao,
    private val now: () -> String = { formatSyncTime(LocalDateTime.now()) },
) {

    fun projects(): Flow<List<Project>> =
        dao.observeAll().map { projects -> projects.map { it.toDomain() } }

    /** プロジェクト編集の保存(1-d)。lastUpdated はここで刻む。 */
    suspend fun upsert(project: Project) {
        dao.upsert(project.copy(lastUpdated = now()).toEntity())
    }

    /**
     * 健康状態の更新(提案承認用・2-c)。AI分析由来の自由な値(タスク過多 等)も受け入れる。
     * 対象プロジェクトが存在しない場合は false。
     */
    suspend fun updateHealth(projectId: String, health: String): Boolean =
        dao.updateHealth(id = projectId, health = health, lastUpdated = now()) > 0
}
