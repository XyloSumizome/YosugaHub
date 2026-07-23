package com.shiro.yosugahub.data.repository

import android.content.Context
import com.shiro.yosugahub.data.file.AiExporter
import com.shiro.yosugahub.data.file.model.AiExportFile
import com.shiro.yosugahub.data.local.db.dao.PendingProposalDao
import com.shiro.yosugahub.data.local.db.toDomainOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * AI向け用途別JSONの生成(v4 Phase2 / AI Interface の初期実装)。
 * ドメインRepositoryからスナップショットを集め、5ファイルを組み立てて
 * `filesDir/ai/` へ保存する(サーバー同期はこの生成物を送る)。
 */
class AiExportRepository(
    private val context: Context,
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val diaryRepository: DiaryRepository,
    private val calendarRepository: CalendarRepository,
    private val projectStatusRepository: ProjectStatusRepository,
    private val pendingProposalDao: PendingProposalDao,
    private val documentRepository: DocumentRepository,
) {

    /** 6ファイルを生成してローカルへ保存し、内容を返す。 */
    suspend fun buildAndSave(): List<AiExportFile> = withContext(Dispatchers.IO) {
        val files = AiExporter.buildAll(
            generatedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            projects = projectRepository.projects().first(),
            statuses = projectStatusRepository.statuses().first(),
            tasks = taskRepository.tasks().first(),
            items = knowledgeRepository.items().first(),
            diary = diaryRepository.entries().first(),
            todayEvents = calendarRepository.todayEvents().first(),
            upcomingEvents = calendarRepository.upcomingEvents().first(),
            pastEvents = calendarRepository.pastEvents().first(),
            exchanges = pendingProposalDao.recent(EXCHANGE_LIMIT)
                .mapNotNull { it.toDomainOrNull() },
            documents = documentRepository.documents().first(),
        )

        val dir = File(context.filesDir, AI_DIR).apply { mkdirs() }
        files.forEach { file -> File(dir, file.name).writeText(file.content) }
        files
    }

    private companion object {
        const val AI_DIR = "ai"
        const val EXCHANGE_LIMIT = 50
    }
}
