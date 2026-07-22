package com.shiro.yosugahub.di

import android.content.Context
import androidx.room.Room
import com.shiro.yosugahub.data.local.datastore.UserPreferencesRepository
import com.shiro.yosugahub.data.local.db.SampleSeed
import com.shiro.yosugahub.data.local.db.YosugaDatabase
import com.shiro.yosugahub.data.repository.AssistantRepository
import com.shiro.yosugahub.data.repository.CalendarRepository
import com.shiro.yosugahub.data.repository.ProjectRepository
import com.shiro.yosugahub.util.formatSyncTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * 手動DIコンテナ(設計書3.4: 初期版は手動DIで開始し、規模が増えたら Hilt を検討)。
 * Room データベース・DataStore・Repository の生成と依存関係の組み立てを一箇所に集約する。
 */
interface AppContainer {
    val calendarRepository: CalendarRepository
    val projectRepository: ProjectRepository
    val assistantRepository: AssistantRepository
    val userPreferencesRepository: UserPreferencesRepository
}

/** Room + DataStore を用いる既定の実装。初回起動時に仮データ(SampleSeed)を投入する。 */
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
    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context.applicationContext)
    }

    init {
        seedIfEmpty()
    }

    /**
     * テーブルが空のときだけ仮データを投入する(再起動後の実データを壊さない)。
     * 実際に投入したら最終同期時刻を記録する。Phase 3/4 で本物の同期処理に置き換わる暫定処理。
     */
    private fun seedIfEmpty() {
        applicationScope.launch {
            var seededAnything = false
            if (database.projectDao().count() == 0) {
                database.projectDao().insertAll(SampleSeed.projects)
                seededAnything = true
            }
            if (database.calendarEventDao().count() == 0) {
                database.calendarEventDao().insertAll(SampleSeed.events)
                seededAnything = true
            }
            if (database.recommendationDao().count() == 0) {
                database.recommendationDao().insertAll(SampleSeed.recommendations)
                seededAnything = true
            }
            if (seededAnything) {
                userPreferencesRepository.setLastSyncedAt(formatSyncTime(LocalDateTime.now()))
            }
        }
    }
}
