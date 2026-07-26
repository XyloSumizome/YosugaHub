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
import com.shiro.yosugahub.data.local.db.MIGRATION_6_7
import com.shiro.yosugahub.data.local.db.MIGRATION_7_8
import com.shiro.yosugahub.data.local.db.YosugaDatabase
import com.shiro.yosugahub.data.file.DocumentWriter
import com.shiro.yosugahub.data.obsidian.KnowledgeStore
import com.shiro.yosugahub.data.obsidian.NoteTransformer
import com.shiro.yosugahub.data.obsidian.ObsidianVaultStore
import com.shiro.yosugahub.data.obsidian.SafVaultReader
import com.shiro.yosugahub.data.obsidian.SafVaultWriter
import com.shiro.yosugahub.data.security.KeystoreTokenCrypto
import com.shiro.yosugahub.data.repository.AssistantRepository
import com.shiro.yosugahub.data.repository.CalendarRepository
import com.shiro.yosugahub.data.repository.ContextHistoryRepository
import com.shiro.yosugahub.data.repository.ConversationImportRepository
import com.shiro.yosugahub.data.repository.DiaryRepository
import com.shiro.yosugahub.data.repository.DirectiveRepository
import com.shiro.yosugahub.data.repository.DocumentRepository
import com.shiro.yosugahub.data.repository.ExportRepository
import com.shiro.yosugahub.data.github.GitHubApi
import com.shiro.yosugahub.data.repository.AiExportRepository
import com.shiro.yosugahub.data.repository.GitHubSettingsRepository
import com.shiro.yosugahub.data.repository.GitHubStatusRepository
import com.shiro.yosugahub.data.repository.ImportRepository
import com.shiro.yosugahub.data.repository.KnowledgeRepository
import com.shiro.yosugahub.data.repository.ProjectRepository
import com.shiro.yosugahub.data.repository.MorningRoutineRepository
import com.shiro.yosugahub.data.repository.NoteImportRepository
import com.shiro.yosugahub.data.repository.ProjectStatusRepository
import com.shiro.yosugahub.data.repository.ProposalRepository
import com.shiro.yosugahub.data.repository.RepoNoteRepository
import com.shiro.yosugahub.data.repository.ServerSyncRepository
import com.shiro.yosugahub.data.repository.SyncSettingsRepository
import com.shiro.yosugahub.data.repository.TaskRepository
import com.shiro.yosugahub.data.repository.VaultRepository
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
    val directiveRepository: DirectiveRepository
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
    val vaultRepository: VaultRepository
    val documentWriter: DocumentWriter
    val contextHistoryRepository: ContextHistoryRepository
    val repoNoteRepository: RepoNoteRepository
    val noteImportRepository: NoteImportRepository
    val conversationImportRepository: ConversationImportRepository
    val morningRoutineRepository: MorningRoutineRepository
}

