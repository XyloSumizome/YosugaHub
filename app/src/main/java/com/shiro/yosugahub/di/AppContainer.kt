package com.shiro.yosugahub.di

import com.shiro.yosugahub.data.local.SampleDataSource
import com.shiro.yosugahub.data.repository.AssistantRepository
import com.shiro.yosugahub.data.repository.CalendarRepository
import com.shiro.yosugahub.data.repository.ProjectRepository

/**
 * 手動DIコンテナ(設計書3.4: 初期版は手動DIで開始し、規模が増えたら Hilt を検討)。
 * Repository の生成と依存関係の組み立てを一箇所に集約する。
 */
interface AppContainer {
    val calendarRepository: CalendarRepository
    val projectRepository: ProjectRepository
    val assistantRepository: AssistantRepository
}

/** 仮データ(SampleDataSource)を用いる既定の実装。 */
class DefaultAppContainer : AppContainer {

    private val sampleDataSource = SampleDataSource()

    override val calendarRepository: CalendarRepository by lazy {
        CalendarRepository(sampleDataSource)
    }
    override val projectRepository: ProjectRepository by lazy {
        ProjectRepository(sampleDataSource)
    }
    override val assistantRepository: AssistantRepository by lazy {
        AssistantRepository(sampleDataSource)
    }
}
