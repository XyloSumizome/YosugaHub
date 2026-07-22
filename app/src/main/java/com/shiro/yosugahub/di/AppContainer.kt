package com.shiro.yosugahub.di

import android.content.Context
import androidx.room.Room
import com.shiro.yosugahub.data.local.db.SampleSeed
import com.shiro.yosugahub.data.local.db.YosugaDatabase
import com.shiro.yosugahub.data.repository.AssistantRepository
import com.shiro.yosugahub.data.repository.CalendarRepository
import com.shiro.yosugahub.data.repository.ProjectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 手動DIコンテナ(設計書3.4: 初期版は手動DIで開始し、規模が増えたら Hilt を検討)。
 * Room データベースと Repository の生成・依存関係の組み立てを一箇所に集約する。
 */
interface AppContainer {
    val calendarRepository: CalendarRepository
    val projectRepository: ProjectRepository
    val assistantRepository: AssistantRepository
}

/** Room を用いる既定の実装。初回起動時に仮データ(SampleSeed)を投入する。 */
class DefaultAppContainer(
    context: Context,
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : AppContainer {

    private val database: YosugaDatabase = Room.databaseBuilder(
        context.applicationContext,
        YosugaDatabase::class.java,
        "yosuga.db",
    ).build()

    override val calendarRepository: CalendarRepository by lazy {
        CalendarRepository(database.calendarEventDao())
    }
    override val projectRepository: ProjectRepository by lazy {
        ProjectRepository(database.projectDao())
    }
    override val assistantRepository: AssistantRepository by lazy {
        AssistantRepository(database.recommendationDao())
    }

    init {
        seedIfEmpty()
    }

    /** テーブルが空のときだけ仮データを投入する(再起動後もデータが残ることを妨げない)。 */
    private fun seedIfEmpty() {
        applicationScope.launch {
            if (database.projectDao().count() == 0) {
                database.projectDao().insertAll(SampleSeed.projects)
            }
            if (database.calendarEventDao().count() == 0) {
                database.calendarEventDao().insertAll(SampleSeed.events)
            }
            if (database.recommendationDao().count() == 0) {
                database.recommendationDao().insertAll(SampleSeed.recommendations)
            }
        }
    }
}
