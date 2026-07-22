package com.shiro.yosugahub.data.repository

import android.content.Context
import com.shiro.yosugahub.data.file.ContextExporter
import com.shiro.yosugahub.domain.model.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** エクスポート結果。共有用のJSON本文と、保存したファイル名を持つ。 */
data class ExportResult(
    val fileName: String,
    val json: String,
)

/**
 * 状況JSONの生成と保存を担う Repository(設計書2.3 / 4.2)。
 * 現在のプロジェクト・予定のスナップショットを取り、JSON化してアプリ専用領域へ保存する。
 */
class ExportRepository(
    private val context: Context,
    private val projectRepository: ProjectRepository,
    private val calendarRepository: CalendarRepository,
) {

    /** 状況JSNを生成し `exports/` へ保存する。保存したファイル名とJSON本文を返す。 */
    suspend fun createContextExport(): ExportResult = withContext(Dispatchers.IO) {
        val projects = projectRepository.projects().first()
        val events: List<CalendarEvent> =
            calendarRepository.todayEvents().first() +
                calendarRepository.upcomingEvents().first() +
                calendarRepository.pastEvents().first()

        val now = OffsetDateTime.now()
        val export = ContextExporter.build(
            projects = projects,
            events = events,
            generatedAt = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )
        val json = ContextExporter.toJson(export)

        val fileName = "context_${now.toLocalDateTime().format(FILE_TIMESTAMP)}.json"
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        File(dir, fileName).writeText(json)

        ExportResult(fileName = fileName, json = json)
    }

    private companion object {
        val FILE_TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
    }
}
