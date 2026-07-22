package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.db.dao.ProjectDao
import com.shiro.yosugahub.data.local.db.toDomain
import com.shiro.yosugahub.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ゲーム制作プロジェクトの進捗を吸収する Repository。
 * 現状は Room(仮データでシード)。Phase 3 で GitHub の status.json 取得へ差し替える。
 */
class ProjectRepository(private val dao: ProjectDao) {

    /** 優先タスク(暫定)。後で提案・進捗から導出する。 */
    val priorityTask: String get() = PLACEHOLDER_PRIORITY_TASK

    fun projects(): Flow<List<Project>> =
        dao.observeAll().map { projects -> projects.map { it.toDomain() } }

    private companion object {
        const val PLACEHOLDER_PRIORITY_TASK = "ANRI: メインシナリオ第2章の執筆を進める"
    }
}
