package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.db.dao.ProjectDao
import com.shiro.yosugahub.data.local.db.dao.TaskDao
import com.shiro.yosugahub.data.local.db.toDomain
import com.shiro.yosugahub.data.local.db.toEntity
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.ProjectProgress
import com.shiro.yosugahub.util.formatSyncTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * ゲーム制作プロジェクトの進捗を吸収する Repository。
 * 現状は Room(仮データでシード)。将来 GitHub の status.json 取得へ差し替える。
 * now はテスト容易性のため注入可能(lastUpdated の表示形式に合わせる)。
 */
class ProjectRepository(
    private val dao: ProjectDao,
    private val taskDao: TaskDao,
    private val now: () -> String = { formatSyncTime(LocalDateTime.now()) },
) {

    /**
     * プロジェクト一覧。**「作業中 / 次」はタスクから導出して差し替える**(案C)。
     *
     * ここで一度だけ導出することで、ホーム・一覧・AI向けエクスポートのすべてが
     * 同じ値を見る。画面ごとに計算すると、どこか1つを直し忘れて食い違う。
     * タスクが無いプロジェクトは保存済みの文字列のまま(導出で空にしない)。
     */
    fun projects(): Flow<List<Project>> =
        combine(dao.observeAll(), taskDao.observeAll()) { projects, tasks ->
            ProjectProgress.deriveAll(
                projects = projects.map { it.toDomain() },
                tasks = tasks.map { it.toDomain() },
            )
        }

    /** 実在するプロジェクトIDか(指示書の宛先確認など)。 */
    suspend fun exists(projectId: String): Boolean =
        projectId.isNotBlank() && dao.countById(projectId) > 0

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

    /**
     * プロジェクトを削除する。**この Repository はプロジェクト行だけを消す。**
     * 紐づくタスク・進捗キャッシュの後始末は呼び出し側が行う(孤児レコードを残さないこと)。
     */
    suspend fun delete(projectId: String): Boolean = dao.deleteById(projectId) > 0
}
