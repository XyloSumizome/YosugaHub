package com.shiro.yosugahub.di

import android.content.Context
import androidx.room.Room
import com.shiro.yosugahub.data.calendar.DeviceCalendarDataSource
import com.shiro.yosugahub.data.local.datastore.UserPreferencesRepository
import com.shiro.yosugahub.data.local.db.MIGRATION_1_2
import com.shiro.yosugahub.data.local.db.MIGRATION_2_3
import com.shiro.yosugahub.data.local.db.MIGRATION_3_4
import com.shiro.yosugahub.data.local.db.MIGRATION_4_5
import com.shiro.yosugahub.data.local.db.MIGRATION_5_6
import com.shiro.yosugahub.data.local.db.SampleSeed
import com.shiro.yosugahub.data.local.db.YosugaDatabase
import com.shiro.yosugahub.data.obsidian.KnowledgeStore
import com.shiro.yosugahub.data.obsidian.ObsidianVaultStore
import com.shiro.yosugahub.data.security.KeystoreTokenCrypto
import com.shiro.yosugahub.data.repository.AssistantRepository
import com.shiro.yosugahub.data.repository.CalendarRepository
import com.shiro.yosugahub.data.repository.DiaryRepository
import com.shiro.yosugahub.data.repository.DocumentRepository
import com.shiro.yosugahub.data.repository.ExportRepository
import com.shiro.yosugahub.data.github.GitHubApi
import com.shiro.yosugahub.data.repository.AiExportRepository
import com.shiro.yosugahub.data.repository.GitHubSettingsRepository
import com.shiro.yosugahub.data.repository.GitHubStatusRepository
import com.shiro.yosugahub.data.repository.ImportRepository
import com.shiro.yosugahub.data.repository.KnowledgeRepository
import com.shiro.yosugahub.data.repository.ProjectRepository
import com.shiro.yosugahub.data.repository.ProjectStatusRepository
import com.shiro.yosugahub.data.repository.ProposalRepository
import com.shiro.yosugahub.data.repository.ServerSyncRepository
import com.shiro.yosugahub.data.repository.SyncSettingsRepository
import com.shiro.yosugahub.data.repository.TaskRepository
import com.shiro.yosugahub.data.sync.SyncApi
import com.shiro.yosugahub.util.formatSyncTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * 手動DIコンテナ(設計書3.4: 初期版は手動DIで開始し、規模が増えたら Hilt を検討)。
 * Room データベース・DataStore・Repository の生成と依存関係の組み立てを一箇所に集約する。
 */
interface AppContainer {
    val calendarRepository: CalendarRepository
    val projectRepository: ProjectRepository
    val taskRepository: TaskRepository
    val knowledgeRepository: KnowledgeRepository
    val diaryRepository: DiaryRepository
    val documentRepository: DocumentRepository
    val assistantRepository: AssistantRepository
    val proposalRepository: ProposalRepository
    val userPreferencesRepository: UserPreferencesRepository
    val gitHubSettingsRepository: GitHubSettingsRepository
    val gitHubStatusRepository: GitHubStatusRepository
    val projectStatusRepository: ProjectStatusRepository
    val exportRepository: ExportRepository
    val importRepository: ImportRepository
    val syncSettingsRepository: SyncSettingsRepository
    val serverSyncRepository: ServerSyncRepository
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
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .build()

    override val calendarRepository: CalendarRepository by lazy {
        CalendarRepository(
            dao = database.calendarEventDao(),
            dataSource = DeviceCalendarDataSource(context.applicationContext),
        )
    }
    override val projectRepository: ProjectRepository by lazy {
        ProjectRepository(database.projectDao())
    }
    override val taskRepository: TaskRepository by lazy {
        TaskRepository(database.taskDao())
    }
    override val knowledgeRepository: KnowledgeRepository by lazy {
        KnowledgeRepository(database.knowledgeDao())
    }
    override val diaryRepository: DiaryRepository by lazy {
        DiaryRepository(database.diaryDao())
    }
    override val documentRepository: DocumentRepository by lazy {
        DocumentRepository(database.documentDao())
    }
    override val assistantRepository: AssistantRepository by lazy {
        AssistantRepository(database.recommendationDao())
    }
    /** 長文知識の書き出し先(初期実装は Obsidian Vault / SAF)。 */
    private val knowledgeStore: KnowledgeStore by lazy {
        ObsidianVaultStore(context.applicationContext, userPreferencesRepository)
    }

