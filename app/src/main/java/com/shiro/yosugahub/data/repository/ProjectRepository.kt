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

    /** 優先タスク(暫定)。後で提案・進捗から導出する。 */
    val priorityTask: String get() = PLACEHOLDER_PRIORITY_TASK

    fun projects(): Flow<List<Project>> =
        dao.observeAll().map { projects -> projects.map { it.toDomain() } }

    /** プロジェクト編集の保存(1-d)。lastUpdated はここで刻む。 */
    suspend fun upsert(project: Project) {
        dao.upsert(project.copy(lastUpdated = now()).toEntity())
    }

    private companion object {
        const val PLACEHOLDER_PRIORITY_TASK = "ANRI: メインシナリオ第2章の執筆を進める"
    }
}
