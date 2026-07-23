package com.shiro.yosugahub.data.file.model

import kotlinx.serialization.Serializable

/**
 * アプリ → ChatGPT(ヨスガ)へ渡す状況JSON(設計書2.3「アプリからChatGPTへ」)。
 * schemaVersion を必ず含める(設計書15章)。
 * v2(v3-Step 2-e): tasks / recentDecisions を追加し、AIが現状を把握できるようにする。
 * responseSchemaVersion は「この版の提案JSONで返してほしい」という宣言。
 */
@Serializable
data class ContextExport(
    val schemaVersion: Int = SCHEMA_VERSION,
    val responseSchemaVersion: Int = RESPONSE_SCHEMA_VERSION,
    val generatedAt: String,
    val userContext: UserContext,
    val calendar: CalendarExport,
    val projects: List<ProjectExport>,
    val tasks: List<TaskExport> = emptyList(),
    val recentDecisions: List<DecisionExport> = emptyList(),
    val recentAssistantExchange: String? = null,
) {
    companion object {
        /** このアプリが出力する状況JSONのスキーマ版。 */
        const val SCHEMA_VERSION = 2

        /** 回答JSONとして期待するスキーマ版(proposals 形式)。 */
        const val RESPONSE_SCHEMA_VERSION = 2
    }
}

@Serializable
data class UserContext(
    val purpose: String,
)

@Serializable
data class CalendarExport(
    val pastDays: Int,
    val futureDays: Int,
    val events: List<EventExport>,
)

@Serializable
data class EventExport(
    val title: String,
    val start: String,
    val end: String,
    val calendarName: String,
    val description: String = "",
)

@Serializable
data class ProjectExport(
    val id: String,
    val name: String,
    val statusMarkdown: String,
    val lastUpdated: String,
)

/** タスクの現状(v2)。AIがタスク化・優先順位の提案をする材料。 */
@Serializable
data class TaskExport(
    val projectId: String? = null,
    val title: String,
    val detail: String = "",
    val status: String,     // todo / doing / done
    val priority: String,   // high / medium / low
    val dueDate: String? = null,
)

/** 最近の決定事項(v2)。AIが過去の決定と矛盾しない提案をする材料。 */
@Serializable
data class DecisionExport(
    val date: String,       // "yyyy-MM-dd"
    val title: String,
    val body: String = "",
)
