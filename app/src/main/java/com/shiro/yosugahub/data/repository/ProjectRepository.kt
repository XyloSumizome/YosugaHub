package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.SampleDataSource
import com.shiro.yosugahub.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * ゲーム制作プロジェクトの進捗を吸収する Repository。
 * 現状はインメモリの仮データ。Phase 3 で GitHub の status.json 取得へ差し替える。
 */
class ProjectRepository(private val source: SampleDataSource) {

    /** 優先タスク(暫定)。後で提案・進捗から導出する。 */
    val priorityTask: String get() = source.priorityTask

    fun projects(): Flow<List<Project>> = flowOf(source.projects)
}