    override val proposalRepository: ProposalRepository by lazy {
        ProposalRepository(
            dao = database.pendingProposalDao(),
            taskRepository = taskRepository,
            knowledgeRepository = knowledgeRepository,
            diaryRepository = diaryRepository,
            projectRepository = projectRepository,
            knowledgeStore = knowledgeStore,
        )
    }
    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context.applicationContext)
    }
    override val gitHubSettingsRepository: GitHubSettingsRepository by lazy {
        GitHubSettingsRepository(userPreferencesRepository, tokenCrypto)
    }
    override val gitHubStatusRepository: GitHubStatusRepository by lazy {
        GitHubStatusRepository(
            api = GitHubApi(),
            tokenProvider = { gitHubSettingsRepository.currentToken() },
        )
    }
    override val projectStatusRepository: ProjectStatusRepository by lazy {
        ProjectStatusRepository(
            dao = database.projectStatusDao(),
            fetch = { project -> gitHubStatusRepository.fetchStatus(project) },
        )
    }
    override val exportRepository: ExportRepository by lazy {
        ExportRepository(
            context.applicationContext,
            projectRepository,
            calendarRepository,
            taskRepository,
            knowledgeRepository,
            projectStatusRepository,
        )
    }
    override val importRepository: ImportRepository by lazy {
        ImportRepository(
            context.applicationContext,
            database.recommendationDao(),
            database.pendingProposalDao(),
        )
    }

    private val tokenCrypto by lazy { KeystoreTokenCrypto() }

    override val syncSettingsRepository: SyncSettingsRepository by lazy {
        SyncSettingsRepository(userPreferencesRepository, tokenCrypto)
    }

    private val aiExportRepository: AiExportRepository by lazy {
        AiExportRepository(
            context = context.applicationContext,
            projectRepository = projectRepository,
            taskRepository = taskRepository,
            knowledgeRepository = knowledgeRepository,
            diaryRepository = diaryRepository,
            calendarRepository = calendarRepository,
            projectStatusRepository = projectStatusRepository,
            pendingProposalDao = database.pendingProposalDao(),
        )
    }

    override val serverSyncRepository: ServerSyncRepository by lazy {
        ServerSyncRepository(
            aiExportRepository = aiExportRepository,
            api = SyncApi(),
            urlProvider = { syncSettingsRepository.baseUrl.first() },
            tokenProvider = { syncSettingsRepository.currentToken() },
            onSynced = {
                userPreferencesRepository.setLastSyncedAt(
                    formatSyncTime(LocalDateTime.now())
                )
            },
        )
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
            // カレンダーは端末から同期する実データに置き換わったためシードしない
            // (SampleSeed.events はテスト用に残している)。
            if (database.recommendationDao().count() == 0) {
                database.recommendationDao().insertAll(SampleSeed.recommendations)
                seededAnything = true
            }
            if (database.taskDao().count() == 0) {
                database.taskDao().insertAll(SampleSeed.tasks)
                seededAnything = true
            }
            if (database.knowledgeDao().countItems() == 0) {
                SampleSeed.knowledgeItems.forEach { database.knowledgeDao().upsertItem(it) }
                SampleSeed.tags.forEach { database.knowledgeDao().insertTag(it) }
                SampleSeed.entities.forEach { database.knowledgeDao().insertEntity(it) }
                SampleSeed.itemTags.forEach { database.knowledgeDao().insertItemTag(it) }
                SampleSeed.itemEntities.forEach { database.knowledgeDao().insertItemEntity(it) }
                seededAnything = true
            }
            if (database.diaryDao().count() == 0) {
                database.diaryDao().insertAll(SampleSeed.diaryEntries)
                seededAnything = true
            }
            if (seededAnything) {
                userPreferencesRepository.setLastSyncedAt(formatSyncTime(LocalDateTime.now()))
            }
        }
    }
}
