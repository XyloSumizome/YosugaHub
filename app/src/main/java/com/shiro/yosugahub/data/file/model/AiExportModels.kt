package com.shiro.yosugahub.data.file.model

import kotlinx.serialization.Serializable

/**
 * AI向けの用途別エクスポート(v4: 巨大なJSON一つにしない)。
 * 各ファイルは schemaVersion / generatedAt を持ち、単体で読んで意味が通るようにする。
 */

/** アップロード・保存の1ファイル分。 */
@Serializable
data class AiExportFile(
    val name: String,     // 例: "projects.json"
    val content: String,  // ファイル本文(JSON文字列)
)

@Serializable
data class ProjectsFile(
    val schemaVersion: Int = AI_EXPORT_SCHEMA_VERSION,
    val generatedAt: String,
    val projects: List<ProjectExport> = emptyList(),
)

@Serializable
data class TasksFile(
    val schemaVersion: Int = AI_EXPORT_SCHEMA_VERSION,
    val generatedAt: String,
    val tasks: List<TaskExport> = emptyList(),
)

@Serializable
data class KnowledgeFile(
    val schemaVersion: Int = AI_EXPORT_SCHEMA_VERSION,
    val generatedAt: String,
    val items: List<KnowledgeItemExport> = emptyList(),
    val diary: List<DiaryExport> = emptyList(),
)

@Serializable
data class CalendarFile(
    val schemaVersion: Int = AI_EXPORT_SCHEMA_VERSION,
    val generatedAt: String,
    val today: List<EventExport> = emptyList(),
    val upcoming: List<EventExport> = emptyList(),
    val past: List<EventExport> = emptyList(),
)

@Serializable
data class ConversationsFile(
    val schemaVersion: Int = AI_EXPORT_SCHEMA_VERSION,
    val generatedAt: String,
    /** 回答JSONの取込・承認の履歴(会話本文は保持しないため、やり取りの記録で代用)。 */
    val exchanges: List<ExchangeExport> = emptyList(),
)

/**
 * 未整理文書(v4.1)。ヨスガに分類してほしいものだけを載せる。
 * 分類済み・アーカイブ済みは対象外(再分類を誘発しないため)。
 */
@Serializable
data class DocumentsFile(
    val schemaVersion: Int = AI_EXPORT_SCHEMA_VERSION,
    val generatedAt: String,
    val pendingClassification: List<DocumentExport> = emptyList(),
)

/** 分類対象の文書1件。原文をそのまま渡す(要約するのはヨスガの仕事)。 */
@Serializable
data class DocumentExport(
    /** 分類結果を返すときに使うID(回答JSONの document_id)。 */
    val documentId: String,
    val title: String,
    val body: String,
    val status: String,
    val createdAt: String = "",
)

@Serializable
data class KnowledgeItemExport(
    val kind: String,
    val title: String,
    val body: String = "",
    val tags: List<String> = emptyList(),
    val entities: List<EntityRefExport> = emptyList(),
    val createdAt: String = "",
)

@Serializable
data class EntityRefExport(
    val name: String,
    val type: String,
)

@Serializable
data class DiaryExport(
    val date: String,
    val body: String,
)

@Serializable
data class ExchangeExport(
    val type: String,       // task / item / diary / health
    val status: String,     // pending / approved / rejected
    val receivedAt: String,
    val title: String = "",
)

const val AI_EXPORT_SCHEMA_VERSION = 1