/** Room + DataStore を用いる既定の実装。 */
class DefaultAppContainer(
    context: Context,
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : AppContainer {

    private val database: YosugaDatabase = Room.databaseBuilder(
        context.applicationContext,
        YosugaDatabase::class.java,
        "yosuga.db",
    )
        .addMigrations(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
        )
        .build()

    override val calendarRepository: CalendarRepository by lazy {
        CalendarRepository(
            dao = database.calendarEventDao(),
            dataSource = DeviceCalendarDataSource(context.applicationContext),
        )
    }
    override val projectRepository: ProjectRepository by lazy {
        ProjectRepository(database.projectDao(), database.taskDao())
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
    override val directiveRepository: DirectiveRepository by lazy {
        DirectiveRepository(database.directiveDao())
    }
    override val assistantRepository: AssistantRepository by lazy {
        AssistantRepository(database.recommendationDao())
    }
    /** 長文知識の書き出し先(初期実装は Obsidian Vault / SAF)。 */
    private val knowledgeStore: KnowledgeStore by lazy {
        ObsidianVaultStore(context.applicationContext, userPreferencesRepository)
    }

    /** Vault への書き込み口(v5 Phase 3-b / 3-d で共用)。 */
    private val vaultWriter by lazy {
        SafVaultWriter(context.applicationContext, userPreferencesRepository)
    }

    /** ヨスガの会話まとめを Vault へ保存する(v5 Phase 3-d)。 */
    override val conversationImportRepository: ConversationImportRepository by lazy {
        ConversationImportRepository(vaultWriter)
    }

    /** 取得した知識ノートを Vault へ収める(v5 Phase 3-b)。 */
    override val noteImportRepository: NoteImportRepository by lazy {
        NoteImportRepository(
            projectRepository = projectRepository,
            repoNoteRepository = repoNoteRepository,
            vaultWriter = vaultWriter,
            dao = database.importedNoteDao(),
        )
    }

    /**
     * 朝にやることを1つにまとめる(2026-07-26)。
     * 既存の Repository を順番に呼ぶだけで、新しいデータ経路は作らない。
     */
    override val morningRoutineRepository: MorningRoutineRepository by lazy {
        MorningRoutineRepository(
            projects = { projectRepository.projects() },
            syncCalendar = { calendarRepository.sync() },
            refreshStatus = { list -> projectStatusRepository.refreshAll(list) },
            importNotes = { onEvent -> noteImportRepository.importAll(onEvent) },
            syncServer = { serverSyncRepository.sync() },
        )
    }

    /** 各ゲームの `.yosuga/notes/` から知識ノートを取得する(v5 Phase 3-a)。 */
    override val repoNoteRepository: RepoNoteRepository by lazy {
        RepoNoteRepository(
            api = GitHubApi(),
            tokenProvider = { gitHubSettingsRepository.currentToken() },
        )
    }

    /** ヨスガへ渡したコンテキストの控え(v5 Phase 2)。 */
    override val contextHistoryRepository: ContextHistoryRepository by lazy {
        ContextHistoryRepository(context.applicationContext)
    }

    /** SAF で選ばれたファイルへの書き出し(v5 Phase 1-c)。 */
    override val documentWriter: DocumentWriter by lazy {
        DocumentWriter(context.applicationContext)
    }

    /** Vault の読み取り側(v5 Phase 1)。書き込み側の knowledgeStore と対になる。 */
    override val vaultRepository: VaultRepository by lazy {
        VaultRepository(
            reader = SafVaultReader(context.applicationContext, userPreferencesRepository),
            // Phase 1 は要約しない。Phase 4 で要約を入れる場合はここを差し替える。
            transformer = NoteTransformer.Identity,
        )
    }

    override val proposalRepository: ProposalRepository by lazy {
        ProposalRepository(
            dao = database.pendingProposalDao(),
            taskRepository = taskRepository,
            knowledgeRepository = knowledgeRepository,
            diaryRepository = diaryRepository,
            projectRepository = projectRepository,
            knowledgeStore = knowledgeStore,
            directiveRepository = directiveRepository,
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
            // GitHub の進捗を取ったら、レコルが読む側もその場で新しくする(未設定なら即座に返る)。
            // ラムダで受けるので、serverSyncRepository → AiExportRepository → ここ、の循環にならない。
            syncAfterFetch = { serverSyncRepository.sync() },
        )
    }
    override val exportRepository: ExportRepository by lazy {
        ExportRepository(
            context.applicationContext,
            projectRepository,
            calendarRepository,
            taskRepository,
            knowledgeRepository,
            diaryRepository,
            projectStatusRepository,
        )
    }
    override val importRepository: ImportRepository by lazy {
        ImportRepository(
            context.applicationContext,
            database.recommendationDao(),
            database.pendingProposalDao(),
            documentRepository,
            // 取り込んだ内容をヨスガが読む側へすぐ反映する(未設定なら sync が即座に返る)。
            syncAfterImport = { serverSyncRepository.sync() },
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
            documentRepository = documentRepository,
            directiveRepository = directiveRepository,
        )
    }

    override val serverSyncRepository: ServerSyncRepository by lazy {
        ServerSyncRepository(
            buildFiles = { aiExportRepository.buildAndSave() },
            api = SyncApi(),
            urlProvider = { syncSettingsRepository.baseUrl.first() },
            tokenProvider = { syncSettingsRepository.currentToken() },
            onSynced = {
                userPreferencesRepository.setLastSyncedAt(
                    formatSyncTime(LocalDateTime.now())
                )
            },
            // アップロードが届いた時点で未整理文書は「分類待ち」になる(v4.1)。
            onDocumentsUploaded = { documentRepository.markUnclassifiedAsPending() },
        )
    }

}
